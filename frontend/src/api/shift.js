import request from './index'

// 上班签到
export function startShift() {
  return request({ url: '/admin/shifts/start', method: 'post' })
}

// 下班交班
export function endShift(data) {
  return request({ url: '/admin/shifts/end', method: 'post', data })
}

// 查询当前班次
export function getActiveShift() {
  return request({ url: '/admin/shifts/active', method: 'get' })
}

// 班次历史列表
export function getShiftList(params = {}) {
  return request({
    url: '/admin/shifts/list',
    method: 'get',
    params: { page: params.page || 1, size: params.size || 20 }
  })
}
