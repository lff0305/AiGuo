package org.lff.aiguo.vo;

import org.json.JSONObject;

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
        JSONObject o = new JSONObject();
        o.put("status", status);
        o.put("content", content);
        return o.toString();
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
