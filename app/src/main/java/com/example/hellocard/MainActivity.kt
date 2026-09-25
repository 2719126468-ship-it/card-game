package com.example.hellocard

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.*
import com.example.hellocard.data.Card
import com.example.hellocard.data.CardRepository
import com.example.hellocard.data.DeckStorage
import com.example.hellocard.game.AIStep
import com.example.hellocard.game.GameState
import com.example.hellocard.game.Phase

class MainActivity : Activity() {

    private lateinit var root: FrameLayout
    private lateinit var game: GameState
    private lateinit var animationManager: AnimationManager
    private lateinit var soundManager: SoundManager

    private lateinit var oppLife: TextView
    private lateinit var myLife: TextView
    private lateinit var oppMonsterZone: LinearLayout
    private lateinit var myMonsterZone: LinearLayout
    private lateinit var oppSpellZone: LinearLayout
    private lateinit var mySpellZone: LinearLayout
    private lateinit var oppExtraZone: LinearLayout
    private lateinit var myExtraZone: LinearLayout
    private lateinit var oppFieldZone: LinearLayout
    private lateinit var myFieldZone: LinearLayout
    private lateinit var handLayout: LinearLayout
    private lateinit var turnInfo: TextView
    private lateinit var logView: TextView
    private lateinit var actionBtn: Button

    private lateinit var oppDeckTv: TextView
    private lateinit var oppGraveTv: TextView
    private lateinit var oppExileTv: TextView
    private lateinit var myDeckTv: TextView
    private lateinit var myGraveTv: TextView
    private lateinit var myExileTv: TextView

