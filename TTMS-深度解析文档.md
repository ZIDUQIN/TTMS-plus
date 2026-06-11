# TTMS 电影院综合管理系统 - 全方位深度解析

> **阅读指南**：本文档涵盖项目架构、数据库设计、安全体系、所有模块的实现逻辑与设计理念，并对每个模块提出了深度问答。文中使用 `📖` 标记对专业术语的注解。

---

## 目录

1. [项目概述与技术栈](#1-项目概述与技术栈)
2. [系统架构设计理念](#2-系统架构设计理念)
3. [数据库设计全景](#3-数据库设计全景)
4. [安全体系深度解析](#4-安全体系深度解析)
5. [核心业务流程详解](#5-核心业务流程详解)
6. [后端模块逐个解析 (22个Controller + 21个Service)](#6-后端模块逐个解析)
7. [前端架构解析](#7-前端架构解析)
8. [定时任务体系](#8-定时任务体系)
9. [设计亮点与最佳实践总结](#9-设计亮点与最佳实践总结)

---

## 1. 项目概述与技术栈

### 1.1 项目定位

TTMS (Theater Ticket Management System) 是一个**仿猫眼电影系统**的完整影院管理平台，同时提供**前台用户购票端**和**后台影院管理端**。

| 层级 | 技术 | 版本 |
|------|------|------|
| 后端框架 | Spring Boot | 3.2.5 |
| Java版本 | JDK | 17 |
| ORM框架 | MyBatis-Plus | 3.5.6 |
| 数据库 | MySQL | 8.0 |
| 连接池 | Druid | 1.2.22 |
| 安全框架 | Spring Security + JWT | 6.x + jjwt 0.12.5 |
| API文档 | SpringDoc OpenAPI | 2.5.0 |
| Excel导出 | Apache POI | 5.2.5 |
| 前端框架 | Vue 3 + Element Plus | 最新 |
| 图表 | ECharts | 最新 |
| 状态管理 | Pinia | 最新 |
| 构建工具 | Maven + Vite | 最新 |

### 1.2 系统角色与权限体系

```
┌─────────────────────────────────────────────────────────────┐
│  超级管理员 (ROLE_SUPER_ADMIN)                                │
│  ├─ 所有功能                                                  │
│  ├─ 员工管理（增删改查、密码重置、状态切换）                      │
│  ├─ 系统配置（主题、公告、参数）                                │
│  └─ 权限标识: ["movie:manage","hall:manage","employee:manage"...]│
├─────────────────────────────────────────────────────────────┤
│  普通员工 (ROLE_STAFF)                                        │
│  ├─ 影片/影厅/场次管理                                        │
│  ├─ 订单管理 + POS柜台售票                                     │
│  ├─ 数据统计查看                                              │
│  └─ 权限标识: ["movie:manage","hall:manage","statistics:view"...]│
├─────────────────────────────────────────────────────────────┤
│  普通用户 (ROLE_USER)                                         │
│  ├─ 浏览影片、搜索                                            │
│  ├─ 选座购票、支付                                            │
│  ├─ 改签、退票                                                │
│  ├─ 会员积分、优惠券                                          │
│  └─ 权限标识: ["movie:view","order:create","order:refund"...]  │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. 系统架构设计理念

### 2.1 分层架构 (Layered Architecture)

```
┌──────────────────────────────────────────────────┐
│  Controller 层 (22个)       │  接收HTTP请求        │
│  - 参数校验 (@Valid)         │  返回统一ApiResponse  │
│  - 权限校验 (@PreAuthorize)  │                      │
├──────────────────────────────────────────────────┤
│  Service 层 (21个)           │  业务逻辑            │
│  - 接口 + 实现分离           │  事务管理 @Transactional│
│  - 业务异常抛出 BusinessEx   │                      │
├──────────────────────────────────────────────────┤
│  Mapper 层 (25个)            │  数据访问            │
│  - MyBatis-Plus BaseMapper   │  自定义SQL           │
│  - 聚合查询/乐观锁/批量操作   │                      │
├──────────────────────────────────────────────────┤
│  Entity 层 (25个)            │  数据库表映射         │
│  - @TableName 表映射         │  @TableLogic 逻辑删除 │
│  - 非数据库字段 @TableField(exist=false)           │
└──────────────────────────────────────────────────┘
```

> 📖 **分层架构**：将系统按职责划分为多层，每层只依赖下一层。Controller 负责接收请求和返回响应，Service 负责业务逻辑，Mapper 负责数据库操作。这种分层方式让代码更容易维护和测试。

> 📖 **逻辑删除 (@TableLogic)**：不是真的从数据库中删除记录，而是将 `deleted` 字段标记为 1。好处是数据可恢复、保留审计追踪。MyBatis-Plus 会自动在查询时添加 `deleted=0` 条件。

### 2.2 核心设计模式

| 模式 | 应用位置 | 说明 |
|------|---------|------|
| **依赖注入** | 全局 | Spring 自动管理对象创建和依赖关系，通过 `@RequiredArgsConstructor` 生成构造函数实现注入 |
| **接口-实现分离** | Service层 | 7个核心服务有接口定义契约，便于未来扩展和Mock测试 |
| **统一响应格式** | ApiResponse | 所有接口返回 `{code, message, data}` 格式，前端无需针对不同接口做不同处理 |
| **全局异常处理** | GlobalExceptionHandler | 统一捕获所有异常并转换为标准ApiResponse，Controller中零try-catch |
| **令牌黑名单** | TokenBlacklist | 内存Map实现JWT主动失效，登出/改密时加入黑名单 |
| **乐观锁** | Seat/Order | 使用SQL条件更新（`WHERE status=0`）而非数据库行锁，高并发下性能更好 |
| **N+1查询优化** | 多处 | 批量查询关联数据后内存组装，避免循环查数据库 |

### 2.3 配置体系

项目采用**多环境配置**策略：

```
application.yml          ← 基础配置（开发环境默认值）
application-prod.yml     ← 生产环境覆盖（华为云部署）
环境变量                 ← 最高优先级（12-Factor App原则）
```

> 📖 **12-Factor App**：一套云原生应用开发方法论，其中第3条要求"配置与代码分离"，通过环境变量注入配置而非硬编码。这个项目使用 `${DB_URL:jdbc:mysql://localhost...}` 的语法，意为优先读环境变量 `DB_URL`，未设置时使用冒号后的默认值。

---

## 3. 数据库设计全景

### 3.1 25张表分类

```
核心业务表（6张）:
  user, employee, role, movie, hall, schedule

订单与座位（3张）:
  order, seat, order_log

卖品系统（3张）:
  snack, snack_combo, snack_order

会员与营销（4张）:
  member_level, coupon, user_coupon, group_booking

运营管理（5张）:
  shift, shift_record, notification, invoice, report

基础设施（4张）:
  system_config, cinema, payment_record, backup_log
```

### 3.2 关键表设计决策

#### 3.2.1 座位表设计的精妙之处

```sql
-- seat 表设计
schedule_id  BIGINT   -- 场次ID（核心外键）
seat_row     INT      -- 行号
seat_col     INT      -- 列号
seat_number  VARCHAR  -- 座位编号如 "A-05"
status       INT      -- 0-空闲 1-已锁定 2-已售出 3-不可用
lock_time    DATETIME -- 锁定时间（超时释放用）
order_id     BIGINT   -- 关联订单ID
price_adjustment DECIMAL -- 座位分区定价的加价金额
```

**为什么每个场次都要生成独立的座位记录？**
因为不同场次的同一物理座位（如"3排5座"）状态不同——今天下午场的3排5座可能已售出，但晚上场的3排5座还空闲。所以座位表以 `(schedule_id, seat_number)` 为唯一约束，每个场次独立拥有一套座位记录。

#### 3.2.2 订单号的生成策略

```
用户端: yyyyMMddHHmmss + 4位随机数 → 如 202606111430221234
POS柜台: POS + yyyyMMdd + 毫秒后5位 + 3位随机数 → 如 POS2026061100123345
卖品: SNK + yyyyMMdd + 6位随机数 → 如 SNK20260611123456
```

#### 3.2.3 `@JsonFormat` 与 `@JsonProperty` 的使用场景

```java
// Schedule.java - 时间格式化
@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
private LocalDateTime startTime;

// Employee.java / User.java - 密码只写不读
@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
private String password;
```

> 📖 **@JsonProperty(WRITE_ONLY)**：密码字段在序列化为JSON返回前端时被跳过（防止密码泄露），但接收前端JSON时可以正常反序列化。是保护敏感字段的标准做法。

---

## 4. 安全体系深度解析

### 4.1 整体安全架构

```
请求 → TraceIdFilter → CorsFilter → SecurityFilterChain
         ↓                              ↓
    生成追踪ID               JwtAuthenticationFilter
                             ↓
                    1. 提取 Authorization: Bearer <token>
                    2. 检查 TokenBlacklist
                    3. 验证签名 + 解析角色
                    4. 设置 SecurityContext
                             ↓
                    Controller (@PreAuthorize)
                             ↓
                    业务处理 → 返回 ApiResponse
```

### 4.2 JWT 令牌机制

> 📖 **JWT (JSON Web Token)**：一种无状态的身份认证令牌，服务器不需要存Session，令牌本身包含用户信息。格式为 `Header.Payload.Signature`。Header声明算法(HS256)，Payload包含业务数据(userId/username/role)，Signature是前两部分用密钥加密的结果。任何人篡改内容都会导致签名验证失败。

```java
// 令牌中存储的信息
claims.put("userId", userId);       // 用户ID
claims.put("username", username);   // 用户名
claims.put("role", role);           // 角色: ROLE_SUPER_ADMIN / ROLE_STAFF / ROLE_USER
claims.put("loginType", loginType); // 登录类型: USER / ADMIN
```

**JWT 过期时间**: 默认 86400000ms = **24小时**（可配置）

### 4.3 双表登录设计

系统支持**用户(User表)**和**员工(Employee表)**两类账号登录，通过 `loginType` 参数区分：

```java
// AuthServiceImpl.login() 的核心逻辑
loginType = "USER"  → 查 user 表 → 验证密码 → 生成 ROLE_USER 令牌
loginType = "ADMIN" → 查 employee 表 → 验证密码 → 根据 role_id 查角色表获取权限
loginType = "" 或 null → 自动检测：先查 employee 表，找不到再查 user 表
```

> 📖 **BCrypt**：一种密码哈希算法，特点是"慢哈希"——故意让计算变慢（约100ms），即使数据库泄露，攻击者也无法用GPU快速批量破解密码。同时自动包含随机盐值，相同密码每次哈希结果不同。

### 4.4 安全防护层次

| 防护类型 | 实现方式 | 说明 |
|---------|---------|------|
| **密码加密** | BCryptPasswordEncoder(10) | strength=10，约100ms/次 |
| **登录限流** | LoginRateLimiter | 同IP+用户名连续5次失败后锁定15分钟 |
| **令牌主动失效** | TokenBlacklist | 登出/改密时将token加入内存黑名单 |
| **URL权限控制** | SecurityConfig | 按HTTP Method + URL Pattern控制 |
| **方法权限控制** | @PreAuthorize | 敏感操作加注解（如重置密码） |
| **CSRF防护** | 关闭 | 前后端分离 + JWT无状态，不需要CSRF |
| **Session管理** | STATELESS | 不使用HttpSession，每个请求独立认证 |
| **文件上传防护** | 魔数验证 | 检查文件头字节而非信任Content-Type |
| **CORS控制** | CorsConfig | 开发环境localhost，生产环境指定域名 |
| **SQL防注入** | Druid Wall Filter + MyBatis参数化 | 双层防护 |
| **慢SQL监控** | Druid Stat Filter | 超过2秒的SQL打印警告 |

### 4.5 模块Q&A

**Q1: 为什么选择JWT而不是传统Session？**
A: JWT是无状态的，服务器不需要存储会话信息，适合前后端分离架构和水平扩展（加服务器不需要共享Session）。每次请求携带token，服务器仅需验证签名即可确认身份。

**Q2: JWT有个天然缺陷——签发后无法主动失效，如何解决？**
A: 项目通过 `TokenBlacklist` 机制解决：登出和修改密码时，将当前token加入内存黑名单。`JwtAuthenticationFilter` 在验证token后会检查黑名单。黑名单条目会随JWT原始过期时间自动清理，防止内存无限膨胀。

**Q3: 为什么要分User表和Employee表而不是合并？**
A: 用户和员工字段差异大（员工有工号、角色；用户有会员等级、积分、余额），合并会导致大量NULL字段。而且登录时查不同的表逻辑更清晰，安全审计更方便。

**Q4: `BCryptPasswordEncoder(10)` 的10是什么意思？会不会太慢？**
A: 10 是强度参数（4-31），每增加1，计算量翻倍。10 约需 100ms 验证一次，对于登录场景可以接受。高并发场景可以降至 8（约 25ms）。BCrypt 的慢是故意设计的——让暴力破解变得不可行。

---

## 5. 核心业务流程详解

### 5.1 购票流程（最重要的业务流程）

```
用户登录 → 浏览影片 → 选择场次 → 查看座位图 → 选择座位
    → 创建订单（锁定座位15分钟）
    → 支付订单（座位标记为已售）
    → 取票/检票入场
```

#### 第一步：创建订单 `OrderServiceImpl.createOrder()`

```java
@Transactional  // 📖 事务：下面所有数据库操作要么全部成功，要么全部回滚
public Order createOrder(OrderRequest request, Long userId) {
    // 1. 验证场次存在且状态正常
    // 2. 验证所有座位存在且状态为"空闲"(status=0)
    // 3. 乐观锁锁定座位: UPDATE seat SET status=1 WHERE id=? AND status=0
    //    ↑ 如果座位刚被别人锁定，affected rows=0，抛出异常
    // 4. 计算票价（基础票价 + 座位分区加价 + 时段折扣 + 人群折扣）
    // 5. 生成唯一订单号
    // 6. 插入订单(status=0-待支付)
    // 7. 增加场次已售计数
}
```

#### 第二步：支付订单 `OrderServiceImpl.payOrder()`

```java
@Transactional
public Order payOrder(Long orderId, Long userId) {
    // 1. 验证订单属于当前用户且状态为"待支付"
    // 2. 更新订单状态: UPDATE order SET status=1 WHERE id=? AND status=0
    //    ↑ 乐观锁：只更新待支付的订单，防止重复支付
    // 3. 将所有座位从"已锁定"改为"已售出": UPDATE seat SET status=2 WHERE id=?
    // 4. 累积会员积分
    // 5. 记录支付流水
}
```

> 📖 **乐观锁 (Optimistic Locking)**：不像数据库行锁那样"悲观"地锁住行等别人释放，而是用版本/状态条件来更新。SQL是 `UPDATE ... WHERE status=0`，如果返回影响行数为0，说明记录已被别人修改，直接报错。这种方式不阻塞其他事务，在高并发场景下性能远好于悲观锁。

> 📖 **@Transactional**：Spring 的事务注解。当方法中任何一步抛出异常时，已执行的数据库操作会自动回滚。保证数据一致性——不会出现"订单创建了但座位没锁"或"钱扣了但票没出"的情况。

### 5.2 改签流程 `OrderServiceImpl.reschedule()`

```
验证原订单 → 验证新场次 → 锁定新座位 → 释放旧座位
    → 计算差价（新票价 - 旧票价）
    → 创建新订单(status=3-已改签，originalOrderId=原订单ID)
    → 更新旧订单(status=3-已改签)
    → 记录操作日志
```

### 5.3 退票流程 `OrderServiceImpl.refund()`

```java
// 阶梯退款费率（对应用户行为激励）
退票时间距离开场 > 24小时: 退款 100%（免费退）
退票时间距离开场 2-24小时: 退款 80%（扣20%手续费）
退票时间距离开场 < 2小时: 退款 50%（扣50%手续费）
```

### 5.4 座位超时释放机制（两层防护）

```
第一层：订单支付时，如果超时（默认15分钟），订单被标记为"已过期"
        ↓
第二层：定时任务每5分钟扫描 lock_time > 30分钟 且 status=1 的座位 → 释放
        这是兜底机制，防止第一层逻辑因系统重启等原因未执行
```

### 5.5 模块Q&A

**Q5: 为什么锁定座位用乐观锁而不是数据库行锁(SELECT FOR UPDATE)？**
A: 行锁在查询时加锁，其他事务必须等待。在高并发抢座场景下，所有请求会串行排队，吞吐量极低。乐观锁只在实际更新时检查，并发请求可以同时执行，其中一个成功、其余失败——失败者只需要重新选座即可，用户体验远好于等待。

**Q6: 如果用户在支付页停留太久，座位被释放后又有人买了，支付时会发生什么？**
A: 支付时座位标记为已售出的SQL是 `UPDATE seat SET status=2 WHERE id=? AND status=0`（只有空闲座位才标记）。但座位在订单创建时已变为 status=1（锁定），所以这个SQL会失败。代码会检查 affected rows，如果为0则抛出"座位已被释放"异常，阻止支付。

**Q7: 退票的阶梯费率为什么这样设计？**
A: 越早退票影院损失越小——24小时前退票，影院有足够时间重新售出该座位。开演前2小时内退票，座位大概率卖不出去了，所以扣50%。这种设计是行业通用做法（参考猫眼/淘票票）。

**Q8: 改签时如果新场次票价更贵或更便宜怎么处理？**
A: 新旧价格对比——新票价 > 旧票价时，用户需要补差价；新票价 < 旧票价时，退差价。具体实现在 `OrderServiceImpl.reschedule()` 中计算 priceDiff。

---

## 6. 后端模块逐个解析

### 6.1 认证模块 (AuthController + AuthServiceImpl)

**功能**：登录、注册、修改密码、登出

**核心逻辑**：
- 登录时判断 `loginType` 参数决定查User表还是Employee表
- 密码使用 BCrypt 验证（存储的也是BCrypt哈希，永不明文存储）
- 登录成功生成JWT令牌，返回用户信息+角色+权限列表
- 登出时将当前Token加入黑名单

**登录限流器 LoginRateLimiter**：
```java
// 每个key(IP:用户名)最多连续失败5次
// 超过后锁定15分钟
// 登录成功自动清除失败计数
```

**Q9: 为什么注册只支持普通用户，不支持注册为管理员？**
A: 安全设计。管理员账号由超级管理员在后台创建，防止任何人注册后拥有管理权限。这是最小权限原则的体现。

**Q10: 修改密码后为什么要将当前Token加入黑名单？**
A: 防止旧密码泄露场景——如果有人获取了用户的旧密码和当前Token，修改密码后如果不让旧Token失效，攻击者仍能操作账户。加入黑名单强制所有设备重新登录。

---

### 6.2 影片模块 (MovieController + MovieServiceImpl)

**功能**：影片CRUD、搜索、热门设置、状态管理

**排序逻辑**：
```sql
ORDER BY sort_order DESC, create_time DESC
-- 先按排序权重降序（数字越大越靠前），再按创建时间降序
```

**搜索实现**：
```sql
-- 支持按片名、导演、主演模糊匹配
WHERE movie_name LIKE '%关键词%' 
   OR director LIKE '%关键词%' 
   OR actors LIKE '%关键词%'
```

**自动上下架**（定时任务 `autoUpdateMovieStatus`）：
```java
// 每小时检查一次：release_date 已到 → 自动从"即将上映"变为"上架"
// 过期30天的影片 → 日志提醒（不自动下架，人工操作更安全）
```

**Q11: 热门设置和排序权重有什么区别？**
A: `isHot`（热门）是布尔标记，用于"首页推荐"展示。`sortOrder`（排序权重）是数字，用于精细控制列表中的展示顺序。两者独立——一部影片可以不热门但排在高位（如正在热映的进口大片但未标记热门）。

**Q12: 影片搜索为什么不用Elasticsearch等搜索引擎？**
A: 对于单体应用、小规模数据（几百到几千部影片），MySQL的LIKE查询足够快。如果未来数据量增长到万级以上，可以升级为Elasticsearch。项目保持了架构的简洁性，不过度设计。

---

### 6.3 影厅模块 (HallController + HallServiceImpl)

**功能**：影厅CRUD、状态管理、座位布局配置

**座位布局**：
```java
// seatLayout: JSON数组记录不可用座位 ["1-5", "2-10"]
// layoutCfg: 增强布局配置（含过道、情侣座、无障碍座）
```

**删除影厅的安全检查**：
```java
// 1. 检查是否有进行中的场次 → 有则禁止删除
// 2. 检查是否有已取消但关联售票的场次 → 有则警告
// 3. 都通过才允许删除
```

**Q13: 为什么影厅的座位布局分开存储在Hall表和Seat表？**
A: Hall存储的是"物理布局"（哪些位置有座位），Seat存储的是"每场次座位状态"。这样设计支持影厅布局变更不影响历史订单。

---

### 6.4 场次模块 (ScheduleController + ScheduleServiceImpl) **最复杂的模块之一**

**功能**：场次CRUD、时间冲突检查、座位自动生成、批量排片

**时间冲突检查逻辑（核心算法）**：
```java
// 同一影厅内，新场次开始时间必须 >= 已有场次结束时间 + 缓冲时间(20分钟)
// 缓冲时间用于观众退场 + 清洁 + 下一场观众入场
LocalDateTime newEnd = startTime + 影片时长 + 20分钟缓冲
// 查询同影厅场次: WHERE hall_id = ? AND 时间重叠
```

**座位自动生成**：
```java
// 添加场次时自动生成 seat 表记录
// 座位编号映射: 1→A, 2→B, ... 26→Z, 27→AA, 28→AB ...
// 根据 Hall.seatLayout 中的不可用座位配置，将对应座位标记为 status=3(不可用)
// 根据 Hall.layoutCfg 中的增强布局配置，识别情侣座/无障碍座等特殊座位
```

**批量排片**：
```java
// 参数: [{hallId, movieId}, ...] × [日期范围] × [时段列表]
// 如: 3个影厅 × 7天 × 4个时段 = 最多84个场次
// 每个场次都会自动进行时间冲突检查，冲突则跳过并记录日志
```

**Q14: 为什么把座位信息放到每个场次而不是全局共享？**
A: 因为不同场次的同一物理位置状态不同。全局共享意味着"3排5座今天下午场卖了，晚上场也不能卖"，这不合理。按场次独立管理座位状态是正确的建模。

**Q15: 缓冲时间20分钟是固定的吗？**
A: 系统配置表中有 `buffer_minutes` 配置项，管理员可以通过系统设置修改。默认值20分钟是行业标准（通常15-30分钟）。

---

### 6.5 订单模块 (OrderController + AdminOrderController + OrderServiceImpl) **最长最复杂的模块**

**功能**：创建订单、支付、改签、退票、取消过期订单、管理端协助下单

**座位编号存储格式**：
```java
// 单个座位: "A-05"
// 多个座位: 逗号分隔 "A-05,B-06,B-07"
```

**管理端协助下单**：
```java
// 场景: 顾客到柜台买票，员工在后台操作
// assistCreate: 员工替顾客下单（记录 cashierId）
// assistPay: 员工确认收款后帮顾客支付
```

**取消过期订单**：
```java
// 定时任务 cancelExpired: 每分钟执行
// 查询创建超过15分钟(status=0)的订单
// 乐观锁取消: UPDATE order SET status=5 WHERE id=? AND status=0
// 释放对应座位（也是乐观锁）
```

**Q16: 为什么订单表的表名要用反引号 \`order\`？**
A: `order` 是 MySQL 的保留关键字。用反引号包裹告诉MySQL这是表名不是SQL关键字。更优雅的做法是建表时命名为 `t_order` 或 `ttms_order`，项目注释中也提到了这一点。

**Q17: 为什么"取消过期订单"和"支付"可能同时发生时不会出错？**
A: 两边都用乐观锁——取消是 `WHERE status=0`，支付也是 `WHERE status=0`。谁先执行谁成功，后执行的SQL影响行数为0，代码检测后不做处理。这种无锁并发控制保证了数据安全。

---

### 6.6 POS柜台售票模块 (PosController)

**功能**：线下柜台快速售票、支持现金/微信/支付宝

**与用户端下单的区别**：
| 维度 | 用户端 | POS端 |
|------|--------|-------|
| 订单状态 | 先创建(待支付)，再支付 | 直接完成(status=1) |
| 支付方式 | 微信/支付宝/余额 | 现金/微信/支付宝 |
| 用户关联 | 必须有user_id | 可指定客户ID或默认操作员 |
| 会员折扣 | 自动应用 | 关联客户时应用 |
| 积分 | 支付后自动累积 | 出票后自动累积 |
| 订单号前缀 | 日期+随机 | POS+日期+随机 |

**Q18: POS端为什么不走"创建→支付"两步流程？**
A: 柜台场景是面对面交易，顾客当场付款，不需要线上支付环节。简化为一步流程减少等待时间，符合柜台售票的效率要求。

---

### 6.7 票价计算引擎 (PricingServiceImpl)

**这是整个系统中定价逻辑的核心**

```java
// 三层定价叠加计算：
最终票价 = 基础票价 + 座位分区加价 → × 时段系数 → × 人群折扣

// 时段系数:
10:00前       → ×0.50 (早场5折)
10:00-18:00   → ×1.00 (正常价格)
18:00-21:00   → ×1.20 (黄金时段溢价20%)
22:00后       → ×0.80 (深夜场8折)

// 人群折扣:
STUDENT (学生) → ×0.50 (5折)
CHILD   (儿童) → ×0.50
SENIOR  (老人) → ×0.50
DISABLED(残障) → ×0.50
MILITARY(军人) → ×0.80
普通票        → ×1.00
```

> 📖 **RoundingMode.HALF_UP**：四舍五入模式，确保价格保留2位小数。

**Q19: 为什么座位分区定价存在Seat表而不是Hall表？**
A: 同一影厅不同场次可以有不同分区定价策略。比如IMAX厅平时所有座位同价，但首映场可能前排加价。存Seat表按场次灵活配置。

---

### 6.8 支付模块 (PaymentServiceImpl)

**当前实现**：Mock模拟支付（真实部署时替换为微信/支付宝SDK）

```java
// 创建支付 → 生成交易流水号
// simulatePaymentCallback → 模拟支付回调成功
// refund → 模拟退款（生成退款流水号）
```

**Q20: 为什么支付要做成独立服务？**
A: 支付逻辑与订单逻辑解耦。如果未来要接入多种支付方式（微信、支付宝、银联、Apple Pay等），只需修改这个服务，不影响订单模块。

---

### 6.9 票房统计模块 (BoxOfficeController + BoxOfficeServiceImpl)

**功能**：票房排行榜、大盘数据、影片票房详情、趋势图

**综合票房 vs 分账票房**：
```java
综合票房(comprehensive): 售票总金额（用户实际支付）
分账票房(share): 售票总金额 × 影院分账比例（默认52%）
// 分账比例来自系统配置 share_ratio，可调整
```

> 📖 **分账票房**：电影行业的术语。一部电影的票房收入需要在影院、制片方、发行方之间分配。中国通常影院拿约52%，制片方+发行方拿约48%（具体比例因片而异）。分账票房就是排除税费后按比例分配的那部分。

**Q21: 为什么要在数据库层做聚合而非Java层？**
A: 项目的 `OrderMapper` 中有专门的聚合查询（`aggregateByMovie`、`aggregateDailyRevenue`等），在SQL中用 `SUM`/`COUNT`/`GROUP BY` 完成汇总。如果拉所有订单到Java内存再计算，十几万条订单会耗尽内存（OOM —— Out Of Memory）。数据库聚合只返回几十条汇总结果，内存开销极小。

---

### 6.10 会员体系 (MemberController + MemberServiceImpl)

**4个会员等级**：
| 等级 | 累计消费 | 折扣率 | 积分倍率 |
|------|---------|--------|---------|
| 普通会员 | ¥0+ | 1.00 (无折扣) | 1.00× |
| 银卡会员 | ¥500+ | 0.95 (95折) | 1.20× |
| 金卡会员 | ¥2,000+ | 0.88 (88折) | 1.50× |
| 钻石会员 | ¥5,000+ | 0.80 (8折) | 2.00× |

**积分规则**：1元 = 1积分（基础倍率）× 会员积分倍率
**积分兑换**：100积分 = ¥5优惠券

**自动升级**：每次累积积分时检查是否达到下一级的消费门槛

**Q22: 消费金额存储的是累计值，退款时需要扣减吗？**
A: 项目当前实现是扣减的——退票时会减少积分。但累计消费金额的扣减逻辑目前比较简化，完整实现需要追踪每笔消费和退款的历史。

---

### 6.11 优惠券系统 (CouponController + CouponServiceImpl)

**两种类型**：
```java
FIXED (满减券)  : 如"满50减10" → 直接减10元
PERCENT (折扣券): 如"9折券" → 订单金额 × 0.10 = 折扣额
```

**使用流程**：领取 → 支付时选择 → 计算优惠金额 → 核销（标记已使用）

**Q23: 优惠券的 `remaining_qty` 扣减有并发问题吗？**
A: 有的。`decrementQty` 使用乐观锁 `WHERE remaining_qty > 0`，如果并发领取导致库存不足，后到的请求会失败。这是一个简化的处理——对于影院优惠券场景并发量不大，足够用了。

---

### 6.12 团体预约 (GroupBookingController)

**功能**：企业/团体包场预约

**业务流程**：用户提交预约 → 管理员审核 → 通过/拒绝 → 线下沟通具体安排

---

### 6.13 交接班模块 (ShiftController + ShiftServiceImpl)

**功能**：员工上班签到、下班交班结算

**交班记录包含**：
```java
现金收款、微信收款、支付宝收款  → 与实际系统统计对比
售票数、退票数                   → 核对差异
备注                            → 异常情况说明
```

**Q24: 为什么要设计交接班功能？**
A: 这是实体影院运营的真实需求。每个收银员下班时需要核对系统记录的收款与实际现金/扫码收款是否一致，确保没有短款或长款。这也是财务审计的重要环节。

---

### 6.14 报表模块 (ReportController + ReportServiceImpl)

**三种报表**：
- **日报**：每日凌晨2点自动生成昨天的日报
- **周报**：每周一凌晨3点生成上周周报
- **月报**：通过API手动触发或定时生成

**报表内容**：`content` 字段存储JSON格式的完整报表数据（营收、订单数、售票数、排行等）

---

### 6.15 数据看板 (DashboardController + DashboardServiceImpl)

**管理端首页仪表盘**：今日营收、今日订单数、今日售票数、今日场次数、在映影片数

---

### 6.16 其他模块速览

| 模块 | Controller | 说明 |
|------|-----------|------|
| 文件上传 | FileController | 魔数验证+安全扩展名推断 |
| 发票管理 | InvoiceController | 电子发票申请与开具 |
| 卖品管理 | SnackController + PublicSnackController | 小吃/套餐CRUD+售卖 |
| 取票检票 | TicketController | 取票码生成+扫码入场 |
| 统计 | StatisticsController | 营收/排行/Excel导出 |
| 系统设置 | SystemController | 配置管理+日志查询+主题偏好 |

---

## 6.17 核心代码走查：OrderServiceImpl 完整解析

这是整个项目**最长（950行）、最复杂**的服务类。下面逐方法走查。

### createOrder() 购票创建订单

```java
@Transactional  // 事务：10步操作，任一步失败全部回滚
public Order createOrder(OrderRequest request, Long userId) {
    // 第一步：验证场次存在且可以购票
    Schedule schedule = scheduleMapper.selectById(request.getScheduleId());
    if (schedule.getStatus() != 1) throw ...
    if (schedule.getStartTime().isBefore(now())) throw ...  // 已开场的不能买

    // 第二步：逐个座位验证存在且空闲——注意此时只读不锁，锁在后面
    for (String seatNumber : seatNumbers) {
        Seat seat = seatMapper.selectByScheduleAndNumber(scheduleId, seatNumber);
        if (seat.getStatus() != 0) throw ...  // 不是空闲座位就拒绝
    }

    // 第三步：生成唯一订单号——yyyyMMdd + 8位随机字母数字
    String orderNo = generateOrderNo();
    // 查重：极小概率碰撞时递归重试

    // 第四步：先插入订单记录获取ID，状态=0(待支付)
    orderMapper.insert(order);

    // 第五步：锁定座位——这是乐观锁的关键点
    for (String seatNumber : seatNumbers) {
        int locked = seatMapper.lockSeat(seat.getId(), order.getId());
        // lockSeat 的SQL: UPDATE seat SET status=1 WHERE id=? AND status=0
        // 如果A和B同时点了同一座位，只有一个的affected rows=1，另一个=0
        if (locked != 1) throw ...  // 锁定失败则事务回滚
    }

    // 第六步：计算票价
    // = 基础票价 + 座位分区加价(PricingServiceImpl)
    // × 时段系数(早场5折/晚间1.2倍/深夜8折)
    // × 人群折扣(学生5折/军人8折)
    // × 会员折扣(银卡95折/金卡88折/钻石8折)
    totalPrice = pricingService.calculateOrderTotal(schedule, seatNumbers, ticketTypes);
    totalPrice = totalPrice × memberService.getDiscountRate(userId);

    // 第七步：更新订单总价
    order.setTotalPrice(totalPrice);
    orderMapper.updateById(order);

    // 第八步：原子增加场次已售计数
    scheduleMapper.incrementSoldCount(schedule.getId(), seatNumbers.size());

    // 第九步：记录操作日志到 order_log 表

    // 第十步：补充关联信息(movieName/hallName/startTime)返回给前端
}
```

**关键设计决策**：为什么先INSERT订单再锁定座位？因为锁定座位时需要order_id关联，必须先有订单ID。如果锁定失败，事务回滚会自动删除刚插入的订单——这是`@Transactional`的核心价值。

### payOrder() 支付订单

```java
@Transactional
public Order payOrder(Long orderId, Long userId) {
    // 状态校验：按订单状态逐一给出明确的错误提示
    if (order.getStatus() != 0) {
        if (status == 1) throw "已支付，请勿重复支付"
        if (status == 3) throw "已改签"
        if (status == 4) throw "已退票"
        if (status == 5) throw "已过期"
    }

    // 检查场次是否已开场——开演后不能再支付
    if (schedule.getStartTime().isBefore(now())) throw ...

    // 将所有座位从"已锁定"(1)改为"已售出"(2)
    for (seatNumber : seatNumbers) {
        seatMapper.markSold(seat.getId());  // UPDATE seat SET status=2 WHERE id=?
    }

    // 如果是余额支付，扣减储值余额
    if ("BALANCE".equals(paymentMethod)) {
        memberService.payWithBalance(userId, totalPrice);
    }

    // 更新订单状态: 0→1
    order.setStatus(1);  // 待观影
    order.setPayTime(now());
    orderMapper.updateById(order);

    // 累积积分+自动升级会员等级
    memberService.accumulatePoints(userId, totalPrice);
    // ↑ 注意：积分失败不影响支付结果(catch后仅log不抛异常)
}
```

**关键设计决策**：积分累积失败被catch后只记日志不抛异常。如果因为积分逻辑bug导致支付失败，对用户体验的影响远大于积分少加。这是一个务实的取舍。

### reschedule() 改签

```java
@Transactional
public Order reschedule(RescheduleRequest request, Long userId) {
    // 验证原订单: 必须是status=1(待观影)且场次未开始
    // 验证新场次: 必须有效且未开始
    // 验证不能改签到同一场次
    // 验证新座位都空闲

    // 释放旧座位+减少旧场次已售计数
    seatMapper.releaseSeatsByOrderId(oldOrder.getId());
    scheduleMapper.decrementSoldCount(oldSchedule.getId(), oldOrder.getSeatCount());

    // 原订单标记为已改签(status=3)
    oldOrder.setStatus(3);

    // 计算价差
    priceDiff = newTotalPrice - oldTotalPrice;

    // 创建新订单
    if (priceDiff > 0) {
        newOrder.setStatus(0);  // 需补差价=待支付
    } else {
        newOrder.setStatus(1);  // 无需补=直接待观影
        newOrder.setPayTime(now());
    }

    // 锁定新座位(已支付的直接标已售)
    // 记录操作日志(含价差详情)
}
```

**关键设计决策**：改签后新票价更高时需要补差价（新订单待支付），新票价更低时退差价且新订单直接生效。这符合猫眼/淘票票的实际业务规则。

### refund() 退票

```java
// 阶梯退款费率
calculateRefundFee(schedule, totalPrice):
  距离开场 >= 24小时 → 0%手续费（全退）
  距离开场 2~24小时 → 20%手续费
  距离开场 < 2小时   → 50%手续费

// 退款时：释放座位、减少场次计数、订单状态改为4
```

### cancelExpired() 取消过期订单（定时任务）

```java
@Scheduled(fixedDelay = 120000)  // 每2分钟
public void cancelExpired() {
    // 1. 读配置: order_timeout（默认15分钟）
    // 2. 查过期订单: WHERE status=0 AND create_time < NOW() - timeout
    // 3. 乐观锁取消: UPDATE order SET status=5 WHERE id=? AND status=0
    //    如果 affected rows=0 → 说明用户刚好支付了，跳过
    // 4. 乐观锁释放座位: UPDATE seat SET status=0 WHERE order_id=? AND status=1
    //    同样只释放已锁定的，不误释放已售出的
    // 5. 记日志(操作人=SYSTEM)
}
```

**关键设计决策**：为什么取消订单和释放座位都用乐观锁（带状态条件），而不是在Java层加 `synchronized`？因为这是后台定时任务，与用户支付请求不在同一个JVM线程内，Java锁无效。只有数据库层的条件UPDATE能保证并发安全。

## 6.18 核心代码走查：ScheduleServiceImpl 座位生成算法

### 行号字母映射算法

```java
// 0→A, 1→B, ..., 25→Z, 26→AA, 27→AB, ..., 51→AZ, 52→BA ...
// 本质是Excel列名的生成逻辑：26进制但无0位
private String getRowLetter(int rowIndex) {
    StringBuilder sb = new StringBuilder();
    int index = rowIndex;
    while (index >= 0) {
        sb.insert(0, ROW_LETTERS[index % 26]);  // 取余数→字母
        index = index / 26 - 1;                  // 进位后-1（因为从0开始）
    }
    return sb.toString();
}
```

> 📖 **为什么是26进制但处理特殊？** 普通的26进制是0-25循环，但字母A-Z对应1-26（没有0映射）。所以用 `index/26 - 1` 来补偿。这是Excel列名算法的经典实现。

### 时间冲突检测算法

```java
private void checkTimeConflict(hallId, startTime, endTime, excludeId) {
    // 两个时间段重叠的数学公式：
    // 时间段A和B有交集 ⟺ A开始 < B结束 AND A结束 > B开始
    // 例如：新场次 14:00-16:20，已有场次 15:00-17:00
    //   14:00 < 17:00 ✓  AND  16:20 > 15:00 ✓  → 冲突！
    for (Schedule existing : hallSchedules) {
        if (startTime.isBefore(existing.getEndTime())
            && endTime.isAfter(existing.getStartTime())) {
            throw new BusinessException("时间段冲突！);
        }
    }
}
```

### N+1查询优化

```java
// ❌ 错误做法：循环查库（N次SQL）
for (Schedule s : schedules) {
    Movie m = movieMapper.selectById(s.getMovieId());  // 每次一条SQL
    s.setMovieName(m.getMovieName());
}

// ✅ 正确做法：一次性批量查询后在内存映射（2次SQL）
Set<Long> movieIds = schedules.stream().map(Schedule::getMovieId).collect(toSet());
Map<Long, Movie> movieMap = movieMapper.selectBatchIds(movieIds)
    .stream().collect(toMap(Movie::getId, m -> m));
// selectBatchIds 内部用 WHERE id IN (1,2,3,...) 一条SQL完成
for (Schedule s : schedules) {
    Movie m = movieMap.get(s.getMovieId());  // 内存查找，纳秒级
    s.setMovieName(m.getMovieName());
}
```

> 📖 **N+1查询问题**：查N条记录后，每条记录又执行1次关联查询，总共N+1次SQL。100个场次就是101次数据库往返。批量查询优化后只需2次——1次查主表，1次批量查关联表。

### 锁定座位数的批量统计

```java
// 进一步优化：用 GROUP BY 一次SQL替代N次 COUNT
List<Long> scheduleIds = schedules.stream().map(Schedule::getId).toList();
List<Map<String, Object>> lockedCounts = seatMapper.countLockedByScheduleIds(scheduleIds);
// SQL: SELECT schedule_id, COUNT(*) FROM seat WHERE status=1 AND schedule_id IN (1,2,...) GROUP BY schedule_id
```

---

## 7. 前端架构深度解析

### 7.1 技术栈

```
Vue 3 (Composition API)  ← 前端框架
Element Plus             ← UI组件库（表格/表单/对话框/菜单等）
ECharts                  ← 数据可视化（图表）
Pinia                    ← 状态管理（替代Vuex）
Vue Router               ← 路由管理
Vite                     ← 构建工具（替代Webpack）
```

### 7.2 页面结构

```
前台用户端:
├── Home.vue         首页（热门影片展示）
├── MovieDetail.vue  影片详情 + 选场次
├── SeatSelection.vue 可视化选座
├── MyOrders.vue     我的订单
├── MyCoupons.vue    我的优惠券
├── Profile.vue      个人中心
├── Login.vue        登录
└── Register.vue     注册

后台管理端:
├── Dashboard.vue      数据看板仪表盘
├── MovieManage.vue    影片管理
├── HallManage.vue     影厅管理
├── ScheduleManage.vue 场次排片
├── OrderManage.vue    订单管理
├── PosView.vue        柜台售票
├── Statistics.vue     数据统计
├── BoxOffice.vue      票房管理
├── EmployeeManage.vue 员工管理
├── MemberManage.vue   会员管理
├── CouponManage.vue   优惠券管理
├── SnackManage.vue    卖品管理
├── ShiftManage.vue    交接班
├── ReportView.vue     报表
├── SystemSettings.vue 系统设置
└── NotFound.vue       404页面
```

### 7.3 核心组件

| 组件 | 功能 |
|------|------|
| NavBar.vue | 前台导航栏 |
| AdminHeader.vue | 后台顶栏 |
| AdminSidebar.vue | 后台侧边栏菜单 |
| MovieCard.vue | 影片卡片 |
| SeatGrid.vue | 选座网格（核心组件） |
| ThemeSwitcher.vue | 主题切换器 |

### 7.4 状态管理 (Pinia)

```javascript
// stores/auth.js  — 认证状态
token, userId, username, roleCode, permissions, theme

// stores/theme.js — 主题状态
当前主题模式 (light/dark)
```

### 7.5 API请求层核心实现

```javascript
// api/index.js — Axios实例封装
const service = axios.create({
  baseURL: '/api',       // 所有请求自动加 /api 前缀
  timeout: 15000         // 15秒超时
})

// 请求拦截器：自动在每次请求头附加JWT令牌
service.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截器：统一处理错误
service.interceptors.response.use(
  (response) => {
    // 非200状态码统一弹窗提示并reject
    if (res.code !== 200) {
      ElMessage.error(res.message)
      return Promise.reject(...)
    }
    return res  // 直接返回res.data，组件中无需再取.data
  },
  (error) => {
    // 401 → 清除本地token，跳转登录页（防重定向循环）
    if (status === 401) {
      localStorage.removeItem('token')
      if (!window.location.pathname.includes('/login')) {
        window.location.href = '/login'
      }
    }
    // 403 → 权限不足 | 500 → 服务器错误
  }
)
```

**关键设计点**：
- 响应拦截器对401做**重定向循环防护**——如果已经在 `/login` 页面就不再重定向
- 对blob类型响应（文件下载）不做JSON校验
- `baseURL: '/api'` 配合后端 `WebMvcConfig` 的SPA路由回退，确保开发时Vite代理正确

### 7.6 路由设计与权限守卫

```javascript
// router/index.js — 路由配置 + 导航守卫
const routes = [
  // 公开路由(public: true): 首页、登录、注册、电影详情
  { path: '/home', meta: { public: true } },
  // 用户路由(requiresAuth + ROLE_USER): 选座、我的订单、个人中心
  { path: '/booking/:scheduleId', meta: { requiresAuth: true, role: 'ROLE_USER' } },
  // 管理路由(requiresAdmin): 管理后台仪表盘、订单管理、票房等
  { path: '/admin/dashboard', meta: { requiresAuth: true, requiresAdmin: true } },
  // 超级管理员路由(ROLE_SUPER_ADMIN): 员工管理、系统设置
  { path: '/admin/employees', meta: { role: 'ROLE_SUPER_ADMIN' } },
]

// 导航守卫：每次路由切换前的权限校验
router.beforeEach((to, from, next) => {
  // 1. 公开路由 → 直接放行
  if (to.meta.public) return next()

  // 2. 需要登录但没登录 → 跳转到登录页（记住来源路径）
  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    return next({ name: 'Login', query: { redirect: to.fullPath } })
  }

  // 3. 管理端路由但非管理员 → 拒绝并跳转首页
  if (to.meta.requiresAdmin && !authStore.isAdmin) {
    return next({ name: 'Home' })
  }

  // 4. 超级管理员专属页面 → 非超管重定向到仪表盘
  if (to.meta.role === 'ROLE_SUPER_ADMIN') {
    if (authStore.user?.roleCode !== 'ROLE_SUPER_ADMIN') {
      return next({ name: 'AdminDashboard' })
    }
  }

  next()
})
```

**关键设计点**：
- 三级权限校验：`public` < `requiresAuth` < `requiresAdmin` < `ROLE_SUPER_ADMIN`
- `query: { redirect }` 保存登录前URL，登录后自动跳回（改善UX）
- `sessionStorage` 记住最后访问的路由，页面刷新后可恢复

### 7.7 Pinia 认证状态管理

```javascript
// stores/auth.js — Composition API 风格
export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const user = ref(parseStoredUser())  // 从localStorage恢复

  // 计算属性
  const isAdmin = computed(() =>
    user.value?.roleCode === 'ROLE_SUPER_ADMIN' || user.value?.roleCode === 'ROLE_STAFF'
  )
  const isSuperAdmin = computed(() => user.value?.roleCode === 'ROLE_SUPER_ADMIN')

  // 登录：调API → 存token到localStorage → 更新响应式状态
  async function login(credentials) {
    const res = await loginApi({ username, password })
    token.value = res.data.token
    user.value = { userId, username, roleCode, permissions, theme }
    localStorage.setItem('token', res.data.token)
    localStorage.setItem('user', JSON.stringify(user.value))
  }

  // 登出：清除内存和localStorage
  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }
})
```

> 📖 **Pinia vs Vuex**：Pinia 是 Vue 3 官方推荐的状态管理库。优势在于：支持 Composition API、天然TypeScript友好、不再有 mutations/actions 的概念区分、体积更小。

### 7.8 SeatGrid 选座组件核心实现

这是前台最关键的交互组件，实现可视化选座。

```javascript
// 核心逻辑
function getSeatClass(row, col) {
  const key = `${rowLabel(row)}-${String(col).padStart(2, '0')}`  // 例如 "A-05"

  if (props.selectedSeats.includes(key)) return 'selected'  // 黄色（用户选中）
  const status = props.seatStatusMap[key]
  if (status === 2) return 'occupied'   // 红色 ×（已售出，不可点击）
  if (status === 1) return 'locked'     // 灰色 🔒（锁定中，不可点击）
  if (status === 3) return 'aisle'      // 透明（过道/不可用，不可点击）
  return 'available'                    // 绿色（空闲，可点击）
}

function handleSeatClick(row, col) {
  const cls = getSeatClass(row, col)
  // 只有available和selected可以点击
  if (cls === 'occupied' || cls === 'locked' || cls === 'aisle') return
  if (cls === 'selected') {
    emit('deselect-seat', key)   // 取消选择
  } else {
    if (selectedSeats.length >= maxSelect) return  // 最多选6个
    emit('select-seat', key)     // 选择座位
  }
}
```

**CSS视觉设计**：
```css
.available { background: #67c23a }  /* 绿色——可购买 */
.selected  { background: #e6a23c }  /* 橙色——用户选中 */
.occupied  { background: #f56c6c; cursor: not-allowed }  /* 红色——他人已买 */
.locked    { background: #909399; cursor: not-allowed }  /* 灰色——他人锁定中 */
.aisle     { background: transparent; cursor: default }  /* 透明——过道 */
```

**组件上方渲染"银幕"标识**，电影院座位图的标准做法——让用户能直观判断座位与银幕的相对位置。

### 7.9 主题系统

```
纯白主题 (white/light)  — 日间模式
暗黑主题 (dark)         — 夜间模式
蓝紫主题                 — 品牌色主题
```

主题偏好存储在：
1. 服务端：`user.theme` 字段（跨设备同步）
2. 客户端：`localStorage`（页面加载时立即应用，防闪烁）

---

## 8. 定时任务体系

### 8.1 任务清单

| 任务 | 频率 | 功能 |
|------|------|------|
| `autoEndSchedules` | 每分钟 | 将结束时间已过的场次标记为"已结束" |
| `releaseStaleLockedSeats` | 每5分钟 | 释放超时30分钟未支付的锁定座位（兜底） |
| `remindPendingPayment` | 每2分钟 | 提醒创建超过12分钟未支付的订单 |
| `autoUpdateMovieStatus` | 每小时 | 自动将"即将上映"影片改为"上架" |
| `cleanOldOrderLogs` | 每天凌晨3点 | 清理90天前的操作日志 |
| `generateDailyReport` | 每天凌晨2点 | 生成昨日日报 |
| `generateWeeklyReport` | 每周一凌晨3点 | 生成上周周报 |
| `TokenBlacklist.cleanExpired` | 每分钟 | 清理黑名单中已过期的Token |
| `LoginRateLimiter.cleanExpired` | 每10分钟 | 清理登录限流中已过期的记录 |

### 8.2 设计考量

所有定时任务都使用了 `try-catch` 包裹，确保一个任务异常不影响其他任务。定时任务的 `fixedDelay` 参数表示上一次执行完成后间隔多少毫秒再执行，而非固定频率。

> 📖 **Cron 表达式**：`0 0 3 * * ?` 的意思是"每天凌晨3点0分0秒"，格式为 `秒 分 时 日 月 周`。`*` 表示"每"，`?` 表示"不指定"。

---

## 9. 设计亮点与最佳实践总结

### 9.1 安全性

1. ✅ 密码BCrypt加密，永不明文存储
2. ✅ JWT令牌 + 黑名单机制，支持主动失效
3. ✅ 登录限流防暴力破解
4. ✅ 多维度权限控制（URL模式 + HTTP Method + 方法注解）
5. ✅ 文件上传魔数验证，防止恶意文件上传
6. ✅ Druid SQL防火墙，防御SQL注入
7. ✅ 密码字段 `@JsonProperty(WRITE_ONLY)` 防止泄露
8. ✅ 日志中用户名脱敏显示

### 9.2 性能

1. ✅ 乐观锁替代悲观锁，高并发下性能优越
2. ✅ 数据库层聚合查询，避免全量数据加载到JVM
3. ✅ 批量查询避免N+1问题（批量查角色、用户、影片信息）
4. ✅ Druid连接池管理数据库连接
5. ✅ 慢SQL监控（超过2秒自动记录）

### 9.3 可维护性

1. ✅ 统一ApiResponse格式，前端处理一致
2. ✅ 全局异常处理器，Controller零try-catch
3. ✅ 接口-实现分离，便于扩展和测试
4. ✅ 定时任务独立try-catch，互不影响
5. ✅ 多环境配置（开发/生产），12-Factor原则
6. ✅ Swagger API文档自动生成
7. ✅ Actuator健康检查端点

### 9.4 业务设计

1. ✅ 双表登录（User/Employee），职责清晰
2. ✅ 票价三层计算引擎（基础+分区+时段+人群）
3. ✅ 阶梯退票费率，符合行业惯例
4. ✅ 座位按场次独立管理，数据模型正确
5. ✅ 会员自动升降级，积分兑换闭环
6. ✅ 交接班财务核对，满足实体运营需求
7. ✅ 数据库迁移脚本（migration.sql），支持版本升级

### 9.5 可改进点

| 问题 | 建议 |
|------|------|
| TokenBlacklist用内存存储 | 多实例部署时应改用Redis共享 |
| LoginRateLimiter用内存存储 | 同上，Redis更合适 |
| 订单号生成依赖随机数 | 可考虑雪花算法(Snowflake)保证绝对唯一 |
| 支付为Mock实现 | 接入真实微信/支付宝SDK |
| 日志文件仅本地存储 | 接入ELK集中式日志平台 |
| 图片存储本地 | 接入OSS对象存储（阿里云/华为云） |
| 缺少单元测试 | 为关键业务逻辑补充JUnit测试 |
| 缺少接口限流 | 可引入Sentinel/Guava RateLimiter |

---

## 附录A: 专业名词索引

| 名词 | 解释 |
|------|------|
| **JWT** | JSON Web Token，无状态认证令牌，由Header.Payload.Signature三部分组成 |
| **BCrypt** | 一种故意慢速的密码哈希算法，自动加盐，防暴力破解 |
| **乐观锁** | 通过版本号或状态条件更新数据，不阻塞其他事务 |
| **悲观锁** | 使用数据库行锁，SELECT FOR UPDATE，其他事务等待 |
| **CSRF** | Cross-Site Request Forgery，跨站请求伪造攻击 |
| **CORS** | Cross-Origin Resource Sharing，跨域资源共享机制 |
| **N+1查询** | 查1条主记录后，循环查N条关联记录的性能问题 |
| **OOM** | Out Of Memory，内存耗尽 |
| **POI** | Apache POI，Java操作Office文档的库 |
| **ECharts** | 百度开源的数据可视化图表库 |
| **Pinia** | Vue 3 官方推荐的状态管理库 |
| **Vite** | 新一代前端构建工具，冷启动极快 |
| **Druid** | 阿里巴巴开源的高性能数据库连接池 |
| **MyBatis-Plus** | MyBatis增强工具，简化CRUD操作 |
| **SPA** | Single Page Application，单页面应用 |
| **RBAC** | Role-Based Access Control，基于角色的访问控制 |
| **MDC** | Mapped Diagnostic Context，日志追踪上下文 |
| **魔数** | Magic Bytes，文件开头的特定字节序列，标识文件真实类型 |
| **分账票房** | 扣除税费后在影院/制片方/发行方间分配的那部分票房 |
| **12-Factor** | 云原生应用开发的12条方法论 |
| **雪花算法** | Twitter开源的分布式ID生成算法，保证全局唯一 |

---

## 附录B: 完整API路由表

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| POST | `/api/auth/login` | 公开 | 登录 |
| POST | `/api/auth/register` | 公开 | 注册 |
| POST | `/api/auth/change-password` | 登录 | 修改密码 |
| POST | `/api/auth/logout` | 登录 | 登出 |
| GET | `/api/movies/list` | 公开 | 影片列表 |
| GET | `/api/movies/detail/{id}` | 公开 | 影片详情 |
| GET | `/api/movies/hot` | 公开 | 热门影片 |
| GET | `/api/movies/search` | 公开 | 搜索影片 |
| POST | `/api/movies/add` | 管理 | 添加影片 |
| PUT | `/api/movies/update` | 管理 | 更新影片 |
| DELETE | `/api/movies/delete/{id}` | 管理 | 删除影片 |
| GET | `/api/schedules/query/movie/{id}` | 公开 | 影片场次 |
| GET | `/api/schedules/query/upcoming` | 公开 | 即将上映 |
| GET | `/api/schedules/query/{id}/seats` | 公开 | 场次座位 |
| GET | `/api/schedules/list` | 管理 | 场次列表 |
| POST | `/api/schedules/add` | 管理 | 添加场次 |
| POST | `/api/schedules/batch` | 管理 | 批量排片 |
| GET | `/api/admin/halls/list` | 管理 | 影厅列表 |
| POST | `/api/admin/halls/add` | 管理 | 添加影厅 |
| POST | `/api/user/orders/create` | 用户 | 创建订单 |
| POST | `/api/user/orders/pay/{id}` | 用户 | 支付订单 |
| POST | `/api/user/orders/reschedule` | 用户 | 改签 |
| POST | `/api/user/orders/refund/{id}` | 用户 | 退票 |
| POST | `/api/user/orders/cancel/{id}` | 用户 | 取消订单 |
| GET | `/api/user/orders/my` | 用户 | 我的订单 |
| GET | `/api/admin/orders/list` | 管理 | 所有订单 |
| POST | `/api/admin/orders/assist-create` | 管理 | 协助下单 |
| POST | `/api/admin/pos/create-order` | 管理 | POS售票 |
| GET | `/api/admin/statistics/revenue` | 管理 | 营收统计 |
| GET | `/api/admin/statistics/movie-ranking` | 管理 | 影片排行 |
| GET | `/api/admin/statistics/export` | 管理 | Excel导出 |
| GET | `/api/admin/box-office/ranking` | 管理 | 票房排行 |
| GET | `/api/admin/box-office/dashboard` | 管理 | 大盘数据 |
| GET | `/api/admin/employees/list` | 管理 | 员工列表 |
| POST | `/api/admin/employees/add` | 管理 | 添加员工 |
| GET | `/api/admin/system/config` | 管理 | 系统配置 |
| GET | `/api/admin/dashboard` | 管理 | 数据看板 |
| GET | `/api/user/membership` | 用户 | 会员信息 |
| GET | `/api/user/coupons` | 用户 | 我的优惠券 |
| GET | `/api/snacks/combos` | 公开 | 卖品套餐 |
| GET | `/api/tickets/pickup/{id}` | 登录 | 取票信息 |
| POST | `/api/tickets/check-in` | 管理 | 检票入场 |
| POST | `/api/admin/shifts/start` | 管理 | 上班签到 |
| POST | `/api/admin/shifts/end` | 管理 | 下班交班 |
| POST | `/api/upload` | 管理 | 文件上传 |

---

> **文档版本**: v1.0  
> **生成日期**: 2026-06-11  
> **适用项目版本**: TTMS 1.0.0  

