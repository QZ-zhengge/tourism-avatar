package com.example.let_us_go.data.prefs

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.File

/**
 * 登录状态文件管理器——全 App 唯一的登录状态源。
 *
 * 逻辑极简：
 * - 文件不存在 → 未登录
 * - 文件存在 → 已登录，内容包含 userId 和 username
 * - 登录时写入，退出时删除
 *
 * 所有需要 userId 的地方直接调用 [LoginStateFile.read]，不走任何缓存/Flow/DataStore。
 */
object LoginStateFile {

    private const val TAG = "LoginStateFile"
    private const val FILE_NAME = "login_state.json"

    /** 读取登录状态 */
    fun read(context: Context? = null): LoginState? {
        val ctx = context ?: run {
            Log.w(TAG, "read: context=null，返回 null")
            return null
        }
        return try {
            val file = File(ctx.filesDir, FILE_NAME)
            if (!file.exists()) {
                Log.w(TAG, "read: 文件不存在 (${file.absolutePath})，返回 null")
                return null
            }
            val json = file.readText(Charsets.UTF_8)
            if (json.isBlank()) return null
            val obj = JSONObject(json)
            val uid = obj.optLong("userId", 0L)
            val uname = obj.optString("username", "")
            Log.d(TAG, "read: userId=$uid, username=$uname")
            if (uid > 0L) LoginState(uid, uname) else null
        } catch (e: Throwable) {
            Log.w(TAG, "读取登录状态失败: ${e.message}")
            null
        }
    }

    /** 同步获取 userId（未登录返回 0） */
    fun readUserId(context: Context? = null): Long = read(context)?.userId ?: 0L

    /** 登录时写入 */
    fun write(context: Context, userId: Long, username: String) {
        try {
            val obj = JSONObject().apply {
                put("userId", userId)
                put("username", username)
            }
            File(context.filesDir, FILE_NAME).writeText(obj.toString(), Charsets.UTF_8)
            Log.d(TAG, "写入登录状态: userId=$userId, username=$username")
        } catch (e: Throwable) {
            Log.e(TAG, "写入登录状态失败: ${e.message}")
        }
    }

    /** 退出登录时删除文件 */
    fun clear(context: Context) {
        try {
            val file = File(context.filesDir, FILE_NAME)
            if (file.exists()) file.delete()
            Log.d(TAG, "已清除登录状态文件")
        } catch (e: Throwable) {
            Log.e(TAG, "清除登录状态失败: ${e.message}")
        }
    }
}

data class LoginState(val userId: Long, val username: String)
