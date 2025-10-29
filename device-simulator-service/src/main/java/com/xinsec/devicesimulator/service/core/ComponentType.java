package com.xinsec.devicesimulator.service.core;

import org.springframework.stereotype.Component;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义注解, 用于标记组件实现类对应的配置类型(type)
 * 例如: @ComponentType("tcp-client")
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component // 让Spring扫描到所有带此注解的类
public @interface ComponentType {
    String value();
}