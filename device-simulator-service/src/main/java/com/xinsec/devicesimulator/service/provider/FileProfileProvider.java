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

    // 原始的 classpath 扫描，用于加载初始的只读画像
    private static final String CLASSPATH_PROFILES_LOCATION_PATTERN = "classpath*:simulation-profiles/*.json";

    // 新增：可写入的目录，用于存放用户管理的画像
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
            log.info("已创建画像存储目录: {}", profileStorageDirectory);
        } else {
            log.info("使用已存在的画像存储目录: {}", profileStorageDirectory);
        }
    }

    @Override
    public List<SimulationProfile> loadProfiles() {
        List<SimulationProfile> profiles = Stream.concat(
                loadFromClasspath().stream(),
                loadFromFileSystem().stream()
        ).collect(Collectors.toList());

        // 如果 classpath 和文件系统中存在同名画像，确保唯一性
        // 文件系统中的画像应覆盖 classpath 中的画像
        return profiles.stream()
                .collect(Collectors.toMap(SimulationProfile::getProfileName, p -> p, (p1, p2) -> p2))
                .values()
                .stream()
                .collect(Collectors.toList());
    }

    private List<SimulationProfile> loadFromClasspath() {
        log.info("正在扫描 classpath 中的画像: {}", CLASSPATH_PROFILES_LOCATION_PATTERN);
        try {
            Resource[] resources = resourcePatternResolver.getResources(CLASSPATH_PROFILES_LOCATION_PATTERN);
            if (resources.length == 0) {
                log.warn("在 classpath 位置未找到任何模拟画像: {}", CLASSPATH_PROFILES_LOCATION_PATTERN);
                return Collections.emptyList();
            }

            return Arrays.stream(resources)
                    .map(this::loadProfileFromResource)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("在 classpath 位置扫描画像时出错: {}", CLASSPATH_PROFILES_LOCATION_PATTERN, e);
            return Collections.emptyList();
        }
    }

    private List<SimulationProfile> loadFromFileSystem() {
        log.info("正在文件系统中扫描画像: {}", profileStorageDirectory);
        try (Stream<Path> paths = Files.walk(profileStorageDirectory, 1)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".json"))
                    .map(this::loadProfileFromFile)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("在文件系统位置扫描画像时出错: {}", profileStorageDirectory, e);
            return Collections.emptyList();
        }
    }

    private SimulationProfile loadProfileFromResource(Resource resource) {
        try {
            log.debug("从 classpath 资源加载画像: {}", resource.getFilename());
            SimulationProfile profile = objectMapper.readValue(resource.getInputStream(), SimulationProfile.class);
            log.info("成功从 classpath 加载画像: {}", profile.getProfileName());
            return profile;
        } catch (IOException e) {
            log.error("解析 classpath 画像资源失败: {}", resource.getFilename(), e);
            return null;
        }
    }

    private SimulationProfile loadProfileFromFile(Path filePath) {
        try {
            log.debug("从文件系统加载画像: {}", filePath.getFileName());
            SimulationProfile profile = objectMapper.readValue(filePath.toFile(), SimulationProfile.class);
            log.info("成功从文件系统加载画像: {}", profile.getProfileName());
            return profile;
        } catch (IOException e) {
            log.error("解析画像文件失败: {}", filePath.getFileName(), e);
            return null;
        }
    }

    @Override
    public void saveProfile(SimulationProfile profile) {
        if (profile == null || profile.getProfileName() == null || profile.getProfileName().trim().isEmpty()) {
            throw new IllegalArgumentException("画像和画像名称不能为空。");
        }
        Path filePath = getProfileFilePath(profile.getProfileName());
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), profile);
            log.info("成功将画像 '{}' 保存到文件: {}", profile.getProfileName(), filePath);
        } catch (IOException e) {
            log.error("将画像 '{}' 保存到文件失败: {}", profile.getProfileName(), filePath, e);
            throw new RuntimeException("保存画像失败: " + profile.getProfileName(), e);
        }
    }

    @Override
    public void deleteProfile(String profileName) {
        if (profileName == null || profileName.trim().isEmpty()) {
            throw new IllegalArgumentException("画像名称不能为空。");
        }
        Path filePath = getProfileFilePath(profileName);
        try {
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("成功删除画像文件: {}", filePath);
            } else {
                log.warn("尝试删除一个不存在的画像文件: {}", filePath);
            }
        } catch (IOException e) {
            log.error("删除画像文件失败: {}", filePath, e);
            throw new RuntimeException("删除画像失败: " + profileName, e);
        }
    }

    private Path getProfileFilePath(String profileName) {
        // 确保画像名称对于文件系统是安全的（例如，不包含路径分隔符）
        String safeProfileName = profileName.replaceAll("[^a-zA-Z0-9-_.]", "_");
        return profileStorageDirectory.resolve(safeProfileName + ".json");
    }
}