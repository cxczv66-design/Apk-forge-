package com.example.engine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.utils.Constants
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class TermuxExecCallbackResult(
    val mode: String,
    val stdout: String,
    val stderr: String,
    val exitCode: Int,
    val errorCode: Int,
    val errorMessage: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    val requiresExternalAppsEnable: Boolean
        get() = errorCode == 2 ||
            errorMessage.contains("allow-external-apps", ignoreCase = true) ||
            stderr.contains("allow-external-apps", ignoreCase = true)

    val isSuccess: Boolean
        get() = errorCode == 0 && (exitCode == 0 || (mode == "build" && exitCode == -1))
}

/**
 * يستقبل نتائج التنفيذ الحقيقية العائدة من Termux RunCommandService عبر PendingIntent
 * لالتقاط stdout/stderr/exitCode ورسائل الخطأ مثل allow-external-apps و err=-1 داخل التطبيق فوراً.
 */
class TermuxResultReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val resultBundle = intent.getBundleExtra("result")
        val stdout = resultBundle?.getString("stdout")
            ?: intent.getStringExtra("stdout")
            ?: ""
        val stderr = resultBundle?.getString("stderr")
            ?: intent.getStringExtra("stderr")
            ?: ""
        val hasExitCode = resultBundle?.containsKey("exitCode") == true || intent.hasExtra("exitCode")
        val exitCode = when {
            resultBundle?.containsKey("exitCode") == true -> resultBundle.getInt("exitCode", 0)
            intent.hasExtra("exitCode") -> intent.getIntExtra("exitCode", 0)
            else -> -1
        }
        val err = when {
            resultBundle?.containsKey("err") == true -> resultBundle.getInt("err", 0)
            intent.hasExtra("err") -> intent.getIntExtra("err", 0)
            !hasExitCode && stderr.isNotBlank() -> -1
            else -> 0
        }
        val errmsg = resultBundle?.getString("errmsg")
            ?: intent.getStringExtra("errmsg")
            ?: ""
        val mode = intent.getStringExtra(Constants.EXTRA_EXEC_MODE) ?: "build"

        emitResult(
            TermuxExecCallbackResult(
                mode = mode,
                stdout = stdout,
                stderr = stderr,
                exitCode = exitCode,
                errorCode = err,
                errorMessage = errmsg
            )
        )
    }

    companion object {
        private val _results = MutableSharedFlow<TermuxExecCallbackResult>(extraBufferCapacity = 32)
        val results: SharedFlow<TermuxExecCallbackResult> = _results.asSharedFlow()

        fun emitResult(result: TermuxExecCallbackResult) {
            _results.tryEmit(result)
        }
    }
}
