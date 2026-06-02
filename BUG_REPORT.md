# TTMS 电影院管理系统 - 深度审查报告

> 审查日期: 2026-06-02 | 审查范围: 全栈代码 (后端 Java + 前端 Vue) | 总计发现: 65+ 个问题

---

## CRITICAL (致命) - 必须立即修复

### 🔴 C1. 员工管理明文密码泄露到日志和API响应
**文件**: `backend/src/main/java/com/ttms/controller/EmployeeController.java:115-116, 183-184`
**问题**: 添加员工和重置密码时，原始明文密码被记录到INFO级别日志并直接返回到API响应消息中。
```java
log.info("员工添加成功: ..., 初始密码={}", rawPassword);  // 密码明文入日志!
return ApiResponse.success("员工添加成功，默认密码: " + rawPassword, employee);  // 密码明文返回到前端!
```
**修复**: 从日志和API响应中移除明文密码；通过安全渠道（如邮件/短信）发送初始密码。

---

### 🔴 C2. CorsConfig凭证泄漏风险
**文件**: `backend/src/main/java/com/ttms/config/CorsConfig.java:24-31`
**问题**: `allowCredentials(true)` + `addAllowedOriginPattern("*")` 组合意味着**任何网站**都可以发送带凭证的跨域请求。在生产环境中，攻击者可以利用用户的登录状态发起CSRF攻击。
```java
config.addAllowedOriginPattern("*");  // 允许任意来源
config.setAllowCredentials(true);     // 同时允许携带凭证 → 安全漏洞!
```
**修复**: 生产环境使用白名单限定具体的允许域名。

---

### 🔴 C3. SecurityConfig缺少CORS集成，预检请求被拦截
**文件**: `backend/src/main/java/com/ttms/config/SecurityConfig.java:35`
**问题**: Spring Security过滤器链中没有 `.cors(withDefaults())` 配置。虽然定义了 `CorsFilter` Bean，但浏览器发送的OPTIONS预检请求会先被Spring Security拦截（返回401/403），CorsFilter根本没有机会处理。
**修复**: 在 `http` 配置链中添加 `.cors(withDefaults())`。

---

### 🔴 C4. 卖座计数存在竞态条件 (Race Condition)
**文件**: `backend/src/main/java/com/ttms/service/impl/OrderServiceImpl.java:123-126, 291-294, 335-337, 398-401, 514-519`
**问题**: `soldCount` 的更新是读-修改-写模式，两个并发请求可能都读到相同的旧值，导致最终计数不正确。有5处代码存在此问题。
```java
// 非原子操作：读到旧值 + N → 写回
schedule.setSoldCount(oldValue + newSeats);
scheduleMapper.updateById(schedule);
```
**修复**: 使用原子SQL更新: `UPDATE schedule SET sold_count = sold_count + ? WHERE id = ?`

---

### 🔴 C5. GlobalExceptionHandler返回HTTP 200掩盖错误状态码
**文件**: `backend/src/main/java/com/ttms/exception/GlobalExceptionHandler.java:22-26`
**问题**: `handleBusinessException` 没有设置 `@ResponseStatus` 或返回 `ResponseEntity`。即使业务异常码是401/403/500，HTTP层面永远返回200 OK。REST客户端无法通过HTTP状态码感知错误。
**修复**: 返回 `ResponseEntity.status(e.getCode()).body(...)` 或添加 `@ResponseStatus`。

---

### 🔴 C6. payOrder()缺少权限校验 —— 任意用户可支付他人订单
**文件**: `backend/src/main/java/com/ttms/service/impl/OrderServiceImpl.java:168-169`
**问题**: 代码注释写明"此处不限制，因为管理员也有可能需要支付"，但实际效果是**任何登录用户**都可以支付任意订单ID。恶意用户可抢占他人选定的座位。
```java
// 没有任何 ownerId 校验！
public Order payOrder(Long orderId, Long userId) {
    Order order = orderMapper.selectById(orderId); // 直接查，不验证归属
```
**修复**: 添加归属校验 `if (!order.getUserId().equals(userId)) throw ...`；管理员协助支付应使用独立的管理端接口。

---

