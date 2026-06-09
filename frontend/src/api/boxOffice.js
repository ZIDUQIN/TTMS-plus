import request from './index'

/**
 * 获取票房排行榜
 * @param {string} startDate - 开始日期 YYYY-MM-DD
 * @param {string} endDate - 结束日期 YYYY-MM-DD
 * @param {string} type - 票房类型: comprehensive(综合票房) / share(分账票房)
 */
export function getBoxOfficeRanking(startDate, endDate, type = 'comprehensive') {
  return request({
    url: '/admin/box-office/ranking',
    method: 'get',
    params: { startDate, endDate, type }
  })
}

/**
 * 获取大盘数据
 * @param {string} startDate - 开始日期 YYYY-MM-DD
 * @param {string} endDate - 结束日期 YYYY-MM-DD
 * @param {string} type - 票房类型
 */
export function getBoxOfficeDashboard(startDate, endDate, type = 'comprehensive') {
  return request({
    url: '/admin/box-office/dashboard',
    method: 'get',
    params: { startDate, endDate, type }
  })
}

/**
 * 获取指定影片详细票房信息
 * @param {number} movieId - 影片ID
 * @param {string} startDate - 开始日期
 * @param {string} endDate - 结束日期
 * @param {string} type - 票房类型
 */
export function getBoxOfficeMovieDetail(movieId, startDate, endDate, type = 'comprehensive') {
  return request({
    url: `/admin/box-office/movie/${movieId}`,
    method: 'get',
    params: { startDate, endDate, type }
  })
}

/**
 * 获取指定影片近N日票房趋势
 * @param {number} movieId - 影片ID
 * @param {string} date - 查询截止日期
 * @param {string} type - 票房类型
 * @param {number} days - 统计天数（默认7）
 */
export function getBoxOfficeMovieTrend(movieId, date, type = 'comprehensive', days = 7) {
  return request({
    url: `/admin/box-office/movie/${movieId}/trend`,
    method: 'get',
    params: { date, type, days }
  })
}
