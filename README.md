# mvc-scaffold 增强 MVC 架构脚手架

简单 CRUD、流程固定的业务系统首选骨架：**Controller → Service → Manager → Mapper** 四层，编码约束对齐《Java 开发手册（黄山版）》。与 [ddd-scaffold](../ddd-scaffold) 二选一，选型边界见文末。

## 技术栈（2026-09 裁决版本）

| 组件 | 版本 | 说明 |
|------|------|------|
| JDK | 17（LTS） | Boot 4 基线，可平滑升 21/25 |
| Spring Boot | 4.1.1 | 3.5 线已于 2026-06-30 OSS 停更，新项目不再钉死 |
| spring-boot-starter-webmvc | Boot 管理 | Boot 4 起 `starter-web` 更名为 `starter-webmvc` |
| MyBatis | spring-boot-starter 4.1.0 | Boot 4 对齐线 |
| PageHelper | spring-boot-starter 4.1.1 | 物理分页 |
| springdoc-openapi | 3.1.1 | 访问 `/swagger-ui.html` |
| ArchUnit | 1.5.0（test） | 四层增强 MVC 依赖规则在测试期强制执行 |
| Spotless | 2.44.3 | 代码格式化门禁，支持 `mvn spotless:apply` 一键排版 |
| SpotBugs | 4.8.6.6 | 字节码静态缺陷扫描门禁 |
| PMD | 3.26.0 | 代码规范与潜在缺陷静态检查门禁 |
| JaCoCo | 0.8.12 | 单元测试指令与分支覆盖率门禁 |
| H2 / MySQL | Boot 管理 | 默认 H2（MySQL 模式）开箱即跑，生产切 MySQL |

## 快速开始

```bash
mvn test -DskipTests=false          # 全量测试（父 POM 若配置 skipTests 需强制打开）
mvn spring-boot:run                 # 默认 H2 内置库，直接可跑
# 生产/联调：先执行 doc/sql/init.sql，再
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

启动后：接口文档 <http://localhost:8080/swagger-ui.html>，H2 控制台 `/h2-console`（JDBC URL `jdbc:h2:mem:mvc_scaffold;MODE=MySQL`）。

## 分层与调用规则

```
Controller ──> Service ──> Manager ──> Mapper
   api/v1        业务规则     防腐/下沉     数据访问
```

| 层 | 职责 | 可以调 | 禁止 |
|----|------|--------|------|
| Controller | 参数校验、转发、组装 R | Service | 业务逻辑、直接调 Mapper/Manager |
| Service | 业务规则、事务边界 | Manager、本表 Mapper | 跨表 Mapper 混调、直接碰第三方 |
| Manager | 第三方封装（防腐）、通用能力下沉、DAO 组合 | Mapper、外部系统 | 业务规则 |
| Mapper | SQL 与结果映射 | — | 写业务 |

**领域模型规约（黄山版）**：`DO`（表结构，禁止出 Service 边界给前端）、`DTO`（入参）、`VO`（出参，含展示冗余字段）、`Query`（查询请求对象）。转换一律显式映射，禁用 `BeanUtils.copyProperties`。

## 目录结构

```
com.example.app
├── common
│   ├── api          # R / PageQuery / PageResult
│   ├── config       # MyBatis / Jackson(Long->String) / OpenAPI
│   ├── enums        # EmployeeStatus（枚举替代魔法值）
│   └── exception    # ErrorCode(A/B/C 分级) / BizException / 全局异常
├── controller       # 员工接口
├── service          # 业务接口 + impl
├── manager          # DeptManager 防腐层：local(配置模拟)/remote(HTTP) 双实现
├── mapper           # EmployeeMapper + resources/mapper/*.xml
└── model
    ├── dataobject   # DO
    ├── dto          # 入参/查询
    └── vo           # 出参
```

## 关键机制

- **统一响应**：`R{code, message, data}`，`code="0"` 成功；错误码 A 用户端 / B 系统执行侧 / C 第三方。业务异常返回 HTTP 200 + 业务码（后台前端惯例），参数错误 400，未知错误 500。
- **Manager 防腐层**：部门数据在部门服务，本工程经 `DeptManager` 访问；`dept.provider=local`（演示）与 `remote`（HTTP + 超时 + 异常翻译）切换零改动业务代码。
- **分页**：`PageHelper.startPage` 必须紧跟查询，`clearPage()` 放 `finally` 防线程池分页串号；`pageSize` 上限 200。
- **Long→String**：Jackson 全局把 Long 序列化为字符串（JS 2^53 精度问题），前端拿到的 `id`/`total` 均为字符串。
- **逻辑删除**：`deleted` 位 + 查询统一过滤，数据不物理删除。
- **工号回填**：先插入拿自增 ID，再回填 `EMP` + 6 位 ID。

## 多实例部署约定

1. 应用无状态：禁用 HttpSession 存业务状态、禁用本地文件做共享存储。
2. 定时任务必须带分布式锁（Redisson/ShedLock，接入时评审）。
3. 本地缓存仅允许只读且可容忍不一致的数据，共享状态走 Redis。
4. 写接口按需做幂等（唯一键 / 状态机 / 去重表），先想清楚重复请求语义。
5. `gmt_modified` 乐观锁可选启用；深分页用游标方案替代大 offset。

## 新增一个业务接口（清单）

1. `model/dto` 建入参/查询对象（英文校验 message）→ `model/vo` 建出参。
2. `mapper` 加接口方法 + XML（显式列名、`#{}`、禁 `SELECT *`）。
3. `service` 加接口方法 + 实现：业务规则、`@Transactional(rollbackFor = Exception.class)`、日志占位符。
4. 涉及外部服务 → 走 `manager` 防腐层，禁止直接 HTTP/RPC。
5. `controller` 加端点（`@Valid`、`@Operation`），`ErrorCode` 补错误码。
6. 补测试：Service 单测（Mock Mapper/Manager）+ Controller 集成测试（H2 全链路）。

## 测试与质量门禁

```bash
mvn clean verify -DskipTests=false  # 全量验证：测试 + Spotless + PMD + SpotBugs + JaCoCo + ArchUnit
mvn spotless:apply                  # 格式化代码（若 Spotless 校验不通过可一键修复）
```

- `EmployeeServiceImplTest`：Service 层单测（Mockito），验证部门校验、工号回填、离职冻结规则。
- `EmployeeControllerTest`：HTTP 全链路集成测试（H2 + 本地防腐层），覆盖创建/查询/更新/分页/逻辑删除/异常分支。
- `ArchitectureTest`：ArchUnit 守护 Controller -> Service -> Manager -> Mapper 严格依赖方向、DO 不得外泄。
- `Spotless` / `PMD` / `SpotBugs` / `JaCoCo`：在 `verify` 阶段自动化执行质量门禁，未达标直接阻断构建。
