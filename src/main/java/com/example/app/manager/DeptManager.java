package com.example.app.manager;

import java.util.Collection;
import java.util.List;

/**
 * 部门 Manager（防腐层）：封装对部门服务的访问，Service 层禁止直接调用第三方服务。
 * <p>职责（手册 Manager 层规约）：第三方平台封装，对外部模型先转换、异常先翻译再返回。</p>
 */
public interface DeptManager {

    /**
     * 查询部门信息。
     * <p>部门不存在抛 {@link com.example.app.common.exception.ErrorCode#DEPT_NOT_FOUND}，
     * 已禁用抛 {@link com.example.app.common.exception.ErrorCode#DEPT_DISABLED}。</p>
     */
    DeptInfo getDeptInfo(Long deptId);

    /**
     * 批量查询部门信息：分页组装部门名时使用，避免循环内逐个调用（N+1）。
     */
    List<DeptInfo> listDeptInfos(Collection<Long> deptIds);

    /**
     * 部门信息的本工程内部模型：外部服务返回的结构必须先转换成该模型，禁止外泄到 Service 层。
     */
    record DeptInfo(Long deptId, String deptName, boolean enabled) {
    }
}
