package com.varc.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.varc.app.data.ProfileRepository
import com.varc.app.data.SessionRepository
import com.varc.app.data.models.ProgramComponents
import com.varc.app.data.models.SkaterProfile
import kotlinx.coroutines.launch
import kotlin.math.cos

import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Rendimiento", "Evolución", "Perfil")
    val profileRepo = remember { ProfileRepository(context) }
    val sessionRepo = remember { SessionRepository(context) }
    val profile by profileRepo.getProfile().collectAsState(initial = SkaterProfile())
    val sessions by sessionRepo.getSessions().collectAsState(initial = emptyList())
    val categories = listOf(
        "Benjamín Femenino", "Benjamín Masculino",
        "Alevín Femenino", "Alevín Masculino",
        "Infantil Femenino", "Infantil Masculino",
        "Cadete Femenino", "Cadete Masculino",
        "Juvenil Femenino", "Juvenil Masculino",
        "Senior Femenino", "Senior Masculino",
        "Máster Femenino", "Máster Masculino"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Perfil") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            SkaterHeader(profile)

            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal)
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> PerformancePanel(sessions)
                1 -> EvolutionPanel(sessions)
                2 -> SkaterInfoPanel(profile, profileRepo, scope)
            }
        }
    }
}

@Composable
private fun SkaterHeader(profile: SkaterProfile) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    profile.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${profile.category} · ${profile.level}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                if (profile.club.isNotBlank()) {
                    Text(
                        profile.club,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PerformancePanel(sessions: List<com.varc.app.data.models.ScoringResult>) {
    val latestSession = sessions.firstOrNull()
    val avgComponents = if (sessions.isNotEmpty()) {
        val count = sessions.size.coerceAtMost(10)
        val recent = sessions.take(count)
        ProgramComponents(
            skills = averageAndClamp(recent.map { it.tes }, 10.0),
            transitions = averageAndClamp(recent.map { it.tes }, 12.0),
            performance = averageAndClamp(recent.map { it.totalScore - it.tes }, 5.0),
            choreography = averageAndClamp(recent.map { it.totalScore }, 15.0),
            interpretation = averageAndClamp(recent.map { it.totalScore - it.deductions }, 12.0)
        )
    } else ProgramComponents()

    val validElements = latestSession?.elements?.count { it.isValid } ?: 0
    val invalidElements = (latestSession?.elements?.size ?: 0) - validElements

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Componentes del Programa",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold)
        }

        item {
            RadarChart(
                components = avgComponents,
                modifier = Modifier.fillMaxWidth().height(300.dp)
            )
        }

        item {
            Text("Última sesión",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Puntuación Total", fontWeight = FontWeight.SemiBold)
                        Text(
                            if (latestSession != null) String.format("%.1f", latestSession.totalScore)
                            else "—",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Elementos", style = MaterialTheme.typography.bodySmall)
                        Text(
                            if (latestSession != null) "$validElements válidos · $invalidElements no válidos"
                            else "Sin datos",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Deducciones", style = MaterialTheme.typography.bodySmall)
                        Text(
                            if (latestSession != null) String.format("%.1f", latestSession.deductions)
                            else "—",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun RadarChart(
    components: ProgramComponents,
    modifier: Modifier = Modifier
) {
    val labels = listOf("Skills", "Transitions", "Performance", "Choreography", "Interpretation")
    val values = listOf(
        components.skills, components.transitions,
        components.performance, components.choreography,
        components.interpretation
    )
    val maxVal = 10f

    Canvas(modifier = modifier) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val radius = minOf(centerX, centerY) * 0.7f
        val angles = (0 until labels.size).map {
            Math.toRadians((it * 72.0 - 90.0)).toFloat()
        }

        for (level in 1..5) {
            val r = radius * level / 5
            val path = Path()
            angles.forEachIndexed { i, angle ->
                val x = centerX + r * cos(angle)
                val y = centerY + r * sin(angle)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(path, color = Color.LightGray.copy(alpha = 0.3f), style = Stroke(1f))
        }

        for (i in labels.indices) {
            val x1 = centerX + radius * cos(angles[i])
            val y1 = centerY + radius * sin(angles[i])
            drawLine(Color.LightGray.copy(alpha = 0.3f), Offset(centerX, centerY), Offset(x1, y1), strokeWidth = 1f)
        }

        val dataPath = Path()
        angles.forEachIndexed { i, angle ->
            val r = radius * (values[i] / maxVal)
            val x = centerX + r * cos(angle)
            val y = centerY + r * sin(angle)
            if (i == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
        }
        dataPath.close()
        drawPath(dataPath, color = Color(0xFF6C63FF).copy(alpha = 0.4f))
        drawPath(dataPath, color = Color(0xFF6C63FF), style = Stroke(2f))

        angles.forEachIndexed { i, angle ->
            val r = radius * (values[i] / maxVal)
            val x = centerX + r * cos(angle)
            val y = centerY + r * sin(angle)
            drawCircle(Color(0xFF6C63FF), 5f, Offset(x, y))
        }
    }
}

@Composable
private fun EvolutionPanel(sessions: List<com.varc.app.data.models.ScoringResult>) {
    var selectedSessions by remember { mutableIntStateOf(0) }
    val displayLimit = when (selectedSessions) {
        0 -> 3
        1 -> 10
        else -> Int.MAX_VALUE
    }
    val displaySessions = sessions.take(displayLimit)

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Evolución del TES",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("Últimas 3", "Últimas 10", "Todas").forEachIndexed { i, label ->
                        FilterChip(
                            selected = selectedSessions == i,
                            onClick = { selectedSessions = i },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }
            }
        }

        item {
            if (displaySessions.isEmpty()) {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Box(modifier = Modifier.padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("Aún no hay sesiones analizadas",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                displaySessions.forEachIndexed { index, session ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Sesión ${sessions.size - index}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold)
                                Text(java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.getDefault())
                                    .format(java.util.Date(session.timestamp)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(String.format("%.1f", session.totalScore),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary)
                                Text("TES: ${String.format("%.1f", session.tes)}",
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun SkaterInfoPanel(
    profile: SkaterProfile,
    profileRepo: ProfileRepository,
    scope: kotlinx.coroutines.CoroutineScope
) {
    var showEditDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ProfileInfoRow("Categoría", profile.category)
                    ProfileInfoRow("Club", profile.club.ifEmpty { "—" })
                    ProfileInfoRow("Nivel", profile.level)
                    ProfileInfoRow("Reglamento", "World Skate 2025-2026")
                }
            }
        }

        item {
            Button(
                onClick = { showEditDialog = true },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Edit, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Editar perfil")
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }

    if (showEditDialog) {
        EditProfileDialog(
            current = profile,
            onSave = { updated ->
                scope.launch { profileRepo.updateProfile(updated) }
                showEditDialog = false
            },
            onDismiss = { showEditDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileDialog(
    current: SkaterProfile,
    onSave: (SkaterProfile) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(current.name) }
    var category by remember { mutableStateOf(current.category) }
    var club by remember { mutableStateOf(current.club) }
    var level by remember { mutableStateOf(current.level) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var levelExpanded by remember { mutableStateOf(false) }
    val categories = listOf(
        "Benjamín Femenino", "Benjamín Masculino",
        "Alevín Femenino", "Alevín Masculino",
        "Infantil Femenino", "Infantil Masculino",
        "Cadete Femenino", "Cadete Masculino",
        "Juvenil Femenino", "Juvenil Masculino",
        "Senior Femenino", "Senior Masculino",
        "Máster Femenino", "Máster Masculino"
    )
    val levels = listOf("Iniciación", "Nacional", "Internacional")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar perfil") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Nombre") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoría") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = { category = cat; categoryExpanded = false }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = levelExpanded,
                    onExpandedChange = { levelExpanded = !levelExpanded }
                ) {
                    OutlinedTextField(
                        value = level,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Nivel") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = levelExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = levelExpanded,
                        onDismissRequest = { levelExpanded = false }
                    ) {
                        levels.forEach { lvl ->
                            DropdownMenuItem(
                                text = { Text(lvl) },
                                onClick = { level = lvl; levelExpanded = false }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = club, onValueChange = { club = it },
                    label = { Text("Club") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(SkaterProfile(name = name, category = category, club = club, level = level))
            }) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun ProfileInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold)
    }
}

private fun averageAndClamp(values: List<Double>, divisor: Double): Float {
    if (values.isEmpty()) return 0f
    val avg = values.map { (it / divisor).coerceIn(0.0, 10.0) }.average()
    return avg.toFloat()
}
