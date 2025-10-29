package com.xinsec.devicesimulator.service.core;

import com.xinsec.devicesimulator.service.config.SimulationProfile;
import com.xinsec.devicesimulator.service.provider.ProfileProvider;
import com.xinsec.devicesimulator.service.websocket.WebSocketLogService; // Import new service
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

@Service
@Slf4j
@RequiredArgsConstructor
public class SimulationManager implements ApplicationListener<ContextRefreshedEvent> {

    private final ProfileProvider profileProvider;
    private final ComponentFactory componentFactory;
    private final WebSocketLogService webSocketLogService;
    private final Map<String, SimulationInstance> runningSimulations = new ConcurrentHashMap<>();
    private final Map<String, SimulationProfile> availableProfiles = new ConcurrentHashMap<>();
    private ExecutorService executorService;

    /**
     * DTO for profile status, combining SimulationProfile with runtime status.
     */
    @Data // Lombok annotation for getters, setters, etc.
    @NoArgsConstructor // Lombok annotation for no-arg constructor
    @AllArgsConstructor // Lombok annotation for all-args constructor
    public static class SimulationProfileStatus {
        private SimulationProfile profile;
        private String status; // e.g., "RUNNING", "STOPPED", "NOT_STARTED", "ERROR"
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
            throw new IllegalArgumentException("Simulation profile not found: " + profileName);
        }
        if (runningSimulations.containsKey(profileName)) {
            log.warn("Simulation '{}' is already running.", profileName);
            return runningSimulations.get(profileName);
        }

        // 只有当画像被标记为启用时才允许启动
        if (!profile.getEnabled()) {
            throw new IllegalStateException("Simulation profile '" + profileName + "' is not enabled.");
        }

        try {
            log.info("为画像 '{}' 创建并启动模拟实例。", profileName);
            SimulationInstance instance = new SimulationInstance(profile, componentFactory, webSocketLogService);
            runningSimulations.put(profileName, instance);
            executorService.submit(instance);
            return instance;
        } catch (Exception e) {
            log.error("为画像 '{}' 创建或启动模拟时失败。", profileName, e);
            throw new RuntimeException("Failed to start simulation: " + profileName, e);
        }
    }

    public void stopSimulation(String profileName) {
        SimulationInstance instance = runningSimulations.remove(profileName);
        if (instance != null) {
            log.info("正在停止模拟实例 '{}'。", profileName);
            instance.stop();
        } else {
            log.warn("Simulation '{}' is not running.", profileName);
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
        log.info("Profile '{}' saved and cache reloaded.", profile.getProfileName());
    }

    public void deleteProfile(String profileName) {
        if (profileName == null || profileName.trim().isEmpty()) {
            throw new IllegalArgumentException("画像名称不能为空。");
        }
        if (runningSimulations.containsKey(profileName)) {
            throw new IllegalStateException("Cannot delete a profile that has a running simulation instance. Please stop the instance first.");
        }
        profileProvider.deleteProfile(profileName);
        loadAllProfiles();
        log.info("Profile '{}' deleted and cache reloaded.", profileName);
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
                    log.error("Failed to auto-start enabled profile '{}' on startup.", profile.getProfileName(), e);
                }
            });
    }
}