### 🔴 C7. DatabaseInitializer在每次启动时可能删除所有电影数据
**文件**: `backend/src/main/java/com/ttms/config/DatabaseInitializer.java:197`
**问题**: `initMovies()` 中，当影片数量不足5部时，调用 `movieMapper.delete(null)` **无条件删除全部影片**，然后重新插入25部初始影片。用户手动添加的1-4部影片数据会永久丢失。
```java
movieMapper.delete(null); // 删除所有电影！然后重新插入...
```
**修复**: 使用 `INSERT IGNORE` 按具体电影名检查存在性，不要删除已有数据。

---

### 🔴 C8. JWT密钥弱填充导致暴力破解风险
**文件**: `backend/src/main/java/com/ttms/security/JwtTokenProvider.java:40-48`
**问题**: 当 `jwt.secret` 不足32字节时，代码用零字节填充。如果配置的密钥较短（如"mykey"只有5字节），有效熵从256位降至仅40位，JWT令牌可被轻易暴力破解。
```java
byte[] paddedKey = new byte[32];  // 填充零字节 → 弱密钥!
System.arraycopy(keyBytes, 0, paddedKey, 0, keyBytes.length);
```
**修复**: 使用HKDF等密钥派生函数，或直接拒绝过短的密钥配置并抛出启动错误。

---

### 🔴 C9. 营收统计按创建时间而非支付时间过滤
**文件**: `backend/src/main/java/com/ttms/service/impl/StatisticsServiceImpl.java:56-59, 174`
**问题**: `getRevenue()` 按 `createTime`（订单创建时间）过滤营收，而不是 `payTime`（支付时间）。6月1日创建但6月15日支付的订单会计入6月1日的营收。
**修复**: 使用 `payTime` 作为营收统计的时间维度。

---

### 🔴 C10. N+1查询导致性能问题
**文件**:
- `EmployeeController.java:52-60` - 为每个员工单独查询角色
- `ScheduleServiceImpl.java:407-431` - 为每个场次单独查询影片和影厅
**问题**: 查询N条记录时会产生N+1次数据库查询。100个员工=101次查询，100个场次=201次查询。
**修复**: 批量查询相关ID，然后在内存中映射。

---

### 🔴 C11. EmployeeManage删除操作为假删除且状态类型错误
**文件**: `frontend/src/views/admin/EmployeeManage.vue:196-209`
**问题**: "删除"按钮调用 `toggleEmployeeStatus`（切换启用/禁用），而非真正的删除API。且删除设置 `row.status = 'DELETED'`（字符串），但模板中使用 `=== 1`/`=== 0` 严格数字比较，导致"已删除"员工仍显示为"正常"。整个删除操作既不是真删除也不是逻辑删除，是Bug。
**修复**: 调用实际的删除API，或从UI中移除不存在的删除功能。

---

### 🔴 C12. SystemSettings日志分页完全失效
**文件**: `frontend/src/views/admin/SystemSettings.vue:200-212`
**问题**: `logs` 是 `ref` 而非 `computed`。当用户点击分页按钮时，`logPage` 改变，但没有watch/computed来响应变化重新切片数据。用户看到的数据永远停留在第一页。分页控件是装饰性UI。
**修复**: 将 `logs` 改为 `computed`，基于 `logPage` 动态计算切片。

---

### 🔴 C13. 统计导出始终失败
**文件**: `frontend/src/api/statistics.js:31-34` + `frontend/src/api/index.js:27`
**问题**: `exportStatistics` 使用 `responseType: 'blob'`，但响应拦截器检查 `res.code !== 200`。对于Blob响应，`res.code` 是 `undefined`，导致 `undefined !== 200` 为 `true`，拦截器显示错误并reject Promise。即使用户触发了导出下载，也会弹出一个虚假的错误提示。
**修复**: 拦截器中检查 `responseType`，若为 `blob` 则跳过JSON响应码校验。

---

### 🔴 C14. 路由守卫SUPER_ADMIN权限绕过
**文件**: `frontend/src/router/index.js:135-146`
**问题**: 路由 `/admin/movies`、`/admin/halls`、`/admin/schedules`、`/admin/statistics` 同时设置了 `requiresAdmin: true` 和 `role: 'ROLE_SUPER_ADMIN'`。守卫先检查 `isAdmin`（STAFF也通过），SUPER_ADMIN检查需要 `!to.meta.requiresAdmin` 才触发（为false），导致这些路由**跳过SUPER_ADMIN检查**。STAFF角色实际可以访问这些应该只有超级管理员能访问的页面。
**修复**: 统一权限控制逻辑，修复守卫判断条件。

