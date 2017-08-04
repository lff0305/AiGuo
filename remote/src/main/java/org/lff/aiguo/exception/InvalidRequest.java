package org.lff.aiguo.exception;

/**
 * @author Feifei Liu
 * @datetime Aug 04 2017 11:28
 */
public class InvalidRequest extends Exception {
    private static final long serialVersionUID = 7581142599510356774L;

    public InvalidRequest(String msg) {
        super(msg);
    }

    public InvalidRequest(String msg, Throwable cause) {
        super(msg, cause);
    }
}
