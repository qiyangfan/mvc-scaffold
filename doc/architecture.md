# mvc-scaffold 架构设计与技术决策说明

## 1. 架构全景

本项目遵循《阿里巴巴 Java 开发手册（黄山版）》推荐的**四层增强型 MVC 架构**规范：

```text
+-------------------------------------------------------------------+
|                        CONTROLLER 表现层                           |
|   (EmployeeController, R, GlobalExceptionHandler, OpenApiConfig)  |
+-------------------------------------------------------------------+
                                  │
                                  ▼
+-------------------------------------------------------------------+
|                          SERVICE 业务逻辑层                        |
|   (EmployeeService, EmployeeServiceImpl, 事务控制, 业务规则)        |
+-------------------------------------------------------------------+
                                  │
                                  ▼
+-------------------------------------------------------------------+
|                         MANAGER 通用业务层                         |
|   (DeptManager 防腐层, LocalDeptManager / RemoteDeptManager)       |
+-------------------------------------------------------------------+
                                  │
                                  ▼
+-------------------------------------------------------------------+
|                         MAPPER 数据访问层                          |
|   (EmployeeMapper, EmployeeMapper.xml, MyBatisConfig)             |
+-------------------------------------------------------------------+
```

### 1.1 分层职责与调用矩阵

| 层级 | 职责说明 | 允许调用 | 禁止行为 |
| :--- | :--- | :--- | :--- |
| **Controller** | 参数校验、请求分发、组装统一返回体 `R` | `Service` | 编写业务逻辑、直接调用 Mapper、直接调用 Manager |
| **Service** | 业务规则编排、事务边界（`@Transactional`） | `Manager`、本表 `Mapper` | 跨表 Mapper 混乱调用、直接直连外部第三方系统 |
| **Manager** | 第三方服务防腐、通用能力下沉、跨 DAO 编排 | `Mapper`、外部服务 | 承载复杂业务规则、反向调用 Service |
| **Mapper** | SQL 映射与底层数据访问 | — | 编写业务逻辑、调用任何上层 |

---

## 2. 关键设计决策

### 决策 1：引入 Manager 层实现第三方防腐与解耦

- **传统 MVC 痛点**：在传统三层中，Service 层往往充斥着 HTTP/RPC 外部调用、网络异常重试和跨表查询，代码动辄上千行，
  且容易引发 Service 间的循环依赖。
- **架构方案**：引入 `Manager` 层承载外部服务调用（如 `DeptManager`）。
  1. 将外部系统的返回结构转换为内部模型（`DeptPayload` ➔ `DeptInfo`）；
  2. 将外部异常统一捕获并翻译为内部错误码（`ErrorCode.THIRD_PARTY_ERROR`）；
  3. 提供 `local`（配置/本地模拟）与 `remote`（真实 HTTP）双实现，实现环境快速平滑切换。

### 决策 2：DO（Data Object）禁止越界

- **设计初衷**：数据库表结构与前端交互展示存在本质差异。若直接将 DO 暴露给前端，会导致表字段暴露、冗余字段难以装配、甚至引发恶意字段篡改风险。
- **规约实施**：
  - 入参使用 `model/dto`；
  - 出参使用 `model/vo`（补充展示用冗余字段，如部门名）；
  - `model/dataobject/EmployeeDO` 仅在数据访问与 Service 内部流转，
    由 `ArchitectureTest` 在测试期强制断言 Controller 不得引用 DO。

### 决策 3：物理分页与安全线程池隔离

- **设计初衷**：`PageHelper` 依赖 `ThreadLocal` 传递分页参数，若在调用 `startPage` 后发生业务异常未执行 SQL，
  会导致该线程复用时污染后续无分页查询。
- **规约实施**：分页查询必须成对使用，`PageHelper.startPage` 紧跟查询操作，
  并在异常/边界时显式 `clearPage()` 防串号。

### 决策 4：Jackson 序列化 Long 转 String

- **设计初衷**：JavaScript `Number` 遵循 IEEE 754 双精度浮点标准，超过 $2^{53} - 1$（约 9007 万亿）
  的雪花 ID 或自增大整数在前端反序列化时会丢失低位精度。
- **规约实施**：在 `JacksonConfig` 全局配置 `Long.class` 序列化为 `ToStringSerializer`，
  前端接收到的 `id` 统一为 String 类型。

### 决策 5：主键 UUID v7 应用侧生成（ADR-0006）

- **背景**：自增主键让"工号"这类业务编号必须依赖"先插入拿 ID 再回填"的二次 UPDATE，
  多实例唯一性还要另配雪花/号段；64 位 ID 出到前端又必须全局序列化成字符串。
- **裁决**：`common/support/Ids` 生成 UUID v7，Service 插入前即持有主键；工号由主键派生
  （`EMP` + UUID v7 去横线前 12 位）；Mapper 去掉 `useGeneratedKeys` 并删除 `updateEmpNo`。
- **收益**：创建路径从两次写变一次写；多实例唯一天然成立；时间有序使主键索引保持顺序插入。

### 决策 6：Flyway 单一 baseline 与 schema 纪律（ADR-0007）

- **背景**：原实现有 `schema.sql`（H2 初始化）与 `doc/sql/init.sql`（MySQL 人工执行）两份结构定义，
  靠人工保持同构，MySQL 侧无版本概念；状态列用 `TINYINT` + 注释，脏数据难以审计。
- **裁决**：结构变更只走 `db/migration/` 单一 baseline；主键 UUID v7 字符串、状态列 `VARCHAR + CHECK`
  与 `EmployeeStatus` 同源、时间列统一、已规划能力一次建全、DDL 双库兼容。
- **收益**：schema 成为有版本、有校验和、可回放的工件，CI 全链路测试顺带验证迁移可执行。

### 决策 7：关键配置 fail-closed（ADR-0006 配套）

- **背景**：`dept.provider` 这类开关历史上是散落在两个 Manager 的 `@ConditionalOnProperty` 上的字符串，
  没有任何地方声明"合法取值有哪些"，拼写错误被静默忽略并一路带到生产。
- **裁决**：`dept.provider` 绑定 `DeptProviderMode` 枚举；路径主键声明为 `UUID`、状态过滤直接绑枚举，
  非法值在启动期或进入 Service 前即失败。
- **收益**：配置错误从"生产事故"提前到"启动即失败"。

### 决策 8：API 契约单一来源与 CI 漂移门禁（ADR-0009）

- **背景**：前后端契约靠口头约定与各写一份类型维持，漂移在联调甚至上线后才暴露。
- **裁决**：OpenAPI schema 为唯一事实源，`contracts/openapi.json` 由测试导出并入库，CI 用
  `git status --porcelain contracts/` 阻断漂移；前端类型一律由该文件生成。
- **收益**：契约漂移从"联调期人肉发现"变成"CI 期自动阻断"，零额外基础设施。
