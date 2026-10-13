# ADR-0003: DO/DTO/VO 模型规约与禁止 DO 外泄

## 状态
已接受 (Accepted) — 2026-09

## 上下文 (Context)
在传统项目中，为了“图省事”，开发常直接将数据库实体（`EmployeeDO`）作为 Controller 的入参或直接当做响应返回给前端。
这种做法的危害：
1. 数据库新增敏感列（如密码哈希、内部状态）可能无意间泄露给前端；
2. 表结构的微小变动直接破坏对外 API 契约；
3. 很多需要展示的冗余字段（如通过部门 ID 关联出的部门名称）无处放置，导致字段在实体上乱打 `@Transient`。

## 决策 (Decision)
1. **严格划分四类数据模型**：
   - `model/dataobject/EmployeeDO`：严格与数据库表字段一一对应；
   - `model/dto/EmployeeCreateDTO`：入参请求对象，携带 JSR-303/380 校验注解；
   - `model/vo/EmployeeVO`：出参视图对象，包含业务展示所需的组合字段；
   - `model/dto/EmployeePageQueryDTO`：查询请求对象。
2. **ArchUnit 强制执行 DO 边界**：
   `ArchitectureTest.doShouldNotLeakToController` 确保 Controller 绝不引用 `EmployeeDO`。
3. **禁用反射拷贝**：禁止 `BeanUtils.copyProperties`，所有对象转换显式声明赋值。

## 后果 (Consequences)
### 正向收益
- API 契约稳定，前后端解耦；
- 避免反射拷贝带来的性能损耗与静默类型丢失。
