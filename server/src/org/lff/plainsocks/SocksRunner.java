package org.lff.plainsocks;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.http.protocol.HTTP;
import org.json.JSONObject;
import org.lff.BytesCipher;
import org.lff.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * Created by liuff on 2017/7/16 21:22
 */
public class SocksRunner implements Runnable {

    private static ExecutorService pool = Executors.newFixedThreadPool(256, new NamedThreadFactory("Socks5Pool"));
    private static ExecutorService reader = Executors.newFixedThreadPool(256, new NamedThreadFactory("InputReader"));
    private final Socket socket;

    private final String uid;
    private final BytesCipher cipher;

    public SocksRunner(Socket socket, BytesCipher cipher) {
        this.socket = socket;
        this.uid = UUID.randomUUID().toString();
        MDC.put("uid", String.valueOf(uid.hashCode()));
        this.cipher = cipher;
    }
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final byte[] NO_AUTH = new byte[]{0x05, 0x00};
    private static final byte[] CONNECT_OK = new byte[]{0x05, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    private static final byte[] INVALID_ADDR = (new byte[]{0x05, 0x04, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00});
    private static final byte[] FAILED_TO_CONNECT = (new byte[]{0x05, 0x05, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00});

    public void run()  {

        try {
            DataInputStream inputStream = new DataInputStream(socket.getInputStream());
            OutputStream outputStream = socket.getOutputStream();
            int b = 0;
            b = inputStream.read();
            if (b == 0x5) {
                int nmethods = inputStream.read();
                byte[] methods = new byte[nmethods];
                for (int i=0; i<nmethods; i++) {
                    methods[i] = inputStream.readByte();
                }
                logger.info("Read a 0x5 {} {}", nmethods, Arrays.toString(methods));
                outputStream.write(NO_AUTH);
                outputStream.flush();

                connect(inputStream, outputStream);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void connect(final DataInputStream inputStream, final OutputStream outputStream) throws IOException {
        int ver = inputStream.read();
        int cmd = inputStream.read();
        int rsv = inputStream.read();
        int atyp = inputStream.read();
        logger.info("connect ver = {}, cmd = {}, rsv = {}, atyp = {}", ver, cmd, rsv, atyp);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InetAddress address = null;
        switch (atyp) {
            case 0x01: //IP V4
                // host = net.IPv4(b[4], b[5], b[6], b[7]).String()
                byte[] inet4 = new byte[4];
                int len = inputStream.read(inet4);
                if (len < 4) {
                    outputStream.write(INVALID_ADDR);
                    outputStream.flush();
                    return;
                }
                address = InetAddress.getByAddress(inet4);
                //logger.info("Get IPV4 Addr {}", dst.getHostAddress());
                out.write(inet4);
                break;
            case 0x03: //domain
                // host = string(b[5 : n-2]) //b[4] is the length of the domain name
                int hostLen = inputStream.readByte();
                byte[] host = new byte[hostLen];
                inputStream.read(host);
                String h = new String(host);
                logger.info("Get host = {}", h);
                address = InetAddress.getByName(h);
                out.write(host);
                break;
            case 0x04: //IP V6, 16 bytes
                byte[] inet6 = new byte[16];
                len = inputStream.read(inet6);
                if (len < 16) {
                    outputStream.write(INVALID_ADDR);
                    outputStream.flush();
                    return;
                }
                address = InetAddress.getByAddress(inet6);
                out.write(inet6);
                break;
            default: {
                logger.info("Invalid Addr type {}" + atyp);
                outputStream.write(INVALID_ADDR);
                outputStream.flush();
                return;
            }
        }

        out.close();

        int port =  inputStream.readUnsignedShort();
        logger.info("Read address = {} {}", address, port);
      //  post(out.toByteArray(), atyp, port);
        logger.info("Request posted.");

        String connected = connect(uid, out.toByteArray(), atyp, port);
        logger.info("connected = {}", connected);
        switch (connected) {
            case "OK" : {
                outputStream.write(CONNECT_OK);
                break;
            }
            case "Invalid Address" : {
                outputStream.write(INVALID_ADDR);
                break;
            }
            case "Failed to connect": {
                outputStream.write(FAILED_TO_CONNECT);
                break;
            }
            case "Failed to start reader": {
                outputStream.write(new byte[]{0x05, 0x01, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00});
                break;
            }
        }

        outputStream.flush();
        logger.info("Response {} to client", connected);
        if (!connected.equals("OK")) {
            outputStream.close();
            return;
        }
        byte[] buffer = new byte[1024 * 32];
        AtomicBoolean exited = new AtomicBoolean(false);
        ContentFetcher fetcher = new ContentFetcher(cipher, uid, outputStream, exited);
        pool.submit(fetcher);
        reader.submit(()-> {
            try {
                MDC.put("uid", uid);
                int readCount = 0;
                int len = 0;
                while (len != -1) {
                    len = inputStream.read(buffer);
                    logger.info("{}: Read {} bytes from socket source", ++readCount, len);
                    if (len > 0) {
                        byte[] source = new byte[len];
                        System.arraycopy(buffer, 0, source, 0, len);
                        byte[] result = post(uid, out.toByteArray(), atyp, port, source);
                    }
                }
                logger.info("InputStream exited.");
                exited.set(true);
            } catch (IOException e) {

            }
        });
        fetcher.waitForExit();
        disconnect(uid);
        outputStream.close();
    }

    private void disconnect(String uid) {
        JSONObject o = new JSONObject();
        o.put("uid", uid);

        String body = o.toString();

        logger.info("To send disconnect request to remote");
        Unirest.post(RemoteConfig.getCloseURL()).body(cipher.encode(body)).asStringAsync();
        logger.info("Disconnect request to remote was sent");
    }

    private String connect(String uid, byte[] dst, int atyp, int port) {
        JSONObject o = new JSONObject();
        o.put("dist", Base64.getEncoder().encodeToString(dst));
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
            e.printStackTrace();
        }
        return "Failed to connect";
    }

    private byte[] post(String uid, byte[] dst, int atyp, int port, byte[] buffer) {
        JSONObject o = new JSONObject();
        o.put("dist", Base64.getEncoder().encodeToString(dst));
        o.put("atyp", atyp);
        o.put("port", port);
        o.put("uid", uid);
        o.put("buffer", Base64.getEncoder().encodeToString(buffer));

        String body = o.toString();

        try {
            logger.info("To send request to remote {}", body);
            HttpResponse<String> result = Unirest.post(RemoteConfig.getPostURL())
                    .body(cipher.encode(body))
                    .asString();
            logger.info("Result from remote is ", result.getStatus());
            return cipher.decodeBytes(result.getBody());
        } catch (UnirestException e) {
            e.printStackTrace();
        }
        return new byte[]{};
    }

}
