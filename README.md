# TTMS - 电影院综合管理系统

仿猫眼电影系统的完整影院管理平台，基于 Java + Spring Boot + Vue 3 + MySQL 开发。

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Java 17 + Spring Boot 3.2 + MyBatis-Plus 3.5 |
| 前端 | Vue 3 + Element Plus + ECharts + Pinia |
| 数据库 | MySQL 8.0 + Druid 连接池 |
| 安全 | Spring Security + JWT (Bearer Token) |
| 构建 | Maven + Vite |

## 快速启动

### 1. 环境要求
- JDK 17+
- Maven 3.9+
- MySQL 8.0+
- Node.js 18+

### 2. 数据库
确保 MySQL 服务已启动，默认连接配置：
- 主机：localhost:3306
- 数据库名：TTMS（自动创建）
- 用户名：root
- 密码：见 `application.yml` 中 `spring.datasource.password`

### 3. 启动后端
```bash
cd backend
mvn spring-boot:run
```
首次启动自动建表并初始化数据。默认管理员账号：`admin / admin123`

### 4. 启动前端
```bash
cd frontend
npm install
npm run dev
```
浏览器访问 `http://localhost:3000`

### 5. 打包部署
```bash
cd backend
mvn clean package -DskipTests
java -jar target/ttms-1.0.0.jar
```

## 系统功能

### 前台用户端
- 影片浏览与搜索
- 可视化选座购票
- 个人订单管理
- 改签/退票操作
- 多主题切换（纯白/暗黑/蓝紫）

### 后台管理端
- 影片管理（上架/下架/热门置顶）
- 影厅管理（自定义行列座位）
- 场次排片管理
- 售票订单管理
- 员工账号管理（RBAC权限）
- 数据统计（ECharts图表 + Excel导出）
- 系统设置（主题/公告/参数）

## 权限体系

| 角色 | 权限范围 |
|------|---------|
| 超级管理员 | 全部功能，员工管理，系统配置 |
| 普通员工 | 售票、订单处理、数据查看 |
| 普通用户 | 浏览影片、购票、改签、退票 |

## 项目结构

```
backend/
├── src/main/java/com/ttms/
│   ├── config/        # Spring Security/CORS/MyBatis-Plus配置
│   ├── security/      # JWT认证过滤器
│   ├── controller/    # REST控制器
│   ├── service/       # 业务逻辑层
│   ├── mapper/        # MyBatis数据访问层
│   ├── entity/        # 数据库实体
│   ├── dto/           # 数据传输对象
│   └── exception/     # 全局异常处理
frontend/
├── src/
│   ├── views/user/    # 用户端页面
│   ├── views/admin/   # 管理端页面
│   ├── components/    # 可复用组件
│   ├── api/           # HTTP请求封装
│   ├── stores/        # Pinia状态管理
│   └── styles/        # 主题样式
```
