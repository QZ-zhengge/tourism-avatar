package com.example.let_us_go.ui.pages.login

import android.util.Log
import com.example.let_us_go.core.base.BaseViewModel
import com.example.let_us_go.core.bus.AppEventBus
import com.example.let_us_go.data.network.NetworkModule
import com.example.let_us_go.data.prefs.UserPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LoginViewModel : BaseViewModel() {

    fun login(username: String, password: String, onSuccess: () -> Unit) {
        if (username.isBlank() || password.isBlank()) {
            setError("用户名和密码不能为空")
            return
        }
        launchIo {
            val res = NetworkModule.authApi.login(
                mapOf("username" to username, "password" to password)
            )
            if (res.isSuccess && res.data != null) {
                UserPrefs.saveLogin(res.data)
                Log.d("LoginVM", "登录成功: userId=${res.data.userId}")
                AppEventBus.emit(AppEventBus.LoginStateChanged(res.data.userId))
                withContext(Dispatchers.Main) { onSuccess() }
            } else {
                setError(res.msg ?: "登录失败")
            }
        }
    }
}
