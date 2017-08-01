package org.lff.aiguo;

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
    }
}
