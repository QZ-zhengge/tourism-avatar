package com.example.let_us_go.core.bus

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 应用级事件总线
 *
 * 替代 uni-app 的 `uni.$emit` / `uni.$on` / `uni.$off`。
 *
 * 用法：
 * ```
 * // 发送
 * AppEventBus.emit(EventItinerary.Refresh)
 *
 * // 订阅（任何生命周期内，使用 ViewModel.viewModelScope）
 * AppEventBus.events.collect { event ->
 *     when (event) {
 *         is EventItinerary.Refresh -> load()
 *     }
 * }
 * ```
 *
 * 设计要点：
 * - 跨 Activity / Fragment / Composable 共享
 * - replay = 0：不重放历史事件（避免误消费）
 * - extraBufferCapacity = 16：发射时若无订阅者则缓存
 */
object AppEventBus {

    sealed interface Event

    // ─────────────── 业务事件 ───────────────

    /** 旅友圈：刷新动态流 */
    object MomentsRefresh : Event

    /** 旅友圈：发帖成功 */
    data class PostCreated(val postId: Long) : Event

    /** 旅友圈：评论更新 */
    data class CommentChanged(val postId: Long) : Event

    /** 行程：AI 添加了新条目 */
    object ItineraryChanged : Event

    /** 登录态变化 */
    data class LoginStateChanged(val userId: Long?) : Event

    /** 主题/字号变化（适老化切换） */
    data class FontScaleChanged(val scale: Float) : Event

    /** 数字人：与数字人对话产生的行程项 */
    data class DhBookingCard(val title: String, val summary: String) : Event

    /** 旅友圈：选中草稿恢复 */
    data class MomentsDraftRestore(val draftJson: String) : Event

    /** Mine：点亮省份更新 */
    object ProvinceChanged : Event

    /** 旅友圈：点赞更新 */
    data class PostLikeChanged(val postId: Long, val liked: Boolean, val count: Int) : Event

    /** 全局：显示错误 Toast */
    data class ShowError(val msg: String) : Event

    /** 全局：切换到数字人 tab */
    object GotoDigitalHuman : Event

    /** 唤醒词触发 */
    data class WakeUpDetected(val keyword: String) : Event

    // ─────────────── 内部实现 ───────────────

    private val _events = MutableSharedFlow<Event>(
        replay = 0,
        extraBufferCapacity = 16
    )
    val events: SharedFlow<Event> = _events.asSharedFlow()

    fun emit(event: Event) {
        // tryEmit 在 buffer 满时返回 false，事件丢失但不影响主流程
        val ok = _events.tryEmit(event)
        if (!ok) {
            // 缓冲满了，使用 emit 协程挂起（不常用，安全兜底）
            @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
            kotlinx.coroutines.runBlocking {
                _events.emit(event)
            }
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun clear() {
        _events.resetReplayCache()
    }
}
