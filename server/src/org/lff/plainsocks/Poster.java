package org.lff.plainsocks;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.lff.SimpleAESCipher;

/**
 * @author Feifei Liu
 * @datetime Jul 31 2017 18:18
 */
public class Poster {
    public static void main(String[] argu) throws UnirestException {
        String s = "{\"port\":8080,\"atyp\":1,\"dist\":\"fwAAAQ==\",\"buffer\":\"R0VUIC8gSFRUUC8xLjENCkhvc3Q6IGxvY2FsaG9zdDo4MDgwDQpVc2VyLUFnZW50OiBjdXJsLzcuNTQuMQ0KQWNjZXB0OiAqLyoNCg0K\"}\n";
        SimpleAESCipher cipher = new SimpleAESCipher();

        String body = cipher.encode(s);

        HttpResponse<String> result = Unirest.post("http://localhost:80/h/c")
                .body(body)
                .asString();
        System.out.println(result.getBody());
    }
}
