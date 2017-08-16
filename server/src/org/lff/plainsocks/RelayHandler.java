package org.lff.plainsocks;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.ReferenceCountUtil;
import org.json.JSONObject;
import org.lff.BytesCipher;
import org.lff.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;


public final class RelayHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static ExecutorService pool = Executors.newFixedThreadPool(256, new NamedThreadFactory("Socks5Pool"));

    private final Channel relayChannel;
    private final String uid;
    private final BytesCipher cipher;
    private boolean fetchStarted = false;
    private AtomicBoolean stopped = new AtomicBoolean(false);


    public RelayHandler(Channel relayChannel, String uid, BytesCipher cipher) {
        this.relayChannel = relayChannel;
        this.uid = uid;
        this.cipher = cipher;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER);
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
        if (relayChannel.isActive()) {
            logger.info("Received {}", msg.readableBytes());
            byte[] buffer = new byte[msg.readableBytes()];
            msg.readBytes(buffer);
            pool.submit(()-> {
                String result = post(uid, buffer);
                logger.info("Post result = {}", result);
                if (!fetchStarted) {
                    pool.submit(new ContentFetcher(cipher, uid, relayChannel, stopped));
                }
            });
        } else {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        logger.info("channel Inactive for {}", uid);
        stopped.set(false);
        if (relayChannel.isActive()) {
            SocksServerUtils.closeOnFlush(relayChannel);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    private String post(String uid, byte[] buffer) {
        JSONObject o = new JSONObject();
        o.put("uid", uid);
        o.put("buffer", Base64.getEncoder().encodeToString(buffer));

        String body = o.toString();

        try {
            logger.info("To send request to remote {}", body);
            HttpResponse<String> result = Unirest.post(RemoteConfig.getPostURL())
                    .body(cipher.encode(body))
                    .asString();
            logger.info("Result from remote is ", result.getStatus());
            return new String(cipher.decodeBytes(result.getBody()));
        } catch (UnirestException e) {
            e.printStackTrace();
        }
        return null;
    }
}
