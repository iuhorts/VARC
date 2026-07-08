package com.varc.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.varc.app.data.SessionRepository
import com.varc.app.data.models.DetectedElement
import com.varc.app.data.models.ProgramComponents
import com.varc.app.data.models.ScoringResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { SessionRepository(context) }
    val sessions by repository.getSessions().collectAsState(initial = emptyList())
    val result = sessions.firstOrNull()

    var expandedElement by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Resultados") },
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
        if (result != null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    ScoreSummaryCard(result)
                }

                item {
                    PcsCard(result.programComponents, result.pcs)
                }

                item {
                    Text(
                        "Elementos (${result.elements.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(result.elements) { element ->
                    ElementCard(
                        element = element,
                        isExpanded = expandedElement == element.name,
                        onToggle = {
                            expandedElement = if (expandedElement == element.name) null else element.name
                        }
                    )
                }

                item {
                    Text(
                        "Resultados generados por VARC AI basados en análisis de vídeo mediante ML Kit.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Cargando resultados…",
                        style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun ScoreSummaryCard(result: ScoringResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Puntuación Total",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                String.format("%.1f", result.totalScore),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (result.fallCount > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("${result.fallCount} caída${if (result.fallCount > 1) "s" else ""}",
                        color = Color(0xFFCF6679),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ScoreItem("TES", String.format("%.1f", result.tes))
                ScoreItem("PCS", String.format("%.1f", result.pcs))
                ScoreItem("Deducciones", String.format("%.1f", result.deductions))
                ScoreItem("Duración", String.format("%.1fs", result.programDuration))
            }
        }
    }
}

@Composable
private fun PcsCard(components: ProgramComponents, pcsTotal: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Componentes del Programa",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold)
                Text(String.format("%.1f", pcsTotal),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(12.dp))
            PcsRow("Skating Skills", components.skatingSkills)
            PcsRow("Transitions", components.transitions)
            PcsRow("Performance", components.performance)
            PcsRow("Choreography", components.choreography)
        }
    }
}

@Composable
private fun PcsRow(label: String, value: Float) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(String.format("%.1f", value),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ScoreItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(label, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
    }
}

@Composable
private fun ElementCard(
    element: DetectedElement,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    val typeColor = when (element.type) {
        "JUMP" -> Color(0xFFFF6584)
        "SPIN" -> Color(0xFF00D9FF)
        "STEP" -> Color(0xFF6C63FF)
        else -> Color.Gray
    }

    val goeColor = when {
        element.goe > 0 -> Color(0xFF4CAF50)
        element.goe < 0 -> Color(0xFFCF6679)
        else -> Color.Gray
    }

    val startSec = element.timestampStart.toInt()
    val endSec = element.timestampEnd.toInt()
    val timeStr = "${startSec / 60}:${"%02d".format(startSec % 60)}-${endSec / 60}:${"%02d".format(endSec % 60)}"

    Card(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(typeColor, RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(element.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold)
                    Row {
                        Text("Nivel ${element.level} · ${element.type}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (element.confidence > 0) {
                            Text(" · ${(element.confidence * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(String.format("%.2f", element.finalValue),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(timeStr,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp))
                    }
                }
            }

            if (isExpanded) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Valor base:", style = MaterialTheme.typography.bodySmall)
                        Text(String.format("%.2f", element.baseValue),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("GOE:", style = MaterialTheme.typography.bodySmall)
                        Text("${if (element.goe >= 0) "+" else ""}${element.goe}",
                            style = MaterialTheme.typography.bodySmall,
                            color = goeColor,
                            fontWeight = FontWeight.SemiBold)
                    }
                    if (element.type == "JUMP" && element.rotationQuality.isNotEmpty()) {
                        val qualLabel = when (element.rotationQuality) {
                            "<" -> "Under-rotado (<)"
                            "<<" -> "Half-rotado (<<)"
                            "<<<" -> "Downgraded (<<<)"
                            else -> element.rotationQuality
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Rotación:", style = MaterialTheme.typography.bodySmall)
                            Text(qualLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFCF6679),
                                fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Tiempo:", style = MaterialTheme.typography.bodySmall)
                        Text("${"%.1f".format(element.timestampStart)}s - ${"%.1f".format(element.timestampEnd)}s",
                            style = MaterialTheme.typography.bodySmall)
                    }

                    if (element.goeFactors.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Factores GOE",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        element.goeFactors.forEach { factor ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = goeColor)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(factor, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
