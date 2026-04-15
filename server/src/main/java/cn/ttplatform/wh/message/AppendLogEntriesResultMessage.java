package cn.ttplatform.wh.message;

import cn.ttplatform.wh.constant.DistributableType;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * @author Wang Hao
 * @date 2020/6/30 下午9:24
 */
@Getter
@Setter
@SuperBuilder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class AppendLogEntriesResultMessage extends AbstractMessage{

    private int term;
    private int lastLogIndex;
    private boolean success;

    @Override
    public byte getType() {
        return DistributableType.APPEND_LOG_ENTRIES_RESULT;
    }

}
