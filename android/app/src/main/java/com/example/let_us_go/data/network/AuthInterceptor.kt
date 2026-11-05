package com.example.let_us_go.data.network

import com.example.let_us_go.data.prefs.UserPrefs
import okhttp3.Interceptor
import okhttp3.Response

/**
 * 鉴权拦截器
 *
 * 替代 uni 端 `request()` 没有 Authorization 头的行为：
 * uni 端鉴权是业务层通过 `user_id` / `current_user_id` query 参数实现。
 * Android 端**保持一致**，只在登录后注入 `X-User-Id` 头（可选），
 * 业务接口的鉴权仍由业务层携带 `user_id` 参数。
 *
 * 如果后端未来要 JWT，本拦截器可无缝升级到 Bearer 鉴权。
 */
class AuthInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // 跳过需要匿名访问的接口（如登录、注册、上传）
        val path = request.url.encodedPath
        val anonymous = path.endsWith("/user/login/") ||
            path.endsWith("/user/register/") ||
            path.endsWith("/upload/image") ||
            path.endsWith("/upload/images")

        if (anonymous) {
            return chain.proceed(request)
        }

        // 注入用户标识（与 uni 端"伪造 token = user-{userId}"语义一致）
        val userId = UserPrefs.userIdSync() ?: return chain.proceed(request)

        val newRequest = request.newBuilder()
            .header("X-User-Id", userId.toString())
            .header("X-Client", "android")
            .build()

        return chain.proceed(newRequest)
    }
}
