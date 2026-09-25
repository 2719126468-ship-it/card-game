package com.example.hellocard.game

import com.example.hellocard.Dimens
import com.example.hellocard.EffectSyntax
import com.example.hellocard.data.Card
import com.example.hellocard.data.PlayerState

/**
 * 效果解析器（world.execute(me); 参数体系）
 *
 * 格式：`[触发时机:]类型:参数;...`
 * 触发时机：summon（默认）/ attack / destroy
 * 效果类型：
 *   presence   降低对方存在感（原 damage）
 *   awareness  恢复自己觉察（原 heal）
 *   observe    自己抽卡（原 draw）
 *   imitate    对方抽卡（原 opponent_draw）
 *   delete     删除对方 ATK 最低怪兽（原 destroy）
 *   delete_strong 删除对方 ATK 最高怪兽
 *   restore    从墓地复活 4 星以下（原 revive）
 *   control    吸取控制（原 lifesteal）
 *   inspect    削对方卡组（原 mill）
 */
class EffectResolver(private val log: (String) -> Unit) {

    private val TRIGGERS = setOf("summon", "attack", "destroy")

    fun resolveSummon(card: Card, owner: PlayerState, opponent: PlayerState) =
        resolve(card, "summon", owner, opponent)

    fun resolveAttack(card: Card, owner: PlayerState, opponent: PlayerState) =
        resolve(card, "attack", owner, opponent)

    fun resolveDestroy(card: Card, owner: PlayerState, opponent: PlayerState) =
        resolve(card, "destroy", owner, opponent)

    private fun resolve(card: Card, trigger: String, owner: PlayerState, opponent: PlayerState) {
        if (card.effect.isBlank()) return
        card.effect.split(";").forEach { raw ->
            // "delete+observe:1" 这类组合要拆开，但 "awareness:+500" 的 + 是数值符号
            EffectSyntax.splitCombined(raw.trim()).forEach { sub ->
                runOne(sub.trim(), trigger, card, owner, opponent)
            }
        }
    }

    private fun runOne(raw: String, trigger: String, card: Card,
                       owner: PlayerState, opponent: PlayerState) {
        if (raw.isEmpty()) return
        val parts = raw.split(":")
        if (parts.isEmpty() || parts[0].isEmpty()) return

        val hasTrigger = parts[0] in TRIGGERS
        val actualTrigger = if (hasTrigger) parts[0] else "summon"
        val actionParts = if (hasTrigger) parts.drop(1) else parts
        if (actualTrigger != trigger) return

        val rawAction = actionParts.getOrNull(0) ?: return
        val action = EffectSyntax.alias(rawAction)
        val valueStr = actionParts.getOrNull(1)?.trim() ?: ""
        val value = valueStr.replace("+", "").replace("-", "").toIntOrNull() ?: 0
        val isNegative = valueStr.startsWith("-")

        when (action) {
            "presence" -> {
                opponent.life -= value
                log("【${card.name}】presence ${if (isNegative) "-" else "+"}$value")
            }
            "awareness" -> {
                owner.life += value
                log("【${card.name}】awareness +$value")
            }
            "observe" -> {
                repeat(value.coerceAtLeast(1)) { owner.draw() }
                log("【${card.name}】observe × $value")
            }
            "imitate" -> {
                repeat(value.coerceAtLeast(1)) { opponent.draw() }
                log("【${card.name}】imitate × $value")
            }
            "delete" -> {
                val t = opponent.field.minByOrNull { it.atk }
                if (t != null) {
                    opponent.field.remove(t)
                    opponent.graveyard.add(t)
                    log("【${card.name}】delete 「${t.name}」")
                }
            }
            "delete_strong" -> {
                val t = opponent.field.maxByOrNull { it.atk }
                if (t != null) {
                    opponent.field.remove(t)
                    opponent.graveyard.add(t)
                    log("【${card.name}】delete_strong 「${t.name}」")
                }
            }
            "restore" -> {
                val t = owner.graveyard.filter { it.level <= Dimens.RESTORE_MAX_LEVEL }.randomOrNull()
                if (t != null && owner.field.size < Dimens.MAX_FIELD_SIZE) {
                    owner.graveyard.remove(t)
                    owner.field.add(t)
                    log("【${card.name}】restore 「${t.name}」")
                }
            }
            "control" -> {
                opponent.life -= value
                owner.life += value
                log("【${card.name}】control +$value")
            }
            "inspect" -> {
                repeat(value.coerceAtLeast(1)) {
                    val c = opponent.deck.removeFirstOrNull()
                    if (c != null) opponent.graveyard.add(c)
                }
                log("【${card.name}】inspect × $value")
            }
            "damage" -> {
                opponent.life -= value
                log("【${card.name}】damage $value")
            }
            "heal" -> {
                owner.life += value
                log("【${card.name}】heal +$value")
            }
        }
    }
}
