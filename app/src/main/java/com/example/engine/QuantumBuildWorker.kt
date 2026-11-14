package com.example.engine

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.example.data.repository.ProjectRepository
import com.example.domain.model.BuildStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

/**
 * عامل الخلفية (WorkManager CoroutineWorker) لمحرك Quantum:
 * يضمن استمرار عملية البناء والتوقيع بأمان حتى لو تم تدوير الشاشة أو التنقل أو وضع التطبيق في الخلفية،
 * مع نشر التقدم الحقيقي وحفظ النتائج في ProjectRepository فور الانتهاء.
 */
class QuantumBuildWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val jobFilePath = inputData.getString(KEY_JOB_FILE_PATH).orEmpty()
        val jobFile = File(jobFilePath)
        if (jobFilePath.isBlank() || !jobFile.exists()) {
            return@withContext Result.failure(
                Data.Builder()
                    .putString(KEY_ERROR_MESSAGE, "ملف مهمة البناء غير موجود")
                    .build()
            )
        }

        val repository = ProjectRepository(applicationContext)
        val engine = QuantumBuildEngine.getInstance(applicationContext)

        return@withContext try {
            val payload = JSONObject(jobFile.readText(Charsets.UTF_8))
            val projectJson = payload.getJSONObject("project").toString()
            val buildType = payload.optString("buildType", "debug")
            val optJson = payload.optJSONObject("options")
            val options = BuildOptimizationOptions(
                r8Minify = optJson?.optBoolean("r8Minify", true) ?: true,
                shrinkResources = optJson?.optBoolean("shrinkResources", true) ?: true,
                abiSplit = optJson?.optBoolean("abiSplit", false) ?: false,
                zipAlign = optJson?.optBoolean("zipAlign", true) ?: true,
                autoSignWithKeystore = optJson?.optBoolean("autoSignWithKeystore", true) ?: true,
                autoBumpVersionCode = optJson?.optBoolean("autoBumpVersionCode", false) ?: false
            )

            val project = repository.parseProjectFromJsonString(projectJson)
                ?: return@withContext Result.failure(
                    Data.Builder()
                        .putString(KEY_ERROR_MESSAGE, "تعذر قراءة بيانات المشروع لبدء البناء")
                        .build()
                )

            repository.updateBuildStatus(project.id, BuildStatus.BUILDING, null)

            var outFileName = ""
            var outFilePath = ""
            var outSizeText = ""

            val success = engine.executeBuildWithProgress(
                project = project,
                buildType = buildType,
                options = options,
                onProgressUpdate = { pct, task ->
                    setProgress(
                        Data.Builder()
                            .putInt(KEY_PROGRESS_PCT, (pct * 100f).toInt().coerceIn(0, 100))
                            .putString(KEY_CURRENT_TASK, task)
                            .build()
                    )
                },
                onArtifactProduced = { fileName, outputPath, sizeText ->
                    outFileName = fileName
                    outFilePath = outputPath
                    outSizeText = sizeText
                }
            )

            if (success && outFilePath.isNotBlank()) {
                repository.updateBuildStatus(
                    projectId = project.id,
                    status = BuildStatus.SUCCESS,
                    apkPath = outFilePath,
                    customFileName = outFileName,
                    sizeText = outSizeText
                )
                jobFile.delete()
                Result.success(
                    Data.Builder()
                        .putString(KEY_OUTPUT_FILE_NAME, outFileName)
                        .putString(KEY_OUTPUT_APK_PATH, outFilePath)
                        .putString(KEY_OUTPUT_SIZE_TEXT, outSizeText)
                        .build()
                )
            } else {
                repository.updateBuildStatus(project.id, BuildStatus.FAILED, null)
                val errMsg = (engine.engineState.value as? QuantumBuildEngine.EngineStatus.Failure)?.error
                    ?: "تعذر إكمال البناء"
                Result.failure(
                    Data.Builder()
                        .putString(KEY_ERROR_MESSAGE, errMsg)
                        .build()
                )
            }
        } catch (e: Exception) {
            Result.failure(
                Data.Builder()
                    .putString(KEY_ERROR_MESSAGE, e.message ?: "خطأ غير متوقع في WorkManager")
                    .build()
            )
        }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "apkforge_quantum_on_device_build"
        const val KEY_JOB_FILE_PATH = "quantum_job_file_path"
        const val KEY_PROGRESS_PCT = "quantum_progress_pct"
        const val KEY_CURRENT_TASK = "quantum_current_task"
        const val KEY_OUTPUT_FILE_NAME = "quantum_output_file_name"
        const val KEY_OUTPUT_APK_PATH = "quantum_output_apk_path"
        const val KEY_OUTPUT_SIZE_TEXT = "quantum_output_size_text"
        const val KEY_ERROR_MESSAGE = "quantum_error_message"

        fun stageBuildJobFile(
            context: Context,
            projectJson: String,
            buildType: String,
            options: BuildOptimizationOptions
        ): File {
            val jobsDir = File(context.filesDir, "quantum_jobs").apply { mkdirs() }
            val jobFile = File(jobsDir, "job_${System.currentTimeMillis()}.json")
            val json = JSONObject().apply {
                put("buildType", buildType)
                put("project", JSONObject(projectJson))
                put("options", JSONObject().apply {
                    put("r8Minify", options.r8Minify)
                    put("shrinkResources", options.shrinkResources)
                    put("abiSplit", options.abiSplit)
                    put("zipAlign", options.zipAlign)
                    put("autoSignWithKeystore", options.autoSignWithKeystore)
                    put("autoBumpVersionCode", options.autoBumpVersionCode)
                })
            }
            jobFile.writeText(json.toString(), Charsets.UTF_8)
            return jobFile
        }
    }
}
