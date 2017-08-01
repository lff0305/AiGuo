package org.lff.aiguo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.invoke.MethodHandles;

/**
 * @author Feifei Liu
 * @datetime Aug 01 2017 18:21
 */
public class ContentReader implements Runnable {

    private Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final InputStream inputStream;
    private final PipedOutputStream outputStream;

    public ContentReader(InputStream inputStream, PipedOutputStream outputStream) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
    }

    @Override
    public void run() {
        byte[] buffer = new byte[1024 * 1024];
        int len = 0;
        while (len >= -1) {
            try {
                len = inputStream.read(buffer);
                if (len > -1) {
                    logger.info("Write {} bytes to buffer", len);
                    outputStream.write(buffer, 0, len);
                }
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
    }
}
