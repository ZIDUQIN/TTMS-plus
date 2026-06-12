#!/usr/bin/env bash
# ============================================================
# TTMS 华为云一键部署脚本 — Maven 构建 + Spring Boot JAR
# 用法: ./deploy.sh <ECS公网IP> [ssh用户] [ssh端口]
# 示例: ./deploy.sh 124.70.xxx.xxx root
#       ./deploy.sh 124.70.xxx.xxx root 22
# ============================================================
set -euo pipefail

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'
log()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
warn() { echo -e "${YELLOW}[WARN]${NC}  $*"; }
err()  { echo -e "${RED}[ERROR]${NC} $*"; exit 1; }

# ---- 参数 ----
ECS_HOST="${1:-}"
SSH_USER="${2:-root}"
SSH_PORT="${3:-22}"
REMOTE_DIR="${REMOTE_DIR:-/data/ttms}"
APP_PORT="${APP_PORT:-8080}"

if [ -z "$ECS_HOST" ]; then
    echo "用法: ./deploy.sh <ECS公网IP> [SSH用户] [SSH端口] [MySQL密码]"
    echo ""
    echo "示例:"
    echo "  ./deploy.sh 124.70.xxx.xxx root 22 Root@123456"
    echo ""
    echo "前置条件:"
    echo "  1. 本地安装 JDK17+ 和 Maven 3.9+"
    echo "  2. ECS 安装 JDK17+ 和 MySQL 8.0+"
    echo "  3. ECS 已创建 TTMS 数据库"
    exit 1
fi

DB_PWD="${4:-}"
if [ -z "$DB_PWD" ]; then
    read -sp "请输入华为云 MySQL root 密码: " DB_PWD
    echo ""
fi

SSH_OPTS="-o ConnectTimeout=10 -o StrictHostKeyChecking=no -p ${SSH_PORT}"

# ---- 1. 构建前端 ----
log "Step 1/5: 构建前端..."
cd frontend
npm run build --silent 2>/dev/null || npm run build
log "前端构建完成"
cd ..

