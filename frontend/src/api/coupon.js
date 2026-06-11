import request from './index'

// 管理端
export function getCouponList() {
  return request({ url: '/admin/coupons', method: 'get' })
}
export function createCoupon(data) {
  return request({ url: '/admin/coupons', method: 'post', data })
}
export function updateCoupon(data) {
  return request({ url: '/admin/coupons', method: 'put', data })
}
export function deleteCoupon(id) {
  return request({ url: `/admin/coupons/${id}`, method: 'delete' })
}
// 用户端
export function getMyCoupons() {
  return request({ url: '/user/coupons', method: 'get' })
}
export function getAvailableCoupons() {
  return request({ url: '/user/coupons/available', method: 'get' })
}
export function obtainCoupon(couponId) {
  return request({ url: `/user/coupons/${couponId}/obtain`, method: 'post' })
}
export function calculateCoupon(data) {
  return request({ url: '/user/coupons/calculate', method: 'post', data })
}
export function useCoupon(userCouponId, orderId) {
  return request({ url: `/user/coupons/${userCouponId}/use`, method: 'post', data: { orderId } })
}
