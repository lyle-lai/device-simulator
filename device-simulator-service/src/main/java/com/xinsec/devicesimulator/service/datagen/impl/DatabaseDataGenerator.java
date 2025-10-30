package com.xinsec.devicesimulator.service.datagen.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinsec.devicesimulator.service.core.ComponentType;
import com.xinsec.devicesimulator.service.datagen.DataGenerator;
import com.xinsec.devicesimulator.service.entity.PayloadRepositoryEntity;
import com.xinsec.devicesimulator.service.mapper.PayloadRepositoryMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

/**
 * 从数据库加载指定报文组中的报文，并随机选择一个进行发送的数据生成器。
 */
@ComponentType("database-generator")
@RequiredArgsConstructor
@Slf4j
@Scope("prototype") // 确保每次获取都是新实例
public class DatabaseDataGenerator implements DataGenerator {
    private final PayloadRepositoryMapper payloadRepositoryMapper;
    private final ObjectMapper objectMapper;
    private final Random random = new Random();

    @Override
    public String generate(Object context) {
        if (!(context instanceof Map)) {
            log.warn("DatabaseDataGenerator 需要一个包含 'groupKey' 的 Map 上下文。");
            return null;
        }

        GeneratorProperty property = objectMapper.convertValue(context, GeneratorProperty.class);
        String groupKey = property.getGroupKey();

        if (!StringUtils.hasText(groupKey)) {
            log.warn("DatabaseDataGenerator 的属性中必须包含 'groupKey'。");
            return null;
        }

        List<PayloadRepositoryEntity> payloads = payloadRepositoryMapper.findByGroupKey(groupKey);

        if (payloads.isEmpty()) {
            log.warn("在数据库中未找到报文分组 '{}'，或者该分组为空。", groupKey);
            return null;
        }

        // 随机选择一个报文
        PayloadRepositoryEntity selectedPayload = payloads.get(random.nextInt(payloads.size()));
        return selectedPayload.getPayload();
    }

    @Data
    public static class GeneratorProperty {
        private String groupKey;
    }
}
