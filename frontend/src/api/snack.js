import request from './index'

// ===== 卖品CRUD =====
export function getSnacks(category) {
  return request({
    url: '/admin/snacks',
    method: 'get',
    params: category ? { category } : {}
  })
}
export function addSnack(data) {
  return request({ url: '/admin/snacks', method: 'post', data })
}
export function updateSnack(data) {
  return request({ url: '/admin/snacks', method: 'put', data })
}
export function deleteSnack(id) {
  return request({ url: `/admin/snacks/${id}`, method: 'delete' })
}

// 公开：首页套餐展示
export function getPublicCombos() {
  return request({ url: '/snacks/combos', method: 'get' })
}
// 公开：快速购买套餐
export function orderCombo(comboId) {
  return request({ url: '/snacks/combo-order', method: 'post', data: { comboId } })
}

// ===== 套餐CRUD =====
export function getCombos() {
  return request({ url: '/admin/snacks/combos', method: 'get' })
}
export function addCombo(data) {
  return request({ url: '/admin/snacks/combos', method: 'post', data })
}
export function updateCombo(data) {
  return request({ url: '/admin/snacks/combos', method: 'put', data })
}
export function deleteCombo(id) {
  return request({ url: `/admin/snacks/combos/${id}`, method: 'delete' })
}

// ===== 卖品下单 =====
export function createSnackOrder(data) {
  return request({ url: '/admin/snacks/order', method: 'post', data })
}
export function getSnackOrders() {
  return request({ url: '/admin/snacks/orders', method: 'get' })
}
