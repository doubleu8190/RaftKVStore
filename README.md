# RaftKVStore - 基于 Raft 协议的分布式键值存储系统

## 项目概述

RaftKVStore 是一个基于 Raft 共识算法实现的高可用分布式键值存储系统。项目采用 Java 21 开发，使用 Maven 进行构建管理，实现了完整的 Raft 协议规范，包括领导选举、日志复制、成员变更、Pre-Vote 和快照等功能。

## 项目特性

- **完整的 Raft 协议实现**：支持 Leader、Follower、Candidate 三种角色状态转换，以及 Pre-Vote 协议
- **联合共识（Joint Consensus）**：支持安全的动态成员变更，包含 SYNCING → OLD_NEW → NEW → STABLE 四个阶段
- **高性能网络通信**：基于 Netty 框架实现高性能网络通信，双通道架构（客户端通道 + 节点间通道）
- **高效序列化**：使用 Protostuff 进行对象序列化，配合 LinkedBuffer 对象池减少 GC 压力
- **灵活的存储策略**：支持同步（Sync）和异步（Async）两种日志刷盘方式
- **完善的快照系统**：支持日志压缩、状态机快照生成、快照分块传输和增量安装
- **集群管理**：支持动态成员变更和集群配置
- **高可用性**：自动故障转移、Leader 重定向和数据一致性保证
- **对象池化**：ByteBuffer、LinkedBuffer、Channel、Role 实例均使用对象池，减少 GC 开销

## 架构设计

### 模块结构

```
RaftKVStore/
├── core/                          # 核心模块（命令定义、序列化、工具类）
│   └── src/main/java/cn/ttplatform/wh/
│       ├── cmd/                   # 命令定义（Set、Get、ClusterChange 等 10 种）
│       │   └── factory/           # 命令序列化器（Protostuff）
│       ├── constant/              # 常量定义（消息类型、错误消息、CLI 选项）
│       ├── exception/             # 异常定义（6 种异常类）
│       ├── log4j/                 # Log4j 辅助类
│       └── support/               # 通用支持类（序列化框架、对象池、编解码器）
├── server/                        # 服务器模块（Raft 协议实现）
│   └── src/main/java/cn/ttplatform/wh/
│       ├── config/                # 配置管理（模块化配置，支持 env 覆盖）
│       ├── data/                  # 数据存储层
│       │   ├── log/               # 日志存储（Sync/Async 两种实现）
│       │   ├── index/             # 索引管理
│       │   └── snapshot/          # 快照系统（生成、传输、安装）
│       ├── group/                 # 集群管理（Cluster、Endpoint、Connector）
│       ├── handler/               # 命令处理器（Set/Get/ClusterChange 等）
│       ├── message/               # Raft 消息定义及处理器
│       │   ├── handler/           # 消息处理器（AppendEntries、Vote、Snapshot）
│       │   └── serializer/        # 消息序列化器
│       ├── role/                  # Raft 角色实现（Leader、Candidate、Follower）
│       ├── scheduler/             # 调度器（选举超时、日志复制）
│       └── support/               # 服务器支持类（通道池、缓冲区池、LRU 缓存）
└── client/                        # 客户端模块（开发中）
```

### 核心组件

#### 1. GlobalContext（全局上下文）
- 系统的依赖注入和状态管理中心
- 持有所有核心组件的引用（Node、StateMachine、DataManager、Cluster 等）
- 注册所有消息/命令处理器和序列化器
- 协调 Raft 状态变更（选举、日志复制、提交推进、集群阶段转换）

#### 2. Node（节点）
- 管理节点的状态和角色转换（Follower → Candidate → Leader）
- 持久化当前 term 和 votedFor 到 `node.metadata` 文件（memory-mapped）
- 协调各个组件的工作

#### 3. DataManager（数据管理器）
- 管理日志存储、索引和快照
- 提供日志追加、读取、传输接口
- 处理日志压缩和快照生成/安装

#### 4. StateMachine（状态机）
- 基于 `HashMap<String, String>` 的键值对存储
- 应用已提交的日志条目
- 支持快照生成和应用（使用 Protostuff 序列化）

#### 5. Cluster（集群）
- 管理集群成员（Endpoint）和集群阶段（Phase）
- 实现联合共识（Joint Consensus）成员变更
- 计算多数派 commit index

