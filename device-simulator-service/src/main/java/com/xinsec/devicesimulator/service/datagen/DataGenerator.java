package com.xinsec.devicesimulator.service.datagen;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;

/**
 * 数据生成器接口
 * 职责: 创建模拟的业务数据。
 */
public interface DataGenerator {

    /**
     * 生成数据.
     *
     * @param context 上下文对象，可以包含任何需要的数据，例如:
     *                - 对于请求-响应模式，它可以是请求报文 (String).
     *                - 对于需要参数的数据生成器，它可以是属性的Map (Map<String, Object>).
     * @return 生成的报文内容 (String).
     */
    String generate(Object context);
}