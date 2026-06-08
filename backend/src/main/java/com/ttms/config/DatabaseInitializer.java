package com.ttms.config;

import com.ttms.entity.Employee;
import com.ttms.entity.Movie;
import com.ttms.entity.Role;
import com.ttms.entity.SystemConfig;
import com.ttms.mapper.EmployeeMapper;
import com.ttms.mapper.MovieMapper;
import com.ttms.mapper.RoleMapper;
import com.ttms.mapper.SystemConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 数据库初始化器
 * 应用启动时自动检查并插入默认数据（角色、管理员账号、系统配置）
 * 实现CommandLineRunner接口，在Spring容器初始化完成后执行
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseInitializer implements CommandLineRunner {

    private final RoleMapper roleMapper;
    private final EmployeeMapper employeeMapper;
    private final SystemConfigMapper systemConfigMapper;
    private final MovieMapper movieMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * 定义的三种默认角色及其权限
     * 权限格式为JSON数组字符串，前端根据权限列表控制功能显示
     */
    private static final String SUPER_ADMIN_PERMISSIONS =
        "[\"movie:manage\",\"hall:manage\",\"schedule:manage\",\"order:manage\"," +
        "\"employee:manage\",\"statistics:view\",\"system:manage\",\"all\"]";

    private static final String STAFF_PERMISSIONS =
        "[\"movie:manage\",\"hall:manage\",\"schedule:manage\",\"order:manage\",\"statistics:view\"]";

    private static final String USER_PERMISSIONS =
        "[\"movie:view\",\"order:create\",\"order:view\",\"order:reschedule\",\"order:refund\",\"theme:set\"]";

    @Override
    public void run(String... args) {
        log.info("========== 开始数据库初始化检查 ==========");
        initRoles();
        initSuperAdmin();
        initSystemConfig();
        initMovies();
        log.info("========== 数据库初始化检查完成 ==========");
    }

    /**
     * 初始化角色数据
     * 如果角色表为空，则插入三种默认角色
     */
    private void initRoles() {
        List<Role> roles = roleMapper.selectList(null);
        if (roles == null || roles.isEmpty()) {
            log.info("角色表为空，开始插入默认角色...");

            Role superAdmin = new Role();
            superAdmin.setRoleCode("ROLE_SUPER_ADMIN");
            superAdmin.setRoleName("超级管理员");
            superAdmin.setDescription("拥有系统所有权限，可管理员工账号和系统配置");
            superAdmin.setPermissions(SUPER_ADMIN_PERMISSIONS);
            roleMapper.insert(superAdmin);

            Role staff = new Role();
            staff.setRoleCode("ROLE_STAFF");
            staff.setRoleName("普通员工");
            staff.setDescription("可管理影片、影厅、场次、订单，查看统计数据");
            staff.setPermissions(STAFF_PERMISSIONS);
            roleMapper.insert(staff);

            Role user = new Role();
            user.setRoleCode("ROLE_USER");
            user.setRoleName("普通用户");
            user.setDescription("可浏览影片、购买电影票、管理自己的订单");
            user.setPermissions(USER_PERMISSIONS);
            roleMapper.insert(user);

            log.info("默认角色插入完成: 超级管理员(ROLE_SUPER_ADMIN), 普通员工(ROLE_STAFF), 普通用户(ROLE_USER)");
        } else {
            log.info("角色表已有 {} 条数据，跳过初始化", roles.size());
        }
    }

    /**
     * 初始化超级管理员账号
     * 如果员工表不存在admin用户，则创建默认超级管理员
     * 默认用户名: admin, 密码: admin123（BCrypt加密）
     */
    private void initSuperAdmin() {
        Employee admin = employeeMapper.findByUsername("admin");
        if (admin == null) {
            log.info("超级管理员账号不存在，开始创建默认管理员...");

            // 查询超级管理员角色ID
            Role superAdminRole = roleMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Role>()
                    .eq(Role::getRoleCode, "ROLE_SUPER_ADMIN")
            ).stream().findFirst().orElse(null);

            if (superAdminRole == null) {
                log.error("未找到ROLE_SUPER_ADMIN角色，无法创建管理员账号！");
                return;
            }

            Employee employee = new Employee();
            employee.setEmployeeNo("EMP001");           // 工号
            employee.setUsername("admin");               // 用户名
            employee.setPassword(passwordEncoder.encode("admin123")); // 密码BCrypt加密
            employee.setRealName("系统管理员");          // 真实姓名
            employee.setPhone("13800000000");            // 默认手机号
            employee.setRoleId(superAdminRole.getId());   // 关联角色
            employee.setStatus(0);                        // 状态正常
            employeeMapper.insert(employee);

            log.info("默认超级管理员创建完成: 用户名=admin, 密码=admin123");
        } else {
            log.info("超级管理员账号已存在，用户名={}", admin.getUsername());
        }
    }

    /**
     * 初始化系统配置
     * 如果系统配置表为空，插入默认配置项
     */
    private void initSystemConfig() {
        List<SystemConfig> configs = systemConfigMapper.selectList(null);
        if (configs == null || configs.isEmpty()) {
            log.info("系统配置表为空，开始插入默认配置...");

            // 默认主题色
            SystemConfig theme = new SystemConfig();
            theme.setConfigKey("theme");
            theme.setConfigValue("white");
            theme.setDescription("系统默认主题: white(日间模式) / dark(夜间模式)");
            systemConfigMapper.insert(theme);

            // 默认分账比例
            SystemConfig shareRatio = new SystemConfig();
            shareRatio.setConfigKey("share_ratio");
            shareRatio.setConfigValue("52");
            shareRatio.setDescription("影院分账比例(百分比)，如52代表影院拿52%");
            systemConfigMapper.insert(shareRatio);

            // 影院名称
            SystemConfig cinemaName = new SystemConfig();
            cinemaName.setConfigKey("cinema_name");
            cinemaName.setConfigValue("TTMS电影院");
            cinemaName.setDescription("影院名称");
            systemConfigMapper.insert(cinemaName);

            // 默认联系电话
            SystemConfig phone = new SystemConfig();
            phone.setConfigKey("contact_phone");
            phone.setConfigValue("400-888-8888");
            phone.setDescription("影院联系电话");
            systemConfigMapper.insert(phone);

            // 订单超时时间（分钟）
            SystemConfig orderTimeout = new SystemConfig();
            orderTimeout.setConfigKey("order_timeout");
            orderTimeout.setConfigValue("15");
            orderTimeout.setDescription("未支付订单超时自动取消时间(分钟)");
            systemConfigMapper.insert(orderTimeout);

            // 默认公告
            SystemConfig notice = new SystemConfig();
            notice.setConfigKey("notice");
            notice.setConfigValue("欢迎来到TTMS电影院管理系统！");
            notice.setDescription("影院公告信息");
            systemConfigMapper.insert(notice);

            log.info("系统默认配置插入完成: 共5项配置");
        } else {
            log.info("系统配置表已有 {} 条数据，跳过初始化", configs.size());
        }
    }

    /**
     * 初始化电影数据
     * 仅当电影表少于5条记录时导入初始数据
     */
    private void initMovies() {
        List<Movie> existing = movieMapper.selectList(null);
        if (existing != null && existing.size() >= 5) {

            log.info("影片表已有 {} 条数据，跳过电影数据初始化", existing.size());
            return;
        }
        log.info("影片数据不足5条，开始导入初始热门电影（跳过已存在的影片）...");

        Movie[] movies = {
            createMovie("流浪地球3", "科幻,冒险", 150, "吴京,刘德华,李雪健", "郭帆", "太阳即将毁灭，人类在地球表面建造出巨大的推进器，开启长达2500年的流浪之旅。", "2026-06-01", 59.90, 1, 1, 100, "中国", "国语", 8.5),
            createMovie("哪吒之魔童闹海", "动画,奇幻", 120, "吕艳婷,囧森瑟夫", "饺子", "哪吒与敖丙在天劫之后的故事继续展开，封神大战一触即发。", "2026-07-15", 49.90, 1, 1, 99, "中国", "国语", 8.8),
            createMovie("阿凡达3：火与烬", "科幻,动作,冒险", 197, "萨姆·沃辛顿,佐伊·索尔达娜", "詹姆斯·卡梅隆", "杰克·萨利和奈蒂莉探索潘多拉的新区域，遭遇火族纳美人部落的冲突。", "2026-07-20", 79.90, 1, 1, 98, "美国", "英语", 8.2),
            createMovie("超人", "动作,科幻,冒险", 129, "大卫·科伦斯韦,瑞秋·布罗斯纳安", "詹姆斯·古恩", "新一代超人克拉克·肯特重新定义英雄的意义，在帮助人类与保护氪星遗产之间找到平衡。", "2026-08-10", 59.90, 1, 1, 97, "美国", "英语", 7.8),
            createMovie("镖人：风起大漠", "动作,武侠,古装", 135, "吴京,谢霆锋,李连杰,于适", "袁和平", "隋末大漠之中，侠客刀马护送神秘人物，与各方势力展开惊心动魄的对决。", "2026-09-01", 54.90, 1, 1, 96, "中国", "国语", 8.0),
            createMovie("封神第二部：战火西岐", "奇幻,战争", 148, "费翔,黄渤,于适,陈牧驰", "乌尔善", "姬发回到西岐，闻仲率大军征讨，两大阵营的旷世之战正式拉开帷幕。", "2026-08-01", 54.90, 1, 1, 95, "中国", "国语", 8.3),
            createMovie("碟中谍8：最终清算", "动作,惊悚,冒险", 156, "汤姆·克鲁斯,海莉·阿特维尔", "克里斯托弗·麦奎里", "伊森·亨特面临终极挑战，AI智体的真相即将揭晓，IMF小组迎来最后的任务。", "2026-05-30", 59.90, 1, 0, 94, "美国", "英语", 8.4),
            createMovie("复仇者联盟：末日", "动作,科幻,冒险", 150, "小罗伯特·唐尼,佩德罗·帕斯卡", "罗素兄弟", "多元宇宙面临毁灭危机，复仇者联盟集结应对史上最大威胁——毁灭博士。", "2026-10-01", 69.90, 1, 1, 93, "美国", "英语", 8.0),
            createMovie("飞驰人生3", "剧情,喜剧,运动", 125, "沈腾,尹正,黄景瑜", "韩寒", "张驰再次踏上赛场，这一次的挑战不同以往，但他心中的赛车梦从未熄灭。", "2026-07-01", 49.90, 1, 1, 92, "中国", "国语", 7.9),
            createMovie("女足", "喜剧,运动", 115, "张小斐,张艺兴,迪丽热巴", "周星驰", "一群热爱足球的姑娘们组成业余女足队伍，在星爷的指导下追逐梦想。", "2026-09-15", 49.90, 1, 0, 91, "中国", "国语", 7.5),
            createMovie("利刃出鞘3", "悬疑,犯罪", 144, "丹尼尔·克雷格,乔什·奥康纳", "莱恩·约翰逊", "贝努瓦·布兰克侦探面临最具个人意义的案件，一个家族秘密即将被揭穿。", "2026-09-20", 54.90, 1, 0, 90, "美国", "英语", 8.1),
            createMovie("米奇17", "科幻,剧情,冒险", 137, "罗伯特·帕丁森,史蒂文·元", "奉俊昊", "一个可以不断复制的'消耗品'在殖民星球上遭遇自己的另一个副本，引发身份危机的黑色喜剧。", "2026-06-15", 54.90, 1, 0, 89, "美国,韩国", "英语", 7.8),
            createMovie("我的朋友安德烈", "剧情", 111, "刘昊然,董子健,殷桃", "董子健", "两个少年从初中到大学的成长故事，时光荏苒，友谊却始终如一。", "2026-06-20", 44.90, 1, 0, 87, "中国", "国语", 8.0),
            createMovie("角斗士2", "动作,剧情,历史", 148, "保罗·麦斯卡,丹泽尔·华盛顿", "雷德利·斯科特", "卢修斯在角斗场为生存而战，罗马帝国的命运悬于一线。", "2026-05-28", 59.90, 1, 0, 86, "美国", "英语", 7.6),
            createMovie("人·鱼", "剧情", 120, "王一博,汤唯,王传君", "程耳", "两个来自不同世界的灵魂相遇，跨越物种的爱恋在命运的洪流中挣扎。", "2026-12-01", 49.90, 2, 0, 85, "中国", "国语", 7.5),
            createMovie("神探之痕迹", "剧情,犯罪", 128, "张译,马丽", "陈思诚", "退休刑警追踪一桩未破的悬案，蛛丝马迹中隐藏的真相比想象更加骇人。", "2026-10-15", 49.90, 2, 0, 84, "中国", "国语", 7.8),
            createMovie("群星闪耀时", "动作,科幻,冒险", 140, "黄渤,吴磊,高叶,孙阳", "章笛沙", "未来世界能源枯竭，一群探险者深入宇宙寻找新的希望，却发现惊天的秘密。", "2026-11-15", 54.90, 2, 0, 83, "中国", "国语", 7.3),
            createMovie("欢迎来龙餐馆", "剧情,喜剧,战争", 118, "沈腾,蒋奇明", "文牧野", "战火中的中东地区，一家中国餐馆成为各方避风的港湾，笑中带泪的人性故事。", "2026-08-20", 49.90, 1, 0, 82, "中国", "国语", 8.2),
            createMovie("熊出没·年年有熊", "喜剧,动画,奇幻", 105, "张秉君,张伟", "林汇达", "光头强和熊大熊二再次踏上欢乐冒险，这次他们穿越到了古老的神话世界。", "2026-06-01", 39.90, 1, 0, 81, "中国", "国语", 7.0),
            createMovie("弗兰肯斯坦", "恐怖,科幻,剧情", 149, "奥斯卡·伊萨克,雅各布·艾洛蒂", "吉尔莫·德尔·托罗", "德尔·托罗重新诠释玛丽·雪莱的经典，一个关于创造与毁灭的哥特式黑暗童话。", "2026-07-25", 54.90, 1, 0, 80, "美国", "英语", 8.3),
            createMovie("96分钟", "剧情,动作,犯罪", 119, "林柏宏,宋芸桦,王柏杰", "洪子烜", "台湾犯罪片，96分钟内警方必须阻止一场劫持，时间紧迫，步步惊心。", "2026-06-10", 39.90, 1, 0, 78, "中国台湾", "国语", 7.1),
            createMovie("惊天魔盗团3", "动作,惊悚,犯罪", 113, "杰西·艾森伯格,伍迪·哈里森", "鲁本·弗雷斯彻", "天启四骑士重出江湖，这一次他们将面对最危险的对手——国际刑警组织的精英追捕队。", "2026-11-01", 49.90, 2, 0, 88, "美国", "英语", 7.0),
            createMovie("不过是上班", "喜剧", 101, "吴俊霆,李孝谦,安娜", "王梓骏", "初入职场的年轻人遭遇各种奇葩职场事件，笑料百出却也引人共鸣。", "2026-09-01", 39.90, 2, 0, 77, "中国", "国语", 6.8),
            createMovie("植物学家", "剧情,爱情,奇幻", 96, "叶斯力·加和斯力克,任紫晗", "景一", "一位哈萨克植物学家在深山发现一株具有魔力的植物，与心爱之人跨时空相连。", "2026-07-10", 39.90, 1, 0, 76, "中国", "国语", 7.6),
            createMovie("铁血战士：恶土", "动作,科幻,恐怖", 107, "艾丽·范宁", "丹·特拉亨伯格", "外星猎手降临地球荒原，一位年轻女性必须独自面对超越想象的恐惧。", "2026-08-15", 54.90, 2, 0, 79, "美国", "英语", 7.2),
        };

        for (Movie movie : movies) {
            // 检查影片是否已存在，避免重复插入
            Movie existingMovie = movieMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Movie>()
                    .eq(Movie::getMovieName, movie.getMovieName())
            );
            if (existingMovie == null) {
                movieMapper.insert(movie);
            }
        }
        log.info("已成功导入初始热门电影数据");
    }

    private Movie createMovie(String movieName, String genre, int duration, String actors, String director,
                               String description, String releaseDate, double basePrice, int status, int isHot,
                               int sortOrder, String country, String language, double rating) {
        Movie movie = new Movie();
        movie.setMovieName(movieName);
        movie.setGenre(genre);
        movie.setDuration(duration);
        movie.setActors(actors);
        movie.setDirector(director);
        movie.setDescription(description);
        movie.setReleaseDate(LocalDate.parse(releaseDate));
        movie.setBasePrice(BigDecimal.valueOf(basePrice));
        movie.setStatus(status);
        movie.setIsHot(isHot);
        movie.setSortOrder(sortOrder);
        movie.setCountry(country);
        movie.setLanguage(language);
        movie.setRating(rating);
        return movie;
    }
}
