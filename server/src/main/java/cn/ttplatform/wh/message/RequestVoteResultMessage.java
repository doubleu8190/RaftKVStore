package cn.ttplatform.wh.message;

import cn.ttplatform.wh.constant.DistributableType;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * @author Wang Hao
 * @date 2020/6/30 下午9:21
 */
@Getter
@Setter
@ToString
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class RequestVoteResultMessage extends AbstractMessage {

    private int term;
    private boolean isVoted;

    @Override
    public byte getType() {
        return DistributableType.REQUEST_VOTE_RESULT;
    }

}
