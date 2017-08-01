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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Feifei Liu
 * @datetime Jul 31 2017 16:50
 */
@Controller
@RequestMapping("/h")
public class ItemController {

    private Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static ExecutorService pool = Executors.newFixedThreadPool(256);

    private SimpleAESCipher cipher = new SimpleAESCipher();

    @RequestMapping(path = "/c", method = RequestMethod.POST)
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
        String strBuffer = o.optString("buffer");
        if (strBuffer != null) {
            buffer = Base64.getDecoder().decode(strBuffer);
        }
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
        worker.setSoTimeout(10000);
        worker.setKeepAlive(false);
        worker.connect(new InetSocketAddress(address, port));

        try {
            if (buffer != null) {
                logger.info("Writting {}" , new String(buffer));
                worker.getOutputStream().write(buffer, 0, buffer.length);
            }
        } catch (IOException e) {

        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        InputStream s = null;
        try {
            s = worker.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        int len = 0;
        byte[] received = new byte[1024 * 32];
        while (len != -1) {
            try {
                len = s.read(received);
                if (len > 0) {
                    logger.info("Real server response with {} bytes of data", len);
                    outputStream.write(received, 0, len);
                }
                outputStream.flush();
            } catch (IOException e) {
                break;
            }
        }
        logger.info("Worker finished.");
        logger.info("Result = {}", new String(outputStream.toByteArray()));

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }

    @RequestMapping(path = "/g", method = RequestMethod.POST)
    public @ResponseBody
    String get(@RequestBody String body) {
        return "";
    }

    @RequestMapping(path = "/cl", method = RequestMethod.POST)
    public @ResponseBody
    String close(@RequestBody String body) {
        return "";
    }
}
