import request from './index'

export function getPosSchedules() {
  return request({ url: '/admin/pos/schedules', method: 'get' })
}
export function getPosSeats(scheduleId) {
  return request({ url: `/admin/pos/seats/${scheduleId}`, method: 'get' })
}
export function posCreateOrder(data) {
  return request({ url: '/admin/pos/create-order', method: 'post', data })
}
