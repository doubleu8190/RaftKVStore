package cn.ttplatform.wh.role;

import java.util.concurrent.ScheduledFuture;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Wang Hao
 * @date 2020/6/30 下午9:12
 */
@Setter
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public abstract class AbstractRole implements Role {

    protected int term;
    protected ScheduledFuture<?> scheduledFuture;

    @Override
    public void cancelTask() {
        if (scheduledFuture != null) {
            log.debug("cancel task...");
            scheduledFuture.cancel(false);
        }
    }

}
