package org.lff.plainsocks;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lff.BytesCipher;
import org.lff.SimpleAESCipher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;

/**
 * @author Feifei Liu
 * @datetime Aug 01 2017 18:11
 */
public class ContentFetcher implements Runnable{

    private static LongAdder sequence = new LongAdder();

    private Semaphore s = new Semaphore(0);

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private String uid;
    private OutputStream outputStream;
    private AtomicBoolean stopped;
    private Map<Long, byte[]> window = new HashMap<>();
    private final BytesCipher cipher;

    public ContentFetcher(BytesCipher cipher, String uid, OutputStream outputStream, AtomicBoolean stopped) {
        this.uid = uid;
        this.cipher = cipher;
        this.outputStream = outputStream;
        this.stopped = stopped;
    }

    @Override
    public void run() {
        MDC.put("uid", String.valueOf(uid.hashCode()));
        logger.info("Fetcher started.");

        try {
            work();
        } finally {
            logger.info("work finished");
            s.release();
        }
    }

    private void work() {
        int emptyCount = 0;
        int errorCount = 0;
        int stopCount = 0;
        while (emptyCount < 10 && errorCount < 2) {
            if (stopped.get() && stopCount++ > 1) {
                return;
            }
            try {

                JSONObject o = new JSONObject();
                o.put("uid", uid);

                String body = o.toString();

                long l0 = System.currentTimeMillis();
                logger.info("Start to post fetch request");
//                HttpResponse<String> result = Unirest.post("http://localhost:80/h/p")
//                        .body(cipher.encode(body))
//                        .asString();
                String result = SimpleHttpClient.post(RemoteConfig.getFetchURL(), new HashMap<>(), cipher.encode(body));
                logger.info("Finished fetch request in {}", (System.currentTimeMillis() - l0));
                String responseBody = result; //.getBody();
                logger.info("Received fetch len = {}", responseBody.length());
                String json = cipher.decode(responseBody);
                JSONObject jsonObject = new JSONObject(json);
                int status = jsonObject.optInt("status", Integer.MIN_VALUE);
                logger.info("Get status {}", status);
                if (status == -1) {
                    return;
                }
                if (status == -2) {
                    emptyCount++;
                    logger.info("Result is empty {} stopped = {}", emptyCount, stopped.get());
                    try {
                        Thread.sleep(emptyCount * 100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    continue;
                }
                if (status == 0) {
                    emptyCount = 0;
                    String content = jsonObject.getString("content");
                    byte[] buffer = Base64.getDecoder().decode(content);
                    try {
                        outputStream.write(buffer);
                        outputStream.flush();
                        buffer = null;
                    } catch (Exception e) {
                        logger.info("Write to output failed, closed ?");
                    }
                }
            } catch (JSONException e) {
                logger.error("Failed to parse json", e);
                return;
            } catch (Exception e) {
                return;
            }
        }
    }

    public void waitForExit() {
        try {
            s.acquire(1);
        } catch (InterruptedException e) {
        }
    }
}
