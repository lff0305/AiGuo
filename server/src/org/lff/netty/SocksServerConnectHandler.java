package org.lff.netty;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.handler.codec.socksx.SocksMessage;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import org.json.JSONObject;
import org.lff.BytesCipher;
import org.lff.plainsocks.RemoteConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.lang.invoke.MethodHandles;

/**
 * @author Feifei Liu
 * @datetime Aug 15 2017 17:35
 */
@ChannelHandler.Sharable
public final class SocksServerConnectHandler extends
        SimpleChannelInboundHandler<SocksMessage> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());


    public static final int BUFFER_SIZE = 16384;

    private final Bootstrap b = new Bootstrap();
    private final BytesCipher cipher;
    private ByteArrayOutputStream _remoteOutStream;
    private ByteArrayOutputStream _localOutStream;
    private String uid;

    public SocksServerConnectHandler(BytesCipher cipher, String uid) {
        this.cipher = cipher;
        this._remoteOutStream = new ByteArrayOutputStream(BUFFER_SIZE);
        this._localOutStream = new ByteArrayOutputStream(BUFFER_SIZE);
        this.uid = uid;
    }

    @Override
    public void channelRead0(final ChannelHandlerContext ctx,
                             final SocksMessage message) throws Exception {
        Socks5CommandRequest request = (Socks5CommandRequest)message;
        logger.info("READ {} {} bytes", request.dstAddr(), request.dstPort(), request);
        String connected = connect(uid, request.dstAddr(),
                request.dstAddrType().byteValue(), request.dstPort());
        logger.info("Connected = {}", connected);
        switch (connected) {
            case "OK" : {
                ChannelFuture responseFuture = ctx.channel().writeAndFlush(new DefaultSocks5CommandResponse(
                        Socks5CommandStatus.SUCCESS, request.dstAddrType()));


                responseFuture.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture channelFuture) {
                        ctx.pipeline().remove(SocksServerConnectHandler.this);
                        ctx.pipeline().addLast(new RelayHandler(ctx.channel(), uid, cipher));
                    }
                });
                break;
            }
            case "Invalid Address" : {
                ctx.channel().writeAndFlush(new DefaultSocks5CommandResponse(
                        Socks5CommandStatus.HOST_UNREACHABLE, request.dstAddrType()));
                break;
            }
            case "Failed to connect": {
                ctx.channel().writeAndFlush(new DefaultSocks5CommandResponse(
                        Socks5CommandStatus.HOST_UNREACHABLE, request.dstAddrType()));
                break;
            }
            case "Failed to start reader": {
                ctx.channel().writeAndFlush(new DefaultSocks5CommandResponse(
                        Socks5CommandStatus.FAILURE, request.dstAddrType()));
                break;
            }
        }

        logger.info("Response {} to client", connected);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        SocksServerUtils.closeOnFlush(ctx.channel());
    }

    private String connect(String uid, String dst, int atyp, int port) {
        JSONObject o = new JSONObject();
        o.put("dist", dst);
        o.put("atyp", atyp);
        o.put("port", port);
        o.put("uid", uid);

        String body = o.toString();

        try {
            logger.info("To send request to remote {}", body);
            HttpResponse<String> result = Unirest.post(RemoteConfig.getConnectURL())
                    .body(cipher.encode(body))
                    .asString();
            if (result.getStatus() != 200) {
                logger.error("Failed to connect {} {}", result.getStatus() , result.getBody());
                return "Failed to connect";
            }
            logger.info("Result from remote is ", result.getStatus());
            return cipher.decode(result.getBody());
        } catch (UnirestException e) {
            logger.error("Failed to connect", e);
        }
        return "Failed to connect";
    }

}