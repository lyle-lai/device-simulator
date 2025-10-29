package com.xinsec.devicesimulator.service.codecs;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;

/**
 * 编解码器接口
 * 职责: 在结构化数据(如 Map<String, Object>)和字节流(byte[])之间进行转换。
 */
public interface MessageCodec {

    /**
     * 配置编解码器
     * @param config codec部分的JSON配置
     */
    void configure(JsonNode config);

    /**
     * 将业务数据编码为字节
     * @param data 业务数据
     * @param metadata 元数据，可能包含编码所需的一些额外信息
     * @return 编码后的字节数组
     */
    byte[] encode(Map<String, Object> data, Map<String, Object> metadata);

    /**
     * 将业务数据字符串编码为字节
     * @param data 业务数据字符串
     * @param metadata 元数据，可能包含编码所需的一些额外信息
     * @return 编码后的字节数组
     */
    byte[] encode(String data, Map<String, Object> metadata);

    /**
     * 将收到的字节解码为业务数据
     * @param rawData 收到的原始字节数据
     * @return 解码后的业务数据Map
     */
    Map<String, Object> decode(byte[] rawData);
}