---

### 🔴 C15. 选座页面重复订单风险
**文件**: `frontend/src/views/user/SeatSelection.vue:304-320`
**问题**: `handleConfirm` 中如果 `createOrder` 成功但响应缺少 `id`/`orderId`，代码显示错误后返回。`submitting` 在finally中恢复，但用户可能已经点过按钮，再次点击会创建重复订单。
**修复**: 创建订单前禁用按钮并显示loading状态，失败时给出明确提示。

---

### 🔴 C11 (原). 订单列表使用内存分页
**文件**: `OrderServiceImpl.java:434-451`
**问题**: `listByUser()` 先从数据库加载用户**所有**订单到内存，再手动截取分页。用户有10000个订单时直接OOM。
**修复**: 使用MyBatis-Plus的 `Page<>` 对象进行数据库级分页。

---

## HIGH (高危) - 应尽快修复

### 🟠 H1. MovieController/HallController空指针异常
**文件**: `MovieController.java:136-141`
**问题**: `params.get("id").toString()` 当参数不存在时抛出NPE，返回500错误。
**修复**: 添加null检查。

---

### 🟠 H2. FileController文件类型验证可被绕过
**文件**: `FileController.java:46-49`
**问题**: 仅检查HTTP `Content-Type` 头判断文件类型，攻击者可以伪造上传恶意文件。
**修复**: 使用文件魔数（magic bytes）检测真实类型。

---

### 🟠 H3. 退票操作误用rescheduleTime字段
**文件**: `OrderServiceImpl.java:408`
**问题**: `refund()` 方法设置了 `order.setRescheduleTime(LocalDateTime.now())`——字段名是"改签时间"却被用于退票操作，语义完全错误。
**修复**: 添加专用的 `refundTime` 字段。

---

### 🟠 H4. 改签不存在价格差额处理
**文件**: `OrderServiceImpl.java:314-332`
**问题**: 改签时新订单直接标记为已支付(status=1)，新旧票价差额从未计算/退还/补收。便宜座位多付了不退，贵座位少付了不补。
**修复**: 计算差额并记录；若需要补差价，要求用户先支付。

---

### 🟠 H5. 取消过期订单的定时任务单事务问题
**文件**: `OrderServiceImpl.java:500-543`
**问题**: `cancelExpired()` 整个方法在一个 `@Transactional` 中运行。如果100个过期订单中第50个处理失败，异常被catch吞掉，但前面的49个仍然提交了——这是个不完整的事务。应该每个订单独立事务。
**修复**: 将 `@Transactional` 移到每个订单处理的子方法上，或使用 `@Transactional(propagation = REQUIRES_NEW)`。

---

### 🟠 H6. GlobalExceptionHandler捕获了错误的异常类型
**文件**: `GlobalExceptionHandler.java:50-53`
**问题**: 捕获 `java.sql.SQLIntegrityConstraintViolationException`，但Spring JDBC会将其包装为 `org.springframework.dao.DataIntegrityViolationException`。这个处理器**永远不会被触发**。数据库唯一约束冲突会漏到通用Exception处理器，返回500。
**修复**: 改为捕获 `DataIntegrityViolationException`。

---

### 🟠 H7. 员工工号生成的竞态条件
**文件**: `EmployeeController.java:224-238`
**问题**: `generateEmployeeNo()` 先查询最大工号再+1生成新工号，并发请求可能产生重复工号。数据库虽然有唯一约束，但错误返回500而非友好提示。
**修复**: 捕获唯一约束异常后重试，或使用数据库序列/自增ID。

---

### 🟠 H8. JWT过期时间和密钥无默认值
**文件**: `JwtTokenProvider.java:29-33`
**问题**: `jwt.secret` 和 `jwt.expiration` 没有默认值。如果配置文件缺少这些属性，`jwtSecret` 为null导致NPE，`jwtExpiration` 为0导致所有令牌立即过期。
**修复**: 使用 `@Value("${jwt.secret:}")` 带默认值语法，并在 `@PostConstruct` 中验证。