    private var lastMyLife = Dimens.STARTING_LIFE
    private var lastOppLife = Dimens.STARTING_LIFE
    private val aiHandler = Handler(Looper.getMainLooper())
    private var aiPlaying = false
    private var aiStepRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CardRepository.load(this)
        soundManager = SoundManager(this)
        soundManager.loadAll()
        root = FrameLayout(this)
        root.setBackgroundColor(Theme.BG)
        setContentView(root)
        animationManager = AnimationManager(this, root, root)
        showMenu()
    }

    override fun onPause() {
        super.onPause()
        soundManager.pauseBgm()
        aiPlaying = false
        aiStepRunnable?.let { aiHandler.removeCallbacks(it) }
    }
    override fun onResume() { super.onResume(); soundManager.resumeBgm() }
    override fun onDestroy() {
        super.onDestroy()
        aiHandler.removeCallbacksAndMessages(null)
        soundManager.release()
    }

    private fun showMenu() {
        animationManager.ambientStop()
        root.removeAllViews()
        soundManager.playBgm("menu")
        val v = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(40, 40, 40, 40)
        }
        v.addView(TextView(this).apply {
            text = "代码深渊"
            setTextColor(Theme.ACCENT)
            textSize = 42f
            gravity = Gravity.CENTER
        })
        v.addView(TextView(this).apply {
            text = "world.execute(me);"
            setTextColor(Theme.PINK)
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, 10, 0, 60)
        })
        v.addView(Button(this).apply {
            text = "开始决斗"
            textSize = 20f
            setOnClickListener { soundManager.play("click"); startGame() }
        })
        v.addView(Button(this).apply {
            text = "编辑卡组"
            textSize = 18f
            setPadding(0, 20, 0, 0)
            setOnClickListener {
                soundManager.play("click")
                startActivity(Intent(this@MainActivity, DeckEditActivity::class.java))
            }
        })
        v.addView(Button(this).apply {
            text = "退出"
            textSize = 18f
            setPadding(0, 20, 0, 0)
            setOnClickListener { soundManager.play("click"); finish() }
        })
        root.addView(v)
    }

    private fun startGame() {
        game = GameState()
        val savedMain = DeckStorage.loadMainDeck(this)
        val mainIds = if (savedMain.size in 40..60) savedMain else CardRepository.defaultMainDeckIds()
        val savedExtra = DeckStorage.loadExtraDeck(this)
        val extraIds = if (savedExtra.isNotEmpty()) savedExtra else CardRepository.defaultExtraDeckIds()
        val (playerDeck, uid1) = CardRepository.buildDeck(mainIds, 1)
        val (oppDeck, uid2) = CardRepository.buildDeck(CardRepository.aiMainDeckIds(), uid1)
        val (playerExtra, uid3) = CardRepository.buildDeck(extraIds, uid2)
        val (oppExtra, _) = CardRepository.buildDeck(CardRepository.aiExtraDeckIds(), uid3)
        game.init(playerDeck, oppDeck, playerExtra, oppExtra)
        lastMyLife = game.player.life
        lastOppLife = game.opponent.life
        soundManager.playBgm("battle")
        animationManager.ambientStart()
        buildGameView()
    }

    private fun bg(fill: String, stroke: String, w: Int): GradientDrawable = GradientDrawable().apply {
        cornerRadius = 14f
        setColor(Color.parseColor(fill))
        setStroke(w, Color.parseColor(stroke))
    }

    private fun monsterZoneStyle(): GradientDrawable = GradientDrawable().apply {
        cornerRadius = 10f
        setColor(Color.parseColor("#101620"))
        setStroke(1, Color.parseColor("#2a3441"))
    }

    private fun spellZoneStyle(): GradientDrawable = GradientDrawable().apply {
        cornerRadius = 8f
        setColor(Color.parseColor("#0d1a14"))
        setStroke(1, Color.parseColor("#1d3a2a"))
    }

    private fun fieldZoneStyle(): GradientDrawable = GradientDrawable().apply {
        cornerRadius = 8f
        setColor(Color.parseColor("#1a1206"))
        setStroke(1, Color.parseColor("#4a3a10"))
    }

    private fun extraZoneStyle(): GradientDrawable = GradientDrawable().apply {
        cornerRadius = 10f
        setColor(Color.parseColor("#160b1a"))
        setStroke(1, Color.parseColor("#3a1a4a"))
    }

    private fun smallBtn(text: String): TextView = TextView(this).apply {
        this.text = text
        setTextColor(Color.parseColor("#8b949e"))
        textSize = 11f
        gravity = Gravity.CENTER
        setPadding(10, 6, 10, 6)
        background = bg("#1a1f28", "#2a3441", 1)
        isClickable = true
    }

    private fun space(w: Int): View = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(w, 1)
    }

    private fun buildGameView() {
        root.removeAllViews()
        val main = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Theme.BG)
        }
        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            isFillViewport = true
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(10, 14, 10, 10)
        }

        // ===== 对手信息栏 =====
        val oppInfo = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        oppDeckTv = smallBtn("卡组 40"); oppDeckTv.setOnClickListener { showListDialog("Momo 卡组", game.opponent.deck.toList()) }
        oppGraveTv = smallBtn("墓地 0"); oppGraveTv.setOnClickListener { showListDialog("Momo 墓地", game.opponent.graveyard.toList()) }
        oppExileTv = smallBtn("额外 0"); oppExileTv.setOnClickListener { showListDialog("Momo 额外卡组", game.opponent.exile.toList()) }
        oppInfo.addView(oppDeckTv); oppInfo.addView(space(6))
        oppInfo.addView(oppGraveTv); oppInfo.addView(space(6))
        oppInfo.addView(oppExileTv); oppInfo.addView(space(12))
        oppInfo.addView(TextView(this).apply {
            text = "Momo"
            setTextColor(Theme.RED)
            textSize = 15f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        oppLife = TextView(this).apply {
            setTextColor(Theme.RED)
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(14, 10, 14, 10)
            background = bg("#2d1519", "#ff4757", 2)
            setOnClickListener { onOpponentLifeClicked() }
        }
        oppInfo.addView(oppLife)
        content.addView(oppInfo)

        // ===== 对手场地：额外怪兽 | 场地魔法 | 魔陷区 =====
        val oppTopRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 4)
        }
        oppExtraZone = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        oppExtraZone.addView(zoneBox(Dimens.ZONE_SIDE_W, Dimens.ZONE_SIDE_H, extraZoneStyle()))
        oppTopRow.addView(oppExtraZone); oppTopRow.addView(space(8))
        oppFieldZone = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        oppFieldZone.addView(zoneBox(Dimens.ZONE_SIDE_W, Dimens.ZONE_SIDE_H, fieldZoneStyle()))
        oppTopRow.addView(oppFieldZone); oppTopRow.addView(space(8))
        oppSpellZone = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        repeat(Dimens.MAX_SPELL_ZONE) { oppSpellZone.addView(zoneBox(Dimens.ZONE_SPELL_W, Dimens.ZONE_SPELL_H, spellZoneStyle())) }
        oppTopRow.addView(oppSpellZone)
        content.addView(oppTopRow)

        // ===== 对手怪兽区 =====
        oppMonsterZone = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 4, 0, 10)
        }
        content.addView(HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(oppMonsterZone)
        })

        // ===== 回合信息 =====
        turnInfo = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 15f
            gravity = Gravity.CENTER
            setPadding(14, 10, 14, 10)
            background = bg("#161b22", "#30363d", 1)
        }
        content.addView(turnInfo)

        // ===== 我方怪兽区 =====
        myMonsterZone = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 10, 0, 4)
        }
        content.addView(HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(myMonsterZone)
        })

        // ===== 我方场地：魔陷区 | 场地魔法 | 额外怪兽 =====
        val myTopRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 4, 0, 10)
        }
        mySpellZone = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        repeat(Dimens.MAX_SPELL_ZONE) { mySpellZone.addView(zoneBox(Dimens.ZONE_SPELL_W, Dimens.ZONE_SPELL_H, spellZoneStyle())) }
        myTopRow.addView(mySpellZone); myTopRow.addView(space(8))
        myFieldZone = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        myFieldZone.addView(zoneBox(Dimens.ZONE_SIDE_W, Dimens.ZONE_SIDE_H, fieldZoneStyle()))
        myTopRow.addView(myFieldZone); myTopRow.addView(space(8))
        myExtraZone = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setOnClickListener { showExtraDeckDialog() } }
        myExtraZone.addView(zoneBox(Dimens.ZONE_SIDE_W, Dimens.ZONE_SIDE_H, extraZoneStyle()))
        myTopRow.addView(myExtraZone)
        content.addView(myTopRow)

        // ===== 我方信息栏 =====
        val myInfo = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        myLife = TextView(this).apply {
            setTextColor(Theme.GREEN)
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(14, 10, 14, 10)
            background = bg("#0f2a1a", "#2ed573", 2)
        }
        myInfo.addView(myLife); myInfo.addView(space(12))
        myInfo.addView(TextView(this).apply {
            text = "你"
            setTextColor(Theme.GREEN)
            textSize = 15f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        myDeckTv = smallBtn("卡组 40"); myDeckTv.setOnClickListener { showListDialog("你的卡组", game.player.deck.toList()) }
        myGraveTv = smallBtn("墓地 0"); myGraveTv.setOnClickListener { showListDialog("你的墓地", game.player.graveyard.toList()) }
        myExileTv = smallBtn("额外 0"); myExileTv.setOnClickListener { showListDialog("你的额外卡组", game.player.exile.toList()) }
        myInfo.addView(myDeckTv); myInfo.addView(space(6))
        myInfo.addView(myGraveTv); myInfo.addView(space(6))
        myInfo.addView(myExileTv)
        content.addView(myInfo)

        // ===== 日志 =====
        logView = TextView(this).apply {
            setTextColor(Color.parseColor("#8b949e"))
            textSize = 11f
            setPadding(12, 8, 12, 8)
            maxLines = 5
            background = bg("#161b22", "#30363d", 1)
        }
        val logParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        logParams.topMargin = 10
        content.addView(logView, logParams)

        // ===== 手牌 =====
        handLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 10, 0, 10)
        }
        content.addView(HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(handLayout)
        })

        scroll.addView(content)
        main.addView(scroll)

        actionBtn = Button(this).apply {
            text = "进入战斗阶段"
            textSize = 16f
            setOnClickListener { onActionClicked() }
        }
        main.addView(actionBtn, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(16, 6, 16, 12) })

        root.addView(main)
        refresh()
    }

    private fun zoneBox(w: Int, h: Int, style: GradientDrawable): View = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(w, h).apply { marginEnd = 4 }
        background = style
    }

    /** 统一入口：异步加载 assets 卡图 */
    private fun loadAssetImage(target: ImageView, path: String, sizePx: Int, radius: Float) {
        CardImageLoader.load(this, target, path, sizePx, radius)
    }

    private fun cardView(card: Card, owner: String, selected: Boolean, small: Boolean = false): View {
        val w = if (small) Dimens.CARD_WIDTH_SMALL else Dimens.CARD_WIDTH_NORMAL
        val h = if (small) Dimens.CARD_HEIGHT_SMALL else Dimens.CARD_HEIGHT_NORMAL
        val imgS = if (small) Dimens.CARD_IMG_SIZE_SMALL else Dimens.CARD_IMG_SIZE_NORMAL
        val lp = LinearLayout.LayoutParams(w, h)
        lp.marginEnd = Dimens.CARD_MARGIN_END
        val v = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(4, 6, 4, 6)
            background = bg(
                when (card.cardType) {
                    "spell" -> "#142a1a"
                    "trap" -> "#2a1414"
                    "field" -> "#2a2414"
                    else -> if (card.faction == "ai") "#2b1b45" else "#162c45"
                },
                if (selected) Theme.GOLD_HEX else if (card.hasAttacked) "#5a3a3a" else "#4a5568",
                if (selected) 6 else 2
            )
            layoutParams = lp
            tag = card.uid
        }

        val imgFrame = FrameLayout(this)
        val img = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(imgS, imgS)
            scaleType = ImageView.ScaleType.CENTER_CROP
            // 使用 Coil 异步加载 assets 图片
            loadAssetImage(this, card.image, imgS, Dimens.IMG_CORNER_RADIUS)
        }
        imgFrame.addView(img)

        if (card.isDefense) {
            imgFrame.addView(View(this).apply {
                layoutParams = FrameLayout.LayoutParams(imgS, imgS)
                setBackgroundColor(Color.parseColor("#AA0a0e14"))
            })
            imgFrame.addView(TextView(this).apply {
                text = "守"
                setTextColor(Color.parseColor("#4ecdc4"))
                textSize = 28f
                gravity = Gravity.CENTER
                layoutParams = FrameLayout.LayoutParams(imgS, imgS)
            })
        }
        v.addView(imgFrame)

        v.addView(TextView(this).apply {
            text = card.name
            setTextColor(Color.WHITE)
            textSize = if (small) 10f else 12f
            gravity = Gravity.CENTER
            maxLines = 1
        })
        val infoText = when (card.cardType) {
            "spell" -> "魔法"
            "trap" -> "陷阱"
            "field" -> "场地"
            else -> "${card.atk}/${card.def}"
        }
        v.addView(TextView(this).apply {
            text = infoText
            setTextColor(
                if (card.hasAttacked) Color.parseColor("#666666")
                else if (card.isDefense) Color.parseColor("#4ecdc4")
                else Color.parseColor("#ffd166")
            )
            textSize = if (small) 9f else 11f
            gravity = Gravity.CENTER
        })

        // 3D 触摸反馈：按下浮起、松开弹回
        v.setOnTouchListener { vv, ev ->
            when (ev.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    animationManager.pressDown(vv); false
                }
                android.view.MotionEvent.ACTION_UP,
                android.view.MotionEvent.ACTION_CANCEL -> {
                    animationManager.pressUp(vv); false
                }
                else -> false
            }
        }

        when (owner) {
            "me" -> {
                v.setOnClickListener { onMyCardClicked(card) }
                v.setOnLongClickListener { showCardDetail(card); true }
            }
            "opp" -> {
                v.setOnClickListener { onOpponentCardClicked(card) }
                v.setOnLongClickListener { showCardDetail(card); true }
            }
            "hand" -> {
                v.setOnClickListener { onHandCardClicked(card) }
                v.setOnLongClickListener { showCardDetail(card); true }
            }
            "spell" -> v.setOnLongClickListener { showCardDetail(card); true }
            "extra" -> {
                v.setOnClickListener { onExtraMonsterClicked(card) }
                v.setOnLongClickListener { showCardDetail(card); true }
            }
        }
        return v
    }

    private fun findCardView(parent: LinearLayout, uid: Int): View? {
        for (i in 0 until parent.childCount) {
            val t = parent.getChildAt(i).tag
            if (t is Int && t == uid) return parent.getChildAt(i)
        }
        return null
    }

    private fun refresh() {
        if (game.opponent.life < lastOppLife) {
            soundManager.play("damage")
            val loc = IntArray(2); oppLife.getLocationOnScreen(loc)
            animationManager.damagePopup(loc[0] + 80f, loc[1] + 20f, lastOppLife - game.opponent.life, false)
        } else if (game.opponent.life > lastOppLife) {
            soundManager.play("heal")
            val loc = IntArray(2); oppLife.getLocationOnScreen(loc)
            animationManager.damagePopup(loc[0] + 80f, loc[1] + 20f, game.opponent.life - lastOppLife, true)
        }
        if (game.player.life < lastMyLife) {
            soundManager.play("damage")
            val loc = IntArray(2); myLife.getLocationOnScreen(loc)
            animationManager.damagePopup(loc[0] + 80f, loc[1] + 20f, lastMyLife - game.player.life, false)
        } else if (game.player.life > lastMyLife) {
            soundManager.play("heal")
            val loc = IntArray(2); myLife.getLocationOnScreen(loc)
            animationManager.damagePopup(loc[0] + 80f, loc[1] + 20f, game.player.life - lastMyLife, true)
        }
        lastMyLife = game.player.life
        lastOppLife = game.opponent.life

        oppLife.text = "LP ${game.opponent.life}"
        myLife.text = "LP ${game.player.life}"
        oppDeckTv.text = "卡组 ${game.opponent.deck.size}"
        oppGraveTv.text = "墓地 ${game.opponent.graveyard.size}"
        oppExileTv.text = "额外 ${game.opponent.exile.size}"
        myDeckTv.text = "卡组 ${game.player.deck.size}"
        myGraveTv.text = "墓地 ${game.player.graveyard.size}"
        myExileTv.text = "额外 ${game.player.exile.size}"

        turnInfo.text = if (game.gameOver) game.result
            else "第 ${game.turn} 回合 · ${if (game.isPlayerTurn) "你的回合" else "Momo 的回合"} · ${game.phaseName()}"

        if (game.isPlayerTurn && !game.gameOver) {
            actionBtn.isEnabled = true
            when (game.phase) {
                Phase.MAIN -> {
                    actionBtn.text = if (game.player.normalSummoned)
                        "进入战斗阶段（已召唤）"
                    else "进入战斗阶段（可召唤）"
                }
                Phase.BATTLE -> actionBtn.text = "结束回合"
            }
        } else {
            actionBtn.isEnabled = false
            actionBtn.text = "等待对方..."
        }

        // 对手怪兽区
        oppMonsterZone.removeAllViews()
        repeat(Dimens.MAX_FIELD_SIZE - game.opponent.field.size) {
            oppMonsterZone.addView(zoneBox(Dimens.ZONE_MONSTER_W, Dimens.ZONE_MONSTER_H, monsterZoneStyle()))
        }
        game.opponent.field.forEach { c -> oppMonsterZone.addView(cardView(c, "opp", false)) }

        // 我方怪兽区
        myMonsterZone.removeAllViews()
        repeat(Dimens.MAX_FIELD_SIZE - game.player.field.size) {
            myMonsterZone.addView(zoneBox(Dimens.ZONE_MONSTER_W, Dimens.ZONE_MONSTER_H, monsterZoneStyle()))
        }
        game.player.field.forEach { c ->
            val sel = game.selectedAttacker?.uid == c.uid
            myMonsterZone.addView(cardView(c, "me", sel))
        }

        // 额外怪兽区
        oppExtraZone.removeAllViews()
        if (game.opponent.extraMonster != null) {
            oppExtraZone.addView(cardView(game.opponent.extraMonster!!, "opp", false, small = true))
        } else {
            oppExtraZone.addView(zoneBox(Dimens.ZONE_SIDE_W, Dimens.ZONE_SIDE_H, extraZoneStyle()))
        }
        myExtraZone.removeAllViews()
        if (game.player.extraMonster != null) {
            myExtraZone.addView(cardView(game.player.extraMonster!!, "me", game.selectedAttacker?.uid == game.player.extraMonster!!.uid, small = true))
        } else {
            myExtraZone.addView(zoneBox(Dimens.ZONE_SIDE_W, Dimens.ZONE_SIDE_H, extraZoneStyle()))
        }

        // 场地魔法
        oppFieldZone.removeAllViews()
        if (game.opponent.fieldSpell != null) {
            oppFieldZone.addView(cardView(game.opponent.fieldSpell!!, "spell", false, small = true))
        } else oppFieldZone.addView(zoneBox(Dimens.ZONE_SIDE_W, Dimens.ZONE_SIDE_H, fieldZoneStyle()))
        myFieldZone.removeAllViews()
        if (game.player.fieldSpell != null) {
            myFieldZone.addView(cardView(game.player.fieldSpell!!, "spell", false, small = true))
        } else myFieldZone.addView(zoneBox(Dimens.ZONE_SIDE_W, Dimens.ZONE_SIDE_H, fieldZoneStyle()))

        // 魔陷区（展示已有卡，其余留空格）
        renderSpellZone(oppSpellZone, game.opponent.spellZone, "opp")
        renderSpellZone(mySpellZone, game.player.spellZone, "me")

        // 手牌
        handLayout.removeAllViews()
        if (game.isPlayerTurn && game.phase == Phase.MAIN && !game.player.normalSummoned) {
            game.player.hand.forEach { c -> handLayout.addView(cardView(c, "hand", false)) }
        } else {
            game.player.hand.forEach { c ->
                val v = cardView(c, "hand", false)
                v.alpha = 0.4f
                handLayout.addView(v)
            }
        }

        logView.text = game.battleLog.takeLast(5).joinToString("\n")
    }

    private fun renderSpellZone(parent: LinearLayout, cards: List<Card>, owner: String) {
        parent.removeAllViews()
        cards.forEach { c -> parent.addView(cardView(c, "spell", false, small = true)) }
        repeat((Dimens.MAX_SPELL_ZONE - cards.size).coerceAtLeast(0)) { parent.addView(zoneBox(Dimens.ZONE_SPELL_W, Dimens.ZONE_SPELL_H, spellZoneStyle())) }
    }

    private fun onActionClicked() {
        if (!game.isPlayerTurn || game.gameOver || aiPlaying) return
        soundManager.play("click")
        when (game.phase) {
            Phase.MAIN -> { animationManager.phaseChange(); game.advancePhase(); refresh() }
            Phase.BATTLE -> { onEndTurn() }
        }
    }

    private fun onEndTurn() {
        if (game.gameOver || aiPlaying) return
        soundManager.play("turn")
        animationManager.turnChange()
        game.startAITurn()
        refresh()
        root.postDelayed({ playAITurn() }, Dimens.AI_STEP_DELAY)
    }

    private fun playAITurn() {
        // startAITurn() 阶段就可能已分出胜负（对方卡组耗尽），此时必须走结算
        if (game.gameOver) {
            refresh()
            checkOver()
            return
        }
        val steps = game.planAITurn()
        if (steps.isEmpty()) {
            finishAITurn()
            return
        }
        aiPlaying = true
        var i = 0
        fun next() {
            if (game.gameOver || !aiPlaying) {
                aiPlaying = false
                finishAITurn()
                return
            }
            if (i >= steps.size) {
                aiPlaying = false
                finishAITurn()
                return
            }
            val step = steps[i++]
            executeAndAnimate(step)
            val delay = when (step.kind) {
                "attack", "direct" -> Dimens.AI_DELAY_ATTACK
                "extra" -> Dimens.AI_DELAY_EXTRA
                "summon", "spell", "field" -> Dimens.AI_DELAY_SUMMON
                else -> Dimens.AI_DELAY_DEFAULT
            }
            aiStepRunnable = Runnable { next() }
            aiHandler.postDelayed(aiStepRunnable!!, delay)
        }
        next()
    }

    private fun executeAndAnimate(step: AIStep) {
        when (step.kind) {
            "extra" -> {
                val card = step.sourceCard ?: return
                game.executeAIStep(step)
                refresh()
                soundManager.play("summon")
                val v = findCardView(oppExtraZone, card.uid)
                if (v != null) animationManager.extraSummon(v)
                else animationManager.particleSummon(root)
            }
            "summon" -> {
                val card = step.sourceCard ?: return
                game.executeAIStep(step)
                refresh()
                soundManager.play("summon")
                val v = findCardView(oppMonsterZone, card.uid)
                if (v != null) animationManager.flip3D(v)
                else animationManager.particleSummon(root)
            }
            "spell", "field" -> {
                game.executeAIStep(step)
                refresh()
                soundManager.play("effect")
                if (step.kind == "field") {
                    animationManager.fieldSpellActivate(root)
                } else {
                    animationManager.particleEffect(root)
                }
            }
            "attack" -> {
                val a = step.sourceCard ?: return
                val aView = findCardView(oppMonsterZone, a.uid)
                    ?: findCardView(oppExtraZone, a.uid)
                val tView = findCardView(myMonsterZone, step.targetCard?.uid ?: -1)
                    ?: findCardView(myExtraZone, step.targetCard?.uid ?: -1)
                    ?: findFirstView(myMonsterZone)
                soundManager.play("attack")
                if (aView != null && tView != null) {
                    animationManager.attack(aView, tView,
                        onHit = { animationManager.screenShake() },
                        onEnd = {
                            game.executeAIStep(step)
                            refresh()
                            checkOver()
                        })
                } else {
                    game.executeAIStep(step)
                    refresh()
                    checkOver()
                }
            }
            "direct" -> {
                val a = step.sourceCard ?: return
                val aView = findCardView(oppMonsterZone, a.uid)
                    ?: findCardView(oppExtraZone, a.uid)
                val loc = IntArray(2); myLife.getLocationOnScreen(loc)
                soundManager.play("attack")
                if (aView != null) {
                    animationManager.attackDirect(aView, loc[0] + 100f, loc[1] + 20f,
                        onHit = { animationManager.screenShake() },
                        onEnd = {
                            game.executeAIStep(step)
                            refresh()
                            checkOver()
                        })
                } else {
                    game.executeAIStep(step)
                    refresh()
                    checkOver()
                }
            }
        }
    }

    private fun findFirstView(parent: LinearLayout): View? {
        for (i in 0 until parent.childCount) {
            val t = parent.getChildAt(i).tag
            if (t is Int && t > 0) return parent.getChildAt(i)
        }
        return null
    }

    private fun finishAITurn() {
        if (game.gameOver) { checkOver(); return }
        game.endAITurn()
        refresh()
        animationManager.turnChange()
        soundManager.play("turn")
        checkOver()
    }

    private fun onOpponentLifeClicked() {
        val a = game.selectedAttacker ?: return
        val err = game.canPlayerAttack(a)
        if (err != null) { soundManager.play("error"); Toast.makeText(this, err, Toast.LENGTH_SHORT).show(); return }
        val oppMonsters = game.opponent.field + listOfNotNull(game.opponent.extraMonster)
        if (oppMonsters.isEmpty()) {
            val attackerView = findCardView(myMonsterZone, a.uid)
                ?: findCardView(myExtraZone, a.uid)
            val loc = IntArray(2); oppLife.getLocationOnScreen(loc)
            soundManager.play("attack")
            if (attackerView != null) {
                animationManager.attackDirect(attackerView, loc[0] + 60f, loc[1] + 10f,
                    onHit = { animationManager.screenShake() },
                    onEnd = { game.directAttack(a); refresh(); checkOver() })
            } else { game.directAttack(a); refresh(); checkOver() }
        } else {
            soundManager.play("error")
            Toast.makeText(this, "先击破对方怪兽", Toast.LENGTH_SHORT).show()
        }
    }

    private fun onMyCardClicked(c: Card) {
        if (!game.isPlayerTurn || game.gameOver) return
        val err = game.canPlayerAttack(c)
        if (err != null) { soundManager.play("error"); Toast.makeText(this, err, Toast.LENGTH_SHORT).show(); return }
        soundManager.play("click")
        game.selectedAttacker = if (game.selectedAttacker?.uid == c.uid) null else c
        val sel = findCardView(myMonsterZone, c.uid)
        if (sel != null) {
            if (game.selectedAttacker == null) animationManager.deselected(sel)
            else animationManager.selected(sel)
        }
        refresh()
    }

    private fun onOpponentCardClicked(t: Card) {
        val a = game.selectedAttacker ?: run {
            soundManager.play("error")
            Toast.makeText(this, "先选自己的怪兽", Toast.LENGTH_SHORT).show()
            return
        }
        val err = game.canPlayerAttack(a)
        if (err != null) { soundManager.play("error"); Toast.makeText(this, err, Toast.LENGTH_SHORT).show(); return }
        val attackerView = findCardView(myMonsterZone, a.uid)
            ?: findCardView(myExtraZone, a.uid)
        val targetView = findCardView(oppMonsterZone, t.uid)
            ?: findCardView(oppExtraZone, t.uid)
        soundManager.play("attack")
        if (attackerView == null || targetView == null) {
            game.attack(a, t); refresh(); checkOver(); return
        }
        val attackerDies = if (t.isDefense) a.atk < t.def else a.atk < t.atk
        val targetDies = if (t.isDefense) a.atk > t.def else a.atk > t.atk
        animationManager.attack(attackerView, targetView,
            onHit = {
                animationManager.screenShake()
                soundManager.play("destroy")
                if (targetDies) animationManager.destroy(targetView) { }
                if (attackerDies) animationManager.destroy(attackerView) { }
            },
            onEnd = { game.attack(a, t); refresh(); checkOver() })
    }

    private fun onExtraMonsterClicked(c: Card) {
        // 额外怪兽已经在场上，与普通怪兽一样处理
        onMyCardClicked(c)
    }

    private fun onHandCardClicked(c: Card) {
        if (!game.isPlayerTurn || game.gameOver) return
        if (game.phase != Phase.MAIN) {
            soundManager.play("error")
            Toast.makeText(this, "只能在主要阶段操作", Toast.LENGTH_SHORT).show()
            return
        }
        soundManager.play("click")
        when (c.cardType) {
            "monster" -> {
                if (game.player.normalSummoned) {
                    soundManager.play("error")
                    Toast.makeText(this, "本回合已通常召唤过", Toast.LENGTH_SHORT).show()
                    return
                }
                showMonsterSummonDialog(c)
            }
            "spell" -> {
                if (game.activateSpell(game.player, c)) { soundManager.play("effect"); animationManager.particleEffect(root); refresh() }
                else Toast.makeText(this, "无法发动（魔陷区已满）", Toast.LENGTH_SHORT).show()
            }
            "trap" -> {
                if (game.setTrap(game.player, c)) { soundManager.play("click"); refresh() }
                else Toast.makeText(this, "无法盖放（魔陷区已满）", Toast.LENGTH_SHORT).show()
            }
            "field" -> {
                if (game.activateField(game.player, c)) { soundManager.play("effect"); animationManager.fieldSpellActivate(root); refresh() }
                else Toast.makeText(this, "场地魔法区已被占用", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showMonsterSummonDialog(c: Card) {
        val tributes = game.tributeCountFor(c)
        if (game.player.field.size < tributes) {
            soundManager.play("error")
            Toast.makeText(this, "需要 $tributes 个祭品", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("召唤「${c.name}」")
            .setItems(arrayOf("攻击表示（正面朝上）", "守备表示（背面朝下）")) { _, which ->
                if (game.summon(game.player, c, which == 1)) {
                    soundManager.play("summon")
                    refresh()
                    val newView = findCardView(myMonsterZone, c.uid)
                    newView?.let {
                        animationManager.flip3D(it)
                        if (c.effect.isNotEmpty()) {
                            root.postDelayed({
                                soundManager.play("effect")
                                animationManager.effect(it, "效果发动")
                            }, 550)
                        }
                    }
                } else {
                    soundManager.play("error")
                    Toast.makeText(this, "召唤失败", Toast.LENGTH_SHORT).show()
                }
            }.show()
    }

    private fun onExtraMonsterSummon(c: Card) {
        val err = game.canSpecialSummon(game.player, c)
        if (err != null) {
            soundManager.play("error")
            Toast.makeText(this, err, Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("特殊召唤「${c.name}」")
            .setMessage("消耗 2 只怪兽作为素材")
            .setPositiveButton("确认") { _, _ ->
                if (game.specialSummon(game.player, c)) {
                    soundManager.play("summon")
                    refresh()
                    val newView = findCardView(myExtraZone, c.uid)
                    newView?.let {
                        animationManager.flip3D(it)
                        root.postDelayed({
                            soundManager.play("effect")
                            animationManager.effect(it, "额外召唤")
                        }, 550)
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showExtraDeckDialog() {
        if (!game.isPlayerTurn || game.gameOver) return
        if (game.player.extraMonster != null) {
            Toast.makeText(this, "额外怪兽区已被占用", Toast.LENGTH_SHORT).show()
            return
        }
        if (game.player.exile.isEmpty()) {
            Toast.makeText(this, "额外卡组为空", Toast.LENGTH_SHORT).show()
            return
        }
        val cards = game.player.exile.toList()
        val names = cards.map { "${it.name}  (${it.atk}/${it.def})  [${extraTypeName(it.extraType)}]" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("额外卡组（消耗 2 只祭品）")
            .setItems(names) { _, which -> onExtraMonsterSummon(cards[which]) }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun extraTypeName(t: String): String = when (t) {
        "aggregate" -> "聚合"
        "resonate" -> "共鸣"
        "overlay" -> "叠加"
        "link" -> "链接"
        else -> "额外"
    }

    private fun showCardDetail(card: Card) {
        val v = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 30, 30, 30)
            setBackgroundColor(Theme.BG_CARD)
        }
        val img = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(400, 400)
            scaleType = ImageView.ScaleType.CENTER_CROP
            loadAssetImage(this, card.image, Dimens.DETAIL_IMG_SIZE, 12f)
        }
        v.addView(img, LinearLayout.LayoutParams(400, 400).apply { gravity = Gravity.CENTER_HORIZONTAL })

        fun addLine(label: String, value: String, color: Int = Theme.FG) {
            v.addView(TextView(this).apply {
                text = "$label：$value"
                setTextColor(color)
                textSize = 15f
                setPadding(0, 10, 0, 0)
            })
        }

        addLine("名称", card.name)
        addLine("类型", EffectText.typeName(card), Theme.ACCENT)
        if (card.cardType == "monster") {
            addLine("星级", "★".repeat(card.level.coerceAtMost(8)))
            addLine("ATK / DEF", "${card.atk} / ${card.def}", Theme.GOLD)
        }
        val eff = EffectText.toChinese(card.effect)
        addLine("效果", eff, if (card.effect.isBlank()) Theme.FG_MUTE else Theme.PINK)
        addLine("阵营", if (card.faction == "ai") "Momo（AI）" else "人类", Theme.FG_DIM)

        AlertDialog.Builder(this)
            .setView(ScrollView(this).apply { addView(v) })
            .setPositiveButton("关闭", null)
            .show()
    }

    private fun showListDialog(title: String, cards: List<Card>) {
        if (cards.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("$title（空）")
                .setMessage("这里还没有卡牌。")
                .setPositiveButton("关闭", null)
                .show()
            return
        }
        val v = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 10, 0, 10)
            setBackgroundColor(Theme.BG)
        }
        val scroll = ScrollView(this)
        val inner = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        cards.forEach { c ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(20, 14, 20, 14)
                gravity = Gravity.CENTER_VERTICAL
                setBackgroundColor(Theme.BG_CARD)
                isClickable = true
            }
            // 小缩略图（Coil 异步加载）
            val thumb = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(72, 72)
                scaleType = ImageView.ScaleType.CENTER_CROP
                loadAssetImage(this, c.image, Dimens.LIST_THUMB_SIZE, Dimens.THUMB_CORNER_RADIUS)
            }
            row.addView(thumb)

            val info = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 0, 0, 0)
            }
            info.addView(TextView(this).apply {
                text = c.name
                setTextColor(Theme.FG)
                textSize = 15f
            })
            val stat = when (c.cardType) {
                "monster" -> "${EffectText.typeName(c)}  ATK ${c.atk} / DEF ${c.def}"
                else -> EffectText.typeName(c)
            }
            info.addView(TextView(this).apply {
                text = stat
                setTextColor(Theme.ACCENT)
                textSize = 12f
                setPadding(0, 4, 0, 0)
            })
            if (c.effect.isNotBlank()) {
                info.addView(TextView(this).apply {
                    text = EffectText.toChinese(c.effect)
                    setTextColor(Theme.PINK)
                    textSize = 11f
                    setPadding(0, 4, 0, 0)
                    maxLines = 2
                })
            }
            row.addView(info, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

            row.setOnClickListener { showCardDetail(c) }
            inner.addView(row)
            // 分隔线
            inner.addView(View(this).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
                setBackgroundColor(Theme.BORDER)
            })
        }
        scroll.addView(inner)
        v.addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 900))
        AlertDialog.Builder(this)
            .setTitle("$title（${cards.size} 张，点击查看详情）")
            .setView(v)
            .setPositiveButton("关闭", null)
            .show()
    }

    private fun checkOver() {
        if (game.gameOver) {
            actionBtn.isEnabled = false
            val won = game.result.contains("赢")
            soundManager.play(if (won) "win" else "lose")
            if (won) soundManager.playBgm("win")
            Toast.makeText(this, game.result, Toast.LENGTH_LONG).show()
            root.postDelayed({ showResult() }, 1800)
        }
    }

    private fun showResult() {
        root.removeAllViews()
        val won = game.result.contains("赢")
        val ending = Endings.pick(
            won = won,
            deletes = game.player.graveyard.size,
            turn = game.turn,
            playerLife = game.player.life
        )

        val v = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(40, 40, 40, 40)
            setBackgroundColor(Theme.BG)
        }

        // END_xx / TAG
        v.addView(TextView(this).apply {
            text = "${ending.id} / ${ending.tag}"
            setTextColor(Theme.FG_MUTE)
            textSize = 11f
            typeface = android.graphics.Typeface.MONOSPACE
            gravity = Gravity.CENTER
        })

        // 大标题
        v.addView(TextView(this).apply {
            text = ending.title
            setTextColor(if (won) Theme.GREEN else Theme.RED)
            textSize = 22f
            typeface = android.graphics.Typeface.MONOSPACE
            gravity = Gravity.CENTER
            setPadding(0, 30, 0, 20)
        })

        // 叙事行
        v.addView(TextView(this).apply {
            text = ending.line
            setTextColor(Theme.FG_DIM)
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 50)
        })

        // 参数回顾
        v.addView(TextView(this).apply {
            text = "turn ${game.turn}  |  LP ${game.player.life}  |  grave ${game.player.graveyard.size}"
            setTextColor(Theme.ACCENT)
            textSize = 11f
            typeface = android.graphics.Typeface.MONOSPACE
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 40)
        })

        v.addView(Button(this).apply {
            text = "再来一局"
            textSize = 18f
            setOnClickListener { soundManager.play("click"); startGame() }
        })
        v.addView(Button(this).apply {
            text = "返回主菜单"
            textSize = 18f
            setPadding(0, 20, 0, 0)
            setOnClickListener { soundManager.play("click"); showMenu() }
        })

        root.addView(v)
    }
}
