-- =============================================================================
-- mvc-scaffold 全量基线（Flyway 单一 baseline）
-- 取代历史上的 schema.sql（H2 初始化）与 doc/sql/init.sql（MySQL 人工执行），schema 变更只走 Flyway。
-- 设计约定（详见 doc/adr/ADR-0006 / ADR-0007）：
--   1. 主键 UUID v7 字符串，应用侧生成，禁用自增主键（自增会逼出"插入后回填"的二次写）；
--   2. 状态列 VARCHAR + CHECK，与 EmployeeStatus 共用同一份取值清单，禁 TINYINT 数字码；
--   3. 时间列统一 gmt_create / gmt_modified，DB 默认值兜底，应用侧显式赋值优先；
--   4. 已规划能力（审计、反馈等）在建表时一次到位，不留 V(n) 追补迁移；
--   5. 逻辑删除沿用 deleted 位（本脚手架为增强 MVC，非 DDD 状态机模型，删除位是有意保留的约定）；
--   6. DDL 保持 H2(MySQL 模式) 与 MySQL 双兼容，不写引擎/字符集/表级 COMMENT 等方言特有子句。
-- =============================================================================

CREATE TABLE t_employee
(
    id           VARCHAR(36)    NOT NULL COMMENT '主键(UUID v7)',
    emp_no       VARCHAR(32)    NOT NULL COMMENT '工号(由主键派生)',
    name         VARCHAR(64)    NOT NULL COMMENT '姓名',
    dept_id      BIGINT         NOT NULL COMMENT '部门ID',
    salary       DECIMAL(12, 2) NOT NULL COMMENT '月薪',
    status       VARCHAR(16)    NOT NULL DEFAULT 'TRIAL' COMMENT '状态:TRIAL/REGULAR/RESIGNED',
    hire_date    DATE           NOT NULL COMMENT '入职日期',
    gmt_create   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modified DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    deleted      TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除:0否 1是',
    PRIMARY KEY (id),
    UNIQUE KEY uk_employee_emp_no (emp_no),
    KEY idx_employee_dept_id (dept_id),
    CONSTRAINT ck_employee_status CHECK (status IN ('TRIAL', 'REGULAR', 'RESIGNED'))
);