---

### 🟠 H9. ScheduleServiceImpl 并发生成座位导致重复行
**文件**: `ScheduleServiceImpl.java:247-248`
**问题**: `getSeats()` 检查座位为空后调用 `generateSeats()`，但两个并发请求可能同时发现为空，都执行INSERT，产生重复座位数据。
**修复**: 在 `generateSeats` 中先检查是否已有数据，或使用 `INSERT IGNORE`。

---

### 🟠 H10. AuthServiceImpl多处BusinessException缺少HTTP状态码
**文件**: `AuthServiceImpl.java:69, 135, 140, 168`
**问题**: 使用无参 `BusinessException(String)` 构造函数，默认返回HTTP 500。但"用户不存在"(line 168)和"角色未配置"(line 69)应该返回400或404。
**修复**: 传入正确的HTTP状态码，如 `new BusinessException(404, "用户不存在")`。

---

### 🟠 H11. User和Employee密码字段未忽略序列化
**文件**: `User.java:20`, `Employee.java:22`
**问题**: 无 `@JsonIgnore` 或 `@JsonProperty(access = WRITE_ONLY)`。任何API返回User/Employee对象时BCrypt密文将被泄露。
**修复**: 添加 `@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)`。

---

### 🟠 H12. 营收排名无日期限制
**文件**: `StatisticsServiceImpl.java:108`
**问题**: `getMovieRanking()` 查询全部历史订单，无日期过滤。随着时间的推移性能会越来越差。
**修复**: 添加日期范围参数。

---

### 🟠 H13. Seat status=3未在实体文档中记录
**文件**: `Seat.java:28`
**问题**: 实体注释只有0-空闲/1-锁定/2-售出三种状态，但 `ScheduleServiceImpl.generateSeats()` 实际使用了status=3表示过道/不可用。前端映射未知状态时默认fallback为"AVAILABLE"，可能导致选中不可用座位。
**修复**: 完善文档，前端默认值改为 "AISLE"。

---

### 🟠 H14. Role和SystemConfig表缺少deleted字段
**文件**: `Role.java`, `SystemConfig.java`
**问题**: MyBatis-Plus全局配置 `logic-delete-field: deleted`，但这两个表没有deleted列。执行逻辑删除时SQL报错。
**修复**: 添加deleted列和对应的 `@TableLogic` 字段。

---

### 🟠 H15. MovieDetail存在CSS注入风险
**文件**: `frontend/src/views/user/MovieDetail.vue:9`
**问题**: `:style="{ backgroundImage: \`url(${movie.poster || ''})\` }"`直接将用户可控制的URL（管理员上传的电影海报）嵌入内联样式，存在CSS注入攻击面。
**修复**: 对poster URL进行转义处理。

---

### 🟠 H16. Dashboard使用Promise.all导致单点故障
**文件**: `frontend/src/views/admin/Dashboard.vue:160-183`
**问题**: 使用 `Promise.all` 并行请求订单和电影数据，任一个请求失败都会导致两个数据集全部丢失。
**修复**: 使用 `Promise.allSettled` 分别处理成功和失败情况。

---

### 🟠 H17. AuthStore JSON.parse无异常保护导致应用崩溃
**文件**: `frontend/src/stores/auth.js:8`
**问题**: `JSON.parse(localStorage.getItem('user') || 'null')` 没有 try-catch。如果localStorage数据损坏，整个应用启动即崩溃。
**修复**: 添加 try-catch，解析失败时清除脏数据并回退到未登录状态。

---

### 🟠 H18. 401处理使用硬页面重载
**文件**: `frontend/src/api/index.js:40`
**问题**: 401响应时使用 `window.location.href = '/login'` 硬重载，销毁所有应用状态。应使用Vue Router进行无刷新跳转。
**修复**: 导入router并使用 `router.push('/login')`。

---

### 🟠 H15 (原). Order状态2（已完成）从未被设置
**文件**: `OrderServiceImpl.java`
**问题**: 订单状态定义包含 `2-已完成`，但整个代码中没有将订单更新为状态2的逻辑。电影结束后订单永远停留在"待观影"状态。
**修复**: 添加定时任务将过期场次的订单标记为已完成。

---

