package com.example.app.service.impl;

import com.example.app.common.api.PageResult;
import com.example.app.common.enums.EmployeeStatus;
import com.example.app.common.exception.BizException;
import com.example.app.common.exception.ErrorCode;
import com.example.app.common.support.Ids;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 员工业务实现：承载业务规则；外部部门数据一律经 {@link DeptManager} 防腐层获取，禁止绕过。
 * <p>主键 UUID v7 由应用侧生成，工号由主键派生——插入前即确定，
 * 不再需要"先插入拿自增 ID 再 UPDATE 回填工号"的二次写（见 ADR-0006）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    /** 工号前缀 */
    private static final String EMP_NO_PREFIX = "EMP";
    /** 工号取自 UUID v7 十六进制的位数：前 12 位十六进制恰为 48 位毫秒时间戳，天然时间有序 */
    private static final int EMP_NO_BODY_LENGTH = 12;

    private final EmployeeMapper employeeMapper;
    private final DeptManager deptManager;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createEmployee(EmployeeCreateDTO createDTO) {
        // 部门校验走防腐层：不存在或已禁用直接抛业务异常
        deptManager.getDeptInfo(createDTO.getDeptId());

        String id = Ids.newId();
        EmployeeDO employeeDO = new EmployeeDO();
        employeeDO.setId(id);
        employeeDO.setEmpNo(nextEmpNo(id));
        employeeDO.setName(createDTO.getName());
        employeeDO.setDeptId(createDTO.getDeptId());
        employeeDO.setSalary(createDTO.getSalary());
        employeeDO.setStatus(createDTO.getStatus().getCode());
        employeeDO.setHireDate(createDTO.getHireDate());
        employeeDO.setGmtCreate(LocalDateTime.now());
        employeeDO.setGmtModified(employeeDO.getGmtCreate());
        employeeMapper.insert(employeeDO);
        log.info("employee created, id:{}, empNo:{}, deptId:{}", id, employeeDO.getEmpNo(),
                createDTO.getDeptId());
        return id;
    }

    /**
     * 工号生成：EMP + 主键 UUID v7 去横线后的前 12 位十六进制（大写）。
     * <p>由主键派生保证主键与工号一一对应，且前 12 位即时间戳，工号随时间递增、可排序、
     * 不暴露自增序列量级。</p>
     */
    private String nextEmpNo(String id) {
        return EMP_NO_PREFIX + id.replace("-", "").substring(0, EMP_NO_BODY_LENGTH).toUpperCase(Locale.ROOT);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEmployee(EmployeeUpdateDTO updateDTO) {
        EmployeeDO existing = requireEmployee(updateDTO.getId());
        // 业务规则：离职员工档案冻结，禁止修改
        if (EmployeeStatus.RESIGNED.getCode().equals(existing.getStatus())) {
            throw new BizException(ErrorCode.BIZ_RULE_CONFLICT, "resigned employee cannot be modified");
        }
        if (updateDTO.getName() == null && updateDTO.getSalary() == null
                && updateDTO.getStatus() == null && updateDTO.getHireDate() == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "at least one updatable field is required");
        }

        EmployeeDO employeeDO = new EmployeeDO();
        employeeDO.setId(updateDTO.getId());
        employeeDO.setName(updateDTO.getName());
        employeeDO.setSalary(updateDTO.getSalary());
        employeeDO.setStatus(updateDTO.getStatus() == null ? null : updateDTO.getStatus().getCode());
        employeeDO.setHireDate(updateDTO.getHireDate());
        employeeMapper.update(employeeDO);
        log.info("employee updated, id:{}", updateDTO.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeEmployee(String id) {
        requireEmployee(id);
        employeeMapper.remove(id);
        log.info("employee removed, id:{}", id);
    }

    @Override
    public EmployeeVO getEmployee(String id) {
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

    private EmployeeDO requireEmployee(String id) {
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
