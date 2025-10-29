package com.xinsec.devicesimulator.service.datagen.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.xinsec.devicesimulator.service.core.ComponentType;
import com.xinsec.devicesimulator.service.datagen.DataGenerator;

import java.util.Collections;
import java.util.Map;

/**
 * 一个“无操作”的数据生成器。
 */
@ComponentType("passthrough-datagen")
public class PassthroughDataGenerator implements DataGenerator {
    @Override
    public void configure(JsonNode config) {
        // 无需配置
    }

    /**
     * 不生成任何数据，直接返回一个空Map。
     */
    @Override
    public Map<String, Object> generate() {
        return Collections.emptyMap();
    }
}