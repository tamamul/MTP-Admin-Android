package com.marsa.mtpadmin.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

fun statusLabel(status: String?): String = when (status) {
    "queued" -> "Antrean"
    "run" -> "Dikerjakan"
    "wait" -> "Tertunda"
    "ok" -> "Selesai"
    "fail" -> "Gagal"
    "cancel" -> "Dibatalkan"
    else -> status ?: "Tidak diketahui"
}

fun statusColor(status: String?): Color = when (status) {
    "queued" -> Color(0xFF3B82F6)
    "run" -> Color(0xFF0EA5E9)
    "wait" -> Color(0xFFF59E0B)
    "ok" -> Color(0xFF16A34A)
    "fail" -> Color(0xFFEF4444)
    "cancel" -> Color(0xFF94A3B8)
    else -> Color.Gray
}

fun formatBytes(bytes: Long?): String {
    if (bytes == null || bytes <= 0) return "Ukuran tidak diketahui"
    return when {
        bytes >= 1_048_576 -> "%.2f MB".format(bytes / 1_048_576.0)
        bytes >= 1_024 -> "%.1f KB".format(bytes / 1_024.0)
        else -> "$bytes B"
    }
}

val statusOptions = listOf(
    "queued" to "Antrean",
    "run" to "Dikerjakan",
    "wait" to "Tertunda",
    "ok" to "Selesai",
    "fail" to "Gagal",
    "cancel" to "Dibatalkan"
)

val filterStatusOptions = listOf(
    "" to "Semua",
    "queued" to "Antrean",
    "run" to "Dikerjakan",
    "wait" to "Tertunda",
    "ok" to "Selesai",
    "fail" to "Gagal",
    "cancel" to "Dibatalkan"
)

@Composable
fun StatusBadge(status: String?) {
    val color = statusColor(status)
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = statusLabel(status),
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun DetailCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun DetailRow(label: String, value: String?) {
    if (!value.isNullOrBlank()) {
        Column(Modifier.padding(vertical = 4.dp)) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
