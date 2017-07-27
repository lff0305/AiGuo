package org.lff.plainsocks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Created by liuff on 2017/7/16 21:22
 */
public class SocksRunner implements Runnable {

    private static ExecutorService pool = Executors.newFixedThreadPool(256);

    private final Socket socket;

    public SocksRunner(Socket socket) {
        this.socket = socket;
    }
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final byte[] NO_AUTH = new byte[]{0x05, 0x00};

    public void run()  {

        try {
            DataInputStream inputStream = new DataInputStream(socket.getInputStream());
            OutputStream outputStream = socket.getOutputStream();
            int b = 0;
            b = inputStream.read();
            if (b == 0x5) {
                int nmethods = inputStream.read();
                int methods = inputStream.read();
                logger.info("Read a 0x5 {} {}", nmethods, methods);
                outputStream.write(NO_AUTH);
                outputStream.flush();

                connect(inputStream, outputStream);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void connect(DataInputStream inputStream, OutputStream outputStream) throws IOException {
        int ver = inputStream.read();
        int cmd = inputStream.read();
        int rsv = inputStream.read();
        int atyp = inputStream.read();
        logger.info("connect ver = {}, cmd = {}, rsv = {}, atyp = {}", ver, cmd, rsv, atyp);

        InetAddress dst = null;
        switch (atyp) {
            case 0x01: //IP V4
                // host = net.IPv4(b[4], b[5], b[6], b[7]).String()
                byte[] inet4 = new byte[4];
                int len = inputStream.read(inet4);
                if (len < 4) {
                    throw new IOException("Invalid ipv4 address");
                }
                dst = InetAddress.getByAddress(inet4);
                logger.info("Get IPV4 Addr {}", dst.getHostAddress());
                break;
            case 0x03: //域名
                // host = string(b[5 : n-2]) //b[4]表示域名的长度
                int hostLen = inputStream.readByte();
                byte[] host = new byte[hostLen];
                inputStream.read(host);
                String h = new String(host);
                logger.info("Get host = {}", h);
                dst = InetAddress.getByName(h);
                break;
            case 0x04: //IP V6
                byte[] inet6 = new byte[4];
                len = inputStream.read(inet6);
                if (len < 4) {
                    throw new IOException("Invalid ipv6 address");
                }
                dst = InetAddress.getByAddress(inet6);
                break;
            default: {
                throw new IOException("Invalid Addr type {}" + atyp);
            }
        }


        if (dst != null) {
            int port =  inputStream.readShort();
            logger.info("Read port = {}", port);
            Socket worker = new Socket();
            worker.connect(new InetSocketAddress(dst, port));
            logger.info("Connected");

            outputStream.write(new byte[]{0x05, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00});
            logger.info("Response OK to client");

            pool.submit(()-> {

                int len = 0;
                byte[] buffer = new byte[1024 * 32];
                OutputStream workerOutputStream = null;
                try {
                    workerOutputStream = worker.getOutputStream();
                    while (len != -1) {
                        len = inputStream.read(buffer);
                        if (len > 0) {
                            workerOutputStream.write(buffer, 0, len);
                        }
                    }
                } catch (IOException e) {

                }
                logger.info("InputStream reader stopped.");
                try {
                    worker.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            pool.submit(()-> {
                InputStream s = null;
                try {
                    s = worker.getInputStream();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                int len = 0;
                byte[] buffer = new byte[1024 * 32];
                while (len != -1) {
                    try {
                        len = s.read(buffer);
                        if (len > 0) {
                            outputStream.write(buffer, 0, len);
                        }
                        outputStream.flush();
                    } catch (IOException e) {
                        break;
                    }
                }
                logger.info("Worker finished.");
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }
}
