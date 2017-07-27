package org.lff.plainsocks;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by liuff on 2017/7/16 21:13
 */
public class Server {

    private static ExecutorService pool = Executors.newFixedThreadPool(256);

    public static void main(String[] argu) throws IOException {
        ServerSocket server = new ServerSocket(12345);
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
