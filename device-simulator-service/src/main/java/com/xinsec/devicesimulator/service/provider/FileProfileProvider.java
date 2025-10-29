package com.xinsec.devicesimulator.service.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinsec.devicesimulator.service.config.SimulationProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component("file")
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "simulator.config.provider", havingValue = "file", matchIfMissing = true)
public class FileProfileProvider implements ProfileProvider {

    // Original classpath scanning for initial profiles (read-only)
    private static final String CLASSPATH_PROFILES_LOCATION_PATTERN = "classpath*:simulation-profiles/*.json";

    // New: Writable directory for user-managed profiles
    @Value("${simulator.config.file.storage-path:./data/simulation-profiles}")
    private String storagePath;

    private Path profileStorageDirectory;

    private final ResourcePatternResolver resourcePatternResolver;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void init() throws IOException {
        profileStorageDirectory = Paths.get(storagePath).toAbsolutePath().normalize();
        if (!Files.exists(profileStorageDirectory)) {
            Files.createDirectories(profileStorageDirectory);
            log.info("Created profile storage directory: {}", profileStorageDirectory);
        } else {
            log.info("Using existing profile storage directory: {}", profileStorageDirectory);
        }
    }

    @Override
    public List<SimulationProfile> loadProfiles() {
        List<SimulationProfile> profiles = Stream.concat(
                loadFromClasspath().stream(),
                loadFromFileSystem().stream()
        ).collect(Collectors.toList());

        // Ensure uniqueness if profiles with same name exist in both classpath and file system
        // File system profiles should override classpath profiles
        return profiles.stream()
                .collect(Collectors.toMap(SimulationProfile::getProfileName, p -> p, (p1, p2) -> p2))
                .values()
                .stream()
                .collect(Collectors.toList());
    }

    private List<SimulationProfile> loadFromClasspath() {
        log.info("Scanning for profiles in classpath at: {}", CLASSPATH_PROFILES_LOCATION_PATTERN);
        try {
            Resource[] resources = resourcePatternResolver.getResources(CLASSPATH_PROFILES_LOCATION_PATTERN);
            if (resources.length == 0) {
                log.warn("No simulation profiles found in classpath at location: {}", CLASSPATH_PROFILES_LOCATION_PATTERN);
                return Collections.emptyList();
            }

            return Arrays.stream(resources)
                    .map(this::loadProfileFromResource)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("Error scanning for profiles in classpath at location: {}", CLASSPATH_PROFILES_LOCATION_PATTERN, e);
            return Collections.emptyList();
        }
    }

    private List<SimulationProfile> loadFromFileSystem() {
        log.info("Scanning for profiles in file system at: {}", profileStorageDirectory);
        try (Stream<Path> paths = Files.walk(profileStorageDirectory, 1)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".json"))
                    .map(this::loadProfileFromFile)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("Error scanning for profiles in file system at location: {}", profileStorageDirectory, e);
            return Collections.emptyList();
        }
    }

    private SimulationProfile loadProfileFromResource(Resource resource) {
        try {
            log.debug("Loading profile from classpath resource: {}", resource.getFilename());
            SimulationProfile profile = objectMapper.readValue(resource.getInputStream(), SimulationProfile.class);
            log.info("Successfully loaded profile from classpath: {}", profile.getProfileName());
            return profile;
        } catch (IOException e) {
            log.error("Failed to parse profile classpath resource: {}", resource.getFilename(), e);
            return null;
        }
    }

    private SimulationProfile loadProfileFromFile(Path filePath) {
        try {
            log.debug("Loading profile from file system: {}", filePath.getFileName());
            SimulationProfile profile = objectMapper.readValue(filePath.toFile(), SimulationProfile.class);
            log.info("Successfully loaded profile from file system: {}", profile.getProfileName());
            return profile;
        } catch (IOException e) {
            log.error("Failed to parse profile file: {}", filePath.getFileName(), e);
            return null;
        }
    }

    @Override
    public void saveProfile(SimulationProfile profile) {
        if (profile == null || profile.getProfileName() == null || profile.getProfileName().trim().isEmpty()) {
            throw new IllegalArgumentException("Profile and profile name cannot be null or empty.");
        }
        Path filePath = getProfileFilePath(profile.getProfileName());
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), profile);
            log.info("Successfully saved profile '{}' to file: {}", profile.getProfileName(), filePath);
        } catch (IOException e) {
            log.error("Failed to save profile '{}' to file: {}", profile.getProfileName(), filePath, e);
            throw new RuntimeException("Failed to save profile: " + profile.getProfileName(), e);
        }
    }

    @Override
    public void deleteProfile(String profileName) {
        if (profileName == null || profileName.trim().isEmpty()) {
            throw new IllegalArgumentException("Profile name cannot be null or empty.");
        }
        Path filePath = getProfileFilePath(profileName);
        try {
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("Successfully deleted profile file: {}", filePath);
            } else {
                log.warn("Attempted to delete non-existent profile file: {}", filePath);
            }
        } catch (IOException e) {
            log.error("Failed to delete profile file: {}", filePath, e);
            throw new RuntimeException("Failed to delete profile: " + profileName, e);
        }
    }

    private Path getProfileFilePath(String profileName) {
        // Ensure profile names are safe for file system (e.g., no path separators)
        String safeProfileName = profileName.replaceAll("[^a-zA-Z0-9-_.]", "_");
        return profileStorageDirectory.resolve(safeProfileName + ".json");
    }
}