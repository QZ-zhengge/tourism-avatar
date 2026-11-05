package com.example.let_us_go.ui.pages.mine

import android.util.Log
import com.example.let_us_go.core.base.BaseViewModel
import com.example.let_us_go.data.api.ChangePasswordRequest
import com.example.let_us_go.data.api.UpdateProfileRequest
import com.example.let_us_go.data.api.UserProfileData
import com.example.let_us_go.data.network.NetworkModule
import com.example.let_us_go.data.prefs.UserPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 个人资料编辑 ViewModel
 *
 * 处理获取/修改个人信息和修改密码
 */
class ProfileEditViewModel : BaseViewModel() {

    companion object {
        private const val TAG = "ProfileEditVM"
    }

    /** 用户个人资料 */
    private val _profile = MutableStateFlow<UserProfileData?>(null)
    val profile: StateFlow<UserProfileData?> = _profile.asStateFlow()

    /** 操作成功消息 */
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    /** 错误消息 */
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /**
     * 加载用户个人资料
     */
    fun loadProfile() {
        val userId = UserPrefs.userIdSync() ?: return
        launchIo(showLoading = true) {
            try {
                val res = NetworkModule.userProfileApi.getProfile(userId)
                if (res.isSuccess && res.data != null) {
                    _profile.value = res.data
                    Log.d(TAG, "加载成功: ${res.data}")
                } else {
                    _errorMessage.value = res.msg ?: "加载失败"
                    Log.w(TAG, "加载失败: ${res.msg}")
                }
            } catch (e: Throwable) {
                Log.e(TAG, "加载异常: ${e.message}", e)
                _errorMessage.value = "网络错误: ${e.message}"
            }
        }
    }

    /**
     * 修改个人信息
     *
     * @param nickname 新昵称（null 不修改）
     * @param email 新邮箱（null 不修改）
     * @param phoneNumber 新手机号（null 不修改）
     */
    fun updateProfile(
        nickname: String? = null,
        email: String? = null,
        phoneNumber: String? = null
    ) {
        val userId = UserPrefs.userIdSync() ?: return
        launchIo(showLoading = true) {
            try {
                val res = NetworkModule.userProfileApi.updateProfile(
                    UpdateProfileRequest(
                        user_id = userId,
                        nickname = nickname,
                        email = email,
                        phone_number = phoneNumber
                    )
                )
                if (res.isSuccess && res.data != null) {
                    _profile.value = res.data
                    _successMessage.value = "修改成功"
                    Log.d(TAG, "修改成功: ${res.data}")
                } else {
                    _errorMessage.value = res.msg ?: "修改失败"
                    Log.w(TAG, "修改失败: ${res.msg}")
                }
            } catch (e: Throwable) {
                Log.e(TAG, "修改异常: ${e.message}", e)
                _errorMessage.value = "网络错误: ${e.message}"
            }
        }
    }

    /**
     * 修改密码
     *
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     * @param confirmPassword 确认新密码
     */
    fun changePassword(
        oldPassword: String,
        newPassword: String,
        confirmPassword: String
    ) {
        val userId = UserPrefs.userIdSync() ?: return

        // 前端校验
        if (oldPassword.isBlank()) {
            _errorMessage.value = "请输入旧密码"
            return
        }
        if (newPassword.length < 6) {
            _errorMessage.value = "新密码至少6位"
            return
        }
        if (newPassword != confirmPassword) {
            _errorMessage.value = "两次输入的新密码不一致"
            return
        }

        launchIo(showLoading = true) {
            try {
                val res = NetworkModule.userProfileApi.changePassword(
                    ChangePasswordRequest(
                        user_id = userId,
                        old_password = oldPassword,
                        new_password = newPassword,
                        confirm_password = confirmPassword
                    )
                )
                if (res.isSuccess) {
                    _successMessage.value = "密码修改成功"
                    Log.d(TAG, "密码修改成功")
                } else {
                    _errorMessage.value = res.msg ?: "密码修改失败"
                    Log.w(TAG, "密码修改失败: ${res.msg}")
                }
            } catch (e: Throwable) {
                Log.e(TAG, "密码修改异常: ${e.message}", e)
                _errorMessage.value = "网络错误: ${e.message}"
            }
        }
    }

    /**
     * 清除成功消息
     */
    fun clearSuccess() {
        _successMessage.value = null
    }

    /**
     * 清除用户错误消息
     */
    fun clearUserError() {
        _errorMessage.value = null
    }
}