#### 6. Connector（连接器）
- 双 Netty 架构：客户端 Bootstrap（出站）+ 服务端 ServerBootstrap（入站）
- 使用 ChannelPool 缓存到对等节点的连接
- 负责节点间 Raft 消息的发送和接收

#### 7. Server（服务器）
- Netty TCP 服务器，处理客户端请求
- ServerDuplexChannelHandler 检查当前节点是否为 Leader，否则返回 RedirectCommand

#### 8. Scheduler（调度器）
- 基于 `ScheduledThreadPoolExecutor` 的单线程调度器
- 选举超时任务使用随机超时时间（minElectionTimeout ~ maxElectionTimeout）
- 所有 Raft 状态变更串行化到单线程 core executor 执行，避免并发问题

#### 9. RoleCache（角色对象池）
- 回收和复用 Follower、Candidate、Leader 角色实例
- 重置角色状态、取消定时任务

## 快速开始

### 环境要求

- Java 21 或更高版本
- Maven 3.6 或更高版本

### 构建项目

```bash
# 克隆项目
git clone <repository-url>
cd RaftKVStore

# 构建项目
mvn clean package -DskipTests
```

### 单机模式启动

单机模式（Singleton）下，节点直接成为 Leader，无需选举。适用于开发调试或单节点部署。

```bash
# 方式一：使用启动脚本（推荐）
./start-singleton.sh

# 方式二：自定义参数
NODE_ID=my-node HOST=0.0.0.0 PORT=8080 ./start-singleton.sh

# 方式三：直接使用 java -jar
java -jar server/target/server-1.0-jar-with-dependencies.jar \
    -i node-1 -h 127.0.0.1 -p 6666 -m singleton
```

### 集群模式启动

集群模式（Cluster）下，节点参与 Raft 协议，通过选举产生 Leader。默认启动一个 3 节点集群。

```bash
# 方式一：使用启动脚本（推荐，一键启动 3 节点集群）
./start-cluster.sh

# 方式二：强制重新构建后启动
./start-cluster.sh --build

# 停止集群
./start-cluster.sh stop

# 查看集群状态
./start-cluster.sh status

# 方式三：手动分别启动各节点
java -jar server/target/server-1.0-jar-with-dependencies.jar \
    -c server/src/main/resources/A/server.properties &
java -jar server/target/server-1.0-jar-with-dependencies.jar \
    -c server/src/main/resources/B/server.properties &
java -jar server/target/server-1.0-jar-with-dependencies.jar \
    -c server/src/main/resources/C/server.properties &
```

**默认集群端口分配：**

| 节点 | 客户端端口 | 节点间通信端口 | 数据目录 |
|------|-----------|---------------|---------|
| A    | 6666      | 6665          | ./data/A |
| B    | 7777      | 7776          | ./data/B |
| C    | 8888      | 8887          | ./data/C |

### 自定义集群启动

如需自定义集群配置（如不同节点数、非本地主机等），创建自己的 properties 文件后手动启动：

```bash
# 启动节点 1
java -jar server/target/server-1.0-jar-with-dependencies.jar \
    -i node1 -h 192.168.1.10 -p 8000 -m cluster \
    -C "node1,192.168.1.10,8000,8001 node2,192.168.1.11,8000,8001 node3,192.168.1.12,8000,8001"

# 启动节点 2
java -jar server/target/server-1.0-jar-with-dependencies.jar \
    -i node2 -h 192.168.1.11 -p 8000 -m cluster \
    -C "node1,192.168.1.10,8000,8001 node2,192.168.1.11,8000,8001 node3,192.168.1.12,8000,8001"

# 启动节点 3
java -jar server/target/server-1.0-jar-with-dependencies.jar \
    -i node3 -h 192.168.1.12 -p 8000 -m cluster \
    -C "node1,192.168.1.10,8000,8001 node2,192.168.1.11,8000,8001 node3,192.168.1.12,8000,8001"
```

### 命令行参数

