import request from './index'

// Get movie list
export function getMovieList(params = {}) {
  return request({
    url: '/movies/list',
    method: 'get',
    params: {
      page: params.page || 1,
      size: params.size || 10
    }
  })
}

// Get movie detail
export function getMovieDetail(id) {
  return request({
    url: `/movies/detail/${id}`,
    method: 'get'
  })
}

// Get hot movies
export function getHotMovies() {
  return request({
    url: '/movies/hot',
    method: 'get'
  })
}

// Search movies
export function searchMovies(keyword) {
  return request({
    url: '/movies/search',
    method: 'get',
    params: { keyword }
  })
}

// Add movie (admin)
export function addMovie(data) {
  return request({
    url: '/movies/add',
    method: 'post',
    data
  })
}

// Update movie (admin)
export function updateMovie(data) {
  return request({
    url: '/movies/update',
    method: 'put',
    data
  })
}

// Upload poster image
export function uploadPoster(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/upload',
    method: 'post',
    data: formData
    // Don't set Content-Type manually — axios will auto-set multipart/form-data with boundary
  })
}

// Delete movie (admin)
export function deleteMovie(id) {
  return request({
    url: `/movies/delete/${id}`,
    method: 'delete'
  })
}
