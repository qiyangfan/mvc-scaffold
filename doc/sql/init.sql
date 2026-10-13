-- =============================================
-- mvc-scaffold MySQL 建表脚本
-- 遵循手册建表规约：必备三字段 gmt_create / gmt_modified / deleted
-- =============================================
CREATE DATABASE IF NOT EXISTS mvc_scaffold DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE mvc_scaffold;

CREATE TABLE IF NOT EXISTS t_employee
(
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    emp_no       VARCHAR(32)    DEFAULT NULL COMMENT '工号，插入后回填生成',
    name         VARCHAR(64)    NOT NULL COMMENT '姓名',
    dept_id      BIGINT         NOT NULL COMMENT '部门ID，数据在部门服务',
    salary       DECIMAL(12, 2) NOT NULL COMMENT '月薪',
    status       TINYINT        NOT NULL DEFAULT 0 COMMENT '状态:0试用 1正式 2离职',
    hire_date    DATE           NOT NULL COMMENT '入职日期',
    gmt_create   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modified DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    deleted      TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除:0否 1是',
    PRIMARY KEY (id),
    UNIQUE KEY uk_employee_emp_no (emp_no),
    KEY idx_employee_dept_id (dept_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '员工表';
