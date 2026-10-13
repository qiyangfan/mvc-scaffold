package com.example.app.controller;

import com.example.app.common.api.PageResult;
import com.example.app.common.api.R;
import com.example.app.model.dto.EmployeeCreateDTO;
import com.example.app.model.dto.EmployeePageQueryDTO;
import com.example.app.model.dto.EmployeeUpdateDTO;
import com.example.app.model.vo.EmployeeVO;
import com.example.app.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 员工接口：只做参数校验、转发与返回体组装，禁止业务逻辑。
 */
@Tag(name = "员工管理")
@Validated
@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @Operation(summary = "创建员工")
    @PostMapping
    public R<Long> create(@Valid @RequestBody EmployeeCreateDTO createDTO) {
        return R.ok(employeeService.createEmployee(createDTO));
    }

    @Operation(summary = "更新员工")
    @PutMapping
    public R<Void> update(@Valid @RequestBody EmployeeUpdateDTO updateDTO) {
        employeeService.updateEmployee(updateDTO);
        return R.ok();
    }

    @Operation(summary = "删除员工（逻辑删除）")
    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable("id") @Min(value = 1, message = "id must be greater than or equal to 1") Long id) {
        employeeService.removeEmployee(id);
        return R.ok();
    }

    @Operation(summary = "员工详情")
    @GetMapping("/{id}")
    public R<EmployeeVO> get(@PathVariable("id") @Min(value = 1, message = "id must be greater than or equal to 1") Long id) {
        return R.ok(employeeService.getEmployee(id));
    }

    @Operation(summary = "分页查询员工")
    @GetMapping
    public R<PageResult<EmployeeVO>> list(@ParameterObject @Valid EmployeePageQueryDTO pageQueryDTO) {
        return R.ok(employeeService.listEmployees(pageQueryDTO));
    }
}
