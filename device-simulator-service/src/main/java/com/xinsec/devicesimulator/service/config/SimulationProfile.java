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
    private boolean enabled;
    private Device device;
    private JsonNode protocol;
    private JsonNode codec;
    private JsonNode strategy;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Device {
        private String id;
        private String description;
    }
}