# 设备模拟器系统设计文档

## 1. 概述

本项目是一个高度可配置的设备模拟器平台，旨在通过软件模拟各种硬件设备（如医疗设备、物联网传感器等）的网络行为。它解决了在软件开发和测试过程中，由于物理设备不足、环境搭建复杂或设备行为难以复现所带来的挑战。

平台的核心价值在于其灵活性和可扩展性，用户通过编写“模拟画像”（Simulation Profile）配置文件，即可定义一个模拟设备的完整行为，包括通信协议、数据格式、行为策略和数据来源，而无需修改任何代码。

**目标读者**: 
- 后端开发工程师
- 测试工程师
- 运维与部署工程师

## 2. 系统架构

### 2.1 架构图

系统采用组件化的插件式架构。核心引擎根据配置源（数据库或文件）加载模拟画像，并为每个画像动态装配所需的组件，创建一个独立的模拟实例。

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

    A1 & A2 --> B1;
    
    C1 -- 发送/接收数据 --> D1;
    C2 -- 使用 --> C4;
    C2 -- 使用 --> C3;
    C2 -- 控制 --> C1;

    style B2 fill:#f9f,stroke:#333,stroke-width:2px
    style B3 fill:#ccf,stroke:#333,stroke-width:2px
```

### 2.2 模块划分

项目采用 Maven 多模块管理，结构清晰：

- `device-simulator` (父模块): 聚合所有子模块，统一管理依赖版本。
- `device-simulator-client`: 客户端模块。设计上用于存放与服务端交互的DTO和API客户端，方便其他Java服务集成（目前内容较少）。
- `device-simulator-service`: **核心服务模块**。包含了所有的业务逻辑、Web接口、核心模拟引擎和配置等。

### 2.3 技术栈

| 分类 | 技术 | 版本/说明 |
| :--- | :--- | :--- |
| **后端核心** | Java | 1.8 |
| | Spring Boot | 2.7.17 |
| **网络通信** | Netty | 4.1.50.Final (用于实现TCP/UDP等协议) |
| **数据库** | MySQL | 5.7+ |
| | MyBatis-Plus | 3.5.3.1 (数据持久化) |
| | Flyway | 7.15.0 (数据库迁移管理) |
| **前端** | Vue.js | 3.x |
| | Element Plus | UI组件库 |
| | WebSocket | SockJS + STOMP.js (用于实时日志) |
| **构建/部署** | Maven | 3.8+ |
| | Docker | 容器化 |
| | Docker Compose | 服务编排 |

## 3. 核心概念与设计

### 3.1 模拟画像 (Simulation Profile)

“模拟画像”是驱动所有模拟行为的蓝图，它是一个JSON对象，定义了一个模拟设备的全部特征。系统通过解析画像来动态构建一个模拟实例。

一个画像主要由以下部分组成：

```json
{
  "profileName": "mindray-monitor-demo",
  "enabled": true,
  "device": {
    "id": "MINDRAY-SN-001"
  },
  "protocol": {
    "type": "tcp-client",
    "properties": {
      "host": "127.0.0.1",
      "port": 18888
    }
  },
  "strategy": {
    "type": "periodic-push",
    "properties": {
      "intervalMillis": 2000
    }
  },
  "dataGenerator": {
    "type": "random",
    "properties": [...]
  },
  "codec": {
    "type": "passthrough"
  }
}
```

### 3.2 核心组件 (Pluggable Components)

系统设计的精髓在于其插件化的组件模型。每个模拟实例都是由以下四种类型的组件动态组装而成，每种组件都可以有多种实现。

- **`SimulationStrategy` (模拟策略)**
  - **职责**: 定义设备的核心行为模式，即“如何发送数据”。
  - **已有实现**: 
    - `periodic-push`: 周期性地主动推送数据。
    - `request-response`: 被动地等待请求，然后根据规则查找并返回响应。
    - `passthrough`: 透传策略，简单地将收到的数据转发。

- **`ProtocolHandler` (协议处理器)**
  - **职责**: 负责具体的网络通信，即“用什么协议发送数据”。
  - **已有实现**: 
    - `tcp-client`: 作为TCP客户端连接到指定服务器。
    - `tcp-server`: 作为TCP服务端，等待客户端连接。

- **`DataGenerator` (数据生成器)**
  - **职责**: 负责生成模拟的业务数据，即“发送什么内容”。
  - **已有实现**: 
    - `random`: 根据配置的规则（如范围、精度）生成随机数据。
    - `database`: 从数据库的报文表 (`payload_repository`) 中随机读取一个报文。
    - `db-rule-based`: 根据请求内容，从规则表 (`request_response_rules`) 中匹配并返回响应报文。

- **`MessageCodec` (消息编解码器)**
  - **职责**: 负责在发送前对数据进行编码，或在接收后进行解码，即“按什么格式发送”。
  - **已有实现**: 
    - `json`: 处理JSON格式数据。
    - `hex`: 处理十六进制字符串数据。
    - `passthrough`: 不进行任何编解码，直接透传原始字节。

### 3.3 生命周期管理

- **`SimulationManager`**: 是所有模拟任务的“总指挥”。它在应用启动时被初始化，负责从配置源加载所有画像，并根据用户指令（通过API）来创建、启动、停止和管理 `SimulationInstance`。
- **`SimulationInstance`**: 代表一个正在运行的模拟器实例。它在被创建时，会根据画像配置，从Spring容器中动态获取并组装上述四种核心组件。每个实例都在自己的线程中运行（由 `SimulationManager` 的线程池管理）。

## 4. 数据模型

### 4.1 数据库设计

系统使用Flyway管理数据库版本，表结构定义在 `src/main/resources/db/migration` 中。

- **`simulation_profiles`**: 存储模拟画像的主表。
  | 字段名 | 类型 | 描述 |
  | :--- | :--- | :--- |
  | `id` | `bigint` | 主键ID |
  | `profile_name` | `varchar` | 画像唯一名称 |
  | `is_enabled` | `tinyint` | 是否启用此画像 |
  | `profile_config` | `text` | 存储完整画像的JSON字符串 |

- **`payload_repository`**: 报文存储库，用于 `database-generator`。
  | 字段名 | 类型 | 描述 |
  | :--- | :--- | :--- |
  | `id` | `bigint` | 主键ID |
  | `payload_key` | `varchar` | 报文唯一键 |
  | `group_key` | `varchar` | 报文分组键 |
  | `content` | `text` | 报文内容 |

- **`request_response_rules`**: 请求-响应规则表，用于 `db-rule-based` 生成器。
  | 字段名 | 类型 | 描述 |
  | :--- | :--- | :--- |
  | `id` | `bigint` | 主键ID |
  | `rule_group` | `varchar` | 规则分组键 |
  | `request_key` | `varchar` | 用于匹配请求的关键字 |
  | `response_key` | `varchar` | 匹配成功后，对应的报文键 |

### 4.2 配置文件设计

除了数据库，系统也支持从文件加载画像。内置的只读画像位于 `src/main/resources/simulation-profiles/`。通过API创建或修改的画像，默认存储在应用运行目录下的 `./data/simulation-profiles/` 文件夹中。

## 5. 接口设计

### 5.1 REST API

系统通过标准的RESTful API提供管理能力。

- `GET /api/profiles`: 获取所有可用的模拟画像。
- `POST /api/profiles`: 创建一个新的模拟画像。
- `PUT /api/profiles/{profileName}`: 更新一个已有的模拟画像。
- `DELETE /api/profiles/{profileName}`: 删除一个模拟画像。
- `GET /api/instances`: 获取所有画像的当前运行状态。
- `POST /api/instances/{profileName}/_start`: 启动一个模拟实例。
- `POST /api/instances/{profileName}/_stop`: 停止一个模拟实例。
- `GET /api/payloads`, `POST`, `PUT`, `DELETE`: 对报文存储库进行增删改查。
- `GET /api/rules`, `POST`, `PUT`, `DELETE`: 对请求响应规则进行增删改查。

### 5.2 WebSocket API

系统使用WebSocket向前端推送实时日志，方便用户监控模拟实例的运行情况。

- **连接端点**: `/ws` (使用SockJS以增强兼容性)
- **订阅主题**: `/topic/logs/{profileName}` (订阅特定实例的日志)

## 6. 部署与配置

### 6.1 容器化部署

项目已完全容器化，推荐使用 Docker 和 Docker Compose 进行部署。

- **`Dockerfile`**: 位于项目根目录，采用多阶段构建，生成一个轻量、高效的应用镜像。
- **`docker-compose.yml`**: 位于项目根目录，用于一键编排和启动应用容器及MySQL数据库容器。
- **构建与运行**: 
  1. **构建镜像**: `docker build -t xinsec/device-simulator:1.0 .`
  2. **启动服务**: `docker-compose up -d`

### 6.2 关键配置

所有关键配置均已外部化，可在 `docker-compose.yml` 的 `environment` 部分进行设置。

- `SERVER_PORT`: 应用服务端口 (默认为 `18080`)。
- `DB_URL`: 数据库连接地址。
- `DB_USER`: 数据库用户名。
- `DB_PASS`: 数据库密码。
- `SIMULATOR_PROVIDER`: 配置源，`database` 或 `file`。
- `SIMULATOR_FILE_PATH`: 当使用 `file` 源时，画像文件的存储路径。

## 7. 如何扩展

得益于插件化的设计，为系统添加新功能非常简单。下面以“添加一个新的模拟策略”为例。

1.  **创建策略类**: 在 `com.xinsec.devicesimulator.service.strategies.impl` 包下创建一个新的Java类，实现 `SimulationStrategy` 接口。

    ```java
    @ComponentType("my-new-strategy") // <-- 1. 定义一个唯一的类型名称
    @Scope("prototype")
    @Component
    public class MyNewStrategy implements SimulationStrategy {
        @Override
        public void configure(...) { ... }

        @Override
        public void execute() { ... }

        @Override
        public void stop() { ... }
    }
    ```

2.  **实现接口方法**: 在新类中实现 `configure`, `execute`, `stop` 三个核心方法，编写你的业务逻辑。

3.  **在画像中使用**: 创建或修改一个模拟画像JSON文件，将 `strategy.type` 的值设置为你在 `@ComponentType` 注解中定义的名称。

    ```json
    "strategy": {
      "type": "my-new-strategy", // <-- 2. 在配置中使用
      "properties": { ... } 
    }
    ```

4.  **完成**: 重新启动应用，`SimulationManager` 将能够自动发现并加载你的新策略。添加新的协议处理器、数据生成器和编解码器的流程与此完全相同。
