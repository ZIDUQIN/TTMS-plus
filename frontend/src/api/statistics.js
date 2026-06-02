import request from './index'

// Get revenue statistics (summary)
export function getRevenueStats(startDate, endDate) {
  return request({
    url: '/admin/statistics/revenue',
    method: 'get',
    params: { startDate, endDate }
  })
}

// Get daily revenue data (for trend chart)
export function getDailyRevenue(startDate, endDate) {
  return request({
    url: '/admin/statistics/revenue/daily',
    method: 'get',
    params: { startDate, endDate }
  })
}

// Get movie ranking statistics
export function getMovieRanking() {
  return request({
    url: '/admin/statistics/movie-ranking',
    method: 'get'
  })
}

// Get monthly statistics
export function getMonthlyStats() {
  return request({
    url: '/admin/statistics/monthly',
    method: 'get'
  })
}

// Export statistics - returns file path, open in browser to download
export function exportStatistics(startDate, endDate) {
  return request({
    url: '/admin/statistics/export',
    method: 'get',
    params: { startDate, endDate }
  })
}
