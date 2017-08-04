package org.lff.aiguo;

import org.json.JSONObject;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

/**
 * @author Feifei Liu
 * @datetime Aug 01 2017 18:50
 */
public class PipeTest {
    public static void main(String[] argu) throws IOException {
        PipedOutputStream out = new PipedOutputStream();
        PipedInputStream pin = new PipedInputStream(out);

        out.write("Aaaaaaaaaaaa".getBytes());
       // int d = pin.read();
       // System.out.println(d);

        System.out.println(pin.available());

        byte[] b = new byte[]{(byte)0xff};

        JSONObject o = new JSONObject();
        o.put("a", b);

        System.out.println(o.toString());

        JSONObject o1 = new JSONObject(o.toString());

        System.out.println(o1.getJSONArray("a"));

        byte b1 = (byte)o1.getJSONArray("a").getInt(0);
        System.out.println(b1);
    }
}
