package com.xinsec.devicesimulator.service.codecs.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinsec.devicesimulator.service.codecs.MessageCodec;
import com.xinsec.devicesimulator.service.core.ComponentType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

@ComponentType("json")
@Slf4j
@RequiredArgsConstructor
public class JsonCodec implements MessageCodec {

    private final ObjectMapper objectMapper;
    private Charset charset;

    @Override
    public void configure(JsonNode config) {
        // 从配置中读取字符集，默认为UTF-8
        this.charset = Charset.forName(config.path("charset").asText("UTF-8"));
    }

    @Override
    public byte[] encode(Map<String, Object> data, Map<String, Object> metadata) {
        try {
            String jsonString = objectMapper.writeValueAsString(data);
            return jsonString.getBytes(charset);
        } catch (JsonProcessingException e) {
            log.error("将Map序列化为JSON时失败", e);
            return new byte[0];
        }
    }

    @Override
    public Map<String, Object> decode(byte[] rawData) {
        try {
            // 使用TypeReference可以正确地将JSON反序列化为Map<String, Object>
            return objectMapper.readValue(rawData, new TypeReference<Map<String, Object>>() {});
        } catch (IOException e) {
            log.error("将JSON字节反序列化为Map时失败", e);
            return Collections.emptyMap();
        }
    }
}
