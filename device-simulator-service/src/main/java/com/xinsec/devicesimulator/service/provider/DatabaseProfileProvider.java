package com.xinsec.devicesimulator.service.provider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper; // Import QueryWrapper
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
import java.util.Objects;
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

            // 过滤掉转换失败的画像
            return entities.stream()
                    .map(this::convertToSimulationProfile)
                    .filter(Objects::nonNull) // 过滤掉转换失败的画像
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("从数据库加载模拟画像时发生错误。", e);
            return Collections.emptyList();
        }
    }

    @Override
    public void saveProfile(SimulationProfile profile) {
        if (profile == null || profile.getProfileName() == null || profile.getProfileName().trim().isEmpty()) {
            throw new IllegalArgumentException("画像和画像名称不能为空。");
        }

        try {
            // 将 SimulationProfile 转换为 SimulationProfileEntity
            SimulationProfileEntity entity = convertToSimulationProfileEntity(profile);

            // 检查画像是否已存在
            QueryWrapper<SimulationProfileEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("profile_name", profile.getProfileName());
            SimulationProfileEntity existingEntity = profileMapper.selectOne(queryWrapper);

            if (existingEntity != null) {
                // 更新现有画像
                entity.setId(existingEntity.getId()); // 确保更新的是同一个记录
                profileMapper.updateById(entity);
                log.info("成功更新数据库中的画像: {}", profile.getProfileName());
            } else {
                // 插入新画像
                profileMapper.insert(entity);
                log.info("成功保存新画像到数据库: {}", profile.getProfileName());
            }
        } catch (JsonProcessingException e) {
            log.error("保存画像 '{}' 时，JSON序列化失败。", profile.getProfileName(), e);
            throw new RuntimeException("保存画像时JSON处理失败。", e);
        } catch (Exception e) {
            log.error("保存画像 '{}' 到数据库时发生错误。", profile.getProfileName(), e);
            throw new RuntimeException("保存画像到数据库失败。", e);
        }
    }

    @Override
    public void deleteProfile(String profileName) {
        if (profileName == null || profileName.trim().isEmpty()) {
            throw new IllegalArgumentException("画像名称不能为空。");
        }
        QueryWrapper<SimulationProfileEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("profile_name", profileName);
        int deletedRows = profileMapper.delete(queryWrapper);
        if (deletedRows > 0) {
            log.info("成功从数据库删除画像: {}", profileName);
        } else {
            log.warn("尝试删除不存在的数据库画像: {}", profileName);
        }
    }

    /**
     * 将 SimulationProfileEntity 转换为 SimulationProfile 配置对象
     */
    private SimulationProfile convertToSimulationProfile(SimulationProfileEntity entity) {
        try {
            // 将 profileConfig 字段的JSON字符串反序列化为 SimulationProfile 对象
            SimulationProfile profile = objectMapper.readValue(entity.getProfileConfig(), SimulationProfile.class);
            // 使用数据库中的profileName覆盖JSON中的值，以数据库为准
            profile.setProfileName(entity.getProfileName());
            return profile;
        } catch (IOException e) {
            log.error("解析数据库中画像 '{}' 的配置时失败。", entity.getProfileName(), e);
            return null;
        }
    }

    /**
     * 将 SimulationProfile 转换为 SimulationProfileEntity
     */
    private SimulationProfileEntity convertToSimulationProfileEntity(SimulationProfile profile) throws JsonProcessingException {
        SimulationProfileEntity entity = new SimulationProfileEntity();
        entity.setProfileName(profile.getProfileName());
        // 将整个 profile 对象序列化为 JSON 字符串存储
        entity.setProfileConfig(objectMapper.writeValueAsString(profile));
        return entity;
    }
}
