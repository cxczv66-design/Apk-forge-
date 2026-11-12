package com.example.engine

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

data class ExtractedAssetItem(
    val relativePath: String,
    val sizeBytes: Long,
    val isDirectory: Boolean,
    val extension: String
)

data class ImportedWorkspaceResult(
    val workspaceId: String,
    val displayFileName: String,
    val workspaceDirPath: String,
    val entryHtmlPath: String,
    val detectedTitle: String,
    val assets: List<ExtractedAssetItem>,
    val totalSizeBytes: Long
)

/**
 * محرك معالجة واستخراج ملفات HTML وأرشيفات ZIP الحقيقية محلياً
 * لعرضها بأعلى جودة في شاشة المعاينة والمحاكي التفاعلي، وحفظ أيقونات التطبيق بشكل دائم.
 */
object LocalWorkspaceEngine {

    fun getDisplayName(context: Context, uri: Uri, fallback: String): String {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst()) {
                    cursor.getString(idx)?.takeIf { it.isNotBlank() }
                } else null
            } ?: uri.lastPathSegment?.substringAfterLast('/') ?: fallback
        } catch (_: Exception) {
            uri.lastPathSegment?.substringAfterLast('/') ?: fallback
        }
    }

    suspend fun importHtmlUri(context: Context, uri: Uri): Result<ImportedWorkspaceResult> = withContext(Dispatchers.IO) {
        try {
            val fileName = getDisplayName(context, uri, "index.html")
            val workspaceId = "ws_html_${System.currentTimeMillis()}"
            val workspaceDir = File(context.filesDir, "imported_workspaces/$workspaceId").apply { mkdirs() }
            val safeEntryName = if (fileName.endsWith(".html", true) || fileName.endsWith(".htm", true)) {
                "index.html"
            } else {
                "index.html"
            }
            val targetFile = File(workspaceDir, safeEntryName)
            var sizeBytes = 0L
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { out ->
                    sizeBytes = input.copyTo(out)
                }
            } ?: return@withContext Result.failure(IllegalStateException("تعذر فتح ملف HTML من الذاكرة"))

            val htmlText = targetFile.readText(Charsets.UTF_8).take(120_000)
            val title = extractHtmlTitle(htmlText) ?: fileName.substringBeforeLast('.')

            Result.success(
                ImportedWorkspaceResult(
                    workspaceId = workspaceId,
                    displayFileName = fileName,
                    workspaceDirPath = workspaceDir.absolutePath,
                    entryHtmlPath = targetFile.absolutePath,
                    detectedTitle = title,
                    assets = listOf(
                        ExtractedAssetItem(
                            relativePath = fileName,
                            sizeBytes = sizeBytes,
                            isDirectory = false,
                            extension = fileName.substringAfterLast('.', "html").lowercase()
                        )
                    ),
                    totalSizeBytes = sizeBytes
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importZipUri(context: Context, uri: Uri): Result<ImportedWorkspaceResult> = withContext(Dispatchers.IO) {
        try {
            val fileName = getDisplayName(context, uri, "website.zip")
            val workspaceId = "ws_zip_${System.currentTimeMillis()}"
            val workspaceDir = File(context.filesDir, "imported_workspaces/$workspaceId").apply { mkdirs() }
            val canonicalRoot = workspaceDir.canonicalPath + File.separator

            val extractedAssets = mutableListOf<ExtractedAssetItem>()
            var totalBytes = 0L

            context.contentResolver.openInputStream(uri)?.use { rawIn ->
                ZipInputStream(rawIn).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val cleanName = entry.name.replace('\\', '/').trimStart('/')
                        if (cleanName.isNotBlank() && !cleanName.startsWith("__MACOSX")) {
                            val outFile = File(workspaceDir, cleanName)
                            val canonicalOut = outFile.canonicalPath
                            if (canonicalOut.startsWith(canonicalRoot)) {
                                if (entry.isDirectory) {
                                    outFile.mkdirs()
                                    extractedAssets.add(
                                        ExtractedAssetItem(cleanName, 0L, isDirectory = true, extension = "dir")
                                    )
                                } else {
                                    outFile.parentFile?.mkdirs()
                                    var written = 0L
                                    FileOutputStream(outFile).use { fos ->
                                        written = zis.copyTo(fos)
                                    }
                                    totalBytes += written
                                    extractedAssets.add(
                                        ExtractedAssetItem(
                                            relativePath = cleanName,
                                            sizeBytes = written,
                                            isDirectory = false,
                                            extension = cleanName.substringAfterLast('.', "").lowercase()
                                        )
                                    )
                                }
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            } ?: return@withContext Result.failure(IllegalStateException("تعذر فتح ملف ZIP"))

            val entryHtml = findEntryHtml(workspaceDir)
                ?: return@withContext Result.failure(IllegalStateException("الأرشيف لا يحتوي على ملف HTML للعرض"))

            val title = extractHtmlTitle(entryHtml.readText(Charsets.UTF_8).take(120_000))
                ?: fileName.substringBeforeLast('.')

            Result.success(
                ImportedWorkspaceResult(
                    workspaceId = workspaceId,
                    displayFileName = fileName,
                    workspaceDirPath = workspaceDir.absolutePath,
                    entryHtmlPath = entryHtml.absolutePath,
                    detectedTitle = title,
                    assets = extractedAssets.sortedWith(compareBy<ExtractedAssetItem> { !it.isDirectory }.thenBy { it.relativePath }),
                    totalSizeBytes = totalBytes
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun findEntryHtml(workspaceDir: File): File? {
        if (!workspaceDir.exists()) return null
        val rootIndex = File(workspaceDir, "index.html")
        if (rootIndex.exists() && rootIndex.isFile) return rootIndex

        val rootHtm = File(workspaceDir, "index.htm")
        if (rootHtm.exists() && rootHtm.isFile) return rootHtm

        val allHtml = workspaceDir.walkTopDown()
            .filter { it.isFile && (it.extension.equals("html", true) || it.extension.equals("htm", true)) }
            .sortedBy { it.relativeTo(workspaceDir).path.count { ch -> ch == '/' || ch == '\\' } }
            .toList()

        return allHtml.firstOrNull { it.name.equals("index.html", true) } ?: allHtml.firstOrNull()
    }

    fun listWorkspaceAssets(workspacePath: String?): List<ExtractedAssetItem> {
        if (workspacePath.isNullOrBlank()) return emptyList()
        val dir = File(workspacePath)
        if (!dir.exists() || !dir.isDirectory) return emptyList()
        return dir.walkTopDown()
            .drop(1)
            .take(150)
            .map { file ->
                ExtractedAssetItem(
                    relativePath = file.relativeTo(dir).path.replace('\\', '/'),
                    sizeBytes = if (file.isDirectory) 0L else file.length(),
                    isDirectory = file.isDirectory,
                    extension = if (file.isDirectory) "dir" else file.extension.lowercase()
                )
            }
            .sortedWith(compareBy<ExtractedAssetItem> { !it.isDirectory }.thenBy { it.relativePath })
            .toList()
    }

    suspend fun persistImageUriToInternalFile(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.filesDir, "app_icons").apply { mkdirs() }
            val outFile = File(dir, "icon_${System.currentTimeMillis()}.png")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext uri.toString()
            Uri.fromFile(outFile).toString()
        } catch (_: Exception) {
            uri.toString()
        }
    }

    private fun extractHtmlTitle(html: String): String? {
        val m = Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)).find(html)
        return m?.groupValues?.getOrNull(1)?.replace(Regex("\\s+"), " ")?.trim()?.takeIf { it.isNotBlank() }
    }
}
