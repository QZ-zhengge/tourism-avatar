package com.example.let_us_go.ui.pages.mine

import androidx.lifecycle.viewModelScope
import com.example.let_us_go.core.base.BaseViewModel
import com.example.let_us_go.core.bus.AppEventBus
import com.example.let_us_go.data.model.User
import com.example.let_us_go.data.network.NetworkModule
import com.example.let_us_go.data.prefs.UserPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * 我的页 ViewModel
 */
class MineViewModel : BaseViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _provinceCount = MutableStateFlow(0)
    val provinceCount: StateFlow<Int> = _provinceCount.asStateFlow()

    private val _agingMode = MutableStateFlow(UserPrefs.agingMode())
    val agingMode: StateFlow<Boolean> = _agingMode.asStateFlow()

    init {
        viewModelScope.launch {
            UserPrefs.currentUserFlow().collect { u -> _user.value = u }
        }
    }

    fun loadAll() {
        loadProfile()
        loadProvinceCount()
    }

    private fun loadProfile() {
        launchIo(showLoading = false) {
            val uid = UserPrefs.userId()
            if (uid == 0L) return@launchIo
            val res = NetworkModule.authApi.profile(uid)
            if (res.isSuccess && res.data != null) {
                _user.value = res.data
                // ★ 不调 saveLogin——profile 接口不负责更新登录态
            }
        }
    }

    private fun loadProvinceCount() {
        launchIo(showLoading = false) {
            val uid = UserPrefs.userId() ?: 0L
            runCatching {
                if (uid == 0L) {
                    0
                } else {
                    val res = NetworkModule.checkinApi.getUserCheckins(userId = uid)
                    if (res.isSuccess && res.data != null) {
                        // 已点亮省份数 = 用户已点亮省份编号数组的长度
                        res.data.size
                    } else {
                        // 失败回退：本地记录
                        val json = UserPrefs.provinceVisitedFlow().firstOrNull()
                        org.json.JSONArray(json ?: "[]").length()
                    }
                }
            }.onSuccess { _provinceCount.value = it }
        }
    }

    fun setAgingMode(enabled: Boolean) {
        viewModelScope.launch {
            UserPrefs.setAgingMode(enabled)
            UserPrefs.setFontScale(if (enabled) 1.3f else 1.0f)
            _agingMode.value = enabled
            AppEventBus.emit(AppEventBus.FontScaleChanged(if (enabled) 1.3f else 1.0f))
        }
    }

    fun logout() {
        viewModelScope.launch {
            UserPrefs.logout()
            _user.value = null
            AppEventBus.emit(AppEventBus.LoginStateChanged(null))
        }
    }
}
