package org.lff.plainsocks;

import org.lff.BytesCipher;
import org.lff.Configuration;
import org.lff.ECCipher;
import org.lff.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.Remote;
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

        String remote = Configuration.getData("remote");
        RemoteConfig.remote = remote;

        String base = Configuration.getData("uri.base");
        RemoteConfig.base = base;

        String keyUri = Configuration.getData("uri.key");
        RemoteConfig.keyUri = keyUri;

        String aes = fetchAES();
        logger.info("Get AES from remote server = {}", aes);

        String ecpublic = Configuration.getData("public");
        String ecprivate = Configuration.getData("private");

        ECCipher keyCipher = new ECCipher(ecpublic, ecprivate);

        final BytesCipher cipher = CipherBuilder.buildContentCipher(keyCipher.decodeBytes(aes));

        int port = 12345;

        ServerSocket server = new ServerSocket(port);
        logger.info("Listened on port {}", port);
        while (true) {
            Socket s = server.accept();
            pool.submit(() -> {
                try {
                    Processor.process(s, cipher);
                } catch (IOException e) {
                }
            });

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
