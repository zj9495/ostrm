#!/bin/bash

# Ostrm Docker 开发脚本

set -e

PROJECT_NAME="ostrm"
CONTAINER_NAME="app"
DEFAULT_PORT="3111"
DOCKER_COMPOSE=()

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

print_success() {
    echo -e "${GREEN}$1${NC}"
}

print_error() {
    echo -e "${RED}$1${NC}"
}

print_warning() {
    echo -e "${YELLOW}$1${NC}"
}

print_info() {
    echo -e "${CYAN}$1${NC}"
}

print_step() {
    echo -e "${BLUE}$1${NC}"
}

compose() {
    "${DOCKER_COMPOSE[@]}" "$@"
}

dev_compose_running() {
    [ -f "docker-compose.dev.yml" ] &&
        compose -f docker-compose.dev.yml ps --services --filter status=running 2> /dev/null |
            grep -Eq '^(frontend|backend|proxy)$'
}

current_compose() {
    if dev_compose_running; then
        compose -f docker-compose.dev.yml "$@"
    else
        compose "$@"
    fi
}

check_dependencies() {
    print_step "检查依赖..."

    if ! command -v docker > /dev/null 2>&1; then
        print_error "Docker 未安装或不在 PATH 中"
        exit 1
    fi

    if ! docker info > /dev/null 2>&1; then
        print_error "Docker daemon 未运行，请启动 Docker"
        exit 1
    fi

    if docker compose version > /dev/null 2>&1; then
        DOCKER_COMPOSE=(docker compose)
    elif command -v docker-compose > /dev/null 2>&1; then
        DOCKER_COMPOSE=(docker-compose)
    else
        print_error "docker compose 或 docker-compose 未安装"
        exit 1
    fi

    print_success "依赖检查通过"
}

setup_environment() {
    print_step "设置开发环境..."

    mkdir -p data/config data/db logs strm data/tmp backups

    if [ ! -f ".env" ]; then
        cp .env.docker.example .env
        print_success "已从 .env.docker.example 创建 .env"
    fi

    if ! grep -q '^FRONTEND_DEV_IMAGE=' .env; then
        printf '\nFRONTEND_DEV_IMAGE=node:20\n' >> .env
    fi

    if ! grep -q '^BACKEND_DEV_IMAGE=' .env; then
        printf 'BACKEND_DEV_IMAGE=gradle:8.12.1-jdk21-noble\n' >> .env
    fi

    if ! grep -q '^PROXY_DEV_IMAGE=' .env; then
        printf 'PROXY_DEV_IMAGE=caddy:2\n' >> .env
    fi

    print_success "环境配置完成"
}

build_image() {
    local no_cache="$1"

    print_step "构建 Docker 镜像..."

    if [ "$no_cache" = "true" ]; then
        compose build --no-cache
    else
        compose build
    fi

    print_success "镜像构建完成"
}

