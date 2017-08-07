package org.lff;

/**
 * @author Feifei Liu
 * @datetime Jul 31 2017 16:26
 */
public interface BytesCipher {
    public String encode(String source);
    public String encode(byte[] source);
    public byte[] decodeBytes(String source);
    public String decode(String encoded);
}
