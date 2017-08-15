package org.lff.aiguo;

import io.netty.channel.ChannelFuture;
import org.json.JSONObject;
import org.lff.BytesCipher;
import org.lff.ECCipher;
import org.lff.SimpleAESCipher;
import org.lff.aiguo.exception.InvalidRequest;
import org.lff.aiguo.service.KeyService;
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

import javax.annotation.PostConstruct;
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
@RequestMapping("${uri.base}")
public class ItemController {

    @Autowired
    RemoteConfig config;

    static ConcurrentHashMap<String, ResponseHandler> channelMap = new ConcurrentHashMap<>();
    static ConcurrentHashMap<String, ConcurrentLinkedQueue<byte[]>> bufferMap = new ConcurrentHashMap<>();

    private Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static ExecutorService pool = Executors.newFixedThreadPool(256);

    private BytesCipher keyCipher;
    private BytesCipher contentCipher = null;

    @Autowired
    private KeyService keyService;

    @PostConstruct
    private void init() {
        keyCipher = new ECCipher(config.getEcPublic(), config.getEcPrivate());
    }

    @RequestMapping(path = "${uri.post}", method = RequestMethod.POST)
    public @ResponseBody
    String doit(@RequestBody String body) throws IOException {
        delayIfConfigured();
        String json = contentCipher.decode(body);
        JSONObject o = new JSONObject(json);
        byte[] buffer = new byte[]{};
        String strBuffer = o.optString("buffer");
        if (strBuffer != null) {
            buffer = Base64.getDecoder().decode(strBuffer);
        }
        String uid = o.getString("uid");
        if (uid == null) {
            return contentCipher.encode("ERR");
        }

        MDC.put("uid", String.valueOf(uid.hashCode()));
        logger.info("Getting a work request json {}", o.toString());

        ResponseHandler worker = channelMap.get(uid);

        if (worker == null) {
            return contentCipher.encode("ERR");
        }

        if (buffer != null) {
            logger.info("Writting {} bytes" , buffer.length);
            if (!worker.send(buffer)) {
                return contentCipher.encode("ERR");
            }
        }


        logger.info("OK returned");

        return contentCipher.encode("OK");
    }

    @RequestMapping(path = "${uri.connect}", method = RequestMethod.POST)
    public @ResponseBody
    String connect(@RequestBody String body) {
        delayIfConfigured();
        MDC.put("uid", "CONN");
        logger.info("Getting a request for connect");
        String json = contentCipher.decode(body);
        JSONObject o = new JSONObject(json);
        logger.info("Getting a connect request json {}", o.toString());
        byte[] dst = Base64.getDecoder().decode(o.getString("dist"));
        int atyp = o.getInt("atyp");
        int port = o.getInt("port");
        String uid = o.optString("uid");
        InetAddress address = null;
        if (uid == null) {
            return contentCipher.encode("ERR");
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
            return contentCipher.encode("Invalid Address");
        }

        ConcurrentLinkedQueue out = new ConcurrentLinkedQueue();
        try {
            bufferMap.put(uid, out);
            NettyReader reader = new NettyReader(uid, address, port, out);
            ResponseHandler channel = reader.connect();
            channelMap.put(uid, channel);
        } catch (Exception e) {
            logger.error("Failed to start reader", e);
            return contentCipher.encode("Failed to start reader".getBytes());
        }

        return contentCipher.encode("OK");
    }

    @RequestMapping(path = "${uri.fetch}", method = RequestMethod.POST)
    public @ResponseBody String fetch(@RequestBody String body) {
        long l0 = System.currentTimeMillis();
        logger.info("Fetch entered");
        String uid = null;
        delayIfConfigured();
        try {
            uid = getUid(body);
        } catch (InvalidRequest e) {
            return contentCipher.encode(FetchVO.buildError().getBytes());
        }

        logger.info("Start to pull");
        ConcurrentLinkedQueue<byte[]> queue = bufferMap.get(uid);
        if (queue == null) {
            return contentCipher.encode(FetchVO.noContent().getBytes());
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        logger.info("poll0 entered");
        byte[] buffer = queue.poll();
        logger.info("poll0 finished");
        if (buffer == null) {
            logger.info("Nothing to fetch, exit.");
            return contentCipher.encode(FetchVO.noContent().getBytes());
        }
        logger.info("Available bytes {}", buffer.length);
        while (buffer != null) {
            out.write(buffer, 0, buffer.length);
            if (out.size() > 1024 * 1024) {
                break;
            }
            buffer = queue.poll();
        }



        byte[] bytes = out.toByteArray();
        logger.info("Fetch Result = " + bytes.length);
        String encoded = contentCipher.encode(FetchVO.build(bytes).getBytes());
        logger.info("Encoded length = {}", encoded.length());
        logger.info("Fetch returned in {}", (System.currentTimeMillis() - l0));
        return encoded;
    }

    @RequestMapping(path = "${uri.close}", method = RequestMethod.POST)
    public @ResponseBody
    String close(@RequestBody String body) {
        String uid = null;
        try {
            uid = getUid(body);
            logger.info("close got for {}", uid);
            delayIfConfigured();
        } catch (InvalidRequest e) {
            return contentCipher.encode("ERR");
        }


        ConcurrentLinkedQueue queue = bufferMap.remove(uid);
        if (queue != null) {
            queue.clear();
        }
        ResponseHandler worker = channelMap.remove(uid);
        if (worker != null) {
            worker.close();
            logger.info("Worker closed for {}", uid);
        }

        return contentCipher.encode("OK");
    }

    @RequestMapping(path = "${uri.key}", method = RequestMethod.GET)
    public @ResponseBody
    String key() {
        try {
            byte[] key = keyService.generateAESKey();
            this.contentCipher = new SimpleAESCipher(key);
            return keyCipher.encode(key);
        } catch (Exception e) {
            return keyCipher.encode("ERR");
        }
    }

    private String getUid(String body) throws InvalidRequest {
        String uid = null;
        try {
            String json = contentCipher.decode(body);
            JSONObject o = new JSONObject(json);
            uid = o.optString("uid");
            if (uid == null) {
                return contentCipher.encode("ERR");
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
