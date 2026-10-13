# AGENTS.md — mvc-scaffold 架构宪法与执行者工作契约

> **项目定位**：标准工业级增强型 MVC 架构脚手架（对齐《阿里巴巴 Java 开发手册（黄山版）》推荐的 Controller → Service → Manager → Mapper 四层架构）。专用于简单 CRUD、业务流转确定、管理中后台等业务系统，通过 ArchUnit 架构守护测试与 Spotless/PMD/SpotBugs/JaCoCo 质量门禁将分层纪律固化至 CI。

---

## 一、架构宪法（不可逾越的六条红线，违背即打回）

1. **分层严格单向调用铁律**
   - 依赖链路严格单向：`Controller ──> Service ──> Manager ──> Mapper`。
   - `Controller`：只负责 HTTP 接入、参数校验、转发、组装统一返回体 `R`。**严禁写业务逻辑，严禁绕过 Service 直连 Manager 或 Mapper**。
   - `Service`：负责核心业务规则编排、事务边界（`@Transactional`）。可以调用 Manager 与本表 Mapper，**禁止跨表 Mapper 杂乱混调，禁止直接调用第三方外部系统**。
   - `Manager`：通用业务处理层。负责第三方平台调用封装（防腐层）、异常翻译、通用能力（缓存/复杂多 DAO 编排）下沉。**只对 Service 提供能力，不写业务规则**。
   - `Mapper`：纯粹的数据访问层，只负责 SQL 映射与数据存取。
   - **门禁执行**：`ArchitectureTest` 通过 ArchUnit 在 CI 阶段强制执行依赖规则，越界直接构建失败。

2. **数据对象（DO）严禁外泄铁律**
   - `EmployeeDO` 严格映射数据库表结构，生命周期严格封闭在数据访问与 Service 内部，**严禁穿透 Service 层暴露给 Controller 或前端**。
   - 对外入参使用 `DTO`（配合 JSR-303 校验），对外出参使用 `VO`（补充展示冗余字段）。
   - **门禁执行**：`ArchitectureTest.doShouldNotLeakToController` 强制断言 Controller 不得依赖 `model.dataobject` 包。

3. **第三方调用必走 Manager 防腐铁律**
   - 任何对外部服务（HTTP、RPC、第三方开放平台）的调用，**必须抽象为 Manager 接口并在 Manager 内部实现**（如 `DeptManager`）。
   - Manager 必须负责对外部返回模型做本地转换（如 `DeptPayload` ➔ `DeptInfo`），并对外部网络超时、失败进行异常捕获与翻译（如 `ErrorCode.THIRD_PARTY_ERROR`）。
   - 必须显式设置网络连接与读取超时时间（`setConnectTimeout` / `setReadTimeout`），严禁无超时的外部调用拖垮线程池。

4. **事务与持久化规范铁律**
   - 数据写操作必须由 Service 声明 `@Transactional(rollbackFor = Exception.class)`。
   - SQL 映射严禁使用 `SELECT *`，必须显式指明查询字段。
   - 严禁在 SQL 中使用 `${}` 拼接参数，必须使用 `#{}`。
   - 分页必须使用物理分页，`PageHelper.startPage` 紧跟查询，且必须在 `finally` 中调用 `clearPage()` 防范线程池串号污染。

5. **多实例部署无状态铁律**
   - 应用进程必须保持完全无状态：严禁使用 `HttpSession` 暂存状态，严禁使用本地文件作为跨实例共享存储。
   - 本地缓存仅限只读不变量；共享状态走 Redis 分布式缓存；定时任务必须挂载分布式锁（Redisson/ShedLock）。
   - 状态流转写接口按需实现幂等防护（唯一索引 / CAS 状态变更）。

6. **质量门禁 100% 全绿铁律**
   - 本地提交前必须执行 `mvn clean verify -DskipTests=false`。
   - **Spotless** 代码格式、**PMD** 静态分析（0 violation）、**SpotBugs** 字节码缺陷（0 bug）、**JaCoCo** 测试覆盖率（Instruction ≥ 70%, Branch ≥ 50%）、**ArchUnit** 架构测试必须全部通过。

---

## 二、代码与模型规范（黄山版规约）

### 1. 语言规范
- **默认语言 = 简体中文**：面向开发者的对话、解释、代码注释（Javadoc/行注释）、技术文档、Git Commit Message、PR 描述必须使用简体中文。
- **英文硬性规范**：业务错误码信息（ErrorMessage）、DTO 校验信息（`@NotNull(message = "...")`）、HTTP 响应提示、日志文本（`log.info/error`）、代码标识符（类名/变量名/方法名/表名）一律使用英文。

### 2. 对象分层转换规范
- **禁止反射拷贝**：严禁使用 `BeanUtils.copyProperties`，所有对象转换必须手写显式映射，避免静默属性丢失与安全隐患。
- 精度防护：全局配置 Jackson 将 `Long` 序列化为 `String`，防范前端 JS 处理 64 位大整数时的精度截断问题。

### 3. Java 核心零容忍规则
- 条件分支 `if`/`else`/`for`/`while` 即使单行也必须使用大括号 `{}`。
- `switch` 必须包含 `default` 分支。
- 严禁魔法值，所有状态必须使用预定义枚举（如 `EmployeeStatus`）。
- 判空防 NPE：字符串使用 `org.apache.commons.lang3.StringUtils`；集合判空使用 `CollectionUtils`。
- 日志严禁拼接字符串，必须使用占位符 `{}`；捕获异常并抛出或记录日志时，必须保留原始异常 `e` 根因。
- 严禁使用 `System.out.println` 或 `e.printStackTrace()`。

---

## 三、四步验证流程（每次修改必跑）

1. **编译验证**：`mvn compile test-compile` 确认语法与注解处理器零告警。
2. **测试与门禁执行**：`mvn clean verify -DskipTests=false` 完整跑通所有单元测试、集成测试、Spotless、PMD、SpotBugs、JaCoCo 与 ArchUnit。
3. **代码审查**：内置 `/code-review` 审查改动（安全变更加 `/security-review`），核查是否违背增强 MVC 六条宪法红线。
4. **策略验证**：核验测试覆盖充分，`git status` 确认无遗留废弃文件。

---

## 四、高频踩坑清单（动手前必读）

- **坑 1**：在 Controller 中为了“省事”直接调用了 `EmployeeMapper` 或 `DeptManager`。
  - *后果*：`ArchitectureTest.layerDependencyRule` 直接报错阻断构建。
  - *解法*：Controller 只能调用 Service，跨层调用必须下沉。
- **坑 2**：在 Controller 的响应方法中直接返回了 `EmployeeDO`。
  - *后果*：`ArchitectureTest.doShouldNotLeakToController` 拦截失败。
  - *解法*：在 Service 或 Controller 组装为 `EmployeeVO` 返回。
- **坑 3**：修改了代码格式导致 Spotless 拦截失败。
  - *解法*：直接执行 `mvn spotless:apply` 一键自动修复格式。