create_dev_compose_file() {
    cat > Caddyfile.dev << EOF
:80 {
    handle /api/* {
        reverse_proxy backend:8080
    }

    handle /ws/* {
        reverse_proxy backend:8080
    }

    handle {
        reverse_proxy frontend:3000
    }
}
EOF

    cat > docker-compose.dev.yml << EOF
services:
  frontend:
    image: \${FRONTEND_DEV_IMAGE}
    container_name: ${CONTAINER_NAME}-frontend
    hostname: frontend
    working_dir: /app
    command: >
      sh -c "if [ ! -d node_modules/nuxt ]; then npm ci --prefer-offline --no-audit --no-fund; fi &&
             npm run dev -- --host 0.0.0.0"
    environment:
      NUXT_PUBLIC_APP_VERSION: dev
    ports:
      - "3000:3000"
    volumes:
      - ./frontend:/app
      - frontend_node_modules:/app/node_modules
      - \${HOME}/.npm:/root/.npm
    restart: unless-stopped

  backend:
    image: \${BACKEND_DEV_IMAGE}
    container_name: ${CONTAINER_NAME}-backend
    hostname: backend
    working_dir: /workspace
    command: >
      sh -c "gradle classes && (gradle -t classes & gradle bootRun)"
    environment:
      SPRING_PROFILES_ACTIVE: dev
      LOG_PATH: /maindata/log
      DATABASE_PATH: /maindata/db/openlist2strm.db
      CONFIG_PATH: /maindata/config
      USER_INFO_PATH: /maindata/config/userInfo.json
      FRONTEND_LOGS_PATH: /maindata/log/frontend
    ports:
      - "8080:8080"
    volumes:
      - ./backend:/workspace
      - \${HOME}/.gradle:/home/gradle/.gradle
      - \${LOG_PATH_HOST}:/maindata/log
      - \${CONFIG_PATH_HOST}:/maindata/config
      - \${DB_PATH_HOST}:/maindata/db
      - \${STRM_PATH_HOST}:/workspace/strm
    restart: unless-stopped

  proxy:
    image: \${PROXY_DEV_IMAGE}
    container_name: ${CONTAINER_NAME}
    hostname: app
    depends_on:
      - frontend
      - backend
    ports:
      - "${DEFAULT_PORT}:80"
    volumes:
      - ./Caddyfile.dev:/etc/caddy/Caddyfile:ro
    restart: unless-stopped

volumes:
  frontend_node_modules:
EOF
}

start_services() {
    local compose_file="$1"
    local rebuild="${2:-false}"

    print_step "启动服务..."

    if [ "$compose_file" = "docker-compose.dev.yml" ]; then
        if [ "$rebuild" = "true" ]; then
            compose -f docker-compose.dev.yml up -d --build --force-recreate --remove-orphans
        else
            compose -f docker-compose.dev.yml up -d --remove-orphans
        fi
    else
        compose up -d
    fi

    print_success "服务启动完成"
    print_info "访问地址: http://localhost:${DEFAULT_PORT}"
}

health_check() {
    print_step "执行健康检查..."

    local max_attempts=30
    local attempt=1

    while [ "$attempt" -le "$max_attempts" ]; do
        if curl -f -s "http://localhost:${DEFAULT_PORT}/health" > /dev/null 2>&1; then
            print_success "应用健康检查通过"
            return 0
        fi

        print_info "等待应用启动... (${attempt}/${max_attempts})"
        sleep 2
        attempt=$((attempt + 1))
    done

    print_warning "应用健康检查未通过，请检查日志"
    return 1
}

show_status() {
    current_compose ps
}

show_logs() {
    local follow="$1"

    if [ "$follow" = "true" ]; then
        current_compose logs -f
    else
        current_compose logs --tail=100
    fi
}

exec_container() {
    local shell_name="$1"

    if docker ps --format '{{.Names}}' | grep -Fxq "$CONTAINER_NAME"; then
        docker exec -it "$CONTAINER_NAME" "$shell_name"
    else
        print_error "容器未运行，请先启动服务"
        exit 1
    fi
}

cleanup() {
    local deep_clean="$1"

    if [ "$deep_clean" = "true" ]; then
        current_compose down --rmi all --volumes
        rm -f docker-compose.dev.yml Caddyfile.dev
    else
        current_compose down
    fi

    print_success "清理完成"
}

backup_data() {
    setup_environment

    local backup_name="backup-$(date +%Y%m%d-%H%M%S).tar.gz"

    tar -czf "backups/${backup_name}" data strm
    print_success "备份完成: backups/${backup_name}"
}

show_help() {
    cat << EOF
Ostrm Docker 开发脚本

用法: $0 [命令] [参数]

命令:
  install              初始化开发环境
  start, up            启动服务
  start-dev, up-dev    以开发配置启动服务
  start-dev --build    重新构建镜像后以开发配置启动服务
  stop, down           停止服务
  restart              重启服务
  build                构建镜像
  rebuild              强制重新构建镜像
  logs                 查看最近 100 行日志
  logs -f              实时查看日志
  logs-f               实时查看日志
  status               显示服务状态
  exec [shell]         进入容器，默认 bash
  clean                停止并清理容器
  clean-all            停止并清理容器、镜像和卷
  backup               备份 data 和 strm 目录
  health               执行健康检查
  help, -h, --help     显示帮助

示例:
  $0 install
  $0 start
  $0 start-dev
  $0 rebuild --no-cache
  $0 logs -f
  $0 exec sh
  $0 backup
EOF
}

install_dev_env() {
    check_dependencies
    setup_environment
    build_image false
    print_success "开发环境初始化完成"
}

main() {
    local command="help"

    if [ "$#" -gt 0 ]; then
        command="$1"
        shift
    fi

    case "$command" in
        install)
            install_dev_env
            ;;
        start|up)
            check_dependencies
            setup_environment
            start_services "docker-compose.yml"
            health_check
            show_status
            ;;
        start-dev|up-dev)
            check_dependencies
            setup_environment
            create_dev_compose_file
            if [ "${1:-}" = "--build" ] || [ "${1:-}" = "--rebuild" ]; then
                start_services "docker-compose.dev.yml" true
            else
                start_services "docker-compose.dev.yml"
            fi
            show_status
            ;;
        stop|down)
            check_dependencies
            current_compose down
            print_success "服务已停止"
            ;;
        restart)
            check_dependencies
            current_compose restart
            print_success "服务已重启"
            ;;
        build)
            check_dependencies
            setup_environment
            if [ "${1:-}" = "--no-cache" ]; then
                build_image true
            else
                build_image false
            fi
            ;;
        rebuild)
            check_dependencies
            setup_environment
            build_image true
            ;;
        logs)
            check_dependencies
            if [ "${1:-}" = "-f" ]; then
                show_logs true
            else
                show_logs false
            fi
            ;;
        logs-f)
            check_dependencies
            show_logs true
            ;;
        status)
            check_dependencies
            show_status
            ;;
        exec)
            check_dependencies
            exec_container "${1:-bash}"
            ;;
        clean)
            check_dependencies
            cleanup false
            ;;
        clean-all)
            check_dependencies
            cleanup true
            ;;
        backup)
            backup_data
            ;;
        health)
            health_check
            ;;
        help|-h|--help)
            show_help
            ;;
        *)
            print_error "未知命令: $command"
            show_help
            exit 1
            ;;
    esac
}

trap 'print_warning "脚本被中断"; exit 1' INT TERM

main "$@"
