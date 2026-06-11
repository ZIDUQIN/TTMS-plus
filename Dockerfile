# ============================================================
# TTMS 多阶段 Docker 构建
# 一键构建: docker build -t ttms:latest .
# 运行: docker run -p 8080:8080 -e DB_URL=... -e DB_USERNAME=... -e DB_PASSWORD=... ttms:latest
# ============================================================

# ============ Stage 1: 构建前端 ============
FROM node:20-alpine AS build-frontend
WORKDIR /frontend
COPY frontend/package*.json ./
RUN npm ci --registry=https://registry.npmmirror.com
COPY frontend/ ./
RUN npm run build

# ============ Stage 2: 构建后端 ============
FROM maven:3.9-eclipse-temurin-17-alpine AS build-backend
WORKDIR /backend
# 先拷贝 pom.xml 利用 Docker 缓存层
COPY backend/pom.xml ./
RUN mvn dependency:go-offline -B -q || true
# 拷贝源码
COPY backend/src ./src
# 把前端产物放入 Spring Boot 静态资源目录
COPY --from=build-frontend /frontend/dist ./src/main/resources/static
# 打包（跳过测试）
RUN mvn clean package -DskipTests -B -q

# ============ Stage 3: 运行时 ============
FROM eclipse-temurin:17-jre-alpine AS runtime
# 时区 & 字体（图表导出用）
RUN apk add --no-cache tzdata fontconfig ttf-dejavu curl && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

# 安全: 非 root 用户运行
RUN addgroup -S ttms && adduser -S ttms -G ttms
USER ttms

WORKDIR /app
COPY --from=build-backend /backend/target/*.jar app.jar

# 创建上传目录
RUN mkdir -p /app/uploads

EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -sf http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
