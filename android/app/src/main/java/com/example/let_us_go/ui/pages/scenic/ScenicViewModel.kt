package com.example.let_us_go.ui.pages.scenic

import com.example.let_us_go.core.base.BaseViewModel
import com.example.let_us_go.data.network.NetworkModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ScenicViewModel : BaseViewModel() {

    private val _tags = MutableStateFlow(listOf("全部", "景点", "美食", "活动", "历史", "民俗"))
    val tags: StateFlow<List<String>> = _tags.asStateFlow()

    private val _nodes = MutableStateFlow(
        listOf(
            KGNode("1", "灵山胜境", "scenic"),
            KGNode("2", "素斋馆", "food"),
            KGNode("3", "梵宫演出", "event"),
            KGNode("4", "玄奘传说", "history"),
            KGNode("5", "九龙灌浴", "scenic"),
            KGNode("6", "五印坛城", "scenic"),
            KGNode("7", "禅意手作", "民俗")
        )
    )
    val nodes: StateFlow<List<KGNode>> = _nodes.asStateFlow()

    fun load() {
        launchIo(showLoading = false) {
            val res = NetworkModule.knowledgeGraphApi.overview(scenicId = "lingshan")
            // 简化：使用默认数据
        }
    }

    fun filterByTag(tag: String) {
        // 实际生产中调用 /knowledge-graph/filter
    }
}
