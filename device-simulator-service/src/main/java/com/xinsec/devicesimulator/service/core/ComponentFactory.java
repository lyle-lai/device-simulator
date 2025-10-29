package com.xinsec.devicesimulator.service.core;

import com.xinsec.devicesimulator.service.codecs.MessageCodec;
import com.xinsec.devicesimulator.service.datagen.DataGenerator;
import com.xinsec.devicesimulator.service.protocols.ProtocolHandler;
import com.xinsec.devicesimulator.service.strategies.SimulationStrategy;

/**
 * 组件工厂接口
 * 职责: 动态创建协议、编解码器、数据生成器和策略的实例。
 */
public interface ComponentFactory {

    /**
     * 根据类型创建协议处理器
     * @param type 配置中指定的type, e.g., "tcp-client"
     * @return ProtocolHandler的实例
     */
    ProtocolHandler createProtocolHandler(String type);

    /**
     * 根据类型创建编解码器
     * @param type 配置中指定的type, e.g., "hl7", "json"
     * @return MessageCodec的实例
     */
    MessageCodec createMessageCodec(String type);

    /**
     * 根据类型创建数据生成器
     * @param type 配置中指定的type, e.g., "random"
     * @return DataGenerator的实例
     */
    DataGenerator createDataGenerator(String type);

    /**
     * 根据类型创建模拟策略
     * @param type 配置中指定的type, e.g., "periodic-push"
     * @return SimulationStrategy的实例
     */
    SimulationStrategy createSimulationStrategy(String type);
}
