package com.marsa.mtpadmin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marsa.mtpadmin.ApiClient
import com.marsa.mtpadmin.ReportSummary
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    name: String,
    darkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onLogout: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()

    var reports by remember { mutableStateOf<List<ReportSummary>>(emptyList()) }
    var total by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf("") }
    var filterExpanded by remember { mutableStateOf(false) }

    val statuses = listOf(
        "" to "Semua status",
        "queued" to "Antrean",
        "run" to "Dikerjakan",
        "wait" to "Tertunda",
        "ok" to "Selesai",
        "fail" to "Gagal",
        "cancel" to "Dibatalkan"
    )

    fun loadReports() {
        scope.launch {
            loading = true
            error = ""

            try {
                val result = ApiClient.service.reports(
                    status = selectedStatus.ifBlank { null },
                    search = search.trim().ifBlank { null },
                    perPage = 30,
                    page = 1
                )

                if (result.success && result.data != null) {
                    reports = result.data.data
                    total = result.data.total
                } else {
                    error = result.message ?: "Gagal mengambil laporan."
                }
            } catch (e: Exception) {
                error = when (e) {
                    is retrofit2.HttpException ->
                        if (e.code() == 401 || e.code() == 403)
                            "Sesi tidak valid atau akses administrator ditolak."
                        else
                            "Server mengembalikan error ${e.code()}."
                    else -> "Tidak dapat mengambil data. Periksa koneksi internet."
                }
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadReports()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "MTP ADMIN",
                    color = colors.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Halo, $name 👋",
                    color = colors.onBackground,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(onClick = { onThemeChange(!darkTheme) }) {
                Icon(
                    if (darkTheme) Icons.Default.LightMode
                    else Icons.Default.DarkMode,
                    contentDescription = "Ganti tema",
                    tint = colors.primary
                )
            }

            IconButton(onClick = onLogout) {
                Text(
                    "Keluar",
                    color = colors.error,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = colors.primaryContainer
            )
        ) {
            Column(Modifier.padding(18.dp)) {
                Text(
                    "TOTAL LAPORAN",
                    color = colors.onPrimaryContainer.copy(alpha = 0.75f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    total.toString(),
                    color = colors.onPrimaryContainer,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Data laporan dari server",
                    color = colors.onPrimaryContainer.copy(alpha = 0.75f),
                    fontSize = 12.sp
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        Text(
            "Daftar laporan",
            color = colors.onBackground,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Cari nomor atau judul") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            IconButton(
                onClick = { loadReports() },
                enabled = !loading
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Cari",
                    tint = colors.primary
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Box {
            OutlinedButton(
                onClick = { filterExpanded = true },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    statuses.firstOrNull { it.first == selectedStatus }?.second
                        ?: "Semua status"
                )
            }

            DropdownMenu(
                expanded = filterExpanded,
                onDismissRequest = { filterExpanded = false }
            ) {
                statuses.forEach { (value, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            selectedStatus = value
                            filterExpanded = false
                            loadReports()
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${reports.size} laporan ditampilkan",
                color = colors.onBackground.copy(alpha = 0.65f),
                fontSize = 12.sp
            )

            IconButton(
                onClick = { loadReports() },
                enabled = !loading
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Muat ulang"
                )
            }
        }

        if (loading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (error.isNotBlank()) {
            Text(
                error,
                color = colors.error,
                fontSize = 13.sp,
                modifier = Modifier.padding(vertical = 10.dp)
            )
        }

        if (!loading && reports.isEmpty() && error.isBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Belum ada laporan.",
                    color = colors.onBackground.copy(alpha = 0.6f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(reports, key = { it.id }) { report ->
                    ReportCard(report)
                }
            }
        }
    }
}

@Composable
private fun ReportCard(report: ReportSummary) {
    val colors = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.surface
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    report.report_number ?: "Laporan #${report.id}",
                    color = colors.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = statusColor(report.status).copy(alpha = 0.15f)
                ) {
                    Text(
                        statusLabel(report.status),
                        color = statusColor(report.status),
                        modifier = Modifier.padding(
                            horizontal = 10.dp,
                            vertical = 5.dp
                        ),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                report.title ?: "Tanpa judul",
                color = colors.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Spacer(Modifier.height(6.dp))

            Text(
                "${report.reporter_name ?: "Anonim"} • ${report.category ?: "Umum"}",
                color = colors.onSurface.copy(alpha = 0.65f),
                fontSize = 12.sp
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Tahap ${report.stage}/5",
                    color = colors.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    report.created_at?.take(10) ?: "",
                    color = colors.onSurface.copy(alpha = 0.55f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

private fun statusLabel(status: String?): String {
    return when (status) {
        "queued" -> "Antrean"
        "run" -> "Dikerjakan"
        "wait" -> "Tertunda"
        "ok" -> "Selesai"
        "fail" -> "Gagal"
        "cancel" -> "Dibatalkan"
        else -> status ?: "Tidak diketahui"
    }
}

private fun statusColor(status: String?): Color {
    return when (status) {
        "queued" -> Color(0xFF3B82F6)
        "run" -> Color(0xFF0EA5E9)
        "wait" -> Color(0xFFF59E0B)
        "ok" -> Color(0xFF16A34A)
        "fail" -> Color(0xFFEF4444)
        "cancel" -> Color(0xFF94A3B8)
        else -> Color.Gray
    }
}