# ---- 2. 将前端产物嵌入后端 static 目录 ----
log "Step 2/5: 嵌入前端到后端 static..."
rm -rf backend/src/main/resources/static/assets backend/src/main/resources/static/index.html
cp -r frontend/dist/* backend/src/main/resources/static/

# ---- 3. Maven 构建（前端已打包在内）----
log "Step 3/5: Maven 构建后端 JAR..."
cd backend
mvn clean package -DskipTests -B -q || err "Maven 构建失败，请检查 JDK/Maven 版本"
JAR_FILE=$(ls target/ttms-*.jar | head -1)
log "构建完成: ${JAR_FILE}"
cd ..

# ---- 4. 检查远程连接 ----
log "Step 4/5: 连接 ECS ${SSH_USER}@${ECS_HOST}:${SSH_PORT}..."
if ! ssh ${SSH_OPTS} "${SSH_USER}@${ECS_HOST}" "echo ok" &>/dev/null; then
    err "无法连接 ECS，请检查 IP / 用户名 / 端口"
fi

# ---- 5. 上传 JAR 到 ECS ----
log "Step 5/5: 上传文件..."

# 在 ECS 上停止旧进程
ssh ${SSH_OPTS} "${SSH_USER}@${ECS_HOST}" "
    mkdir -p ${REMOTE_DIR}/{bin,config,logs,uploads}
    if [ -f ${REMOTE_DIR}/bin/app.pid ]; then
        PID=\$(cat ${REMOTE_DIR}/bin/app.pid)
        if kill -0 \$PID 2>/dev/null; then
            echo '停止旧进程 PID=\$PID'
            kill \$PID 2>/dev/null || true
            sleep 3
            kill -9 \$PID 2>/dev/null || true
        fi
    fi
"

# 上传 JAR 和迁移脚本
log "  上传文件..."
scp -P ${SSH_PORT} backend/target/ttms-*.jar "${SSH_USER}@${ECS_HOST}:${REMOTE_DIR}/bin/app.jar"
scp -P ${SSH_PORT} backend/src/main/resources/migration.sql "${SSH_USER}@${ECS_HOST}:${REMOTE_DIR}/config/"
scp -P ${SSH_PORT} backend/src/main/resources/schema.sql "${SSH_USER}@${ECS_HOST}:${REMOTE_DIR}/config/"

# ---- 6. 数据库迁移（安全添加缺失列）----
log "Step 6/7: 执行数据库迁移..."
ssh ${SSH_OPTS} "${SSH_USER}@${ECS_HOST}" "
    echo '执行 migration.sql...'
    mysql -u root -p'${DB_PWD}' TTMS < ${REMOTE_DIR}/config/migration.sql 2>&1
    echo '迁移完成'
"

log "Step 7/7: 配置环境变量并启动..."

# 生成启动脚本
ssh ${SSH_OPTS} "${SSH_USER}@${ECS_HOST}" "cat > ${REMOTE_DIR}/bin/start.sh << 'SCRIPT'
#!/usr/bin/env bash
cd ${REMOTE_DIR}

# === 生产环境变量（请根据实际情况修改）===
export SERVER_PORT=${APP_PORT}
export DB_URL=jdbc:mysql://localhost:3306/TTMS?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
export DB_USERNAME=root
export DB_PASSWORD=__DB_PASSWORD__
export JWT_SECRET=TTMS2024ProductionSecretKeyAtLeast32CharsChangeMe!
export JWT_EXPIRATION=86400000
export ADMIN_USERNAME=admin
export ADMIN_PASSWORD=admin123
export UPLOAD_PATH=${REMOTE_DIR}/uploads/
export LOG_PATH=${REMOTE_DIR}/logs/
export CORS_ORIGINS=http://${ECS_HOST}:${APP_PORT}

# JVM 参数
JAVA_OPTS=\"-Xms256m -Xmx512m -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom\"

# 启动
nohup java \$JAVA_OPTS -jar ${REMOTE_DIR}/bin/app.jar \
    --spring.profiles.active=prod \
    --server.port=\${SERVER_PORT} \
    > ${REMOTE_DIR}/logs/app.log 2>&1 &

echo \$! > ${REMOTE_DIR}/bin/app.pid
echo \"TTMS 已启动，PID=\$(cat ${REMOTE_DIR}/bin/app.pid)\"
echo \"日志: tail -f ${REMOTE_DIR}/logs/app.log\"
SCRIPT
chmod +x ${REMOTE_DIR}/bin/start.sh
# 替换数据库密码
sed -i 's/__DB_PASSWORD__/${DB_PWD}/' ${REMOTE_DIR}/bin/start.sh"

# 执行启动
ssh ${SSH_OPTS} "${SSH_USER}@${ECS_HOST}" "bash ${REMOTE_DIR}/bin/start.sh"

# ---- 6. 配置 systemd 服务（开机自启）----
log "配置 systemd 开机自启..."
ssh ${SSH_OPTS} "${SSH_USER}@${ECS_HOST}" "cat > /etc/systemd/system/ttms.service << 'UNIT'
[Unit]
Description=TTMS 电影票务管理系统
After=network.target mysql.service

[Service]
Type=forking
User=${SSH_USER}
WorkingDirectory=${REMOTE_DIR}
ExecStart=${REMOTE_DIR}/bin/start.sh
ExecStop=/bin/kill \$(cat ${REMOTE_DIR}/bin/app.pid)
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
UNIT
systemctl daemon-reload
systemctl enable ttms.service 2>/dev/null || true"

# ---- 7. 等待就绪 ----
log "等待服务启动..."
for i in \$(seq 1 30); do
    if curl -sf "http://${ECS_HOST}:${APP_PORT}/actuator/health" >/dev/null 2>&1; then
        echo ""
        echo "========================================"
        echo -e "  \${GREEN}部署成功！\${NC}"
        echo ""
        echo "  访问地址: http://${ECS_HOST}:${APP_PORT}"
        echo ""
        echo "  默认管理员: admin / admin123"
        echo ""
        echo "  SSH 命令:"
        echo "    ssh -p ${SSH_PORT} ${SSH_USER}@${ECS_HOST}"
        echo "    查看日志: tail -f ${REMOTE_DIR}/logs/app.log"
        echo "    重启服务: systemctl restart ttms"
        echo "    停止服务: systemctl stop ttms"
        echo ""
        echo "  重要提醒:"
        echo "    1. 华为云控制台安全组放行 ${APP_PORT} 端口"
        echo "    2. 编辑 ${REMOTE_DIR}/bin/start.sh 修改数据库密码"
        echo "    3. 首次部署需手动执行 schema.sql 初始化数据库"
        echo "========================================"
        exit 0
    fi
    printf "."
    sleep 3
done

warn "服务可能还在启动，查看日志: ssh ${SSH_OPTS} ${SSH_USER}@${ECS_HOST} 'tail -f ${REMOTE_DIR}/logs/app.log'"