## MEDIUM (中危) - 建议优先修复

### 🟡 M1. 数据库种子数据只插入1个影厅
**文件**: `backend/src/main/resources/schema.sql:182-192`
**问题**: 三条INSERT都使用相同的 `WHERE NOT EXISTS (SELECT X FROM hall LIMIT 1)`，第一条执行后hall表已非空，后两条被跳过。
**修复**: 每条INSERT检查具体的 `hall_name` 是否存在。

---

### 🟡 M2. 路由守卫中权限逻辑不一致
**文件**: `frontend/src/router/index.js:60-77, 142`
**问题**: MovieManage/HallManage/ScheduleManage同时设置了 `requiresAdmin: true` 和 `role: 'ROLE_SUPER_ADMIN'`。但由于守卫逻辑的条件判断，`role: 'ROLE_SUPER_ADMIN'` 在这些路由上被忽略（因为 `!to.meta.requiresAdmin` 条件不满足），STAFF角色实际也能访问这些页面。
**修复**: 统一权限控制逻辑，仅使用一种检查方式。

---

### 🟡 M3. 前端API未支持分页
**文件**: `frontend/src/api/order.js:23-27`
**问题**: `getMyOrders()` 硬编码为 page=1, size=10，用户超过10个订单时无法查看全部。
**修复**: 添加page和size参数。

---

### 🟡 M4. 401重定向无循环防护
**文件**: `frontend/src/api/index.js:37-38`
**问题**: 401响应时执行 `window.location.href = '/login'`。如果登录页面本身触发401，会无限循环重定向。
**修复**: 检查当前路径，已在 `/login` 时不重定向。

---

### 🟡 M5. 座位状态映射未知值默认为可用
**文件**: `frontend/src/views/user/SeatSelection.vue:280`
**问题**: `statusMap[seat.status] || 'AVAILABLE'` 将任何未知状态fallback为"可用"。如果后端新增状态值，前端会错误地将不可用座位显示为可选。
**修复**: 默认值改为 `'AISLE'`。

---

### 🟡 M6. 前端isAdmin检查包含不存在的角色
**文件**: `frontend/src/stores/auth.js:13-15`
**问题**: 检查了 `ROLE_ADMIN`，但系统中只存在 `ROLE_SUPER_ADMIN` 和 `ROLE_STAFF`。死代码。
**修复**: 移除 `ROLE_ADMIN` 检查。

---

### 🟡 M7. ScheduleManage.vue动态导入不必要
**文件**: `frontend/src/views/admin/ScheduleManage.vue:98, 220`
**问题**: 顶层已导入 `getSchedulesByMovie`（且未使用），`fetchSchedules` 中又用 `await import()` 动态导入 `getScheduleList`。
**修复**: 使用顶层静态导入。

---

### 🟡 M8. createOrder重复查询座位
**文件**: `OrderServiceImpl.java:79-87, 109-111`
**问题**: 创建订单时对同一座位查询两次（验证阶段一次、锁定阶段一次），浪费数据库查询。
**修复**: 在验证阶段缓存查询结果。

---

### 🟡 M9. listByUser未填充关联信息
**文件**: `OrderServiceImpl.java:434-451`
**问题**: `listByUser()` 返回的订单没有调用 `fillOrderInfo()`（与 `listAll()` 不一致），前端需要获取 `movieName`/`hallName` 等信息。
**修复**: 调用 `fillOrderInfo()` 或在SQL JOIN中获取关联数据。

---

### 🟡 M10. 订单日志重复创建
**文件**: `OrderServiceImpl.java:556-568`
**问题**: `assistCreate()` 调用 `createOrder()`（内部已创建CREATE日志），然后又插入第二条CREATE日志。同一订单有两条日志。
**修复**: 在 `assistCreate` 中更新第一条日志的operator信息，而非创建第二条。

---

### 🟡 M11. 注册接口缺少验证码/频率限制
**文件**: `AuthController.java`
**问题**: 注册接口没有验证码或IP频率限制，可被自动化脚本批量注册。
**修复**: 添加图形验证码或邮箱验证。

---

### 🟡 M12. 登录接口无暴力破解防护
**文件**: `AuthServiceImpl.java:47`
**问题**: 登录无失败次数限制，可被暴力破解。
**修复**: 5次失败后临时锁定账号15分钟。

