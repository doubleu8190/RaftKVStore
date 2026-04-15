package cn.ttplatform.wh.handler;

import cn.ttplatform.wh.cmd.AbstractCommand;
import cn.ttplatform.wh.constant.DistributableType;
import cn.ttplatform.wh.group.EndpointMetaData;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * @author Wang Hao
 * @date 2021/5/3 10:25
 */
@Getter
@Setter
@SuperBuilder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class SyncingCommand extends AbstractCommand {

    private EndpointMetaData leaderMetaData;
    private int term;
    private EndpointMetaData followerMetaData;

    @Override
    public byte getType() {
        return DistributableType.SYNCING;
    }
}
