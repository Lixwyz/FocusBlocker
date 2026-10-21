package com.example.focusblocker

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

data class AppItem(val name: String, val packageName: String)

enum class ThemeMode {
    BLACK, WHITE, GRADIENT, CUSTOM
}

enum class ModalType {
    NONE, BLOCKED, ALLOWED, SETTINGS
}

data class GradientPreset(val name: String, val start: Color, val end: Color)

val ENGLISH_QUOTES = listOf(
    "Disconnect to reconnect.",
    "Own your attention.",
    "Exit the loop.",
    "Silence the noise.",
    "Be here now.",
    "Stop scrolling. Start living.",
    "Your time is finite.",
    "Reclaim your mind.",
    "Break the algorithm.",
    "Focus is freedom.",
    "Less screen, more life.",
    "Don't feed the machine.",
    "Wake up from the feed.",
    "Real life is offline.",
    "Protect your peace.",
    "Step out of the matrix.",
    "Attention is currency.",
    "Choose presence over pixels.",
    "Unplug your habits.",
    "Master your impulses.",
    "Nothing changes if nothing changes.",
    "Create, don't just consume.",
    "Control the device, or it controls you.",
    "Clarity comes from stillness.",
    "Look up from the glass.",
    "Dopamine is a trap.",
    "Stay sharp. Stay present.",
    "Discipline equals freedom.",
    "Guard your focus.",
    "Silence is power.",
    "Do what matters today.",
    "Escape the endless scroll.",
    "Mind over screen.",
    "You are not a notification.",
    "Live intentionally.",
    "Deep work, deep life.",
    "Starve your distractions.",
    "Reset your focus.",
    "Take back your hours.",
    "Simplicity is the ultimate focus."
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FocusAppScreen()
        }
    }
}

