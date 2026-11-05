const config = require('../config/index')

function getCurrentDistrict() {
  return new Promise((resolve) => {
    uni.getLocation({
      type: 'gcj02',
      success(res) {
        // 高德逆地理编码 API
        uni.request({
          url: config.GAODE_REVERSE_GEOCODER,
          method: 'GET',
          data: {
            key: config.GAODE_MAP_KEY,
            location: `${res.longitude},${res.latitude}`,
            extensions: 'base'
          },
          success: (response) => {
            const data = response.data
            const adInfo = data && data.regeocode && data.regeocode.addressComponent
            const district = adInfo
              ? `${adInfo.city || ''}${adInfo.district || ''}`
              : '获取中…'
            resolve({
              district,
              location: {
                latitude: res.latitude,
                longitude: res.longitude
              }
            })
          },
          fail() {
            resolve({ district: '获取中…', location: { latitude: res.latitude, longitude: res.longitude } })
          }
        })
      },
      fail() {
        resolve({ district: '获取中…', location: null })
      }
    })
  })
}

module.exports = {
  getCurrentDistrict
}
