package org.lff.plainsocks;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Feifei Liu
 * @datetime Aug 03 2017 12:11
 */
public class UnirestTest {
    public static void main(String[] argu) throws UnirestException, IOException {
        long l0 = System.currentTimeMillis();
        HttpResponse<String> result = Unirest.post("http://localhost:8080/t.7z")
                .asString();
        System.out.println(result.getRawBody().available());
        long l1 = System.currentTimeMillis();
        System.out.println(l1 - l0);
    }
}
