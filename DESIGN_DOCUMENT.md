# 设备模拟器项目整体设计文档

## 1. 概述

本项目是一个高度可配置的设备模拟器，旨在模拟各种医疗设备、物联网设备或其他数据源的行为。它能够通过不同的协议（如 TCP）发送根据预设策略生成或回放的数据。系统采用插件化和策略化的设计，使得添加新的设备行为、协议或数据格式变得简单。

## 2. 架构设计

系统核心是一个`SimulationManager`，它负责加载和管理多个`SimulationInstance`。每个实例代表一个正在运行的模拟设备，并由多个可插拔的组件构成，这些组件根据配置文件（Simulation Profile）动态装配。

### 2.1. 架构设计图 (Mermaid)

```mermaid
graph TD
    subgraph "配置源 (Profile Source)"
        direction LR
        A1[文件 (JSON)]
        A2[数据库 (DB)]
    end

    subgraph "核心引擎 (Core Engine)"
        B1[ProfileProvider] --> B2{SimulationManager};
        B2 -- 为每个Profile创建一个 --> B3(SimulationInstance);
    end

    subgraph "模拟实例 (SimulationInstance)"
        direction LR
        B3 -- 包含 --> C1[协议处理器<br>ProtocolHandler];
        B3 -- 包含 --> C2[模拟策略<br>SimulationStrategy];
        B3 -- 包含 --> C3[消息编解码器<br>MessageCodec];
        B3 -- 包含 --> C4[数据生成器<br>DataGenerator];
    end
    
    subgraph "外部系统 (External System)"
        D1[数据接收方<br>e.g., EMR/HIS]
    end

    A1 --> B1;
    A2 --> B1;
    
    C1 -- 发送数据 --> D1;
    C2 -- 使用 --> C4;
    C2 -- 使用 --> C3;
    C2 -- 控制 --> C1;

    style B2 fill:#f9f,stroke:#333,stroke-width:2px
    style B3 fill:#ccf,stroke:#333,stroke-width:2px
```

## 3. 各个模块的介绍

项目代码结构清晰，主要模块（包）职责如下：

-   **`device-simulator-client`**: 客户端模块，目前为空。设计上用于存放与服务端交互的DTO（数据传输对象）和API客户端，方便其他服务集成。
-   **`device-simulator-service`**: 核心服务模块。
    -   **`core`**: 包含模拟器的核心管理类，如 `SimulationManager`（管理所有模拟任务）和 `SimulationInstance`（单个模拟任务的实例）。`ComponentFactory` 负责根据配置动态创建组件。
    -   **`config`**: 定义了核心配置模型，如 `SimulationProfile`，它是所有模拟行为的蓝图。
    -   **`provider`**: 负责加载 `SimulationProfile`。`FileProfileProvider` 从文件系统加载JSON配置，`DatabaseProfileProvider` 从数据库加载。
    -   **`protocols`**: 处理网络通信。`ProtocolHandler` 是接口，`TcpClientHandler` 和 `TcpServerHandler` 提供了作为TCP客户端或服务端的具体实现。使用了Netty框架处理底层网络IO。
    -   **`strategies`**: 定义了模拟设备的行为逻辑。`SimulationStrategy` 是接口，`PeriodicPushStrategy`（周期性推送数据）、`RawReplayStrategy`（原始数据回放）是其具体实现。
    -   **`datagen`**: 负责生成模拟数据。`DataGenerator` 是接口，`RandomDataGenerator`（生成随机数据）、`PassthroughDataGenerator`（透传数据）是其具体实现。
    -   **`codecs`**: 负责消息的编码和解码。`MessageCodec` 是接口，`JsonCodec`（处理JSON格式）、`PassthroughCodec`（不进行编解码，直接透传）是其具体实现。
    -   **`entity` / `mapper`**: 用于数据库持久化，`SimulationProfileEntity` 是数据库表实体，`SimulationProfileMapper` 用于实体和DTO之间的转换。

## 4. 业务流程

系统启动和运行的主要流程如下：

1.  **启动**: Spring Boot应用 `DeviceSimulatorApplication` 启动。
2.  **加载配置**: `SimulationManager` 在启动时，通过注入的 `ProfileProvider`（可以是文件或数据库提供者）加载所有启用的 `SimulationProfile`。
3.  **创建实例**: `SimulationManager` 遍历加载的每个 `SimulationProfile`，为其创建一个对应的 `SimulationInstance`。
4.  **组件装配**: 在创建 `SimulationInstance` 的过程中，`SpringComponentFactory` 会根据 `SimulationProfile` 中定义的类型（如 `protocol.type`, `strategy.type`）从Spring容器中查找并装配对应的组件（`ProtocolHandler`, `SimulationStrategy` 等）。
5.  **启动模拟**: `SimulationManager` 启动每个 `SimulationInstance`。
6.  **执行策略**: `SimulationStrategy` (如 `PeriodicPushStrategy`) 开始执行。它会按预设的间隔（`intervalMillis`）触发。
7.  **生成数据**: 策略调用 `DataGenerator` (如 `RandomDataGenerator`) 来生成模拟的业务数据（如心率、血氧）。
8.  **编码数据**: 生成的数据被传递给 `MessageCodec` (如 `PassthroughCodec`) 进行格式化。
9.  **发送数据**: 策略通过 `ProtocolHandler` (如 `TcpClientHandler`) 将编码后的数据发送到目标地址（如 `127.0.0.1:18888`）。
10. **生命周期管理**: `ProtocolHandler` 负责管理网络连接，包括连接、断线重连等。`SimulationManager` 负责整个模拟实例的生命周期。

