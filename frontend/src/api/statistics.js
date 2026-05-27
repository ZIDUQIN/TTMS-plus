import request from './index'

// Get revenue statistics
export function getRevenueStats() {
  return request({
    url: '/admin/statistics/revenue',
    method: 'get'
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

// Export statistics
export function exportStatistics() {
  return request({
    url: '/admin/statistics/export',
    method: 'get',
    responseType: 'blob'
  })
}
