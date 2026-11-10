package com.example.drinksomewater

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin

private val GoalGreen = Color(0xFF26C6A5)

private fun fmt(n: Int): String = String.format(Locale.getDefault(), "%,d", n)

// ───────────────────────── shared pieces ─────────────────────────

@Composable
private fun AppCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.padding(20.dp), content = content)
    }
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= 5 && it.all(Char::isDigit)) onChange(it) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    )
}

// ───────────────────────── TODAY ─────────────────────────

@Composable
fun WaterWaveProgress(progress: Float, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "wave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)),
        label = "phase"
    )
    val animated by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "progress"
    )

    val colors = MaterialTheme.colorScheme
    val waterColor = colors.primary

    Box(
        modifier = modifier
            .size(240.dp)
            .clip(CircleShape)
            .background(colors.surfaceVariant)
            .border(6.dp, colors.outline.copy(alpha = 0.5f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val level = h * (1f - animated.coerceIn(0f, 1f))
            val ampFactor = (animated * 8f).coerceIn(0f, 1f) // flat when empty

            fun drawWave(amp: Float, shift: Float, color: Color) {
                val path = Path().apply {
                    moveTo(0f, h)
                    var x = 0f
                    while (x <= w) {
                        val y = level + amp * ampFactor *
                            sin((x / w) * 2f * PI.toFloat() + phase + shift)
                        lineTo(x, y)
                        x += 6f
                    }
                    lineTo(w, h)
                    close()
                }
                drawPath(path, color)
            }
            drawWave(10.dp.toPx(), PI.toFloat() / 2f, waterColor.copy(alpha = 0.40f))
            drawWave(12.dp.toPx(), 0f, waterColor.copy(alpha = 0.90f))
        }

        Text(
            text = "${(animated * 100).toInt()}%",
            fontSize = 44.sp,
            fontWeight = FontWeight.Bold,
            color = if (animated > 0.55f) Color.White else colors.onSurface
        )
    }
}

