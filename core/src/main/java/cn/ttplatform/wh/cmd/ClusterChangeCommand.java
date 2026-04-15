package cn.ttplatform.wh.cmd;

import cn.ttplatform.wh.constant.DistributableType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.Set;

/**
 * @author Wang Hao
 * @date 2021/4/23 23:16
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ClusterChangeCommand extends AbstractCommand {

    private Set<String> newConfig;

    @Override
    public byte getType() {
        return DistributableType.CLUSTER_CHANGE_COMMAND;
    }

    @Override
    public String toString() {
        return "ClusterChangeCommand{" +
            "id='" + id + '\'' +
            ", newConfig=" + newConfig +
            '}';
    }
}
