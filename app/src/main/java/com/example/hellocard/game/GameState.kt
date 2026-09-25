package com.example.hellocard.game

import com.example.hellocard.Dimens
import com.example.hellocard.data.Card
import com.example.hellocard.data.PlayerState

enum class Phase { MAIN, BATTLE }

/** AI 的单个行动步骤 */
data class AIStep(
    val kind: String,               // extra / summon / spell / field / attack / direct
    val sourceCard: Card? = null,
    val targetCard: Card? = null,
    val defense: Boolean = false
)

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
        player.exile.addAll(playerExtra.map { it.copy(uid = uid++) })
        opponent.exile.addAll(opponentExtra.map { it.copy(uid = uid++) })
        repeat(Dimens.INITIAL_DRAW_COUNT) { player.draw(); opponent.draw() }
        player.resetTurn()
        opponent.resetTurn()
        log("决斗开始！")
    }

    fun phaseName(): String = when (phase) {
        Phase.MAIN -> "主要阶段"
        Phase.BATTLE -> "战斗阶段"
    }

    /** 星级 → 通常召唤所需祭品数（全局唯一实现，UI / AI 共用） */
    fun tributeCountFor(card: Card): Int = when {
        card.level < Dimens.TRIBUTE_THRESHOLD_5_6 -> 0
        card.level < Dimens.TRIBUTE_THRESHOLD_7 -> 1
        else -> Dimens.SPECIAL_SUMMON_TRIBUTES
    }

    // ============ 玩家操作 ============

    fun summon(p: PlayerState, card: Card, defense: Boolean): Boolean {
        if (p === player && phase != Phase.MAIN) return false
        if (p.field.size >= Dimens.MAX_FIELD_SIZE) return false
        if (p.normalSummoned) return false
        val tributes = tributeCountFor(card)
        if (p.field.size < tributes) return false
        // 先确认卡在手牌，避免无效召唤白吃祭品
        if (card !in p.hand) return false
        repeat(tributes) {
            val t = p.field.minByOrNull { it.atk }
            if (t != null) { p.field.remove(t); p.graveyard.add(t) }
        }
        p.hand.remove(card)
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

    fun canSpecialSummon(p: PlayerState, card: Card): String? {
        if (p === player && phase != Phase.MAIN) return "只能在主要阶段"
        if (p.extraMonster != null) return "额外怪兽区已被占用"
        if (p.field.size < Dimens.SPECIAL_SUMMON_TRIBUTES) return "需要 ${Dimens.SPECIAL_SUMMON_TRIBUTES} 只祭品"
        return null
    }

    fun specialSummon(p: PlayerState, card: Card): Boolean {
        if (canSpecialSummon(p, card) != null) return false
        // 先确认卡在额外卡组，避免无效召唤白吃祭品
        if (card !in p.exile) return false
        repeat(Dimens.SPECIAL_SUMMON_TRIBUTES) {
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

    fun activateSpell(p: PlayerState, card: Card): Boolean {
        if (p === player && phase != Phase.MAIN) return false
        if (!p.hand.remove(card)) return false
        log("${p.name} 发动魔法「${card.name}」")
        val other = if (p === player) opponent else player
        effectResolver.resolveSummon(card, p, other)
        p.graveyard.add(card)
        log("  → 「${card.name}」送入墓地")
        checkWin()
        return true
    }

    fun setTrap(p: PlayerState, card: Card): Boolean {
        if (p === player && phase != Phase.MAIN) return false
        if (p.spellZone.size >= Dimens.MAX_SPELL_ZONE) return false
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
            // 攻击表示互殴：攻击力低的一方承受差值战斗伤害
            val diff = attacker.atk - target.atk
            if (diff > 0) {
                defenderOwner.life -= diff
                log("  → 战斗伤害 $diff")
            } else if (diff < 0) {
                attackerOwner.life -= -diff
                log("  → 战斗伤害 ${-diff}")
            }
        }

        if (targetDies) {
            if (defenderOwner.extraMonster == target) defenderOwner.extraMonster = null
            else defenderOwner.field.remove(target)
            defenderOwner.graveyard.add(target)
            log("  → 「${target.name}」被破坏")
            effectResolver.resolveDestroy(target, defenderOwner, attackerOwner)
        }
        if (attackerDies) {
            if (attackerOwner.extraMonster == attacker) attackerOwner.extraMonster = null
            else attackerOwner.field.remove(attacker)
            attackerOwner.graveyard.add(attacker)
            log("  → 「${attacker.name}」被破坏")
            effectResolver.resolveDestroy(attacker, attackerOwner, defenderOwner)
        }
        attacker.hasAttacked = true
        if (attackerIsPlayer) selectedAttacker = null
        checkWin()
    }

    fun attack(attacker: Card, target: Card) {
        val aIn = attacker in player.field || attacker == player.extraMonster
        if (!aIn) return
        val tIn = target in opponent.field || target == opponent.extraMonster
        if (!tIn) return
        if (canPlayerAttack(attacker) != null) return
        performBattle(attacker, target)
    }

    fun directAttack(attacker: Card) {
        val inF = attacker in player.field || attacker == player.extraMonster
        if (!inF) return
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

    // ============ AI 回合：分步骤系统 ============

    /** 玩家结束回合 → 切换到 AI 回合，但**不执行** AI 行动 */
    fun startAITurn() {
        if (gameOver) return
        selectedAttacker = null
        isPlayerTurn = false
        phase = Phase.MAIN
        opponent.resetTurn()
        opponent.draw()
        log("--- Momo 的回合 ---")
        checkWin()
    }

    /**
     * 规划 AI 的全部行动。
     * 只规划"做什么"，不真的执行。执行交给 executeAIStep()。
     */
    fun planAITurn(): List<AIStep> {
        val steps = mutableListOf<AIStep>()
        val ai = opponent
        val pl = player

        // 1. 额外怪兽召唤（第一优先）
        if (ai.extraMonster == null && ai.exile.isNotEmpty() &&
            ai.field.size >= Dimens.SPECIAL_SUMMON_TRIBUTES) {
            val extra = ai.exile.maxByOrNull { it.atk }
            if (extra != null) steps.add(AIStep("extra", sourceCard = extra))
        }

        // 2. 通常召唤（每回合一次）
        if (!ai.normalSummoned && ai.field.size < Dimens.MAX_FIELD_SIZE) {
            val sorted = ai.hand
                .filter { it.cardType == "monster" }
                .sortedByDescending { it.level }
            for (card in sorted) {
                val tributes = tributeCountFor(card)
                if (ai.field.size < tributes) continue
                if (ai.field.size - tributes >= Dimens.MAX_FIELD_SIZE) continue
                val defense = ai.life < Dimens.AI_LOW_LIFE_THRESHOLD &&
                    Math.random() < Dimens.AI_DEFENSE_CHANCE
                steps.add(AIStep("summon", sourceCard = card, defense = defense))
                break
            }
        }

        // 3. 发动魔法
        val spells = ai.hand.filter { it.cardType == "spell" }.toList()
        for (sp in spells) {
            steps.add(AIStep("spell", sourceCard = sp))
        }

        // 4. 发动场地魔法
        if (ai.fieldSpell == null) {
            val field = ai.hand.firstOrNull { it.cardType == "field" }
            if (field != null) steps.add(AIStep("field", sourceCard = field))
        }

        // 5. 盖放陷阱
        val traps = ai.hand.filter { it.cardType == "trap" }.toList()
        for (tr in traps) {
            if (ai.spellZone.size >= Dimens.MAX_SPELL_ZONE) break
            steps.add(AIStep("spell", sourceCard = tr))
        }

        // 6. 攻击（只打能击破的目标，避免自杀式攻击）
        val attackers = (ai.field.filter { !it.isDefense && !it.hasAttacked } +
                listOfNotNull(ai.extraMonster?.takeIf { !it.hasAttacked })).toList()
        for (attacker in attackers) {
            val plMonsters = pl.field + listOfNotNull(pl.extraMonster)
            if (plMonsters.isEmpty()) {
                steps.add(AIStep("direct", sourceCard = attacker))
                continue
            }
            val breakable = plMonsters.filter { t ->
                if (t.isDefense) attacker.atk > t.def else attacker.atk > t.atk
            }
            val target = breakable.minByOrNull { if (it.isDefense) it.def else it.atk }
                ?: continue
            steps.add(AIStep("attack", sourceCard = attacker, targetCard = target))
        }

        return steps
    }

    /** 执行单步 AI 行动（由 MainActivity 播放器调用） */
    fun executeAIStep(step: AIStep) {
        val ai = opponent
        val pl = player
        when (step.kind) {
            "extra" -> {
                val card = step.sourceCard ?: return
                specialSummon(ai, card)
            }
            "summon" -> {
                val card = step.sourceCard ?: return
                summon(ai, card, step.defense)
            }
            "spell" -> {
                val card = step.sourceCard ?: return
                when (card.cardType) {
                    "spell" -> activateSpell(ai, card)
                    "trap" -> setTrap(ai, card)
                    else -> activateSpell(ai, card)
                }
            }
            "field" -> {
                val card = step.sourceCard ?: return
                activateField(ai, card)
            }
            "attack" -> {
                val a = step.sourceCard ?: return
                // 攻击者还在吗？
                if (a !in ai.field && a != ai.extraMonster) return
                // 实时重选目标
                val plMonsters = pl.field + listOfNotNull(pl.extraMonster)
                val target = step.targetCard?.takeIf { it in pl.field || it == pl.extraMonster }
                    ?: pl.field.filter { !it.isDefense }.minByOrNull { it.atk }
                    ?: plMonsters.minByOrNull { it.def }
                    ?: return
                performBattle(a, target)
            }
            "direct" -> {
                val a = step.sourceCard ?: return
                if (a !in ai.field && a != ai.extraMonster) return
                pl.life -= a.atk
                log("「${a.name}」直接攻击你，造成 ${a.atk} 伤害")
                effectResolver.resolveAttack(a, ai, pl)
                a.hasAttacked = true
                checkWin()
            }
        }
    }

    /** AI 回合结束 → 回到玩家回合 */
    fun endAITurn() {
        if (gameOver) return
        isPlayerTurn = true
        turn++
        player.resetTurn()
        player.draw()
        phase = Phase.MAIN
        log("--- 回合 $turn · 你的回合 ---")
        checkWin()
    }

    fun checkWin() {
        if (gameOver) return
        if (player.life <= 0) { gameOver = true; result = "💀 你输了" }
        else if (opponent.life <= 0) { gameOver = true; result = "🎉 你赢了！" }
        // 卡组耗尽：只有"该抽卡却抽不到"才算败北，而非卡组刚变空
        else if (player.deckOut) { gameOver = true; result = "卡组耗尽，你输了" }
        else if (opponent.deckOut) { gameOver = true; result = "对方卡组耗尽，你赢了！" }
    }

    fun log(s: String) {
        battleLog.add(s)
        if (battleLog.size > Dimens.BATTLE_LOG_MAX) battleLog.removeAt(0)
    }
}
