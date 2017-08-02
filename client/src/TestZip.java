
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipOutputStream;

/**
 * @author Feifei Liu
 * @datetime Aug 02 2017 15:56
 */
public class TestZip {
    public static void main(String[] argu) throws IOException {
        String s = "213421342AAAAAAAAAAAAAAAAA142134R0VUIC8gSFRUUC8xLjENCkhvc3Q6IGxvY2FsaG9zdDo4MDgwDQpVc2VyLUFnZW50OiBjdXJsLzcuNTQuMQ0KQWNjZXB0OiAqLyoNCg0K";
        byte[] b = Base64.getDecoder().decode(s);
        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        DeflaterOutputStream os = new DeflaterOutputStream(bs);
        os.write(b);
        os.close();
        System.out.println(s);
        System.out.println(Base64.getEncoder().encodeToString(bs.toByteArray()));

        System.out.println(b.length + " " + bs.toByteArray().length);
    }
}
