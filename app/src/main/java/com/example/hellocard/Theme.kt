package com.example.hellocard

import android.graphics.Color

/**
 * world.execute(me); 视觉体系
 * 深色底 + 青色主色 + 粉色强调 + 等宽字体
 */
object Theme {
    // 背景层
    val BG         = Color.parseColor("#05070a")
    val BG_STAGE   = Color.parseColor("#04060a")
    val BG_PANEL   = Color.parseColor("#070a0e")
    val BG_CARD    = Color.parseColor("#0c1219")
    val BG_ZONE    = Color.parseColor("#101620")

    // 边框
    val BORDER     = Color.parseColor("#101820")
    val BORDER_LIT = Color.parseColor("#1e2a36")
    val BORDER_ACC = Color.parseColor("#2c4a52")

    // 文字
    val FG         = Color.parseColor("#c8d2dc")
    val FG_DIM     = Color.parseColor("#8fa0b0")
    val FG_MUTE    = Color.parseColor("#5d6b78")

    // 强调色
    val ACCENT     = Color.parseColor("#57d7e8")   // 青
    val PINK       = Color.parseColor("#ff9db6")   // 粉
    val GREEN      = Color.parseColor("#7fe3c4")   // 绿
    val GOLD       = Color.parseColor("#ffd479")   // 金
    val RED        = Color.parseColor("#ff5a52")   // 红
    val ORANGE     = Color.parseColor("#e0a95f")   // 橙

    // 阵营色
    val AI_PURPLE  = Color.parseColor("#a855f7")   // Momo
    val HM_BLUE    = Color.parseColor("#4ecdc4")   // 人类
    val AI_BG      = Color.parseColor("#2b1b45")
    val HM_BG      = Color.parseColor("#162c45")

    // 卡牌类型底色
    val SPELL_BG   = Color.parseColor("#142a1a")
    val TRAP_BG    = Color.parseColor("#2a1414")
    val FIELD_BG   = Color.parseColor("#2a2414")
    val EXTRA_BG   = Color.parseColor("#160b1a")

    // 效果类型
    const val TYPE_MONOSPACE = "monospace"
    const val GOLD_HEX = "#ffd479"
}
