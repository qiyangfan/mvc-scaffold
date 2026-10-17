package com.example.app.service.impl;

import com.example.app.common.enums.EmployeeStatus;
import com.example.app.common.exception.BizException;
import com.example.app.common.exception.ErrorCode;
import com.example.app.manager.DeptManager;
import com.example.app.mapper.EmployeeMapper;
import com.example.app.model.dataobject.EmployeeDO;
import com.example.app.model.dto.EmployeeCreateDTO;
import com.example.app.model.dto.EmployeeUpdateDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Service 层单元测试：Mock 掉 Mapper 与防腐层，只验证业务规则本身。
 */
@ExtendWith(MockitoExtension.class)
class EmployeeServiceImplTest {

    @Mock
    private EmployeeMapper employeeMapper;

    @Mock
    private DeptManager deptManager;

    @InjectMocks
    private EmployeeServiceImpl employeeService;

    /** 测试用固定主键 */
    private static final String ID = "019b1f2c-7a3e-7c4b-8f1d-2e6a9b0c1d2e";

    @Test
    void createShouldThrowWhenDeptNotFound() {
        doThrow(new BizException(ErrorCode.DEPT_NOT_FOUND)).when(deptManager).getDeptInfo(99L);

        assertThatThrownBy(() -> employeeService.createEmployee(createDTO(99L)))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getErrorCode())
                .isEqualTo(ErrorCode.DEPT_NOT_FOUND);
        verify(employeeMapper, never()).insert(any());
    }

    /**
     * 主键与工号在插入前即确定：只有一次 insert，不再有"插入后回填工号"的二次 UPDATE。
     */
    @Test
    void createShouldAssignIdAndEmpNoBeforeInsert() {
        ArgumentCaptor<EmployeeDO> doCaptor = ArgumentCaptor.forClass(EmployeeDO.class);

        String id = employeeService.createEmployee(createDTO(1L));

        assertThat(id).isNotBlank();
        verify(employeeMapper).insert(doCaptor.capture());
        EmployeeDO persisted = doCaptor.getValue();
        // 工号由主键派生：EMP + UUID v7 去横线前 12 位（大写）
        assertThat(persisted.getId()).isEqualTo(id);
        assertThat(persisted.getEmpNo()).isEqualTo(
                "EMP" + id.replace("-", "").substring(0, 12).toUpperCase());
        assertThat(persisted.getStatus()).isEqualTo(EmployeeStatus.TRIAL.getCode());
        assertThat(persisted.getGmtCreate()).isNotNull();
    }

    @Test
    void updateShouldRejectResignedEmployee() {
        givenResignedEmployeeInRepository();

        EmployeeUpdateDTO updateDTO = updateDTO();
        updateDTO.setName("New Name");

        assertThatThrownBy(() -> employeeService.updateEmployee(updateDTO))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getErrorCode())
                .isEqualTo(ErrorCode.BIZ_RULE_CONFLICT);
        verify(employeeMapper, never()).update(any());
    }

    @Test
    void updateShouldRejectEmptyFields() {
        givenRegularEmployeeInRepository();

        assertThatThrownBy(() -> employeeService.updateEmployee(updateDTO()))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getErrorCode())
                .isEqualTo(ErrorCode.PARAM_INVALID);
    }

    @Test
    void updateShouldSucceed() {
        givenRegularEmployeeInRepository();

        EmployeeUpdateDTO updateDTO = updateDTO();
        updateDTO.setName("Updated Name");
        updateDTO.setStatus(EmployeeStatus.TRIAL);

        employeeService.updateEmployee(updateDTO);
        verify(employeeMapper).update(any(EmployeeDO.class));
    }

    /** 库里已存在一名正式员工，并 stub 掉按 ID 查询 */
    private void givenRegularEmployeeInRepository() {
        EmployeeDO regular = new EmployeeDO();
        regular.setId(ID);
        regular.setStatus(EmployeeStatus.REGULAR.getCode());
        when(employeeMapper.get(ID)).thenReturn(regular);
    }

    /** 库里已存在一名离职员工 */
    private void givenResignedEmployeeInRepository() {
        EmployeeDO resigned = new EmployeeDO();
        resigned.setId(ID);
        resigned.setStatus(EmployeeStatus.RESIGNED.getCode());
        when(employeeMapper.get(ID)).thenReturn(resigned);
    }

    /** 只带主键的空更新入参：具体改什么由各测试自己声明 */
    private EmployeeUpdateDTO updateDTO() {
        EmployeeUpdateDTO updateDTO = new EmployeeUpdateDTO();
        updateDTO.setId(ID);
        return updateDTO;
    }

    private EmployeeCreateDTO createDTO(Long deptId) {
        EmployeeCreateDTO createDTO = new EmployeeCreateDTO();
        createDTO.setName("Zhang San");
        createDTO.setDeptId(deptId);
        createDTO.setSalary(new BigDecimal("20000.00"));
        createDTO.setStatus(EmployeeStatus.TRIAL);
        createDTO.setHireDate(LocalDate.of(2026, 9, 1));
        return createDTO;
    }
}
