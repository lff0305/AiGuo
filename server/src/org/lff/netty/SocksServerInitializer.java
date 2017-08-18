package org.lff.netty;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.socks.SocksMessageEncoder;
import io.netty.handler.codec.socksx.SocksPortUnificationServerHandler;
import org.lff.BytesCipher;

/**
 * @author Feifei Liu
 * @datetime Aug 15 2017 17:32
 */
public final class SocksServerInitializer extends
        ChannelInitializer<SocketChannel> {

    private SocksMessageEncoder socksMessageEncoder;
    private SocksServerHandler socksServerHandler;

    public SocksServerInitializer(BytesCipher cipher) {
        socksMessageEncoder = new SocksMessageEncoder();
        socksServerHandler = new SocksServerHandler(cipher);
    }

    @Override
    public void initChannel(SocketChannel socketChannel) throws Exception {
        ChannelPipeline p = socketChannel.pipeline();
        p.addLast(new SocksPortUnificationServerHandler());
        p.addLast(socksServerHandler);
    }
}