## 5. 技术架构

-   **核心框架**: Spring Boot 2.7.17，用于快速构建和管理应用，提供依赖注入、自动配置等能力。
-   **构建工具**: Apache Maven，用于项目构建和依赖管理。
-   **编程语言**: Java 1.8。
-   **网络通信**: Netty，用于实现高性能的TCP客户端和服务器。
-   **数据库与持久化**:
    -   使用 `spring-boot-starter-data-jpa` 或类似JPA实现进行数据库操作。
    -   数据库迁移: Flyway (从`V1__...`的命名方式推断)，用于管理数据库表结构的演进。
    -   数据库: MySQL (从SQL语法推断)。
-   **JSON处理**: FastJSON。
-   **设计模式**:
    -   **策略模式 (Strategy Pattern)**: `SimulationStrategy` 的设计是典型的策略模式，允许在运行时切换或配置不同的设备行为。
    -   **工厂模式 (Factory Pattern)**: `ComponentFactory` 用于根据配置创建不同的组件实例。
    -   **依赖注入 (Dependency Injection)**: Spring框架的核心特性，用于解耦组件。

## 6. 关键算法/逻辑

-   **动态组件装配**: `SpringComponentFactory` 是实现插件化设计的关键。它可能通过一个Map或`@Qualifier`机制，将配置中的字符串（如 "tcp-client"）映射到具体的Spring Bean实例上，从而动态组装一个`SimulationInstance`。
-   **周期性推送策略 (`PeriodicPushStrategy`)**: 内部可能使用Java的 `ScheduledExecutorService` 或Spring的 `@Scheduled` 注解来实现定时任务。在每个任务周期，它调用数据生成器和协议处理器来完成一次数据推送。
-   **原始数据回放策略 (`RawReplayStrategy`)**: 此策略会从配置中读取一个预定义的`messages`数组，每个message包含要发送的数据和发送延迟。策略会按顺序遍历数组，发送数据，然后等待指定的延迟时间，模拟真实场景中的数据流。

## 7. 数据库表

根据 `V1__create_simulation_profiles_table.sql` 文件，定义了以下数据库表用于存储模拟配置。

### `simulation_profiles`

| 字段名         | 数据类型      | 约束/索引                | 描述                               |
| -------------- | ------------- | ------------------------ | ---------------------------------- |
| `id`           | `bigint(20)`  | `PRIMARY KEY`, `AUTO_INCREMENT` | 唯一标识符                         |
| `profile_name` | `varchar(255)`| `NOT NULL`, `UNIQUE KEY` | 模拟配置的名称，必须唯一           |
| `is_enabled`   | `tinyint(1)`  | `NOT NULL`               | 是否启用此配置 (1: 启用, 0: 禁用) |
| `profile_config`| `text`        | `NOT NULL`               | 存储完整配置的JSON字符串           |
| `created_at`   | `timestamp`   | `NOT NULL`, `DEFAULT CURRENT_TIMESTAMP` | 记录创建时间                       |
| `updated_at`   | `timestamp`   | `NOT NULL`, `DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP` | 记录最后更新时间                   |

## 8. 已知设计缺陷/可改进点

1.  **无管理界面**: 所有模拟配置都通过JSON文件或直接操作数据库来管理，缺乏一个用户友好的Web界面来动态创建、更新、启动或停止模拟任务。
2.  **配置热加载**: 当前设计似乎在服务启动时加载所有配置。如果修改了文件或数据库中的配置，很可能需要重启服务才能生效，缺乏动态热加载能力。
3.  **单点瓶颈**: `SimulationManager` 在单个服务实例中运行。当模拟的设备数量巨大时，可能会成为性能瓶颈。对于大规模模拟，需要考虑分布式架构。
4.  **状态管理**: 当前的策略（如周期性推送）大多是无状态的。对于需要模拟复杂状态机（如设备开关机、不同工作模式切换）的场景，现有设计可能需要扩展。
5.  **客户端API未定义**: `device-simulator-client` 模块目前是空的，意味着还没有为外部系统提供标准的、强类型的API来查询或控制模拟器。
6.  **监控与度量**: 项目缺少对模拟过程的监控。例如，无法方便地查看每个模拟实例的当前状态、已发送消息数、错误率等关键指标。