| 参数 | 长参数 | 说明 |
|------|--------|------|
| `-i` | `--id` | 节点 ID，集群中必须唯一 |
| `-h` | `--host` | 服务器监听地址 |
| `-p` | `--port` | 服务器监听端口（客户端端口） |
| `-c` | `--config` | 配置文件路径（.properties 格式） |
| `-m` | `--mode` | 运行模式：`singleton` 或 `cluster` |
| `-C` | `--cluster` | 集群信息（cluster 模式必需）<br>格式：`<id>,<host>,<port>,<connectorPort> <id>,<host>,<port>,<connectorPort> ...` |

## 配置说明

### 完整配置项

以下是 `server.properties` 中所有支持的配置项（参见 `ServerProperties.java`）：

#### 节点和集群配置
| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `nodeId` | String | 随机 UUID | 节点唯一标识 |
| `mode` | String | `singleton` | 运行模式：`singleton` 或 `cluster` |
| `clusterInfo` | String | - | 集群成员信息（cluster 模式必需） |

#### 网络配置
| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `host` | String | `127.0.0.1` | 服务监听地址 |
| `port` | int | `6666` | 客户端端口 |
| `connectorHost` | String | - | 节点间通信监听地址 |
| `connectorPort` | int | `6665` | 节点间通信端口 |
| `bossThreads` | int | `1` | Boss EventLoopGroup 线程数 |
| `workerThreads` | int | `1` | Worker EventLoopGroup 线程数 |
| `backlog` | int | - | TCP backlog |
| `tcpNoDelay` | boolean | - | 是否禁用 Nagle 算法 |

#### 选举配置
| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `minElectionTimeout` | int | `3000` | 最小选举超时（毫秒） |
| `maxElectionTimeout` | int | `4000` | 最大选举超时（毫秒） |

#### 日志复制配置
| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `logReplicationDelay` | long | `1000` | 日志复制延迟（毫秒） |
| `logReplicationInterval` | long | `1000` | 日志复制间隔（毫秒） |
| `retryTimeout` | long | `900` | 重试超时（毫秒） |
| `maxTransferLogs` | int | `10000` | 单次最大传输日志条数 |
| `maxTransferSize` | int | `10240` | 单次最大传输字节数 |

#### 存储配置
| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `basePath` | String | - | 数据存储根目录 |
| `snapshotGenerateThreshold` | int | `10485760` | 快照生成阈值（字节，默认 10MB） |
| `synLogFlush` | boolean | `false` | 是否同步刷盘（false=异步） |
| `blockCacheSize` | int | `50` | 异步刷盘块缓存大小 |
| `blockSize` | int | `4194304` | 块大小（字节，默认 4MB） |
| `blockFlushInterval` | long | `1000` | 块刷盘间隔（毫秒） |
| `logIndexCacheSize` | int | `100` | 日志索引缓存大小 |

#### 缓冲区池配置
| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `linkedBuffPoolSize` | int | `16` | LinkedBuffer 池大小 |
| `byteBufferPoolSize` | int | `10` | ByteBuffer 池大小 |
| `byteBufferSizeLimit` | int | `16777216` | ByteBuffer 大小上限（字节，默认 16MB） |
| `useDirectByteBuffer` | boolean | `true` | 是否使用堆外内存 |

#### 空闲检测和 Lazy Flush 配置
| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `readIdleTimeout` | int | `0` | 读空闲超时（秒） |
| `writeIdleTimeout` | int | `0` | 写空闲超时（秒） |
| `allIdleTimeout` | int | `30` | 全空闲超时（秒） |
| `lazyFlushInterval` | long | `100` | Lazy flush 间隔（毫秒） |
| `lazyFlushThreshold` | double | `0.001` | Lazy flush 阈值（发送缓冲区比例） |

### 环境变量覆盖

以下系统属性可在启动时覆盖默认配置（通过 `-D` 参数传递）：

| 系统属性 | 对应配置项 |
|---------|-----------|
| `ENV_NODE_ID` | `nodeId` |
| `ENV_MODE` | `mode` |
| `ENV_HOST` | `host` |
| `ENV_PORT` | `port` |
| `ENV_CONNECTOR_HOST` | `connectorHost` |
| `ENV_CONNECTOR_PORT` | `connectorPort` |
| `ENV_MIN_ELECTION_TIMEOUT` | `minElectionTimeout` |
| `ENV_MAX_ELECTION_TIMEOUT` | `maxElectionTimeout` |
| `ENV_BASE_PATH` | `basePath` |
| `ENV_BOSS_THREADS` | `bossThreads` |
| `ENV_WORKER_THREADS` | `workerThreads` |