---

### 🟡 M13. 账号禁用状态泄露用户存在性
**文件**: `AuthServiceImpl.java:59-60, 95-96`
**问题**: 登录时对于被禁用的账号返回不同错误消息（"账号已被禁用" vs "用户名或密码错误"），可用于枚举有效用户名。
**修复**: 统一返回"用户名或密码错误"。

---

### 🟡 M14. JwtAuthenticationFilter未跳过公开路径
**文件**: `JwtAuthenticationFilter.java:41-45`
**问题**: `SKIP_PATHS` 不包含 `/api/movies/**`，每个公开的电影查询请求都会经历无效的JWT解析。
**修复**: 添加 `/api/movies/**` 到SKIP_PATHS，或将WARN日志降级为DEBUG。

---

### 🟡 M15. loginType无验证导致静默回退
**文件**: `LoginRequest.java:15`, `AuthServiceImpl.java:53`
**问题**: `loginType` 没有 `@NotBlank`，为null时静默回退到用户端登录（else分支），可能导致混淆。
**修复**: 添加 `@NotBlank`，拒绝未知的loginType值。

---

### 🟡 M16. 注册缺少手机号/邮箱格式验证
**文件**: `RegisterRequest.java:17-18`
**问题**: phone和email没有任何 `@Pattern`/`@Email` 验证注解。
**修复**: 添加验证注解。

---

### 🟡 M17. EmployeeController架构违规
**文件**: `EmployeeController.java:30-32`
**问题**: Controller直接注入Mapper和PasswordEncoder，业务逻辑（角色解析、密码编码、工号生成、用户名唯一性检查）散落在Controller中。违反了分层架构。
**修复**: 创建 `EmployeeService` 并将业务逻辑移至其中。

---

### 🟡 M18. 文件上传目录失败处理
**文件**: `FileController.java:70`
**问题**: `dir.mkdirs()` 返回值被忽略。如果目录创建失败，代码继续执行并在 `Files.write()` 时失败，错误信息不明确。
**修复**: 检查返回值，失败时快速失败并给出明确错误。

---

### 🟡 M19. Order表持有冗余字段
**文件**: `Order.java:27, 30`
**问题**: `movieId` 和 `hallId` 可从 `scheduleId` 推导，存在数据不一致风险。且 `fillOrderInfo()` 也通过Schedule来获取这些信息，导致冗余字段实际上未被信任使用。
**修复**: 移除冗余字段，或在INSERT时从Schedule获取。

---

### 🟡 M20. 座位JSON解析使用手动字符串处理
**文件**: `AuthServiceImpl.java:192-212`, `ScheduleServiceImpl.java:360-381`
**问题**: 权限JSON和座位布局JSON使用手动split/replace解析，脆弱且不处理边缘情况（含引号的值、空格等）。
**修复**: 使用 `com.fasterxml.jackson.databind.ObjectMapper`。

---

### 🟡 M21. JWT令牌中loginType未被利用
**文件**: `JwtTokenProvider.java:67, 157`
**问题**: `loginType` 写入JWT但从未在过滤器中用于区分管理员/用户令牌。理论上被盗的管理员token可在用户端使用。
**修复**: 在过滤器中验证 `loginType` 与请求路径的匹配性。

---

### 🟡 M22. 密码策略过弱
**文件**: `EmployeeController.java:100`, `RegisterRequest.java`
**问题**: 默认密码 `123456`，密码仅要求6位长度无复杂度要求。
**修复**: 要求8位以上含大小写字母和数字；首次登录强制改密。

---

### 🟡 M23. 前端影片管理搜索仅为前端过滤
**文件**: `frontend/src/views/admin/MovieManage.vue:168-172`
**问题**: `filteredMovies` computed在前端过滤，每次都拉取全部影片。影片数量大时性能下降。
**修复**: 使用后端搜索API `/api/movies/search?keyword=xxx`。

---

## LOW (低危) - 改进建议

### 🔵 L1. 缺少 `BindException` 处理
**文件**: `GlobalExceptionHandler.java`
**问题**: 只处理了 `MethodArgumentNotValidException`（@RequestBody校验），未处理 `BindException`（简单参数校验）。

---

