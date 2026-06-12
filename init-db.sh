#!/usr/bin/env bash
# ============================================================
# TTMS 数据库初始化（首次部署执行一次）
# 用法: ./init-db.sh <ECS公网IP> [ssh用户] [ssh端口]
# ============================================================
set -euo pipefail

ECS_HOST="${1:-}"
SSH_USER="${2:-root}"
SSH_PORT="${3:-22}"
REMOTE_DIR="${REMOTE_DIR:-/data/ttms}"

if [ -z "$ECS_HOST" ]; then
    echo "用法: ./init-db.sh <ECS公网IP> [SSH用户] [SSH端口]"
    echo "示例: ./init-db.sh 124.70.xxx.xxx root 22"
    echo ""
    echo "前置条件: ECS 上已安装 MySQL 8.0+，且已创建空数据库 TTMS"
    exit 1
fi

SSH_OPTS="-o StrictHostKeyChecking=no -p ${SSH_PORT}"

echo "=== 初始化 TTMS 数据库 ==="
echo "ECS: ${SSH_USER}@${ECS_HOST}:${SSH_PORT}"
echo ""

# 上传 schema.sql
scp -P ${SSH_PORT} backend/src/main/resources/schema.sql "${SSH_USER}@${ECS_HOST}:/tmp/ttms-schema.sql"

# 执行
ssh ${SSH_OPTS} "${SSH_USER}@${ECS_HOST}" "
    echo '请输入 MySQL root 密码:'
    read -s MYSQL_PWD
    mysql -u root -p\"\$MYSQL_PWD\" < /tmp/ttms-schema.sql 2>&1
    rm /tmp/ttms-schema.sql
    echo ''
    echo '数据库初始化完成！'
    mysql -u root -p\"\$MYSQL_PWD\" -e 'SHOW TABLES;' TTMS 2>&1
"

echo ""
echo "TTMS 数据库已就绪，可以执行 ./deploy.sh 部署应用"
