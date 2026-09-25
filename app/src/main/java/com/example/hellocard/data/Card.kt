package com.example.hellocard.data

data class Card(
    val uid: Int = 0,
    val id: String,
    val name: String,
    val atk: Int,
    val def: Int,
    val level: Int,
    val faction: String,
    val effect: String,
    val image: String,
    // 卡牌类型：monster / spell / trap / field
    val cardType: String = "monster",
    // 额外怪兽类型："" / aggregate（聚合）/ resonate（共鸣）/ overlay（叠加）/ link（链接）
    val extraType: String = "",
    var isDefense: Boolean = false,
    var summonedThisTurn: Boolean = false,
    var hasAttacked: Boolean = false
)
