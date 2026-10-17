# ADR-0008: JDK 25 单一基线与质量门禁工具链升级

## 状态

已接受 (Accepted) — 2026-10

## 上下文 (Context)

脚手架原基线为 JDK 21，README 声称"CI 矩阵覆盖 21/25"，但 `ci.yml` 实际只装 JDK 21——文档与实现漂移。
2026-10 时点，Java **25 已是当前唯一新 LTS**（26/27 为特性版），生态
（Spring Boot 4.1.1 / Spring AI 2.0 / MyBatis 4.1 / AgentScope Java）均已支持。

从 21 升 25 不是改一个版本号，实测踩到四个真实门槛，全部已在本次升级中解决：

| # | 门槛 | 现象 | 处置 |
| --- | --- | --- | --- |
| 1 | **JaCoCo 版本** | 0.8.12 只官方支持到 Java 22，跑在 25 上覆盖率统计失真 | 升 0.8.14+（现 0.8.15） |
| 2 | **javac 注解处理器发现机制** | JDK 24+ 起 javac 不再默认从 classpath 发现处理器，Lombok **静默失效**——getter/构造器不生成，构建却只报"变量未初始化"这类间接错误 | `maven-compiler-plugin` 显式声明 `annotationProcessorPaths` |
| 3 | **PMD 大版本** | 3.26.0 → 3.28.0 内置 PMD 6 → **7.17.0**，规则收紧，一次报出 4 个真实违规 | 逐个修复（见下） |
| 4 | **Boot 4 Flyway 模块化** | Flyway 自动配置移出 `spring-boot-autoconfigure` | 显式依赖 `spring-boot-flyway`（见 ADR-0007） |

第 3 项修掉的违规本身也是质量收益，值得记录：

- `Money` 实现 `Comparable` 却没覆写 `equals/hashCode` → 补上，并显式按"归一化后金额"比较
  （避开 `BigDecimal.equals` 比较标度导致 `1.0 ≠ 1.00` 的坑）；
- `Ids.uuidV7` 用 `synchronized` 语句 → 改 `ReentrantLock`；条件里赋值 → 重构消除；
- `ProductGateway` 隐式函数式接口 → 显式 `@FunctionalInterface`，声明这是稳定能力契约。

## 决策 (Decision)

1. **JDK 25 单一基线**：源码/字节码/运行时统一 25，CI 只跑 25；README 的"21/25 矩阵"说法按实际实现修正；
2. 门禁工具链同步升级：JaCoCo **0.8.15**、ArchUnit **1.5.1**、
   SpotBugs **4.10.5.0**、PMD **3.28.0**、Spotless **3.10.4**；
3. `maven-compiler-plugin` 显式声明 `annotationProcessorPaths`（Lombok），
   这是 JDK 24+ 的必需项而非可选项；
4. 下游项目若因合规必须留在 21：改 `maven.compiler.release=21` 即可，领域与基础设施代码不依赖 25 专有 API。

## 后果 (Consequences)

### 正向收益

- 拿到分代 ZGC、更成熟的虚拟线程与 CDS/AOT 启动改进；
- 门禁工具链全部跟上 JDK，覆盖率与静态扫描结论可信；
- 顺手消掉 4 个真实代码质量问题。

### 代价与缓解

- 下游若锁死 21 运行时，需显式降 `release`：一次编译参数改动；
- PMD 7 规则更严，存量代码接入时可能一次报多个违规：按"修复而非豁免"处理，确需豁免才在
  `pmd-ruleset.xml` 显式排除并注明原因。

## 备选方案（已否决）

- 维持 21、把 README 的"21/25 矩阵"改成"仅 21"：回避生态现实，且下游新项目仍要各自趟一遍这四个坑，否决；
- 21/25 双矩阵：同一份源码在 25 上编译后无法在 21 运行时加载（class file 69），双矩阵只会给出虚假的安全感，否决。
