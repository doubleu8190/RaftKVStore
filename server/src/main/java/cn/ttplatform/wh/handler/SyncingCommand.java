package cn.ttplatform.wh.handler;

import cn.ttplatform.wh.cmd.AbstractCommand;
import cn.ttplatform.wh.constant.DistributableType;
import cn.ttplatform.wh.group.EndpointMetaData;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
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
    public int getType() {
        return DistributableType.SYNCING;
    }
}
