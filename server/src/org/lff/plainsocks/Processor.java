package org.lff.plainsocks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by liuff on 2017/7/16 21:18
 */
public class Processor {


    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());


    public static void process(Socket socket) throws IOException {
        SocksRunner r = new SocksRunner(socket);
        r.run();
    }
}
