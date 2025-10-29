package com.xinsec.devicesimulator.service.strategies;

import com.fasterxml.jackson.databind.JsonNode;
import com.xinsec.devicesimulator.service.codecs.MessageCodec;
import com.xinsec.devicesimulator.service.datagen.DataGenerator;
import com.xinsec.devicesimulator.service.protocols.ProtocolHandler;
import com.xinsec.devicesimulator.service.websocket.WebSocketLogService;

/**
 * 模拟策略/工作流接口
 * 职责: 编排所有组件,执行具体的业务模拟逻辑。
 */
public interface SimulationStrategy {

    /**
     * 配置策略
     * @param config strategy部分的JSON配置
     * @param protocol 协议处理器实例
     * @param codec 编解码器实例
     * @param generator 数据生成器实例
     */
    void configure(JsonNode config, ProtocolHandler protocol, MessageCodec codec, DataGenerator generator, JsonNode generatorProperties, WebSocketLogService logService, String profileName);

    /**
     * 启动策略
     */
    void execute();

    /**
     * 停止策略
     */
    void stop();
}