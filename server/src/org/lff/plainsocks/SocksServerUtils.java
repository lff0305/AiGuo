package org.lff.plainsocks;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;

/**
 * @author Feifei Liu
 * @datetime Aug 15 2017 17:38
 */
public class SocksServerUtils {

    /**
     * Closes the specified channel after all queued write requests are flushed.
     */
    public static void closeOnFlush(Channel ch) {
        if (ch.isActive()) {
            ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }

    private SocksServerUtils() {
    }
}
