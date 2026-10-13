# ADR-0004: 安全物理分页与 Long 精度序列化决策

## 状态
已接受 (Accepted) — 2026-09

## 上下文 (Context)
在中后台业务开发中，经常遇到两个经典隐蔽缺陷：
1. **JavaScript 大整数精度丢失**：前端原生 `Number` 类型仅能精确表示 $-(2^{53}-1)$ 到 $2^{53}-1$ 范围内的整数（约 9007 万亿），对于数据库自增偏大或分布式雪花算法生成的 64 位 Long 主键，前端反序列化后尾数会被四舍五入变成 0，导致修改/删除误伤数据；
2. **PageHelper 线程池污染**：`PageHelper` 依赖 `ThreadLocal` 传递分页参数，若在 `startPage` 后业务校验失败抛出异常，未执行目标 SQL，该分页状态会残留在线程上下文中，导致该线程下次执行其他全量查询时被错误截断。

## 决策 (Decision)
1. **全局 Long 转 String 序列化**：
   在 `JacksonConfig` 中，为 `Long.class` 与 `Long.TYPE` 统一注册 `ToStringSerializer.instance`，JSON 输出给前端的所有大整数均以字符串形式传输；
2. **安全物理分页**：
   `PageHelper.startPage` 紧贴查询调用，查询结束后及时收敛，且对 `pageSize` 强制限定最大上限（200 条），防止大 offset 拖垮数据库。

## 后果 (Consequences)
### 正向收益
- 彻底根除前端因数字精度丢失导致的严重业务缺陷；
- 避免 Tomcat/Undertow 线程复用引起的分页串号污染。
