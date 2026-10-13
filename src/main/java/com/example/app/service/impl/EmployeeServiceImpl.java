package com.example.app.service.impl;

import com.example.app.common.api.PageResult;
import com.example.app.common.enums.EmployeeStatus;
import com.example.app.common.exception.BizException;
import com.example.app.common.exception.ErrorCode;
import com.example.app.manager.DeptManager;
import com.example.app.mapper.EmployeeMapper;
import com.example.app.model.dataobject.EmployeeDO;
import com.example.app.model.dto.EmployeeCreateDTO;
import com.example.app.model.dto.EmployeePageQueryDTO;
import com.example.app.model.dto.EmployeeUpdateDTO;
import com.example.app.model.vo.EmployeeVO;
import com.example.app.service.EmployeeService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 员工业务实现：承载业务规则；外部部门数据一律经 {@link DeptManager} 防腐层获取，禁止绕过。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    /** 工号格式：EMP + 6 位自增 ID */
    private static final String EMP_NO_FORMAT = "EMP%06d";

    private final EmployeeMapper employeeMapper;
    private final DeptManager deptManager;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createEmployee(EmployeeCreateDTO createDTO) {
        if (!EmployeeStatus.isValid(createDTO.getStatus())) {
            throw new BizException(ErrorCode.PARAM_INVALID, "invalid employee status");
        }
        // 部门校验走防腐层：不存在或已禁用直接抛业务异常
        deptManager.getDeptInfo(createDTO.getDeptId());

        EmployeeDO employeeDO = new EmployeeDO();
        employeeDO.setName(createDTO.getName());
        employeeDO.setDeptId(createDTO.getDeptId());
        employeeDO.setSalary(createDTO.getSalary());
        employeeDO.setStatus(createDTO.getStatus());
        employeeDO.setHireDate(createDTO.getHireDate());
        employeeMapper.insert(employeeDO);

        // 工号依赖自增主键生成，插入后回填
        String empNo = String.format(EMP_NO_FORMAT, employeeDO.getId());
        employeeMapper.updateEmpNo(employeeDO.getId(), empNo);
        log.info("employee created, id:{}, empNo:{}, deptId:{}", employeeDO.getId(), empNo, createDTO.getDeptId());
        return employeeDO.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEmployee(EmployeeUpdateDTO updateDTO) {
        EmployeeDO existing = requireEmployee(updateDTO.getId());
        // 业务规则：离职员工档案冻结，禁止修改
        if (EmployeeStatus.isResigned(existing.getStatus())) {
            throw new BizException(ErrorCode.BIZ_RULE_CONFLICT, "resigned employee cannot be modified");
        }
        if (updateDTO.getStatus() != null && !EmployeeStatus.isValid(updateDTO.getStatus())) {
            throw new BizException(ErrorCode.PARAM_INVALID, "invalid employee status");
        }
        if (updateDTO.getName() == null && updateDTO.getSalary() == null
                && updateDTO.getStatus() == null && updateDTO.getHireDate() == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "at least one updatable field is required");
        }

        EmployeeDO employeeDO = new EmployeeDO();
        employeeDO.setId(updateDTO.getId());
        employeeDO.setName(updateDTO.getName());
        employeeDO.setSalary(updateDTO.getSalary());
        employeeDO.setStatus(updateDTO.getStatus());
        employeeDO.setHireDate(updateDTO.getHireDate());
        employeeMapper.update(employeeDO);
        log.info("employee updated, id:{}", updateDTO.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeEmployee(Long id) {
        requireEmployee(id);
        employeeMapper.remove(id);
        log.info("employee removed, id:{}", id);
    }

    @Override
    public EmployeeVO getEmployee(Long id) {
        EmployeeDO employeeDO = requireEmployee(id);
        return toVO(employeeDO, resolveDeptName(employeeDO.getDeptId()));
    }

    @Override
    public PageResult<EmployeeVO> listEmployees(EmployeePageQueryDTO pageQueryDTO) {
        // startPage 必须紧跟查询语句；clearPage 放 finally，防止线程池复用导致分页串号
        PageHelper.startPage(pageQueryDTO.getPageNum(), pageQueryDTO.getPageSize());
        try {
            List<EmployeeDO> employeeList = employeeMapper.listByQuery(pageQueryDTO);
            PageInfo<EmployeeDO> pageInfo = new PageInfo<>(employeeList);

            Set<Long> deptIds = employeeList.stream()
                    .map(EmployeeDO::getDeptId)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            Map<Long, DeptManager.DeptInfo> deptInfoMap = deptManager.listDeptInfos(deptIds).stream()
                    .collect(Collectors.toMap(DeptManager.DeptInfo::deptId, Function.identity()));

            List<EmployeeVO> voList = new ArrayList<>(employeeList.size());
            for (EmployeeDO employeeDO : employeeList) {
                DeptManager.DeptInfo deptInfo = deptInfoMap.get(employeeDO.getDeptId());
                voList.add(toVO(employeeDO, deptInfo == null ? null : deptInfo.deptName()));
            }
            return PageResult.of(voList, pageInfo.getTotal());
        } finally {
            PageHelper.clearPage();
        }
    }

    private EmployeeDO requireEmployee(Long id) {
        EmployeeDO employeeDO = employeeMapper.get(id);
        if (employeeDO == null) {
            throw new BizException(ErrorCode.EMPLOYEE_NOT_FOUND);
        }
        return employeeDO;
    }

    /** 详情页的部门名是展示信息：部门已删除/禁用时不阻断主流程，仅置空并告警 */
    private String resolveDeptName(Long deptId) {
        try {
            return deptManager.getDeptInfo(deptId).deptName();
        } catch (BizException e) {
            if (Objects.equals(e.getErrorCode(), ErrorCode.DEPT_NOT_FOUND)
                    || Objects.equals(e.getErrorCode(), ErrorCode.DEPT_DISABLED)) {
                log.warn("dept unavailable for display, deptId:{}, code:{}", deptId, e.getErrorCode().getCode());
                return null;
            }
            throw e;
        }
    }

    private EmployeeVO toVO(EmployeeDO employeeDO, String deptName) {
        EmployeeVO employeeVO = new EmployeeVO();
        employeeVO.setId(employeeDO.getId());
        employeeVO.setEmpNo(employeeDO.getEmpNo());
        employeeVO.setName(employeeDO.getName());
        employeeVO.setDeptId(employeeDO.getDeptId());
        employeeVO.setDeptName(deptName);
        employeeVO.setSalary(employeeDO.getSalary());
        employeeVO.setStatus(employeeDO.getStatus());
        employeeVO.setStatusDesc(EmployeeStatus.descOf(employeeDO.getStatus()));
        employeeVO.setHireDate(employeeDO.getHireDate());
        employeeVO.setGmtCreate(employeeDO.getGmtCreate());
        return employeeVO;
    }
}
