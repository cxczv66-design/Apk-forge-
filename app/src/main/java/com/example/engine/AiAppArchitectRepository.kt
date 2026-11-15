package com.example.engine

import android.content.Context
import android.util.Log
import com.example.domain.model.*
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.io.File

private const val TAG = "AiAppArchitect"

/**
 * مستودع مساعد الذكاء الاصطناعي (AI Wizard) المبني على Firebase AI Logic:
 * يستخدم البث التدفقي (generateContentStream) لتقليل زمن الاستجابة الأولي (TTFT)،
 * مع نموذج gemini-3.8-flash افتراضياً ونموذج gemini-3.5-flash-lite كمسار احتياطي فائق السرعة.
 */
class AiAppArchitectRepository {

    private val architectSchema = Schema.obj(
        properties = mapOf(
            "appNameAr" to Schema.string("اسم عربي مختصر (2-3 كلمات)"),
            "packageSuffix" to Schema.string("معرّف إنجليزي صغير بدون فراغات مثل mystore"),
            "summaryAr" to Schema.string("ملخص مختصر في جملة واحدة"),
            "accentColorHex" to Schema.string("لون رئيسي HEX مثل #FF7A2F"),
            "roadmap" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "stageNumber" to Schema.integer("رقم المرحلة بدءاً من 1"),
                        "titleAr" to Schema.string("عنوان مختصر (2-4 كلمات)"),
                        "descriptionAr" to Schema.string("وصف موجز في سطر واحد"),
                        "featureCountHint" to Schema.integer("عدد الميزات بين 8 و 28"),
                        "category" to Schema.string("أحد التصنيفات: identity, design, auth, data, api, navigation, notifications, permissions, packaging")
                    )
                ),
                description = "قائمة من 12 مرحلة متسلسلة ومختصرة"
            ),
            "smartQuestions" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "id" to Schema.string("معرف مثل q_auth"),
                        "questionAr" to Schema.string("سؤال مختصر وواضح بالعربية"),
                        "kind" to Schema.enumeration(listOf("boolean", "choice"), "نوع السؤال"),
                        "options" to Schema.array(
                            items = Schema.string(),
                            description = "خيارات الإجابة إذا كان النوع choice (3 خيارات قصيرة)"
                        )
                    )
                ),
                description = "5 أسئلة ذكية مختصرة مخصصة للفكرة"
            )
        )
    )

    private val structuredConfig = generationConfig {
        responseMimeType = "application/json"
        responseSchema = architectSchema
        temperature = 0.4f
        maxOutputTokens = 2048
    }

    private val primaryModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = structuredConfig
        )
    }

    private val fastFallbackModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite",
            generationConfig = structuredConfig
        )
    }

    private suspend fun streamJsonFromModel(
        model: GenerativeModel,
        instruction: String,
        timeoutMs: Long
    ): String {
        val sb = StringBuilder()
        withTimeout(timeoutMs) {
            model.generateContentStream(instruction).collect { chunk ->
                val part = chunk.text
                if (!part.isNullOrEmpty()) {
                    sb.append(part)
                }
            }
        }
        return sb.toString().trim()
    }

    suspend fun generateArchitectPlan(
        ideaPrompt: String,
        presetCategory: String? = null
    ): Result<AiArchitectPlan> {
        val cleanPrompt = ideaPrompt.trim()
        val combinedIdea = buildString {
            if (!presetCategory.isNullOrBlank()) append("الفئة: $presetCategory. ")
            append(cleanPrompt.ifBlank { presetCategory ?: "تطبيق أندرويد حديث" })
        }

        val instruction = """
            أنت المهندس المعماري لمنصة ApkForge. حلّل الفكرة وأرجع JSON مختصراً وسريعاً بالعربية:
            الفكرة: "$combinedIdea"
            الشروط:
            - roadmap: 12 مرحلة بعناوين قصيرة (2-4 كلمات) وأوصاف موجزة (أقل من 10 كلمات).
            - smartQuestions: 5 أسئلة ذكية قصيرة خاصة بالفكرة (مثل تسجيل Google، الإشعارات، قاعدة البيانات، ونمط الألوان بصيغة choice مع أكواد hex مثل "#FF7A2F").
        """.trimIndent()

        return try {
            val rawJson = try {
                streamJsonFromModel(primaryModel, instruction, timeoutMs = 45_000L)
                    .ifBlank { throw IllegalStateException("استجابة النموذج الأولي فارغة.") }
            } catch (primaryTimeoutOrErr: TimeoutCancellationException) {
                Log.w(TAG, "Primary model timed out at 45s; falling back to gemini-3.5-flash-lite", primaryTimeoutOrErr)
                streamJsonFromModel(fastFallbackModel, instruction, timeoutMs = 45_000L)
            }

            if (rawJson.isBlank()) {
                throw IllegalStateException("استجابة النموذج فارغة.")
            }

            val json = JSONObject(rawJson)

            val appNameAr = json.optString("appNameAr", "تطبيقي الذكي").ifBlank { "تطبيقي الذكي" }
            val packageSuffix = json.optString("packageSuffix", "smartapp")
                .lowercase().filter { it in 'a'..'z' || it in '0'..'9' }.ifBlank { "smartapp" }
            val summaryAr = json.optString("summaryAr", "")
            val accentColorHex = json.optString("accentColorHex", "#FF7A2F")
                .takeIf { it.startsWith("#") && (it.length == 7 || it.length == 9) } ?: "#FF7A2F"

            val roadmapArr = json.optJSONArray("roadmap") ?: org.json.JSONArray()
            val roadmap = (0 until roadmapArr.length()).map { i ->
                val obj = roadmapArr.getJSONObject(i)
                AiRoadmapStage(
                    stageNumber = obj.optInt("stageNumber", i + 1),
                    titleAr = obj.optString("titleAr", "مرحلة ${i + 1}"),
                    descriptionAr = obj.optString("descriptionAr", ""),
                    featureCountHint = obj.optInt("featureCountHint", 10).coerceIn(6, 32),
                    category = obj.optString("category", "design")
                )
            }

            val questionsArr = json.optJSONArray("smartQuestions") ?: org.json.JSONArray()
            val questions = (0 until questionsArr.length()).map { i ->
                val obj = questionsArr.getJSONObject(i)
                val kind = obj.optString("kind", "boolean")
                val optsArr = obj.optJSONArray("options")
                val opts = if (optsArr != null) {
                    (0 until optsArr.length()).mapNotNull { j -> optsArr.optString(j).takeIf { it.isNotBlank() } }
                } else emptyList()
                AiSmartQuestion(
                    id = obj.optString("id", "q_$i"),
                    questionAr = obj.optString("questionAr", ""),
                    kind = if (kind == "choice" && opts.isNotEmpty()) "choice" else "boolean",
                    options = opts,
                    boolAnswer = true,
                    selectedOption = opts.firstOrNull().orEmpty()
                )
            }

            if (roadmap.isEmpty()) {
                throw IllegalStateException("لم يُرجع الذكاء الاصطناعي مراحل صالحة.")
            }

            Result.success(
                AiArchitectPlan(
                    ideaPrompt = combinedIdea,
                    appNameAr = appNameAr,
                    packageSuffix = packageSuffix,
                    summaryAr = summaryAr,
                    accentColorHex = accentColorHex,
                    roadmap = roadmap,
                    smartQuestions = questions
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI architect generation timed out across primary and fallback models", e)
            Result.failure(Exception("انتهت مهلة الاتصال بالذكاء الاصطناعي. تحقق من الاتصال بالإنترنت وأعد المحاولة."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI architect generation failed", e)
            Result.failure(Exception(e.localizedMessage ?: "تعذر الاتصال بمحرك الذكاء الاصطناعي."))
        }
    }

    /**
     * يبني AppProject حقيقي ومجتمع HTML5 تفاعلي محلي بناءً على خطة الذكاء الاصطناعي وأجوبة المستخدم.
     */
    suspend fun materializeProjectFromPlan(
        context: Context,
        plan: AiArchitectPlan,
        answeredQuestions: List<AiSmartQuestion>
    ): AppProject = withContext(Dispatchers.IO) {
        val wantsAuth = answeredQuestions.any {
            (it.questionAr.contains("دخول") || it.questionAr.contains("Google") || it.id.contains("auth", true)) && it.boolAnswer
        }
        val wantsPush = answeredQuestions.any {
            (it.questionAr.contains("إشعار") || it.id.contains("push", true) || it.id.contains("notif", true)) && it.boolAnswer
        }
        val wantsDb = answeredQuestions.any {
            (it.questionAr.contains("بيانات") || it.questionAr.contains("حفظ") || it.id.contains("db", true)) && it.boolAnswer
        }
        val chosenColorHex = answeredQuestions
            .firstOrNull { it.kind == "choice" && it.selectedOption.contains("#") }
            ?.selectedOption
            ?.let { Regex("#[0-9A-Fa-f]{6}").find(it)?.value }
            ?: plan.accentColorHex

        val wsDir = File(context.filesDir, "omni_workspaces/ai_${System.currentTimeMillis()}").apply { mkdirs() }
        val sectionsHtml = plan.roadmap.take(6).joinToString("\n") { st ->
            """
            <div class="card">
              <div class="badge">المرحلة ${st.stageNumber}</div>
              <h3>${escapeHtml(st.titleAr)}</h3>
              <p>${escapeHtml(st.descriptionAr)}</p>
            </div>
            """.trimIndent()
        }

        val authSection = if (wantsAuth) {
            """
            <div class="card auth-card">
              <h3>🔐 تسجيل الدخول السريع</h3>
              <p>دعم تسجيل الدخول بحساب Google وحفظ جلسة المستخدم بأمان</p>
              <button class="btn-primary" onclick="ApkForge.showToast('تم تفعيل المصادقة الذكية ✓')">تسجيل الدخول الآن</button>
            </div>
            """.trimIndent()
        } else ""

        val htmlContent = """
            <!DOCTYPE html>
            <html lang="ar" dir="rtl">
            <head>
              <meta charset="UTF-8" />
              <meta name="viewport" content="width=device-width, initial-scale=1.0" />
              <title>${escapeHtml(plan.appNameAr)}</title>
              <style>
                :root { --accent: $chosenColorHex; }
                * { box-sizing: border-box; }
                body { margin: 0; font-family: system-ui, -apple-system, sans-serif; background: #0B0E17; color: #F8FAFC; padding: 16px; }
                .hero { background: linear-gradient(135deg, ${chosenColorHex}33, #141928); border: 1px solid ${chosenColorHex}80; border-radius: 20px; padding: 20px; text-align: center; box-shadow: 0 14px 32px rgba(0,0,0,0.4); }
                .hero h1 { margin: 0 0 8px 0; font-size: 22px; color: #FFFFFF; }
                .hero p { margin: 0 0 14px 0; font-size: 13px; color: #CBD5E1; line-height: 1.6; }
                .btn-primary { background: var(--accent); color: #fff; border: none; border-radius: 12px; padding: 11px 18px; font-size: 13px; font-weight: bold; cursor: pointer; box-shadow: 0 6px 16px ${chosenColorHex}55; }
                .card { background: #141826; border: 1px solid #262E45; border-radius: 16px; padding: 14px; margin-top: 12px; }
                .card h3 { margin: 6px 0 4px 0; font-size: 15px; color: #FFFFFF; }
                .card p { margin: 0; font-size: 12px; color: #94A3B8; line-height: 1.5; }
                .badge { display: inline-block; background: ${chosenColorHex}26; color: var(--accent); font-size: 11px; font-weight: bold; padding: 3px 10px; border-radius: 999px; }
              </style>
            </head>
            <body>
              <div class="hero">
                <h1 data-omni-hero>${escapeHtml(plan.appNameAr)}</h1>
                <p>${escapeHtml(plan.summaryAr.ifBlank { plan.ideaPrompt })}</p>
                <button class="btn-primary" data-omni-cta onclick="ApkForge.showToast('أهلاً بك في ${escapeHtml(plan.appNameAr)}!')">ابدأ الاستخدام الآن</button>
              </div>
              $authSection
              $sectionsHtml
            </body>
            </html>
        """.trimIndent()

        val entryFile = File(wsDir, "index.html")
        entryFile.writeText(htmlContent, Charsets.UTF_8)

        AppProject(
            id = "proj_ai_${System.currentTimeMillis()}",
            name = plan.appNameAr.take(28),
            description = plan.summaryAr.ifBlank { plan.ideaPrompt },
            primaryColorHex = chosenColorHex,
            accentColorHex = chosenColorHex,
            sourceType = SourceType.AI_SCRATCH,
            sourceValue = "index.html",
            localWorkspacePath = wsDir.absolutePath,
            splashConfig = SplashConfig(
                enabled = true,
                backgroundColorHex = "#0B0E17",
                indicatorStyle = "حلقة"
            ),
            onboardingConfig = OnboardingConfig(
                enabled = true,
                slides = listOf(
                    OnboardingSlide(plan.appNameAr, plan.summaryAr.ifBlank { "تجربة ذكية مصممة لك خصيصاً" }, "bolt"),
                    OnboardingSlide("جاهز للانطلاق", "استمتع بأداء سريع وواجهة تفاعلية حديثة", "verified")
                ),
                accentColorHex = chosenColorHex
            ),
            webViewConfig = WebViewConfig(
                fullScreen = false,
                pullToRefresh = true,
                cacheEnabled = true,
                offlineFallback = true
            ),
            bottomNavConfig = BottomNavConfig(
                enabled = true,
                activeColorHex = chosenColorHex,
                destinations = listOf(
                    BottomNavDestination("home", "الرئيسية", "/", "home"),
                    BottomNavDestination("explore", "الأقسام", "/explore", "explore"),
                    BottomNavDestination("profile", "حسابي", "/profile", "person")
                )
            ),
            packageConfig = PackageConfig(
                packageName = "com.apkforge.${plan.packageSuffix}",
                versionName = "1.0.0",
                versionCode = 1
            ),
            permissionsConfig = PermissionsConfig(
                notifications = wantsPush,
                networkState = true,
                vibration = true
            ),
            pushConfig = PushNotificationConfig(
                enabled = wantsPush,
                channelName = "تنبيهات ${plan.appNameAr}"
            ),
            omniConfig = OmniIdeConfig(
                gradientEnabled = true,
                gradientStartHex = chosenColorHex,
                gradientEndHex = "#1E1B4B",
                authBuilderEnabled = wantsAuth,
                localDbEnabled = wantsDb,
                appAcceleratorEnabled = true
            )
        )
    }

    private fun escapeHtml(raw: String): String =
        raw.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
}
