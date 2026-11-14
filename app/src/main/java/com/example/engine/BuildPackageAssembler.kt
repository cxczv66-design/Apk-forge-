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
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class AssembledBuildArtifact(
    val fileName: String,
    val internalFile: File,
    val v4IdsigFile: File?,
    val publicDownloadsNote: String,
    val totalBytes: Long,
    val formattedSize: String,
    val filesCount: Int,
    val signatureSha256: String,
    val includesRealWorkspaceAssets: Boolean,
    val usedCustomAppIcon: Boolean,
    val v1Verified: Boolean = false,
    val v2Verified: Boolean = false,
    val v3Verified: Boolean = false,
    val v4Verified: Boolean = false,
    val alignmentVerified: Boolean = false,
    val vaultProtectionMode: String = ""
)

/**
 * منسق مساحة العمل والتخزين العام (MediaStore API) لحزم Quantum المبنية داخل التطبيق.
 */
class BuildPackageAssembler(
    private val context: Context,
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

    fun mirrorFileToPublicDownloads(
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

    fun buildQuantumSourceArchive(
        project: AppProject,
        options: BuildOptimizationOptions,
        commandSpec: QuantumBuildCommandSpec,
        vaultFile: File
    ): File {
        val safeName = project.name.replace(Regex("[^a-zA-Z0-9_]"), "_").ifBlank { "ApkForgeApp" }
        val srcZipFile = File(getBuildsDir(), "Quantum_Src_${safeName}.zip")
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
            writeRawZipEntry(
                zos,
                "quantum_pipeline_spec.sh",
                commandSpec.toFullScriptText(project, vaultFile).toByteArray(Charsets.UTF_8)
            )
        }
        return srcZipFile
    }

    fun createQuantumBuildReceipt(
        project: AppProject,
        targetType: String,
        verification: QuantumSigningVerificationResult,
        aapt2Res: QuantumAapt2Result,
        d8Res: QuantumD8Result,
        alignment: QuantumAlignmentReport
    ): ByteArray {
        return JSONObject().apply {
            put("engine", "ApkForge Quantum On-Device Engine 3.0")
            put("projectName", project.name)
            put("packageName", project.packageConfig.packageName)
            put("versionCode", project.packageConfig.versionCode)
            put("versionName", project.packageConfig.versionName)
            put("target", targetType)
            put("alignmentVerified", alignment.allStoredEntriesAligned)
            put("alignedStoredEntries", alignment.alignedStoredEntries)
            put("aapt2PermissionsCount", aapt2Res.encodedPermissions.size)
            put("d8ClassesCount", d8Res.dexClassCount)
            put("v1Verified", verification.v1Verified)
            put("v2Verified", verification.v2Verified)
            put("v3Verified", verification.v3Verified)
            put("v4IdsigGenerated", verification.v4FileExists)
            put("vaultProtectionMode", verification.vaultProtectionMode)
            put("signerCertSha256", verification.signerCertSha256)
            put("generatedAt", System.currentTimeMillis())
        }.toString(2).toByteArray(Charsets.UTF_8)
    }

    private fun writeRawZipEntry(zos: ZipOutputStream, path: String, bytes: ByteArray) {
        zos.putNextEntry(ZipEntry(path))
        zos.write(bytes)
        zos.closeEntry()
    }

    fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
}
