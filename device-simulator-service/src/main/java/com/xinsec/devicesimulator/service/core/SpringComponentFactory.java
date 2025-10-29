package com.xinsec.devicesimulator.service.core;

import com.xinsec.devicesimulator.service.codecs.MessageCodec;
import com.xinsec.devicesimulator.service.datagen.DataGenerator;
import com.xinsec.devicesimulator.service.protocols.ProtocolHandler;
import com.xinsec.devicesimulator.service.strategies.SimulationStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SpringComponentFactory implements ComponentFactory {

    private final ApplicationContext context;

    @Override
    public ProtocolHandler createProtocolHandler(String type) {
        return findComponent(ProtocolHandler.class, type);
    }

    @Override
    public MessageCodec createMessageCodec(String type) {
        return findComponent(MessageCodec.class, type);
    }

    @Override
    public DataGenerator createDataGenerator(String type) {
        return findComponent(DataGenerator.class, type);
    }

    @Override
    public SimulationStrategy createSimulationStrategy(String type) {
        return findComponent(SimulationStrategy.class, type);
    }

    private <T> T findComponent(Class<T> componentClass, String type) {
        // 从Spring容器中获取所有实现了该接口的Bean
        Map<String, T> beans = context.getBeansOfType(componentClass);
        
        // 遍历这些Bean，找到那个带有匹配的@ComponentType注解的实例
        Optional<T> component = beans.values().stream()
                .filter(bean -> {
                    // 处理Spring AOP代理, 确保能拿到原始类上的注解
                    Class<?> targetClass = org.springframework.aop.support.AopUtils.getTargetClass(bean);
                    ComponentType annotation = targetClass.getAnnotation(ComponentType.class);
                    return annotation != null && annotation.value().equalsIgnoreCase(type);
                })
                .findFirst();

        // 如果找不到，就抛出一个明确的异常，方便排查问题
        return component.orElseThrow(() -> new IllegalArgumentException(
                "找不到类型为 '" + type + "' 的 " + componentClass.getSimpleName() + " 实现。"
                + "请确认实现类已添加 @Component 和 @ComponentType(\"" + type + "\") 注解。"));
    }
}