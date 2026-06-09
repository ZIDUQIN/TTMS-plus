import request from './index'

// ===== User Order APIs =====

// Create order
export function createOrder(data) {
  return request({
    url: '/user/orders/create',
    method: 'post',
    data
  })
}

// Pay order
export function payOrder(orderId) {
  return request({
    url: `/user/orders/pay/${orderId}`,
    method: 'post'
  })
}

// Get my orders
export function getMyOrders(page = 1, size = 10) {
  return request({
    url: '/user/orders/my',
    method: 'get',
    params: { page, size }
  })
}

// Get order detail
export function getOrderDetail(id) {
  return request({
    url: `/user/orders/detail/${id}`,
    method: 'get'
  })
}

// Reschedule order
export function rescheduleOrder(data) {
  return request({
    url: '/user/orders/reschedule',
    method: 'post',
    data
  })
}

// Refund order
export function refundOrder(orderId) {
  return request({
    url: `/user/orders/refund/${orderId}`,
    method: 'post'
  })
}

// ===== Admin Order APIs =====

// Get all orders (admin)
export function getAdminOrders(params = {}) {
  return request({
    url: '/admin/orders/list',
    method: 'get',
    params: { page: params.page || 1, size: params.size || 10, ...params }
  })
}

// Assist create order (admin)
export function assistCreateOrder(data) {
  return request({
    url: '/admin/orders/assist-create',
    method: 'post',
    data
  })
}

// Assist pay order (admin)
export function assistPayOrder(orderId) {
  return request({
    url: `/admin/orders/assist-pay/${orderId}`,
    method: 'post'
  })
}

// ===== Schedule APIs =====

// Get schedules by movie
export function getSchedulesByMovie(movieId) {
  return request({
    url: `/schedules/query/movie/${movieId}`,
    method: 'get'
  })
}

// Get upcoming schedules
export function getUpcomingSchedules() {
  return request({
    url: '/schedules/query/upcoming',
    method: 'get'
  })
}

// Get seat status for a schedule
export function getScheduleSeats(scheduleId) {
  return request({
    url: `/schedules/query/${scheduleId}/seats`,
    method: 'get'
  })
}

// Get all schedules (admin)
export function getScheduleList(params = {}) {
  return request({
    url: '/schedules/list',
    method: 'get',
    params: { page: params.page || 1, size: params.size || 10 }
  })
}

// Add schedule (admin)
export function addSchedule(data) {
  return request({
    url: '/schedules/add',
    method: 'post',
    data
  })
}

// Update schedule (admin)
export function updateSchedule(data) {
  return request({
    url: '/schedules/update',
    method: 'put',
    data
  })
}

// Delete schedule (admin)
export function deleteSchedule(id) {
  return request({
    url: `/schedules/delete/${id}`,
    method: 'delete'
  })
}

// ===== Hall APIs =====

// Get hall list (admin)
export function getHallList(params = {}) {
  return request({
    url: '/admin/halls/list',
    method: 'get',
    params: { page: params.page || 1, size: params.size || 10 }
  })
}

// Add hall (admin)
export function addHall(data) {
  return request({
    url: '/admin/halls/add',
    method: 'post',
    data
  })
}

// Update hall (admin)
export function updateHall(data) {
  return request({
    url: '/admin/halls/update',
    method: 'put',
    data
  })
}

// Delete hall (admin)
export function deleteHall(id) {
  return request({
    url: `/admin/halls/delete/${id}`,
    method: 'delete'
  })
}

// ===== Employee APIs =====

// Get employee list (admin)
export function getEmployeeList(params = {}) {
  return request({
    url: '/admin/employees/list',
    method: 'get',
    params: { page: params.page || 1, size: params.size || 10 }
  })
}

// Add employee (admin)
export function addEmployee(data) {
  return request({
    url: '/admin/employees/add',
    method: 'post',
    data
  })
}

// Update employee (admin)
export function updateEmployee(data) {
  return request({
    url: '/admin/employees/update',
    method: 'put',
    data
  })
}

// Reset employee password (admin)
export function resetEmployeePassword(id) {
  return request({
    url: `/admin/employees/reset-password/${id}`,
    method: 'put'
  })
}

// Toggle employee status (admin)
export function toggleEmployeeStatus(id) {
  return request({
    url: `/admin/employees/toggle-status/${id}`,
    method: 'put'
  })
}

// ===== System APIs =====

// Get system config
export function getSystemConfig() {
  return request({
    url: '/admin/system/config',
    method: 'get'
  })
}

// Update system config
export function updateSystemConfig(data) {
  return request({
    url: '/admin/system/config',
    method: 'put',
    data
  })
}

// Set system theme
export function setSystemTheme(data) {
  return request({
    url: '/user/theme',
    method: 'post',
    data
  })
}

// Get system logs
export function getSystemLogs() {
  return request({
    url: '/admin/system/logs',
    method: 'get'
  })
}
