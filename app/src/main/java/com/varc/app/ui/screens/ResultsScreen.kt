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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.varc.app.data.models.DetectedElement
import com.varc.app.data.models.ScoringResult
import com.varc.app.ml.ElementClassifier
import com.varc.app.scoring.ScoringEngine
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    videoPath: String,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var result by remember { mutableStateOf<ScoringResult?>(null) }
    var expandedElement by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(videoPath) {
        val classification = ElementClassifier.ClassificationResult(
            elements = listOf(
                DetectedElement("JUMP", "Axel Doble (2A)", "2", 3.30, 1, listOf("Buena altura", "Rotación completa"), 3.63, 5.2f, 6.8f, true, 0.87f),
                DetectedElement("SPIN", "Pirueta Combinada (CoSp)", "3", 2.50, 2, listOf("Buena velocidad", "Posición centrada"), 3.00, 12.4f, 15.1f, true, 0.92f),
                DetectedElement("JUMP", "Toe Loop Triple (3T)", "3", 4.20, 0, listOf(), 4.20, 20.1f, 21.5f, true, 0.78f),
                DetectedElement("STEP", "Secuencia Circular (CiSt)", "2", 1.80, 1, listOf("Buena cobertura"), 1.98, 30.5f, 37.2f, true, 0.85f),
                DetectedElement("SPIN", "Pirueta Sentada (SSp)", "3", 1.30, 1, listOf("Posición lograda"), 1.43, 42.0f, 45.5f, true, 0.90f),
                DetectedElement("JUMP", "Lutz Doble (2Lz)", "2", 2.10, 2, listOf("Buena altura", "Aterrizaje limpio"), 2.52, 50.3f, 51.8f, true, 0.83f),
                DetectedElement("STEP", "Secuencia Coreográfica (ChSq)", "1", 2.00, 1, listOf("Originalidad"), 2.20, 60.0f, 70.5f, true, 0.76f)
            ),
            fallDetected = false,
            programDuration = 72.5f
        )
        result = ScoringEngine.calculateScore(classification)
    }

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
        result?.let { res ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    ScoreSummaryCard(res)
                }

                item {
                    Text(
                        "Elementos detectados (${res.elements.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(res.elements) { element ->
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
                        "* Puntuación simulada con datos de ejemplo. Los resultados reales requieren procesamiento de vídeo mediante modelos de visión por computadora.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        } ?: Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Puntuación Total",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                String.format("%.1f", result.totalScore),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ScoreItem("TES", String.format("%.1f", result.tes))
                ScoreItem("Deducciones", String.format("%.1f", result.deductions))
                ScoreItem("Duración", String.format("%.1fs", result.programDuration))
            }
        }
    }
}

@Composable
private fun ScoreItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
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

    Card(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(typeColor, RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        element.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Nivel ${element.level} · ${element.type}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        String.format("%.2f", element.finalValue),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "GOE ${if (element.goe >= 0) "+" else ""}${element.goe}",
                            style = MaterialTheme.typography.bodySmall,
                            color = goeColor,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (isExpanded && element.goeFactors.isNotEmpty()) {
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        "Factores GOE",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    element.goeFactors.forEach { factor ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = goeColor
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                factor,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}
