package com.xinsec.devicesimulator.service.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.xinsec.devicesimulator.service.codecs.MessageCodec;
import com.xinsec.devicesimulator.service.config.SimulationProfile;
import com.xinsec.devicesimulator.service.datagen.DataGenerator;
import com.xinsec.devicesimulator.service.protocols.ProtocolHandler;
import com.xinsec.devicesimulator.service.strategies.SimulationStrategy;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SimulationInstance implements Runnable {

    private final SimulationProfile profile;
    private final SimulationStrategy strategy;

    public SimulationInstance(SimulationProfile profile, ComponentFactory factory) {
        this.profile = profile;

        // 从配置中提取各层级的JsonNode
        JsonNode protocolNode = profile.getProtocol();
        JsonNode codecNode = profile.getCodec();
        JsonNode strategyNode = profile.getStrategy();
        JsonNode generatorNode = strategyNode != null && strategyNode.has("dataGenerator") ? strategyNode.get("dataGenerator") : null;

        // 1. 使用工厂创建组件
        ProtocolHandler protocolHandler = factory.createProtocolHandler(getType(protocolNode));
        MessageCodec messageCodec = factory.createMessageCodec(getType(codecNode));
        this.strategy = factory.createSimulationStrategy(getType(strategyNode));

        // 根据配置创建DataGenerator，如果未定义，则使用一个无操作的默认实现
        DataGenerator dataGenerator;
        if (generatorNode != null) {
            // 如果画像中定义了dataGenerator，则创建指定的实例
            dataGenerator = factory.createDataGenerator(getType(generatorNode));
        } else {
            // 如果未定义，则创建一个默认的、无操作的生成器，避免空指针
            log.debug("No dataGenerator defined in profile, creating default 'passthrough-datagen' generator.");
            dataGenerator = factory.createDataGenerator("passthrough-datagen");
        }

        // 2. 配置组件
        protocolHandler.configure(protocolNode);
        messageCodec.configure(codecNode);
        if (generatorNode != null) {
            dataGenerator.configure(generatorNode);
        }

        // 3. 配置主策略，并将其他组件注入
        this.strategy.configure(strategyNode, protocolHandler, messageCodec, dataGenerator);
    }

    private String getType(JsonNode node) {
        // 如果节点为空或没有type属性，返回一个无操作的"passthrough"类型，增强鲁棒性
        if (node == null || node.get("type") == null || !node.get("type").isTextual()) {
            return "passthrough";
        }
        return node.get("type").asText();
    }

    @Override
    public void run() {
        log.info("启动模拟实例: {}", profile.getProfileName());
        try {
            strategy.execute();
            log.info("模拟实例 '{}' 已成功启动。", profile.getProfileName());
        } catch (Exception e) {
            log.error("启动模拟实例 '{}' 时发生错误。", profile.getProfileName(), e);
        }
    }

    public void stop() {
        log.info("正在停止模拟实例: {}", profile.getProfileName());
        try {
            strategy.stop();
            log.info("模拟实例 '{}' 已成功停止。", profile.getProfileName());
        } catch (Exception e) {
            log.error("停止模拟实例 '{}' 时发生错误。", profile.getProfileName(), e);
        }
    }

    public String getProfileName() {
        return profile.getProfileName();
    }
}