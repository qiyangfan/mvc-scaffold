# 数据库结构管理（Flyway）

> 本目录不再存放手工 SQL 脚本。建表与结构变更统一由 Flyway 迁移管理，单一事实源是
> `src/main/resources/db/migration/`。

## 为什么废弃 doc/sql/init.sql

历史上这里放了一份 MySQL 建表脚本，`application-mysql.yml` 要求"先人工执行脚本再启动"。
问题是：脚本与代码无版本绑定，谁改了表结构忘了改脚本、哪套环境执行到哪个版本，全靠人记。
Flyway 把 schema 变成**有版本、有校验和、可回放**的工件：`flyway_schema_history` 表记录每次迁移，
启动时自动补齐，CI 里 H2 与 MySQL 跑同一套脚本。

## 使用方式

```bash
# 本地开发（H2 内置库，开箱即跑，无需任何外部依赖）
mvn spring-boot:run

# 生产/联调：切 MySQL profile，启动时自动执行迁移
mvn spring-boot:run -Dspring-boot.run.profiles=mysql

# 查看迁移状态 / 手动执行 / 校验
mvn flyway:info -Dspring.profiles.active=mysql
mvn flyway:migrate -Dspring.profiles.active=mysql
mvn flyway:validate -Dspring.profiles.active=mysql
```

## 新增一次结构变更

1. 新建 `src/main/resources/db/migration/V{n}__{描述}.sql`（版本号递增，双下划线后是人类可读描述）；
2. 迁移脚本**只增量、不改历史**：已发布的迁移文件禁止修改内容，改了就换新版本；
3. 遵守 `V1__baseline.sql` 头部约定的 6 条 schema 纪律（主键 UUID v7、状态 VARCHAR+CHECK、
   时间列规范、已规划能力一次到位、状态机模型不设删除位、DDL 双库兼容）；
4. 涉及领域枚举变化时，同步更新对应枚举与 CHECK 约束，两边必须是同一份清单。

## 新项目起步

`V1__baseline.sql` 就是"按最终形态一次建全"的基线：不复刻迭代过程的中间态，
已规划能力（审计、反馈、归档等）有 spec 就一次建好，避免上线后连打追补迁移。
