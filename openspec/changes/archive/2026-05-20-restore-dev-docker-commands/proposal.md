## Why

`DEV_SCRIPTS.md` 将 `dev-docker.sh` 定义为项目唯一 Docker 开发入口，并记录了 `install`、`start-dev`、`logs`、`exec`、`backup`、`health` 等常用命令。

当前 `dev-docker.sh` 只接受 `start`，其他文档命令会失败，导致开发文档和实际脚本行为不一致。

## What Changes

- 恢复 `DEV_SCRIPTS.md` 中记录的 `dev-docker.sh` 命令集。
- 保持 `dev-docker.sh` 作为唯一 Docker 开发入口，不恢复已删除的冗余脚本。
- 支持文档中出现的两种日志命令形式：`logs -f` 和 `logs-f`。
- 保持破坏性操作只由显式命令触发，例如 `clean-all` 和 `rebuild`。

## Non-Goals

- 不新增 `DEV_SCRIPTS.md` 未记录的新开发命令。
- 不改变 Dockerfile、后端、前端或数据库逻辑。
- 不主动调整容器镜像构建策略以外的部署行为。
- 不在提案阶段修改实现代码。

## Impact

- 计划修改 `dev-docker.sh`。
- 计划补齐脚本帮助输出，使其与文档命令保持一致。
- 可能涉及 `docker-compose.dev.yml` 的生成逻辑，但该文件仍应作为运行时生成文件，不纳入仓库。
