# RaftKVStore - 基于Raft协议的分布式键值存储系统

## 项目概述

RaftKVStore 是一个基于 Raft 共识算法实现的高可用分布式键值存储系统。项目采用 Java 开发，使用 Maven 进行构建管理，实现了完整的 Raft 协议规范，包括领导选举、日志复制、成员变更和快照等功能。

## 项目特性

- **完整的 Raft 协议实现**：支持 Leader、Follower、Candidate 三种角色状态转换
- **高性能网络通信**：基于 Netty 框架实现高性能网络通信
- **高效序列化**：使用 Protostuff 进行对象序列化，性能优异
- **灵活的存储策略**：支持同步和异步两种日志刷盘方式
- **完善的快照系统**：支持日志压缩和状态机快照
- **集群管理**：支持动态成员变更和集群配置
- **高可用性**：自动故障转移和数据一致性保证

## 架构设计

### 模块结构

```
RaftKVStore/
├── core/          # 核心模块
│   ├── cmd/       # 命令定义和序列化
│   ├── constant/  # 常量定义
│   ├── exception/ # 异常定义
│   └── support/   # 通用支持类
├── server/        # 服务器模块
│   ├── config/    # 配置管理
│   ├── data/      # 数据存储层
│   │   ├── log/   # 日志存储
│   │   ├── index/ # 索引管理
│   │   └── snapshot/ # 快照系统
│   ├── group/     # 集群管理
│   ├── handler/   # 命令处理器
│   ├── message/   # 消息处理
│   ├── role/      # Raft角色实现
│   ├── scheduler/ # 调度器
│   └── support/   # 服务器支持类
└── client/        # 客户端模块（可选）
```

### 核心组件

#### 1. Node（节点）
- 管理节点的状态和角色转换
- 协调各个组件的工作
- 处理领导选举和日志复制

#### 2. DataManager（数据管理器）
- 管理日志存储、索引和快照
- 提供数据读写接口
- 处理日志压缩和快照生成

#### 3. StateMachine（状态机）
- 存储键值对数据
- 应用已提交的日志
- 生成和应用快照

#### 4. Cluster（集群）
- 管理集群成员
- 处理节点间通信
- 支持成员变更

#### 5. Server（服务器）
- 处理客户端请求
- 管理网络连接
- 实现请求转发

## 快速开始

### 环境要求

- Java 8 或更高版本
- Maven 3.6 或更高版本

### 构建项目

```bash
# 克隆项目
git clone <repository-url>
cd RaftKVStore

# 构建项目
mvn clean package
```

### 配置说明

项目使用 properties 文件进行配置，主要配置项包括：

#### 服务器配置 (server.properties)
```properties
# 节点配置
nodeId=A
mode=cluster
port=6666
connectorPort=6665

# 网络配置
bossThreads=1
workerThreads=1
host=127.0.0.1

# 选举配置
minElectionTimeout=3000
maxElectionTimeout=4000

# 日志复制配置
logReplicationDelay=1000
logReplicationInterval=1000
retryTimeout=900

# 存储配置
basePath=/path/to/data
snapshotGenerateThreshold=52428800
maxTransferLogs=30000
maxTransferSize=10485760

# 集群配置
clusterInfo=A,127.0.0.1,6666,6665 B,127.0.0.1,7777,7776 C,127.0.0.1,8888,8887
```

#### 运行模式
- **singleton**: 单节点模式，直接成为 Leader
- **cluster**: 集群模式，参与 Raft 协议

### 启动服务器

```bash
# 启动单个节点
java -jar server/target/server-1.0-SNAPSHOT-jar-with-dependencies.jar --config /path/to/server.properties

# 启动集群（需要分别启动多个节点）
# 节点A
java -jar server/target/server-1.0-SNAPSHOT-jar-with-dependencies.jar --config config/A/server.properties

# 节点B
java -jar server/target/server-1.0-SNAPSHOT-jar-with-dependencies.jar --config config/B/server.properties

# 节点C
java -jar server/target/server-1.0-SNAPSHOT-jar-with-dependencies.jar --config config/C/server.properties
```

## API 使用

### 键值操作

#### 设置键值对
```java
// 通过客户端设置键值对
client.set("key", "value");
```

#### 获取键值
```java
// 通过客户端获取键值
String value = client.get("key");
```

#### 集群信息
```java
// 获取集群信息
ClusterInfo info = client.getClusterInfo();
```

### 集群管理