@Composable
fun FocusAppScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("focus_prefs", Context.MODE_PRIVATE) }

    // Режим приложения: false = Кастомный со списками (Версия 1), true = Ультра-минимализм Pure Black (Версия 2)
    var isUltraMinimalMode by remember {
        mutableStateOf(prefs.getBoolean("ultra_minimal_mode", true))
    }

    // Рандомная короткая надпись на английском при каждом заходе в приложение
    val currentQuote = remember { ENGLISH_QUOTES.random() }

    var activeModal by remember { mutableStateOf(ModalType.NONE) }

    var themeMode by remember { mutableStateOf(ThemeMode.BLACK) }
    var showColorStudio by remember { mutableStateOf(false) }

    var isCustomGradient by remember { mutableStateOf(true) }
    var r1 by remember { mutableFloatStateOf(0.06f) }
    var g1 by remember { mutableFloatStateOf(0.06f) }
    var b1 by remember { mutableFloatStateOf(0.10f) }

    var r2 by remember { mutableFloatStateOf(0.16f) }
    var g2 by remember { mutableFloatStateOf(0.12f) }
    var b2 by remember { mutableFloatStateOf(0.28f) }

    val gradientPresets = remember {
        listOf(
            GradientPreset("Obsidian", Color(0xFF0A0A0C), Color(0xFF1E1F26)),
            GradientPreset("Cyber", Color(0xFF0F0C29), Color(0xFF302B63)),
            GradientPreset("Forest", Color(0xFF091E1A), Color(0xFF1B4332)),
            GradientPreset("Sunset", Color(0xFF1F1124), Color(0xFF5A1846)),
            GradientPreset("Arctic", Color(0xFFE8EEF5), Color(0xFFC7D5E8))
        )
    }

    var isBlockerActive by remember { mutableStateOf(prefs.getBoolean("is_active", false)) }
    var blockedPackages by remember {
        mutableStateOf(prefs.getStringSet("blocked_apps", emptySet()) ?: emptySet())
    }

    val allApps = remember {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .map {
                AppItem(
                    name = it.loadLabel(pm).toString(),
                    packageName = it.activityInfo.packageName
                )
            }
            .distinctBy { it.packageName }
            .filter { it.packageName != context.packageName }
            .sortedBy { it.name.lowercase() }
    }

    val blockedList = allApps.filter { blockedPackages.contains(it.packageName) }
    val allowedList = allApps.filter { !blockedPackages.contains(it.packageName) }

    if (isUltraMinimalMode) {
        // ВЕРСИЯ 2: УЛЬТРА-МИНИМАЛИСТИЧНЫЙ ПОЛНОСТЬЮ ЧЕРНЫЙ ЭКРАН
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(24.dp)
        ) {
            // Крошечная кнопка настроек в углу
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(50))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(50))
                    .clickable { activeModal = ModalType.SETTINGS }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "НАСТРОЙКИ",
                    color = Color.White.copy(alpha = 0.65f),
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )
            }

            // Центр экрана: рандомная короткая фраза на английском + кнопка блокировки + 2 маленькие кнопки списков
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = currentQuote,
                    color = Color.White.copy(alpha = 0.75f),
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.6.sp
                )

                Spacer(modifier = Modifier.height(36.dp))

                // Главная меняющаяся кнопка: Включить блокировку / Выключить блокировку
                val btnBg = if (isBlockerActive) Color.Black else Color.White
                val btnText = if (isBlockerActive) Color.White else Color.Black

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.82f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(50))
                        .background(btnBg)
                        .border(1.5.dp, Color.White, RoundedCornerShape(50))
                        .clickable {
                            isBlockerActive = !isBlockerActive
                            prefs.edit().putBoolean("is_active", isBlockerActive).apply()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isBlockerActive) "ВЫКЛЮЧИТЬ БЛОКИРОВКУ" else "ВКЛЮЧИТЬ БЛОКИРОВКУ",
                        color = btnText,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Две маленькие кнопки: Блокируемые (+ добавить) и Разрешенные (+ добавить)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SmallBlackButton(
                        text = "БЛОКИРУЕМЫЕ (${blockedList.size}) +",
                        onClick = { activeModal = ModalType.BLOCKED }
                    )
                    SmallBlackButton(
                        text = "РАЗРЕШЕННЫЕ (${allowedList.size}) +",
                        onClick = { activeModal = ModalType.ALLOWED }
                    )
                }
            }
        }
    } else {
        // ВЕРСИЯ 1: КАСТОМНАЯ СТУДИЯ С ГРАДИЕНТАМИ И ОТКРЫТЫМИ СПИСКАМИ
        val startColor = when (themeMode) {
            ThemeMode.BLACK -> Color(0xFF08080A)
            ThemeMode.WHITE -> Color(0xFFF7F7FA)
            ThemeMode.GRADIENT, ThemeMode.CUSTOM -> Color(r1, g1, b1)
        }

        val endColor = when (themeMode) {
            ThemeMode.BLACK -> Color(0xFF08080A)
            ThemeMode.WHITE -> Color(0xFFF7F7FA)
            ThemeMode.GRADIENT -> Color(r2, g2, b2)
            ThemeMode.CUSTOM -> if (isCustomGradient) Color(r2, g2, b2) else Color(r1, g1, b1)
        }

        val backgroundBrush = Brush.verticalGradient(listOf(startColor, endColor))
        val avgLuminance = (startColor.luminance() + endColor.luminance()) / 2f
        val contentColor = if (avgLuminance > 0.5f) Color(0xFF111113) else Color(0xFFF5F5F7)
        val cardBgColor = contentColor.copy(alpha = 0.07f)
        val cardBorderColor = contentColor.copy(alpha = 0.15f)
        val subtleTextColor = contentColor.copy(alpha = 0.55f)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = backgroundBrush)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MinimalChip(
                            text = "ЧЕРНАЯ",
                            selected = themeMode == ThemeMode.BLACK && !showColorStudio,
                            contentColor = contentColor,
                            onClick = {
                                themeMode = ThemeMode.BLACK
                                showColorStudio = false
                            }
                        )
                        MinimalChip(
                            text = "БЕЛАЯ",
                            selected = themeMode == ThemeMode.WHITE && !showColorStudio,
                            contentColor = contentColor,
                            onClick = {
                                themeMode = ThemeMode.WHITE
                                showColorStudio = false
                            }
                        )
                        MinimalChip(
                            text = "ЦВЕТ",
                            selected = showColorStudio || themeMode == ThemeMode.CUSTOM || themeMode == ThemeMode.GRADIENT,
                            contentColor = contentColor,
                            onClick = { showColorStudio = !showColorStudio }
                        )
                    }

                    MinimalChip(
                        text = "НАСТРОЙКИ",
                        selected = false,
                        contentColor = contentColor,
                        onClick = { activeModal = ModalType.SETTINGS }
                    )
                }

                AnimatedVisibility(visible = showColorStudio) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(cardBgColor)
                            .border(1.dp, cardBorderColor, RoundedCornerShape(20.dp))
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "КАСТОМИЗАЦИЯ ФОНА",
                                color = contentColor,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            MinimalChip(
                                text = if (isCustomGradient) "РЕЖИМ: ГРАДИЕНТ" else "РЕЖИМ: СПЛОШНОЙ",
                                selected = isCustomGradient,
                                contentColor = contentColor,
                                onClick = {
                                    isCustomGradient = !isCustomGradient
                                    themeMode = ThemeMode.CUSTOM
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            gradientPresets.forEach { preset ->
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(listOf(preset.start, preset.end)))
                                        .border(1.5.dp, contentColor.copy(alpha = 0.5f), CircleShape)
                                        .clickable {
                                            themeMode = ThemeMode.GRADIENT
                                            isCustomGradient = true
                                            r1 = preset.start.red; g1 = preset.start.green; b1 = preset.start.blue
                                            r2 = preset.end.red; g2 = preset.end.green; b2 = preset.end.blue
                                        }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Основной тон (R / G / B)", color = subtleTextColor, fontSize = 11.sp)
                        ColorSliders(
                            r = r1, g = g1, b = b1, contentColor = contentColor,
                            onChange = { nr, ng, nb ->
                                themeMode = ThemeMode.CUSTOM
                                r1 = nr; g1 = ng; b1 = nb
                            }
                        )

                        if (isCustomGradient) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Второй тон градиента (R / G / B)", color = subtleTextColor, fontSize = 11.sp)
                            ColorSliders(
                                r = r2, g = g2, b = b2, contentColor = contentColor,
                                onChange = { nr, ng, nb ->
                                    themeMode = ThemeMode.CUSTOM
                                    r2 = nr; g2 = ng; b2 = nb
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = currentQuote.uppercase(),
                    color = subtleTextColor,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "ПОРА ПРИЙТИ В СЕБЯ.\nВЫЙДИ ИЗ СИСТЕМЫ.\nХВАТИТ СКРОЛЛИТЬ В ПУСТОТУ — ВЕРНИ КОНТРОЛЬ НАД ЖИЗНЬЮ.",
                    color = contentColor,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    lineHeight = 26.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        isBlockerActive = !isBlockerActive
                        prefs.edit().putBoolean("is_active", isBlockerActive).apply()
                    },
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = contentColor,
                        contentColor = startColor
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Text(
                        text = if (isBlockerActive) "ВЫКЛЮЧИТЬ БЛОКИРОВКУ" else "ВКЛЮЧИТЬ БЛОКИРОВКУ",
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = 0.8.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AppListCard(
                        modifier = Modifier.weight(1f),
                        title = "БЛОКИРУЕМЫЕ",
                        count = blockedList.size,
                        subtitle = "Тап — разблокировать",
                        apps = blockedList,
                        prefix = "✕",
                        contentColor = contentColor,
                        subtleTextColor = subtleTextColor,
                        cardBgColor = cardBgColor,
                        cardBorderColor = cardBorderColor,
                        onAppClick = { app ->
                            val updated = blockedPackages - app.packageName
                            blockedPackages = updated
                            prefs.edit().putStringSet("blocked_apps", updated).apply()
                        }
                    )

                    AppListCard(
                        modifier = Modifier.weight(1f),
                        title = "РАЗРЕШЕННЫЕ",
                        count = allowedList.size,
                        subtitle = "Тап — заблокировать",
                        apps = allowedList,
                        prefix = "✓",
                        contentColor = contentColor,
                        subtleTextColor = subtleTextColor,
                        cardBgColor = cardBgColor,
                        cardBorderColor = cardBorderColor,
                        onAppClick = { app ->
                            val updated = blockedPackages + app.packageName
                            blockedPackages = updated
                            prefs.edit().putStringSet("blocked_apps", updated).apply()
                        }
                    )
                }
            }
        }
    }

    // МОДАЛЬНЫЕ ОКНА (ДЛЯ ОТДЕЛЬНОГО УПРАВЛЕНИЯ СПИСКАМИ И НАСТРОЙКАМИ)
    if (activeModal == ModalType.BLOCKED) {
        AppSelectionModal(
            title = "БЛОКИРУЕМЫЕ ПРИЛОЖЕНИЯ",
            subtitle = "Отметь приложения для блокировки или сними галочку",
            allApps = allApps,
            blockedPackages = blockedPackages,
            isBlockedModal = true,
            onToggleApp = { pkg ->
                val updated = if (blockedPackages.contains(pkg)) blockedPackages - pkg else blockedPackages + pkg
                blockedPackages = updated
                prefs.edit().putStringSet("blocked_apps", updated).apply()
            },
            onDismiss = { activeModal = ModalType.NONE }
        )
    }

    if (activeModal == ModalType.ALLOWED) {
        AppSelectionModal(
            title = "РАЗРЕШЕННЫЕ ПРИЛОЖЕНИЯ",
            subtitle = "Нажми на приложение, чтобы перенести его в разрешенные / блокируемые",
            allApps = allApps,
            blockedPackages = blockedPackages,
            isBlockedModal = false,
            onToggleApp = { pkg ->
                val updated = if (blockedPackages.contains(pkg)) blockedPackages - pkg else blockedPackages + pkg
                blockedPackages = updated
                prefs.edit().putStringSet("blocked_apps", updated).apply()
            },
            onDismiss = { activeModal = ModalType.NONE }
        )
    }

    if (activeModal == ModalType.SETTINGS) {
        SettingsModal(
            isUltraMinimalMode = isUltraMinimalMode,
            onModeChange = { newMode ->
                isUltraMinimalMode = newMode
                prefs.edit().putBoolean("ultra_minimal_mode", newMode).apply()
            },
            onOpenAccessibility = {
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            },
            onDismiss = { activeModal = ModalType.NONE }
        )
    }
}

