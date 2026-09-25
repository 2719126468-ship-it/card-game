package com.example.hellocard.data

import android.content.Context
import org.json.JSONArray

object CardRepository {
    private var allCards: List<Card> = emptyList()

    fun load(context: Context) {
        if (allCards.isNotEmpty()) return
        val json = context.assets.open("cards/cards.json").bufferedReader().use { it.readText() }
        val arr = JSONArray(json)
        val list = mutableListOf<Card>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(
                Card(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    atk = o.getInt("atk"),
                    def = o.getInt("def"),
                    level = o.getInt("level"),
                    faction = o.getString("faction"),
                    effect = o.optString("effect", ""),
                    image = o.getString("image"),
                    cardType = o.optString("cardType", "monster"),
                    extraType = o.optString("extraType", "")
                )
            )
        }
        allCards = list
    }

    fun getAll(): List<Card> = allCards

    /** 主卡组可用：普通怪兽 + 魔法 + 陷阱 + 场地魔法 */
    fun getMainDeckPool(faction: String): List<Card> =
        allCards.filter { it.faction == faction && it.extraType.isEmpty() }

    /** 额外卡组：带有 extraType 的怪兽 */
    fun getExtraDeckPool(faction: String): List<Card> =
        allCards.filter { it.faction == faction && it.extraType.isNotEmpty() }

    fun findById(id: String): Card? = allCards.firstOrNull { it.id == id }

    fun buildDeck(ids: List<String>, startUid: Int = 1): Pair<List<Card>, Int> {
        var uid = startUid
        val list = ids.mapNotNull { id -> findById(id)?.copy(uid = uid++) }
        return Pair(list, uid)
    }

    /** 默认主卡组：40 张（人类普通卡 4 份） */
    fun defaultMainDeckIds(): List<String> {
        val pool = getMainDeckPool("human").map { it.id }
        val result = mutableListOf<String>()
        while (result.size < 40) result.addAll(pool)
        return result.take(40)
    }

    /** 默认额外卡组：15 张（人类额外卡，同名最多 3） */
    fun defaultExtraDeckIds(): List<String> {
        val pool = getExtraDeckPool("human").map { it.id }
        val result = mutableListOf<String>()
        while (result.size < 15) result.addAll(pool)
        return result.take(15)
    }

    /** AI 主卡组：40 张 */
    fun aiMainDeckIds(): List<String> {
        val pool = getMainDeckPool("ai").map { it.id }
        val result = mutableListOf<String>()
        while (result.size < 40) result.addAll(pool)
        return result.take(40)
    }

    /** AI 额外卡组：15 张 */
    fun aiExtraDeckIds(): List<String> {
        val pool = getExtraDeckPool("ai").map { it.id }
        val result = mutableListOf<String>()
        while (result.size < 15) result.addAll(pool)
        return result.take(15)
    }
}
