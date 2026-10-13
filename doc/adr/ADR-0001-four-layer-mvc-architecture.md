# ADR-0001: Controller-Service-Manager-Mapper 四层增强架构选型

## 状态
已接受 (Accepted) — 2026-09

## 上下文 (Context)
在传统 Spring 3 层架构（Controller ➔ Service ➔ DAO）中，随着业务迭代，Service 层很容易沦为“杂物间”：
1. 既包含复杂的业务逻辑，又直接进行 HTTP/RPC 外部通信，导致网络细节侵蚀业务逻辑；
2. 跨实体或跨表的通用查询逻辑无处安放，导致不同 Service 互相调用，频繁产生死锁或循环依赖；
3. 代码结构膨胀，单一 Service 类动辄上千行。

## 决策 (Decision)
对齐《阿里巴巴 Java 开发手册（黄山版）》推荐的分层规约，引入 `Manager` 层演进为四层架构：
1. **Controller**：负责入口接入、参数校验、转发；
2. **Service**：负责核心业务规则编排、事务边界；
3. **Manager**：负责外部第三方调用封装（防腐层）、通用能力下沉与跨 DAO 编排；
4. **Mapper**：只负责纯粹的数据存取与 SQL 映射；
5. **ArchUnit 守护**：编写 `ArchitectureTest`，硬性保证依赖只能单向向下流动。

## 后果 (Consequences)
### 正向收益
- 职责清晰，Service 层纯粹化，外部网络与通用能力集中沉淀；
- 架构规则有测试强制保护，新入职同学无法随意跨层调用。
