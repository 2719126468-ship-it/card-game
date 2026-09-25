package com.example.hellocard

/**
 * 把 cards.json 里的效果字符串翻译成中文
 * 例：summon:delete;destroy:observe:2
 *     → 【召唤时】删除（破坏对方 ATK 最低怪兽）；【被破坏时】观察 ×2（抽卡）
 */
object EffectText {

    fun toChinese(effect: String): String {
        if (effect.isBlank()) return "无效果"
        val sb = StringBuilder()
        effect.split(";").forEach { raw ->
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return@forEach

            val trigger = when {
                trimmed.startsWith("summon:")  -> "【召唤时】"
                trimmed.startsWith("attack:")  -> "【攻击时】"
                trimmed.startsWith("destroy:") -> "【被破坏时】"
                else -> ""
            }
            val body = trimmed
                .removePrefix("summon:")
                .removePrefix("attack:")
                .removePrefix("destroy:")

            // delete+observe:1 这类组合
            val plusParts = body.split("+")
            var first = true
            plusParts.forEach { sub ->
                if (!first) sb.append("，")
                first = false
                sb.append(trigger).append(translateOne(sub.trim()))
            }
            sb.append("；")
        }
        val out = sb.toString().trimEnd('；')
        return if (out.isEmpty()) "无效果" else out
    }

    private fun translateOne(raw: String): String {
        if (raw.isEmpty()) return ""
        val parts = raw.split(":")
        val action = parts.getOrNull(0) ?: return raw
        val valueStr = parts.getOrNull(1)?.trim() ?: ""
        val value = valueStr.replace("+", "").replace("-", "").toIntOrNull() ?: 0
        val neg = valueStr.startsWith("-")

        return when (action) {
            "presence"      -> "存在感 ${if (neg) "-" else "+"}$value（削对方 LP）"
            "awareness"     -> "觉察 +$value（恢复 LP）"
            "observe"       -> "观察 ×$value（抽卡）"
            "imitate"       -> "模仿 ×$value（对方抽卡）"
            "delete"        -> "删除（破坏对方 ATK 最低怪兽）"
            "delete_strong" -> "删除·强（破坏对方 ATK 最高怪兽）"
            "restore"       -> "恢复（复活墓地 4 星以下）"
            "control"       -> "控制 +$value（吸取 LP）"
            "inspect"       -> "检查 ×$value（削对方卡组）"
            else            -> raw
        }
    }

    /** 卡牌类型显示 */
    fun typeName(card: com.example.hellocard.data.Card): String = when (card.cardType) {
        "monster" -> {
            val extra = when (card.extraType) {
                "aggregate" -> " [聚合]"
                "resonate"  -> " [共鸣]"
                "overlay"   -> " [叠加]"
                "link"      -> " [链接]"
                else        -> ""
            }
            "怪兽$extra"
        }
        "spell" -> "通常魔法"
        "trap"  -> "陷阱卡"
        "field" -> "场地魔法"
        else    -> "未知"
    }
}
