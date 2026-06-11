import request from './index'

// ===== 会员等级管理 =====
export function getMemberLevels() {
  return request({ url: '/admin/member-levels', method: 'get' })
}
export function addMemberLevel(data) {
  return request({ url: '/admin/member-levels', method: 'post', data })
}
export function updateMemberLevel(data) {
  return request({ url: '/admin/member-levels', method: 'put', data })
}
export function deleteMemberLevel(id) {
  return request({ url: `/admin/member-levels/${id}`, method: 'delete' })
}

// ===== 会员用户管理（管理端）=====
export function getMemberUsers(params = {}) {
  return request({
    url: '/admin/members',
    method: 'get',
    params: { page: params.page || 1, size: params.size || 20 }
  })
}
export function setUserLevel(userId, levelId) {
  return request({
    url: `/admin/members/${userId}/level`,
    method: 'put',
    data: { levelId }
  })
}
export function adjustUserPoints(userId, delta) {
  return request({
    url: `/admin/members/${userId}/points`,
    method: 'put',
    data: { delta }
  })
}
export function deleteUser(userId) {
  return request({ url: `/admin/members/${userId}`, method: 'delete' })
}

// ===== 用户端会员信息 =====
export function getMyMembership() {
  return request({ url: '/user/membership', method: 'get' })
}
export function rechargeBalance(data) {
  return request({ url: '/user/recharge', method: 'post', data })
}
export function redeemPoints(points) {
  return request({ url: '/user/redeem-points', method: 'post', data: { points } })
}
