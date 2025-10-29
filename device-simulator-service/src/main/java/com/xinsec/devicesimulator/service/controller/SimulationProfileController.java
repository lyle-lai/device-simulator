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
            // Check if a profile with the same name already exists
            if (simulationManager.getProfile(profile.getProfileName()) != null) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build(); // 409 Conflict
            }
            simulationManager.saveProfile(profile);
            return ResponseEntity.status(HttpStatus.CREATED).body(profile);
        } catch (IllegalArgumentException e) {
            log.error("Error creating profile: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            log.error("Internal server error creating profile: {}", profile.getProfileName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{profileName}")
    public ResponseEntity<SimulationProfile> updateProfile(@PathVariable String profileName, @RequestBody SimulationProfile profile) {
        if (!profileName.equals(profile.getProfileName())) {
            return ResponseEntity.badRequest().build(); // Profile name in path must match name in body
        }
        try {
            // Check if the profile exists before updating
            if (simulationManager.getProfile(profileName) == null) {
                return ResponseEntity.notFound().build();
            }
            simulationManager.saveProfile(profile); // saveProfile handles both create and update
            return ResponseEntity.ok(profile);
        } catch (IllegalArgumentException e) {
            log.error("Error updating profile: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            log.error("Internal server error updating profile: {}", profileName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{profileName}")
    public ResponseEntity<Void> deleteProfile(@PathVariable String profileName) {
        try {
            // Check if the profile exists before deleting
            if (simulationManager.getProfile(profileName) == null) {
                return ResponseEntity.notFound().build();
            }
            simulationManager.deleteProfile(profileName);
            return ResponseEntity.noContent().build(); // 204 No Content
        } catch (IllegalStateException e) {
            log.error("Error deleting profile {}: {}", profileName, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build(); // 409 Conflict if instance is running
        } catch (IllegalArgumentException e) {
            log.error("Error deleting profile: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            log.error("Internal server error deleting profile: {}", profileName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
