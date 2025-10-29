package com.xinsec.devicesimulator.service.provider;

import com.xinsec.devicesimulator.service.config.SimulationProfile;
import java.util.List;

/**
 * 配置提供者接口
 * 职责: 负责加载一个或多个模拟画像配置。这是连接核心引擎和配置源的桥梁。
 */
public interface ProfileProvider {

    /**
     * 加载所有可用的模拟画像配置
     * @return 配置对象列表
     */
    List<SimulationProfile> loadProfiles();
}