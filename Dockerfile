# ---- Stage 1: Build Stage ----
# 使用官方的 Maven 镜像，它包含了 JDK 8，用于构建项目
FROM maven:3.8-openjdk-8 as builder

# 设置工作目录
WORKDIR /app

# 优化 Docker 缓存：首先只复制 pom 文件来下载依赖
# 这样只有当 pom 文件变化时，才会重新下载依赖
COPY pom.xml .
COPY device-simulator-client/pom.xml ./device-simulator-client/
COPY device-simulator-service/pom.xml ./device-simulator-service/
# RUN mvn dependency:go-offline

# 复制整个项目的源代码
COPY . .

# 执行 Maven 打包命令，跳过测试
RUN mvn clean package -DskipTests


# ---- Stage 2: Runtime Stage ----
# 使用一个更小的 JRE 镜像作为最终的运行环境
FROM openjdk:8-jre-slim

# 设置工作目录
WORKDIR /app

# 从构建阶段复制已打包好的 JAR 文件
# 这里的路径需要根据实际的JAR包名称进行匹配，通配符能很好地处理版本号变化
COPY --from=builder /app/device-simulator-service/target/device-simulator-service-*.jar app.jar

# 声明应用将使用的端口（即使在host模式下，这也是一个好习惯，用于文档化）
EXPOSE 18080

# 设置容器启动时执行的命令
ENTRYPOINT ["java", "-jar", "app.jar"]
