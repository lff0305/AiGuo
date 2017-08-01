package org.lff.plainsocks;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

/**
 * @author Feifei Liu
 * @datetime Jul 31 2017 18:18
 */
public class Poster {
    public static void main(String[] argu) throws UnirestException {
        HttpResponse<String> result = Unirest.post("http://localhost:8080/h/c")
                .body("{}")
                .asString();
        System.out.println(result.getBody());
    }
}
