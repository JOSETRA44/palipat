 package com.example.palipat
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.Alignment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import com.example.palipat.data.TimerStorage
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import androidx.core.net.toUri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateContentSize
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import com.example.palipat.ui.theme.PalipatTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PalipatTheme {
                TimerApp()
            }
        }
    }
}

// Modelo de actividad con tiempo
private enum class Mode { Countdown, Stopwatch }

private data class TaskTimer(
    val id: Int,
    val name: String,
    val mode: Mode,
    val durationMillis: Long, // solo relevante para Countdown
    val remainingMillis: Long = durationMillis, // para Countdown
    val elapsedMillis: Long = 0L, // para Stopwatch
    val isRunning: Boolean = false,
    val lastStartedAt: Long? = null // epoch millis cuando se inició por última vez
)

 @OptIn(ExperimentalMaterial3Api::class)
 @Composable
 private fun TimerApp() {
     val context = LocalContext.current
     val storage = remember { TimerStorage(context) }
     val scope = rememberCoroutineScope()
     var showAddDialog by remember { mutableStateOf(false) }
     var showStats by remember { mutableStateOf(false) }
     var showSettings by remember { mutableStateOf(false) }
     val items = remember { mutableStateListOf<TaskTimer>() }
     var nextId by remember { mutableIntStateOf(1) }
     var selectedRange by remember { mutableStateOf("Hoy") }
     var customStartMillis by remember { mutableStateOf<Long?>(null) }
     var customEndMillis by remember { mutableStateOf<Long?>(null) }
     var showStartPicker by remember { mutableStateOf(false) }
     var showEndPicker by remember { mutableStateOf(false) }
     val soundUriState = storage.observeSoundUri().collectAsState(initial = null)
     val snackbarHostState = remember { SnackbarHostState() }

     fun updateItem(id: Int, transform: (TaskTimer) -> TaskTimer) {
        val index = items.indexOfFirst { it.id == id }
        if (index != -1) items[index] = transform(items[index])
    }

     Scaffold(
         modifier = Modifier.fillMaxSize(),
         containerColor = MaterialTheme.colorScheme.background,
         snackbarHost = { SnackbarHost(snackbarHostState) },
         topBar = { CenterAlignedTopAppBar(
             title = { Text("control de horas", fontWeight = FontWeight.SemiBold) },
             colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                 containerColor = Color(0xFFFFC107),
                 titleContentColor = Color(0xFF1B1B1B),
                 actionIconContentColor = Color(0xFF1B1B1B),
                 navigationIconContentColor = Color(0xFF1B1B1B)
             ),
             actions = {
                 TextButton(onClick = { showStats = !showStats }) { Text(if (showStats) "Tareas" else "Estadísticas", color = Color(0xFF1B1B1B)) }
                 TextButton(onClick = { showSettings = true }) { Text("Ajustes", color = Color(0xFF1B1B1B)) }
             }
         ) },
         floatingActionButton = {
             Column(
                 verticalArrangement = Arrangement.spacedBy(12.dp),
                 horizontalAlignment = Alignment.End
             ) {
                 // Burbuja de estadísticas
                 val statsSelected = showStats
                 SmallFloatingActionButton(
                     onClick = { showStats = !showStats },
                     containerColor = if (statsSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                     contentColor = if (statsSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                 ) {
                     Text(if (statsSelected) "📈" else "📊")
                 }
                 if (selectedRange == "Personalizado") {
                     Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                         val fmt = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
                         val sTxt = customStartMillis?.let { fmt.format(java.util.Date(it)) } ?: "Inicio"
                         val eTxt = customEndMillis?.let { fmt.format(java.util.Date(it)) } ?: "Fin"
                         FilterChip(selected = false, onClick = { showStartPicker = true }, label = { Text(sTxt) }, shape = RoundedCornerShape(20.dp))
                         FilterChip(selected = false, onClick = { showEndPicker = true }, label = { Text(eTxt) }, shape = RoundedCornerShape(20.dp))
                     }
                 }

                 // FAB principal para agregar
                 val rot by animateFloatAsState(targetValue = if (showAddDialog) 45f else 0f, animationSpec = tween(200), label = "fabRot")
                 FloatingActionButton(
                     onClick = { showAddDialog = true },
                     containerColor = Color.Transparent,
                     contentColor = Color.White,
                     shape = RoundedCornerShape(28.dp),
                     elevation = FloatingActionButtonDefaults.elevation(8.dp),
                     modifier = Modifier
                         .background(
                             brush = Brush.horizontalGradient(colors = listOf(Color(0xFFFF5722), Color(0xFFFFC107))),
                             shape = RoundedCornerShape(28.dp)
                         )
                 ) { Text("+", modifier = Modifier.rotate(rot)) }
             }
         }
     ) { innerPadding ->
         Box(
             modifier = Modifier
                 .fillMaxSize()
                 .background(
                     Brush.verticalGradient(listOf(Color(0xFF0A1931), Color(0xFF1E3A5F)))
                 )
                 .padding(innerPadding)
         ) {
             // sombra radial sutil como en estadísticas
             Box(
                 modifier = Modifier
                     .matchParentSize()
                     .background(
                         Brush.radialGradient(
                             colors = listOf(Color(0x33243B55), Color.Transparent),
                             center = Offset(200f, 200f),
                             radius = 500f
                         )
                     )
             )
             if (showStats) {
                 StatsScreen(storage = storage)
             } else if (items.isEmpty()) {
                 EmptyState()
             } else {
                 TaskList(
                   items = items,
                   storage = storage,
                   onStart = { id -> updateItem(id) {
                        val now = System.currentTimeMillis()
                        if (it.isRunning) it else it.copy(isRunning = true, lastStartedAt = now)
                    } },
                    onPause = { id -> updateItem(id) {
                         if (!it.isRunning) it else {
                             val now = System.currentTimeMillis()
                             val delta = (now - (it.lastStartedAt ?: now)).coerceAtLeast(0L)
                             // Log de sesión al pausar
                             scope.launch {
                                 storage.appendSession(
                                     TimerStorage.SessionLog(
                                         timerId = it.id,
                                         name = it.name,
                                         mode = if (it.mode == Mode.Stopwatch) "Stopwatch" else "Countdown",
                                         startTime = (it.lastStartedAt ?: now - delta),
                                         endTime = now,
                                         durationMillis = delta
                                     )
                                 )
                             }
                             when (it.mode) {
                                 Mode.Stopwatch -> it.copy(
                                     isRunning = false,
                                     lastStartedAt = null,
                                     elapsedMillis = it.elapsedMillis + delta
                                 )
                                 Mode.Countdown -> {
                                     val newRem = (it.remainingMillis - delta).coerceAtLeast(0L)
                                     it.copy(
                                         isRunning = false,
                                         lastStartedAt = null,
                                         remainingMillis = newRem
                                     )
                                 }
                             }
                         }
                     } },
                     onReset = { id -> updateItem(id) { t ->
                         if (t.mode == Mode.Countdown) t.copy(isRunning = false, remainingMillis = t.durationMillis)
                         else t.copy(isRunning = false, elapsedMillis = 0L, lastStartedAt = null)
                     } },
                     onDelete = { id -> items.removeAll { it.id == id } },
                     onTick = { id ->
                         updateItem(id) { t ->
                             if (!t.isRunning) t else when (t.mode) {
                                 Mode.Countdown -> {
                                    val newRem = (t.remainingMillis - 1000L).coerceAtLeast(0L)
                                    t.copy(
                                        remainingMillis = newRem,
                                        isRunning = if (newRem <= 0L) false else t.isRunning,
                                        lastStartedAt = if (newRem <= 0L) null else t.lastStartedAt
                                    )
                                }
                                 Mode.Stopwatch -> t.copy(elapsedMillis = t.elapsedMillis + 1000L)
                             }
                         }
                     },
                     onFinished = { finishedItem ->
                        // Vibrar
                        vibrateOnce(context, 500)
                         // Sonido (personalizable con fallback por defecto)
                         runCatching {
                             val chosen = soundUriState.value?.toUri()
                             val uri: Uri = chosen ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                             val ring: Ringtone? = RingtoneManager.getRingtone(context, uri)
                             ring?.play()
                         }
                         // Registrar sesión final si venía corriendo
                         if (finishedItem.lastStartedAt != null) {
                             val end = System.currentTimeMillis()
                             val delta = (end - finishedItem.lastStartedAt).coerceAtLeast(0L)
                             scope.launch {
                                 storage.appendSession(
                                     TimerStorage.SessionLog(
                                         timerId = finishedItem.id,
                                         name = finishedItem.name,
                                         mode = "Countdown",
                                         startTime = finishedItem.lastStartedAt,
                                         endTime = end,
                                         durationMillis = delta
                                     )
                                 )
                             }
                         }
                          // Snackbar con acción Reiniciar
                          scope.launch {
                              val res = snackbarHostState.showSnackbar(
                                  message = "Actividad terminada: ${finishedItem.name}",
                                  actionLabel = "Reiniciar",
                                  withDismissAction = true,
                                  duration = SnackbarDuration.Short
                              )
                              if (res == SnackbarResult.ActionPerformed) {
                                  updateItem(finishedItem.id) { t ->
                                      if (t.mode == Mode.Countdown) t.copy(isRunning = false, remainingMillis = t.durationMillis)
                                      else t.copy(isRunning = false, elapsedMillis = 0L, lastStartedAt = null)
                                  }
                              }
                          }
                    }
                )
             }
         }
     }

     // Cargar desde DataStore al iniciar (solo una vez)
     LaunchedEffect(Unit) {
        val saved: List<com.example.palipat.data.TimerStorage.TimerRecord> = storage.observe().first()
        if (saved.isNotEmpty()) {
            items.clear()
            saved.forEach { r: com.example.palipat.data.TimerStorage.TimerRecord ->
                val now = System.currentTimeMillis()
                val recMode = if (r.mode == "Stopwatch") Mode.Stopwatch else Mode.Countdown
                // Recalcular tiempo transcurrido si estaba corriendo
                val (newElapsed, newRemaining, stillRunning) = if (r.isRunning && r.lastStartedAt != null) {
                    val delta = (now - r.lastStartedAt).coerceAtLeast(0L)
                    when (recMode) {
                        Mode.Stopwatch -> Triple(r.elapsedMillis + delta, r.remainingMillis, true)
                        Mode.Countdown -> {
                            val rem = (r.remainingMillis - delta).coerceAtLeast(0L)
                            Triple(r.elapsedMillis, rem, rem > 0L)
                        }
                    }
                } else Triple(r.elapsedMillis, r.remainingMillis, false)

                items.add(
                    TaskTimer(
                        id = r.id,
                        name = r.name,
                        mode = recMode,
                        durationMillis = r.durationMillis,
                        remainingMillis = newRemaining,
                        elapsedMillis = newElapsed,
                        isRunning = stillRunning,
                        lastStartedAt = if (stillRunning) now else null
                    )
                )
                nextId = maxOf(nextId, r.id + 1)
            }
        }
    }

     // Guardar en DataStore cada vez que cambie la lista o algún campo
     LaunchedEffect(items) {
        snapshotFlow { items.map { it.copy() } }.collectLatest { list ->
            val records = list.map { t ->
                com.example.palipat.data.TimerStorage.TimerRecord(
                    id = t.id,
                    name = t.name,
                    mode = if (t.mode == Mode.Stopwatch) "Stopwatch" else "Countdown",
                    durationMillis = t.durationMillis,
                    remainingMillis = t.remainingMillis,
                    elapsedMillis = t.elapsedMillis,
                    isRunning = t.isRunning,
                    lastStartedAt = t.lastStartedAt
                )
            }
            storage.save(records)
        }
    }

     if (showAddDialog) {
         AddTaskDialog(
             onDismiss = { showAddDialog = false },
             onConfirm = { name, minutes, mode ->
                 val duration = (minutes.coerceAtLeast(0)) * 60_000L
                 val finalName = name.ifBlank { "Actividad ${nextId}" }
                 val now = System.currentTimeMillis()
                 val item = if (mode == Mode.Countdown) {
                     TaskTimer(
                         id = nextId++,
                         name = finalName,
                         mode = Mode.Countdown,
                         durationMillis = duration,
                         remainingMillis = duration,
                         elapsedMillis = 0L,
                         isRunning = true,
                         lastStartedAt = now
                     )
                 } else {
                     TaskTimer(
                         id = nextId++,
                         name = finalName,
                         mode = Mode.Stopwatch,
                         durationMillis = 0L,
                         remainingMillis = 0L,
                         elapsedMillis = 0L,
                         isRunning = true,
                         lastStartedAt = now
                     )
                 }
                 items.add(item)
                 showAddDialog = false
             }
         )
     }

     if (showSettings) {
         SettingsDialog(
             currentUri = soundUriState.value,
             onPickNew = { uri -> 
                 scope.launch { storage.setSoundUri(uri) }
             },
             onDismiss = { showSettings = false }
         )
     }
 }

 @Composable
 private fun TaskList(
      items: List<TaskTimer>,
      onStart: (Int) -> Unit,
      onPause: (Int) -> Unit,
      onReset: (Int) -> Unit,
      onDelete: (Int) -> Unit,
      onTick: (Int) -> Unit,
      onFinished: (TaskTimer) -> Unit,
      storage: TimerStorage,
  ) {
     val finishedNotified = remember { mutableStateListOf<Int>() }
      LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
          items(items, key = { it.id }) { item ->
            if (item.isRunning && (item.mode == Mode.Stopwatch || item.remainingMillis > 0)) {
                LaunchedEffect(item.id, item.isRunning, item.remainingMillis, item.elapsedMillis) {
                    delay(1000)
                    onTick(item.id)
                }
            }

            // Detectar fin de cuenta regresiva y notificar una sola vez
            if (item.mode == Mode.Countdown) {
                if (item.remainingMillis <= 0 && item.id !in finishedNotified) {
                    finishedNotified.add(item.id)
                    onFinished(item)
                } else if (item.remainingMillis > 0 && item.id in finishedNotified) {
                    // Se reinició o volvió a estar con tiempo: permitir notificar de nuevo en un próximo fin
                    finishedNotified.remove(item.id)
                }
            }

             AnimatedVisibility(
                 visible = true,
                 enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn()
             ) {
                 TaskRow(
                     item = item,
                     onStart = { onStart(item.id) },
                     onPause = { onPause(item.id) },
                     onReset = { onReset(item.id) },
                     onDelete = { onDelete(item.id) },
                     storage = storage
                 )
             }
          }
      }
  }

 @OptIn(ExperimentalMaterial3Api::class)
 @Composable
 private fun StatsScreen(storage: TimerStorage) {
     val sessions by storage.observeSessions().collectAsState(initial = emptyList())
     val timers by storage.observe().collectAsState(initial = emptyList())
     val context = LocalContext.current
     // Construir sesiones en curso (live) para que se reflejen en estadísticas
     val nowStat = System.currentTimeMillis()
     val liveSessions = remember(timers) {
         timers.filter { it.isRunning }.map { tr ->
             val elapsedLive = if (tr.mode == "Countdown") {
                 (tr.durationMillis - tr.remainingMillis).coerceAtLeast(0L)
             } else {
                 val base = tr.elapsedMillis
                 val extra = tr.lastStartedAt?.let { nowStat - it } ?: 0L
                 (base + extra).coerceAtLeast(0L)
             }
             com.example.palipat.data.TimerStorage.SessionLog(
                 timerId = tr.id,
                 name = tr.name,
                 mode = tr.mode,
                 startTime = tr.lastStartedAt ?: (nowStat - elapsedLive),
                 endTime = nowStat,
                 durationMillis = elapsedLive
             )
         }
     }
     val allSessions = remember(sessions, liveSessions) { sessions + liveSessions }
     val allActivities = remember(allSessions) { allSessions.map { it.name }.distinct().sorted() }
     var selectedActivity by remember { mutableStateOf<String?>(null) }
     var selectedRange by remember { mutableStateOf("Hoy") } // Hoy, Semana, Mes, Personalizado
     var selectedTab by remember { mutableIntStateOf(0) } // 0 Cards, 1 Circular, 2 Barras
     var showStartPicker by remember { mutableStateOf(false) }
     var showEndPicker by remember { mutableStateOf(false) }
     val startState = rememberDatePickerState()
     val endState = rememberDatePickerState()
     var customStartMillis by remember { mutableStateOf<Long?>(null) }
     var customEndMillis by remember { mutableStateOf<Long?>(null) }
     var selectedSort by remember { mutableStateOf("Más tiempo") } // Más tiempo, Menos tiempo, Promedio

     val now = System.currentTimeMillis()
     val rangeCutoff: Long? = when (selectedRange) {
         "Hoy" -> now - 24L * 60 * 60 * 1000
         "Semana" -> now - 7L * 24 * 60 * 60 * 1000
         "Mes" -> now - 30L * 24 * 60 * 60 * 1000
         else -> null
     }

     val filtered by remember(allSessions, selectedActivity, selectedRange, customStartMillis, customEndMillis, rangeCutoff) {
         derivedStateOf {
             allSessions.filter { s ->
                 val activityOk = selectedActivity?.let { s.name == it } ?: true
                 val modeOk = true
                 val eventTime = s.endTime
                 val timeOk = if (selectedRange == "Personalizado") {
                     val st = customStartMillis
                     val en = customEndMillis
                     if (st != null && en != null) eventTime in st..en else true
                 } else {
                     rangeCutoff?.let { eventTime >= it } ?: true
                 }
                 activityOk && modeOk && timeOk
             }
         }
     }

     Box(
         modifier = Modifier
             .fillMaxSize()
             .background(
                 brush = Brush.verticalGradient(listOf(Color(0xFF0A1931), Color(0xFF1E3A5F)))
             )
     ) {
         // sombra radial sutil
         Box(
             modifier = Modifier
                 .matchParentSize()
                 .background(
                     Brush.radialGradient(
                         colors = listOf(Color(0x33243B55), Color.Transparent),
                         center = Offset(200f, 200f),
                         radius = 500f
                     )
                 )
         )

         Column(
             modifier = Modifier
                 .fillMaxSize()
                 .padding(16.dp)
                 .verticalScroll(rememberScrollState()),
             verticalArrangement = Arrangement.spacedBy(16.dp)
         ) {
             Text("Estadísticas", style = MaterialTheme.typography.headlineMedium, color = Color.White)

             // Filtros organizados
         Card(
             shape = RoundedCornerShape(16.dp),
             colors = CardDefaults.cardColors(containerColor = Color(0x22FFFFFF))
         ) {
             Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                 // Rango de fechas
                 Text("Rango", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.labelLarge)
                 Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                     listOf("Hoy","Semana","Mes","Personalizado").forEach { r ->
                         val selected = r == selectedRange
                         FilterChip(
                             selected = selected,
                             onClick = {
                                 selectedRange = r
                                 if (r == "Personalizado") {
                                     showStartPicker = true
                                 }
                             },
                             label = { Text(r) },
                             colors = FilterChipDefaults.filterChipColors(
                                 containerColor = if (selected) Color.Unspecified else Color(0xFFF5F5F5),
                                 labelColor = if (selected) Color.White else Color(0xFF333333),
                                 selectedContainerColor = MaterialTheme.colorScheme.primary
                             ),
                             shape = RoundedCornerShape(20.dp)
                         )
                     }
                 }
                 // Actividad
                 Text("Actividad", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.labelLarge)
                 Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                     allActivities.forEach { a ->
                         val selected = a == selectedActivity
                         val selColor = pastelColorFor(a)
                         val txtColor = if (selColor.luminance() < 0.5f) Color.White else Color(0xFF222222)
                         FilterChip(
                             selected = selected,
                             onClick = { selectedActivity = if (selected) null else a },
                             label = { Text(a) },
                             colors = FilterChipDefaults.filterChipColors(
                                 containerColor = if (selected) Color.Unspecified else Color(0x22FFFFFF),
                                 labelColor = if (selected) txtColor else Color(0xFF333333),
                                 selectedContainerColor = selColor
                             ),
                             shape = RoundedCornerShape(20.dp)
                         )
                     }
                 }
                 // Orden
                 Text("Orden", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.labelLarge)
                 Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                     listOf("Más tiempo","Menos tiempo","Promedio").forEach { p ->
                         val selected = p == selectedSort
                         FilterChip(
                             selected = selected,
                             onClick = { selectedSort = p },
                             label = { Text(p) },
                             colors = FilterChipDefaults.filterChipColors(
                                 containerColor = if (selected) Color.Unspecified else Color(0x22FFFFFF),
                                 labelColor = if (selected) Color.White else Color(0xFF333333),
                                 selectedContainerColor = MaterialTheme.colorScheme.tertiary
                             ),
                             shape = RoundedCornerShape(20.dp)
                         )
                     }
                 }
             }
         }

         // Tabs de visualización
         TabRow(selectedTabIndex = selectedTab) {
             Tab(text = { Text("Cards") }, selected = selectedTab == 0, onClick = { selectedTab = 0 })
             Tab(text = { Text("Circular") }, selected = selectedTab == 1, onClick = { selectedTab = 1 })
             Tab(text = { Text("Barras") }, selected = selectedTab == 2, onClick = { selectedTab = 2 })
         }

             // Resumen y exportación
             val totalsByName = remember(filtered) { filtered.groupBy { it.name }.mapValues { it.value.sumOf { s -> s.durationMillis } } }
             val countsByName = remember(filtered) { filtered.groupBy { it.name }.mapValues { it.value.size } }
             val grandTotal = totalsByName.values.sum()
             val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
                 if (uri != null) exportSessionsToCsv(context, uri, filtered)
             }

             Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                 Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                     Text("Tiempo total", color = Color(0xFFCFD8DC), style = MaterialTheme.typography.titleMedium)
                     Text(formatMillis(grandTotal), color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                 }
                 Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                     Button(onClick = {
                         val ts = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
                         exportLauncher.launch("palipat_sesiones_" + ts + ".csv")
                     }) { Text("Exportar CSV") }
                 }
                 if (totalsByName.isNotEmpty() && selectedTab == 0) {
                     val ordered = when (selectedSort) {
                         "Menos tiempo" -> totalsByName.entries.sortedBy { it.value }
                         "Promedio" -> totalsByName.entries.sortedByDescending { (it.value.toDouble() / (countsByName[it.key] ?: 1).coerceAtLeast(1)) }
                         else -> totalsByName.entries.sortedByDescending { it.value }
                     }
                     Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                         ordered.forEach { (name, total) ->
                             Card(
                                 shape = RoundedCornerShape(20.dp),
                                 colors = CardDefaults.cardColors(containerColor = pastelColorFor(name).copy(alpha = 0.9f)),
                                 elevation = CardDefaults.cardElevation(6.dp),
                             ) {
                                 Row(
                                     modifier = Modifier.fillMaxWidth().padding(16.dp),
                                     horizontalArrangement = Arrangement.SpaceBetween,
                                     verticalAlignment = Alignment.CenterVertically
                                 ) {
                                     Column {
                                         Text(name, color = Color(0xFF263238), fontWeight = FontWeight.SemiBold)
                                         val subtitle = if (selectedSort == "Promedio")
                                             "Promedio: " + formatMillis(total / (countsByName[name] ?: 1).coerceAtLeast(1))
                                         else
                                             formatMillis(total)
                                         Text(subtitle, color = Color(0xFF37474F), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                     }
                                     androidx.compose.material3.Icon(
                                         imageVector = Icons.Filled.CheckCircle,
                                         contentDescription = null,
                                         tint = Color(0xFF26A69A)
                                     )
                                 }
                             }
                         }
                     }
                 }
             }

             // Donut chart (solo si tab Circular)
             if (selectedTab == 1) {
             Card(
                 modifier = Modifier.fillMaxWidth(),
                 shape = RoundedCornerShape(20.dp),
                 elevation = CardDefaults.cardElevation(10.dp),
                 colors = CardDefaults.cardColors(containerColor = Color(0xCCFFFFFF))
             ) {
                 Column(Modifier.padding(16.dp)) {
                     Text("Distribución de actividades", fontWeight = FontWeight.Bold, color = Color(0xFF333333))
                     Spacer(Modifier.height(12.dp))
                     Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                         val totals = filtered.groupBy { it.name }.mapValues { v -> v.value.sumOf { it.durationMillis } }
                         val sum = totals.values.sum().coerceAtLeast(1L)
                         val sections = totals.entries.map { it.value.toFloat() / sum.toFloat() to pastelColorFor(it.key) }
                         Canvas(modifier = Modifier.height(180.dp).fillMaxWidth()) {
                             val sizeMin = this.size.minDimension
                             val stroke = sizeMin * 0.12f
                             val diameter = sizeMin - stroke
                             val topLeft = Offset((this.size.width - diameter) / 2f, (this.size.height - diameter) / 2f)
                             var start = -90f
                             (if (sections.isNotEmpty()) sections else listOf(1f to Color(0xFF007AFF))).forEach { (ratio, color) ->
                                 val sweep = 360f * ratio
                                 drawArc(
                                     color = color,
                                     startAngle = start,
                                     sweepAngle = sweep,
                                     useCenter = false,
                                     style = Stroke(width = stroke, cap = StrokeCap.Round),
                                     size = Size(diameter, diameter),
                                     topLeft = topLeft
                                 )
                                 start += sweep
                             }
                             drawCircle(color = Color.White, radius = (diameter - stroke) / 2.2f, center = this.center)
                         }
                     }
                     Spacer(Modifier.height(8.dp))
                     val legendItems = filtered.groupBy { it.name }
                         .mapValues { it.value.sumOf { s -> s.durationMillis } }
                         .entries
                         .sortedByDescending { it.value }
                         .map { it.key to pastelColorFor(it.key) }
                     LegendRow(legendItems)
                 }
             }
             }

             // Barras semanales (solo si tab Barras)
             if (selectedTab == 2) {
             Card(
                 modifier = Modifier.fillMaxWidth(),
                 shape = RoundedCornerShape(20.dp),
                 elevation = CardDefaults.cardElevation(10.dp),
                 colors = CardDefaults.cardColors(containerColor = Color(0xCCFFFFFF))
             ) {
                 Column(Modifier.padding(16.dp)) {
                     Text("Progreso semanal", fontWeight = FontWeight.Bold, color = Color(0xFF333333))
                     Spacer(Modifier.height(12.dp))
                     val perDayMillis = remember(filtered) {
                         val arr = LongArray(7)
                         val cal = java.util.Calendar.getInstance()
                         filtered.forEach { s ->
                             val t = (s.endTime ?: s.startTime)
                             cal.timeInMillis = t
                             val day = cal.get(java.util.Calendar.DAY_OF_WEEK)
                             val idx = ((day + 5) % 7)
                             arr[idx] += s.durationMillis
                         }
                         arr
                     }
                     val maxV = perDayMillis.maxOrNull()?.coerceAtLeast(1L) ?: 1L
                     val bars = perDayMillis.map { it.toFloat() / maxV.toFloat() }
                     val barBrush = Brush.verticalGradient(listOf(Color(0xFF4FACFE), Color(0xFF007AFF)))
                     Row(
                         modifier = Modifier.fillMaxWidth().height(180.dp),
                         horizontalArrangement = Arrangement.spacedBy(10.dp),
                         verticalAlignment = Alignment.Bottom
                     ) {
                         bars.forEach { value ->
                             Box(
                                 modifier = Modifier
                                     .weight(1f)
                                     .fillMaxHeight(value)
                                     .background(barBrush, RoundedCornerShape(12.dp))
                             )
                         }
                     }
                     Spacer(Modifier.height(8.dp))
                     Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                         listOf("L","M","X","J","V","S","D").forEach { d -> Text(d, color = Color(0xFF888888)) }
                     }
                     Spacer(Modifier.height(8.dp))
                     // Leyenda (mismos colores por actividad que el donut)
                     val legendItemsBars = filtered.groupBy { it.name }
                         .map { it.key to pastelColorFor(it.key) }
                     LegendRow(legendItemsBars)
                 }
             }
             }
         
         // Diálogos de fecha para personalizado
         if (showStartPicker) {
             DatePickerDialog(onDismissRequest = { showStartPicker = false }, confirmButton = {
                 TextButton(onClick = {
                     showStartPicker = false
                     showEndPicker = true
                 }) { Text("Siguiente") }
             }) { DatePicker(state = startState) }
         }
         if (showEndPicker) {
             DatePickerDialog(onDismissRequest = { showEndPicker = false }, confirmButton = {
                 TextButton(onClick = {
                     showEndPicker = false
                     val start = startState.selectedDateMillis
                     val end = endState.selectedDateMillis
                     if (start != null && end != null) {
                         // Normalizar a inicio y fin de día en milis
                         fun startOfDay(ms: Long): Long {
                             val cal = java.util.Calendar.getInstance()
                             cal.timeInMillis = ms
                             cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                             cal.set(java.util.Calendar.MINUTE, 0)
                             cal.set(java.util.Calendar.SECOND, 0)
                             cal.set(java.util.Calendar.MILLISECOND, 0)
                             return cal.timeInMillis
                         }
                         fun endOfDay(ms: Long): Long {
                             val cal = java.util.Calendar.getInstance()
                             cal.timeInMillis = ms
                             cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
                             cal.set(java.util.Calendar.MINUTE, 59)
                             cal.set(java.util.Calendar.SECOND, 59)
                             cal.set(java.util.Calendar.MILLISECOND, 999)
                             return cal.timeInMillis
                         }
                         customStartMillis = startOfDay(minOf(start, end))
                         customEndMillis = endOfDay(maxOf(start, end))
                         selectedRange = "Personalizado"
                     }
                 }) { Text("Aplicar") }
             }) { DatePicker(state = endState) }
         }
         }
     }
 }

 // Color pastel estable por nombre de actividad
 private fun pastelColorFor(label: String): Color {
     val h = (label.hashCode() and 0x7fffffff) % 360
     val hsv = floatArrayOf(h.toFloat(), 0.25f, 0.98f)
     val argb = android.graphics.Color.HSVToColor(0xCC, hsv) // alpha suave
     return Color(argb)
 }

 @Composable
 private fun LegendRow(items: List<Pair<String, Color>>) {
     if (items.isEmpty()) return
     Row(
         modifier = Modifier
             .fillMaxWidth()
             .horizontalScroll(rememberScrollState()),
         horizontalArrangement = Arrangement.spacedBy(12.dp)
     ) {
         items.forEach { (label, color) ->
             Row(
                 verticalAlignment = Alignment.CenterVertically,
                 modifier = Modifier
                     .background(Color(0x22FFFFFF), RoundedCornerShape(16.dp))
                     .padding(horizontal = 10.dp, vertical = 6.dp)
             ) {
                 Box(
                     modifier = Modifier
                         .size(12.dp)
                         .background(color, RoundedCornerShape(6.dp))
                 )
                 Spacer(Modifier.width(6.dp))
                 Text(label, color = Color.White)
             }
         }
     }
 }

 @Composable
 private fun FilterControls(
     allActivities: List<String>,
     selectedActivity: String?,
     onActivityChange: (String?) -> Unit,
     selectedMode: String,
     onModeChange: (String) -> Unit,
     selectedRange: String,
     onRangeChange: (String) -> Unit,
     selectedSort: String,
     onSortChange: (String) -> Unit,
 ) {
     Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
         Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
             listOf("Hoy","Semana","Mes","Personalizado").forEach { r ->
                 val selected = r == selectedRange
                 FilterChip(
                     selected = selected,
                     onClick = { onRangeChange(r) },
                     label = { Text(r) },
                     colors = FilterChipDefaults.filterChipColors(
                         containerColor = if (selected) Color.Unspecified else Color(0xFFF5F5F5),
                         labelColor = if (selected) Color.White else Color(0xFF333333),
                         selectedContainerColor = MaterialTheme.colorScheme.primary
                     ),
                     shape = RoundedCornerShape(20.dp)
                 )
             }
         }
         Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
             val categories = if (allActivities.isEmpty()) emptyList() else allActivities
             categories.forEach { a ->
                 val selected = a == selectedActivity
                 FilterChip(
                     selected = selected,
                     onClick = { onActivityChange(if (selected) null else a) },
                     label = { Text(a) },
                     colors = FilterChipDefaults.filterChipColors(
                         containerColor = if (selected) Color.Unspecified else Color(0xFFF5F5F5),
                         labelColor = if (selected) Color.White else Color(0xFF333333),
                         selectedContainerColor = MaterialTheme.colorScheme.primary
                     ),
                     shape = RoundedCornerShape(20.dp)
                 )
             }
         }
         Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
             val sorts = listOf("Más tiempo","Menos tiempo","Promedio")
             sorts.forEach { p ->
                 val selected = p == selectedSort
                 FilterChip(
                     selected = selected,
                     onClick = { onSortChange(p) },
                     label = { Text(p) },
                     colors = FilterChipDefaults.filterChipColors(
                         containerColor = if (selected) Color.Unspecified else Color(0xFFF5F5F5),
                         labelColor = if (selected) Color.White else Color(0xFF333333),
                         selectedContainerColor = MaterialTheme.colorScheme.primary
                     ),
                     shape = RoundedCornerShape(20.dp)
                 )
             }
         }
     }
 }

 @Composable
  private fun TaskRow(
      item: TaskTimer,
      onStart: () -> Unit,
      onPause: () -> Unit,
      onReset: () -> Unit,
      onDelete: () -> Unit,
      storage: TimerStorage,
  ) {
      // Estadísticas por actividad (promedio, máximo, mínimo) incluyendo sesión en curso
       val sessions by storage.observeSessions().collectAsState(initial = emptyList())
       val timers by storage.observe().collectAsState(initial = emptyList())
       val nowStats = System.currentTimeMillis()
       val durationsForActivity = remember(sessions, timers, item.name, item.id, item.isRunning, item.elapsedMillis, item.remainingMillis, item.mode, item.lastStartedAt) {
           val live = timers.filter { it.isRunning && it.name == item.name }.map { tr ->
               if (tr.mode == "Countdown") (tr.durationMillis - tr.remainingMillis).coerceAtLeast(0L)
               else (tr.elapsedMillis + (tr.lastStartedAt?.let { nowStats - it } ?: 0L)).coerceAtLeast(0L)
           }
           val historical = sessions.filter { it.name == item.name }.map { it.durationMillis }
           (historical + live).filter { it > 0L }
       }
       val avg = if (durationsForActivity.isNotEmpty()) durationsForActivity.average().toLong() else 0L
       val max = durationsForActivity.maxOrNull() ?: 0L
       val min = if (durationsForActivity.isNotEmpty()) durationsForActivity.minOrNull() ?: 0L else 0L
      val running = item.isRunning
      val titleColor = Color.White
      val subtitleColor = Color(0xFFB0BEC5)
      val timeTargetColor = Color.White
      val timeColor by animateColorAsState(targetValue = timeTargetColor, animationSpec = tween(200), label = "timeColor")

      val gradient = gradientForActivity(item.name)
      val startBtnColorTarget = if (running) MaterialTheme.colorScheme.tertiary else Color(0xFF26A69A)
      val pauseBtnColorTarget = if (running) Color(0xFFFF9800) else MaterialTheme.colorScheme.tertiary
      val startBtnColor by animateColorAsState(startBtnColorTarget, tween(200), label = "startBtn")
      val pauseBtnColor by animateColorAsState(pauseBtnColorTarget, tween(200), label = "pauseBtn")
      val finished = item.mode == Mode.Countdown && item.remainingMillis <= 0
      var refreshAngle by remember { mutableFloatStateOf(0f) }
      val animatedAngle by animateFloatAsState(targetValue = refreshAngle, animationSpec = tween(400), label = "refreshRot")

      Card(
          modifier = Modifier
              .padding(12.dp)
              .animateContentSize(),
          shape = RoundedCornerShape(20.dp),
          elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
          colors = CardDefaults.cardColors(containerColor = Color.Transparent)
      ) {
         // Fondo con gradiente + capa de contenido
         Box(
             modifier = Modifier
                 .background(gradient, shape = RoundedCornerShape(20.dp))
         ) {
             // Barra de progreso superior fina cuando está corriendo
             if (running) {
                 if (item.mode == Mode.Countdown && item.durationMillis > 0) {
                     val progress = (item.durationMillis - item.remainingMillis).toFloat() / item.durationMillis.toFloat()
                     LinearProgressIndicator(
                         progress = { progress.coerceIn(0f, 1f) },
                         modifier = Modifier
                             .fillMaxWidth()
                             .height(3.dp)
                             .align(Alignment.TopStart),
                         color = Color.White
                     )
                 } else {
                     LinearProgressIndicator(
                         modifier = Modifier
                             .fillMaxWidth()
                             .height(3.dp)
                             .align(Alignment.TopStart),
                         color = Color.White
                     )
                 }
             }

             // Contenido principal con padding
             Column(modifier = Modifier.padding(16.dp)) {
                 // Título y botón eliminar en esquina superior derecha
                 Box(modifier = Modifier.fillMaxWidth()) {
                     Text(
                         text = item.name,
                         fontSize = 18.sp,
                         fontWeight = FontWeight.SemiBold,
                         color = titleColor,
                         modifier = Modifier.align(Alignment.CenterStart)
                     )

                     val delInteraction = remember { MutableInteractionSource() }
                     val pressed by delInteraction.collectIsPressedAsState()
                     val delTint by animateColorAsState(
                         targetValue = if (pressed) Color(0xFFFF4D4D) else Color(0xFFCFD8DC),
                         animationSpec = tween(150),
                         label = "delTint"
                     )
                     IconButton(
                         onClick = onDelete,
                         modifier = Modifier.align(Alignment.CenterEnd),
                         interactionSource = delInteraction
                     ) {
                         androidx.compose.material3.Icon(
                             imageVector = Icons.Outlined.Delete,
                             contentDescription = "Eliminar",
                             tint = delTint
                         )
                     }
                 }

                 Spacer(Modifier.height(8.dp))
                 // Tiempo grande, centrado, monospace
                 val timeText = if (item.mode == Mode.Countdown) formatMillis(item.remainingMillis) else formatMillis(item.elapsedMillis)
                  AnimatedContent(targetState = timeText, label = "timeText") { value ->
                      Text(
                          text = value,
                          fontSize = 48.sp,
                          fontWeight = FontWeight.Bold,
                          color = timeColor,
                          fontFamily = FontFamily.Monospace,
                          modifier = Modifier.fillMaxWidth(),
                          textAlign = androidx.compose.ui.text.style.TextAlign.Center
                      )
                  }

                  Spacer(Modifier.height(4.dp))
                  // Promedio / Max / Min
                  Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceEvenly,
                      verticalAlignment = Alignment.CenterVertically
                  ) {
                      Column(horizontalAlignment = Alignment.CenterHorizontally) {
                          Text("Prom", color = subtitleColor, fontSize = 12.sp)
                          Text(formatMillis(avg), color = Color.White, fontWeight = FontWeight.SemiBold)
                      }
                      Column(horizontalAlignment = Alignment.CenterHorizontally) {
                          Text("Max", color = subtitleColor, fontSize = 12.sp)
                          Text(formatMillis(max), color = Color.White, fontWeight = FontWeight.SemiBold)
                      }
                      Column(horizontalAlignment = Alignment.CenterHorizontally) {
                          Text("Min", color = subtitleColor, fontSize = 12.sp)
                          Text(formatMillis(min), color = Color.White, fontWeight = FontWeight.SemiBold)
                      }
                  }
                 Text(text = if (item.mode == Mode.Countdown) "Cuenta regresiva" else "Cronómetro", fontSize = 12.sp, color = subtitleColor)

                 Spacer(Modifier.height(12.dp))
                 // Acciones
                 Row(
                     horizontalArrangement = Arrangement.spacedBy(8.dp),
                     verticalAlignment = Alignment.CenterVertically
                 ) {
                     if (!running && !finished) {
                         Button(
                             onClick = onStart,
                             shape = RoundedCornerShape(50.dp),
                             modifier = Modifier.height(48.dp),
                             colors = ButtonDefaults.buttonColors(containerColor = startBtnColor, contentColor = Color.White)
                         ) {
                             androidx.compose.material3.Icon(
                                 imageVector = Icons.Filled.PlayArrow,
                                 contentDescription = "Iniciar"
                             )
                         }
                     } else if (running) {
                         Button(
                             onClick = onPause,
                             shape = RoundedCornerShape(50.dp),
                             modifier = Modifier.height(48.dp),
                             colors = ButtonDefaults.buttonColors(containerColor = pauseBtnColor, contentColor = Color.White)
                         ) {
                             androidx.compose.material3.Icon(
                                 imageVector = Icons.Filled.Pause,
                                 contentDescription = "Pausar"
                             )
                         }
                     } else {
                         if (finished) Text("Terminado", style = MaterialTheme.typography.labelLarge, color = Color.White)
                     }

                     // Reinicio como icono con rotación
                     IconButton(onClick = {
                         refreshAngle += 360f
                         onReset()
                     }) {
                         androidx.compose.material3.Icon(
                             imageVector = Icons.Filled.Refresh,
                             contentDescription = "Reiniciar",
                             tint = Color(0xFFE53935),
                             modifier = Modifier.rotate(animatedAngle)
                         )
                     }

                     Spacer(Modifier.weight(1f))
                 }
             }
         }
      }
  }

 @Composable
 private fun gradientForActivity(name: String): Brush {
     val n = name.lowercase(Locale.getDefault())
     val dark = isSystemInDarkTheme()
     return when {
         n.contains("juego") -> if (dark)
             Brush.verticalGradient(listOf(Color(0xFF0A3578), Color(0xFF1E88E5)))
         else Brush.verticalGradient(listOf(Color(0xFF0D47A1), Color(0xFF42A5F5)))
         n.contains("comida") -> if (dark)
             Brush.verticalGradient(listOf(Color(0xFFA0154C), Color(0xFFE35186)))
         else Brush.verticalGradient(listOf(Color(0xFFD81B60), Color(0xFFF06292)))
         n.contains("estudio") -> if (dark)
             Brush.verticalGradient(listOf(Color(0xFF1F5A23), Color(0xFF66BB6A)))
         else Brush.verticalGradient(listOf(Color(0xFF2E7D32), Color(0xFF81C784)))
         n.contains("ejercicio") -> if (dark)
             Brush.verticalGradient(listOf(Color(0xFFC15800), Color(0xFFFFB300)))
         else Brush.verticalGradient(listOf(Color(0xFFEF6C00), Color(0xFFFFCA28)))
         n.contains("sueño") || n.contains("sueno") -> if (dark)
             Brush.verticalGradient(listOf(Color(0xFF3A0F6A), Color(0xFF9C4FCC)))
         else Brush.verticalGradient(listOf(Color(0xFF4A148C), Color(0xFFBA68C8)))
         else -> if (dark)
             Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))
         else Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))
     }
 }

 @Composable
 private fun EmptyState() {
     Column(
         modifier = Modifier
             .fillMaxSize()
             .padding(32.dp),
         horizontalAlignment = Alignment.CenterHorizontally,
         verticalArrangement = Arrangement.Center
     ) {
         Text("Aún no hay actividades")
         Spacer(Modifier.height(8.dp))
         Text("Pulsa + para agregar una actividad con duración en minutos")
     }
 }

 @Composable
  private fun AddTaskDialog(
      onDismiss: () -> Unit,
      onConfirm: (name: String, minutes: Int, mode: Mode) -> Unit,
  ) {
     var name by remember { mutableStateOf(TextFieldValue("")) }
     var minutesText by remember { mutableStateOf(TextFieldValue("25")) }
     var stopwatch by remember { mutableStateOf(true) }

     AlertDialog(
         onDismissRequest = onDismiss,
         title = { Text("Nueva actividad") },
         text = {
             Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                 OutlinedTextField(
                     value = name,
                     onValueChange = { name = it },
                     label = { Text("Nombre") },
                     singleLine = true,
                     modifier = Modifier.fillMaxWidth()
                 )
                 Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                     Switch(checked = stopwatch, onCheckedChange = { stopwatch = it })
                     Text("Cronómetro (conteo hacia arriba)")
                 }
                 OutlinedTextField(
                     enabled = !stopwatch,
                     value = minutesText,
                     onValueChange = { minutesText = it },
                     label = { Text("Minutos (cuenta regresiva)") },
                     singleLine = true,
                     modifier = Modifier.fillMaxWidth()
                 )
             }
         },
         confirmButton = {
             TextButton(onClick = {
                 val mins = if (stopwatch) 0 else (minutesText.text.toIntOrNull() ?: 0)
                 val mode = if (stopwatch) Mode.Stopwatch else Mode.Countdown
                 onConfirm(name.text, mins, mode)
             }) { Text("Agregar") }
         },
         dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
     )
  }

 private fun formatMillis(ms: Long): String {
     val totalSec = (ms / 1000).toInt().coerceAtLeast(0)
     val h = totalSec / 3600
     val m = (totalSec % 3600) / 60
     val s = totalSec % 60
     return if (h > 0) "%02d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
 }

 @Composable
  private fun SettingsDialog(
      currentUri: String?,
      onPickNew: (String?) -> Unit,
      onDismiss: () -> Unit
  ) {
     val context = LocalContext.current
     val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
         val data = result.data
         val picked: Uri? = if (Build.VERSION.SDK_INT >= 33) {
             data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
         } else {
             @Suppress("DEPRECATION")
             data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
         }
         onPickNew(picked?.toString())
     }
     val title = runCatching {
         currentUri?.let { uriStr ->
             val uri = Uri.parse(uriStr)
             val r = RingtoneManager.getRingtone(context, uri)
             r?.getTitle(context)
         }
     }.getOrNull() ?: "Sonido por defecto"

     AlertDialog(
         onDismissRequest = onDismiss,
         title = { Text("Ajustes") },
         text = {
             Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                 Text("Sonido de fin de cuenta regresiva")
                 Text("Actual: $title", style = MaterialTheme.typography.labelMedium)
                 Button(onClick = {
                     val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                         putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                         putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                         putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                         putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Seleccionar sonido de notificación")
                         val existing = currentUri?.let { Uri.parse(it) }
                         putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existing)
                     }
                     launcher.launch(intent)
                 }) { Text("Elegir sonido") }
             }
         },
         confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
     )
 }

 private fun vibrateOnce(context: android.content.Context, millis: Long) {
     try {
         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
             val vm = context.getSystemService(VibratorManager::class.java)
             vm?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE))
         } else {
             val v = context.getSystemService(Vibrator::class.java)
             if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                 v?.vibrate(VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE))
             } else {
                 @Suppress("DEPRECATION")
                 v?.vibrate(millis)
             }
         }
     } catch (_: Throwable) { }
 }

 private fun exportSessionsToCsv(
     context: android.content.Context,
     uri: Uri,
     sessions: List<com.example.palipat.data.TimerStorage.SessionLog>
 ) {
     runCatching {
         context.contentResolver.openOutputStream(uri)?.use { os ->
             OutputStreamWriter(os, StandardCharsets.UTF_8).use { writer ->
                 // Encabezados
                 writer.appendLine("timerId,name,mode,startTime,endTime,durationMillis,durationFormatted,startDate,endDate")
                 val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                 sessions.forEach { s ->
                     val start = Date(s.startTime)
                     val end = Date(s.endTime)
                     val durFmt = formatMillis(s.durationMillis)
                     // Escapar comas en nombre si existieran
                     val safeName = if (s.name.contains(',')) '"' + s.name.replace("\"", "\"\"") + '"' else s.name
                     writer.append(s.timerId.toString()).append(',')
                         .append(safeName).append(',')
                         .append(s.mode).append(',')
                         .append(s.startTime.toString()).append(',')
                         .append(s.endTime.toString()).append(',')
                         .append(s.durationMillis.toString()).append(',')
                         .append(durFmt).append(',')
                         .append(sdf.format(start)).append(',')
                         .append(sdf.format(end))
                         .append('\n')
                 }
             }
         }
     }.onFailure { e ->
         // Opcional: mostrar un log/toast en caso de error
     }
 }

  @Preview(showBackground = true)
  @Composable
  private fun PreviewTimerApp() {
      PalipatTheme { TimerApp() }
  }