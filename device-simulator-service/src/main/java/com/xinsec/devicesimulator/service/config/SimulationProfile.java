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
    private Boolean enabled; // Changed to Boolean wrapper class
    private Device device;
    private JsonNode protocol;
    private JsonNode codec;
    private JsonNode strategy;
    private JsonNode dataGenerator;

    // Custom getter to provide a default value if 'enabled' is null after deserialization
    public Boolean getEnabled() {
        return enabled != null ? enabled : true; // Default to true if not explicitly set
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Device {
        private String id;
        private String description;
    }
}