### 🔵 L2. ApiResponse的code与实际HTTP状态码不对应
**文件**: `dto/ApiResponse.java`
**问题**: 所有Controller返回HTTP 200，业务状态码在响应体内。建议使用 `ResponseEntity<ApiResponse<T>>` 正确设置HTTP状态。

---

### 🔵 L3. Movie.isHot 使用 Integer 而非 Boolean
**文件**: `Movie.java:53`
**问题**: 0/1的Integer表示布尔值不直观。

---

### 🔵 L4. 所有实体未实现Serializable
**文件**: 所有实体类
**问题**: 如果引入缓存（如Redis），需要序列化支持。

---

### 🔵 L5. 缺少API文档（Swagger/OpenAPI）
**问题**: 前后端分离项目无API文档，增加沟通成本。

---

### 🔵 L6. 缺少单元测试
**问题**: 整个项目无任何测试文件，特别是订单支付系统涉及金额。

---

### 🔵 L7. 前端缺少TypeScript
**问题**: 纯JavaScript开发，缺少类型安全。

---

### 🔵 L8. 异常处理未覆盖常见Spring异常
**文件**: `GlobalExceptionHandler.java`
**问题**: 缺少 `HttpMessageNotReadableException`、`ConstraintViolationException`、`HttpRequestMethodNotSupportedException` 等常用异常处理器。

---

### 🔵 L9. Export API使用GET方法创建资源
**文件**: `StatisticsController.java:89`
**问题**: 导出操作使用GET，应使用POST或返回文件二进制流。

---

### 🔵 L10. getCurrentUserId重复定义4次
**文件**: `AuthController.java:99`, `OrderController.java:134`, `AdminOrderController.java:102`, `SystemController.java:142`
**问题**: 相同方法定义了4次，应提取到公共工具类。

---

### 🔵 L11. 更新员工信息时密码处理脆弱
**文件**: `EmployeeController.java:151`
**问题**: `employee.setPassword(null)` 依赖 MyBatis-Plus 的字段策略跳过null字段。若策略变更，密码会被设为null。

---

### 🔵 L12. schedule.end_time 应为NOT NULL
**文件**: `schema.sql:106`, `Schedule.java:30`
**问题**: `end_time` 可为NULL，但业务逻辑始终计算并依赖此字段。

---

### 🔵 L13. 改签时新订单 payTime 语义不正确
**文件**: `OrderServiceImpl.java:315`
**问题**: 改签创建的新订单直接设置 `payTime = now()`，但实际未发生新支付。

---

### 🔵 L14. seat_layout 行列范围未验证
**文件**: `Hall.java:43`, `ScheduleServiceImpl.java:332`
**问题**: 被禁用的座位行列号未验证是否在影厅范围内，超出范围的配置被静默忽略。

---

### 🔵 L15. 收入导出的文件路径依赖工作目录
**文件**: `StatisticsServiceImpl.java:304-313`
**问题**: 使用相对路径 `./uploads/reports/`，依赖JVM启动目录。

---

### 🔵 L16. 文件上传返回URL使用硬编码绝对路径
**文件**: `FileController.java:82`
**问题**: 返回 `/uploads/posters/...` 假设部署在根路径，若部署在其他context path会404。

---

## 问题统计汇总

| 严重级别 | 数量 | 说明 |
|----------|------|------|
| 🔴 CRITICAL | 15 | 安全漏洞、数据丢失、系统不可用 |
| 🟠 HIGH     | 17 | 功能缺陷、数据完整性、严重的性能问题 |
| 🟡 MEDIUM   | 27 | 逻辑错误、架构违规、用户体验 |
| 🔵 LOW      | 18 | 代码质量、规范、最佳实践 |
| **总计**    | **77** | |

---

## 修复优先级建议

1. **立即修复** (P0): C1-C11 所有CRITICAL级别问题
2. **本周内修复** (P1): H1-H15 所有HIGH级别问题
3. **下个迭代修复** (P2): M1-M23 所有MEDIUM级别问题
4. **技术债务跟踪** (P3): L1-L16 所有LOW级别问题

---

*报告由LLM深度代码审查生成，基于对Java后端(Spring Boot 3.2)、Vue前端(Vue 3 + Element Plus)、MySQL数据库的全面分析。*
