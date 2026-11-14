package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun TerminalLogView(
    logs: List<String>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    var selectedFilter by remember { mutableStateOf("الكل") }

    val filteredLogs = remember(logs, selectedFilter) {
        when (selectedFilter) {
            "المراحل" -> logs.filter {
                it.contains("🚀") || it.contains("⚙️") || it.contains("🌐") ||
                    it.contains("🔨") || it.contains("🎨") || it.contains("🔌") || it.contains("🔐")
            }
            "النتائج" -> logs.filter {
                it.contains("✅") || it.contains("🎉") || it.contains("📁") ||
                    it.contains("📲") || it.contains("⚠️") || it.contains("❌")
            }
            else -> logs
        }
    }

    LaunchedEffect(filteredLogs.size) {
        if (filteredLogs.isNotEmpty()) {
            listState.animateScrollToItem(filteredLogs.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(TerminalBackground)
            .border(1.dp, GlassBorderSubtle, RoundedCornerShape(16.dp))
            .testTag("terminal_log_view")
    ) {
        // Terminal Window Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF10121A))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFFF5F56)))
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFFFBD2E)))
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF27C93F)))
                    Spacer(modifier = Modifier.width(10.dp))
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "quantum@apkforge (${logs.size})",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        style = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("الكل", "المراحل", "النتائج").forEach { filter ->
                    val active = selectedFilter == filter
                    Surface(
                        color = if (active) OrangePrimary.copy(alpha = 0.2f) else Color(0xFF1A1E2B),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.clickable { selectedFilter = filter }
                    ) {
                        Text(
                            text = filter,
                            fontFamily = CairoFontFamily,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 10.sp,
                            color = if (active) OrangePrimary else TextSecondary,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }

                IconButton(
                    modifier = Modifier.size(26.dp),
                    onClick = {
                        val fullLog = logs.joinToString("\n")
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Terminal Logs", fullLog))
                        Toast.makeText(context, "تم نسخ سجلات البناء!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "نسخ السجلات",
                        tint = AmberPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // Terminal Content with separated LTR timestamp column to eliminate RTL scrambling
        if (filteredLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (logs.isEmpty()) "جاهز للبناء... اضغط 'بناء' لبدء خط التجميع الفعلي" else "لا توجد سجلات مطابقة لهذا الفلتر",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }
        } else {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    itemsIndexed(filteredLogs) { _, rawLine ->
                        val tsMatch = Regex("^\\[([0-9:]{5,8})]\\s*(.*)$").find(rawLine)
                        val timestamp = tsMatch?.groupValues?.getOrNull(1)
                        val body = tsMatch?.groupValues?.getOrNull(2) ?: rawLine

                        val color = when {
                            rawLine.contains("✅") || rawLine.contains("اكتمل") || rawLine.contains("🎉") -> TerminalGreen
                            rawLine.contains("❌") || rawLine.contains("خطأ") -> ErrorRed
                            rawLine.contains("⚠️") || rawLine.contains("تنبيه") -> WarningYellow
                            rawLine.contains("🚀") || rawLine.contains("⚙️") || rawLine.contains("🔐") -> AmberPrimary
                            else -> TerminalText
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            if (timestamp != null) {
                                Text(
                                    text = "[$timestamp] ",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = TextMuted,
                                    style = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
                                    lineHeight = 16.sp
                                )
                            }
                            Text(
                                text = body,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = color,
                                style = LocalTextStyle.current.copy(textDirection = TextDirection.ContentOrLtr),
                                lineHeight = 16.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}
