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
|:---|:---|:---|:---|
| **Controller** | 参数校验、请求分发、组装统一返回体 `R` | `Service` | 编写业务逻辑、直接调用 Mapper、直接调用 Manager |
| **Service** | 业务规则编排、事务边界（`@Transactional`） | `Manager`、本表 `Mapper` | 跨表 Mapper 混乱调用、直接直连外部第三方系统 |
| **Manager** | 第三方服务防腐、通用能力下沉、跨 DAO 编排 | `Mapper`、外部服务 | 承载复杂业务规则、反向调用 Service |
| **Mapper** | SQL 映射与底层数据访问 | — | 编写业务逻辑、调用任何上层 |

---

## 2. 关键设计决策

### 决策 1：引入 Manager 层实现第三方防腐与解耦
- **传统 MVC 痛点**：在传统三层中，Service 层往往充斥着 HTTP/RPC 外部调用、网络异常重试和跨表查询，代码动辄上千行，且容易引发 Service 间的循环依赖。
- **架构方案**：引入 `Manager` 层承载外部服务调用（如 `DeptManager`）。
  1. 将外部系统的返回结构转换为内部模型（`DeptPayload` ➔ `DeptInfo`）；
  2. 将外部异常统一捕获并翻译为内部错误码（`ErrorCode.THIRD_PARTY_ERROR`）；
  3. 提供 `local`（配置/本地模拟）与 `remote`（真实 HTTP）双实现，实现环境快速平滑切换。

### 决策 2：DO（Data Object）禁止越界
- **设计初衷**：数据库表结构与前端交互展示存在本质差异。若直接将 DO 暴露给前端，会导致表字段暴露、冗余字段难以装配、甚至引发恶意字段篡改风险。
- **规约实施**：
  - 入参使用 `model/dto`；
  - 出参使用 `model/vo`（补充展示用冗余字段，如部门名）；
  - `model/dataobject/EmployeeDO` 仅在数据访问与 Service 内部流转，由 `ArchitectureTest` 在测试期强制断言 Controller 不得引用 DO。

### 决策 3：物理分页与安全线程池隔离
- **设计初衷**：`PageHelper` 依赖 `ThreadLocal` 传递分页参数，若在调用 `startPage` 后发生业务异常未执行 SQL，会导致该线程复用时污染后续无分页查询。
- **规约实施**：分页查询必须成对使用，`PageHelper.startPage` 紧跟查询操作，并在异常/边界时显式 `clearPage()` 防串号。

### 决策 4：Jackson 序列化 Long 转 String
- **设计初衷**：JavaScript `Number` 遵循 IEEE 754 双精度浮点标准，超过 $2^{53} - 1$（约 9007 万亿）的雪花 ID 或自增大整数在前端反序列化时会丢失低位精度。
- **规约实施**：在 `JacksonConfig` 全局配置 `Long.class` 序列化为 `ToStringSerializer`，前端接收到的 `id` 统一为 String 类型。
