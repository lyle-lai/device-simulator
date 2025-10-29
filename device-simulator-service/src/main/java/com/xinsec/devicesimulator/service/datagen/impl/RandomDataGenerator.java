package com.xinsec.devicesimulator.service.datagen.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.xinsec.devicesimulator.service.core.ComponentType;
import com.xinsec.devicesimulator.service.datagen.DataGenerator;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@ComponentType("random")
@Slf4j
public class RandomDataGenerator implements DataGenerator {

    private List<GeneratorProperty> properties;

    @Override
    public void configure(JsonNode config) {
        if (config != null && config.has("properties")) {
            // 将JSON配置中的每个property项转换成GeneratorProperty对象
            this.properties = StreamSupport.stream(config.get("properties").spliterator(), false)
                    .map(GeneratorProperty::new)
                    .collect(Collectors.toList());
        } else {
            this.properties = Collections.emptyList();
            log.warn("RandomDataGenerator在没有 'properties' 的情况下被配置，将只生成空数据。");
        }
    }

    @Override
    public Map<String, Object> generate() {
        Map<String, Object> data = new HashMap<>();
        for (GeneratorProperty prop : properties) {
            data.put(prop.key, prop.generateValue());
        }
        return data;
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
        final List<String> values; // for enum type

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
                return "unsupported_type";
            }
        }
    }
}
