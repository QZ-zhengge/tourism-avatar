package com.example.let_us_go.ui.pages.register

import android.util.Log
import com.example.let_us_go.core.base.BaseViewModel
import com.example.let_us_go.data.network.NetworkModule
import com.example.let_us_go.data.prefs.UserPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class RegisterViewModel : BaseViewModel() {

    fun register(
        username: String,
        phone: String,
        email: String,
        password: String,
        confirm: String,
        onSuccess: () -> Unit
    ) {
        if (username.isBlank() || password.isBlank()) {
            setError("用户名和密码不能为空")
            return
        }
        if (password != confirm) {
            setError("两次密码不一致")
            return
        }
        if (password.length < 6) {
            setError("密码长度至少6位")
            return
        }
        if (phone.isBlank()) {
            setError("请输入手机号")
            return
        }
        if (!phone.matches(Regex("^1[3-9]\\d{9}$"))) {
            setError("手机号格式不正确")
            return
        }
        if (email.isBlank()) {
            setError("请输入邮箱")
            return
        }
        if (!email.matches(Regex("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$"))) {
            setError("邮箱格式不正确")
            return
        }

        launchIo {
            val body = mutableMapOf(
                "username" to username,
                "password" to password,
                "confirm_password" to confirm,
                "phone_number" to phone,
                "email" to email
            )
            Log.d("RegisterVM", "注册请求: $body")

            try {
                val res = NetworkModule.authApi.register(body)
                Log.d("RegisterVM", "注册成功: $res")
                if (res.isSuccess && res.data != null) {
                    UserPrefs.saveLogin(res.data)
                    setError(null)
                    // 导航操作必须在主线程
                    withContext(Dispatchers.Main) {
                        onSuccess()
                    }
                } else {
                    setError(res.msg ?: "注册失败")
                }
            } catch (e: HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                Log.e("RegisterVM", "注册失败 422: $errorBody", e)
                setError("注册失败: $errorBody")
            }
        }
    }
}
