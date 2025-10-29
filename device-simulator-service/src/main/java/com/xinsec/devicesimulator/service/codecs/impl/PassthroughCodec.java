package com.xinsec.devicesimulator.service.codecs.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.xinsec.devicesimulator.service.codecs.MessageCodec;
import com.xinsec.devicesimulator.service.core.ComponentType;
import java.util.Collections;
import java.util.Map;

/**
 * 一个直通的“无操作”编解码器。
 * 它不执行任何实际的编码或解码操作。
 */
@ComponentType("passthrough")
public class PassthroughCodec implements MessageCodec {

    @Override
    public void configure(JsonNode config) {
        // 无需配置
    }

    /**
     * 编码时，假定数据已经是以byte[]形式存在于输入Map中。
     * @return data map中 "rawData" key对应的 byte[]
     */
    @Override
    public byte[] encode(Map<String, Object> data, Map<String, Object> metadata) {
        if (data != null && data.get("rawData") instanceof byte[]) {
            return (byte[]) data.get("rawData");
        }
        return new byte[0];
    }

    /**
     * 解码时，只是简单地将原始字节数组包装进一个Map。
     * @return 一个包含 "rawData" key的Map
     */
    @Override
    public Map<String, Object> decode(byte[] rawData) {
        return Collections.singletonMap("rawData", rawData);
    }
}