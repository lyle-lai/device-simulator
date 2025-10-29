package com.xinsec.devicesimulator.service.datagen;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;

/**
 * 数据生成器接口
 * 职责: 创建模拟的业务数据。
 */
public interface DataGenerator {

    /**
     * 配置数据生成器
     * @param config dataGenerator部分的JSON配置
     */
    void configure(JsonNode config);

    /**
     * 生成一组模拟数据
     * @return 代表业务数据的Map
     */
    Map<String, Object> generate();
}