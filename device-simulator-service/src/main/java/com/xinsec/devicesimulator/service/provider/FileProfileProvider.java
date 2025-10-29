package com.xinsec.devicesimulator.service.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinsec.devicesimulator.service.config.SimulationProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component("file")
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "simulator.config.provider", havingValue = "file", matchIfMissing = true)
public class FileProfileProvider implements ProfileProvider {

    private static final String PROFILES_LOCATION_PATTERN = "classpath*:simulation-profiles/*.json";

    private final ResourcePatternResolver resourcePatternResolver;
    private final ObjectMapper objectMapper;

    @Override
    public List<SimulationProfile> loadProfiles() {
        log.info("Scanning for profiles in classpath at: {}", PROFILES_LOCATION_PATTERN);
        try {
            Resource[] resources = resourcePatternResolver.getResources(PROFILES_LOCATION_PATTERN);
            if (resources.length == 0) {
                log.warn("No simulation profiles found at location: {}", PROFILES_LOCATION_PATTERN);
                return Collections.emptyList();
            }

            return Arrays.stream(resources)
                    .map(this::loadProfileFromResource)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("Error scanning for profiles at location: {}", PROFILES_LOCATION_PATTERN, e);
            return Collections.emptyList();
        }
    }

    private SimulationProfile loadProfileFromResource(Resource resource) {
        try {
            log.debug("Loading profile from resource: {}", resource.getFilename());
            SimulationProfile profile = objectMapper.readValue(resource.getInputStream(), SimulationProfile.class);
            log.info("Successfully loaded profile: {}", profile.getProfileName());
            return profile;
        } catch (IOException e) {
            log.error("Failed to parse profile resource: {}", resource.getFilename(), e);
            return null;
        }
    }
}