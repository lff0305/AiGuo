package org.lff.aiguo.vo;

import org.json.JSONObject;

import java.util.Base64;

/**
 * @author Feifei Liu
 * @datetime Aug 04 2017 11:34
 */
public class FetchVO {

    private int status;
    private byte[] content;

    public FetchVO(int status, byte[] content) {
        this.status = status;
        this.content = content;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public FetchVO() {

    }

    public String toJSON() {
        long s0 = System.currentTimeMillis();
        JSONObject o = new JSONObject();
        o.put("status", status);
        o.put("content", Base64.getEncoder().encodeToString(content));
        String s = o.toString();
        long s1 = System.currentTimeMillis();
        System.out.println(">> " + (s1 - s0));
        return s;
    }

    static FetchVO error = new FetchVO(-1, new byte[]{});
    static FetchVO noContent = new FetchVO(-2, new byte[]{});
    public static String buildError() {
        return error.toJSON();
    }

    public static String noContent() {
        return noContent.toJSON();
    }

    public static String build(byte[] bytes) {
        FetchVO vo = new FetchVO();
        vo.setStatus(0);
        vo.setContent(bytes);
        return vo.toJSON();
    }
}
