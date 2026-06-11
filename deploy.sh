#!/usr/bin/env bash
# ============================================================
# TTMS 华为云一键部署脚本
# 用法: ./deploy.sh <ECS主机IP> [ssh用户]
# ============================================================
set -euo pipefail

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'
log()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
warn() { echo -e "${YELLOW}[WARN]${NC}  $*"; }
err()  { echo -e "${RED}[ERROR]${NC} $*"; exit 1; }

# ---- 参数解析 ----
ECS_HOST="${1:-}"
SSH_USER="${2:-root}"
REMOTE_DIR="${REMOTE_DIR:-/data/ttms}"

if [ -z "$ECS_HOST" ]; then
    echo "用法: ./deploy.sh <ECS公网IP> [SSH用户]"
    echo "示例: ./deploy.sh 124.70.xxx.xxx root"
    echo ""
    echo "环境变量（可选）:"
    echo "  REMOTE_DIR     ECS上的部署目录，默认 /data/ttms"
    echo "  REGISTRY       镜像仓库地址（如华为云SWR）"
    exit 1
fi

# ---- 前置检查 ----
log "检查本地环境..."
command -v docker >/dev/null 2>&1 || err "请先安装 Docker"
command -v ssh  >/dev/null 2>&1 || err "请先安装 SSH 客户端"

# ---- 构建镜像 ----
log "开始构建 Docker 镜像..."
docker build -t ttms:latest . || err "镜像构建失败"

# ---- 检查远程连接 ----
log "检查到 ECS 的连接: ${SSH_USER}@${ECS_HOST}"
if ! ssh -o ConnectTimeout=5 -o StrictHostKeyChecking=no "${SSH_USER}@${ECS_HOST}" "echo ok" &>/dev/null; then
    err "无法连接到 ECS，请检查 IP 和 SSH 配置"
fi

# ---- 准备远程环境 ----
log "在 ECS 上创建部署目录: ${REMOTE_DIR}"
ssh "${SSH_USER}@${ECS_HOST}" "mkdir -p ${REMOTE_DIR}"

# ---- 设置远程防火墙 ----
log "配置安全组/防火墙规则..."
ssh "${SSH_USER}@${ECS_HOST}" "
    # 检查防火墙类型并放行 8080 端口
    if command -v firewall-cmd &>/dev/null 2>&1; then
        firewall-cmd --permanent --add-port=8080/tcp 2>/dev/null || true
        firewall-cmd --reload 2>/dev/null || true
    elif command -v ufw &>/dev/null 2>&1; then
        ufw allow 8080/tcp 2>/dev/null || true
    elif command -v iptables &>/dev/null 2>&1; then
        iptables -I INPUT -p tcp --dport 8080 -j ACCEPT 2>/dev/null || true
    fi
    echo '防火墙已配置（如使用华为云安全组，请手动在控制台放行 8080 端口）'"

# ---- 上传文件 ----
log "上传部署文件到 ECS..."
scp docker-compose.yml "${SSH_USER}@${ECS_HOST}:${REMOTE_DIR}/"
scp backend/src/main/resources/schema.sql "${SSH_USER}@${ECS_HOST}:${REMOTE_DIR}/"

# ---- 推送镜像（支持华为云SWR）----
if [ -n "${REGISTRY:-}" ]; then
    log "推送镜像到远程仓库: ${REGISTRY}"
    docker tag ttms:latest "${REGISTRY}/ttms:latest"
    docker push "${REGISTRY}/ttms:latest"
    # 在 ECS 上拉取
    ssh "${SSH_USER}@${ECS_HOST}" "docker pull ${REGISTRY}/ttms:latest && docker tag ${REGISTRY}/ttms:latest ttms:latest"
else
    log "导出镜像并上传到 ECS（无远程仓库模式）..."
    docker save ttms:latest | gzip > ttms.tar.gz
    scp ttms.tar.gz "${SSH_USER}@${ECS_HOST}:${REMOTE_DIR}/"
    ssh "${SSH_USER}@${ECS_HOST}" "docker load < ${REMOTE_DIR}/ttms.tar.gz && rm ${REMOTE_DIR}/ttms.tar.gz"
    rm -f ttms.tar.gz
fi

# ---- 创建 .env 文件 ----
log "配置远程环境变量..."
if [ -f .env ]; then
    scp .env "${SSH_USER}@${ECS_HOST}:${REMOTE_DIR}/.env"
    warn "使用本地 .env 文件，请确认生产配置正确！"
else
    warn "未找到 .env 文件，生成默认配置..."
    cat <<'ENVEOF' | ssh "${SSH_USER}@${ECS_HOST}" "cat > ${REMOTE_DIR}/.env"
# === TTMS 生产环境变量 ===
DB_URL=jdbc:mysql://mysql:3306/TTMS?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
DB_USERNAME=root
DB_PASSWORD=Root@123456
DB_ROOT_PASSWORD=Root@123456
JWT_SECRET=ChangeMeToARandomStringAtLeast32Characters!
JWT_EXPIRATION=86400000
ADMIN_USERNAME=admin
ADMIN_PASSWORD=admin123
CORS_ORIGINS=http://localhost:8080
APP_PORT=8080
ENVEOF
    err "请编辑 ${REMOTE_DIR}/.env 中的敏感配置后重新运行脚本"
fi

# ---- 启动服务 ----
log "在 ECS 上启动服务..."
ssh "${SSH_USER}@${ECS_HOST}" "
    cd ${REMOTE_DIR}
    # 确保 docker-compose 可用
    if command -v docker-compose &>/dev/null 2>&1; then
        docker-compose down --remove-orphans 2>/dev/null || true
        docker-compose up -d
    else
        docker compose down --remove-orphans 2>/dev/null || true
        docker compose up -d
    fi
"

# ---- 等待启动 ----
log "等待服务就绪（最多120秒）..."
for i in $(seq 1 24); do
    if curl -sf "http://${ECS_HOST}:8080/actuator/health" >/dev/null 2>&1; then
        log "部署成功！服务已就绪"
        echo ""
        echo "========================================"
        echo -e "  ${GREEN}TTMS 访问地址${NC}"
        echo "  http://${ECS_HOST}:8080"
        echo ""
        echo -e "  ${YELLOW}默认管理员${NC}"
        echo "  用户名: admin"
        echo "  密码: admin123"
        echo ""
        echo -e "  ${YELLOW}重要提醒${NC}"
        echo "  1. 修改默认管理员密码"
        echo "  2. 在华为云安全组中放行 8080 端口"
        echo "  3. 编辑 ECS 上 ${REMOTE_DIR}/.env 配置"
        echo "========================================"
        exit 0
    fi
    printf "."
    sleep 5
done

log "服务可能还在启动中，请稍后访问 http://${ECS_HOST}:8080"
log "查看日志: ssh ${SSH_USER}@${ECS_HOST} 'cd ${REMOTE_DIR} && docker compose logs -f'"
