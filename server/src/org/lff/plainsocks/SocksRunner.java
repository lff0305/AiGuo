package org.lff.plainsocks;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.HttpRequestWithBody;
import org.json.JSONObject;
import org.lff.SimpleAESCipher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Base64;
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
                if (nmethods == 0x2) {
                    inputStream.read();
                }
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

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        switch (atyp) {
            case 0x01: //IP V4
                // host = net.IPv4(b[4], b[5], b[6], b[7]).String()
                byte[] inet4 = new byte[4];
                int len = inputStream.read(inet4);
                if (len < 4) {
                    throw new IOException("Invalid ipv4 address");
                }
                // dst = InetAddress.getByAddress(inet4);
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
                // dst = InetAddress.getByName(h);
                out.write(host);
                break;
            case 0x04: //IP V6
                byte[] inet6 = new byte[6];
                len = inputStream.read(inet6);
                if (len < 6) {
                    throw new IOException("Invalid ipv6 address");
                }
              //  dst = InetAddress.getByAddress(inet6);
                out.write(inet6);
                break;
            default: {
                throw new IOException("Invalid Addr type {}" + atyp);
            }
        }

        out.close();

        int port =  inputStream.readShort();
        logger.info("Read port = {}", port);
      //  post(out.toByteArray(), atyp, port);
        logger.info("Request posted.");

        outputStream.write(new byte[]{0x05, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00});
        outputStream.flush();
        logger.info("Response OK to client");

        pool.submit(()-> {

            int len = 0;
            byte[] buffer = new byte[1024 * 32];
            try {
                while (len != -1) {
                    len = inputStream.read(buffer);
                    if (len > 0) {
                        byte[] source = new byte[len];
                        System.arraycopy(buffer, 0, source, 0, len);
                        byte[] result = post(out.toByteArray(), atyp, port, source);
                        outputStream.write(result);
                        outputStream.flush();
                    }
                }
            } catch (IOException e) {

            }
            logger.info("InputStream reader stopped.");
        });

    }

    private byte[] post(byte[] dst, int atyp, int port, byte[] buffer) {
        JSONObject o = new JSONObject();
        o.put("dist", Base64.getEncoder().encodeToString(dst));
        o.put("atyp", atyp);
        o.put("port", port);
        o.put("buffer", Base64.getEncoder().encodeToString(buffer));

        String body = o.toString();

        SimpleAESCipher cipher = new SimpleAESCipher();

        try {
            logger.info("To send request to remote");
            HttpResponse<String> result = Unirest.post("http://localhost:8080/h/c")
                    .body(cipher.encode(body))
                    .asString();
            return Base64.getDecoder().decode(result.getBody());
        } catch (UnirestException e) {
            e.printStackTrace();
        }
        return new byte[]{};
    }

    private void post(byte[] dst, int atyp, int port) {

        JSONObject o = new JSONObject();
        o.put("dist", Base64.getEncoder().encodeToString(dst));
        o.put("atyp", atyp);
        o.put("port", port);

        String body = o.toString();

        SimpleAESCipher cipher = new SimpleAESCipher();

        try {
            Unirest.post("http://localhost:8080/h/c")
                    .body(cipher.encode(body))
                    .asString();
        } catch (UnirestException e) {
            e.printStackTrace();
        }
    }
}
