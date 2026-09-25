package com.example.hellocard.game

import com.example.hellocard.data.Card
import com.example.hellocard.data.PlayerState

enum class Phase { MAIN, BATTLE }

class GameState {
    val player = PlayerState("你")
    val opponent = PlayerState("Momo")
    var turn = 1
    var isPlayerTurn = true
    var phase = Phase.MAIN
    var gameOver = false
    var result = ""
    var selectedAttacker: Card? = null
    val battleLog = mutableListOf<String>()
    val effectResolver = EffectResolver { log(it) }

    fun init(playerDeck: List<Card>, opponentDeck: List<Card>,
             playerExtra: List<Card>, opponentExtra: List<Card>) {
        var uid = 1
        player.deck.addAll(playerDeck.map { it.copy(uid = uid++) }.shuffled())
        opponent.deck.addAll(opponentDeck.map { it.copy(uid = uid++) }.shuffled())
        // 额外卡组暂存在 exile 字段里作为待召唤池（简化）
        player.exile.addAll(playerExtra.map { it.copy(uid = uid++) })
        opponent.exile.addAll(opponentExtra.map { it.copy(uid = uid++) })
        repeat(5) { player.draw(); opponent.draw() }
        player.resetTurn()
        opponent.resetTurn()
        log("决斗开始！")
    }

    fun phaseName(): String = when (phase) {
        Phase.MAIN -> "主要阶段"
        Phase.BATTLE -> "战斗阶段"
    }

    // ============ 通常召唤 ============
    fun summon(p: PlayerState, card: Card, defense: Boolean): Boolean {
        if (p === player && phase != Phase.MAIN) return false
        if (p.field.size >= 5) return false
        if (p.normalSummoned) return false
        val tributes = when {
            card.level <= 4 -> 0
            card.level in 5..6 -> 1
            else -> 2
        }
        if (p.field.size < tributes) return false
        repeat(tributes) {
            val t = p.field.minByOrNull { it.atk }
            if (t != null) { p.field.remove(t); p.graveyard.add(t) }
        }
        if (!p.hand.remove(card)) return false
        card.isDefense = defense
        card.summonedThisTurn = true
        p.field.add(card)
        p.normalSummoned = true
        log("${p.name} 通常召唤「${card.name}」(${if (defense) "守备" else "攻击"})")
        val other = if (p === player) opponent else player
        effectResolver.resolveSummon(card, p, other)
        checkWin()
        return true
    }

    // ============ 额外怪兽召唤 ============
    fun canSpecialSummon(p: PlayerState, card: Card): String? {
        if (p === player && phase != Phase.MAIN) return "只能在主要阶段"
        if (p.extraMonster != null) return "额外怪兽区已被占用"
        val cost = when (card.extraType) {
            "aggregate" -> 2  // 聚合：2 只素材
            "resonate" -> 2   // 共鸣：2 只
            "overlay" -> 2    // 叠加：2 只
            "link" -> 2       // 链接：2 只
            else -> 2
        }
        if (p.field.size < cost) return "需要 $cost 只祭品"
        return null
    }

    fun specialSummon(p: PlayerState, card: Card): Boolean {
        if (canSpecialSummon(p, card) != null) return false
        val cost = 2
        repeat(cost) {
            val t = p.field.minByOrNull { it.atk }
            if (t != null) { p.field.remove(t); p.graveyard.add(t) }
        }
        p.exile.remove(card)
        card.isDefense = false
        card.summonedThisTurn = true
        p.extraMonster = card
        val typeName = when (card.extraType) {
            "aggregate" -> "聚合召唤"
            "resonate" -> "共鸣召唤"
            "overlay" -> "叠加召唤"
            "link" -> "链接召唤"
            else -> "特殊召唤"
        }
        log("${p.name} 发动【$typeName】！「${card.name}」登场！")
        val other = if (p === player) opponent else player
        effectResolver.resolveSummon(card, p, other)
        checkWin()
        return true
    }

