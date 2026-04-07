package cn.ttplatform.wh.data.log;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * @author Wang Hao
 * @date 2021/6/8 23:48
 */
public interface LogOperation {

    /**
     * index(4 bytes) + term(4 bytes) + type(4 bytes) + contentLength(4 bytes) = 16
     */
    int LOG_HEADER_SIZE = 4 + 4 + 4 + 4;

    /**
     * Append a log to file
     * 将日志追加到文件，日志长度对齐到4的倍数，因此内存布局将会是这样
     * index｜term｜type｜contentLength｜commandLength｜command｜padding（必要时填充）
     * @param log log to append
     * @return next offset
     */
    long append(Log log);

    long[] append(List<Log> logs);

    void append(ByteBuffer byteBuffer,int length);

    /**
     * read a byte array from file start to end, then transfer to LogEntry
     *
     * @param start start offset
     * @param end   end offset
     * @return an log entry
     */
    Log getLog(long start, long end);

    void loadLogsIntoList(long start, long end, List<Log> res);

    ByteBuffer[] read();

    void transferTo(long offset, LogOperation dst);

    void exchangeLogFileMetadataRegion(LogFileMetadataRegion logFileMetadataRegion);

    void removeAfter(long offset);

    void close();

    long size();

    boolean isEmpty();
}
