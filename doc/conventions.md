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
  - 字符串处理必须使用 `org.apache.commons.lang3.StringUtils`（`isNotBlank` / `isBlank`）；严禁使用 `org.springframework.util.StringUtils` 或手动编写 `str == null || str.isEmpty()`。
  - 集合判空必须使用 `CollectionUtils.isEmpty()` / `isNotEmpty()`。
  - 集合方法返回空时，必须返回 `Collections.emptyList()`，严禁返回 `null`。
- **相等判断防空**：使用 `Objects.equals(a, b)` 或常量放左侧，禁止在未知对象上直接调用 `.equals()`。

### 2. 事务与数据库规约
- **事务回滚声明**：`@Transactional` 必须显式声明 `rollbackFor = Exception.class`，防止受检异常跳过回滚。
- **SQL 与 MyBatis**：
  - 严禁使用 `SELECT *`，必须显式书写需要查询的列名。
  - 参数绑定必须使用 `#{}`，严禁使用 `${}` 防止 SQL 注入。
  - 逻辑删除字段命名为 `deleted`（0 正常，1 删除），查询时显式携带 `deleted = 0`。

### 3. 日志与异常规约
- **日志输出规范**：
  - 日志文本必须使用英文（面向运维监控与国际化）。
  - 必须使用参数占位符 `{}`，严禁使用字符串 `+` 拼接日志。
  - `log.error` 记录异常时，最后一个参数必须传递异常对象 `e`，以便打印完整堆栈。
  - 严禁使用 `System.out.println`、`System.err.println` 或 `e.printStackTrace()`。
- **异常捕获与翻译**：
  - 严禁捕获异常后吞掉。
  - 自定义异常必须保留原始根因（`new BizException(code, e)`），禁止断裂调用栈。

---

## 二、质量门禁准出标准（Quality Gates）

本项目配置了五道自动化质量门禁，执行命令：

```bash
mvn clean verify -DskipTests=false
```

| 工具 | 门禁类型 | 阈值 / 规则 | 修复命令 / 处理方案 |
|:---|:---|:---|:---|
| **Spotless** | 格式化规范 | 严格 4 空格缩进、去除无用 import、尾部换行 | `mvn spotless:apply` 一键自动格式化 |
| **PMD** | 静态代码分析 | 0 Violation（优先排查并发、潜在空指针、异常丢失） | 查看 `target/pmd.xml` 针对性修复代码 |
| **SpotBugs** | 字节码缺陷扫描 | 0 Bug（Effort=Max, Threshold=Medium） | 查看 `target/spotbugs.xml` 修复潜在缺陷 |
| **JaCoCo** | 单元测试覆盖率 | Instruction ≥ 70%, Branch ≥ 50% | 为未覆盖分支补充针对性单测用例 |
| **ArchUnit** | 架构守护测试 | 100% 规则通过（四层依赖方向、DO 不得外泄） | 检查类所属包及引用方向 |

---

## 三、Git 提交与发布规范

- **提交信息必须中文**：所有 `git commit -m` 消息使用中文撰写，格式对齐 Conventional Commits：
  - `feat(employee): 新增员工入职流程及部门有效性校验`
  - `fix(dept): 修复远程部门服务超时异常未保留根因的问题`
  - `test(arch): 补充 DO 不得被 Controller 调用的架构断言测试`
  - `refactor(manager): 下沉跨表员工统计通用逻辑至 Manager 层`
