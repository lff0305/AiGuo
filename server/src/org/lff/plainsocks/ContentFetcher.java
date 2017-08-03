package org.lff.plainsocks;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONObject;
import org.lff.SimpleAESCipher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Feifei Liu
 * @datetime Aug 01 2017 18:11
 */
public class ContentFetcher implements Runnable{

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private String uid;
    private OutputStream outputStream;
    private AtomicBoolean stopped;

    public ContentFetcher(String uid, OutputStream outputStream, AtomicBoolean stopped) {
        this.uid = uid;
        this.outputStream = outputStream;
        this.stopped = stopped;
    }

    @Override
    public void run() {

        logger.info("Fetcher started.");

        int emptyCount = 0;
        while (!stopped.get() && emptyCount < 3) {
            try {

                JSONObject o = new JSONObject();
                o.put("uid", uid);

                String body = o.toString();

                SimpleAESCipher cipher = new SimpleAESCipher();

                logger.info("Start to post fetch request");
                HttpResponse<String> result = Unirest.post("http://localhost:80/h/p")
                        .body(cipher.encode(body))
                        .asString();
                logger.info("Finished fetch request");
                String responseBody = result.getBody();
                if (responseBody.isEmpty()) {
                    emptyCount ++ ;
                    logger.info("Result is empty {}", emptyCount);
                    try {
                        Thread.sleep(emptyCount * 500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    continue;
                } else {
                    emptyCount = 0;
                }
                logger.info("Received fetch len = {}", responseBody.length());
                byte[] r = Base64.getDecoder().decode(responseBody);
                outputStream.write(r);
            } catch (UnirestException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
