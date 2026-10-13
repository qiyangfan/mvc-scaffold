package com.example.app.mapper;

import com.example.app.model.dataobject.EmployeeDO;
import com.example.app.model.dto.EmployeePageQueryDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 员工 Mapper：方法命名遵循手册规约——get 单个 / list 多个 / count 统计 / insert 新增 / update 修改 / remove 删除。
 */
@Mapper
public interface EmployeeMapper {

    /** 新增员工，自增主键回填到 employeeDO.id */
    long insert(EmployeeDO employeeDO);

    /** 回填工号（依赖自增 id 生成业务工号） */
    int updateEmpNo(@Param("id") Long id, @Param("empNo") String empNo);

    /** 按主键查询（未删除） */
    EmployeeDO get(@Param("id") Long id);

    /** 条件查询（未删除），分页由 PageHelper 在调用前 startPage 接管 */
    List<EmployeeDO> listByQuery(EmployeePageQueryDTO query);

    /** 选择性更新（不含工号与逻辑删除位） */
    int update(EmployeeDO employeeDO);

    /** 逻辑删除 */
    int remove(@Param("id") Long id);
}
