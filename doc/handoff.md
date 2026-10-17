# mvc-scaffold 交接说明（Handoff）

## 状态

已交付 — 2026-10-17。24 个测试全绿，七道门禁全过，已推送 `main`，CI 绿灯。

## 本次变更集做了什么

| # | 事项 | 落点 |
| --- | --- | --- |
| 1 | JDK 21 → 25 单一基线 | `pom.xml` + `ci.yml` |
| 2 | 质量门禁工具链升级 | `pom.xml` |
| 3 | 主键 UUID v7 + 工号派生 | `common/support/Ids.java` |
| 4 | 状态列 `VARCHAR + CHECK` | `EmployeeStatus` + `V1__baseline.sql` |
| 5 | Flyway 单一 baseline | `db/migration/` |
| 6 | API 契约入库 + 漂移门禁 | `contracts/` + `OpenApiContractTest` |
| 7 | 配置 fail-closed + 防腐层可测化 | `DeptProperties` / `DeptRemoteConfig` |
| 8 | 文档集规范化 + markdownlint 门禁 | `doc/` + `.markdownlint.json` |

## 关键决策与依据

- **ADR-0006** 主键 UUID v7：主键与工号在插入前即确定，**删掉了"先 insert 拿自增 ID
  再 `updateEmpNo` 回填工号"的二次写**；工号由主键派生（`EMP` + UUID v7 前 12 位十六进制）。
- **ADR-0007** Flyway 单一 baseline：schema 只走迁移，禁双脚本。
- **ADR-0008** JDK 25 与门禁升级：四个真实门槛，详见 `ddd-scaffold` 同名 ADR。
- **ADR-0009** 契约单一来源：CI 用 git diff 阻断漂移。

### 配置 fail-closed（ADR-0006 配套）

`dept.provider` 原先是散落在两个 Manager 的 `@ConditionalOnProperty` 上的字符串，
**没有任何地方声明合法取值有哪些**，拼成 `foo` 这类值只会静默回落到默认实现。
现已建模为 `DeptProviderMode` 枚举，非法取值在启动期即失败。

同理，路径主键声明为 `UUID`、状态过滤直接绑 `EmployeeStatus` 枚举，
非法值在进入 Service 前就被转成 400，不带着脏值查库。

## 防腐层的可测化改造（本次结构性变更）

`RemoteDeptManager` 原先自己 `new SimpleClientHttpRequestFactory()` 并构造 `RestClient`——
业务类不该知道 HTTP 管道参数的存在，这也是它无法单测的原因。现已拆开：

- `DeptRemoteConfig`：装配带超时（连接 2s / 读取 3s）的 `RestClient` bean，
  条件与 Manager 一致，避免 local 模式下因 `base-url` 未配置而起不来；
- `RemoteDeptManager`：收窄为纯构造注入；
- `RemoteDeptManagerTest`：7 个用例，`MockRestServiceServer` 拦截 HTTP，不起真服务；
- `DeptRemoteWiringTest`：remote 模式上下文冒烟测试——
  常规集成测试跑 local profile，**根本不会走到这条装配路径**，
  配置类写错只会在切 remote 模式那天才炸。

## 质量门禁（七道，缺一不可）

```bash
mvn clean verify -DskipTests=false -Dcontract.export=true
npx --yes markdownlint-cli2@0.23.3 --config .markdownlint.json "**/*.md"
```

| 门禁 | 拦什么 |
| --- | --- |
| Spotless 3.10.4 | 格式 |
| PMD 3.28.0（内置 PMD 7.17.0） | 静态缺陷 |
| SpotBugs 4.10.5.0 | 字节码缺陷 |
| JaCoCo 0.8.15 | Instruction ≥ 70%、Branch ≥ 50% |
| ArchUnit 1.5.1 | 分层方向、DO 不外泄、无环 |
| markdownlint 0.23.3 | 文档结构与行宽 |
| 契约漂移 | `git status --porcelain contracts/` 必须为空 |

当前覆盖率：LINE 94%、BRANCH 73%。

## 遗留待办（按优先级）

1. **`EmployeeStatus.descOf()` 对未知编码返回兜底文案"未知状态"**，而 `ddd-scaffold` 的
   `OrderStatus.of()` 是抛异常。两脚手架口径不一致——本项目只用于展示字段故可接受，
   若要把 fail-closed 贯彻到底，应改为抛异常并在 VO 组装处处理。
2. **`infinite-recursion` 静态告警是误报**：`R.ok()` 调 `ok((Void) null)` 是重载派发。
   若升 ast-grep 为门禁需配 suppress，不要改生产代码。
3. **sqlfluff 报 `V1__baseline.sql` 无法解析**：方言未声明，需 `dialect = mysql`。
4. **jscpd 会报与 `ddd-scaffold` 大量重复**：两个脚手架是独立仓库、必须各自成立，
   重复是设计意图。

## 接手后第一件事

先跑一遍上面两条命令确认门禁全绿，再读 `doc/adr/ADR-0006` 与
`doc/architecture.md` 的"防腐层可测化"一节。

## 相关文档

- `doc/architecture.md`：四层边界、调用规则与关键设计决策
- `doc/conventions.md`：编码规约与七道门禁的修复命令
- `doc/adr/`：ADR-0001 ~ ADR-0009
- `doc/sql/README.md`：Flyway 工作流与 schema 纪律
- `doc/handoff.md`：本文件
