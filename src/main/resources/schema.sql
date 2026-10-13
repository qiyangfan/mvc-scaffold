-- H2（MySQL 兼容模式）启动初始化脚本，与 doc/sql/init.sql 保持同构
CREATE TABLE IF NOT EXISTS t_employee
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    emp_no       VARCHAR(32)    DEFAULT NULL COMMENT '工号',
    name         VARCHAR(64)    NOT NULL COMMENT '姓名',
    dept_id      BIGINT         NOT NULL COMMENT '部门ID',
    salary       DECIMAL(12, 2) NOT NULL COMMENT '月薪',
    status       TINYINT        NOT NULL COMMENT '状态:0试用 1正式 2离职',
    hire_date    DATE           NOT NULL COMMENT '入职日期',
    gmt_create   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modified DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    deleted      TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除:0否 1是'
);
CREATE INDEX IF NOT EXISTS idx_employee_dept_id ON t_employee (dept_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_employee_emp_no ON t_employee (emp_no);
