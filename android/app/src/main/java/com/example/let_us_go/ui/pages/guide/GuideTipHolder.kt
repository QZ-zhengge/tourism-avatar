package com.example.let_us_go.ui.pages.guide

import com.example.let_us_go.data.model.GuideTip

/**
 * 简单数据持有器，用于在列表页和详情页之间传递 GuideTip 数据
 */
object GuideTipHolder {
    var currentTip: GuideTip? = null
}