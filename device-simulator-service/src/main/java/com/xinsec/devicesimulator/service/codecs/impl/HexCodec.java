package com.xinsec.devicesimulator.service.codecs.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.xinsec.devicesimulator.service.codecs.MessageCodec;
import com.xinsec.devicesimulator.service.core.ComponentType;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

/**
 * 将字节流与十六进制字符串进行相互转换的编解码器。
 */
@ComponentType("hex")
public class HexCodec implements MessageCodec {

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    public static final String HEX_DATA_KEY = "hex_data";

    @Override
    public void configure(JsonNode config) {
        // No configuration needed
    }

    /**
     * 将业务数据Map中的十六进制字符串编码为字节流。
     * @param data 业务数据, 必须包含一个key为"hex_data"的十六进制字符串。
     * @return 编码后的字节数组
     */
    @Override
    public byte[] encode(Map<String, Object> data, Map<String, Object> metadata) {
        if (data != null && data.get(HEX_DATA_KEY) instanceof String) {
            String hexString = (String) data.get(HEX_DATA_KEY);
            return hexToBytes(hexString);
        }
        return new byte[0];
    }

    /**
     * 将收到的字节解码为包含十六进制字符串的Map。
     * @param rawData 收到的原始字节数据
     * @return 一个Map，其中包含key为"hex_data"的十六进制字符串。
     */
    @Override
    public Map<String, Object> decode(byte[] rawData) {
        return Collections.singletonMap(HEX_DATA_KEY, bytesToHex(rawData));
    }

    private static String bytesToHex(byte[] bytes) {
        if (bytes == null) return "";
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    private static byte[] hexToBytes(String s) {
        if (s == null) return new byte[0];
        s = s.replaceAll("\\s", ""); // Remove any whitespace
        int len = s.length();
        if (len % 2 != 0) {
            throw new IllegalArgumentException("Hex string must have an even number of characters: " + s);
        }
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                                 + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}
