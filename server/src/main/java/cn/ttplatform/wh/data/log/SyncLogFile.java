package cn.ttplatform.wh.data.log;

import cn.ttplatform.wh.data.support.Bits;
import cn.ttplatform.wh.data.support.SyncFileOperator;
import cn.ttplatform.wh.support.Pool;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.List;

import static cn.ttplatform.wh.data.DataManager.MAX_CHUNK_SIZE;

/**
 * @author Wang Hao
 * @date 2020/7/1 下午10:12
 */
@Slf4j
public class SyncLogFile implements LogOperation {

    private final SyncFileOperator fileOperator;
    private final Pool<ByteBuffer> byteBufferPool;
    private LogFileMetadataRegion logFileMetadataRegion;

    public SyncLogFile(File file, Pool<ByteBuffer> byteBufferPool, LogFileMetadataRegion logFileMetadataRegion) {
        this.logFileMetadataRegion = logFileMetadataRegion;
        this.fileOperator = new SyncFileOperator(file, byteBufferPool);
        this.byteBufferPool = byteBufferPool;
    }

    @Override
    public long append(Log log) {
        long offset = logFileMetadataRegion.getFileSize();
        byte[] command = log.getCommand();
        int contentLength = calContentLength(command);
        int length = LOG_HEADER_SIZE + contentLength;
        ByteBuffer byteBuffer = byteBufferPool.allocate(length);
        try {
            byteBuffer.putInt(log.getIndex());
            byteBuffer.putInt(log.getTerm());
            byteBuffer.putInt(log.getType());
            byteBuffer.putInt(contentLength);
            if (command != null) {
                byteBuffer.putInt(command.length);
                byteBuffer.put(command);
            }
            fileOperator.append(offset, byteBuffer, length);
            logFileMetadataRegion.recordFileSize(offset + length);
        } finally {
            byteBufferPool.recycle(byteBuffer);
        }
        return offset;
    }

    @Override
    public long[] append(List<Log> logs) {
        long base = logFileMetadataRegion.getFileSize();
        long[] offsets = new long[logs.size()];
        int[] contentLengths = new int[logs.size()];
        long offset = base;
        for (int i = 0; i < logs.size(); i++) {
            offsets[i] = offset;
            byte[] command = logs.get(i).getCommand();
            int contentLength = calContentLength(command);
            contentLengths[i] = contentLength;
            offset += (LOG_HEADER_SIZE + contentLength);
        }
        int contentLength = (int) (offset - base);
        ByteBuffer byteBuffer = byteBufferPool.allocate(contentLength);
        try {
            for (int i = 0; i < logs.size(); i++) {
                Log log = logs.get(i);
                byteBuffer.putInt(log.getIndex());
                byteBuffer.putInt(log.getTerm());
                byteBuffer.putInt(log.getType());
                byteBuffer.putInt(contentLengths[i]);
                byte[] command = log.getCommand();
                if (command != null) {
                    byteBuffer.putInt(command.length);
                    byteBuffer.put(command);
                }
            }
            fileOperator.append(base, byteBuffer, contentLength);
            logFileMetadataRegion.recordFileSize(offset);
        } finally {
            byteBufferPool.recycle(byteBuffer);
        }
        return offsets;
    }

    @Override
    public void append(ByteBuffer byteBuffer, int length) {
        long fileSize = logFileMetadataRegion.getFileSize();
        fileOperator.append(fileSize, byteBuffer, length);
        logFileMetadataRegion.recordFileSize(fileSize + length);
    }

