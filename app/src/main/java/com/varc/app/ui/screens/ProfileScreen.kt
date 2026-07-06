package com.varc.app.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.varc.app.data.models.ProgramComponents

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onBack: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Rendimiento", "Evolución", "Perfil")

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
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            SkaterHeader()

            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            when (selectedTab) {
                0 -> PerformancePanel()
                1 -> EvolutionPanel()
                2 -> SkaterInfoPanel()
            }
        }
    }
}

@Composable
private fun SkaterHeader() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
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
                    "Patinadora",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Senior Femenino · Nivel Nacional",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun PerformancePanel() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Componentes del Programa",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            RadarChart(
                components = ProgramComponents(8.5f, 7.2f, 8.0f, 7.5f, 7.8f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            )
        }

        item {
            Text(
                "Última sesión",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
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
                        Text("52.3", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Elementos", style = MaterialTheme.typography.bodySmall)
                        Text("7 válidos · 0 no válidos", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Deducciones", style = MaterialTheme.typography.bodySmall)
                        Text("0.0", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun RadarChart(
    components: ProgramComponents,
    modifier: Modifier = Modifier
) {
    val labels = listOf("Skills", "Transitions", "Performance", "Choreography", "Interpretation")
    val values = listOf(
        components.skills,
        components.transitions,
        components.performance,
        components.choreography,
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
                val x = centerX + r * kotlin.math.cos(angle)
                val y = centerY + r * kotlin.math.sin(angle)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(path, color = Color.LightGray.copy(alpha = 0.3f), style = Stroke(1f))
        }

        for (i in labels.indices) {
            val x1 = centerX + radius * kotlin.math.cos(angles[i])
            val y1 = centerY + radius * kotlin.math.sin(angles[i])
            drawLine(Color.LightGray.copy(alpha = 0.3f), Offset(centerX, centerY), Offset(x1, y1), strokeWidth = 1f)
        }

        val dataPath = Path()
        angles.forEachIndexed { i, angle ->
            val r = radius * (values[i] / maxVal)
            val x = centerX + r * kotlin.math.cos(angle)
            val y = centerY + r * kotlin.math.sin(angle)
            if (i == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
        }
        dataPath.close()
        drawPath(dataPath, color = Color(0xFF6C63FF).copy(alpha = 0.4f))
        drawPath(dataPath, color = Color(0xFF6C63FF), style = Stroke(2f))

        angles.forEachIndexed { i, angle ->
            val r = radius * (values[i] / maxVal)
            val x = centerX + r * kotlin.math.cos(angle)
            val y = centerY + r * kotlin.math.sin(angle)
            drawCircle(Color(0xFF6C63FF), 5f, Offset(x, y))
        }
    }
}

@Composable
private fun EvolutionPanel() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Evolución del TES",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            var selectedSessions by remember { mutableIntStateOf(0) }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp),
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Progresión de elementos", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(12.dp))
                    listOf(
                        "Axel Doble (2A)" to "80% de éxito (8/10)",
                        "Toe Loop Triple (3T)" to "60% de éxito (6/10)",
                        "Pirueta Combinada" to "Nivel 3 estable"
                    ).forEach { (element, status) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.TrendingUp,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF4CAF50)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(element, style = MaterialTheme.typography.bodyMedium)
                                Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SkaterInfoPanel() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ProfileInfoRow("Categoría", "Senior Femenino")
                    ProfileInfoRow("Club", "—")
                    ProfileInfoRow("Nivel", "Nacional")
                    ProfileInfoRow("Entrenador", "—")
                    ProfileInfoRow("Reglamento", "World Skate 2025-2026")
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Estadísticas", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(12.dp))
                    ProfileInfoRow("Sesiones analizadas", "12")
                    ProfileInfoRow("Total elementos", "84")
                    ProfileInfoRow("GOE promedio", "+1.2")
                    ProfileInfoRow("Mejor puntuación", "68.4")
                }
            }
        }

        item {
            Button(
                onClick = { /* Export */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Download, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Exportar informe PDF")
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ProfileInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
