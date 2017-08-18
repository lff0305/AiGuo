package org.lff.plainsocks;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.lff.BytesCipher;
import org.lff.Configuration;
import org.lff.ECCipher;
import org.lff.NamedThreadFactory;
import org.lff.netty.SocksServerInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by liuff on 2017/7/16 21:13
 * a
 */
public class Server {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static ExecutorService pool = Executors.newFixedThreadPool(256, new NamedThreadFactory("ConnectionPool"));

    public static void main(String[] argu) throws IOException {
        MDC.put("uid", "BASE");
        logger.info("To Start....");

        RemoteConfig.init();

        String aes = fetchAES();
        logger.info("Get AES from remote server = {}", aes);

        String ecpublic = Configuration.getData("public");
        String ecprivate = Configuration.getData("private");

        ECCipher keyCipher = new ECCipher(ecpublic, ecprivate);

        final BytesCipher cipher = CipherBuilder.buildContentCipher(keyCipher.decodeBytes(aes));

        int port = 12345;

        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch)
                                throws Exception {
                            // 注册handler
                            ch.pipeline().addLast(new SocksServerInitializer(cipher));
                        }
                    }).option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture f = b.bind(port).sync();

            f.channel().closeFuture().sync();
        } catch (Exception e) {
            logger.error("Failed to init", e);
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    private static String fetchAES() {
        try {
            logger.info("Getting key from {}", RemoteConfig.getKeyURL());
            String aes = SimpleHttpClient.get(RemoteConfig.getKeyURL(), new HashMap<>());
            return aes;
        } catch (IOException e) {
            logger.error("Failed to fetch key", e);
            System.exit(1);
            return null;
        }
    }
}