@Composable
fun SmallBlackButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0xFF0E0E10))
            .border(1.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(50))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.9f),
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )
    }
}

@Composable
fun AppSelectionModal(
    title: String,
    subtitle: String,
    allApps: List<AppItem>,
    blockedPackages: Set<String>,
    isBlockedModal: Boolean,
    onToggleApp: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var showAddPicker by remember { mutableStateOf(false) }

    val currentList = if (isBlockedModal) {
        allApps.filter { blockedPackages.contains(it.packageName) }
    } else {
        allApps.filter { !blockedPackages.contains(it.packageName) }
    }

    val candidatesToAdd = if (isBlockedModal) {
        allApps.filter { !blockedPackages.contains(it.packageName) }
    } else {
        allApps.filter { blockedPackages.contains(it.packageName) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.80f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF0A0A0C))
                .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                SmallBlackButton(
                    text = if (showAddPicker) "← К СПИСКУ" else "+ ДОБАВИТЬ",
                    onClick = { showAddPicker = !showAddPicker }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (showAddPicker) "Выбери приложения для добавления в этот список:" else subtitle,
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 11.sp
            )

            Divider(
                color = Color.White.copy(alpha = 0.15f),
                modifier = Modifier.padding(vertical = 12.dp)
            )

            val displayList = if (showAddPicker) candidatesToAdd else currentList

            if (displayList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (showAddPicker) "Нет доступных приложений" else "Список пуст. Нажми «+ ДОБАВИТЬ»",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 12.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(displayList) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.06f))
                                .clickable { onToggleApp(app.packageName) }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = app.name,
                                color = Color.White,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = if (showAddPicker) "+ ДОБАВИТЬ" else "УБРАТЬ ✕",
                                color = Color.White.copy(alpha = 0.65f),
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Text(
                    text = "ЗАКРЫТЬ",
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun SettingsModal(
    isUltraMinimalMode: Boolean,
    onModeChange: (Boolean) -> Unit,
    onOpenAccessibility: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF0C0C0E))
                .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(24.dp))
                .padding(22.dp)
        ) {
            Text(
                text = "НАСТРОЙКИ И РЕЖИМ ЭКРАНА",
                color = Color.White,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Выбери стиль интерфейса:",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Выбор версии 1 или 2
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isUltraMinimalMode) Color.White else Color.White.copy(alpha = 0.06f))
                        .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                        .clickable { onModeChange(true) }
                        .padding(14.dp)
                ) {
                    Column {
                        Text(
                            text = "● УЛЬТРА-МИНИМАЛИЗМ (PURE BLACK)",
                            color = if (isUltraMinimalMode) Color.Black else Color.White,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Полностью черный экран, фраза на английском, списки в отдельных окнах",
                            color = if (isUltraMinimalMode) Color.Black.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.55f),
                            fontSize = 10.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (!isUltraMinimalMode) Color.White else Color.White.copy(alpha = 0.06f))
                        .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                        .clickable { onModeChange(false) }
                        .padding(14.dp)
                ) {
                    Column {
                        Text(
                            text = "● РАСШИРЕННЫЙ (КАСТОМ ЦВЕТА И ГРАДИЕНТЫ)",
                            color = if (!isUltraMinimalMode) Color.Black else Color.White,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Две колонки со списками на экране, настройка темы и RGB-градиентов",
                            color = if (!isUltraMinimalMode) Color.Black.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.55f),
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            OutlinedButton(
                onClick = onOpenAccessibility,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "СИСТЕМНЫЙ ДОСТУП (СПЕЦВОЗМОЖНОСТИ)",
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "ГОТОВО",
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun MinimalChip(
    text: String,
    selected: Boolean,
    contentColor: Color,
    onClick: () -> Unit
) {
    val bg = if (selected) contentColor.copy(alpha = 0.18f) else contentColor.copy(alpha = 0.06f)
    val border = if (selected) contentColor.copy(alpha = 0.65f) else contentColor.copy(alpha = 0.16f)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(50))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text = text,
            color = contentColor,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )
    }
}

