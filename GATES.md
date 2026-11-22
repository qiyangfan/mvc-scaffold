# GATES — 门禁矩阵、触发条件与负向验收

本仓是复制型种子：门禁与业务代码分离，复制后只改标识与连接串即可继承。

## 本地 / CI 对应

```bash
./mvnw clean verify -DskipTests=false   # 格式/静态分析/测试/覆盖率/架构
scripts/migration-guard.sh              # Flyway 迁移只增不改
uvx pre-commit@4.0.0 install --install-hooks  # gitleaks/提交信息/文件卫生
```

CI 分三份：`ci.yml`（质量与契约）、`security.yml`（密钥、依赖漏洞、
SBOM 与许可证、workflow lint）、`codeql.yml`（SAST）。

## 门禁矩阵

| 门禁 | 命令 | 本地 | CI | 负向验收（2026-11-22） |
| --- | --- | --- | --- | --- |
| Spotless | `./mvnw verify` | ✅ | ci.yml | 已有门禁，本轮复跑绿 |
| PMD（0 violation） | `./mvnw verify` | ✅ | ci.yml | 已有门禁，本轮复跑绿 |
| SpotBugs（0 bug） | `./mvnw verify` | ✅ | ci.yml | 已有门禁，本轮复跑绿 |
| JaCoCo（70/50） | `./mvnw verify` | ✅ | ci.yml | 已有门禁，本轮复跑绿 |
| ArchUnit | `./mvnw verify` | ✅ | ci.yml | 已有门禁，本轮复跑绿 |
| 契约漂移 | `-Dcontract.export=true` + `git status` | ✅ | ci.yml | 导出后零漂移 |
| 迁移不可变 | `scripts/migration-guard.sh` | ✅ | ci.yml | 植入 `DROP TABLE` → 红 |
| 密钥 | `gitleaks detect` / `gitleaks dir` | ✅ | security.yml | 植入高熵假 AKIA → 红 |
| 依赖漏洞 | `osv-scanner scan source -r .` | - | security.yml | 建仓首跑实证 |
| SBOM | `cyclonedx-maven-plugin` | ✅ | security.yml | 本地生成 82 组件 |
| 许可证 | `scripts/license-guard.mjs` | ✅ | security.yml | 注入 GPL-3.0 组件 → 红 |
| 提交信息 | `scripts/commitcheck.sh --file` | ✅ pre-commit | - | 非法标题 → 红 |
| Workflow lint | `actionlint` + `yamllint` | ✅ | security.yml | 非法 YAML → 红 |
| Markdown lint | `markdownlint-cli2@0.23.3` | ✅ | ci.yml | 已有门禁，本轮复跑绿 |
| SAST | workflow | - | codeql.yml | 建仓首跑实证 |

## 负向验收记录（2026-11-22）

- 许可证：临时 BOM 注入 `GPL-3.0-only`，脚本 exit 1；移除后 exit 0。
- 迁移：临时目录放 `V2__probe.sql` 含 `DROP TABLE`，守卫 exit 1。
- 密钥：临时文件写入高熵假 `AKIA...`，`gitleaks dir` exit 1。
- Workflow：非法 YAML 交 `actionlint`，exit 1。
- 提交信息：`bad subject` 交 commitcheck，exit 1。
- 依赖漏洞与 CodeQL 需要远端仓库运行，建仓后以 CI 首跑结果为准。

## 平台侧

main 分支保护 required checks 按首跑后的实际 job 名设置：
`verify (JDK 25)`、`secrets`、`deps`、`sbom-license`、`workflows`、
`analyze (java-kotlin)`。

## 触发矩阵（新增形态时补）

| 新增形态 | 必须补的门禁 |
| --- | --- |
| Web 前端 | ESLint/tsc/Prettier、Vitest 覆盖率、Playwright+axe、体积预算 |
| 容器镜像 | hadolint + trivy image（HIGH/CRITICAL，`--ignore-unfixed`）+ 基础镜像钉 sha256 |
| 消息队列/定时任务 | 幂等与重放测试、消费者契约测试 |
| 多实例部署 | 分布式锁/幂等键评审，跨实例集成测试 |

## 抑制纪律

- 许可证例外：MySQL Connector/J 的 GPL-2.0 + Universal FOSS Exception
  登记在 `scripts/license-guard.mjs`，负责人 Solvela，到期日 2027-06-30。
- gitleaks 仅按路径放过测试夹具与文档图片，不允许规则级全局放行。
- 新增抑制必须写原因、负责人、到期日；到期未复审即视为门禁失效。
