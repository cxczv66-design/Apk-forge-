package com.example.engine

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.domain.model.AppProject
import com.example.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.security.Signature
import java.util.Base64
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class AssembledBuildArtifact(
    val fileName: String,
    val internalFile: File,
    val publicDownloadsNote: String,
    val totalBytes: Long,
    val formattedSize: String,
    val filesCount: Int,
    val signatureSha256: String,
    val includesRealWorkspaceAssets: Boolean,
    val usedCustomAppIcon: Boolean
)

/**
 * محرك التجميع والتغليف والتوقيع الحقيقي على القرص:
 * - يكتب ملفات المشروع الحقيقية كاملة (Gradle + Kotlin + Standalone Java + Manifest + Icons + Imported Web/ZIP Assets)
 * - يوقّع جميع الإدخالات بتوقيع SHA256withRSA فعلي (MANIFEST.MF + APKFORGE.SF + APKFORGE.SIG)
 * - يكتب حزمة حقيقية قابلة للفحص والمشاركة في المسار المحلي وفي مجلد Download/ApkForge
 */
class BuildPackageAssembler(
    private val context: Context,
    private val toolchainManager: ToolchainManager = ToolchainManager(context),
    private val exporter: ProjectExporter = ProjectExporter(context)
) {

    fun getBuildsDir(): File {
        val dir = context.getExternalFilesDir("builds") ?: File(context.cacheDir, "builds")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    suspend fun prepareWorkspaceOnDisk(
        project: AppProject,
        options: BuildOptimizationOptions
    ): Pair<File, Int> = withContext(Dispatchers.IO) {
        val safeName = project.name.replace(Regex("[^a-zA-Z0-9_]"), "_").ifBlank { "ApkForgeApp" }
        val wsRoot = File(getBuildsDir(), "ws_${safeName}_${project.id.takeLast(6)}")
        if (wsRoot.exists()) wsRoot.deleteRecursively()
        wsRoot.mkdirs()

        val textEntries = exporter.buildProjectTextEntries(project, options)
        val realAssets = exporter.collectRealWorkspaceAssetFiles(project)
        val iconPack = AdaptiveIconStudio.generateIconPack(context, project)

        var count = 0
        textEntries.forEach { (relPath, content) ->
            if (!realAssets.containsKey(relPath)) {
                val f = File(wsRoot, relPath).apply { parentFile?.mkdirs() }
                f.writeText(content, Charsets.UTF_8)
                count++
            }
        }
        realAssets.forEach { (relPath, srcFile) ->
            if (srcFile.exists() && srcFile.isFile) {
                val dest = File(wsRoot, relPath).apply { parentFile?.mkdirs() }
                srcFile.copyTo(dest, overwrite = true)
                count++
            }
        }
        iconPack.densityPngs.forEach { (relPath, bytes) ->
            val dest = File(wsRoot, relPath).apply { parentFile?.mkdirs() }
            dest.writeBytes(bytes)
            count++
        }

        wsRoot to count
    }

    /**
     * يجمع ويوقّع ويكتب حزمة فعليّة كاملة على القرص وينسخها أيضاً إلى Download/ApkForge
     */
    suspend fun buildAndSignPackageOnDisk(
        project: AppProject,
        targetType: String,
        options: BuildOptimizationOptions,
        termuxBuildScript: String
    ): AssembledBuildArtifact = withContext(Dispatchers.IO) {
        val safeName = project.name.replace(Regex("[^a-zA-Z0-9_]"), "_").ifBlank { "ApkForgeApp" }
        val version = project.packageConfig.versionName.ifBlank { "1.0" }
        val outFileName = "${safeName}_v${version}_${targetType}_signed_bundle.zip"
        val outFile = File(getBuildsDir(), outFileName)

        val textEntries = exporter.buildProjectTextEntries(project, options).toMutableMap()
        textEntries["termux_standalone/build_apk_termux.sh"] = termuxBuildScript

        val realAssets = exporter.collectRealWorkspaceAssetFiles(project)
        val iconPack = AdaptiveIconStudio.generateIconPack(context, project)

        val digestMap = linkedMapOf<String, ByteArray>()
        val manifestLines = StringBuilder().apply {
            append("Manifest-Version: 1.0\r\n")
            append("Created-By: ApkForge Pro Builder 2.0\r\n")
            append("Package-Id: ${project.packageConfig.packageName}\r\n")
            append("Build-Target: ${targetType.uppercase()}\r\n\r\n")
        }

        var filesCount = 0
        ZipOutputStream(BufferedOutputStream(FileOutputStream(outFile))).use { zos ->
            textEntries.forEach { (path, content) ->
                if (!realAssets.containsKey(path)) {
                    val bytes = content.toByteArray(Charsets.UTF_8)
                    writeZipEntryWithDigest(zos, path, bytes, digestMap, manifestLines)
                    filesCount++
                }
            }
            realAssets.forEach { (path, diskFile) ->
                if (diskFile.exists() && diskFile.isFile) {
                    val bytes = diskFile.readBytes()
                    writeZipEntryWithDigest(zos, path, bytes, digestMap, manifestLines)
                    filesCount++
                }
            }
            iconPack.densityPngs.forEach { (path, bytes) ->
                writeZipEntryWithDigest(zos, path, bytes, digestMap, manifestLines)
                filesCount++
            }

            // Real cryptographic RSA-2048 JAR signature entries
            val alias = project.signingConfig.keyAlias.ifBlank { "apkforge_release" }
            val keyPair = toolchainManager.getOrCreateSigningKeyPair(alias)
            val manifestBytes = manifestLines.toString().toByteArray(Charsets.UTF_8)
            val manifestSha = MessageDigest.getInstance("SHA-256").digest(manifestBytes)

            val sfText = buildString {
                append("Signature-Version: 1.0\r\n")
                append("Created-By: ApkForge Keytool & ApkSigner Engine\r\n")
                append("SHA-256-Digest-Manifest: ${Base64.getEncoder().encodeToString(manifestSha)}\r\n")
                append("Signing-Key-Alias: $alias\r\n\r\n")
                digestMap.forEach { (path, sha) ->
                    append("Name: $path\r\n")
                    append("SHA-256-Digest: ${Base64.getEncoder().encodeToString(sha)}\r\n\r\n")
                }
            }
            val sfBytes = sfText.toByteArray(Charsets.UTF_8)

            val sigEngine = Signature.getInstance("SHA256withRSA").apply {
                initSign(keyPair.private)
                update(sfBytes)
            }
            val rsaSigBytes = sigEngine.sign()

            val receiptJson = JSONObject().apply {
                put("projectName", project.name)
                put("packageName", project.packageConfig.packageName)
                put("versionCode", project.packageConfig.versionCode)
                put("versionName", project.packageConfig.versionName)
                put("target", targetType)
                put("signedWithAlias", alias)
                put("signatureAlgorithm", "SHA256withRSA")
                put("publicKeySha256", sha256Hex(keyPair.public.encoded))
                put("filesCount", filesCount)
                put("realAssetsCount", realAssets.size)
                put("usedCustomIcon", iconPack.usedCustomImage)
                put("generatedAt", System.currentTimeMillis())
            }.toString(2).toByteArray(Charsets.UTF_8)

            writeRawZipEntry(zos, "META-INF/MANIFEST.MF", manifestBytes)
            writeRawZipEntry(zos, "META-INF/APKFORGE.SF", sfBytes)
            writeRawZipEntry(zos, "META-INF/APKFORGE.SIG", rsaSigBytes)
            writeRawZipEntry(zos, "META-INF/APKFORGE.RSA", keyPair.public.encoded)
            writeRawZipEntry(zos, "META-INF/BUILD_RECEIPT.json", receiptJson)
            filesCount += 5
        }

        val publicCopyNote = mirrorFileToPublicDownloads(
            sourceFile = outFile,
            displayName = outFileName,
            mimeType = "application/zip",
            subDir = null
        )

        AssembledBuildArtifact(
            fileName = outFileName,
            internalFile = outFile,
            publicDownloadsNote = publicCopyNote,
            totalBytes = outFile.length(),
            formattedSize = formatBytesReadable(outFile.length()),
            filesCount = filesCount,
            signatureSha256 = sha256Hex(outFile.readBytes()),
            includesRealWorkspaceAssets = realAssets.isNotEmpty(),
            usedCustomAppIcon = iconPack.usedCustomImage
        )
    }

    /**
     * يكتب نسخة المصدر الكاملة (.zip) + سكربت البناء (.sh) إلى Download/ApkForge حتى يتمكن Termux من الوصول إليها فوراً
     */
    suspend fun stageSourceZipForTermux(
        project: AppProject,
        options: BuildOptimizationOptions,
        termuxBuildScript: String
    ): Pair<File, String> = withContext(Dispatchers.IO) {
        val safeName = project.name.replace(Regex("[^a-zA-Z0-9_]"), "_").ifBlank { "ApkForgeApp" }
        val srcZipName = "ApkForge_Src_${safeName}.zip"
        val srcZipFile = File(getBuildsDir(), srcZipName)

        val textEntries = exporter.buildProjectTextEntries(project, options)
        val realAssets = exporter.collectRealWorkspaceAssetFiles(project)
        val iconPack = AdaptiveIconStudio.generateIconPack(context, project)

        ZipOutputStream(BufferedOutputStream(FileOutputStream(srcZipFile))).use { zos ->
            textEntries.forEach { (path, content) ->
                if (!realAssets.containsKey(path)) {
                    writeRawZipEntry(zos, path, content.toByteArray(Charsets.UTF_8))
                }
            }
            realAssets.forEach { (path, diskFile) ->
                if (diskFile.exists() && diskFile.isFile) {
                    writeRawZipEntry(zos, path, diskFile.readBytes())
                }
            }
            iconPack.densityPngs.forEach { (path, bytes) ->
                writeRawZipEntry(zos, path, bytes)
            }
            writeRawZipEntry(zos, "build_termux.sh", termuxBuildScript.toByteArray(Charsets.UTF_8))
        }

        val pubPath = mirrorFileToPublicDownloads(
            sourceFile = srcZipFile,
            displayName = srcZipName,
            mimeType = "application/zip",
            subDir = Constants.DOWNLOADS_SUBDIR
        )
        val scriptFile = File(getBuildsDir(), "build_${safeName}.sh").apply {
            writeText(termuxBuildScript, Charsets.UTF_8)
        }
        mirrorFileToPublicDownloads(
            sourceFile = scriptFile,
            displayName = "build_${safeName}.sh",
            mimeType = "text/x-sh",
            subDir = Constants.DOWNLOADS_SUBDIR
        )
        srcZipFile to pubPath
    }

    private fun mirrorFileToPublicDownloads(
        sourceFile: File,
        displayName: String,
        mimeType: String,
        subDir: String? = Constants.DOWNLOADS_SUBDIR
    ): String {
        val relativeFolder = if (subDir.isNullOrBlank()) {
            Environment.DIRECTORY_DOWNLOADS
        } else {
            "${Environment.DIRECTORY_DOWNLOADS}/$subDir"
        }
        val queryFolder = "$relativeFolder/"
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
                // Remove old file with same name in target folder to stay idempotent
                resolver.delete(
                    collection,
                    "${MediaStore.Downloads.DISPLAY_NAME}=? AND ${MediaStore.Downloads.RELATIVE_PATH}=?",
                    arrayOf(displayName, queryFolder)
                )
                val cv = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, displayName)
                    put(MediaStore.Downloads.MIME_TYPE, mimeType)
                    put(MediaStore.Downloads.RELATIVE_PATH, relativeFolder)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val uri = resolver.insert(collection, cv)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { out ->
                        FileInputStream(sourceFile).use { it.copyTo(out) }
                    }
                    cv.clear()
                    cv.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(uri, cv, null, null)
                    if (subDir.isNullOrBlank()) "Download/$displayName" else "${Constants.DOWNLOADS_DIR}/$displayName"
                } else {
                    sourceFile.absolutePath
                }
            } else {
                @Suppress("DEPRECATION")
                val baseDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val pubDir = (if (subDir.isNullOrBlank()) baseDir else File(baseDir, subDir)).apply {
                    mkdirs()
                }
                val dest = File(pubDir, displayName)
                sourceFile.copyTo(dest, overwrite = true)
                dest.absolutePath
            }
        } catch (_: Exception) {
            sourceFile.absolutePath
        }
    }

    private fun writeZipEntryWithDigest(
        zos: ZipOutputStream,
        path: String,
        bytes: ByteArray,
        digestMap: MutableMap<String, ByteArray>,
        manifestLines: StringBuilder
    ) {
        writeRawZipEntry(zos, path, bytes)
        val sha = MessageDigest.getInstance("SHA-256").digest(bytes)
        digestMap[path] = sha
        manifestLines.append("Name: $path\r\n")
        manifestLines.append("SHA-256-Digest: ${Base64.getEncoder().encodeToString(sha)}\r\n\r\n")
    }

    private fun writeRawZipEntry(zos: ZipOutputStream, path: String, bytes: ByteArray) {
        zos.putNextEntry(ZipEntry(path))
        zos.write(bytes)
        zos.closeEntry()
    }

    private fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
}
