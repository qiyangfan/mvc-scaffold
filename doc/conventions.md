# mvc-scaffold 工程编码与质量门禁规范 (Conventions)

本文档是 `mvc-scaffold` 的详细工程规范。所有代码变更必须严格遵守，CI 质量门禁会自动执法。

---

## 一、代码风格与编程规范（对齐阿里黄山版）

### 1. 结构与语法

- **分支大括号**：`if`、`else`、`for`、`while`、`do` 必须使用大括号 `{}`，即使只有单行。
- **Switch 分支**：`switch` 语句必须包含 `default` 分支，即使为空或抛出异常。
- **禁止魔法值**：代码中禁止出现未经预定义的字面量数字或字符串；必须使用枚举或常量类（如 `EmployeeStatus`）。
- **类型使用规约**：
  - POJO / DO / DTO / VO 属性必须使用包装类型（如 `Long`、`Integer`、`BigDecimal`），防止拆箱 NPE；
  - 方法内部的高频局部变量推荐使用基本类型。
- **集合与字符串判空**：
  - 字符串处理必须使用 `org.apache.commons.lang3.StringUtils`（`isNotBlank` / `isBlank`）；
    严禁使用 `org.springframework.util.StringUtils` 或手动编写
    `str == null || str.isEmpty()`。
  - 集合判空必须使用 `CollectionUtils.isEmpty()` / `isNotEmpty()`。
  - 集合方法返回空时，必须返回 `Collections.emptyList()`，严禁返回 `null`。
- **相等判断防空**：使用 `Objects.equals(a, b)` 或常量放左侧，禁止在未知对象上直接调用 `.equals()`。

### 2. 事务与数据库规约

- **事务回滚声明**：`@Transactional` 必须显式声明 `rollbackFor = Exception.class`，防止受检异常跳过回滚。
- **状态类写必须写时仲裁**：Service 前置校验的不变量（如离职冻结）必须在 UPDATE 的 WHERE 子句
  复核（如 `AND status != 冻结态`），按受影响行数判定，0 行即冲突抛业务码；
  纯 check-then-act 在并发下会被绕过。
- **SQL 与 MyBatis**：
  - 严禁使用 `SELECT *`，必须显式书写需要查询的列名。
  - 参数绑定必须使用 `#{}`，严禁使用 `${}` 防止 SQL 注入。
  - 逻辑删除字段命名为 `deleted`（0 正常，1 删除），查询时显式携带 `deleted = 0`。
- **主键（ADR-0006）**：
  - 统一 UUID v7 字符串，由 `common/support/Ids.newId()` 在 Service 插入前生成；
  - 严禁新增自增主键，严禁 `useGeneratedKeys`，严禁"插入后回填"的二次 UPDATE；
  - 工号等业务编号由主键派生或独立生成，不得依赖数据库自增序列。
- **Schema 纪律（ADR-0007）**：
  - 结构变更只走 Flyway 迁移：baseline 为 `V1__baseline.sql`，新增迁移一律用创建时刻时间戳
    `VyyyyMMddHHmmss__{描述}.sql`（并行分支天然免撞号），**已发布迁移禁止改内容**；
  - 状态列一律 `VARCHAR + CHECK`，取值清单与 `EmployeeStatus` 严格一致；严禁 `TINYINT` 数字码 + 注释；
  - 时间列统一 `gmt_create / gmt_modified`；已规划能力（有 spec 的）建表时一次到位，不留追补迁移；
  - DDL 保持 H2(MySQL 模式) 与 MySQL 双兼容，不写引擎/字符集/表级 COMMENT 等方言子句。
- **平台级上限单一来源**：分页上限等底线参数只定义在 `common/api/PageQuery` 的常量上，
  校验注解与任何兜底钳制必须引用同一常量，禁止两处各写一份字面量。

### 2.1 配置与凭证规约

- **关键配置 fail-closed**：启动缺失即失败的配置必须让应用**起不来**（`@ConfigurationProperties` +
  强类型/枚举绑定），严禁"取不到就静默回落默认值"——静默默认值是把配置错误推迟到生产事故。
- **凭证治理**：密钥/令牌只走环境变量或配置中心，严禁入库、严禁进日志、严禁硬编码；
  具备可轮换性（轮换不需改代码）；对外部系统的调用凭证只存在于 `manager` 防腐层内。

### 3. 日志与异常规约

- **日志输出规范**：
  - 日志文本必须使用英文（面向运维监控与国际化）。
  - 必须使用参数占位符 `{}`，严禁使用字符串 `+` 拼接日志。
  - `log.error` 记录异常时，最后一个参数必须传递异常对象 `e`，以便打印完整堆栈。
  - 严禁使用 `System.out.println`、`System.err.println` 或 `e.printStackTrace()`。
- **异常捕获与翻译**：
  - 严禁捕获异常后吞掉。
  - 自定义异常必须保留原始根因（`new BizException(code, e)`），禁止断裂调用栈。

### 4. 时间语义（Clock 注入）

- 业务时间一律取注入的 `java.time.Clock`：Service 经构造注入取时刻（生产装配见 `ClockConfig`），
  测试替换为固定时钟；SQL 写入的时间值由应用显式提供，禁止 `CURRENT_TIMESTAMP` 与应用时钟混用。