#### 成员变更
```java
// 添加节点
client.addNode("D", "127.0.0.1", 9999, 9998);

// 移除节点
client.removeNode("D");
```

## 设计原理

### Raft 协议实现

#### 领导选举
1. **Follower**：接收 Leader 的心跳，超时后转为 Candidate
2. **Candidate**：发起投票请求，获得多数票后转为 Leader
3. **Leader**：处理客户端请求，复制日志到 Followers

#### 日志复制
1. Leader 接收客户端请求，追加到本地日志
2. Leader 并行发送 AppendEntries RPC 给所有 Followers
3. Followers 复制日志，成功后返回确认
4. Leader 收到多数确认后提交日志，通知 Followers 提交

#### 安全性保证
- **选举安全性**：每个任期最多只有一个 Leader
- **日志匹配**：Leader 的日志总是包含所有已提交的日志
- **状态机安全性**：所有节点以相同顺序应用相同日志

### 数据持久化

#### 日志存储
- **日志文件**：存储实际的日志条目
- **索引文件**：存储日志的元数据（位置、任期等）
- **快照文件**：压缩后的状态机数据

#### 刷盘策略
- **同步刷盘**：每次写入都刷盘，数据安全但性能较低
- **异步刷盘**：批量刷盘，性能高但有数据丢失风险

#### 快照机制
- **自动生成**：当日志大小超过阈值时自动生成快照
- **增量传输**：支持快照的分块传输
- **状态恢复**：节点重启时从快照恢复状态

## 性能优化

### 1. 对象池
- **ByteBuffer 池**：重用 ByteBuffer 对象，减少 GC 压力
- **LinkedBuffer 池**：重用 Protostuff 序列化缓冲区
- **Channel 池**：重用网络连接，减少连接建立开销

### 2. 缓存策略
- **LRU 缓存**：缓存热点数据，提高访问速度
- **块缓存**：缓存文件块，减少磁盘 IO

### 3. 异步处理
- **异步刷盘**：提高写入性能
- **异步网络**：非阻塞 IO，提高并发处理能力
- **线程池**：合理使用线程池，避免线程创建开销

## 监控和运维

### 日志配置
项目使用 Log4j 进行日志记录，支持以下日志级别：
- DEBUG：调试信息
- INFO：运行信息
- WARN：警告信息
- ERROR：错误信息

### 监控指标
- **节点状态**：角色、任期、提交索引等
- **性能指标**：QPS、延迟、吞吐量等
- **存储指标**：日志大小、快照大小、磁盘使用等

### 故障处理
1. **节点故障**：自动重新选举 Leader
2. **网络分区**：多数派继续工作，少数派不可用
3. **数据损坏**：从快照或日志中恢复数据

## 测试

### 单元测试
```bash
# 运行所有测试
mvn test

# 运行特定模块测试
mvn test -pl server
```

### 集成测试
项目包含完整的集成测试，覆盖以下场景：
- 领导选举
- 日志复制
- 快照生成和应用
- 成员变更
- 故障恢复

## 代码审查总结

### 优点
1. **架构清晰**：模块划分合理，职责分离明确
2. **设计模式应用得当**：工厂模式、策略模式、建造者模式等应用合理
3. **性能优化**：使用了对象池、缓冲区池、连接池等性能优化手段
4. **异常处理完善**：有完整的异常处理机制
5. **资源管理规范**：有明确的资源释放逻辑
6. **线程安全考虑**：在关键位置使用了同步机制

### 改进建议
1. **测试覆盖**：完善测试用例，提高测试覆盖率
2. **配置管理**：使用相对路径或环境变量替代硬编码路径
3. **代码抽象**：提取公共代码，减少重复
4. **文档完善**：补充更详细的使用文档和API文档
5. **监控集成**：集成更完善的监控和告警系统

## 许可证

本项目基于 Apache License 2.0 许可证开源。

## 贡献指南

1. Fork 项目仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

## 联系方式

如有问题或建议，请通过以下方式联系：
- 提交 Issue
- 发送邮件

## 参考资源

1. [Raft 论文](https://raft.github.io/raft.pdf)
2. [Raft 官方网站](https://raft.github.io/)
3. [Netty 官方文档](https://netty.io/wiki/)
4. [Protostuff 文档](https://protostuff.github.io/docs/)

---

**注意**：本项目为学习 Raft 协议和分布式系统的优秀实践，适用于学习和研究目的。在生产环境中使用前，请进行充分的测试和评估。