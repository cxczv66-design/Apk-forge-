package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.domain.model.AppProject
import com.example.engine.TerminalLineType
import com.example.engine.ToolchainManager
import com.example.engine.ToolchainToolStatus
import com.example.ui.theme.*
import kotlinx.coroutines.launch

/**
 * عرض أيقونة التطبيق الحقيقية (من معرض الهاتف أو رابط الموقع) في جميع أنحاء التطبيق
 * مع تدرج أنيق احتياطي فقط إذا لم يتم تحديد أيقونة بعد.
 */
@Composable
fun RealAppIconAvatar(
    project: AppProject,
    size: Dp = 44.dp,
    cornerRadius: Dp = 12.dp,
    modifier: Modifier = Modifier
) {
    val accent = parseColorSafe(project.accentColorHex, OrangePrimary)
    val iconUri = project.iconImageUri?.takeIf { it.isNotBlank() && it != "preset_anime" }
    var loadFailed by remember(iconUri) { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(Brush.linearGradient(listOf(accent, OrangePrimary)))
            .border(1.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(cornerRadius)),
        contentAlignment = Alignment.Center
    ) {
        if (iconUri != null && !loadFailed) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(iconUri)
                    .crossfade(true)
                    .build(),
                contentDescription = project.name,
                contentScale = ContentScale.Crop,
                onError = { loadFailed = true },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = project.name.trim().take(2).uppercase().ifBlank { "AF" },
                fontFamily = CairoFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = (size.value * 0.36f).sp,
                color = Color.White
            )
        }
    }
}

/**
 * لوحة أدوات البناء والتوقيع المتقدمة + التثبيت التلقائي داخل التطبيق + واجهة الترمينال الحية المنظمة
 */
