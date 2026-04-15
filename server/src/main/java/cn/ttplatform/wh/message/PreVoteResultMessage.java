package cn.ttplatform.wh.message;

import cn.ttplatform.wh.constant.DistributableType;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * @author Wang Hao
 * @date 2020/10/2 下午4:07
 */
@Setter
@Getter
@ToString
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PreVoteResultMessage extends AbstractMessage {

    private boolean isVoted;

    @Override
    public byte getType() {
        return DistributableType.PRE_VOTE_RESULT;
    }

}
