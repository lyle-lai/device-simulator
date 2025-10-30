package com.xinsec.devicesimulator.service.controller;

import com.xinsec.devicesimulator.service.config.SimulationProfile;
import com.xinsec.devicesimulator.service.core.SimulationManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
@Slf4j
public class SimulationProfileController {

    private final SimulationManager simulationManager;

    @GetMapping
    public ResponseEntity<Collection<SimulationProfile>> getAllProfiles() {
        return ResponseEntity.ok(simulationManager.getAvailableProfiles());
    }

    @GetMapping("/{profileName}")
    public ResponseEntity<SimulationProfile> getProfileByName(@PathVariable String profileName) {
        SimulationProfile profile = simulationManager.getProfile(profileName);
        if (profile == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(profile);
    }

    @PostMapping
    public ResponseEntity<SimulationProfile> createProfile(@RequestBody SimulationProfile profile) {
        try {
            // 检查同名画像是否已存在
            if (simulationManager.getProfile(profile.getProfileName()) != null) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build(); // 409 冲突
            }
            simulationManager.saveProfile(profile);
            return ResponseEntity.status(HttpStatus.CREATED).body(profile);
        } catch (IllegalArgumentException e) {
            log.error("创建画像时出错: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            log.error("创建画像 '{}' 时发生内部服务器错误", profile.getProfileName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{profileName}")
    public ResponseEntity<SimulationProfile> updateProfile(@PathVariable String profileName, @RequestBody SimulationProfile profile) {
        if (!profileName.equals(profile.getProfileName())) {
            // 路径中的画像名称必须与请求体中的名称匹配
            return ResponseEntity.badRequest().build();
        }
        try {
            // 更新前，先检查画像是否存在
            if (simulationManager.getProfile(profileName) == null) {
                return ResponseEntity.notFound().build();
            }
            simulationManager.saveProfile(profile); // saveProfile 方法会处理创建和更新两种情况
            return ResponseEntity.ok(profile);
        } catch (IllegalArgumentException e) {
            log.error("更新画像时出错: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            log.error("更新画像 '{}' 时发生内部服务器错误", profileName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{profileName}")
    public ResponseEntity<Void> deleteProfile(@PathVariable String profileName) {
        try {
            // 删除前，先检查画像是否存在
            if (simulationManager.getProfile(profileName) == null) {
                return ResponseEntity.notFound().build();
            }
            simulationManager.deleteProfile(profileName);
            return ResponseEntity.noContent().build(); // 204 无内容
        } catch (IllegalStateException e) {
            log.error("删除画像 '{}' 时出错: {}", profileName, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build(); // 409 冲突，如果实例正在运行
        } catch (IllegalArgumentException e) {
            log.error("删除画像时出错: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            log.error("删除画像 '{}' 时发生内部服务器错误", profileName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
