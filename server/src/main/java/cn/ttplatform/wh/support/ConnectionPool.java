package cn.ttplatform.wh.support;

import io.netty.channel.Channel;

/**
 * @author Wang Hao
 * @date 2022/2/10 20:57
 */
public class ConnectionPool extends LRU<String, Channel> {

    public ConnectionPool(int capacity) {
        super(capacity);
    }



}
