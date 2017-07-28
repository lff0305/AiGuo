package org.lff.plainsocks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by liuff on 2017/7/16 21:13
 */
public class Server {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static ExecutorService pool = Executors.newFixedThreadPool(256);

    public static void main(String[] argu) throws IOException {

        logger.info("To Start....");

        int port = 12345;

        ServerSocket server = new ServerSocket(port);
        logger.info("Listened on port {} port");
        while (true) {
            Socket s = server.accept();
            pool.submit(() -> {
                try {
                    Processor.process(s);
                } catch (IOException e) {
                }
            });

        }
    }
}
