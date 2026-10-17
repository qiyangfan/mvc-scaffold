package com.example.app.service;

import com.example.app.common.api.PageResult;
import com.example.app.model.dto.EmployeeCreateDTO;
import com.example.app.model.dto.EmployeePageQueryDTO;
import com.example.app.model.dto.EmployeeUpdateDTO;
import com.example.app.model.vo.EmployeeVO;

/**
 * 员工业务接口：一个方法一个业务动作，命名遵循手册 get/list/insert/update/remove 规约。
 */
public interface EmployeeService {

    /** 创建员工，返回新员工 ID */
    String createEmployee(EmployeeCreateDTO createDTO);

    /** 更新员工（选择性更新，离职员工禁止修改） */
    void updateEmployee(EmployeeUpdateDTO updateDTO);

    /** 删除员工（逻辑删除） */
    void removeEmployee(String id);

    /** 查询员工详情 */
    EmployeeVO getEmployee(String id);

    /** 分页查询员工 */
    PageResult<EmployeeVO> listEmployees(EmployeePageQueryDTO pageQueryDTO);
}
