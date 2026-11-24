package com.marsa.mtpadmin.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marsa.mtpadmin.ApiClient
import com.marsa.mtpadmin.ReportSummary
import com.marsa.mtpadmin.ui.components.StatusBadge
import com.marsa.mtpadmin.ui.components.filterStatusOptions
import com.marsa.mtpadmin.ui.report.ReportDetailScreen
import kotlinx.coroutines.launch
import retrofit2.HttpException

@Composable
fun DashboardScreen(
    name: String,
    darkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onLogout: () -> Unit
) {
    var selectedReportId by remember { mutableStateOf<Int?>(null) }

    if (selectedReportId != null) {
        ReportDetailScreen(
            reportId = selectedReportId!!,
            onBack = { selectedReportId = null }
        )
    } else {
        DashboardContent(
            name = name,
            darkTheme = darkTheme,
            onThemeChange = onThemeChange,
            onLogout = onLogout,
            onReportClick = { selectedReportId = it }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardContent(
    name: String,
    darkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onLogout: () -> Unit,
    onReportClick: (Int) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var reports by remember { mutableStateOf<List<ReportSummary>>(emptyList()) }
    var total by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf("") }

    fun loadReports(isRefresh: Boolean = false) {
        scope.launch {
            if (isRefresh) refreshing = true else loading = true
            error = ""

            try {
                val result = ApiClient.service.reports(
                    status = selectedStatus.ifBlank { null },
                    search = search.trim().ifBlank { null },
                    perPage = 40,
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
                    is HttpException -> when (e.code()) {
                        401, 403 -> "Sesi tidak valid atau akses ditolak."
                        else -> "Server error (${e.code()})."
                    }
                    else -> "Tidak dapat mengambil data. Periksa koneksi."
                }
            } finally {
                loading = false
                refreshing = false
            }
        }
    }

    LaunchedEffect(selectedStatus) {
        loadReports()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "MTP ADMIN",
                    color = colors.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = "Halo, $name",
                    color = colors.onBackground,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(onClick = { onThemeChange(!darkTheme) }) {
                Icon(
                    imageVector = if (darkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Ganti tema",
                    tint = colors.primary
                )
            }

            TextButton(onClick = onLogout) {
                Text("Keluar", color = colors.error, fontWeight = FontWeight.Medium)
            }
        }

        // Summary card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = colors.primaryContainer)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "TOTAL LAPORAN",
                        color = colors.onPrimaryContainer.copy(alpha = 0.75f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp
                    )
                    Text(
                        text = total.toString(),
                        color = colors.onPrimaryContainer,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(
                    onClick = { loadReports(isRefresh = true) },
                    enabled = !loading && !refreshing
                ) {
                    if (refreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = colors.onPrimaryContainer
                        )
                    } else {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Muat ulang",
                            tint = colors.onPrimaryContainer
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Search
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            placeholder = { Text("Cari nomor atau judul…") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null)
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    focusManager.clearFocus()
                    loadReports()
                }
            )
        )

        Spacer(Modifier.height(10.dp))

        // Status filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filterStatusOptions.forEach { (value, label) ->
                val selected = selectedStatus == value
                FilterChip(
                    selected = selected,
                    onClick = { selectedStatus = value },
                    label = { Text(label, fontSize = 13.sp) },
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.primary,
                        selectedLabelColor = colors.onPrimary
                    )
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (reports.isEmpty()) "Tidak ada laporan" else "${reports.size} ditampilkan",
                color = colors.onBackground.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
        }

        if (loading && reports.isEmpty()) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
        }

        if (error.isNotBlank()) {
            Text(
                text = error,
                color = colors.error,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = { loadReports(isRefresh = true) },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (!loading && reports.isEmpty() && error.isBlank()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Belum ada laporan",
                            color = colors.onBackground.copy(alpha = 0.5f),
                            fontSize = 15.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { loadReports() }) {
                            Text("Muat ulang")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 4.dp,
                        bottom = 24.dp
                    )
                ) {
                    items(reports, key = { it.id }) { report ->
                        ReportCard(
                            report = report,
                            onClick = { onReportClick(report.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportCard(
    report: ReportSummary,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = report.report_number ?: "Laporan #${report.id}",
                    color = colors.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                StatusBadge(report.status)
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = report.title ?: "Tanpa judul",
                color = colors.onSurface,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = listOfNotNull(
                    report.reporter_name,
                    report.category
                ).joinToString(" • ").ifBlank { "—" },
                color = colors.onSurfaceVariant,
                fontSize = 12.sp
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Tahap ${report.stage}/5",
                    color = colors.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = report.created_at?.take(10) ?: "",
                    color = colors.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }
    }
}
