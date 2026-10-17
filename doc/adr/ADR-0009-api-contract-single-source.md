# ADR-0009: API 契约单一来源与 CI 漂移门禁

## 状态

已接受 (Accepted) — 2026-10

## 上下文 (Context)

前后端分离下，接口契约长期靠"口头约定 + 各写一份类型"维持：后端改字段忘了通知前端、
前端按旧结构写类型，问题在联调甚至上线后才暴露。手工同步的契约必然漂移，且漂移发现得越晚成本越高。

## 决策 (Decision)

1. **单一事实源 = 本服务的 OpenAPI schema**（springdoc 从注解实时生成）；
2. `contracts/openapi.json` **入库**：`OpenApiContractTest` 在
   `-Dcontract.export=true` 时把格式化后的 schema 写到该文件，
   PR 里契约变更与代码变更同屏可审；
3. **CI 漂移门禁**：流水线先以 `-Dcontract.export=true` 跑全量 verify，再执行
   `git status --porcelain contracts/`——非空即失败。"改了接口忘了导出"直接让构建红；
4. 前端类型由 `contracts/openapi.json` 生成（`openapi-typescript`），**手写 API 类型视为违规**；
5. 契约测试同时做常态校验：`/v3/api-docs` 必须返回合法 JSON 且包含主链路端点与 `components` 段，
   防止注解被误删导致契约残缺而无人察觉。

## 为什么用"测试导出 + git diff"而不是单独脚本

单独脚本要额外维护一份启动逻辑（起服务、等就绪、拉取、写文件），而在 `@SpringBootTest` 里
MockMvc 已经在跑完整上下文，导出只是顺带。测试同时承担"契约必须有效"的断言，一份代码两个职责。

## 后果 (Consequences)

### 正向收益

- 契约漂移从"联调期人肉发现"变成"CI 期自动阻断"；
- 入库的 schema 可 diff、可 review、可直接喂给前端代码生成；
- 门禁逻辑零额外基础设施，不需要独立契约服务。

### 代价与缓解

- 测试需要 `-Dcontract.export=true` 才写文件：本地默认只校验不写盘，避免开发时误改入库文件；
- 导出依赖 springdoc 的字段顺序稳定性：已实测两次导出字节一致（Jackson 树序列化保证）。

## 备选方案（已否决）

- tRPC / 共享类型包：要求前后端同语言同仓库，与多前端（web/admin）形态冲突，否决；
- 只靠 Code Review 把关：历史反复证明不可靠，否决；
- 独立契约服务：为一个脚手架引入常驻组件，过度设计，否决。
