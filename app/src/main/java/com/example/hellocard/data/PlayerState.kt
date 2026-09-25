package com.example.hellocard.data

import com.example.hellocard.Dimens

class PlayerState(val name: String) {
    var life = Dimens.STARTING_LIFE
    val deck = ArrayDeque<Card>()
    val hand = mutableListOf<Card>()
    val field = mutableListOf<Card>()          // 怪兽区（最多 MAX_FIELD_SIZE）
    val spellZone = mutableListOf<Card>()      // 魔陷区（最多 MAX_SPELL_ZONE）
    var fieldSpell: Card? = null               // 场地魔法（1 格）
    var extraMonster: Card? = null             // 额外怪兽区（1 格）
    val graveyard = mutableListOf<Card>()
    val exile = mutableListOf<Card>()
    var normalSummoned = false

    /** 尝试抽卡但卡组为空时置位 → 用于判定卡组耗尽败北 */
    var deckOut = false

    fun draw(): Card? {
        val c = deck.removeFirstOrNull()
        if (c == null) {
            deckOut = true
            return null
        }
        if (hand.size < Dimens.MAX_HAND_SIZE) hand.add(c)
        else graveyard.add(c)   // 手牌溢出 → 送墓，避免卡牌凭空消失
        return c
    }

    fun resetTurn() {
        normalSummoned = false
        field.forEach {
            it.summonedThisTurn = false
            it.hasAttacked = false
        }
        extraMonster?.let {
            it.summonedThisTurn = false
            it.hasAttacked = false
        }
    }

    fun isDefeated(): Boolean = life <= 0 || deckOut
}