@Composable
fun ColorSliders(
    r: Float,
    g: Float,
    b: Float,
    contentColor: Color,
    onChange: (Float, Float, Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Slider(
            value = r,
            onValueChange = { onChange(it, g, b) },
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(thumbColor = contentColor, activeTrackColor = contentColor)
        )
        Slider(
            value = g,
            onValueChange = { onChange(r, it, b) },
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(thumbColor = contentColor, activeTrackColor = contentColor)
        )
        Slider(
            value = b,
            onValueChange = { onChange(r, g, it) },
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(thumbColor = contentColor, activeTrackColor = contentColor)
        )
    }
}

@Composable
fun AppListCard(
    modifier: Modifier = Modifier,
    title: String,
    count: Int,
    subtitle: String,
    apps: List<AppItem>,
    prefix: String,
    contentColor: Color,
    subtleTextColor: Color,
    cardBgColor: Color,
    cardBorderColor: Color,
    onAppClick: (AppItem) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(22.dp))
            .background(cardBgColor)
            .border(1.dp, cardBorderColor, RoundedCornerShape(22.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = contentColor,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(contentColor.copy(alpha = 0.14f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = count.toString(),
                    color = contentColor,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtitle,
            color = subtleTextColor,
            fontSize = 10.sp
        )

        Divider(
            color = cardBorderColor,
            modifier = Modifier.padding(vertical = 10.dp)
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(apps) { app ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(contentColor.copy(alpha = 0.05f))
                        .clickable { onAppClick(app) }
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$prefix  ${app.name}",
                        color = contentColor,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