@Composable
fun TodayScreen(repo: WaterRepository, refreshKey: Int, onDataChanged: () -> Unit) {
    var consumed by remember { mutableIntStateOf(repo.getConsumed()) }
    var goal by remember { mutableIntStateOf(repo.getGoal()) }
    var streak by remember { mutableIntStateOf(repo.getStreak()) }
    var log by remember { mutableStateOf(repo.getTodayLog()) }
    var custom by remember { mutableStateOf("") }
    val focus = LocalFocusManager.current

    fun refresh() {
        consumed = repo.getConsumed()
        goal = repo.getGoal()
        streak = repo.getStreak()
        log = repo.getTodayLog()
    }

    LaunchedEffect(refreshKey) { refresh() }

    fun add(amount: Int) {
        repo.addWater(amount)
        refresh()
        onDataChanged()
    }

    val progress = if (goal > 0) consumed.toFloat() / goal else 0f
    val remaining = (goal - consumed).coerceAtLeast(0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Today",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    if (remaining == 0) "Goal reached 🎉" else "${fmt(remaining)} ml to go",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (streak > 0) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        "🔥 $streak ${if (streak == 1) "day" else "days"}",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        WaterWaveProgress(progress)

        Text(
            "${fmt(consumed)} / ${fmt(goal)} ml",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(150, 250, 500).forEach { amount ->
                Button(
                    onClick = { add(amount) },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("+$amount", fontWeight = FontWeight.Bold)
                }
            }
        }

        AppCard {
            Text(
                "Add a specific amount",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                NumberField("Amount (ml)", custom, { custom = it }, Modifier.weight(1f))
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = {
                        val amount = custom.toIntOrNull()
                        if (amount != null && amount > 0) {
                            add(amount)
                            custom = ""
                            focus.clearFocus()
                        }
                    },
                    modifier = Modifier.height(56.dp),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Add") }
            }
        }

        AppCard {
            Text(
                "Today's log",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            if (log.isEmpty()) {
                Text(
                    "Nothing logged yet",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                log.withIndex().reversed().take(10).forEach { (i, entry) ->
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "💧 ${fmt(entry.ml)} ml",
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            entry.time,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        IconButton(onClick = {
                            repo.removeLogEntry(i)
                            refresh()
                            onDataChanged()
                        }) {
                            Text("✕", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

// ───────────────────────── HISTORY ─────────────────────────

@Composable
private fun WeekBarChart(
    days: List<DayTotal>,
    goal: Int,
    selected: Int?,
    onSelect: (Int?) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val maxVal = max(goal, days.maxOf { it.ml }).coerceAtLeast(1) * 1.15f
    val goalLine = colors.onSurfaceVariant.copy(alpha = 0.6f)

    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(190.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                val y = size.height * (1f - goal / maxVal)
                drawLine(
                    color = goalLine,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f))
                )
            }
            Row(
                Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                days.forEachIndexed { i, d ->
                    val frac by animateFloatAsState(
                        targetValue = d.ml / maxVal,
                        animationSpec = tween(700, easing = FastOutSlowInEasing),
                        label = "bar$i"
                    )
                    val base = if (d.ml >= goal) GoalGreen else colors.primary
                    val barColor =
                        if (selected != null && selected != i) base.copy(alpha = 0.40f) else base

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onSelect(if (selected == i) null else i) },
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.surfaceVariant),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            if (!d.isFuture) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(frac.coerceIn(0.001f, 1f))
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(barColor)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            days.forEachIndexed { i, d ->
                Text(
                    text = d.label.take(3),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    fontWeight = if (d.isToday || selected == i) FontWeight.Bold else FontWeight.Normal,
                    color = if (d.isToday || selected == i) colors.primary else colors.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun HistoryScreen(repo: WaterRepository, refreshKey: Int) {
    var weekOffset by remember { mutableIntStateOf(0) }
    var selected by remember(weekOffset) { mutableStateOf<Int?>(null) }

    val week = remember(weekOffset, refreshKey) { repo.getWeek(weekOffset) }
    val goal = remember(refreshKey) { repo.getGoal() }

    val past = week.filter { !it.isFuture }
    val total = week.sumOf { it.ml }
    val avg = if (past.isEmpty()) 0 else past.sumOf { it.ml } / past.size
    val best = week.maxOf { it.ml }
    val met = past.count { it.ml >= goal }

    val rangeFmt = SimpleDateFormat("d MMM", Locale.getDefault())
    val rangeLabel =
        "${rangeFmt.format(Date(week.first().millis))} – ${rangeFmt.format(Date(week.last().millis))}"
    val title = when (weekOffset) {
        0 -> "This week"
        -1 -> "Last week"
        else -> "${-weekOffset} weeks ago"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "History",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        AppCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { weekOffset-- },
                    enabled = weekOffset > -WaterRepository.MAX_WEEKS_BACK
                ) { Text("‹", fontSize = 28.sp, color = MaterialTheme.colorScheme.primary) }

                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        title,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        rangeLabel,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = { weekOffset++ }, enabled = weekOffset < 0) {
                    Text("›", fontSize = 28.sp, color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(Modifier.height(8.dp))

            val sel = selected?.let { week[it] }
            Text(
                if (sel != null) SimpleDateFormat("EEEE, d MMM", Locale.getDefault())
                    .format(Date(sel.millis)) else "Weekly total",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "${fmt(sel?.ml ?: total)} ml",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (sel != null && goal > 0) {
                Text(
                    "${sel.ml * 100 / goal}% of daily goal",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(16.dp))
            WeekBarChart(week, goal, selected) { selected = it }
            Spacer(Modifier.height(12.dp))
            Text(
                "- - -  Daily goal ${fmt(goal)} ml    ■ goal met",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Daily avg", "${fmt(avg)} ml", Modifier.weight(1f))
            StatCard("Best day", "${fmt(best)} ml", Modifier.weight(1f))
            StatCard("Goal met", "$met / ${past.size}", Modifier.weight(1f))
        }
    }
}

// ───────────────────────── SETTINGS ─────────────────────────

@Composable
fun SettingsScreen(
    repo: WaterRepository,
    themeMode: Int,
    onThemeMode: (Int) -> Unit,
    onDataChanged: () -> Unit
) {
    val context = LocalContext.current
    var goalInput by remember { mutableStateOf(repo.getGoal().toString()) }
    var widgetInput by remember { mutableStateOf(repo.getWidgetAmount().toString()) }
    var showReset by remember { mutableStateOf(false) }
    var widgetDisplay by remember { mutableIntStateOf(repo.getWidgetDisplay()) }
    var widgetTheme by remember { mutableIntStateOf(repo.getWidgetTheme()) }

    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

    val pickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val file = File(context.filesDir, "emote_image")
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                WaterWidgetProvider.updateAllWidgets(context)
                toast("Emote updated")
            } catch (e: Exception) {
                toast("Failed to update emote")
            }
        }
    }

    if (showReset) {
        AlertDialog(
            onDismissRequest = { showReset = false },
            title = { Text("Reset today's water?") },
            text = { Text("This sets today's intake back to 0 ml. Past days are not affected.") },
            confirmButton = {
                TextButton(onClick = {
                    repo.resetConsumed()
                    onDataChanged()
                    showReset = false
                    toast("Today reset")
                }) { Text("Reset") }
            },
            dismissButton = { TextButton(onClick = { showReset = false }) { Text("Cancel") } }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Settings",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        AppCard {
            Text("Daily goal", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                NumberField("Goal (ml)", goalInput, { goalInput = it }, Modifier.weight(1f))
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = {
                        val g = goalInput.toIntOrNull()
                        if (g != null && g > 0) {
                            repo.setGoal(g)
                            onDataChanged()
                            toast("Goal saved")
                        }
                    },
                    modifier = Modifier.height(56.dp),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Save") }
            }
        }

        AppCard {
            Text("Widget button", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(
                "How much one tap on the widget's + adds",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                NumberField("Amount (ml)", widgetInput, { widgetInput = it }, Modifier.weight(1f))
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = {
                        val a = widgetInput.toIntOrNull()
                        if (a != null && a > 0) {
                            repo.setWidgetAmount(a)
                            onDataChanged()
                            toast("Widget amount saved")
                        }
                    },
                    modifier = Modifier.height(56.dp),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Save") }
            }
        }

        AppCard {
            Text("Widget shows", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Percentage", "Amount (ml)").forEachIndexed { i, label ->
                    val mod = Modifier.weight(1f)
                    val choose = {
                        widgetDisplay = i
                        repo.setWidgetDisplay(i)
                        onDataChanged()
                    }
                    if (widgetDisplay == i) {
                        Button(onClick = choose, modifier = mod, shape = RoundedCornerShape(12.dp)) { Text(label) }
                    } else {
                        OutlinedButton(onClick = choose, modifier = mod, shape = RoundedCornerShape(12.dp)) { Text(label) }
                    }
                }
            }
        }

        AppCard {
            Text("Widget Theme", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Light", "Dark").forEachIndexed { i, label ->
                    val mod = Modifier.weight(1f)
                    val choose = {
                        widgetTheme = i
                        repo.setWidgetTheme(i)
                        WaterWidgetProvider.updateAllWidgets(context)
                        onDataChanged()
                    }
                    if (widgetTheme == i) {
                        Button(onClick = choose, modifier = mod, shape = RoundedCornerShape(12.dp)) { Text(label) }
                    } else {
                        OutlinedButton(onClick = choose, modifier = mod, shape = RoundedCornerShape(12.dp)) { Text(label) }
                    }
                }
            }
        }

        AppCard {
            Text("Widget Emote", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(
                "Select an image for the 3-wide and 2x2 widgets.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { pickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) { Text("Select Emote") }
        }

        AppCard {
            Text("Appearance", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("System", "Light", "Dark").forEachIndexed { i, label ->
                    val mod = Modifier.weight(1f)
                    if (themeMode == i) {
                        Button(onClick = { onThemeMode(i) }, modifier = mod, shape = RoundedCornerShape(12.dp)) {
                            Text(label)
                        }
                    } else {
                        OutlinedButton(onClick = { onThemeMode(i) }, modifier = mod, shape = RoundedCornerShape(12.dp)) {
                            Text(label)
                        }
                    }
                }
            }
        }

        OutlinedButton(
            onClick = { showReset = true },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp)
        ) { Text("Reset today's water") }
    }
}
