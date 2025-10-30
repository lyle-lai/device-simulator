package com.xinsec.devicesimulator.service.config;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimulationProfile {
    private String profileName;
    private Boolean enabled; // 改为布尔包装类，以区分 ‘未设置’ 和 ‘false’
    private Device device;
    private JsonNode protocol;
    private JsonNode codec;
    private JsonNode strategy;
    private JsonNode dataGenerator;

    // 自定义getter方法，用于在反序列化时如果'enabled'字段为null，则提供一个默认值
    public Boolean getEnabled() {
        return enabled != null ? enabled : true; // 如果未明确设置，则默认为 true
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Device {
        private String id;
        private String description;
    }
}