示例：
```bash
java -DENV_NODE_ID=node-A -DENV_PORT=9999 -jar server/target/server-1.0-jar-with-dependencies.jar
```

## 设计原理

### Raft 协议实现

#### 领导选举
1. **Follower**：接收 Leader 的心跳，超时后转为 Candidate，先发起 Pre-Vote
2. **Pre-Vote**：Candidate 先发起预投票，检查是否有可能获得多数票（防止网络分区后的无效选举）
3. **Candidate**：Pre-Vote 通过后，发起正式投票请求（RequestVote），获得多数票后转为 Leader
4. **Leader**：处理客户端请求，复制日志到 Followers，发送心跳维持领导地位

#### 日志复制
1. Leader 接收客户端请求，追加到本地日志
2. Leader 并行发送 AppendEntries RPC 给所有 Followers
3. Followers 检查 prevLogIndex/prevLogTerm 一致性，成功则复制日志
4. Leader 收到多数确认后提交日志（Leader 只能提交当前 term 的日志），通知 Followers 提交
5. 使用 QuickMatchHelper（二分查找）在新 Leader 选举后快速定位匹配的日志位置

#### 安全性保证
- **选举安全性**：每个任期最多只有一个 Leader（通过投票唯一性保证）
- **日志匹配**：AppendEntries 一致性检查确保日志连续性
- **状态机安全性**：所有节点以相同顺序应用相同日志
- **Leader 只提交当前 Term 的日志**：遵循 Raft 论文的安全性约束

### 成员变更（Joint Consensus）

集群成员变更通过联合共识实现，分为四个阶段：

1. **STABLE**：正常状态，所有节点使用同一配置
2. **SYNCING**：新节点加入，追赶日志到 Leader 状态（Leader 继续使用旧配置决策）
3. **OLD_NEW**：联合共识阶段，日志同时提交到新旧两个配置的多数组中
4. **NEW**：过渡到新配置，仅需新配置多数派确认

### 数据持久化

#### 文件布局
所有持久化元数据存储在 `node.metadata` 文件中（memory-mapped），包含：
- Node state（term, voteTo）
- Log file metadata（日志文件大小）
- Log index file metadata（索引文件大小）
- Snapshot file metadata（快照文件大小）

数据文件命名规则：`{term}_{index}.{type}`（如 `1_100.log`、`1_100.snapshot`）

#### 日志存储
- **日志文件**（`.log`）：存储日志条目（index, term, type, content），16 字节头部
- **索引文件**（`.index`）：存储日志元数据（index, term, type, offset），每条 20 字节
- **快照文件**（`.snapshot`）：压缩后的状态机数据

#### 日志类型
| 类型 | 值 | 说明 |
|------|---|------|
| NO_OP | 0 | 空操作日志，Leader 当选时追加 |
| SET | 1 | 键值写入日志 |
| OLD_NEW | 2 | 联合共识配置日志 |
| NEW | 3 | 新配置日志 |

#### 刷盘策略
- **同步刷盘**（`SyncLogFile`）：每次写入都刷盘，数据安全但性能较低
- **异步刷盘**（`AsyncLogFile`）：块缓存 + 批量刷盘，性能高但有少量数据丢失风险

#### 快照机制
- **自动生成**：当日志文件超过 `snapshotGenerateThreshold` 时自动生成快照
- **分块传输**：支持快照的分块传输（Leader 分块发送，Follower 通过 SnapshotBuilder 组装）
- **状态恢复**：节点重启时从快照恢复状态机数据

## 性能优化

### 1. 对象池
- **ByteBuffer 池**：重用 DirectByteBuffer / HeapByteBuffer，减少 GC 压力
- **LinkedBuffer 池**：重用 Protostuff 序列化缓冲区（FixedSizeLinkedBufferPool / FlexibleLinkedBufferPool）
- **ChannelPool**：重用网络连接，减少连接建立开销
- **RoleCache**：回收和复用角色实例（Follower、Candidate、Leader）

### 2. 缓存策略
- **LRU 缓存**：缓存热点数据
- **块缓存**：异步文件操作中的固定大小块缓存，减少磁盘 IO

