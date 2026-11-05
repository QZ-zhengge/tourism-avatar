package com.example.let_us_go.ui.pages.digital_stream

import com.example.let_us_go.core.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DigitalStreamViewModel : BaseViewModel() {

    private val _streams = MutableStateFlow(
        listOf(
            StreamInfo("s1", "灵山胜境 · 实时导览", "🎬", 1240),
            StreamInfo("s2", "西湖 · 雨后清晨", "🌊", 856),
            StreamInfo("s3", "故宫 · 雪中红墙", "🏛", 2350),
            StreamInfo("s4", "黄山 · 云海日出", "⛰", 1820),
            StreamInfo("s5", "鼓浪屿 · 落日余晖", "🌅", 642)
        )
    )
    val streams: StateFlow<List<StreamInfo>> = _streams.asStateFlow()

    fun startStream() {}
    fun selectStream(id: String) {}
}