    // ============ 魔法/陷阱 ============
    fun activateSpell(p: PlayerState, card: Card): Boolean {
        if (p === player && phase != Phase.MAIN) return false
        if (!p.hand.remove(card)) return false
        log("${p.name} 发动魔法「${card.name}」")
        val other = if (p === player) opponent else player
        // 魔法卡效果立即结算（效果已含触发时机，用 summon 触发）
        effectResolver.resolveSummon(card, p, other)
        // 通常魔法：结算后进墓地
        p.graveyard.add(card)
        log("  → 「${card.name}」送入墓地")
        checkWin()
        return true
    }

    fun setTrap(p: PlayerState, card: Card): Boolean {
        if (p === player && phase != Phase.MAIN) return false
        if (p.spellZone.size >= 5) return false
        if (!p.hand.remove(card)) return false
        p.spellZone.add(card)
        log("${p.name} 盖放陷阱「${card.name}」")
        checkWin()
        return true
    }

    fun activateField(p: PlayerState, card: Card): Boolean {
        if (p === player && phase != Phase.MAIN) return false
        if (p.fieldSpell != null) return false
        if (!p.hand.remove(card)) return false
        p.fieldSpell = card
        log("${p.name} 发动场地魔法「${card.name}」")
        val other = if (p === player) opponent else player
        effectResolver.resolveSummon(card, p, other)
        checkWin()
        return true
    }

    // ============ 攻守切换 ============
    fun togglePosition(card: Card): Boolean {
        if (card !in player.field) return false
        if (card.summonedThisTurn) return false
        if (phase != Phase.MAIN) return false
        card.isDefense = !card.isDefense
        log("「${card.name}」切换为${if (card.isDefense) "守备" else "攻击"}表示")
        return true
    }

    fun canPlayerAttack(attacker: Card): String? {
        if (phase != Phase.BATTLE) return "战斗阶段才能攻击"
        if (attacker.isDefense) return "守备表示不能攻击"
        if (attacker.hasAttacked) return "该怪兽本回合已攻击过"
        return null
    }

    private fun performBattle(attacker: Card, target: Card) {
        val attackerIsPlayer = attacker in player.field || attacker == player.extraMonster
        val attackerOwner = if (attackerIsPlayer) player else opponent
        val defenderOwner = if (attackerIsPlayer) opponent else player

        log("「${attacker.name}」攻击「${target.name}」")
        effectResolver.resolveAttack(attacker, attackerOwner, defenderOwner)

        val attackerDies: Boolean
        val targetDies: Boolean
        if (target.isDefense) {
            targetDies = attacker.atk > target.def
            attackerDies = attacker.atk < target.def
        } else {
            targetDies = attacker.atk >= target.atk
            attackerDies = target.atk >= attacker.atk
        }

        if (targetDies) {
            if (defenderOwner.extraMonster == target) {
                defenderOwner.extraMonster = null
            } else {
                defenderOwner.field.remove(target)
            }
            defenderOwner.graveyard.add(target)
            log("  → 「${target.name}」被破坏")
            effectResolver.resolveDestroy(target, defenderOwner, attackerOwner)
        }
        if (attackerDies) {
            if (attackerOwner.extraMonster == attacker) {
                attackerOwner.extraMonster = null
            } else {
                attackerOwner.field.remove(attacker)
            }
            attackerOwner.graveyard.add(attacker)
            log("  → 「${attacker.name}」被破坏")
            effectResolver.resolveDestroy(attacker, attackerOwner, defenderOwner)
        }

        attacker.hasAttacked = true
        if (attackerIsPlayer) selectedAttacker = null
        checkWin()
    }

    fun attack(attacker: Card, target: Card) {
        val attackerInField = attacker in player.field || attacker == player.extraMonster
        if (!attackerInField) return
        val targetInField = target in opponent.field || target == opponent.extraMonster
        if (!targetInField) return
        if (canPlayerAttack(attacker) != null) return
        performBattle(attacker, target)
    }

