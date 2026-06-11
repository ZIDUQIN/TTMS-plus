import request from './index'

// 获取报表列表
export function getReportList(params = {}) {
  return request({
    url: '/admin/reports',
    method: 'get',
    params: {
      type: params.type,
      startDate: params.startDate,
      endDate: params.endDate
    }
  })
}

// 手动生成报表
export function generateReport(data = {}) {
  return request({
    url: '/admin/reports/generate',
    method: 'post',
    params: {
      type: data.type || 'DAILY',
      date: data.date
    }
  })
}
