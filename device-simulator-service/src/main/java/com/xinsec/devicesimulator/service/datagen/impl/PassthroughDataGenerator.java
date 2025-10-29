package com.xinsec.devicesimulator.service.datagen.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.xinsec.devicesimulator.service.core.ComponentType;
import com.xinsec.devicesimulator.service.datagen.DataGenerator;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * 一个“透传”的数据生成器，它直接返回上下文中的字符串内容。
 * 主要用于请求-响应模式，其中策略的输入直接作为下一个组件的输入。
 */
@ComponentType("passthrough-generator")
@Scope("prototype") // 确保每次获取都是新实例
public class PassthroughDataGenerator implements DataGenerator {

    /**
     * 直接返回上下文对象（假定它是一个字符串）。
     */
    @Override
    public String generate(Object context) {
        if (context instanceof String) {
            return (String) context;
        }
        return null;
    }
}