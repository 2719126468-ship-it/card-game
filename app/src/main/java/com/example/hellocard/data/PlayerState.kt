package com.example.hellocard.data

class PlayerState(val name: String) {
    var life = 8000
    val deck = ArrayDeque<Card>()
    val hand = mutableListOf<Card>()
    val field = mutableListOf<Card>()          // 怪兽区（最多 5）
    val spellZone = mutableListOf<Card>()      // 魔陷区（最多 5）
    var fieldSpell: Card? = null               // 场地魔法（1 格）
    var extraMonster: Card? = null             // 额外怪兽区（1 格）
    val graveyard = mutableListOf<Card>()
    val exile = mutableListOf<Card>()
    var normalSummoned = false

    fun draw(): Card? {
        val c = deck.removeFirstOrNull() ?: return null
        if (hand.size < 7) hand.add(c)
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

    fun isDefeated(): Boolean = life <= 0 || deck.isEmpty()
}
