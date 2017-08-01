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

/**
 * @author Feifei Liu
 * @datetime Aug 01 2017 18:11
 */
public class ContentFetcher implements Runnable{

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private String uid;
    private OutputStream outputStream;

    public ContentFetcher(String uid, OutputStream outputStream) {
        this.uid = uid;
        this.outputStream = outputStream;
    }

    @Override
    public void run() {
        while (true) {
            try {

                JSONObject o = new JSONObject();
                o.put("uid", uid);

                String body = o.toString();

                SimpleAESCipher cipher = new SimpleAESCipher();

                HttpResponse<String> result = Unirest.post("http://localhost:80/h/p")
                        .body(cipher.encode(body))
                        .asString();
                logger.info("Result from remote is ", result.getStatus());
                byte[] r = Base64.getDecoder().decode(result.getBody());
                outputStream.write(r);
            } catch (UnirestException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
