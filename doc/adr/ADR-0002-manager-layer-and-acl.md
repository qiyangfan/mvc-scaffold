# ADR-0002: Manager 层作为防腐层 (ACL) 的实现模式

## 状态

已接受 (Accepted) — 2026-09

## 上下文 (Context)

微服务拆分后，业务服务经常需要依赖外部基础服务（如员工服务依赖部门服务）。
如果 Service 直接使用 HTTP RestClient 或 FeignClient 调外部服务：

1. 外部系统的接口变更、字段增删会直接波及核心业务逻辑；
2. 外部系统的各种错误响应代码（HTTP 500/404 等）会泄露到上层；
3. 本地开发与单元测试时受制于外部网络可用性，联调成本高。

## 决策 (Decision)

1. **接口定义在 Manager 根包**：定义 `DeptManager` 接口，定义内部返回模型 `DeptInfo`；
2. **防腐层双实现**：
   - `LocalDeptManager`：基于配置文件的本地实现，开箱即用，零依赖快速启动；
   - `RemoteDeptManager`：基于 Spring `RestClient` 的真实 HTTP 远程实现，配置显式连接超时与读取超时；
3. **模型与异常隔离**：外部响应对象 `ExternalDeptResponse` 严格封装在 `RemoteDeptManager` 内部，
   外部异常统一转译为本系统错误码（如 `THIRD_PARTY_ERROR`、`DEPT_NOT_FOUND`）。

## 后果 (Consequences)

### 正向收益

- 本地和单元测试无外部网络依赖，测试极快；
- 外部接口改动仅需调整 Manager 内部实现，核心业务层完全无感。
