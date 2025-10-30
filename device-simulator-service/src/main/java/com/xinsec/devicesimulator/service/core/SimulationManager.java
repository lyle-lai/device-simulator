package com.xinsec.devicesimulator.service.core;

import com.xinsec.devicesimulator.service.config.SimulationProfile;
import com.xinsec.devicesimulator.service.provider.ProfileProvider;
import com.xinsec.devicesimulator.service.websocket.WebSocketLogService;
import javax.annotation.PreDestroy;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * 模拟器核心管理器。
 * <p>
 * 该服务是整个模拟器应用的中心枢纽，负责管理所有模拟画像 ({@link SimulationProfile}) 的生命周期
 * 和所有模拟实例 ({@link SimulationInstance}) 的运行时状态。
 * </p>
 * 主要职责包括：
 * <ul>
 *     <li>在应用启动时，通过注入的 {@link ProfileProvider} 加载所有可用的模拟画像。</li>
 *     <li>根据配置，自动启动所有被标记为“已启用”的画像。</li>
 *     <li>提供启动、停止、重启单个模拟实例的接口。</li>
 *     <li>管理画像的CRUD（创建、读取、更新、删除）操作，并确保缓存同步。</li>
 *     <li>维护一个正在运行的模拟实例的列表，并跟踪它们的状态。</li>
 *     <li>在应用关闭时，优雅地停止所有正在运行的模拟任务并释放资源。</li>
 * </ul>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SimulationManager implements ApplicationListener<ContextRefreshedEvent> {

    private final ProfileProvider profileProvider;
    private final ComponentFactory componentFactory;
    private final WebSocketLogService webSocketLogService; // WebSocket日志服务
    private final Map<String, SimulationInstance> runningSimulations = new ConcurrentHashMap<>();
    private final Map<String, SimulationProfile> availableProfiles = new ConcurrentHashMap<>();
    private ExecutorService executorService;

    /**
     * 用于表示画像状态的数据传输对象，结合了 SimulationProfile 和其实时运行状态。
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SimulationProfileStatus {
        private SimulationProfile profile;
        private String status; // 运行状态，例如 "RUNNING", "STOPPED", "NOT_STARTED", "ERROR"
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        log.info("Spring上下文刷新完毕，初始化SimulationManager...");
        executorService = Executors.newCachedThreadPool();
        loadAllProfiles();
        startAllEnabledProfiles();
    }

    public void loadAllProfiles() {
        List<SimulationProfile> profiles = profileProvider.loadProfiles();
        availableProfiles.clear();
        profiles.forEach(profile -> availableProfiles.put(profile.getProfileName(), profile));
        log.info("已加载 {} 个模拟画像配置文件。", availableProfiles.size());
    }

    public Collection<SimulationProfile> getAvailableProfiles() {
        return availableProfiles.values();
    }

    public SimulationProfile getProfile(String profileName) {
        return availableProfiles.get(profileName);
    }

    public Collection<SimulationInstance> getRunningSimulations() {
        return runningSimulations.values();
    }

    /**
     * 获取所有画像及其当前状态。
     * @return 包含所有画像及其状态的集合。
     */
    public Collection<SimulationProfileStatus> getAllProfileStatuses() {
        return availableProfiles.values().stream()
                .map(profile -> {
                    String status = "NOT_STARTED";
                    if (runningSimulations.containsKey(profile.getProfileName())) {
                        status = "RUNNING";
                    }
                    // 可以根据需要添加更多状态，例如 ERROR
                    return new SimulationProfileStatus(profile, status);
                })
                .collect(Collectors.toList());
    }

    public SimulationInstance startSimulation(String profileName) {
        SimulationProfile profile = availableProfiles.get(profileName);
        if (profile == null) {
            throw new IllegalArgumentException("找不到模拟画像: " + profileName);
        }
        if (runningSimulations.containsKey(profileName)) {
            log.warn("模拟任务 '{}' 已在运行中。", profileName);
            return runningSimulations.get(profileName);
        }

        // 只有当画像被标记为启用时才允许启动
        if (!profile.getEnabled()) {
            throw new IllegalStateException("模拟画像 '" + profileName + "' 未被启用。");
        }

        try {
            log.info("为画像 '{}' 创建并启动模拟实例。", profileName);
            SimulationInstance instance = new SimulationInstance(profile, componentFactory, webSocketLogService);
            runningSimulations.put(profileName, instance);
            executorService.submit(instance);
            return instance;
        } catch (Exception e) {
            log.error("为画像 '{}' 创建或启动模拟时失败。", profileName, e);
            throw new RuntimeException("启动模拟任务失败: " + profileName, e);
        }
    }

    public void stopSimulation(String profileName) {
        SimulationInstance instance = runningSimulations.remove(profileName);
        if (instance != null) {
            log.info("正在停止模拟实例 '{}'。", profileName);
            instance.stop();
        } else {
            log.warn("模拟任务 '{}' 未在运行中。", profileName);
        }
    }

    public SimulationInstance restartSimulation(String profileName) {
        stopSimulation(profileName);
        return startSimulation(profileName);
    }

    public void saveProfile(SimulationProfile profile) {
        if (profile == null || profile.getProfileName() == null || profile.getProfileName().trim().isEmpty()) {
            throw new IllegalArgumentException("画像和画像名称不能为空。");
        }
        profileProvider.saveProfile(profile);
        loadAllProfiles();
        log.info("画像 '{}' 已保存，并已重新加载缓存。", profile.getProfileName());
    }

    public void deleteProfile(String profileName) {
        if (profileName == null || profileName.trim().isEmpty()) {
            throw new IllegalArgumentException("画像名称不能为空。");
        }
        if (runningSimulations.containsKey(profileName)) {
            throw new IllegalStateException("无法删除一个正在运行的模拟实例所对应的画像。请先停止该实例。");
        }
        profileProvider.deleteProfile(profileName);
        loadAllProfiles();
        log.info("画像 '{}' 已删除，并已重新加载缓存。", profileName);
    }

    @PreDestroy
    public void shutdown() {
        log.info("正在关闭所有模拟任务...");
        runningSimulations.values().forEach(SimulationInstance::stop);
        if (executorService != null) {
            executorService.shutdownNow();
        }
        runningSimulations.clear();
        availableProfiles.clear();
        log.info("所有模拟任务已停止，资源已释放。");
    }

    private void startAllEnabledProfiles() {
        availableProfiles.values().stream()
            .filter(SimulationProfile::getEnabled)
            .forEach(profile -> {
                try {
                    startSimulation(profile.getProfileName());
                } catch (Exception e) {
                    log.error("启动时自动执行已启用的画像 '{}' 失败。", profile.getProfileName(), e);
                }
            });
    }
}