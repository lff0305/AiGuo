package org.lff.aiguo;

import org.json.JSONObject;
import org.lff.SimpleAESCipher;
import org.lff.aiguo.exception.InvalidRequest;
import org.lff.aiguo.vo.FetchVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Feifei Liu
 * @datetime Jul 31 2017 16:50
 */
@Controller
@RequestMapping("/h")
public class ItemController {

    @Autowired
    RemoteConfig config;

    static ConcurrentHashMap<String, Socket> socketsMap = new ConcurrentHashMap<>();
    static ConcurrentHashMap<String, ConcurrentLinkedQueue<byte[]>> bufferMap = new ConcurrentHashMap<>();

    private Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static ExecutorService pool = Executors.newFixedThreadPool(256);

    private SimpleAESCipher cipher = new SimpleAESCipher();

    @RequestMapping(path = "/c", method = RequestMethod.POST)
    public @ResponseBody
    String doit(@RequestBody String body) throws IOException {
        delayIfConfigured();
        logger.info("Getting a request for work");
        String json = cipher.decode(body);
        JSONObject o = new JSONObject(json);
        logger.info("Getting a work request json {}", o.toString());
        byte[] buffer = new byte[]{};
        String strBuffer = o.optString("buffer");
        if (strBuffer != null) {
            buffer = Base64.getDecoder().decode(strBuffer);
        }
        String uid = o.getString("uid");
        if (uid == null) {
            return cipher.encode("ERR");
        }

        MDC.put("uid", String.valueOf(uid.hashCode()));
        Socket worker = socketsMap.get(uid);

        if (worker == null) {
            return cipher.encode("ERR");
        }

        try {
            if (buffer != null) {
                logger.info("Writting {} bytes" , buffer.length);
                worker.getOutputStream().write(buffer, 0, buffer.length);
            }
        } catch (IOException e) {
            return cipher.encode("ERR");
        }


        logger.info("OK returned");

        return cipher.encode("OK");
    }

    @RequestMapping(path = "/g", method = RequestMethod.POST)
    public @ResponseBody
    String connect(@RequestBody String body) {
        delayIfConfigured();
        MDC.put("uid", "CONN");
        logger.info("Getting a request for connect");
        String json = cipher.decode(body);
        JSONObject o = new JSONObject(json);
        logger.info("Getting a connect request json {}", o.toString());
        byte[] dst = Base64.getDecoder().decode(o.getString("dist"));
        int atyp = o.getInt("atyp");
        int port = o.getInt("port");
        String uid = o.optString("uid");
        InetAddress address = null;
        if (uid == null) {
            return cipher.encode("ERR");
        }

        MDC.put("uid", String.valueOf(uid.hashCode()));
        try {
            switch (atyp) {
                case 0x01: //IP V4
                    address = InetAddress.getByAddress(dst);
                    break;
                case 0x03: //domain
                    String h = new String(dst);
                    address = InetAddress.getByName(h);
                    break;
                case 0x04: //IP V6
                    address = InetAddress.getByAddress(dst);
                    break;
                default: {
                }
            }
        } catch (Exception e) {
            return cipher.encode("Invalid Address");
        }

        Socket worker = new Socket();
        InetSocketAddress remote = new InetSocketAddress(address, port);
        try {
            logger.info("TO start to connect to {}", remote );
            worker.setKeepAlive(false);
            worker.setSoTimeout(15000);
            worker.setReceiveBufferSize(128 * 1024);
            worker.connect(remote);
            logger.info("Connect to {} {} successfully.", uid, remote );
        } catch (Exception e) {
            logger.info("Failed to connect to {} {} ", uid, remote, e);
            return cipher.encode("Failed to connect".getBytes());
        }

        socketsMap.put(uid, worker);
        ConcurrentLinkedQueue out = new ConcurrentLinkedQueue();
        try {
            bufferMap.put(uid, out);
            ContentReader reader = new ContentReader(uid, worker.getInputStream(), out);
            pool.submit(reader);
        } catch (Exception e) {
            logger.error("Failed to start reader", e);
            return cipher.encode("Failed to start reader".getBytes());
        }

        return cipher.encode("OK");
    }

    @RequestMapping(path = "/p", method = RequestMethod.POST)
    public @ResponseBody String fetch(@RequestBody String body) {
        String uid = null;
        delayIfConfigured();
        try {
            uid = getUid(body);
        } catch (InvalidRequest e) {
            return cipher.encode(FetchVO.buildError().getBytes());
        }

        ConcurrentLinkedQueue<byte[]> queue = bufferMap.get(uid);
        if (queue == null) {
            return cipher.encode(FetchVO.noContent().getBytes());
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        byte[] buffer = queue.poll();

        if (buffer == null) {
            logger.info("Nothing to fetch, exit.");
            return cipher.encode(FetchVO.noContent().getBytes());
        }
        logger.info("Available bytes {}", buffer.length);
        while (buffer != null) {
            out.write(buffer, 0, buffer.length);
            buffer = queue.poll();
        }



        byte[] bytes = out.toByteArray();
        logger.info("Fetch Result = " + bytes.length);
        String encoded = cipher.encode(FetchVO.build(bytes).getBytes());
        logger.info("Encoded length = {}", encoded.length());
        return encoded;
    }

    @RequestMapping(path = "/d", method = RequestMethod.POST)
    public @ResponseBody
    String close(@RequestBody String body) {
        String uid = null;
        try {
            uid = getUid(body);
            logger.info("close got for {}", uid);
            delayIfConfigured();
        } catch (InvalidRequest e) {
            return cipher.encode("ERR");
        }


        ConcurrentLinkedQueue queue = bufferMap.remove(uid);
        if (queue != null) {
            queue.clear();
        }
        Socket worker = socketsMap.remove(uid);
        try {
            if (worker != null) {
                worker.close();
                logger.info("Worker closed for {}", uid);
            }
        } catch (IOException e) {
        }

        return cipher.encode("OK");
    }

    private String getUid(String body) throws InvalidRequest {
        String uid = null;
        try {
            String json = cipher.decode(body);
            JSONObject o = new JSONObject(json);
            uid = o.optString("uid");
            if (uid == null) {
                return cipher.encode("ERR");
            }
            MDC.put("uid", String.valueOf(uid.hashCode()));
            return uid;
        } catch (Exception e) {
            throw new InvalidRequest(e.getMessage(), e);
        }
    }

    private void delayIfConfigured() {
        if (config.isManualDelay()) {
            try {
                long s = (int)(config.getManualDelayTime() * Math.random());
                logger.info("Sleep {} for manual delay", s);
                Thread.sleep(s);
            } catch (InterruptedException e) {
            }
        }
    }
}
