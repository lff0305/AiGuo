package org.lff.aiguo;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author Feifei Liu
 * @datetime Aug 15 2017 15:20
 */
public class NettyReader {

    private static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static NioEventLoopGroup group = new NioEventLoopGroup();
    private static Bootstrap b = new Bootstrap();
    private final InetAddress address;
    private final int port;
    private final String uid;
    private final ConcurrentLinkedQueue out;

    public NettyReader(String uid, InetAddress address, int port, ConcurrentLinkedQueue out) {
        this.address = address;
        this.port = port;
        this.uid = uid;
        this.out = out;
    }

    public ResponseHandler connect() {
        try {

            ResponseHandler handler = new ResponseHandler(uid, out);

            ChannelFuture channel = b.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(handler);
                        }
                    }).connect().await();
            return handler;
        } catch (Exception e) {
            logger.error("Error connect", e);
        }
        return null;
    }
}

class ResponseHandler extends ChannelInboundHandlerAdapter {

    private static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final String uid;
    private final ConcurrentLinkedQueue out;
    private ChannelHandlerContext activeCtx;

    public ResponseHandler(String uid, ConcurrentLinkedQueue out) {
        this.uid = uid;
        this.out = out;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        logger.info("HelloClientIntHandler.channelRead");
        ByteBuf result = (ByteBuf) msg;
        byte[] buffer = new byte[result.readableBytes()];
        result.readBytes(buffer);
        out.add(buffer);
        result.release();
    }

    // 连接成功后，向server发送消息
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.info("HelloClientIntHandler.channelActive");
        this.activeCtx = ctx;
    }

    public boolean send(byte[] buffer) {
        if (activeCtx == null) {
            return false;
        }
        ByteBuf encoded = activeCtx.alloc().buffer(buffer.length);
        encoded.writeBytes(buffer);
        activeCtx.write(encoded);
        activeCtx.flush();
        return true;
    }

    public void close() {
        activeCtx.close();
    }
}
