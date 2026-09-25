package com.example.hellocard

/**
 * 效果字符串词法工具。
 *
 * cards.json 的效果字段同时存在新旧两套动词命名，且用 `;` 分隔多个效果、
 * 用 `+` 表示同一次触发内的组合效果。这里集中处理这两件事，供
 * EffectResolver（结算）与 EffectText（展示）共用，避免两边逻辑漂移。
 */
object EffectSyntax {

    /** 旧动词 → 当前实现动词 */
    val ACTION_ALIASES: Map<String, String> = mapOf(
        "destroy"       to "delete",
        "draw"          to "observe",
        "revive"        to "restore",
        "mill"          to "inspect",
        "lifesteal"     to "control",
        "opponent_draw" to "imitate"
    )

    fun alias(action: String): String = ACTION_ALIASES[action] ?: action

    /**
     * 按 `+` 拆分组合效果，但保留数值符号。
     *
     * `delete+observe:1`  → ["delete", "observe:1"]
     * `awareness:+500`    → ["awareness:+500"]   ← 这里的 `+` 是数值符号，不能拆
     */
    fun splitCombined(raw: String): List<String> {
        val out = mutableListOf<String>()
        val sb = StringBuilder()
        for (i in raw.indices) {
            val ch = raw[i]
            val isSign = ch == '+' && i > 0 && raw[i - 1] == ':'
            if (ch == '+' && !isSign) {
                out.add(sb.toString())
                sb.setLength(0)
            } else {
                sb.append(ch)
            }
        }
        out.add(sb.toString())
        return out
    }
}
