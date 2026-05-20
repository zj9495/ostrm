## Context

`dev-docker.sh` 曾经承担完整开发脚本职责。后续变更将它简化为只支持 `start`，但 `DEV_SCRIPTS.md`、`docs/dev.md`、`AGENTS.md` 等文档仍引用完整命令集。

本变更应优先恢复文档承诺的命令行为，而不是改写文档来适配当前缩水后的脚本。

## Goals

- 让 `DEV_SCRIPTS.md` 中记录的命令可以直接运行。
- 保留单脚本入口，避免重新引入多个重复脚本。
- 让帮助输出成为可验证的命令清单。
- 对破坏性操作保持显式命令语义。

## Non-Goals

- 不新增自动清理、自动重建、自动测试等文档未要求的流程。
- 不改变应用运行时配置、业务代码或容器镜像内容。
- 不添加额外状态、锁、缓存判断或保护性分支。

## Decisions

1. `start` 应启动 `docker-compose.yml`，不默认执行 `down --rmi all --volumes`。
2. `rebuild` 应负责无缓存构建；`clean-all` 应负责删除镜像和卷。
3. `logs -f` 和 `logs-f` 都应可用，因为文档同时出现了这两种形式。
4. `exec [shell]` 应接受可选 shell 参数，例如 `exec sh`。
5. `install` 应只做依赖检查、环境目录和 `.env` 初始化、镜像构建。
6. 验证阶段只做脚本语法和帮助输出检查；除非另有要求，不启动容器或运行自动化测试。

## Risks

- 恢复 `start-dev` 时如果直接复用历史开发 compose 生成逻辑，可能与当前 Dockerfile 中的 Caddy 运行时不完全匹配。
- 如果实现中新增未记录行为，可能再次造成脚本文档漂移。