- Service 层禁止直呼 `LocalDateTime.now()` / `Instant.now()` / `System.currentTimeMillis()`，
  由 `ArchitectureTest.businessTimeMustComeFromInjectedClock` 执法。
- 身份生成（`common/support/Ids`）需要真实单调时间戳，属身份时间而非业务时间，不在禁令射程。

### 5. 外部事实纪律（来源故障 ≠ 确认事实）

- Manager 读取外部事实必须区分三种结果：确认命中、确认未命中、不可判定（来源故障）。
  超时、5xx、空响应体、非成功业务码属不可判定，翻译为 `THIRD_PARTY_ERROR`，
  不得伪装成「不存在」等确认事实。
- 批量读取的容错只允许发生在确认未命中时（如部门确认裁撤则跳过并告警）；
  来源故障必须上抛，不得静默吞掉，fail 方向默认 fail-closed。

---

## 二、质量门禁准出标准（Quality Gates）

本项目配置了七道自动化质量门禁，执行命令：

```bash
mvn clean verify -DskipTests=false
# 改了接口代码，提交前必须同步契约（CI 会做漂移门禁）
mvn clean verify -DskipTests=false -Dcontract.export=true

# 文档门禁：规则见 .markdownlint.json；CI 用同一条命令拦。
# glob 必须加引号，否则 bash 把 ** 当单层 *，会漏掉 README.md 与 doc/adr/*.md
npx --yes markdownlint-cli2@0.23.3 --config .markdownlint.json "**/*.md"
```

| 工具 | 门禁类型 | 阈值 / 规则 | 修复命令 / 处理方案 |
| :--- | :--- | :--- | :--- |
| **Spotless (3.11.0)** | 格式化规范 | 严格 4 空格缩进、去除无用 import、尾部换行 | `mvn spotless:apply` 一键自动格式化 |
| **PMD (3.28.0 / PMD 7.17.0)** | 静态代码分析 | 0 Violation（优先排查并发、潜在空指针、异常丢失） | 查看 `target/pmd.xml` 针对性修复代码 |
| **SpotBugs (4.10.5.0)** | 字节码缺陷扫描 | 0 Bug（Effort=Max, Threshold=Medium） | 查看 `target/spotbugs.xml` 修复潜在缺陷 |
| **JaCoCo (0.8.16)** | 单元测试覆盖率 | Instruction ≥ 70%, Branch ≥ 50% | 为未覆盖分支补充针对性单测用例 |
| **ArchUnit (1.5.1)** | 架构守护测试 | 100% 规则通过（四层依赖方向、DO 不得外泄、业务墙钟禁令） | 检查类所属包及引用方向 |
| **markdownlint (0.23.3)** | 文档结构与行宽 | 标题/列表/表格空行、表格分隔行风格、围栏须标语言、正文行宽 ≤ 80 | `npx --yes markdownlint-cli2@0.23.3 --config .markdownlint.json "**/*.md"` 定位后手工修 |
| **契约漂移** | 接口契约一致性 | `git status --porcelain contracts/` 必须为空 | `mvn clean verify -DskipTests=false -Dcontract.export=true` 后提交 `contracts/openapi.json` |

> **JDK 25 专项注意（ADR-0008）**：JDK 24+ 起 javac 不再默认从 classpath 发现注解处理器，
> Lombok 失效时不会直接报错，只会报"变量未初始化"这类间接错误。`maven-compiler-plugin` 的
> `annotationProcessorPaths` 是必需配置，新增注解处理器必须在此登记。

---

## 三、测试准入与断言纪律

测试是回归网不是变更锁：断言的价值是「改坏核心逻辑时红」，不是「改任何东西都红」。

- **准入**：只有核心逻辑（业务规则、状态冻结）、核心链路（HTTP 全链路、防腐翻译）、
  易错面（边界值、非法输入、并发仲裁、时间语义、失败路径与降级方向）才值得写用例。
- **期望值只允许是契约或行为结果**：错误码、响应 payload 结构、业务结果与状态转移、
  fail-closed 方向、非法输入被拒。
- **禁止断言**：UI/错误提示文案、日志级别与措辞、内部调用序列、纯 getter/setter 往返。
  文案与日志微调不需要业务评审，不得被断言钉死。
- **例外**：被测对象本身是日志/观测基础设施时，按组件行为测试。

---

## 四、Git 提交与发布规范

- **提交信息必须中文**：所有 `git commit -m` 消息使用中文撰写，格式对齐 Conventional Commits：
  - `feat(employee): 新增员工入职流程及部门有效性校验`
  - `fix(dept): 修复远程部门服务超时异常未保留根因的问题`
  - `test(arch): 补充 DO 不得被 Controller 调用的架构断言测试`
  - `refactor(manager): 下沉跨表员工统计通用逻辑至 Manager 层`

> **行宽规则的边界**：`MD013` 对表格行与代码块豁免（见 `.markdownlint.json`）——表格行无法折行，代码折了就是破坏代码。
这不是放松标准，是两类内容在结构上不可能满足 80 列。