    /**
     * read a byte array from file start to end, then transfer to LogEntry
     *
     * @param start start offset
     * @param end   end offset
     * @return a log entry
     */
    @Override
    public Log getLog(long start, long end) {
        long size = logFileMetadataRegion.getFileSize();
        if (start >= size || start == end) {
            log.debug("can not load a log from offset[{}] to offset[{}].", start, end);
            return null;
        }
        int readLength;
        if (end < start) {
            readLength = (int) (size - start);
        } else {
            readLength = (int) (end - start);
        }
        ByteBuffer byteBuffer = byteBufferPool.allocate(readLength);
        try {
            fileOperator.readBytes(start, byteBuffer, readLength);
            // Convert a byte array to {@link Log}
            // index[0-3]
            int index = byteBuffer.getInt();
            // term[4-7]
            int term = byteBuffer.getInt();
            // type[8,11]
            int type = byteBuffer.getInt();
            // contentLength[12,15]
            // cmd[16,content.length]
            int contentLength = byteBuffer.getInt();
            byte[] cmd = null;
            if (contentLength > 0) {
                int cmdLength = byteBuffer.getInt();
                cmd = new byte[cmdLength];
                byteBuffer.get(cmd, 0, cmdLength);
            }
            return LogFactory.createEntry(type, term, index, cmd);
        } finally {
            byteBufferPool.recycle(byteBuffer);
        }
    }

    @Override
    public void loadLogsIntoList(long start, long end, List<Log> res) {
        int size = (int) (end - start);
        ByteBuffer byteBuffer = byteBufferPool.allocate(size);
        try {
            fileOperator.readBytes(start, byteBuffer, size);
            int offset = 0;
            while (offset < size) {
                int index = byteBuffer.getInt();
                int term = byteBuffer.getInt();
                int type = byteBuffer.getInt();
                int contentLength = byteBuffer.getInt();
                byte[] cmd = null;
                if (contentLength > 0) {
                    int cmdLength = byteBuffer.getInt();
                    cmd = new byte[cmdLength];
                    byteBuffer.get(cmd, 0, cmdLength);
                }
                offset += (contentLength + LOG_HEADER_SIZE);
                res.add(LogFactory.createEntry(type, term, index, cmd));
            }
        } finally {
            byteBufferPool.recycle(byteBuffer);
        }
    }

    @Override
    public ByteBuffer[] read() {
        int position = 0;
        int fileSize = (int) logFileMetadataRegion.getFileSize();
        int size = fileSize % MAX_CHUNK_SIZE == 0 ? fileSize / MAX_CHUNK_SIZE : fileSize / MAX_CHUNK_SIZE + 1;
        ByteBuffer[] buffers = new ByteBuffer[size];
        int index = 0;
        while (position < fileSize) {
            int readLength = Math.min(fileSize - position, MAX_CHUNK_SIZE);
            ByteBuffer byteBuffer = byteBufferPool.allocate(MAX_CHUNK_SIZE);
            fileOperator.readBytes(position, byteBuffer, readLength);
            position += readLength;
            buffers[index++] = byteBuffer;
        }
        return buffers;
    }

    @Override
    public void transferTo(long offset, LogOperation dst) {
        long fileSize = logFileMetadataRegion.getFileSize();
        int contentLength = (int) (fileSize - offset);
        ByteBuffer byteBuffer = byteBufferPool.allocate(contentLength);
        try {
            fileOperator.readBytes(offset, byteBuffer, contentLength);
            dst.append(byteBuffer, contentLength);
        } finally {
            byteBufferPool.recycle(byteBuffer);
        }
    }

    @Override
    public void exchangeLogFileMetadataRegion(LogFileMetadataRegion logFileMetadataRegion) {
        logFileMetadataRegion.recordFileSize(this.logFileMetadataRegion.getFileSize());
        this.logFileMetadataRegion.clear();
        this.logFileMetadataRegion = logFileMetadataRegion;
    }

    @Override
    public void removeAfter(long offset) {
        offset = Math.max(offset, 0);
        fileOperator.truncate(offset);
        logFileMetadataRegion.recordFileSize(offset);
    }

    @Override
    public void close() {
        fileOperator.close();
    }

    @Override
    public long size() {
        return logFileMetadataRegion.getFileSize();
    }

    @Override
    public boolean isEmpty() {
        return logFileMetadataRegion.getFileSize() == 0;
    }
}
