package com.example.hellocard

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import android.widget.ImageView
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import java.util.concurrent.Executors

/**
 * 轻量异步卡图加载器。
 *
 * 替代原先在主线程 `BitmapFactory.decodeStream` 的同步加载方式：
 * 后台线程解码 → LruCache 缓存 → 主线程回填，避免手牌/场上卡牌较多时掉帧。
 */
object CardImageLoader {

    private val executor = Executors.newFixedThreadPool(2)
    private val main = Handler(Looper.getMainLooper())

    /** 以最大堆内存的 1/8 为上限的位图缓存 */
    private val cache: LruCache<String, Bitmap> =
        object : LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 8).toInt()) {
            override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
        }

    fun load(context: Context, target: ImageView, path: String, sizePx: Int, radius: Float = 0f) {
        if (path.isBlank()) return
        val key = "$path@$sizePx"
        // 标记当前视图正在等待的图片，避免视图复用后错位显示
        target.tag = key

        cache.get(key)?.let { bmp ->
            applyBitmap(context, target, bmp, radius)
            return
        }

        val appContext = context.applicationContext
        executor.execute {
            // 双重检查：排队期间可能已被其他任务解码并写入缓存
            val bmp = cache.get(key) ?: decode(appContext, path, sizePx)
            if (bmp == null) return@execute
            cache.put(key, bmp)
            main.post {
                if (target.tag == key) applyBitmap(appContext, target, bmp, radius)
            }
        }
    }

    private fun decode(context: Context, path: String, sizePx: Int): Bitmap? {
        val decoded = runCatching {
            context.assets.open(path).use { BitmapFactory.decodeStream(it) }
        }.getOrNull() ?: return null
        return if (decoded.width == sizePx && decoded.height == sizePx) decoded
        else Bitmap.createScaledBitmap(decoded, sizePx, sizePx, true)
    }

    private fun applyBitmap(context: Context, target: ImageView, bmp: Bitmap, radius: Float) {
        if (radius > 0f) {
            val d = RoundedBitmapDrawableFactory.create(context.resources, bmp)
            d.cornerRadius = radius
            // RoundedBitmapDrawable 只有 setAntiAlias，没有 getter，不能用属性语法
            d.setAntiAlias(true)
            target.setImageDrawable(d)
        } else {
            target.setImageBitmap(bmp)
        }
    }
}
