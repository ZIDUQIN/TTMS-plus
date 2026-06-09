package com.ttms.service.impl;

import com.ttms.entity.Movie;
import com.ttms.entity.Order;
import com.ttms.entity.Schedule;
import com.ttms.entity.SystemConfig;
import com.ttms.mapper.MovieMapper;
import com.ttms.mapper.OrderMapper;
import com.ttms.mapper.ScheduleMapper;
import com.ttms.mapper.SystemConfigMapper;
import com.ttms.service.BoxOfficeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 电影票房服务实现类
 * 支持单日和日期范围的票房统计、大盘数据、影片详情、趋势数据
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BoxOfficeServiceImpl implements BoxOfficeService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final OrderMapper orderMapper;
    private final ScheduleMapper scheduleMapper;
    private final MovieMapper movieMapper;
    private final SystemConfigMapper systemConfigMapper;

    @Override
    public List<Map<String, Object>> getRanking(LocalDate startDate, LocalDate endDate, String type) {
        String startStr = startDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String endStr = endDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate) + 1; // 含头尾

        // 1. 查询该日期范围内所有有效订单（含影片信息）
        List<Order> orders = orderMapper.selectByScheduleDateRange(startStr, endStr);

        // 2. 按影片ID分组聚合票房数据
        Map<Long, List<Order>> ordersByMovie = orders.stream()
                .collect(Collectors.groupingBy(Order::getMovieId, LinkedHashMap::new, Collectors.toList()));

        // 3. 查询该日期范围内所有场次，计算排片统计
        List<Schedule> schedules = scheduleMapper.selectByDateRangeWithHall(startStr, endStr);
        Map<Long, List<Schedule>> schedulesByMovie = schedules.stream()
                .collect(Collectors.groupingBy(Schedule::getMovieId, Collectors.toList()));
        int totalSchedules = schedules.size();

        // 4. 计算每个影片的综合票房
        List<MovieStat> stats = new ArrayList<>();
        for (Map.Entry<Long, List<Order>> entry : ordersByMovie.entrySet()) {
            Long movieId = entry.getKey();
            List<Order> movieOrders = entry.getValue();

            // 综合票房（原始值，元）
            BigDecimal rawBoxOffice = movieOrders.stream()
                    .map(o -> o.getTotalPrice() != null ? o.getTotalPrice() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            int ticketCount = movieOrders.stream()
                    .mapToInt(o -> o.getSeatCount() != null ? o.getSeatCount() : 0)
                    .sum();

            MovieStat stat = new MovieStat();
            stat.movieId = movieId;
            stat.rawBoxOffice = rawBoxOffice;
            stat.ticketCount = ticketCount;

            // 从第一个订单获取影片信息
            Order firstOrder = movieOrders.get(0);
            stat.movieName = firstOrder.getMovieName();
            stat.genre = null;
            stat.posterUrl = firstOrder.getMoviePoster();
            stat.releaseDate = null;

            stats.add(stat);
        }

        // 5. 补充Movie表中的完整信息
        for (MovieStat stat : stats) {
            Movie movie = movieMapper.selectById(stat.movieId);
            if (movie != null) {
                stat.movieName = movie.getMovieName();
                stat.genre = movie.getGenre();
                stat.posterUrl = movie.getPosterUrl();
                stat.releaseDate = movie.getReleaseDate();
            }
        }

        // 6. 按综合票房降序排序
        stats.sort((a, b) -> b.rawBoxOffice.compareTo(a.rawBoxOffice));

        // 7. 总综合票房
        BigDecimal totalRawBoxOffice = stats.stream()
                .map(s -> s.rawBoxOffice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 8. 构建返回数据
        List<Map<String, Object>> ranking = new ArrayList<>();
        int rank = 1;

        for (MovieStat stat : stats) {
            // 排片数据
            List<Schedule> movieSchedules = schedulesByMovie.getOrDefault(stat.movieId, Collections.emptyList());
            int scheduleCount = movieSchedules.size();
            long capacitySum = movieSchedules.stream()
                    .mapToLong(s -> {
                        Integer rows = s.getHallRowCount();
                        Integer cols = s.getHallColCount();
                        return (rows != null && cols != null) ? (long) rows * cols : 0;
                    })
                    .sum();

            // 场均人次 = 总出票 / 场次数（多日聚合仍按单日场均计算再平均）
            double avgAttendance = scheduleCount > 0
                    ? divide(stat.ticketCount, scheduleCount, 1) : 0;

            // 上座率 = 总出票 / 总容量 * 100
            double occupancyRate = capacitySum > 0
                    ? divide(stat.ticketCount * 100.0, capacitySum, 1) : 0;

            // 排片占比
            double scheduleRatio = totalSchedules > 0
                    ? divide(scheduleCount * 100.0, totalSchedules, 1) : 0;

            // 票房占比
            double boxOfficeRatio = totalRawBoxOffice.compareTo(BigDecimal.ZERO) > 0
                    ? stat.rawBoxOffice.multiply(HUNDRED).divide(totalRawBoxOffice, 1, RoundingMode.HALF_UP).doubleValue()
                    : 0;

            // 根据type转换显示票房
            BigDecimal displayBoxOffice = applyShareRatio(stat.rawBoxOffice, type);

            // 上映天数 & 累计票房（到endDate为止）
            long daysSinceRelease = 0;
            double cumulativeBoxOffice = 0;
            if (stat.releaseDate != null) {
                daysSinceRelease = ChronoUnit.DAYS.between(stat.releaseDate, endDate) + 1;
                if (daysSinceRelease < 0) daysSinceRelease = 0;
                List<Order> allOrders = orderMapper.selectByMovieIdAllTime(stat.movieId);
                BigDecimal cumulative = allOrders.stream()
                        .map(o -> o.getTotalPrice() != null ? o.getTotalPrice() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                cumulativeBoxOffice = cumulative.setScale(2, RoundingMode.HALF_UP).doubleValue();
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("rank", rank++);
            item.put("movieId", stat.movieId);
            item.put("movieName", stat.movieName);
            item.put("genre", stat.genre);
            item.put("posterUrl", stat.posterUrl);
            item.put("releaseDate", stat.releaseDate != null ? stat.releaseDate.format(DateTimeFormatter.ISO_LOCAL_DATE) : null);
            item.put("daysSinceRelease", daysSinceRelease);
            item.put("cumulativeBoxOffice", cumulativeBoxOffice);
            item.put("boxOffice", displayBoxOffice.setScale(2, RoundingMode.HALF_UP).doubleValue());
            item.put("boxOfficeRatio", boxOfficeRatio);
            item.put("ticketCount", stat.ticketCount);
            item.put("scheduleCount", scheduleCount);
            item.put("scheduleRatio", scheduleRatio);
            item.put("avgAttendance", avgAttendance);
            item.put("occupancyRate", occupancyRate);

            ranking.add(item);
        }

        log.info("票房排行榜: startDate={}, endDate={}, type={}, 共{}部影片, 总场次={}",
                startStr, endStr, type, ranking.size(), totalSchedules);
        return ranking;
    }

    @Override
    public Map<String, Object> getDashboard(LocalDate startDate, LocalDate endDate, String type) {
        String startStr = startDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String endStr = endDate.format(DateTimeFormatter.ISO_LOCAL_DATE);

        // 查询该日期范围内所有订单
        List<Order> orders = orderMapper.selectByScheduleDateRange(startStr, endStr);

        // 总综合票房
        BigDecimal totalRawBoxOffice = orders.stream()
                .map(o -> o.getTotalPrice() != null ? o.getTotalPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 总出票
        int totalTickets = orders.stream()
                .mapToInt(o -> o.getSeatCount() != null ? o.getSeatCount() : 0)
                .sum();

        // 总场次
        List<Schedule> schedules = scheduleMapper.selectByDateRangeWithHall(startStr, endStr);
        int totalSchedules = schedules.size();

        BigDecimal displayBoxOffice = applyShareRatio(totalRawBoxOffice, type);

        Map<String, Object> dashboard = new LinkedHashMap<>();
        dashboard.put("startDate", startStr);
        dashboard.put("endDate", endStr);
        dashboard.put("totalBoxOffice", displayBoxOffice.setScale(2, RoundingMode.HALF_UP).doubleValue());
        dashboard.put("totalTickets", totalTickets);
        dashboard.put("totalSchedules", totalSchedules);

        log.info("大盘数据: startDate={}, endDate={}, type={}, 票房={}元, 出票={}, 场次={}",
                startStr, endStr, type, dashboard.get("totalBoxOffice"), totalTickets, totalSchedules);
        return dashboard;
    }

    @Override
    public Map<String, Object> getMovieDetail(Long movieId, LocalDate startDate, LocalDate endDate, String type) {
        // 复用排行榜数据找到目标影片
        List<Map<String, Object>> ranking = getRanking(startDate, endDate, type);
        Map<String, Object> movieData = ranking.stream()
                .filter(m -> movieId.equals(m.get("movieId")))
                .findFirst()
                .orElse(null);

        if (movieData != null) {
            return movieData;
        }

        // 该日期范围无票房数据，返回影片基本信息
        Movie movie = movieMapper.selectById(movieId);
        if (movie == null) {
            return null;
        }

        long daysSinceRelease = movie.getReleaseDate() != null
                ? ChronoUnit.DAYS.between(movie.getReleaseDate(), endDate) + 1 : 0;
        if (daysSinceRelease < 0) daysSinceRelease = 0;

        List<Order> allOrders = orderMapper.selectByMovieIdAllTime(movieId);
        BigDecimal cumulative = allOrders.stream()
                .map(o -> o.getTotalPrice() != null ? o.getTotalPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("rank", 0);
        detail.put("movieId", movieId);
        detail.put("movieName", movie.getMovieName());
        detail.put("genre", movie.getGenre());
        detail.put("posterUrl", movie.getPosterUrl());
        detail.put("releaseDate", movie.getReleaseDate() != null ? movie.getReleaseDate().format(DateTimeFormatter.ISO_LOCAL_DATE) : null);
        detail.put("daysSinceRelease", daysSinceRelease);
        detail.put("cumulativeBoxOffice", cumulative.setScale(2, RoundingMode.HALF_UP).doubleValue());
        detail.put("boxOffice", 0.0);
        detail.put("boxOfficeRatio", 0.0);
        detail.put("ticketCount", 0);
        detail.put("scheduleCount", 0);
        detail.put("scheduleRatio", 0.0);
        detail.put("avgAttendance", 0.0);
        detail.put("occupancyRate", 0.0);
        return detail;
    }

    @Override
    public List<Map<String, Object>> getMovieTrend(Long movieId, LocalDate endDate, String type, int days) {
        // 往前推 days-1 天，共 days 天
        LocalDate startDate = endDate.minusDays(days - 1);

        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate d = startDate.plusDays(i);
            String dateKey = d.format(DateTimeFormatter.ISO_LOCAL_DATE);

            // 查询该日期的所有订单，然后过滤目标影片
            List<Order> dayOrders = orderMapper.selectByScheduleDate(dateKey);
            List<Order> movieDayOrders = dayOrders.stream()
                    .filter(o -> movieId.equals(o.getMovieId()))
                    .collect(Collectors.toList());

            BigDecimal rawRevenue = movieDayOrders.stream()
                    .map(o -> o.getTotalPrice() != null ? o.getTotalPrice() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            int tickets = movieDayOrders.stream()
                    .mapToInt(o -> o.getSeatCount() != null ? o.getSeatCount() : 0)
                    .sum();

            BigDecimal revenue = applyShareRatio(rawRevenue, type);

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", dateKey);
            item.put("revenue", revenue.setScale(2, RoundingMode.HALF_UP).doubleValue());
            item.put("ticketCount", tickets);
            trend.add(item);
        }

        log.info("趋势查询: movieId={}, endDate={}, type={}, {}天", movieId,
                endDate.format(DateTimeFormatter.ISO_LOCAL_DATE), type, trend.size());
        return trend;
    }

    /**
     * 从系统配置读取分账比例（百分比，如52代表52%）
     */
    private BigDecimal getShareRatio() {
        try {
            SystemConfig config = systemConfigMapper.selectByKey("share_ratio");
            if (config != null && config.getConfigValue() != null) {
                int percent = Integer.parseInt(config.getConfigValue().trim());
                return new BigDecimal(percent).divide(HUNDRED, 4, RoundingMode.HALF_UP);
            }
        } catch (Exception e) {
            log.warn("读取分账比例失败，使用默认52%", e);
        }
        return new BigDecimal("0.52");
    }

    private BigDecimal applyShareRatio(BigDecimal amount, String type) {
        if ("share".equals(type) && amount != null) {
            return amount.multiply(getShareRatio()).setScale(2, RoundingMode.HALF_UP);
        }
        return amount != null ? amount : BigDecimal.ZERO;
    }

    private double divide(double numerator, double denominator, int scale) {
        if (denominator == 0) return 0;
        return new BigDecimal(numerator).divide(new BigDecimal(denominator), scale, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * 内部影片统计类
     */
    private static class MovieStat {
        Long movieId;
        String movieName;
        String genre;
        String posterUrl;
        LocalDate releaseDate;
        BigDecimal rawBoxOffice;
        int ticketCount;
    }
}
