package cn.ttplatform.wh.support;

import cn.ttplatform.wh.GlobalContext;
import io.netty.channel.*;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;

/**
 * @author Wang Hao
 * @date 2021/7/12 13:06
 */
@Slf4j
@ChannelHandler.Sharable
public abstract class AbstractDuplexChannelHandler extends ChannelDuplexHandler {

    protected final GlobalContext context;
    protected final CommonDistributor distributor;
    protected final ChannelPool channelPool;

    protected AbstractDuplexChannelHandler(GlobalContext context) {
        this.context = context;
        this.distributor = context.getDistributor();
        this.channelPool = context.getChannelPool();
    }

    @Override
    public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) throws Exception {
        super.connect(ctx, remoteAddress, localAddress, promise);
        promise.addListener(channelFuture -> {
            if (channelFuture.isSuccess()) {
                log.debug("create a connection[{} - {}]", localAddress, remoteAddress);
            }
        });
    }

    @Override
    public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        super.disconnect(ctx, promise);
        promise.addListener(channelFuture -> {
            if (channelFuture.isSuccess()) {
                log.debug("disconnect a connection[{}]", ctx.channel());
            }
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause != null) {
            log.error(cause.toString());
        }
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        super.close(ctx, promise);
        log.debug("close the channel[{}].", ctx.channel());
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            log.info("fire IdleStateEvent[{}].", evt);
            ctx.channel().close();
        } else {
            log.info("fire Event[{}].", evt);
            super.userEventTriggered(ctx, evt);
        }
    }
}
