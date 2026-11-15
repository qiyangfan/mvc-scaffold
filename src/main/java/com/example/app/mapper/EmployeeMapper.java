package com.example.app.mapper;

import com.example.app.model.dataobject.EmployeeDO;
import com.example.app.model.dto.EmployeePageQueryDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 员工 Mapper：方法命名遵循手册 get/list/count/insert/update 规约。
 * <p>主键由应用侧生成，插入不使用 useGeneratedKeys，也不再需要插入后回填工号的二次 UPDATE。</p>
 */
@Mapper
public interface EmployeeMapper {

    /** 新增员工，主键与工号均已在 Service 层确定 */
    int insert(EmployeeDO employeeDO);

    /** 按主键查询（未删除） */
    EmployeeDO get(@Param("id") String id);

    /** 条件查询（未删除），供分页使用 */
    List<EmployeeDO> listByQuery(EmployeePageQueryDTO query);

    /** 更新可变字段；WHERE 复核冻结状态（frozenStatus），返回受影响行数供写时仲裁 */
    int update(@Param("e") EmployeeDO employeeDO, @Param("frozenStatus") String frozenStatus);

    /** 逻辑删除 */
    int remove(@Param("id") String id, @Param("gmtModified") LocalDateTime gmtModified);
}
