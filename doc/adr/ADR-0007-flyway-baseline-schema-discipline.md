# ADR-0007: Flyway 单一 baseline 与 schema 纪律

## 状态

已接受 (Accepted) — 2026-10

## 上下文 (Context)

原实现有两份结构定义：`src/main/resources/schema.sql`（H2 启动初始化）
与 `doc/sql/init.sql`（MySQL 人工执行），
README 要求"生产/联调先执行 init.sql 再启动"。问题：

1. **两份脚本靠人工保持同构**，改一边忘一边是常态；
2. MySQL 侧**无版本概念**——哪套环境跑到哪个版本全凭人记，追补迁移越打越乱；
3. 状态列用 `TINYINT` + 注释（`0待支付 1已支付 2已取消`），可读性依赖注释，脏数据难以审计。

## 决策 (Decision)

1. **结构变更只走 Flyway**：
   `src/main/resources/db/migration/V1__baseline.sql` 为单一 baseline，
   H2 与 MySQL 跑同一套脚本；删除 `schema.sql` 与 `doc/sql/init.sql`；
2. **Boot 4 注意**：Flyway 自动配置已从 `spring-boot-autoconfigure` 移出，必须显式依赖
   `org.springframework.boot:spring-boot-flyway`（MySQL 支持另加 `flyway-mysql`）；
3. schema 纪律（baseline 头部即约束）：
   - 主键 UUID v7 字符串（见 ADR-0006）；
   - **状态列 `VARCHAR + CHECK`**，与领域枚举共用同一份取值清单，禁 `SMALLINT` 数字码、禁 PG ENUM；
   - 时间列统一 `gmt_create / gmt_modified`；
   - **已规划能力一次建全**：有 spec 的审计/反馈/归档等能力建表时一次到位，不留 `V(n)` 追补；
   - 订单为状态机模型，终态归档而非删除，主档不设删除位；
   - DDL 保持 H2(MySQL 模式) 与 MySQL 双兼容，不写引擎/字符集/表级 COMMENT 等方言子句。

## 后果 (Consequences)

### 正向收益

- schema 成为**有版本、有校验和、可回放**的工件，`flyway_schema_history` 即事实源；
- 消除双脚本同构负担；CI 里 H2 全链路测试顺带验证了迁移脚本可执行；
- `VARCHAR + CHECK` 让库层直接拒绝脏状态：`status='1'` 这类 TINYINT 数字码残留、
  `status='paid'` 这类大小写不符的脏值，在入库即被拒，而不是靠应用层自觉。

### 代价与缓解

- Flyway 12 对 H2 2.4.240 有"版本未验证"告警：无害，仅提示；
- 老库首次接入用 `baseline-on-migrate: true` 起跑，避免启动即失败；
- 已发布迁移**禁止改内容**，只能新增版本——追补成本前移，但换来历史可重放。

## 备选方案（已否决）

- 维持双脚本 + 人工约定：同构负担与版本不可知，否决；
- Liquibase：能力足够但 XML/YAML 变更集对纯 DDL 脚手架更重，且团队无存量经验，否决；
- 继续用 `spring.sql.init`：无版本、无校验和、无 history，否决。
