package com.s1.dbs.service.creditcard;

import com.s1.arch.exception.S1Exception;
import com.s1.bnk.util.Logger;
import com.s1.dbs.service.creditcard.exceptions.AuthException;
import com.s1.dbs.service.creditcard.exceptions.HTTPException;
import org.grnds.facility.config.GrndsConfiguration;
import org.grnds.facility.config.GrndsConfigurationEnvironment;

import javax.net.ssl.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;

/**
 * @author Feifei Liu
 * @datetime Jun 23 2017 10:29
 */
public class SimpleHttpClient {

    static {
        String key = "cardservice.disableSSL";
        GrndsConfigurationEnvironment ep = GrndsConfiguration.getInstance().getEnvironment("ep");
        String value = ep.getProperty(key);
        Logger.info("Get cardservice.disableSSL = " + key);
        if ("true".equalsIgnoreCase(value)) {
            disableSSLCertificateChecking();
            Logger.info("SSL Certificate checking disabled.");
        }
    }

    /**
     * POST request to url.
     * @param header headers
     * @param body body
     * @return response body, if successful
     */
    public static String post(String httpurl, Map<String, String> headers, String body) throws HTTPException {
        HttpURLConnection conn = null;

        BufferedWriter bw = null;
        try {

            URL url = new URL(httpurl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");
            conn.setUseCaches(false);

            //Set headers
            for (String key : headers.keySet()) {
                String v = headers.get(key);
                if (v != null) {
                    conn.setRequestProperty(key, headers.get(key));
                }
            }

            conn.connect();

            if (body != null) {
                bw = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()));
                bw.write(body);
                bw.flush();
                bw.close();
            }

            handleError(conn);
            // Read response
            String response = readResponse(conn.getInputStream());
            return response.toString();

        } catch (IOException e) {
            throw new HTTPException("IOException", e);
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
     * @param header headers
     * @return response body, if successful
     */
    public static String get(String httpurl, Map<String, String> headers) throws HTTPException {
        HttpURLConnection conn = null;
        try {
            Logger.info("Start to request " + httpurl + " headers : " + headers);
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
            Logger.info("Response got " + result);
            return result;
        } catch (IOException e) {
            throw new HTTPException("IOException", e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static void handleError(HttpURLConnection conn) throws IOException, HTTPException {
        // Request not successful
        if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
            String error = null;
            try {
                error = readResponse(conn.getErrorStream());
            } catch (IOException e) {
                error = "Failed to read error stream " + e.getMessage();
            }
            throw new HTTPException("Request Failed. HTTP Error Code: " + conn.getResponseCode() + " error response = " + error);
        }
    }

    private static String readResponse(InputStream inputStream) throws IOException {
        // Read response
        BufferedReader br = null;
        StringBuffer response = new StringBuffer();
        try {
            br = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (Exception e) {
                    //ignores
                }
            }
        }

        return response.toString();
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
            Logger.error("Failed to disable ssl certificate checking", e);
        }
    }
}