### 3. 异步处理
- **异步刷盘**：通过 FlushStrategy（FIFO/Priority）批量刷盘，提高写入性能
- **异步网络**：Netty NIO 非阻塞 IO，提高并发处理能力
- **单线程串行化**：所有 Raft 状态变更在单线程 core executor 上串行执行，避免锁竞争

### 4. 网络优化
- **LazyFlushStrategy**：延迟批量刷盘，按时间间隔或缓冲区填充比例触发 flush
- **双通道架构**：客户端连接和节点间连接使用独立的 Netty pipeline

## Docker 部署

```bash
# 构建镜像
cd server
docker build -t raft-kv-store .

# 运行单节点容器
docker run -p 6666:6666 raft-kv-store
```

> **注意**：`server/Dockerfile` 中的基础镜像为 `openjdk:11`，可以按需升级为 `openjdk:21`。

## 监控和运维

### 日志配置
项目使用 Log4j 进行日志记录，支持以下日志级别输出到不同文件：
- **DEBUG**：调试信息（`app.log`）
- **INFO**：运行信息（`app.log`）
- **WARN**：警告信息（`app.log`）
- **ERROR**：错误信息（`app.log`）

日志使用 `DailyRollingFileAppenderWrapper`，每日自动滚动，JVM 退出时自动刷新缓冲区。

### 监控指标
- **节点状态**：角色（Leader/Follower/Candidate）、term、commitIndex、appliedIndex
- **集群信息**：leader ID、phase（STABLE/SYNCING/OLD_NEW/NEW）、集群成员配置
- **性能指标**：日志复制延迟、快照生成频率
- **存储指标**：日志文件大小、索引文件大小、快照文件大小

### 故障处理
1. **节点故障**：Follower 超时后自动发起选举，选出新 Leader
2. **网络分区**：多数派继续工作，少数派不可用；恢复后通过日志复制追赶
3. **数据损坏**：从快照恢复基础状态，再通过日志复制追赶增量数据
4. **Pre-Vote 保护**：网络分区节点恢复后不会发起无效选举，避免 term 无意义增长

## 客户端 API

> **注意**：客户端模块目前仍在开发中，以下是规划的 API 设计。

### 键值操作

```java
// 设置键值对（需要连接到 Leader）
client.set("key", "value");

// 获取键值（可连接到任意节点，非 Leader 会自动重定向）
String value = client.get("key");
```

### 集群管理

```java
// 获取集群信息
ClusterInfo info = client.getClusterInfo();

// 添加节点
client.addNode("D", "127.0.0.1", 9999, 9998);

// 移除节点
client.removeNode("D");
```

## 测试

### 单元测试
```bash
# 运行所有测试
mvn test

# 运行特定模块测试
mvn test -pl server
mvn test -pl core
```

### 集成测试
`server/src/test/` 包含完整的测试用例，覆盖以下场景：
- 命令序列化/反序列化
- 日志追加和读取
- 快照生成和应用
- 集群配置解析
- 数据管理器功能
- 端点匹配辅助功能

## 启动脚本参考

### start-singleton.sh
```bash
# 使用默认配置启动单节点
./start-singleton.sh

# 自定义节点参数
NODE_ID=my-node HOST=0.0.0.0 PORT=8080 ./start-singleton.sh

# 强制重新构建
./start-singleton.sh --build

# 自定义 JVM 参数
JAVA_OPTS="-Xms512m -Xmx2g" ./start-singleton.sh
```

### start-cluster.sh
```bash
# 启动 3 节点集群
./start-cluster.sh

# 强制重新构建后启动
./start-cluster.sh --build

# 查看集群状态
./start-cluster.sh status

# 停止集群
./start-cluster.sh stop
```

## 许可证

本项目基于 Apache License 2.0 许可证开源。

## 参考资源

1. [Raft 论文（扩展版）](https://raft.github.io/raft.pdf)
2. [Raft 官方网站](https://raft.github.io/)
3. [Netty 官方文档](https://netty.io/wiki/)
4. [Protostuff 文档](https://protostuff.github.io/docs/)
5. [Raft 成员变更实践](https://github.com/ongardie/dissertation)

---

**注意**：本项目是学习 Raft 协议和分布式系统的优秀实践项目，适用于学习和研究目的。在生产环境中使用前，请进行充分的测试和评估。
