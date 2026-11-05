/**
 * 统一请求工具 - 基于 uni.request 封装
 * 所有 API 调用经过此工具
 */
import config from '@/config/index.js'
const API_BASE = config.API_BASE_URL

/**
 * 通用请求方法
 * @param {string} url - 接口路径（不包含 base URL）
 * @param {string} method - 请求方法 GET/POST/PUT/DELETE
 * @param {object} data - 请求数据
 * @param {boolean} showLoading - 是否显示 loading
 */
function request(url, method = 'GET', data = {}, showLoading = true) {
  return new Promise((resolve, reject) => {
    if (showLoading) {
      uni.showLoading({ title: '加载中...', mask: true })
    }

    uni.request({
      url: API_BASE + url,
      method,
      data,
      header: {
        'Content-Type': 'application/json'
      },
      success: (res) => {
        if (showLoading) uni.hideLoading()

        if (res.statusCode === 200) {
          const data = res.data
          if (data.code === 200) {
            resolve(data)
          } else {
            uni.showToast({ title: data.msg || '请求失败', icon: 'none' })
            reject(data)
          }
        } else {
          uni.showToast({ title: '网络错误 ' + res.statusCode, icon: 'none' })
          reject(res)
        }
      },
      fail: (err) => {
        if (showLoading) uni.hideLoading()
        console.error('请求失败:', err)
        uni.showToast({ title: '网络连接失败', icon: 'none' })
        reject(err)
      }
    })
  })
}

// ========== 用户模块 ==========
export const userLogin = (username, password) =>
  request('/user/login/', 'POST', { username, password })

export const userRegister = (params) =>
  request('/user/register/', 'POST', params)

// ========== 文件上传 ==========
/**
 * 上传单张图片
 * @param {string} filePath - 文件临时路径
 * @returns {object} { url, filename, size }
 */
export const uploadImage = (filePath) => {
  return new Promise((resolve, reject) => {
    uni.showLoading({ title: '上传中...', mask: true })
    uni.uploadFile({
      url: API_BASE + '/upload/image',
      filePath,
      name: 'file',
      success: (res) => {
        uni.hideLoading()
        const data = JSON.parse(res.data)
        if (data.code === 200) {
          resolve(data)
        } else {
          uni.showToast({ title: data.msg || '上传失败', icon: 'none' })
          reject(data)
        }
      },
      fail: (err) => {
        uni.hideLoading()
        uni.showToast({ title: '上传失败', icon: 'none' })
        reject(err)
      }
    })
  })
}

/**
 * 批量上传图片（最多9张）
 * @param {string[]} filePaths - 文件临时路径数组
 * @returns {object[]} [{ url, filename, size, sort_order }]
 */
export const uploadImages = (filePaths) => {
  return new Promise((resolve, reject) => {
    if (!filePaths || filePaths.length === 0) {
      resolve([])
      return
    }
    uni.showLoading({ title: '上传中...', mask: true })

    const results = []
    let completed = 0
    let failed = 0

    filePaths.forEach((filePath, index) => {
      uni.uploadFile({
        url: API_BASE + '/upload/images',
        filePath,
        name: 'files',
        success: (res) => {
          const data = JSON.parse(res.data)
          if (data.code === 200 && data.files && data.files[index]) {
            results.push({ ...data.files[index], sort_order: index })
          } else {
            failed++
          }
        },
        fail: () => {
          failed++
        },
        complete: () => {
          completed++
          if (completed === filePaths.length) {
            uni.hideLoading()
            if (failed > 0) {
              uni.showToast({ title: `有 ${failed} 张图片上传失败`, icon: 'none' })
            }
            resolve(results.sort((a, b) => a.sort_order - b.sort_order))
          }
        }
      })
    })
  })
}

// ========== 帖子模块 ==========
/**
 * 获取帖子列表（广场）
 * @param {number} page
 * @param {number} pageSize
 * @param {number} currentUserId - 当前登录用户ID（可选）
 */
export const getPosts = (page = 1, pageSize = 10, currentUserId = 0) =>
  request('/circle/posts/', 'GET', { page, page_size: pageSize, current_user_id: currentUserId }, false)

/**
 * 获取用户帖子列表
 * @param {number} userId
 * @param {number} page
 * @param {number} pageSize
 * @param {number} currentUserId
 */
export const getUserPosts = (userId, page = 1, pageSize = 10, currentUserId = 0) =>
  request(`/circle/posts/user/${userId}`, 'GET', { page, page_size: pageSize, current_user_id: currentUserId }, false)

/**
 * 发布帖子
 * @param {object} params { user_id, content, location_name, latitude, longitude, visibility }
 */
export const createPost = (params) =>
  request('/circle/posts/', 'POST', params)

/**
 * 帖子绑定图片/视频
 * @param {number} postId
 * @param {object} body { images: [{url, sort_order}], videos: [{url, cover_url, duration}] }
 */
export const bindPostMedia = (postId, body) =>
  request(`/circle/posts/${postId}/media`, 'POST', body)

/**
 * 编辑帖子
 * @param {number} postId
 * @param {number} userId
 * @param {object} data { content, location_name, visibility }
 */
export const updatePost = (postId, userId, data) =>
  request(`/circle/posts/${postId}?user_id=${userId}`, 'PUT', data)

/**
 * 删除帖子
 * @param {number} postId
 * @param {number} userId
 */
export const deletePost = (postId, userId) =>
  request(`/circle/posts/${postId}?user_id=${userId}`, 'DELETE', {})

/**
 * 获取帖子详情
 * @param {number} postId
 * @param {number} currentUserId
 */
export const getPostDetail = (postId, currentUserId = 0) =>
  request(`/circle/posts/${postId}`, 'GET', { current_user_id: currentUserId }, false)

// ========== 点赞模块 ==========
/**
 * 帖子点赞/取消（toggle）
 * @param {number} postId
 * @param {number} userId
 */
export const togglePostLike = (postId, userId) =>
  request('/circle/likes/post', 'POST', { post_id: postId, user_id: userId })

/**
 * 查询是否已点赞
 * @param {number} postId
 * @param {number} userId
 */
export const checkLike = (postId, userId) =>
  request('/circle/likes/check', 'GET', { post_id: postId, user_id: userId }, false)

// ========== 评论模块 ==========
/**
 * 发布评论
 * @param {object} params { post_id, user_id, content, parent_id }
 */
export const createComment = (params) =>
  request('/circle/comments/', 'POST', params)

/**
 * 获取评论列表
 * @param {number} postId
 * @param {number} page
 * @param {number} pageSize
 */
export const getComments = (postId, page = 1, pageSize = 20) =>
  request(`/circle/comments/${postId}`, 'GET', { page, page_size: pageSize }, false)

/**
 * 删除评论
 * @param {number} commentId
 * @param {number} userId
 */
export const deleteComment = (commentId, userId) =>
  request(`/circle/comments/${commentId}?user_id=${userId}`, 'DELETE', {})

// ========== 导出 ==========
export default {
  userLogin,
  userRegister,
  uploadImage,
  uploadImages,
  getPosts,
  getUserPosts,
  createPost,
  bindPostMedia,
  updatePost,
  deletePost,
  getPostDetail,
  togglePostLike,
  checkLike,
  createComment,
  getComments,
  deleteComment
}
