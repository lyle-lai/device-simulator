package com.xinsec.devicesimulator.service.datagen.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinsec.devicesimulator.service.core.ComponentType;
import com.xinsec.devicesimulator.service.datagen.DataGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@ComponentType("random")
@Slf4j
@Scope("prototype") // 确保每次获取都是新实例
public class RandomDataGenerator implements DataGenerator {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String generate(Object context) {
        if (!(context instanceof Map)) {
            log.warn("RandomDataGenerator 需要一个包含 'properties' 键的 Map 上下文。");
            return "{}";
        }
        Map<String, Object> contextMap = (Map<String, Object>) context;
        JsonNode propertiesNode = objectMapper.convertValue(contextMap.get("properties"), JsonNode.class);

        if (propertiesNode == null || !propertiesNode.isArray()) {
            log.warn("RandomDataGenerator 的配置中需要一个 'properties' 数组。");
            return "{}";
        }

        List<GeneratorProperty> properties = StreamSupport.stream(propertiesNode.spliterator(), false)
                .map(GeneratorProperty::new)
                .collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        for (GeneratorProperty prop : properties) {
            data.put(prop.key, prop.generateValue());
        }

        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.error("将随机数据序列化为JSON时出错", e);
            return "{}";
        }
    }


    /**
     * 内部类，用于封装单个数据项的生成逻辑
     */
    private static class GeneratorProperty {
        final String key;
        final String dataType;
        final double min;
        final double max;
        final int precision;
        final List<String> values; // 用于枚举类型

        GeneratorProperty(JsonNode node) {
            this.key = node.path("key").asText();
            this.dataType = node.path("dataType").asText("string");
            this.min = node.path("min").asDouble(0);
            this.max = node.path("max").asDouble(100);
            this.precision = node.path("precision").asInt(0);
            if (node.has("values")) {
                this.values = StreamSupport.stream(node.path("values").spliterator(), false)
                        .map(JsonNode::asText)
                        .collect(Collectors.toList());
            } else {
                this.values = null;
            }
        }

        Object generateValue() {
            String lowerCaseDataType = dataType.toLowerCase();
            if ("int".equals(lowerCaseDataType)) {
                return ThreadLocalRandom.current().nextInt((int) min, (int) max + 1);
            } else if ("double".equals(lowerCaseDataType)) {
                double randomValue = ThreadLocalRandom.current().nextDouble(min, max);
                double scale = Math.pow(10, precision);
                return Math.round(randomValue * scale) / scale;
            } else if ("enum".equals(lowerCaseDataType)) {
                if (values == null || values.isEmpty()) {
                    return null;
                }
                return values.get(ThreadLocalRandom.current().nextInt(values.size()));
            } else if ("boolean".equals(lowerCaseDataType)) {
                return ThreadLocalRandom.current().nextBoolean();
            } else {
                return "不支持的数据类型";
            }
        }
    }
}
