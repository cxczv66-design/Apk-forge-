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
import android.util.Base64
import com.example.domain.model.AppProject
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

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
     * يولّد حزمة أيقونات Android الكاملة باستخدام صورة التطبيق الحقيقية (من المعرض أو رابط أو Base64) إن وجدت،
     * بما في ذلك drawable-nodpi/ic_launcher_foreground.png للأندرويد الحديث (API 26+) وأيقونة شاشة البداية.
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
        """.trimIndent().trimStart()

        val bgDrawableXml = """
            <?xml version="1.0" encoding="utf-8"?>
            <shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
                <solid android:color="$bgHex" />
            </shape>
        """.trimIndent().trimStart()

        // Only used when no custom user image is selected; when customBitmap != null,
        // Android loads drawable-nodpi/ic_launcher_foreground.png directly without conflict.
        val fgVectorXml = if (customBitmap != null) {
            ""
        } else {
            """
                <?xml version="1.0" encoding="utf-8"?>
                <vector xmlns:android="http://schemas.android.com/apk/res/android"
                    android:width="108dp"
                    android:height="108dp"
                    android:viewportWidth="108"
                    android:viewportHeight="108">
                    <path
                        android:fillColor="$accentHex"
                        android:pathData="M54,22 C71.67,22 86,36.33 86,54 C86,71.67 71.67,86 54,86 C36.33,86 22,71.67 22,54 C22,36.33 36.33,22 54,22 Z" />
                    <path
                        android:fillColor="#FFFFFF"
                        android:pathData="M45,38 L67,54 L45,70 Z" />
                </vector>
            """.trimIndent().trimStart()
        }

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

        if (customBitmap != null) {
            // High-res 432x432 foreground layer for Android 8+ adaptive icons (mipmap-anydpi-v26)
            pngs["app/src/main/res/drawable-nodpi/ic_launcher_foreground.png"] = renderAdaptiveForegroundPngBytes(
                sizePx = 432,
                sourceBitmap = customBitmap
            )
        }

        // Dedicated crisp app logo PNG for Splash Screen & Navigation Drawer inside the built APK
        pngs["app/src/main/res/drawable-nodpi/ic_splash_logo.png"] = renderIconPngBytes(
            sizePx = 256,
            bgHex = bgHex,
            accentHex = accentHex,
            shape = shape,
            monogram = initial,
            sourceBitmap = customBitmap
        )

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
                clean.startsWith("data:image", ignoreCase = true) -> {
                    val b64 = clean.substringAfter("base64,", "")
                    if (b64.isNotBlank()) {
                        val decoded = Base64.decode(b64, Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                    } else null
                }
                clean.startsWith("http://", ignoreCase = true) || clean.startsWith("https://", ignoreCase = true) -> {
                    val cacheDir = File(context.filesDir, "app_icons").apply { mkdirs() }
                    val cachedFile = File(cacheDir, "url_icon_${clean.hashCode().toUInt()}.png")
                    if (cachedFile.exists() && cachedFile.length() > 0L) {
                        BitmapFactory.decodeFile(cachedFile.absolutePath)
                    } else {
                        val conn = (URL(clean).openConnection() as HttpURLConnection).apply {
                            connectTimeout = 4000
                            readTimeout = 4000
                            instanceFollowRedirects = true
                            setRequestProperty("User-Agent", "ApkForge-IconFetcher/2.0")
                        }
                        try {
                            if (conn.responseCode in 200..299) {
                                val bytes = conn.inputStream.use { it.readBytes() }
                                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                if (bmp != null) {
                                    cachedFile.writeBytes(bytes)
                                }
                                bmp
                            } else null
                        } finally {
                            conn.disconnect()
                        }
                    }
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun renderAdaptiveForegroundPngBytes(
        sizePx: Int,
        sourceBitmap: Bitmap
    ): ByteArray {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        // Draw cleanly inside 82% of the 108dp adaptive layer (~88dp) with rounded corners
        // so the full custom photo remains clear and centered across circle, squircle, and teardrop launcher masks.
        val inset = sizePx * 0.14f
        val dstRect = RectF(inset, inset, sizePx - inset, sizePx - inset)
        val clip = Path().apply {
            addRoundRect(dstRect, sizePx * 0.14f, sizePx * 0.14f, Path.Direction.CW)
        }
        canvas.save()
        canvas.clipPath(clip)
        val minSide = minOf(sourceBitmap.width, sourceBitmap.height)
        val srcLeft = (sourceBitmap.width - minSide) / 2
        val srcTop = (sourceBitmap.height - minSide) / 2
        val srcRect = Rect(srcLeft, srcTop, srcLeft + minSide, srcTop + minSide)
        canvas.drawBitmap(sourceBitmap, srcRect, dstRect, paint)
        canvas.restore()

        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 96, baos)
        bitmap.recycle()
        return baos.toByteArray()
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
