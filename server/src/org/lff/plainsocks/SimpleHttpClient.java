package org.lff.plainsocks;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.*;
import java.lang.invoke.MethodHandles;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Feifei Liu
 * @datetime Jun 23 2017 10:29
 */
public class SimpleHttpClient {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    static {
    }

    /**
     * POST request to url.
     * @param headers headers
     * @param body body
     * @return response body, if successful
     */
    public static String post(String httpurl, Map<String, String> headers, String body) throws IOException {
        HttpURLConnection conn = null;

        BufferedWriter bw = null;
        try {

            URL url = new URL(httpurl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");
            conn.setUseCaches(false);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            //Set headers
            for (String key : headers.keySet()) {
                String v = headers.get(key);
                if (v != null) {
                    conn.setRequestProperty(key, headers.get(key));
                }
            }

            conn.connect();

            if (body != null) {
                OutputStream out = conn.getOutputStream();
                out.write(body.getBytes("UTF-8"));
                out.flush();
            }

            handleError(conn);
            // Read response
            String response = readResponse(conn.getInputStream());
            return response.toString();

        } catch (IOException e) {
            throw e;
        } finally {
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException e) {
                    //ignores
                }
            }
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * GET request to url.
     * @param headers headers
     * @return response body, if successful
     */
    public static String get(String httpurl, Map<String, String> headers) throws IOException {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(httpurl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(false);
            conn.setDoInput(true);
            conn.setRequestMethod("GET");
            conn.setUseCaches(false);

            //Set headers
            for (String key : headers.keySet()) {
                String v = headers.get(key);
                if (v != null) {
                    conn.setRequestProperty(key, headers.get(key));
                }
            }

            conn.connect();
            handleError(conn);


            // Read response
            InputStream inputStream = conn.getInputStream();
            String result = readResponse(inputStream);
            return result;
        } catch (IOException e) {
            throw e;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static void handleError(HttpURLConnection conn) throws IOException {
        // Request not successful
        if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
            String error = null;
            try {
                error = readResponse(conn.getErrorStream());
            } catch (IOException e) {
                error = "Failed to read error stream " + e.getMessage();
            }
            throw new IOException("Request Failed. HTTP Error Code: " + conn.getResponseCode() + " error response = " + error);
        }
    }

    private static String readResponse(InputStream inputStream) throws IOException {
        // Read response
        BufferedReader br = null;
        StringBuffer response = new StringBuffer();
        ByteArrayOutputStream bos = new ByteArrayOutputStream(1024 * 1024 * 16);
        byte[] buffer = new byte[64 * 1024];
        try {
            logger.info("Start to read response");
//            br = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 512 * 1024);
//            String line;
//            while ((line = br.readLine()) != null) {
//                logger.info("Read a line {}", line.length());
//                response.append(line);
//            }
            int len = 0;
            while (len != -1) {
                len = inputStream.read(buffer);
               // logger.info("Read {} bytes", len);
                if (len == -1) {
                    break;
                }
                bos.write(buffer, 0, len);
            }
            return new String(bos.toByteArray(), "UTF-8");
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (Exception e) {
                    //ignores
                }
            }
            logger.info("Finished bto read response");
        }
    }

    /**
     * Disables the SSL certificate checking for new instances of {@link HttpsURLConnection} This has been created to
     * aid testing on a local box, not for use on production.
     */
    private static void disableSSLCertificateChecking() {
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                // Not implemented
            }

            public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                // Not implemented
            }


        } };


        // Create all-trusting host name verifier
        HostnameVerifier allHostsValid = new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };

        try {
            SSLContext sc = SSLContext.getInstance("TLS");

            sc.init(null, trustAllCerts, new java.security.SecureRandom());

            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        } catch (Exception e) {
        }
    }

    public static void main(String[] argu) throws IOException {
        long start = System.currentTimeMillis();
        String s = post("http://127.0.0.1/test/f", new HashMap<>(), " ");
        System.out.println(s.length());
        long end = System.currentTimeMillis();
        System.out.println(end - start);
    }
}
