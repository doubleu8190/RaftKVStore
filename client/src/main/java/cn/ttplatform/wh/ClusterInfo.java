package cn.ttplatform.wh;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Cluster information returned by {@code getClusterInfo()}.
 *
 * @author Wang Hao
 * @date 2021/5/26 21:25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClusterInfo {

    private String leader;
    private String phase;
    private String mode;
    private String oldConfig;
    private String newConfig;
    private int size;
}
