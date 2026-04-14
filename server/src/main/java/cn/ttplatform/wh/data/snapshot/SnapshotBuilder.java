package cn.ttplatform.wh.data.snapshot;

import cn.ttplatform.wh.data.FileManager;
import cn.ttplatform.wh.data.support.SyncFileOperator;
import cn.ttplatform.wh.exception.OperateFileException;
import cn.ttplatform.wh.support.Pool;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;

/**
 * @author Wang Hao
 * @date 2021/5/17 16:30
 */
@Slf4j
public class SnapshotBuilder {

    @Getter
    private File snapshotFile;
    @Getter
    // 生成快照的节点ID
    private String snapshotSource;
    // 快照中最后一条日志的索引
    private int lastIncludeIndex;
    // 快照中最后一条日志的任期
    private int lastIncludeTerm;
    private final File parent;
    private SyncFileOperator snapshotFileOperator;
    private final Pool<ByteBuffer> byteBufferPool;
    private final SnapshotFileMetadataRegion snapshotFileMetadataRegion;
    private final SnapshotFileMetadataRegion generatingSnapshotFileMetadataRegion;

    public SnapshotBuilder(File parent, Pool<ByteBuffer> byteBufferPool,
            SnapshotFileMetadataRegion snapshotFileMetadataRegion,
            SnapshotFileMetadataRegion generatingSnapshotFileMetadataRegion) {
        this.snapshotFileMetadataRegion = snapshotFileMetadataRegion;
        this.generatingSnapshotFileMetadataRegion = generatingSnapshotFileMetadataRegion;
        this.byteBufferPool = byteBufferPool;
        this.parent = parent;
    }

    public void setBaseInfo(int lastIncludeIndex, int lastIncludeTerm, String snapshotSource) {
        this.lastIncludeIndex = lastIncludeIndex;
        this.lastIncludeTerm = lastIncludeTerm;
        this.snapshotSource = snapshotSource;
        this.snapshotFile = FileManager.newSnapshotFile(parent, lastIncludeIndex, lastIncludeTerm);
        try {
            Files.deleteIfExists(snapshotFile.toPath());
            Files.createFile(snapshotFile.toPath());
        } catch (IOException e) {
            throw new OperateFileException("failed to delete or create file.", e);
        }
        generatingSnapshotFileMetadataRegion.clear();
        this.snapshotFileOperator = new SyncFileOperator(snapshotFile, byteBufferPool);
    }

    public long getInstallOffset() {
        return generatingSnapshotFileMetadataRegion.getFileSize();
    }

    public void append(byte[] chunk) {
        if (chunk.length == 0) {
            log.debug("chunk size is 0.");
            return;
        }
        long fileSize = generatingSnapshotFileMetadataRegion.getFileSize();
        int written = snapshotFileOperator.append(fileSize, chunk, chunk.length);
        generatingSnapshotFileMetadataRegion.recordFileSize(fileSize + written);
    }

    public void complete() {
        snapshotFileMetadataRegion.recordFileSize(generatingSnapshotFileMetadataRegion.getFileSize());
        snapshotFileMetadataRegion.recordLastIncludeIndex(lastIncludeIndex);
        snapshotFileMetadataRegion.recordLastIncludeTerm(lastIncludeTerm);
        snapshotFileOperator.close();
    }
}
