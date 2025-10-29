package com.xinsec.devicesimulator.service.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinsec.devicesimulator.service.config.SimulationProfile;
import com.xinsec.devicesimulator.service.entity.SimulationProfileEntity;
import com.xinsec.devicesimulator.service.mapper.SimulationProfileMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据库配置提供者，从数据库加载模拟画像配置
 */
@Component("database")
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "simulator.config.provider", havingValue = "database")
public class DatabaseProfileProvider implements ProfileProvider {

    private final SimulationProfileMapper profileMapper;
    private final ObjectMapper objectMapper;

    @Override
    public List<SimulationProfile> loadProfiles() {
        try {
            // 从数据库查询所有模拟画像实体
            List<SimulationProfileEntity> entities = profileMapper.selectList(null);
            log.info("从数据库加载到 {} 个模拟画像实体。", entities.size());

            // 过滤出启用的画像，并转换为SimulationProfile对象
            return entities.stream()
                    .filter(SimulationProfileEntity::getIsEnabled) // 只加载启用的画像
                    .map(this::convertToSimulationProfile)
                    .filter(java.util.Objects::nonNull) // 过滤掉转换失败的画像
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("从数据库加载模拟画像时发生错误。", e);
            return Collections.emptyList();
        }
    }

    /**
     * 将 SimulationProfileEntity 转换为 SimulationProfile 配置对象
     */
    private SimulationProfile convertToSimulationProfile(SimulationProfileEntity entity) {
        try {
            // 将 profileConfig 字段的JSON字符串反序列化为 SimulationProfile 对象
            SimulationProfile profile = objectMapper.readValue(entity.getProfileConfig(), SimulationProfile.class);
            // 使用数据库中的enabled和profileName覆盖JSON中的值，以数据库为准
            profile.setEnabled(entity.getIsEnabled());
            profile.setProfileName(entity.getProfileName());
            return profile;
        } catch (IOException e) {
            log.error("解析数据库中画像 '{}' 的配置时失败。", entity.getProfileName(), e);
            return null;
        }
    }
}
