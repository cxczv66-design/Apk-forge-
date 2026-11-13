package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import com.example.domain.model.AppProject
import java.io.ByteArrayOutputStream
import java.io.File

data class GeneratedIconPack(
    val adaptiveXml: String,
    val backgroundDrawableXml: String,
    val foregroundVectorXml: String,
    val densityPngs: Map<String, ByteArray>,
    val usedCustomImage: Boolean = false
)

object AdaptiveIconStudio {

    private val densities = mapOf(
        "mipmap-mdpi" to 48,
        "mipmap-hdpi" to 72,
        "mipmap-xhdpi" to 96,
        "mipmap-xxhdpi" to 144,
        "mipmap-xxxhdpi" to 192
    )

    fun generateIconPack(project: AppProject): GeneratedIconPack {
        return generateIconPackInternal(customBitmap = null, project = project)
    }

    /**
     * يولّد حزمة أيقونات Android الكاملة باستخدام صورة التطبيق الحقيقية (من المعرض أو رابط) إن وجدت.
     */
    fun generateIconPack(context: Context, project: AppProject): GeneratedIconPack {
        val decoded = decodeProjectIconBitmap(context, project.iconImageUri)
        return try {
            generateIconPackInternal(customBitmap = decoded, project = project)
        } finally {
            decoded?.recycle()
        }
    }

    private fun generateIconPackInternal(
        customBitmap: Bitmap?,
        project: AppProject
    ): GeneratedIconPack {
        val bgHex = project.proConfig.iconBgColorHex.ifBlank { project.accentColorHex }
        val accentHex = project.accentColorHex
        val shape = project.proConfig.iconShape
        val initial = project.name.trim().take(2).uppercase().ifBlank { "AF" }

        val adaptiveXml = """
            <?xml version="1.0" encoding="utf-8"?>
            <adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
                <background android:drawable="@drawable/ic_launcher_background" />
                <foreground android:drawable="@drawable/ic_launcher_foreground" />
            </adaptive-icon>
        """.trimIndent()

        val bgDrawableXml = """
            <?xml version="1.0" encoding="utf-8"?>
            <shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
                <solid android:color="$bgHex" />
            </shape>
        """.trimIndent()

        val fgVectorXml = """
            <vector xmlns:android="http://schemas.android.com/apk/res/android"
                android:width="108dp"
                android:height="108dp"
                android:viewportWidth="108"
                android:viewportHeight="108">
                <path
                    android:fillColor="$accentHex"
                    android:pathData="M54,20 C72.77,20 88,35.23 88,54 C88,72.77 72.77,88 54,88 C35.23,88 20,72.77 20,54 C20,35.23 35.23,20 54,20 Z" />
                <path
                    android:fillColor="#FFFFFF"
                    android:pathData="M48,38 L66,54 L48,70 L54,54 Z" />
            </vector>
        """.trimIndent()

        val pngs = linkedMapOf<String, ByteArray>()
        densities.forEach { (folder, px) ->
            pngs["app/src/main/res/$folder/ic_launcher.png"] = renderIconPngBytes(
                sizePx = px,
                bgHex = bgHex,
                accentHex = accentHex,
                shape = shape,
                monogram = initial,
                sourceBitmap = customBitmap
            )
            pngs["app/src/main/res/$folder/ic_launcher_round.png"] = renderIconPngBytes(
                sizePx = px,
                bgHex = bgHex,
                accentHex = accentHex,
                shape = "circle",
                monogram = initial,
                sourceBitmap = customBitmap
            )
        }

        return GeneratedIconPack(
            adaptiveXml = adaptiveXml,
            backgroundDrawableXml = bgDrawableXml,
            foregroundVectorXml = fgVectorXml,
            densityPngs = pngs,
            usedCustomImage = customBitmap != null
        )
    }

    private fun decodeProjectIconBitmap(context: Context, rawUri: String?): Bitmap? {
        val clean = rawUri?.trim()?.takeIf { it.isNotBlank() && it != "preset_anime" } ?: return null
        return try {
            when {
                clean.startsWith("file://") -> {
                    val path = Uri.parse(clean).path ?: return null
                    if (File(path).exists()) BitmapFactory.decodeFile(path) else null
                }
                clean.startsWith("/") -> {
                    if (File(clean).exists()) BitmapFactory.decodeFile(clean) else null
                }
                clean.startsWith("content://") -> {
                    context.contentResolver.openInputStream(Uri.parse(clean))?.use {
                        BitmapFactory.decodeStream(it)
                    }
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun renderIconPngBytes(
        sizePx: Int,
        bgHex: String,
        accentHex: String,
        shape: String,
        monogram: String,
        sourceBitmap: Bitmap?
    ): ByteArray {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val bgColor = parseColorInt(bgHex, 0xFF1A1E29.toInt())
        val accentColor = parseColorInt(accentHex, 0xFFFF7A2F.toInt())

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val rect = RectF(0f, 0f, sizePx.toFloat(), sizePx.toFloat())
        val clipPath = Path()
        when (shape) {
            "circle" -> clipPath.addOval(rect, Path.Direction.CW)
            "rounded" -> clipPath.addRoundRect(rect, sizePx * 0.22f, sizePx * 0.22f, Path.Direction.CW)
            "teardrop" -> {
                val r = sizePx * 0.45f
                clipPath.addRoundRect(rect, floatArrayOf(r, r, r, r, sizePx * 0.14f, sizePx * 0.14f, r, r), Path.Direction.CW)
            }
            else -> clipPath.addRoundRect(rect, sizePx * 0.34f, sizePx * 0.34f, Path.Direction.CW)
        }
        canvas.clipPath(clipPath)

        paint.color = bgColor
        canvas.drawRect(rect, paint)

        if (sourceBitmap != null && sourceBitmap.width > 0 && sourceBitmap.height > 0) {
            // Center-crop real user icon cleanly into the icon frame
            val minSide = minOf(sourceBitmap.width, sourceBitmap.height)
            val srcLeft = (sourceBitmap.width - minSide) / 2
            val srcTop = (sourceBitmap.height - minSide) / 2
            val srcRect = Rect(srcLeft, srcTop, srcLeft + minSide, srcTop + minSide)
            canvas.drawBitmap(sourceBitmap, srcRect, rect, paint)
        } else {
            paint.color = accentColor
            paint.alpha = 52
            canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx * 0.36f, paint)

            paint.alpha = 255
            paint.color = 0xFFFFFFFF.toInt()
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = sizePx * 0.36f
            paint.isFakeBoldText = true
            val yPos = (sizePx / 2f) - ((paint.descent() + paint.ascent()) / 2f)
            canvas.drawText(monogram, sizePx / 2f, yPos, paint)
        }

        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 95, baos)
        bitmap.recycle()
        return baos.toByteArray()
    }

    private fun parseColorInt(hex: String, fallback: Int): Int {
        return try {
            android.graphics.Color.parseColor(hex)
        } catch (_: Exception) {
            fallback
        }
    }
}
