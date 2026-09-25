package com.example.hellocard

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class AnimationManager(
    private val context: Context,
    private val root: FrameLayout,
    private val particleHost: ViewGroup
) {

    // ================= 调色板 =================
    private object Pal {
        val CYAN   = 0xFF7fe3c4.toInt()
        val PINK   = 0xFFff9db6.toInt()
        val GOLD   = 0xFFffd479.toInt()
        val RED    = 0xFFff5a52.toInt()
        val WHITE  = 0xFFFFFFFF.toInt()
        val PURPLE = 0xFFa855f7.toInt()
        val BLUE   = 0xFF4ecdc4.toInt()
        val ORANGE = 0xFFe0a95f.toInt()
        val ALL    = intArrayOf(CYAN, PINK, GOLD, RED, WHITE, PURPLE, BLUE, ORANGE)
        fun rand(): Int = ALL[Random.nextInt(ALL.size)]
    }

    // ================= 粒子工厂 =================
    private fun particleView(color: Int, size: Int, shapeType: String): View {
        return View(context).apply {
            layoutParams = FrameLayout.LayoutParams(size, size)
            background = when (shapeType) {
                "rect" -> GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    setColor(color)
                }
                "ring" -> GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.TRANSPARENT)
                    setStroke(2, color)
                }
                else -> GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(color)
                }
            }
        }
    }

    private fun anchorCenter(anchor: View): Pair<Float, Float> {
        val loc = IntArray(2); anchor.getLocationOnScreen(loc)
        val rloc = IntArray(2); root.getLocationOnScreen(rloc)
        return Pair(
            (loc[0] - rloc[0] + anchor.width / 2f),
            (loc[1] - rloc[1] + anchor.height / 2f)
        )
    }

    private fun spawn(
        x: Float, y: Float, color: Int, size: Int = 8,
        shape: String = "oval",
        dx: Float, dy: Float,
        sFrom: Float = 1f, sTo: Float = 0f,
        aFrom: Float = 1f, aTo: Float = 0f,
        rotTo: Float = 0f,
        duration: Long = 800L, delay: Long = 0L,
        interp: Int = 0  // 0 linear, 1 accel, 2 decel, 3 overshoot
    ): View {
        val v = particleView(color, size, shape)
        val lp = v.layoutParams as FrameLayout.LayoutParams
        lp.leftMargin = (x - size / 2f).toInt()
        lp.topMargin  = (y - size / 2f).toInt()
        v.layoutParams = lp
        v.alpha = aFrom
        v.scaleX = sFrom; v.scaleY = sFrom
        root.addView(v)

        val ax = v.animate().translationX(dx).translationY(dy)
            .scaleX(sTo).scaleY(sTo).alpha(aTo)
            .setDuration(duration)
            .setStartDelay(delay)
        if (rotTo != 0f) ax.rotation(rotTo)
        when (interp) {
            1 -> ax.setInterpolator(AccelerateInterpolator())
            2 -> ax.setInterpolator(DecelerateInterpolator())
            3 -> ax.setInterpolator(OvershootInterpolator())
            else -> ax.setInterpolator(LinearInterpolator())
        }
        ax.withEndAction { (v.parent as? ViewGroup)?.removeView(v) }
        ax.start()
        return v
    }

    // ================= 粒子基础运动 =================

    /** 四周爆散 */
    private fun burst(anchor: View, color: Int, count: Int, speed: Float, dur: Long, shape: String = "oval") {
        anchor.post {
            val (cx, cy) = anchorCenter(anchor)
            repeat(count) { i ->
                val a = Random.nextDouble(0.0, Math.PI * 2)
                val d = Random.nextFloat() * 180f * speed + 30f
                val size = Random.nextInt(5, 14)
                spawn(
                    cx, cy, color, size, shape,
                    dx = (cos(a) * d).toFloat(),
                    dy = (sin(a) * d).toFloat(),
                    duration = dur + Random.nextLong(0, 300),
                    interp = 2
                )
            }
        }
    }

    /** 向上升起 */
    private fun rising(anchor: View, color: Int, count: Int, dur: Long) {
        anchor.post {
            val (cx, cy) = anchorCenter(anchor)
            repeat(count) { i ->
                val ox = Random.nextFloat() * 60f - 30f
                val dy = -(60f + Random.nextFloat() * 120f)
                val size = Random.nextInt(4, 10)
                spawn(
                    cx + ox, cy + Random.nextFloat() * 20f, color, size, "oval",
                    dx = ox * 0.5f + Random.nextFloat() * 30f - 15f,
                    dy = dy,
                    duration = dur + Random.nextLong(0, 400),
                    interp = 2
                )
            }
        }
    }

    /** 向下坠落 */
    private fun falling(x: Float, y: Float, color: Int, count: Int, dur: Long) {
        repeat(count) { i ->
            val ox = Random.nextFloat() * 40f - 20f
            val size = Random.nextInt(3, 8)
            spawn(
                x + ox, y, color, size, "oval",
                dx = ox + Random.nextFloat() * 20f - 10f,
                dy = 100f + Random.nextFloat() * 200f,
                duration = dur + Random.nextLong(0, 500),
                interp = 1
            )
        }
    }

    /** 冲击波（扩散环） */
    private fun shockRing(anchor: View, color: Int, maxR: Float = 260f, dur: Long = 600L) {
        anchor.post {
            val (cx, cy) = anchorCenter(anchor)
            val size = 24
            val v = particleView(color, size, "ring")
            val lp = v.layoutParams as FrameLayout.LayoutParams
            lp.leftMargin = (cx - size / 2f).toInt()
            lp.topMargin  = (cy - size / 2f).toInt()
            v.layoutParams = lp
            v.scaleX = 0.2f; v.scaleY = 0.2f
            root.addView(v)
            v.animate()
                .scaleX(maxR / size * 2).scaleY(maxR / size * 2)
                .alpha(0f)
                .setDuration(dur)
                .setInterpolator(DecelerateInterpolator())
                .withEndAction { (v.parent as? ViewGroup)?.removeView(v) }
                .start()
        }
    }

    /** 螺旋上升 */
    private fun spiral(anchor: View, color: Int, count: Int, dur: Long) {
        anchor.post {
            val (cx, cy) = anchorCenter(anchor)
            repeat(count) { i ->
                val a0 = Random.nextDouble(0.0, Math.PI * 2)
                val r = 20f + Random.nextFloat() * 40f
                val size = Random.nextInt(4, 9)
                val dir = if (Random.nextBoolean()) 1f else -1f
                val v = particleView(color, size, "oval")
                val lp = v.layoutParams as FrameLayout.LayoutParams
                lp.leftMargin = cx.toInt(); lp.topMargin = cy.toInt()
                v.layoutParams = lp
                root.addView(v)

                // 用多段 animate 实现螺旋
                val steps = 8
                val stepDur = dur / steps
                for (s in 0 until steps) {
                    val ang = a0 + dir * (s + 1) * 0.9
                    val rr = r + (s + 1) * 8f
                    v.animate()
                        .translationX((cos(ang) * rr).toFloat())
                        .translationY((sin(ang) * rr).toFloat() - (s + 1) * 20f)
                        .alpha(1f - (s + 1f) / steps)
                        .scaleX(1f - (s + 1f) / steps)
                        .scaleY(1f - (s + 1f) / steps)
                        .setDuration(stepDur)
                        .setStartDelay((s * stepDur).toLong())
                        .setInterpolator(LinearInterpolator())
                        .withEndAction {
                            if (s == steps - 1) (v.parent as? ViewGroup)?.removeView(v)
                        }
                        .start()
                }
            }
        }
    }

    /** 随机闪烁 */
    private fun sparkle(anchor: View, color: Int, count: Int, dur: Long) {
        anchor.post {
            val (cx, cy) = anchorCenter(anchor)
            repeat(count) { i ->
                val ox = Random.nextFloat() * 120f - 60f
                val oy = Random.nextFloat() * 120f - 60f
                val size = Random.nextInt(6, 12)
                val v = particleView(color, size, "oval")
                val lp = v.layoutParams as FrameLayout.LayoutParams
                lp.leftMargin = (cx + ox).toInt(); lp.topMargin = (cy + oy).toInt()
                v.layoutParams = lp
                v.alpha = 0f
                root.addView(v)
                v.animate()
                    .alpha(1f).scaleX(1.4f).scaleY(1.4f)
                    .setDuration(160L)
                    .setStartDelay(Random.nextLong(0, dur / 2))
                    .withEndAction {
                        v.animate().alpha(0f).scaleX(0.2f).scaleY(0.2f)
                            .setDuration(220L)
                            .withEndAction { (v.parent as? ViewGroup)?.removeView(v) }
                            .start()
                    }
                    .start()
            }
        }
    }

    /** 光束 */
    private fun beam(from: View, to: View, color: Int, dur: Long = 300L) {
        val (fx, fy) = anchorCenter(from)
        val (tx, ty) = anchorCenter(to)
        val dx = tx - fx; val dy = ty - fy
        val len = kotlin.math.sqrt(dx * dx + dy * dy).toInt()
        val angle = Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
        val v = View(context).apply {
            layoutParams = FrameLayout.LayoutParams(len, 6)
            background = GradientDrawable().apply {
                gradientType = GradientDrawable.LINEAR_GRADIENT
                orientation = GradientDrawable.Orientation.LEFT_RIGHT
                colors = intArrayOf(color, color and 0x00FFFFFF, color)
            }
        }
        val lp = v.layoutParams as FrameLayout.LayoutParams
        lp.leftMargin = fx.toInt(); lp.topMargin = (fy - 3).toInt()
        v.layoutParams = lp
        v.pivotX = 0f; v.pivotY = 3f
        v.rotation = angle
        v.alpha = 0f
        root.addView(v)
        v.animate().alpha(1f).setDuration(dur / 3)
            .withEndAction {
                v.animate().alpha(0f).setDuration(dur * 2 / 3)
                    .withEndAction { (v.parent as? ViewGroup)?.removeView(v) }
                    .start()
            }.start()
    }

    // ================= 场景动画 =================

    /** 抽牌：从卡组区飞向手牌 */
    fun drawCard(from: View, to: View, color: Int = Pal.CYAN) {
        val (fx, fy) = anchorCenter(from)
        val (tx, ty) = anchorCenter(to)
        val v = particleView(color, 24, "ring")
        val lp = v.layoutParams as FrameLayout.LayoutParams
        lp.leftMargin = (fx - 12).toInt(); lp.topMargin = (fy - 12).toInt()
        v.layoutParams = lp
        root.addView(v)
        v.animate()
            .translationX(tx - fx).translationY(ty - fy)
            .scaleX(1.4f).scaleY(1.4f)
            .alpha(0.3f)
            .setDuration(420L)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                spawn(tx, ty, color, 20, "ring", 0f, 0f,
                    sFrom = 0.6f, sTo = 2.2f, aFrom = 1f, aTo = 0f,
                    duration = 300L, interp = 2)
                (v.parent as? ViewGroup)?.removeView(v)
            }.start()
        // 拖尾
        repeat(8) { i ->
            spawn(fx, fy, color, 6, "oval",
                dx = (tx - fx) * (i + 1f) / 9f + Random.nextFloat() * 20f - 10f,
                dy = (ty - fy) * (i + 1f) / 9f + Random.nextFloat() * 20f - 10f,
                duration = 380L, delay = (i * 30).toLong())
        }
    }

    /** 送入墓地：从场上飞向墓地 + 暗色粒子 */
    fun sendToGrave(from: View, to: View) {
        val (fx, fy) = anchorCenter(from)
        val (tx, ty) = anchorCenter(to)
        val v = particleView(Pal.PURPLE, 30, "rect")
        val lp = v.layoutParams as FrameLayout.LayoutParams
        lp.leftMargin = (fx - 15).toInt(); lp.topMargin = (fy - 15).toInt()
        v.layoutParams = lp
        root.addView(v)
        v.animate()
            .translationX(tx - fx).translationY(ty - fy)
            .rotation(360f)
            .alpha(0.2f)
            .setDuration(520L)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                burst(to, Pal.PURPLE, 15, 0.6f, 600)
                (v.parent as? ViewGroup)?.removeView(v)
            }.start()
    }

    /** LP 变化：冲击波 + 数字抖动 */
    fun lpChange(anchor: View, isHeal: Boolean) {
        val color = if (isHeal) Pal.CYAN else Pal.RED
        shockRing(anchor, color, 240f, 600L)
        burst(anchor, color, 30, 0.7f, 800)
        // 数字抖动
        anchor.animate()
            .translationX(8f).setDuration(50L)
            .withEndAction {
                anchor.animate().translationX(-8f).setDuration(50L)
                    .withEndAction {
                        anchor.animate().translationX(4f).setDuration(50L)
                            .withEndAction {
                                anchor.animate().translationX(0f).setDuration(50L).start()
                            }.start()
                    }.start()
            }.start()
    }

    /** 回合切换：全屏扫描线 */
    fun turnChange() {
        val line = View(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, 4
            )
            setBackgroundColor(Pal.CYAN)
        }
        val lp = line.layoutParams as FrameLayout.LayoutParams
        lp.topMargin = 0
        line.layoutParams = lp
        line.alpha = 0f
        root.addView(line)
        line.animate()
            .alpha(0.9f).setDuration(100L)
            .withEndAction {
                line.animate()
                    .translationY(root.height.toFloat())
                    .alpha(0f)
                    .setDuration(500L)
                    .setInterpolator(AccelerateInterpolator())
                    .withEndAction { (line.parent as? ViewGroup)?.removeView(line) }
                    .start()
            }.start()
    }

    /** 阶段切换：光带扫过 */
    fun phaseChange() {
        repeat(2) { i ->
            val line = View(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, 8
                )
                setBackgroundColor(if (i == 0) Pal.PINK else Pal.CYAN)
            }
            line.alpha = 0f
            root.addView(line)
            line.animate()
                .alpha(0.8f).setDuration(150L)
                .withEndAction {
                    line.animate()
                        .translationY(root.height.toFloat())
                        .alpha(0f)
                        .setDuration(700L + i * 200L)
                        .setStartDelay((i * 100).toLong())
                        .setInterpolator(AccelerateInterpolator())
                        .withEndAction { (line.parent as? ViewGroup)?.removeView(line) }
                        .start()
                }.start()
        }
    }

    /** 场地魔法发动：全屏光环 + 上升粒子 */
    fun fieldSpellActivate(anchor: View) {
        shockRing(anchor, Pal.GOLD, root.width.toFloat() * 1.2f, 1200L)
        shockRing(anchor, Pal.PINK, root.width.toFloat() * 0.9f, 1000L)
        rising(anchor, Pal.GOLD, 50, 1500L)
        sparkle(anchor, Pal.PINK, 40, 800L)
    }

    /** 额外怪兽召唤：强化版 */
    fun extraSummon(anchor: View) {
        burst(anchor, Pal.PURPLE, 80, 1.2f, 1200)
        burst(anchor, Pal.GOLD, 40, 0.8f, 1000)
        burst(anchor, Pal.PINK, 30, 0.6f, 900)
        spiral(anchor, Pal.CYAN, 30, 1500L)
        shockRing(anchor, Pal.GOLD, 300f, 800L)
        shockRing(anchor, Pal.PURPLE, 240f, 900L)
    }

    /** 胜利：金色雨 */
    fun victory() {
        val w = root.width.toFloat()
        repeat(200) { i ->
            val x = Random.nextFloat() * w
            val y = -Random.nextFloat() * 100f
            val color = if (Random.nextBoolean()) Pal.GOLD else Pal.CYAN
            spawn(
                x, y, color, Random.nextInt(4, 10), "oval",
                dx = Random.nextFloat() * 60f - 30f,
                dy = root.height.toFloat() + 100f,
                aFrom = 1f, aTo = 0.8f,
                duration = 2000L + Random.nextLong(0, 1500),
                delay = (i * 8).toLong(),
                interp = 1
            )
        }
    }

    /** 失败：红色碎片风暴 */
    fun defeat() {
        val w = root.width.toFloat()
        val h = root.height.toFloat()
        val cx = w / 2; val cy = h / 2
        repeat(180) { i ->
            val a = Random.nextDouble(0.0, Math.PI * 2)
            val d = Random.nextFloat() * 400f + 100f
            val color = if (Random.nextBoolean()) Pal.RED else Pal.ORANGE
            spawn(
                cx, cy, color, Random.nextInt(6, 16), "rect",
                dx = (cos(a) * d).toFloat(),
                dy = (sin(a) * d).toFloat(),
                rotTo = Random.nextFloat() * 720f - 360f,
                duration = 1200L + Random.nextLong(0, 800),
                delay = (i * 5).toLong(),
                interp = 2
            )
        }
        shockRing(root as View, Pal.RED, w, 1200L)
    }

    // ================= 现有 API（增强） =================

    fun particleSummon(anchor: View) {
        burst(anchor, Pal.CYAN, 40, 0.8f, 1000)
        burst(anchor, Pal.PINK, 25, 0.5f, 900)
        rising(anchor, Pal.CYAN, 20, 1200L)
        sparkle(anchor, Pal.WHITE, 15, 600L)
    }

    fun particleAttack(anchor: View) {
        burst(anchor, Pal.WHITE, 40, 1.2f, 500)
        burst(anchor, Pal.CYAN, 20, 0.8f, 600)
    }

    fun particleDestroy(anchor: View) {
        burst(anchor, Pal.RED, 80, 1.0f, 1200)
        burst(anchor, Pal.GOLD, 40, 0.7f, 1000)
        burst(anchor, Pal.ORANGE, 30, 0.5f, 900)
        shockRing(anchor, Pal.RED, 260f, 700L)
    }

    fun particleEffect(anchor: View) {
        burst(anchor, Pal.PINK, 60, 0.7f, 1200)
        burst(anchor, Pal.CYAN, 30, 0.9f, 1000)
        spiral(anchor, Pal.PINK, 20, 1200L)
        sparkle(anchor, Pal.GOLD, 25, 700L)
    }

    fun particleSelect(anchor: View) {
        burst(anchor, Pal.GOLD, 30, 0.5f, 800)
        sparkle(anchor, Pal.GOLD, 10, 400L)
    }

    fun damagePopup(x: Float, y: Float, amount: Int, heal: Boolean = false) {
        val color = if (heal) Pal.CYAN else Pal.RED
        val tv = TextView(context).apply {
            text = if (heal) "+$amount" else "-$amount"
            setTextColor(color)
            textSize = 30f
            setShadowLayer(14f, 0f, 0f, Color.BLACK)
            gravity = Gravity.CENTER
            typeface = android.graphics.Typeface.MONOSPACE
        }
        tv.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            leftMargin = x.toInt()
            topMargin = y.toInt()
        }
        root.addView(tv)
        tv.animate()
            .translationY(-180f).alpha(0f).scaleX(1.5f).scaleY(1.5f)
            .setDuration(1100L)
            .withEndAction { (tv.parent as? ViewGroup)?.removeView(tv) }
            .start()
        // 数字旁粒子
        falling(x + 40, y + 30, color, 12, 800L)
    }

    // ================= 3D 卡牌 =================

    fun pressDown(view: View) {
        view.animate()
            .scaleX(0.94f).scaleY(0.94f).rotationX(8f)
            .setDuration(80L).start()
    }

    fun pressUp(view: View) {
        view.animate()
            .scaleX(1f).scaleY(1f).rotationX(0f)
            .setInterpolator(OvershootInterpolator(2.8f))
            .setDuration(220L).start()
    }

    fun flip3D(view: View) {
        view.rotationX = -90f
        view.alpha = 0f
        view.animate()
            .rotationX(0f).alpha(1f)
            .setDuration(480L)
            .setInterpolator(OvershootInterpolator(1.4f))
            .start()
        view.postDelayed({
            particleSummon(view)
            shockRing(view, Pal.CYAN, 240f, 700L)
        }, 240L)
    }

    fun summon(view: View) {
        view.scaleX = 0.3f; view.scaleY = 0.3f
        view.alpha = 0f; view.translationY = 80f; view.rotationX = -45f
        view.animate()
            .scaleX(1f).scaleY(1f).alpha(1f).translationY(0f).rotationX(0f)
            .setDuration(520L)
            .setInterpolator(OvershootInterpolator(1.6f))
            .withEndAction { particleSummon(view) }
            .start()
    }

    fun attack(attacker: View, target: View, onHit: () -> Unit, onEnd: () -> Unit) {
        val aLoc = IntArray(2); attacker.getLocationOnScreen(aLoc)
        val tLoc = IntArray(2); target.getLocationOnScreen(tLoc)
        val dx = (tLoc[0] - aLoc[0]).toFloat()
        val dy = (tLoc[1] - aLoc[1]).toFloat()

        // 残影拖尾
        repeat(6) { i ->
            val ghost = particleView(Pal.WHITE, 20, "oval")
            val lp = ghost.layoutParams as FrameLayout.LayoutParams
            lp.leftMargin = aLoc[0]; lp.topMargin = aLoc[1]
            ghost.layoutParams = lp
            ghost.alpha = 0.5f
            root.addView(ghost)
            ghost.animate()
                .translationX(dx * (i + 1) / 8f).translationY(dy * (i + 1) / 8f)
                .alpha(0f).scaleX(0.4f).scaleY(0.4f)
                .setDuration(500L).setStartDelay((i * 40).toLong())
                .withEndAction { (ghost.parent as? ViewGroup)?.removeView(ghost) }
                .start()
        }

        attacker.animate()
            .translationX(-dx * 0.15f).translationY(-dy * 0.15f)
            .rotationX(14f).setDuration(140L)
            .withEndAction {
                attacker.animate()
                    .translationX(dx * 0.7f).translationY(dy * 0.7f)
                    .rotationX(-10f).setDuration(180L)
                    .withEndAction {
                        onHit()
                        particleAttack(target)
                        shockRing(target, Pal.WHITE, 200f, 500L)
                        attacker.animate()
                            .translationX(0f).translationY(0f).rotationX(0f)
                            .setDuration(240L)
                            .withEndAction { onEnd() }
                            .start()
                    }.start()
            }.start()
    }

    fun attackDirect(attacker: View, targetX: Float, targetY: Float, onHit: () -> Unit, onEnd: () -> Unit) {
        val aLoc = IntArray(2); attacker.getLocationOnScreen(aLoc)
        val dx = targetX - aLoc[0]
        val dy = targetY - aLoc[1]

        attacker.animate()
            .translationX(-dx * 0.12f).translationY(-dy * 0.12f)
            .setDuration(140L)
            .withEndAction {
                attacker.animate()
                    .translationX(dx * 0.45f).translationY(dy * 0.45f)
                    .setDuration(180L)
                    .withEndAction {
                        onHit()
                        particleAttack(attacker)
                        sparkle(attacker, Pal.RED, 30, 500L)
                        attacker.animate()
                            .translationX(0f).translationY(0f)
                            .setDuration(240L)
                            .withEndAction { onEnd() }
                            .start()
                    }.start()
            }.start()
    }

    fun destroy(view: View, onEnd: () -> Unit) {
        screenShake()
        particleDestroy(view)
        val shake = ObjectAnimator.ofFloat(view, "translationX",
            0f, 24f, -24f, 18f, -18f, 10f, -10f, 0f)
        shake.duration = 400L
        shake.start()
        view.animate()
            .alpha(0f).scaleX(0.1f).scaleY(0.1f)
            .rotationY(360f).rotationX(90f)
            .setDuration(480L)
            .withEndAction {
                onEnd()
                view.alpha = 1f; view.scaleX = 1f; view.scaleY = 1f
                view.rotationY = 0f; view.rotationX = 0f; view.translationX = 0f
            }.start()
    }

    fun effect(view: View, effectName: String = "") {
        val px = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.25f, 1f, 1.15f, 1f)
        val py = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.25f, 1f, 1.15f, 1f)
        px.duration = 800L; py.duration = 800L
        px.start(); py.start()
        view.animate().translationY(-30f).rotationX(10f).setDuration(200L).start()
        particleEffect(view)
        shockRing(view, Pal.PINK, 220f, 700L)
        if (effectName.isNotEmpty()) {
            val tv = TextView(context).apply {
                text = effectName
                setTextColor(Pal.GOLD)
                textSize = 22f
                setShadowLayer(14f, 0f, 0f, Color.BLACK)
                typeface = android.graphics.Typeface.MONOSPACE
            }
            val (cx, cy) = anchorCenter(view)
            tv.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = (cx - 60).toInt(); topMargin = (cy - 80).toInt()
            }
            root.addView(tv)
            tv.animate().translationY(-120f).alpha(0f).scaleX(1.4f).scaleY(1.4f)
                .setDuration(1200L)
                .withEndAction { (tv.parent as? ViewGroup)?.removeView(tv) }
                .start()
        }
        view.postDelayed({
            view.animate().translationY(0f).rotationX(0f).setDuration(200L).start()
        }, 800)
    }

    fun screenShake() {
        val anim = ObjectAnimator.ofFloat(root, "translationX",
            0f, 24f, -24f, 15f, -15f, 0f)
        anim.duration = 320L
        anim.start()
    }

    fun selected(view: View) {
        view.animate().rotationX(-8f).scaleX(1.06f).scaleY(1.06f)
            .setDuration(180L).start()
        particleSelect(view)
    }

    fun deselected(view: View) {
        view.animate().rotationX(0f).scaleX(1f).scaleY(1f)
            .setDuration(180L).start()
    }

    // ================= 环境粒子 =================

    private var ambientRunning = false
    private var ambientTask: Runnable? = null

    fun ambientStart() {
        if (ambientRunning) return
        ambientRunning = true
        val w = { if (root.width > 0) root.width else 1080 }
        val h = { if (root.height > 0) root.height else 1920 }

        ambientTask = object : Runnable {
            override fun run() {
                if (!ambientRunning) return
                repeat(3) {
                    val x = Random.nextFloat() * w()
                    spawn(
                        x, -20f, Pal.rand(), Random.nextInt(3, 7), "oval",
                        dx = Random.nextFloat() * 60f - 30f,
                        dy = h() + 40f,
                        aFrom = 0.7f, aTo = 0.5f,
                        duration = 5000L + Random.nextLong(0, 3000),
                        interp = 0
                    )
                }
                root.postDelayed(this, 400L)
            }
        }
        root.post(ambientTask!!)
    }

    fun ambientStop() {
        ambientRunning = false
        ambientTask?.let { root.removeCallbacks(it) }
        ambientTask = null
    }

    /** 卡牌呼吸（微妙上下浮动） */
    fun startCardIdle(view: View) {
        val dur = 1800L + Random.nextLong(0, 800)
        val amp = 4f + Random.nextFloat() * 4f
        view.animate()
            .translationY(-amp).setDuration(dur)
            .withEndAction {
                view.animate().translationY(amp).setDuration(dur)
                    .withEndAction { startCardIdle(view) }
                    .start()
            }.start()
    }
}