    fun directAttack(attacker: Card) {
        val inField = attacker in player.field || attacker == player.extraMonster
        if (!inField) return
        if (canPlayerAttack(attacker) != null) return
        opponent.life -= attacker.atk
        log("「${attacker.name}」直接攻击，造成 ${attacker.atk} 伤害")
        effectResolver.resolveAttack(attacker, player, opponent)
        attacker.hasAttacked = true
        selectedAttacker = null
        checkWin()
    }

    fun advancePhase() {
        if (!isPlayerTurn || gameOver) return
        if (phase == Phase.MAIN) {
            phase = Phase.BATTLE
            log("--- 进入战斗阶段 ---")
        }
    }

    fun endTurn() {
        if (gameOver) return
        selectedAttacker = null
        isPlayerTurn = false
        phase = Phase.MAIN
        opponent.resetTurn()
        opponent.draw()
        aiTakeTurn()
        if (!gameOver) {
            isPlayerTurn = true
            turn++
            player.resetTurn()
            player.draw()
            phase = Phase.MAIN
            log("--- 回合 $turn ---")
        }
        checkWin()
    }

    private fun aiTakeTurn() {
        // 优先额外怪兽召唤
        if (opponent.extraMonster == null && opponent.exile.isNotEmpty() && opponent.field.size >= 2) {
            val extra = opponent.exile.maxByOrNull { it.atk }
            if (extra != null) specialSummon(opponent, extra)
        }
        // 通常召唤
        val sorted = opponent.hand
            .filter { it.cardType == "monster" }
            .sortedByDescending { it.level }
            .toList()
        for (card in sorted) {
            if (opponent.normalSummoned) break
            if (opponent.field.size >= 5) break
            val tributes = when {
                card.level <= 4 -> 0
                card.level in 5..6 -> 1
                else -> 2
            }
            if (opponent.field.size < tributes) continue
            repeat(tributes) {
                val t = opponent.field.minByOrNull { it.atk }
                if (t != null) { opponent.field.remove(t); opponent.graveyard.add(t) }
            }
            if (opponent.hand.remove(card)) {
                val defense = opponent.life < 3000 && Math.random() < 0.4
                card.isDefense = defense
                card.summonedThisTurn = true
                opponent.field.add(card)
                opponent.normalSummoned = true
                log("${opponent.name} 召唤「${card.name}」(${if (defense) "守备" else "攻击"})")
                effectResolver.resolveSummon(card, opponent, player)
            }
        }
        // 发动魔法
        opponent.hand.filter { it.cardType == "spell" }.toList().forEach { sp ->
            if (opponent.spellZone.size < 5) activateSpell(opponent, sp)
        }
        // 攻击
        val attackers = (opponent.field.filter { !it.isDefense && !it.hasAttacked } +
                listOfNotNull(opponent.extraMonster?.takeIf { !it.hasAttacked })).toList()
        for (attacker in attackers) {
            val stillAlive = attacker in opponent.field || attacker == opponent.extraMonster
            if (!stillAlive) continue
            val playerMonsters = player.field + listOfNotNull(player.extraMonster)
            if (playerMonsters.isEmpty()) {
                player.life -= attacker.atk
                log("「${attacker.name}」直接攻击你，造成 ${attacker.atk} 伤害")
                effectResolver.resolveAttack(attacker, opponent, player)
                attacker.hasAttacked = true
            } else {
                val target = player.field.filter { !it.isDefense }.minByOrNull { it.atk }
                    ?: playerMonsters.minByOrNull { it.def }
                    ?: continue
                performBattle(attacker, target)
            }
        }
    }

    fun checkWin() {
        if (gameOver) return
        if (player.life <= 0) { gameOver = true; result = "💀 你输了" }
        else if (opponent.life <= 0) { gameOver = true; result = "🎉 你赢了！" }
        else if (player.deck.isEmpty()) { gameOver = true; result = "卡组耗尽，你输了" }
        else if (opponent.deck.isEmpty()) { gameOver = true; result = "对方卡组耗尽，你赢了！" }
    }

    fun log(s: String) {
        battleLog.add(s)
        if (battleLog.size > 100) battleLog.removeAt(0)
    }
}