@Composable
fun InAppToolchainTerminalCard(
    toolchainManager: ToolchainManager,
    activeProject: AppProject? = null,
    compactMode: Boolean = false,
    onToolActionToast: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tools by toolchainManager.tools.collectAsState()
    val terminalLogs by toolchainManager.terminalLogs.collectAsState()
    val isInstalling by toolchainManager.isInstalling.collectAsState()
    val installProgress by toolchainManager.installProgress.collectAsState()

    var commandInput by remember { mutableStateOf("") }
    var showToolsGrid by remember { mutableStateOf(!compactMode) }
    val listState = rememberLazyListState()

    val installedCount = tools.count { it.isInstalled }
    val allReady = tools.isNotEmpty() && installedCount == tools.size

    LaunchedEffect(terminalLogs.size) {
        if (terminalLogs.isNotEmpty()) {
            listState.animateScrollToItem(terminalLogs.lastIndex)
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Surface(
            color = Color(0xFF101622),
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (allReady) StatusGreen.copy(alpha = 0.55f) else OrangePrimary.copy(alpha = 0.45f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("in_app_toolchain_terminal_card")
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (allReady) StatusGreen.copy(alpha = 0.16f)
                                    else OrangePrimary.copy(alpha = 0.16f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (allReady) Icons.Default.Verified else Icons.Default.BuildCircle,
                                contentDescription = null,
                                tint = if (allReady) StatusGreen else OrangePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "أدوات البناء والتوقيع داخل التطبيق",
                                fontFamily = CairoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "مثبت ومُتحقق: $installedCount / ${tools.size} • ${toolchainManager.getDeviceAbi()} • ${toolchainManager.getFreeStorageMb()}MB حرة",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = if (allReady) StatusGreen else TextSecondary
                            )
                        }
                    }

                    Surface(
                        color = Color(0xFF1A2232),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.clickable { showToolsGrid = !showToolsGrid }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (showToolsGrid) "إخفاء" else "الأدوات (${tools.size})",
                                fontFamily = CairoFontFamily,
                                fontSize = 10.sp,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                imageVector = if (showToolsGrid) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = OrangePrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Prominent In-App Install & Verify Button
                Button(
                    onClick = {
                        scope.launch {
                            val ok = toolchainManager.installAndVerifyAllTools(activeProject)
                            val msg = if (ok) {
                                "✅ تم تثبيت والتحقق من جميع الأدوات داخل التطبيق (${tools.size}/${tools.size})"
                            } else {
                                "⚠️ راجع سجل الترمينال لمعرفة التفاصيل"
                            }
                            onToolActionToast?.invoke(msg)
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !isInstalling,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (allReady) StatusGreen else OrangePrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("install_verify_tools_btn")
                ) {
                    if (isInstalling) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "جارٍ التثبيت والتحقق الفعلي (${(installProgress * 100).toInt()}%)...",
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    } else {
                        Icon(
                            imageVector = if (allReady) Icons.Default.FactCheck else Icons.Default.InstallMobile,
                            contentDescription = null,
                            tint = if (allReady) Color.Black else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (allReady) "إعادة فحص وتحديث جميع الأدوات ✓" else "تثبيت والتحقق من أدوات البناء داخل التطبيق ⚡",
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (allReady) Color.Black else Color.White
                        )
                    }
                }

                if (isInstalling) {
                    LinearProgressIndicator(
                        progress = { installProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape),
                        color = OrangePrimary,
                        trackColor = Color(0xFF1F2937)
                    )
                }

                // Expandable Organized Tools Matrix (8 Advanced Real Tools)
                AnimatedVisibility(visible = showToolsGrid) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        tools.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                pair.forEach { tool ->
                                    ToolChipCard(
                                        tool = tool,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            scope.launch {
                                                toolchainManager.runInteractiveCommand("verify", activeProject)
                                            }
                                        }
                                    )
                                }
                                if (pair.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                // Organized Linear Terminal Window
                Surface(
                    color = Color(0xFF06090F),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1F2B3E)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Terminal, contentDescription = null, tint = TerminalGreen, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ApkForge Live Terminal",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = TerminalGreen
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "نسخ السجل",
                                    tint = TextSecondary,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable {
                                            val all = terminalLogs.joinToString("\n") { "[${it.timestamp}] ${it.text}" }
                                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            cm.setPrimaryClip(ClipData.newPlainText("ApkForge Terminal", all))
                                            Toast.makeText(context, "تم نسخ مخرجات الترمينال ✓", Toast.LENGTH_SHORT).show()
                                        }
                                )
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "مسح",
                                    tint = TextSecondary,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { toolchainManager.clearTerminal() }
                                )
                            }
                        }

                        // Quick Terminal Actions
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            QuickTerminalPill("verify") {
                                scope.launch { toolchainManager.runInteractiveCommand("verify", activeProject) }
                            }
                            QuickTerminalPill("gen-key") {
                                scope.launch { toolchainManager.runInteractiveCommand("gen-key", activeProject) }
                            }
                            QuickTerminalPill("storage") {
                                scope.launch { toolchainManager.runInteractiveCommand("storage", activeProject) }
                            }
                            QuickTerminalPill("abi") {
                                scope.launch { toolchainManager.runInteractiveCommand("abi", activeProject) }
                            }
                            QuickTerminalPill("uname -a") {
                                scope.launch { toolchainManager.runInteractiveCommand("sh uname -a", activeProject) }
                            }
                        }

                        // Linear Streaming Output Box
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 120.dp, max = 185.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF04060A))
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                if (terminalLogs.isEmpty()) {
                                    item {
                                        Text(
                                            text = "apkforge@device:~$ اضغط 'تثبيت والتحقق من الأدوات' أو اكتب أمراً أدناه (help).",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp,
                                            color = TextMuted,
                                            style = LocalTextStyle.current.copy(textDirection = androidx.compose.ui.text.style.TextDirection.ContentOrLtr)
                                        )
                                    }
                                } else {
                                    items(terminalLogs, key = { it.id }) { line ->
                                        val color = when (line.type) {
                                            TerminalLineType.CMD -> Color(0xFF38BDF8)
                                            TerminalLineType.SUCCESS -> TerminalGreen
                                            TerminalLineType.WARN -> WarningYellow
                                            TerminalLineType.ERROR -> ErrorRed
                                            TerminalLineType.INFO -> Color(0xFFCBD5E1)
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Text(
                                                text = "[${line.timestamp}] ",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 10.sp,
                                                color = TextMuted,
                                                style = LocalTextStyle.current.copy(textDirection = androidx.compose.ui.text.style.TextDirection.Ltr),
                                                lineHeight = 14.sp
                                            )
                                            Text(
                                                text = line.text,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 10.sp,
                                                color = color,
                                                style = LocalTextStyle.current.copy(textDirection = androidx.compose.ui.text.style.TextDirection.ContentOrLtr),
                                                lineHeight = 14.sp,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Interactive Command Prompt
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedTextField(
                                value = commandInput,
                                onValueChange = { commandInput = it },
                                placeholder = {
                                    Text(
                                        text = "اكتب أمراً (مثال: verify أو gen-key أو ls)...",
                                        fontSize = 10.sp,
                                        color = TextMuted
                                    )
                                },
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = TerminalGreen
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    val cmd = commandInput
                                    commandInput = ""
                                    scope.launch { toolchainManager.runInteractiveCommand(cmd, activeProject) }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(OrangePrimary)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "تنفيذ",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolChipCard(
    tool: ToolchainToolStatus,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = Color(0xFF0A0E17),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (tool.isInstalled) StatusGreen.copy(alpha = 0.45f) else Color(0xFF1F2838)
        ),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(9.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tool.categoryAr,
                    fontFamily = CairoFontFamily,
                    fontSize = 9.sp,
                    color = OrangePrimary,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = if (tool.isInstalled) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (tool.isInstalled) StatusGreen else TextMuted,
                    modifier = Modifier.size(13.dp)
                )
            }
            Text(
                text = tool.titleAr,
                fontFamily = CairoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (tool.isInstalled) "v${tool.version} • SHA:${tool.verifiedSha256.take(6)}" else "v${tool.version} • غير مثبت",
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = if (tool.isInstalled) StatusGreen else TextMuted,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun QuickTerminalPill(label: String, onClick: () -> Unit) {
    Surface(
        color = Color(0xFF141D2D),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF283750)),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        Text(
            text = "$ $label",
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = Color(0xFF38BDF8),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
