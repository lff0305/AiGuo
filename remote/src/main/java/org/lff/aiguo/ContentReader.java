package org.lff.aiguo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author Feifei Liu
 * @datetime Aug 01 2017 18:21
 */
public class ContentReader implements Runnable {

    private Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final InputStream inputStream;
    private final ConcurrentLinkedQueue<byte[]> queue;
    private final String uid;

    public ContentReader(String uid, InputStream inputStream, ConcurrentLinkedQueue<byte[]> queue) {
        this.uid = uid;
        this.inputStream = inputStream;
        this.queue = queue;
    }

    @Override
    public void run() {
        MDC.put("uid", String.valueOf(uid.hashCode()));
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
        }
        byte[] buffer = new byte[64 * 1024];
        int len = 0;
        while (len > -1) {
            try {
                len = inputStream.read(buffer);
                if (len > -1) {
                    logger.info("Write {} bytes to buffer", len);
                    queue.add(Arrays.copyOf(buffer, len));
                }
                if (len == 0) {
                    Thread.sleep(500);
                }
            } catch (IOException e) {
                logger.info("Reader exited after {}", e.getMessage());
                return;
            } catch (InterruptedException e) {
                e.printStackTrace();
                return;
            }
        }
        logger.info("Reader for {} exited.", uid);
    }
}
