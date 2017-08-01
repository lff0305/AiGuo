package org.lff.aiguo;

import org.json.JSONObject;
import org.lff.SimpleAESCipher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Feifei Liu
 * @datetime Jul 31 2017 16:50
 */
@Controller
@RequestMapping("/h")
public class ItemController {

    static ConcurrentHashMap<String, Socket> socketsMap = new ConcurrentHashMap<>();
    static ConcurrentHashMap<String, PipedInputStream> bufferMap = new ConcurrentHashMap<>();

    private Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static ExecutorService pool = Executors.newFixedThreadPool(256);

    private SimpleAESCipher cipher = new SimpleAESCipher();

    @RequestMapping(path = "/c", method = RequestMethod.POST)
    public @ResponseBody
    String doit(@RequestBody String body) throws IOException {

        logger.info("Getting a request.");
        String json = cipher.decode(body);
        JSONObject o = new JSONObject(json);
        logger.info("Getting a request json {}", o.toString());
        byte[] buffer = new byte[]{};
        String strBuffer = o.optString("buffer");
        if (strBuffer != null) {
            buffer = Base64.getDecoder().decode(strBuffer);
        }
        String uid = o.getString("uid");

        Socket worker = socketsMap.get(uid);

        try {
            if (buffer != null) {
                logger.info("Writting {}" , new String(buffer));
                worker.getOutputStream().write(buffer, 0, buffer.length);
            }
        } catch (IOException e) {
            return Base64.getEncoder().encodeToString("ERR".getBytes());
        }

        return Base64.getEncoder().encodeToString("OK".getBytes());
    }

    @RequestMapping(path = "/g", method = RequestMethod.POST)
    public @ResponseBody
    String connect(@RequestBody String body) throws IOException {
        logger.info("Getting a request.");
        String json = cipher.decode(body);
        JSONObject o = new JSONObject(json);
        logger.info("Getting a request json {}", o.toString());
        byte[] dst = Base64.getDecoder().decode(o.getString("dist"));
        int atyp = o.getInt("atyp");
        int port = o.getInt("port");
        byte[] buffer = new byte[]{};
        String uid = o.optString("uid");
        InetAddress address = null;

        switch (atyp) {
            case 0x01: //IP V4
                address = InetAddress.getByAddress(dst);
                break;
            case 0x03: //domain
                String h = new String(dst);
                address = InetAddress.getByName(h);
                break;
            case 0x04: //IP V6
                byte[] inet6 = new byte[6];
                break;
            default: {
            }
        }

        Socket worker = new Socket();
        worker.setKeepAlive(false);
        worker.connect(new InetSocketAddress(address, port));

        socketsMap.put(uid, worker);
        PipedOutputStream out = new PipedOutputStream();
        bufferMap.put(uid, new PipedInputStream(out));

        ContentReader reader = new ContentReader(worker.getInputStream(), out);
        new Thread(reader).start();

        return Base64.getEncoder().encodeToString("OK".getBytes());
    }

    @RequestMapping(path = "/p", method = RequestMethod.POST)
    public @ResponseBody
    String close(@RequestBody String body) {
        logger.info("Getting a request.");
        String json = cipher.decode(body);
        JSONObject o = new JSONObject(json);
        logger.info("Getting a request json {}", o.toString());
        String uid = o.optString("uid");
        PipedInputStream pis = bufferMap.get(uid);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[32 * 1024];
        int len = 0;
        while (len > -1) {
            try {
                len = pis.read(buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (len > 0) {
                out.write(buffer, 0, len);
            } else {
                break;
            }
        }

        return Base64.getEncoder().encodeToString(out.toByteArray());
    }
}
