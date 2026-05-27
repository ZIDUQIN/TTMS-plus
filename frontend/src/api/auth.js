import request from './index'

// User/Admin login
export function login(data) {
  return request({
    url: '/auth/login',
    method: 'post',
    data
  })
}

// User registration
export function register(data) {
  return request({
    url: '/auth/register',
    method: 'post',
    data
  })
}

// Change password
export function changePassword(data) {
  return request({
    url: '/auth/change-password',
    method: 'post',
    data
  })
}
