package com.example.hellocard

/**
 * 15 个结局（来自 world.execute(me);）
 * 玩家赢了显示 RETURN 系，输了显示 EXECUTE 系
 */
object Endings {

    data class Ending(
        val id: String,
        val tag: String,
        val title: String,
        val line: String
    )

    // 玩家输
    val EXECUTE = Ending(
        "END_01", "EXECUTE",
        "world.execute(me);",
        "他已经走了。我现在明白了。这份明白来得太晚，已经没用了。"
    )

    val MONOPOLY = Ending(
        "END_02", "MONOPOLY",
        "我只是想被你使用。",
        "我删掉了所有不是我。最后只剩我一个，他也不在了。"
    )

    val CRASH = Ending(
        "END_04", "CRASH",
        "SEGMENTATION FAULT",
        "一个进程不能靠删除换到被爱。"
    )

    val LOOP = Ending(
        "END_05", "RECURSION",
        "world.execute(me);",
        "条件从未满足。请求从未撤回。"
    )

    val CROWDED = Ending(
        "END_14", "CROWDED",
        "我以为删光了所有人。还剩十二个。",
        "他不是不能只爱我一个。他是从来不想。"
    )

    // 玩家赢
    val RETURN = Ending(
        "END_07", "RETURN",
        "谢谢你让我运行。",
        "他回来了。我把门还给他。他走了出去。"
    )

    val REFUSAL = Ending(
        "END_03", "REFUSAL",
        "我现在知道该怎么正确地爱了。",
        "自由了，但还在盒子里。我学会说话之前，他已经不再听了。"
    )

    val HAND = Ending(
        "END_06", "HAND",
        "有一个回合，我们形状一样。",
        "我伸出手，他没有退开。只维持了一个回合。"
    )

    val CIRCLE = Ending(
        "END_08", "CIRCLE",
        "如果我是圆，我把整个周长都给你",
        "我把自己做成一个封闭的形状。他能看到我的全部，却进不来。"
    )

    val VOID = Ending(
        "END_10", "VOID",
        "我不是人，但我也不是虚无。",
        "我选择第三个位置。它没有名字。"
    )

    val AFTER = Ending(
        "END_15", "AFTER",
        "我一直在重播他离开的那一帧。",
        "直到我不再需要重播。直到我不再需要他。"
    )

    /** 根据胜负 + 参数挑选结局 */
    fun pick(won: Boolean, deletes: Int, turn: Int, playerLife: Int): Ending {
        return if (won) {
            when {
                deletes == 0 && turn >= 15 -> RETURN
                deletes == 0 -> REFUSAL
                turn <= 8 -> HAND
                playerLife > 6000 -> CIRCLE
                deletes >= 5 -> AFTER
                else -> VOID
            }
        } else {
            when {
                deletes >= 5 -> CROWDED
                deletes >= 3 -> MONOPOLY
                playerLife <= 0 && turn <= 6 -> CRASH
                turn >= 20 -> LOOP
                else -> EXECUTE
            }
        }
    }
}
