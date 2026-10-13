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
import static org.mockito.ArgumentMatchers.eq;
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

    @Test
    void createShouldThrowWhenDeptNotFound() {
        doThrow(new BizException(ErrorCode.DEPT_NOT_FOUND)).when(deptManager).getDeptInfo(99L);

        assertThatThrownBy(() -> employeeService.createEmployee(createDTO(99L)))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getErrorCode())
                .isEqualTo(ErrorCode.DEPT_NOT_FOUND);
        verify(employeeMapper, never()).insert(any());
    }

    @Test
    void createShouldFillEmpNoAfterInsert() {
        when(employeeMapper.insert(any())).thenAnswer(invocation -> {
            EmployeeDO employeeDO = invocation.getArgument(0);
            employeeDO.setId(100L);
            return 1L;
        });

        Long id = employeeService.createEmployee(createDTO(1L));

        assertThat(id).isEqualTo(100L);
        ArgumentCaptor<String> empNoCaptor = ArgumentCaptor.forClass(String.class);
        verify(employeeMapper).updateEmpNo(eq(100L), empNoCaptor.capture());
        assertThat(empNoCaptor.getValue()).isEqualTo("EMP000100");
    }

    @Test
    void updateShouldRejectResignedEmployee() {
        EmployeeDO resigned = new EmployeeDO();
        resigned.setId(1L);
        resigned.setStatus(EmployeeStatus.RESIGNED.getCode());
        when(employeeMapper.get(1L)).thenReturn(resigned);

        EmployeeUpdateDTO updateDTO = new EmployeeUpdateDTO();
        updateDTO.setId(1L);
        updateDTO.setName("New Name");

        assertThatThrownBy(() -> employeeService.updateEmployee(updateDTO))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getErrorCode())
                .isEqualTo(ErrorCode.BIZ_RULE_CONFLICT);
        verify(employeeMapper, never()).update(any());
    }

    private EmployeeCreateDTO createDTO(Long deptId) {
        EmployeeCreateDTO createDTO = new EmployeeCreateDTO();
        createDTO.setName("Zhang San");
        createDTO.setDeptId(deptId);
        createDTO.setSalary(new BigDecimal("20000.00"));
        createDTO.setStatus(EmployeeStatus.TRIAL.getCode());
        createDTO.setHireDate(LocalDate.of(2026, 9, 1));
        return createDTO;
    }
}
