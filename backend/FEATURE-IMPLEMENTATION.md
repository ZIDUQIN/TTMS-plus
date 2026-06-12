# TTMS 电影票务管理系统 — 功能实现全景文档

> 按功能模块逐一说明实现方式、核心流程、关键代码位置、设计亮点与注意事项

---

## 目录

1. [认证授权](#1-认证授权)
2. [用户注册与登录](#2-用户注册与登录)
3. [影片管理](#3-影片管理)
4. [影厅管理](#4-影厅管理)
5. [排片管理](#5-排片管理)
6. [选座购票](#6-选座购票)
7. [订单生命周期](#7-订单生命周期)
8. [支付系统](#8-支付系统)
9. [定价引擎](#9-定价引擎)
10. [会员体系](#10-会员体系)
11. [优惠券系统](#11-优惠券系统)
12. [卖品管理](#12-卖品管理)
13. [POS柜台售票](#13-pos柜台售票)
14. [取票与检票](#14-取票与检票)
15. [票房统计](#15-票房统计)
16. [数据统计与导出](#16-数据统计与导出)
17. [仪表盘](#17-仪表盘)
18. [团体订票](#18-团体订票)
19. [发票管理](#19-发票管理)
20. [交班管理](#20-交班管理)
21. [报表系统](#21-报表系统)
22. [文件上传](#22-文件上传)
23. [系统配置与主题](#23-系统配置与主题)
24. [操作日志与审计](#24-操作日志与审计)
25. [定时任务](#25-定时任务)
26. [安全体系](#26-安全体系)
27. [数据初始化](#27-数据初始化)

---

## 1. 认证授权

### 涉及文件

| 层次 | 文件 |
|------|------|
| 配置 | `config/SecurityConfig.java` |
| 过滤器 | `security/JwtAuthenticationFilter.java` |
| JWT工具 | `security/JwtTokenProvider.java` |
| 令牌黑名单 | `security/TokenBlacklist.java` |
| 登录限流 | `security/LoginRateLimiter.java` |

### JWT令牌机制

```
生成：HMAC-SHA256 签名，载荷包含 {userId, username, role, loginType}
配置：密钥(jwt.secret)、有效期(jwt.expiration, 默认24h)
密钥处理：不足32字节时自动SHA-256扩展至256位
```

**关键代码**：`JwtTokenProvider.java:69-87` — `generateToken()` 方法

### 请求认证流程

```
请求到达 → JwtAuthenticationFilter → 判断路径是否公开
  ├─ 公开路径：直接放行
  │   - /api/auth/login, /api/auth/register
  │   - /api/schedules/query/** (场次查询公开)
  │   - /uploads/** (静态文件)
  └─ 需认证路径：
      1. 从 Authorization 头提取 Bearer Token
      2. 校验 Token 签名与有效期
      3. 检查 Token  是否在黑名单中
      4. 解析 userId/role → 构建 UsernamePasswordAuthenticationToken
      5. 存入 SecurityContextHolder
      6. Spring Security 后续过滤器进行角色匹配
```

### URL权限设计（SecurityConfig）

```
GET  /api/movies/**    → 公开 (所有人可查看影片信息)
POST/PUT/DELETE /api/movies/** → ROLE_SUPER_ADMIN / ROLE_STAFF
GET  /api/schedules/query/** → 公开
      /api/schedules/** → ROLE_SUPER_ADMIN / ROLE_STAFF
      /api/admin/**     → ROLE_SUPER_ADMIN / ROLE_STAFF
      /api/user/**      → 需登录 (authenticated)
      /api/snacks/**    → 公开 (卖品信息所有人可查看)
      /api/upload       → ROLE_SUPER_ADMIN / ROLE_STAFF
      其他 /api/**       → 需认证
      /**               → 放行 (SPA静态资源)
```

### 令牌黑名单

**场景**：用户登出或修改密码后，旧的JWT令牌应立  即失效（JWT本身无状态，无法主动失效）

**实现**：`TokenBlacklist.java`
- 内存 `ConcurrentHashMap<String, Long>` 存储 token → 过期时间戳
- `JwtAuthenticationFilter` 在验证token后额外检查黑名单
- 定时任务每60秒清理已过期的黑名单条目
- 多机部署需切换为Redis实现

### 登录限流

**实现**：`LoginRateLimiter.java`
- 基于 IP + username 的失败计数
- 同一IP+用户名连续失败5次 → 锁定15分钟
- 登录成功后清除失败计数
- 定时任务每10分钟清理过期记录
- 单机适用，多机需Redis

---

## 2. 用户注册与登录

### 涉及文件

| 层次 | 文件 |
|------|------|
| Controller | `controller/AuthController.java` |
| Service | `service/impl/AuthServiceImpl.java` |
| DTO | `dto/LoginRequest.java`, `dto/LoginResponse.java`, `dto/RegisterRequest.java` |

### 登录流程（支持双端登录）

```
POST /api/auth/login { username, password, loginType }
  ├─ loginType="ADMIN" → 查询 Employee 表 → 验证 BCrypt 密码
  │   → 生成 JWT(loginType=ADMIN) → 返回角色权限列表
  ├─ loginType="USER"  → 查询 User 表 → 验证 BCrypt 密码
  │   → 生成 JWT(loginType=USER) → 返回 theme 和用户权限
  └─ loginType=null/""
      → 自动检测模式：先查 Employee 表，再查 User 表
      → 兼容前后端未明确传 loginType 的场景
```

**关键点**：
- 密码使用 **BCrypt(strength=10)** 加密，验证耗时约100ms
- 返回的 `LoginResponse` 包含 token、userId、username、realName、roleCode、roleName、permissions 列表、theme 设置
- 权限是 JSON 数组格式存储在数据库：`["movie:manage","hall:manage",...]`

### 注册流程

```
POST /api/auth/register { username, password, phone, email, nickname, realName }
  → 检查 User 表用户名唯一性
  → 检查 Employee 表是否有同名（防止混淆）
  → BCrypt 加密密码
  → status=0(正常), theme="white"(默认)
  → 返回 userId
```

### 密码安全规则

- 新密码长度 ≥ 8 位
- 必须同时包含字母和数字
- 新旧密码不能相同
- 修改成功后当前 Token 加入黑名单，强制重新登录

---

## 3. 影片管理

### 涉及文件

| 层次 | 文件 |
|------|------|
| Controller | `controller/MovieController.java` |
| Service接口 | `service/MovieService.java` |
| Service实现 | `service/impl/MovieServiceImpl.java` |
| Mapper | `mapper/MovieMapper.java` |
| Entity | `entity/Movie.java` |

### 影片状态模型

```
0 = 下架      ← 不可见，不可排片
1 = 上映      ← 正常可见，可排片
2 = 即将上映   ← 可见但不可排片（自动定时上架）
```

### 功能清单

| 操作 | 端点 | 关键逻辑 |
|------|------|----------|
| 分页列表 | `GET /api/movies/list` | 按 sort_order DESC + create_time DESC 排序，支持 status 筛选 |
| 详情 | `GET /api/movies/detail/{id}` | 单条查询 |
| 添加 | `POST /api/movies/add` | 默认 status=2(即将上映), isHot=0, sortOrder=当前最大+1 |
| 更新 | `PUT /api/movies/update` | 先检查是否存在，再更新 |
| 删除 | `DELETE /api/movies/delete/{id}` | MyBatis-Plus `@TableLogic` 逻辑删除 |
| 搜索 | `GET /api/movies/search` | 按片名/导演/主演模糊匹配 |
| 热门影片 | `GET /api/movies/hot` | 查询 is_hot=1 的记录 |
| 设置热门 | `PUT /api/movies/set-hot` | 设置 isHot 字段 |
| 设置状态 | `PUT /api/movies/set-status` | 0/1/2 校验 |

**安全规则**：GET 公开访问，POST/PUT/DELETE 需 STAFF 以上角色

---

## 4. 影厅管理

### 涉及文件

| 层次 | 文件 |
|------|------|
| Controller | `controller/HallController.java` |
| Service | `service/impl/HallServiceImpl.java` |
| Mapper | `mapper/HallMapper.java` |
| Entity | `entity/Hall.java` |

### 核心属性

```
影厅(hall): hallName, rowCount(行数), colCount(列数),
            capacity(总容量=行×列), seatLayout(不可用座位JSON),
            status(0=维护中/1=正常)
```

### 关键规则

- **添加**：名称唯一校验；capacity 自动 = rowCount × colCount
- **更新**：行列变更时自动重算 capacity
- **删除**：必须无进行中的场次，无已售出票的已取消场次
- **状态切换**：0(维护中) ⇄ 1(正常)，维护中的影厅不可排片
- **座位布局**：seatLayout 存 JSON 数组如 `["1-5","2-10"]`，通过 `ScheduleServiceImpl.generateSeats()` 解析

---

## 5. 排片管理

### 涉及文件

| 层次 | 文件 |
|------|------|
| Controller | `controller/ScheduleController.java` |
| Service | `service/impl/ScheduleServiceImpl.java` |
| Mapper | `mapper/ScheduleMapper.java`, `mapper/SeatMapper.java` |
| Entity | `entity/Schedule.java`, `entity/Seat.java` |

### 场次核心流程

### 添加场次

```
POST /api/schedules/add { movieId, hallId, startTime, price? }
  → 校验影片存在且状态≠下架
  → 校验影厅存在且状态≠维护中
  → 计算结束时间 = startTime + 影片时长 + 20分钟缓冲(BUFFER_MINUTES)
  → 时间冲突检查：同影厅内不能有时间重叠
    - 条件：新区间开始 < 旧区间结束 AND 新区间结束 > 旧区间开始
    - 排除已结束的场次、已取消的场次
  → 默认票价 = 影片基础票价(basePrice)
  → status=1(正常), soldCount=0
```

### 时间冲突检查详解（checkTimeConflict）

```java
// 遍历该影厅所有正常状态(status=1)且未结束的场次
// 冲突判断（Overlap检测）：
if (startTime.isBefore(existing.getEndTime()) && endTime.isAfter(existing.getStartTime())) {
    throw BusinessException("该时间段影厅已被占用");
}
```

### 座位自动生成

```
getSeats(scheduleId) → 查询座位 → 若为空则自动生成
  → 根据 Hall.rowCount × Hall.colCount 生成网格
  → 行号: A, B, C ... Z, AA, AB ...
  → 列号: 01, 02, 03 ...
  → 座位编号: "A-05"
  → 解析 hall.seatLayout JSON → 标记不可用座位 (status=3)
  → 可用座位 status=0(空闲)
  → 并发保护：二次检查防止重复生成
```

### 删除场次

```
DELETE /schedules/delete/{id}
  → 检查 soldCount > 0 则拒绝（已售出票不可删除）
  → 逻辑删除 (deleted=1)
```

### 座位矩阵返回

```json
{
  "schedule": { /* 场次信息 */ },
  "seats": [
    [{"seatNumber":"A-01","status":0}, {"seatNumber":"A-02","status":1}, ...],
    [{"seatNumber":"B-01","status":0}, {"seatNumber":"B-02","status":0}, ...]
  ],
  "rowCount": 8,
  "colCount": 12
}
```

**状态含义**：0=空闲, 1=已锁定(待支付), 2=已售出, 3=过道/不可用

### 批量填充优化（fillScheduleInfo）

批量查询所有场次的影片、影厅、锁定座位数，一次性 GROUP BY + batchIds 代替 N+1 查询，避免列表页性能问题。

---

## 6. 选座购票

### 涉及文件

| 层次 | 文件 |
|------|------|
| Controller | `controller/OrderController.java` |
| Service | `service/impl/OrderServiceImpl.java` → `createOrder()` |
| Mapper | `mapper/SeatMapper.java` → `lockSeat()`, `markSold()` |

### createOrder 完整流程

```
POST /api/user/orders/create { scheduleId, seatNumbers[], ticketTypes[], paymentMethod }
  ↓
【事务开始 @Transactional】
  ↓
1. 验证场次存在、status=1(正常)、startTime > NOW（未开始）
  ↓
2. 遍历座位：检查每个座位存在且 status=0（空闲）
  ↓
3. 生成订单号 = yyyyMMdd + 8位随机字母数字
  ↓
4. 插入订单(status=0 待支付)、创建操作日志
  ↓
5. 原子锁定所有座位（status 0→1）
   - lockSeat(id, orderId): UPDATE seat SET status=1, lock_time=NOW(), order_id=?
     WHERE id=? AND status=0
   - 若affected_rows=0 → 并发抢占 → 抛出异常回滚
  ↓
6. 定价引擎计算总价：
   - 基础票价 + 座位分区调价 + 时段折扣 + 人群折扣 + 会员折扣
  ↓
7. 原子增加场次已售数量 incrementSoldCount(scheduleId, count)
  ↓
8. 记录操作日志
【事务结束】
```

**关键设计**：
- 第5步 seat 锁定使用乐观锁 (`WHERE status=0`)，确保并发安全
- 第7步 `incrementSoldCount` 是 `UPDATE schedule SET sold_count=sold_count+?`，原子操作
- 整个方法使用 `@Transactional` 确保全部成功或全部回滚

---

## 7. 订单生命周期

### 涉及文件

| 层次 | 文件 |
|------|------|
| Service | `service/impl/OrderServiceImpl.java` |
| Mapper | `mapper/OrderMapper.java`, `mapper/SeatMapper.java` |

### 状态机

```
    [创建订单]
        ↓
   ┌  status=0 ──────────────→ status=5 (已过期/已取消)
   │  待支付                       ↑
   │   ↓ [支付]                   │ 超时自动取消/手动取消
   │  status=1                    │
   │  待观影                      │
   ├──→ status=3 (已改签) ────────┘
   │         ↓ 创建新订单
   └──→ status=4 (已退票)
```

### 支付 (payOrder)

```
POST /api/user/orders/pay/{orderId}
  → 校验：订单存在、属于当前用户、status=0
  → 校验：场次末开始
  → 将座位全部标记为已售出（status 1→2）
  → 余额支付：memberService.payWithBalance(userId, amount)
  → 订单 status 0→1
  → 记录 payTime
  → 累积积分 + 自动升级会员等级
  → 记录操作日志
```

### 改签 (reschedule)

```
POST /api/user/orders/reschedule { orderId, newScheduleId, newSeatNumbers[] }
  → 校验：原订单属于自己的、status=1(待观影)、场次未开始
  → 校验：新场次有效、未开始、与旧场次不同
  → 校验：新座位全部 status=0
  → 释放原座位 releaseSeatsByOrderId(oldOrderId)
  → 原子减少原场次 soldCount
  → 原订单 status 1→3, 记录改签时间
  → 计算差价：
    - 新价 > 旧价：需补差价，新订单 status=0(待支付)
    - 新价 ≤ 旧价：自动完成，新订单 status=1(待观影)
  → 锁定新座位 + 标记售出
  → 记录操作日志
```

### 退票 (refund)

```
POST /api/user/orders/refund/{orderId}
  → 校验：订单属于自己的、status=1、场次未开始
  → 计算退票手续费：
    - 开场前 ≥ 24小时：免手续费
    - 开场前 2-24小时：20% 手续费
    - 开场前 < 2小时：50% 手续费
  → 释放座位 releaseSeatsByOrderId(orderId)
  → 原子减少 soldCount
  → status 1→4
  → 记录操作日志（含手续费信息）
```

### 取消未支付订单

```
POST /api/user/orders/cancel/{orderId}
  → 校验：status=0、场次未开始
  → 乐观锁取消：cancelIfUnpaid(id) → UPDATE SET status=5 WHERE id=? AND status=0
  → 乐观锁释放座位：releaseSeatsByOrderIdOptimistic(orderId)
    → UPDATE seat SET status=0 WHERE order_id=? AND status=1
  → 原子减少 soldCount
```

### 代客下单/支付/退票（管理端）

`AdminOrderController` 提供三个接口，允许 STAFF 角色替用户操作：
- `POST /api/admin/orders/assist-create` — 替用户下单
- `POST /api/admin/orders/assist-pay/{orderId}` — 替用户支付
- `POST /api/admin/orders/assist-refund/{orderId}` — 替用户退票

与普通流程的区别：跳过用户所有权校验，操作日志记录 `operatorType=EMPLOYEE`

---

## 8. 支付系统

### 涉及文件

| 层次 | 文件 |
|------|------|
| Service | `service/impl/PaymentServiceImpl.java` |
| Entity | `entity/PaymentRecord.java` |

### 支付方式

支持 `WECHAT`、`ALIPAY`、`CASH`、`BALANCE`（储值余额）

### Mock 实现（当前版本）

```java
// PaymentServiceImpl.createPayment()
→ 生成 transactionId = "TXN" + UUID前20位
→ 创建 PaymentRecord(status=0 待支付)
→ simulatePaymentCallback() → 直接标记为 status=1(成功)
```

**生产部署时**：`simulatePaymentCallback()` 需替换为微信支付/支付宝 SDK 的真实回调处理，包括：
- 统一下单 API 调用
- 异步通知回调处理
- 幂等性保障（transactionId 去重）

### 退款

```java
refund(order, refundAmount) → 生成 RFN + UUID 的退款流水号
  → Mock 日志记录，真实环境对接支付网关退款接口
```

---

## 9. 定价引擎

### 涉及文件

| 层次 | 文件 |
|------|------|
| Service | `service/impl/PricingServiceImpl.java` |

### 三层定价模型

```
最终票价 = 场次基础票价
         + 座位分区调价 (seat.priceAdjustment)     【B9】
         × 时段折扣因子                             【B10】
         × 人群折扣因子                             【B11】
```

### B9 座位分区定价

```java
// seat.price_adjustment 字段：正数为加价，负数为优惠
// 如前排 -5元，黄金位置 +10元
BigDecimal price = basePrice.add(seat.getPriceAdjustment());
```

### B10 时段差异化定价

| 时段 | 系数 | 说明 |
|------|------|------|
| 10:00前 | 0.50 (5折) | 早场优惠 |
| 10:00-18:00 | 1.00 (原价) | 正常时段 |
| 18:00-21:00 | 1.20 (溢价20%) | 黄金时段 |
| 22:00后 | 0.80 (8折) | 深夜优惠 |

### B11 人群差异化定价

| 票种 | 折扣率 | 说明 |
|------|--------|------|
| null / 普通 | 1.00 | 原价 |
| STUDENT | 0.50 | 学生半价 |
| CHILD | 0.50 | 儿童半价 |
| SENIOR | 0.50 | 老人半价 |
| DISABLED | 0.50 | 残障人士半价 |
| MILITARY | 0.80 | 军人优惠 |

### 订单总价计算

```java
calculateOrderTotal(schedule, seatNumbers, ticketTypes)
  → 遍历每个座位：
    1. calculateSeatPrice(基础票价, seat, schedule)
       = (基础票价 + seat.priceAdjustment) × 时段系数
    2. applyTicketTypeDiscount(座位价, ticketType)
       = 座位价 × 人群折扣率
  → 总价 = Σ 各座位的折后价
```

---

## 10. 会员体系

### 涉及文件

| 层次 | 文件 |
|------|------|
| Service | `service/impl/MemberServiceImpl.java` |
| Entity | `entity/MemberLevel.java`, `entity/User.java` |

### 积分与等级

```
每消费 1元 = 1积分
积分达到对应等级阈值 → 自动升级会员等级
```

### 会员等级（member_level 表）

| 字段 | 说明 |
|------|------|
| level_name | 等级名称（如：普通会员、银卡会员、金卡会员、钻石会员） |
| min_spending | 升级所需最低消费积分 |
| discount_rate | 购票折扣率（如0.9=9折） |
| points_rate | 积分倍率 |

### 核心功能

| 功能 | 实现 |
|------|------|
| 消费累积积分 | `accumulatePoints(userId, amount)` — 支付完成后自动调用 |
| 自动升级 | `checkAndUpgrade(user)` — 积分变化后检查是否达到下一等级阈值 |
| 储值充值 | `recharge(userId, amount)` — 增加用户余额 |
| 余额支付 | `payWithBalance(userId, amount)` — 扣减余额 |
| 积分兑换 | `redeemPoints(userId, points)` — 100积分起兑，100分=¥5优惠券 |
| 会员折扣 | `getDiscountRate(userId)` — 购票时应用 |

### 关键设计

- **支付即积分**：`payOrder()` 和 `assistPay()` 都调用了 `accumulatePoints()`
- **积分累积失败不阻断支付**：try-catch 包裹，log.error 记录
- **积分为零时不下限**：`Math.max(0, currentPoints + delta)`
- **下一级进度**：`getUserMembershipInfo()` 中计算 progressPercent

---

## 11. 优惠券系统

### 涉及文件

| 层次 | 文件 |
|------|------|
| Service | `service/impl/CouponServiceImpl.java` |
| Mapper | `mapper/CouponMapper.java`, `mapper/UserCouponMapper.java` |
| Entity | `entity/Coupon.java`, `entity/UserCoupon.java` |

### 优惠券类型

| 类型 | 说明 | 折扣金额计算 |
|------|------|-------------|
| FIXED | 固定减免 | 直接扣减 coupon.value |
| PERCENT | 折扣券 | 订单金额 × coupon.value |

### 核心流程

```
领取优惠券：obtain(userId, couponId)
  → 检查库存 remainingQty > 0 → 原子扣减 decrementQty(couponId)
  → 检查是否已领取 → 创建 UserCoupon(expireTime = now + expireDays)

计算折扣：calculateDiscount(userCouponId, orderAmount)
  → 校验订单金额 ≥ minOrderAmount（满减门槛）
  → 按类型计算折扣金额

核销：useCoupon(userCouponId, orderId)
  → UPDATE user_coupon SET status=1, used_order_id=?, used_time=NOW()
    WHERE id=? AND status=0
```

### 安全点

- `decrementQty()` 使用 `UPDATE coupon SET remaining_qty = remaining_qty - 1 WHERE id=? AND remaining_qty > 0` 原子扣减
- `useCoupon()` 使用乐观锁 `WHERE status=0` 防止重复核销
- 单用户同券限制：`obtain()` 中检查 `existing.stream().anyMatch()`

---

## 12. 卖品管理

### 涉及文件

| 层次 | 文件 |
|------|------|
| Controller(管理端) | `controller/SnackController.java` |
| Controller(公开) | `controller/PublicSnackController.java` |
| Entity | `entity/Snack.java`, `entity/SnackCombo.java`, `entity/SnackOrder.java` |

### 数据模型

```
snack: 单个卖品（爆米花、可乐...）
  ├─ category: 分类（零食/饮料/热食）
  ├─ price, stock(库存), image_url, sort_order

snack_combo: 套餐（双人套餐、家庭套餐...）
  ├─ snack_ids: JSON数组 ["1","3","5"]
  ├─ combo_price: 套餐优惠价

snack_order: 卖品订单
  ├─ order_no: SNK + 日期 + 6位随机
  ├─ items: JSON [{snackId, name, qty, price, subtotal}]
  ├─ movie_order_id: 可选关联电影票订单
  └─ payment_method, total_amount, status
```

### 卖品下单流程

```
POST /api/admin/snacks/order { items: [{snackId,qty}], paymentMethod, movieOrderId? }
  → 遍历 items：
    - 查询 Snack → 计算小计
    - 扣库存 stock = max(0, stock - qty)
  → 生成订单号 SNKyyyyMMddXXXXXX
  → 创建 SnackOrder(status=1 直接完成)
  → 累积卖品消费积分
```

### 公开接口

`PublicSnackController` 提供无需管理员权限的套餐查询和下单（`/api/snacks`）
- `GET /api/snacks/combos` — 前端用户查看套餐
- `POST /api/snacks/combo-order` — 用户购买套餐

---

## 13. POS柜台售票

### 涉及文件

| 层次 | 文件 |
|------|------|
| Controller | `controller/PosController.java` |

### POS 三条核心接口

#### 1. 今日可用场次

```
GET /api/admin/pos/schedules
  → 查询今日 upcoming 场次
  → 批量查询空闲座位数（精确计数 + 兜底推算）
  → 返回：movieName, hallName, startTime, price, availableSeats
```

#### 2. 场次座位（柜台视图）

```
GET /api/admin/pos/seats/{scheduleId}
  → 委托 ScheduleService.getSeats() → 自动生成座位
  → 返回二维座位矩阵
```

#### 3. 柜台快速下单

```
POST /api/admin/pos/create-order { scheduleId, seatNumbers, paymentMethod, userId? }
  → 验证场次有效 + 座位全部空闲
  → 若指定 userId → 关联客户 + 应用会员折扣 + 累积积分
  → 若无 userId → 以操作员自身关联
  → POS 订单直接 status=1(已支付)，无需单独支付步骤
  → 标记座位已售出（乐观锁 markSoldByScheduleAndNumber）
  → 订单号格式: POS + yyyyMMdd + 5位毫秒 + 3位随机 = 极低碰撞概率
  → 插入重试机制（最多3次）处理订单号碰撞
```

**关键区别**：POS 订单直接完成支付（status=1），无需线上支付流程，支持现金/微信/支付宝

---

## 14. 取票与检票

### 涉及文件

| 层次 | 文件 |
|------|------|
| Controller | `controller/TicketController.java` |

### 取票码

```
格式: TTMS-{orderNo}-{userId后4位}
示例: TTMS-202603151234ABCD-0001
```

### 检票流程

```
POST /api/tickets/check-in { code: "TTMS-..." }
  → 解析取票码 → 提取 orderNo → 查询订单
  → 校验订单 status=1(已支付)
  → 检查 ConcurrentHashMap 是否已检票（防重复入场）
  → 记录检票时间和 orderId → 返回 "检票通过，欢迎入场"
```

**局限性**：检票记录存储在内存 `ConcurrentHashMap`，服务重启后丢失。生产环境应持久化到数据库。

---

## 15. 票房统计

### 涉及文件

| 层次 | 文件 |
|------|------|
| Controller | `controller/BoxOfficeController.java` |
| Service | `service/impl/BoxOfficeServiceImpl.java` |
| Mapper | `mapper/OrderMapper.java` (SQL聚合) |

### 统计指标

| 指标 | 计算方式 |
|------|----------|
| 综合票房 | Σ 订单总价 (按日期范围筛选) |
| 分账票房 | 综合票房 × 分账比例(share_ratio配置) |
| 出票数 | Σ seatCount |
| 场次数 | 按日期范围查 schedule 表 |
| 场均人次 | 出票数 ÷ 场次数 |
| 上座率 | 出票数 ÷ 总容量 × 100% |
| 排片占比 | 该影片场次数 ÷ 总场次数 × 100% |
| 票房占比 | 该影片票房 ÷ 总票房 × 100% |
| 上映天数 | 上映日期至统计截止日的天数 |
| 累计票房 | 该影片所有时间范围的票房总和 |

### 四个核心接口

| 接口 | 说明 |
|------|------|
| `GET /box-office/ranking?startDate=&endDate=&type=` | 票房排行（综合/分账） |
| `GET /box-office/dashboard?startDate=&endDate=&type=` | 大盘概览 |
| `GET /box-office/movie/{movieId}` | 单影片详情统计 |
| `GET /box-office/movie/{movieId}/trend?endDate=&days=` | 单影片每日趋势（最多365天） |

### 性能优化

- 趋势图使用单条 SQL 聚合 (`aggregateMovieTrend`) 替代逐日 N+1 查询
- 排行榜按影片分组在内存中聚合，而非逐影片查询数据库
- 无数据的日期自动填充零值，确保图表连续性

---

## 16. 数据统计与导出

### 涉及文件

| 层次 | 文件 |
|------|------|
| Controller | `controller/StatisticsController.java` |
| Service | `service/impl/StatisticsServiceImpl.java` |

### 四个统计维度

| 维度 | 接口 | 说明 |
|------|------|------|
| 营收总览 | `GET /statistics/revenue?startDate=&endDate=` | 总营收、订单数、售票数、平均票价 |
| 影片排行 | `GET /statistics/movie-ranking` | Top N 影片票房排行 |
| 每日营收 | `GET /statistics/revenue/daily` | 时间段内每日趋势图数据 |
| 月度数据 | `GET /statistics/monthly` | 最近12个月月度汇总 |
| Excel导出 | `GET /statistics/export` | Apache POI 生成3个Sheet的Excel |

### Excel 导出内容

| Sheet | 内容 |
|-------|------|
| 营收概览 | 日期范围、总营收、订单总数、售票总数、平均票价 |
| 影片票房排行 | 排名、影片名、票房、售票数 |
| 月度趋势 | 月份、营收、订单数、售票数 |

使用 Apache POI `XSSFWorkbook` 生成 .xlsx 文件，返回文件下载URL。

---

## 17. 仪表盘

### 涉及文件

| 层次 | 文件 |
|------|------|
| Controller | `controller/DashboardController.java` |
| Service | `service/impl/DashboardServiceImpl.java` |

### 数据指标

```
GET /api/admin/dashboard
  → todayRevenue:    今日营收
  → todayOrders:     今日订单数
  → todayTickets:    今日售票数
  → todaySchedules:  今日场次数
  → activeMovies:    上映中影片数
  → date:            日期
```

使用 SQL 聚合 `SUM`/`COUNT` 查询，单次返回所有指标，无 N+1 问题。

---

## 18. 团体订票

### 涉及文件

| 层次 | 文件 |
|------|------|
| Controller | `controller/GroupBookingController.java` |
| Entity | `entity/GroupBooking.java` |

### 流程

```
用户提交 → 填写 companyName, attendeeCount, preferredDate, movieName/Id, notes
  → status=0 (待审核)
  → 管理员审核 → 通过(status=1)/拒绝(status=2)
  → 填写 reviewNotes, reviewTime, reviewerId
```

审核通过后，后续是否需要自动创建对应的订单或锁定座位——**当前版本未自动处理**，需管理员手动操作。

---

## 19. 发票管理

### 涉及文件

| 层次 | 文件 |
|------|------|
| Controller | `controller/InvoiceController.java` |
| Entity | `entity/Invoice.java` |

### 流程

```
用户申请发票 → 关联 orderId, amount, title(抬头)
  → status=0 (待开具)
  → 管理员开具 → status=1
  → 填写 invoiceNo(发票号), issueTime(开具时间)
```

---

## 20. 交班管理

### 涉及文件

| 层次 | 文件 |
|------|------|
| Controller | `controller/ShiftController.java` |
| Service | `service/impl/ShiftServiceImpl.java` |
| Entity | `entity/Shift.java`, `entity/ShiftRecord.java` |

### 流程

```
员工上班签到 → POST /api/admin/shifts/start
  → 检查是否已有进行中的班次
  → 创建 Shift(status=0, startTime=NOW)

员工下班交班 → POST /api/admin/shifts/end
  → 提交 ShiftRecord:
    - cashCollected (现金收款)
    - wechatCollected (微信收款)
    - alipayCollected (支付宝收款)
    - totalCollected (总收款)
    - notes (备注)
  → Shift status 0→1, endTime=NOW
```

### 查询

- `GET /shifts/active` — 当前进行中的班次
- `GET /shifts/list` — 班次历史列表（含员工姓名）

---

## 21. 报表系统

### 涉及文件

| 层次 | 文件 |
|------|------|
| Controller | `controller/ReportController.java` |
| Service | `service/impl/ReportServiceImpl.java` |

### 自动生成

| 报表类型 | 触发时间 | 说明 |
|----------|----------|------|
| DAYLY | 每日凌晨2点 | 昨日日报 |
| WEEKLY | 每周一凌晨3点 | 上周周报（最近7天） |
| MONTHLY | 手动触发 | 月度报表 |

```java
generateReport(type, date)
  → 查询时间段内的 aggregateRevenue()
  → 序列化为 JSON 存入 report.content
  → 创建 Report 记录
```

### 管理端接口

- `GET /admin/reports?type=&startDate=&endDate=` — 查询报表列表
- `POST /admin/reports/generate` — 手动生成报表

---

## 22. 文件上传

### 涉及文件

| 层次 | 文件 |
|------|------|
| Controller | `controller/FileController.java` |

### 安全措施（三重校验）

```
POST /api/upload (multipart/form-data, field: "file")
  ↓
1. Content-Type 头检查 (快速初筛)
   → contentType 必须以 "image/" 开头
  ↓
2. 文件魔数(Magic Bytes)验证 (防止伪造 Content-Type)
   → JPEG: FF D8 FF
   → PNG:  89 50 4E 47
   → GIF:  47 49 46 38
   → BMP:  42 4D
   → WebP: 52 49 46 46 ... 57 45 42 50
  ↓
3. 文件大小限制 ≤ 10MB
  ↓
4. 根据魔数推断安全扩展名（不信任用户上传的文件名）
   → 防止上传 evil.jsp 伪装成图片
  ↓
存储路径: ./uploads/posters/yyyyMMdd/{UUID}.{ext}
返回URL:  /uploads/posters/yyyyMMdd/{filename}
```

---

## 23. 系统配置与主题

### 涉及文件

| 层次 | 文件 |
|------|------|
| Controller | `controller/SystemController.java` |
| Entity | `entity/SystemConfig.java` |

### 系统配置（key-value 存储）

| Key | 默认值 | 说明 |
|-----|--------|------|
| theme | white | 系统默认主题 |
| share_ratio | 52 | 影院分账比例(百分比) |
| cinema_name | TTMS电影院 | 影院名称 |
| contact_phone | 400-888-8888 | 联系电话 |
| order_timeout | 15 | 订单超时时间(分钟) |
| notice | 欢迎来到... | 公告信息 |

配置读取/更新通过 `SystemConfigMapper.selectByKey()` / `updateByKey()` 操作。

### 用户主题偏好

- `POST /api/user/theme` — 保存用户个人主题设置（存入 user.theme 字段）
- `GET /api/user/theme` — 获取当前用户主题
- 登录时返回的用户对象中包含 theme 字段

---

## 24. 操作日志与审计

### 涉及文件

| 层次 | 文件 |
|------|------|
| Entity | `entity/OrderLog.java` |
| Mapper | `mapper/OrderLogMapper.java` |
| Controller | `controller/SystemController.java`（日志查询） |

### 操作日志记录（OrderLog）

```
{ orderId, operationType, beforeContent, afterContent,
  operatorId, operatorType(USER/EMPLOYEE/SYSTEM), remark }
```

### 操作类型

| Type | 说明 | 触发方法 |
|------|------|----------|
| CREATE | 创建订单 | createOrder(), assistCreate() |
| PAY | 支付 | payOrder(), assistPay() |
| RESCHEDULE | 改签 | reschedule() |
| REFUND | 退票 | refund(), assistRefund() |
| CANCEL | 取消 | cancelOrder() |
| EXPIRE | 过期自动取消 | cancelExpired() 定时任务 |

### 日志查询

`GET /api/admin/system/logs?page=1&size=20`
- 批量查询 User 和 Employee 表解析操作人名称
- 操作类型映射为中文标签
- 返回前端可读格式（含操作人、操作描述、模块、结果）

### 日志清理

定时任务每天凌晨3点删除90天前的操作日志。

---

## 25. 定时任务

### 涉及文件

| 层次 | 文件 |
|------|------|
| 配置 | `config/ScheduledTasks.java` |
| 订单服务 | `service/impl/OrderServiceImpl.java` → `cancelExpired()` |
| 报表服务 | `service/impl/ReportServiceImpl.java` → 自动报表 |

### 所有定时任务一览

| 任务 | 频率 | 功能 |
|------|------|------|
| autoEndSchedules | 每60秒 | 将 end_time < NOW() 的场次 status 1→2（已结束） |
| cancelExpired (订单) | 每120秒 | 取消创建超过 order_timeout 分钟的未支付订单 |
| releaseStaleLockedSeats | 每300秒 | 释放 lock_time 超过30分钟的座位（兜底机制） |
| remindPendingPayment | 每120秒 | 发送支付超时提醒（创建超过12分钟） |
| cleanOldOrderLogs | 每天凌晨3点 | 删除90天前的操作日志 |
| autoUpdateMovieStatus | 每小时 | 将 release_date ≤ 今天 的影片 2→1（自动上架） |
| generateDailyReport | 每天凌晨2点 | 生成昨日日报 |
| generateWeeklyReport | 每周一凌晨3点 | 生成上周周报 |
| TokenBlacklist.cleanExpired | 每60秒 | 清理过期的黑名单 token |
| LoginRateLimiter.cleanExpired | 每600秒 | 清理过期的限流记录 |

### 过期订单取消的乐观锁保护

```java
// 关键：防止"用户在临界点刚支付完成"的竞态
int updated = orderMapper.cancelIfUnpaid(order.getId());
// UPDATE order SET status=5 WHERE id=? AND status=0
if (updated == 0) return; // 已被支付，跳过

seatMapper.releaseSeatsByOrderIdOptimistic(order.getId());
// UPDATE seat SET status=0 WHERE order_id=? AND status=1
```

---

## 26. 安全体系

### 涉及文件

| 文件 | 职责 |
|------|------|
| `config/SecurityConfig.java` | Spring Security 配置 |
| `config/CorsConfig.java` | CORS 跨域配置 |
| `security/JwtAuthenticationFilter.java` | JWT 过滤器 |
| `security/JwtTokenProvider.java` | JWT 生成/验证 |
| `security/TokenBlacklist.java` | 令牌黑名单 |
| `security/LoginRateLimiter.java` | 登录限流 |
| `config/TraceIdFilter.java` | 请求追踪ID |
| `exception/GlobalExceptionHandler.java` | 全局异常处理 |
| `config/WebMvcConfig.java` | 静态资源与SPA路由 |

### 安全层次

```
网络层：CORS 跨域控制 + HTTPS (生产环境)
  ↓
传输层：JWT(HS256) 无状态令牌认证
  ↓
会话层：TokenBlacklist 主动失效 + 登录限流防爆破
  ↓
授权层：Spring Security URL规则 + @PreAuthorize 方法级
  ↓
数据层：BCrypt密码(strength=10) + SQL参数化防注入(MyBatis-Plus)
  ↓
审计层：OrderLog 完整操作记录 + TraceId 链路追踪
```

### 密码安全

- BCrypt 加密算法，strength=10，加盐自动完成
- 修改密码时验证原密码
- Token 即时黑名单化（强制重新登录）

### 文件上传安全

- Content-Type 初步检查
- 文件魔数验证（防伪造）
- 根据魔数生成安全扩展名（防 .jsp 等）
- 10MB 大小限制

### 日志脱敏

```java
// AuthController
private String maskUsername(String username) {
    if (username == null || username.length() <= 1) return "***";
    return username.charAt(0) + "***";
}
```

---

## 27. 数据初始化

### 涉及文件

| 文件 | 职责 |
|------|------|
| `config/DatabaseInitializer.java` | `CommandLineRunner` 启动时自动初始化 |
| `resources/schema.sql` | 完整 DDL（25张表 + 种子数据） |
| `resources/migration.sql` | 增量迁移脚本 |

### 初始化内容

| 数据 | 说明 |
|------|------|
| 3种角色 | ROLE_SUPER_ADMIN, ROLE_STAFF, ROLE_USER（含权限JSON） |
| 默认管理员 | admin / admin123（EMP001工号） |
| 5项系统配置 | theme, share_ratio, cinema_name, contact_phone, order_timeout, notice |
| 24部电影 | 涵盖科幻/动画/动作/喜剧/剧情等类型，含评分/海报等元数据 |

### 幂等性保证

- 角色：仅当角色表为空时插入
- 管理员：仅当 admin 不存在时创建
- 配置：仅当配置表为空时插入
- 电影：仅当少于5部时补充，按 movieName 去重防止重复

---

## 附录A：架构决策记录

| 决策 | 选择 | 理由 |
|------|------|------|
| ORM框架 | MyBatis-Plus 3.5.6 | 相比JPA更灵活，SQL书写自由，分页/逻辑删除等内置功能丰富 |
| 认证方式 | JWT (HMAC-SHA256) | 无状态、适合前后端分离，无需Redis存储session |
| 密码加密 | BCrypt(strength=10) | 业界标准，自动加盐，抗暴力破解 |
| 支付实现 | Mock模式 | 开发阶段快速验证，预留接口可平滑切换微信/支付宝SDK |
| 数据库 | MySQL + Druid连接池 | Druid提供完善的SQL监控和连接池管理 |
| API文档 | SpringDoc OpenAPI | 自动生成Swagger文档，与Spring Boot 3.x兼容 |
| 前后端部署 | 后端内嵌前端静态资源 | WebMvcConfig SPA fallback 使Vue/React项目的index.html能正确处理路由 |

## 附录B：关键数值常量

| 常量 | 值 | 所在位置 |
|------|-----|----------|
| BCrypt strength | 10 | SecurityConfig |
| JWT 默认过期 | 24小时 (86400000ms) | application.yml |
| 场次缓冲时间 | 20分钟 | ScheduleServiceImpl.BUFFER_MINUTES |
| 订单超时 | 15分钟（可配置） | system_config 表 order_timeout |
| 座位锁定超时 | 30分钟 | ScheduledTasks.releaseStaleLockedSeats |
| 退票手续费 | 0% / 20% / 50% | OrderServiceImpl.calculateRefundFee |
| 登录限流 | 5次失败 / 15分钟 | LoginRateLimiter |
| 操作日志保留 | 90天 | ScheduledTasks.cleanOldOrderLogs |
| 文件上传限制 | 10MB | FileController |
| 分页最大行数 | 500 | MyBatisPlusConfig |
| 积分兑换比例 | 100分 = ¥5 | MemberServiceImpl.redeemPoints |

## 附录C：项目完整包结构

```
com.ttms
├── TTMSApplication.java          (启动类)
├── config/                        (8个配置类)
│   ├── CorsConfig.java
│   ├── DatabaseInitializer.java
│   ├── MyBatisPlusConfig.java
│   ├── OpenApiConfig.java
│   ├── ScheduledTasks.java
│   ├── SecurityConfig.java
│   ├── TraceIdFilter.java
│   └── WebMvcConfig.java
├── controller/                    (22个控制器)
│   ├── AdminOrderController.java
│   ├── AuthController.java
│   ├── BoxOfficeController.java
│   ├── CouponController.java
│   ├── DashboardController.java
│   ├── EmployeeController.java
│   ├── FileController.java
│   ├── GroupBookingController.java
│   ├── HallController.java
│   ├── InvoiceController.java
│   ├── MemberController.java
│   ├── MovieController.java
│   ├── OrderController.java
│   ├── PosController.java
│   ├── PublicSnackController.java
│   ├── ReportController.java
│   ├── ScheduleController.java
│   ├── ShiftController.java
│   ├── SnackController.java
│   ├── StatisticsController.java
│   ├── SystemController.java
│   └── TicketController.java
├── dto/                          (6个DTO)
│   ├── ApiResponse.java
│   ├── LoginRequest.java
│   ├── LoginResponse.java
│   ├── OrderRequest.java
│   ├── RegisterRequest.java
│   └── RescheduleRequest.java
├── entity/                       (25个实体)
│   ├── BackupLog.java
│   ├── Cinema.java
│   ├── Coupon.java
│   ├── Employee.java
│   ├── GroupBooking.java
│   ├── Hall.java
│   ├── Invoice.java
│   ├── MemberLevel.java
│   ├── Movie.java
│   ├── Notification.java
│   ├── Order.java
│   ├── OrderLog.java
│   ├── PaymentRecord.java
│   ├── Report.java
│   ├── Role.java
│   ├── Schedule.java
│   ├── Seat.java
│   ├── Shift.java
│   ├── ShiftRecord.java
│   ├── Snack.java
│   ├── SnackCombo.java
│   ├── SnackOrder.java
│   ├── SystemConfig.java
│   ├── User.java
│   └── UserCoupon.java
├── exception/                    (2个异常)
│   ├── BusinessException.java
│   └── GlobalExceptionHandler.java
├── mapper/                       (25个Mapper)
│   └── ... (对应每个Entity)
├── security/                     (4个安全组件)
│   ├── JwtAuthenticationFilter.java
│   ├── JwtTokenProvider.java
│   ├── LoginRateLimiter.java
│   └── TokenBlacklist.java
└── service/                      (14个服务)
    ├── AuthService.java
    ├── BoxOfficeService.java
    ├── HallService.java
    ├── MovieService.java
    ├── OrderService.java
    ├── ScheduleService.java
    ├── StatisticsService.java
    └── impl/
        ├── AuthServiceImpl.java
        ├── BoxOfficeServiceImpl.java
        ├── CouponServiceImpl.java
        ├── DashboardServiceImpl.java
        ├── HallServiceImpl.java
        ├── MemberServiceImpl.java
        ├── MovieServiceImpl.java
        ├── OrderServiceImpl.java
        ├── PaymentServiceImpl.java
        ├── PricingServiceImpl.java
        ├── ReportServiceImpl.java
        ├── ScheduleServiceImpl.java
        ├── ShiftServiceImpl.java
        └── StatisticsServiceImpl.java
```
