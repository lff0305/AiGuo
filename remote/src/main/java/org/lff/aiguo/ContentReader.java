package org.lff.aiguo;

import java.io.*;

/**
 * @author Feifei Liu
 * @datetime Aug 01 2017 18:21
 */
public class ContentReader implements Runnable {
    private final InputStream inputStream;
    private final PipedOutputStream outputStream;

    public ContentReader(InputStream inputStream, PipedOutputStream outputStream) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
    }

    @Override
    public void run() {
        byte[] buffer = new byte[32 * 1024];
        int len = 0;
        while (len >= -1) {
            try {
                len = inputStream.read(buffer);
                if (len == -1) {
                    outputStream.write(buffer, 0, len);
                }
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
    }
}
