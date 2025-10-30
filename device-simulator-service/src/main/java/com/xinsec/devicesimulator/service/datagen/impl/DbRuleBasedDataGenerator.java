package com.xinsec.devicesimulator.service.datagen.impl;

import com.xinsec.devicesimulator.service.core.ComponentType;
import com.xinsec.devicesimulator.service.datagen.DataGenerator;
import com.xinsec.devicesimulator.service.entity.PayloadRepositoryEntity;
import com.xinsec.devicesimulator.service.entity.RequestResponseRuleEntity;
import com.xinsec.devicesimulator.service.mapper.PayloadRepositoryMapper;
import com.xinsec.devicesimulator.service.mapper.RequestResponseRuleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * 根据请求内容，从数据库的请求-响应规则中查找响应报文的数据生成器
 */
@ComponentType("db-rule-based")
@RequiredArgsConstructor
@Slf4j
@Scope("prototype") // 确保每次获取都是新实例
public class DbRuleBasedDataGenerator implements DataGenerator {

    private final RequestResponseRuleMapper ruleMapper;
    private final PayloadRepositoryMapper payloadRepositoryMapper;
    private final Random random = new Random(); // 用于随机选择

    @Override
    public String generate(Object context) {
        if (!(context instanceof String)) {
            log.warn("DbRuleBasedDataGenerator 需要一个字符串类型的上下文 (即请求数据).");
            return null;
        }
        String requestData = (String) context;

        // 获取所有规则. 在规则数量很多的情况下, 这里可以优化为增加profileName等查询条件
        List<RequestResponseRuleEntity> allRules = ruleMapper.findAll();

        // 过滤出所有匹配请求关键字的规则
        List<RequestResponseRuleEntity> matchingRules = allRules.stream()
                .filter(rule -> requestData.contains(rule.getRequestKeyword()))
                .collect(Collectors.toList());

        if (matchingRules.isEmpty()) {
            log.warn("未找到与请求: '{}' 匹配的响应规则.", requestData);
            return null; // 或返回默认响应
        }

        // 从匹配的规则中随机选择一个
        RequestResponseRuleEntity selectedRule = matchingRules.get(random.nextInt(matchingRules.size()));
        String responsePayloadKey = selectedRule.getResponsePayload(); // responsePayload现在是payloadKey

        // 根据responsePayloadKey从报文库中获取实际报文内容
        return Optional.ofNullable(payloadRepositoryMapper.findByName(responsePayloadKey))
                .map(PayloadRepositoryEntity::getPayload)
                .orElseGet(() -> {
                    log.warn("规则 '{}' (请求关键字: '{}') 对应的响应报文键 '{}' 在报文库中未找到.",
                            selectedRule.getRuleGroup(), selectedRule.getRequestKeyword(), responsePayloadKey);
                    return null;
                });
    }
}
