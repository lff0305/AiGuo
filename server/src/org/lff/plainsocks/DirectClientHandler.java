package org.lff.plainsocks;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.util.concurrent.Promise;

import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;


public final class DirectClientHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final Promise<Channel> promise;

    public DirectClientHandler(Promise<Channel> promise) {
        this.promise = promise;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.pipeline().remove(this);
        promise.setSuccess(ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable throwable) {
        promise.setFailure(throwable);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteBuf buf) throws Exception {
        logger.info("Read " + buf.readableBytes());
    }
}