package com.xinsec.devicesimulator.service.controller;

import com.xinsec.devicesimulator.service.core.SimulationInstance;
import com.xinsec.devicesimulator.service.core.SimulationManager;
import com.xinsec.devicesimulator.service.core.SimulationManager.SimulationProfileStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@RestController
@RequestMapping("/api/instances")
@RequiredArgsConstructor
public class SimulationInstanceController {

    private final SimulationManager simulationManager;

    @GetMapping
    public ResponseEntity<Collection<SimulationProfileStatus>> getAllProfileStatuses() {
        return ResponseEntity.ok(simulationManager.getAllProfileStatuses());
    }

    @PostMapping("/{profileName}/_start")
    public ResponseEntity<SimulationProfileStatus> startInstance(@PathVariable String profileName) {
        try {
            simulationManager.startSimulation(profileName);
            // 返回更新后的所有状态，或者只返回该profile的最新状态
            return ResponseEntity.ok(simulationManager.getAllProfileStatuses().stream()
                    .filter(s -> s.getProfile().getProfileName().equals(profileName))
                    .findFirst().orElse(new SimulationProfileStatus(simulationManager.getProfile(profileName), "ERROR")));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new SimulationProfileStatus(simulationManager.getProfile(profileName), "ERROR: " + e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(new SimulationProfileStatus(simulationManager.getProfile(profileName), "ERROR: " + e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.internalServerError().body(new SimulationProfileStatus(simulationManager.getProfile(profileName), "ERROR: " + e.getMessage()));
        }
    }

    @PostMapping("/{profileName}/_stop")
    public ResponseEntity<SimulationProfileStatus> stopInstance(@PathVariable String profileName) {
        try {
            simulationManager.stopSimulation(profileName);
            // 返回更新后的所有状态，或者只返回该profile的最新状态
            return ResponseEntity.ok(simulationManager.getAllProfileStatuses().stream()
                    .filter(s -> s.getProfile().getProfileName().equals(profileName))
                    .findFirst().orElse(new SimulationProfileStatus(simulationManager.getProfile(profileName), "ERROR")));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new SimulationProfileStatus(simulationManager.getProfile(profileName), "ERROR: " + e.getMessage()));
        }
    }

    @PostMapping("/{profileName}/_restart")
    public ResponseEntity<SimulationProfileStatus> restartInstance(@PathVariable String profileName) {
        try {
            simulationManager.restartSimulation(profileName);
            // 返回更新后的所有状态，或者只返回该profile的最新状态
            return ResponseEntity.ok(simulationManager.getAllProfileStatuses().stream()
                    .filter(s -> s.getProfile().getProfileName().equals(profileName))
                    .findFirst().orElse(new SimulationProfileStatus(simulationManager.getProfile(profileName), "ERROR")));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new SimulationProfileStatus(simulationManager.getProfile(profileName), "ERROR: " + e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(new SimulationProfileStatus(simulationManager.getProfile(profileName), "ERROR: " + e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.internalServerError().body(new SimulationProfileStatus(simulationManager.getProfile(profileName), "ERROR: " + e.getMessage()));
        }
    }
}
