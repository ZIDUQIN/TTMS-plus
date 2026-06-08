import request from './index'

/**
 * 获取票房排行榜
 * @param {string} date - 查询日期 YYYY-MM-DD
 * @param {string} type - 票房类型: comprehensive(综合票房) / share(分账票房)
 */
export function getBoxOfficeRanking(date, type = 'comprehensive') {
  return request({
    url: '/admin/box-office/ranking',
    method: 'get',
    params: { date, type }
  })
}

/**
 * 获取大盘数据
 * @param {string} date - 查询日期 YYYY-MM-DD
 * @param {string} type - 票房类型
 */
export function getBoxOfficeDashboard(date, type = 'comprehensive') {
  return request({
    url: '/admin/box-office/dashboard',
    method: 'get',
    params: { date, type }
  })
}

/**
 * 获取指定影片详细票房信息
 * @param {number} movieId - 影片ID
 * @param {string} date - 查询日期
 * @param {string} type - 票房类型
 */
export function getBoxOfficeMovieDetail(movieId, date, type = 'comprehensive') {
  return request({
    url: `/admin/box-office/movie/${movieId}`,
    method: 'get',
    params: { date, type }
  })
}

/**
 * 获取指定影片近5日票房趋势
 * @param {number} movieId - 影片ID
 * @param {string} date - 查询日期
 * @param {string} type - 票房类型
 */
export function getBoxOfficeMovieTrend(movieId, date, type = 'comprehensive') {
  return request({
    url: `/admin/box-office/movie/${movieId}/trend`,
    method: 'get',
    params: { date, type }
  })
}
