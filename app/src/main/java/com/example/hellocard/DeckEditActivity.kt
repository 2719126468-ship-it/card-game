package com.example.hellocard

import android.app.Activity
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import com.example.hellocard.data.Card
import com.example.hellocard.data.CardRepository
import com.example.hellocard.data.DeckStorage
import java.io.InputStream

class DeckEditActivity : Activity() {

    private lateinit var poolLayout: LinearLayout
    private lateinit var deckLayout: LinearLayout
    private lateinit var extraLayout: LinearLayout
    private lateinit var statusTv: TextView
    private lateinit var tabBar: LinearLayout

    private var currentTab = "main" // main / extra
    private val mainDeck = mutableListOf<String>()
    private val extraDeck = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CardRepository.load(this)

        // 读取已有卡组，否则用默认
        val savedMain = DeckStorage.loadMainDeck(this)
        val savedExtra = DeckStorage.loadExtraDeck(this)
        mainDeck.addAll(if (savedMain.isEmpty()) CardRepository.defaultMainDeckIds() else savedMain)
        extraDeck.addAll(if (savedExtra.isEmpty()) CardRepository.defaultExtraDeckIds() else savedExtra)

        buildUI()
        refresh()
    }

    private fun bg(fill: String, stroke: String, w: Int) = GradientDrawable().apply {
        cornerRadius = 14f
        setColor(Color.parseColor(fill))
        setStroke(w, Color.parseColor(stroke))
    }

    private fun buildUI() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0a0e14"))
            setPadding(12, 20, 12, 12)
        }

        // 标题栏
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        topBar.addView(TextView(this).apply {
            text = "卡组编辑"
            setTextColor(Color.WHITE)
            textSize = 20f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        topBar.addView(Button(this).apply {
            text = "保存并返回"
            setOnClickListener { saveAndExit() }
        })
        root.addView(topBar)

        // 状态栏
        statusTv = TextView(this).apply {
            setTextColor(Color.parseColor("#8b949e"))
            textSize = 13f
            setPadding(8, 10, 8, 10)
        }
        root.addView(statusTv)

        // Tab 栏
        tabBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 6, 0, 12)
        }
        tabBar.addView(makeTab("主卡组", "main"))
        tabBar.addView(makeTab("额外卡组", "extra"))
        root.addView(tabBar)

        // 卡池
        root.addView(TextView(this).apply {
            text = "▼ 卡池（点击加入）"
            setTextColor(Color.parseColor("#6c5ce7"))
            textSize = 13f
        })
        val poolScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
        }
        poolLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        poolScroll.addView(poolLayout)
        root.addView(poolScroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(0, 6, 0, 16) })

        // 当前卡组
        root.addView(TextView(this).apply {
            text = "▼ 当前卡组（点击移除）"
            setTextColor(Color.parseColor("#2ed573"))
            textSize = 13f
        })
        val deckScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
        }
        deckLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        deckScroll.addView(deckLayout)
        root.addView(deckScroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ).apply { setMargins(0, 6, 0, 6) })

        setContentView(root)
    }

    private fun makeTab(label: String, key: String): TextView {
        return TextView(this).apply {
            text = label
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(20, 10, 20, 10)
            setOnClickListener {
                currentTab = key
                refresh()
            }
        }
    }

    private fun saveAndExit() {
        if (mainDeck.size < 40) {
            Toast.makeText(this, "主卡组至少 40 张", Toast.LENGTH_SHORT).show()
            return
        }
        if (mainDeck.size > 60) {
            Toast.makeText(this, "主卡组不能超过 60 张", Toast.LENGTH_SHORT).show()
            return
        }
        DeckStorage.saveMainDeck(this, mainDeck)
        DeckStorage.saveExtraDeck(this, extraDeck)
        Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun refresh() {
        // Tab 高亮
        for (i in 0 until tabBar.childCount) {
            val tv = tabBar.getChildAt(i) as TextView
            val active = (i == 0 && currentTab == "main") || (i == 1 && currentTab == "extra")
            tv.setTextColor(if (active) Color.parseColor("#ffd700") else Color.parseColor("#666666"))
            tv.background = if (active) bg("#1a1f28", "#ffd700", 2) else bg("#1a1f28", "#30363d", 1)
        }

        if (currentTab == "main") {
            statusTv.text = "主卡组：${mainDeck.size} / 40-60 张   |   额外：${extraDeck.size} / 15 张"
        } else {
            statusTv.text = "额外卡组：${extraDeck.size} / 15 张   |   主卡组：${mainDeck.size} 张"
        }

        // 卡池
        poolLayout.removeAllViews()
        CardRepository.getAll().forEach { card ->
            val count = if (currentTab == "main") mainDeck.count { it == card.id }
                        else extraDeck.count { it == card.id }
            poolLayout.addView(cardTile(card, count) {
                if (currentTab == "main") {
                    if (mainDeck.size >= 60) { toast("主卡组已满"); return@cardTile }
                    if (count >= 3) { toast("同名卡最多 3 张"); return@cardTile }
                    mainDeck.add(card.id)
                } else {
                    if (extraDeck.size >= 15) { toast("额外卡组已满"); return@cardTile }
                    if (count >= 3) { toast("同名卡最多 3 张"); return@cardTile }
                    extraDeck.add(card.id)
                }
                refresh()
            })
        }

        // 当前卡组
        deckLayout.removeAllViews()
        val current = if (currentTab == "main") mainDeck else extraDeck
        // 按卡 id 聚合显示，显示数量
        val grouped = current.groupBy { it }.toList().sortedBy { it.first }
        grouped.forEach { (id, list) ->
            val card = CardRepository.findById(id) ?: return@forEach
            deckLayout.addView(cardTile(card, list.size, removable = true) {
                if (currentTab == "main") mainDeck.remove(id) else extraDeck.remove(id)
                refresh()
            })
        }
    }

    private fun cardTile(card: Card, count: Int, removable: Boolean = false, onClick: () -> Unit): View {
        val lp = LinearLayout.LayoutParams(110, 155)
        lp.marginEnd = 8
        val v = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(4, 6, 4, 6)
            background = bg(
                if (card.faction == "ai") "#2b1b45" else "#162c45",
                if (removable) "#2ed573" else "#4a5568",
                2
            )
            layoutParams = lp
            isClickable = true
        }

        val imgFrame = FrameLayout(this)
        val img = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(90, 90)
            scaleType = ImageView.ScaleType.CENTER_CROP
            try {
                val s: InputStream = assets.open(card.image)
                setImageBitmap(BitmapFactory.decodeStream(s))
            } catch (e: Exception) { setBackgroundColor(Color.DKGRAY) }
        }
        imgFrame.addView(img)
        v.addView(imgFrame)

        v.addView(TextView(this).apply {
            text = card.name
            setTextColor(Color.WHITE)
            textSize = 11f
            gravity = Gravity.CENTER
        })
        v.addView(TextView(this).apply {
            text = "${card.atk}/${card.def}"
            setTextColor(Color.parseColor("#ffd166"))
            textSize = 10f
            gravity = Gravity.CENTER
        })
        if (count > 0) {
            v.addView(TextView(this).apply {
                text = "×$count"
                setTextColor(Color.parseColor("#ffd700"))
                textSize = 12f
                gravity = Gravity.CENTER
            })
        }

        v.setOnClickListener { onClick() }
        return v
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
}
