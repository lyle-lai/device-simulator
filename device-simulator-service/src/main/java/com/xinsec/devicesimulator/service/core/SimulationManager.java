package com.xinsec.devicesimulator.service.core;

import com.xinsec.devicesimulator.service.config.SimulationProfile;
import com.xinsec.devicesimulator.service.provider.ProfileProvider;
import javax.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SimulationManager implements ApplicationListener<ContextRefreshedEvent> {

    private final ProfileProvider profileProvider;
    private final ComponentFactory componentFactory;
    private final Map<String, SimulationInstance> runningSimulations = new ConcurrentHashMap<>();
    private ExecutorService executorService;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        log.info("Spring上下文刷新完毕，启动SimulationManager...");
        startAll();
    }

    public void startAll() {
        List<SimulationProfile> profiles = profileProvider.loadProfiles();
        log.info("发现 {} 个模拟画像配置文件。", profiles.size());

        if (executorService != null && !executorService.isShutdown()) {
            log.warn("检测到已有线程池，将强制关闭以重建。");
            stopAll();
        }
        executorService = Executors.newCachedThreadPool();
        runningSimulations.clear();

        for (SimulationProfile profile : profiles) {
            if (profile.isEnabled()) {
                try {
                    log.info("为画像 '{}' 创建模拟实例。", profile.getProfileName());
                    SimulationInstance instance = new SimulationInstance(profile, componentFactory);
                    runningSimulations.put(profile.getProfileName(), instance);
                    executorService.submit(instance);
                } catch (Exception e) {
                    log.error("为画像 '{}' 创建或启动模拟时失败。", profile.getProfileName(), e);
                }
            } else {
                log.info("跳过已禁用的画像: {}", profile.getProfileName());
            }
        }
    }

    @PreDestroy
    public void stopAll() {
        log.info("正在关闭所有模拟任务...");
        runningSimulations.values().forEach(SimulationInstance::stop);
        if (executorService != null) {
            executorService.shutdownNow();
        }
        runningSimulations.clear();
        log.info("所有模拟任务已停止。");
    }
}