package com.marsa.mtpadmin.ui.report

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marsa.mtpadmin.ApiClient
import com.marsa.mtpadmin.AssignmentRequest
import com.marsa.mtpadmin.ReportDetail
import com.marsa.mtpadmin.StatusRequest
import com.marsa.mtpadmin.Technician
import com.marsa.mtpadmin.ui.components.DetailCard
import com.marsa.mtpadmin.ui.components.DetailRow
import com.marsa.mtpadmin.ui.components.StatusBadge
import com.marsa.mtpadmin.ui.components.formatBytes
import com.marsa.mtpadmin.ui.components.statusOptions
import kotlinx.coroutines.launch
import retrofit2.HttpException

@Composable
fun ReportDetailScreen(
    reportId: Int,
    onBack: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()

    var detail by remember(reportId) { mutableStateOf<ReportDetail?>(null) }
    var technicians by remember { mutableStateOf<List<Technician>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var actionMessage by remember { mutableStateOf("") }
    var actionSuccess by remember { mutableStateOf(false) }

    var reloadKey by remember { mutableIntStateOf(0) }
    var selectedStatus by remember { mutableStateOf("queued") }
    var selectedStage by remember { mutableIntStateOf(0) }
    var selectedTechnician by remember { mutableStateOf<Int?>(null) }
    var note by remember { mutableStateOf("") }

    var statusExpanded by remember { mutableStateOf(false) }
    var technicianExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(reportId, reloadKey) {
        loading = true
        error = ""
        actionMessage = ""

        try {
            val result = ApiClient.service.reportDetail(reportId)
            if (result.success && result.data != null) {
                detail = result.data
                selectedStatus = result.data.status ?: "queued"
                selectedStage = result.data.stage.coerceIn(0, 5)
                selectedTechnician = result.data.assigned_to
            } else {
                error = result.message ?: "Detail laporan tidak ditemukan."
            }

            try {
                val techResult = ApiClient.service.technicians()
                if (techResult.success && techResult.data != null) {
                    technicians = techResult.data
                }
            } catch (_: Exception) {
                // non-blocking
            }
        } catch (e: Exception) {
            error = when (e) {
                is HttpException -> "Gagal mengambil detail (HTTP ${e.code()})."
                else -> "Tidak dapat terhubung ke server."
            }
        } finally {
            loading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali"
                )
            }
            Text(
                text = "Detail laporan",
                modifier = Modifier.weight(1f),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colors.onBackground
            )
            IconButton(
                onClick = { reloadKey++ },
                enabled = !loading
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Muat ulang")
            }
        }

        if (loading) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }

        if (error.isNotBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(error, color = colors.error)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { reloadKey++ }) {
                    Text("Coba lagi")
                }
            }
            return
        }

        detail?.let { report ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surface)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = report.report_number ?: "Laporan #${report.id}",
                                color = colors.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = report.title ?: "Tanpa judul",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.onSurface
                            )
                            Spacer(Modifier.height(12.dp))
                            StatusBadge(report.status)
                            Spacer(Modifier.height(14.dp))
                            DetailRow("Kategori", report.category)
                            DetailRow("Prioritas", report.priority)
                            DetailRow("Tahap", "${report.stage}/5")
                            DetailRow("Tanggal", report.created_at)
                        }
                    }
                }

                item {
                    DetailCard(title = "Informasi pelapor") {
                        DetailRow("Nama", report.reporter_name)
                        DetailRow("Peran", report.reporter_role)
                        DetailRow("Unit", report.reporter_unit)
                        DetailRow(
                            "Teknisi ditugaskan",
                            technicians.firstOrNull { it.id == report.assigned_to }?.name
                                ?: report.assigned_to?.let { "ID #$it" }
                                ?: "Belum ditugaskan"
                        )
                    }
                }

                item {
                    DetailCard(title = "Deskripsi laporan") {
                        Text(
                            text = report.description?.takeIf { it.isNotBlank() }
                                ?: "Tidak ada deskripsi.",
                            color = colors.onSurface,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }

                // Manage status
                item {
                    DetailCard(title = "Kelola status") {
                        Text("Status", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                        Spacer(Modifier.height(6.dp))
                        Box {
                            OutlinedButton(
                                onClick = { statusExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    statusOptions.firstOrNull { it.first == selectedStatus }?.second
                                        ?: selectedStatus
                                )
                            }
                            DropdownMenu(
                                expanded = statusExpanded,
                                onDismissRequest = { statusExpanded = false }
                            ) {
                                statusOptions.forEach { (value, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            selectedStatus = value
                                            statusExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        Text(
                            text = "Tahap pengerjaan: $selectedStage / 5",
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                        Slider(
                            value = selectedStage.toFloat(),
                            onValueChange = { selectedStage = it.toInt().coerceIn(0, 5) },
                            valueRange = 0f..5f,
                            steps = 4
                        )

                        Spacer(Modifier.height(6.dp))

                        Text("Catatan perubahan", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Tulis catatan (opsional)") },
                            minLines = 2,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(Modifier.height(14.dp))

                        Button(
                            onClick = {
                                scope.launch {
                                    saving = true
                                    actionMessage = ""
                                    try {
                                        val result = ApiClient.service.updateStatus(
                                            reportId,
                                            StatusRequest(
                                                status = selectedStatus,
                                                stage = selectedStage,
                                                note = note.trim().ifBlank { null }
                                            )
                                        )
                                        if (result.success) {
                                            actionMessage = "Status berhasil diperbarui."
                                            actionSuccess = true
                                            note = ""
                                            reloadKey++
                                        } else {
                                            actionMessage = result.message ?: "Gagal memperbarui status."
                                            actionSuccess = false
                                        }
                                    } catch (e: Exception) {
                                        actionMessage = when (e) {
                                            is HttpException -> "Gagal menyimpan (HTTP ${e.code()})."
                                            else -> "Koneksi gagal saat menyimpan."
                                        }
                                        actionSuccess = false
                                    } finally {
                                        saving = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !saving,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (saving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = colors.onPrimary
                                )
                            } else {
                                Text("Simpan status")
                            }
                        }
                    }
                }

                // Assignment
                item {
                    DetailCard(title = "Penugasan teknisi") {
                        Box {
                            OutlinedButton(
                                onClick = { technicianExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    technicians.firstOrNull { it.id == selectedTechnician }?.name
                                        ?: if (selectedTechnician == null) "Belum ditugaskan"
                                        else "Teknisi #$selectedTechnician"
                                )
                            }
                            DropdownMenu(
                                expanded = technicianExpanded,
                                onDismissRequest = { technicianExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Belum ditugaskan") },
                                    onClick = {
                                        selectedTechnician = null
                                        technicianExpanded = false
                                    }
                                )
                                technicians.forEach { tech ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(tech.name)
                                                Text(
                                                    tech.unit ?: tech.role ?: "",
                                                    fontSize = 11.sp,
                                                    color = colors.onSurfaceVariant
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedTechnician = tech.id
                                            technicianExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    saving = true
                                    actionMessage = ""
                                    try {
                                        val result = ApiClient.service.assignTechnician(
                                            reportId,
                                            AssignmentRequest(technician_id = selectedTechnician)
                                        )
                                        if (result.success) {
                                            actionMessage = "Penugasan berhasil diperbarui."
                                            actionSuccess = true
                                            reloadKey++
                                        } else {
                                            actionMessage = result.message ?: "Gagal menyimpan penugasan."
                                            actionSuccess = false
                                        }
                                    } catch (e: Exception) {
                                        actionMessage = when (e) {
                                            is HttpException -> "Gagal menyimpan (HTTP ${e.code()})."
                                            else -> "Koneksi gagal saat menyimpan."
                                        }
                                        actionSuccess = false
                                    } finally {
                                        saving = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !saving,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (saving) "Menyimpan…" else "Simpan penugasan")
                        }

                        if (actionMessage.isNotBlank()) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = actionMessage,
                                color = if (actionSuccess) Color(0xFF16A34A) else colors.error,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                // Events
                item {
                    DetailCard(title = "Riwayat aktivitas (${report.events.size})") {
                        if (report.events.isEmpty()) {
                            Text(
                                "Belum ada riwayat aktivitas.",
                                color = colors.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        } else {
                            report.events.forEachIndexed { index, event ->
                                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                    Text(
                                        text = event.title ?: event.type ?: "Aktivitas",
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.primary,
                                        fontSize = 14.sp
                                    )
                                    if (!event.note.isNullOrBlank()) {
                                        Text(
                                            text = event.note,
                                            fontSize = 13.sp,
                                            color = colors.onSurface,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = listOfNotNull(event.actor, event.created_at)
                                            .joinToString(" • "),
                                        fontSize = 11.sp,
                                        color = colors.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                                if (index < report.events.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        color = colors.outlineVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Attachments
                item {
                    DetailCard(title = "Lampiran (${report.attachments.size})") {
                        if (report.attachments.isEmpty()) {
                            Text(
                                "Tidak ada lampiran.",
                                color = colors.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        } else {
                            report.attachments.forEachIndexed { index, att ->
                                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                    Text(
                                        text = att.name ?: "File",
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "${att.mime_type ?: "File"} • ${formatBytes(att.size_bytes)}",
                                        fontSize = 12.sp,
                                        color = colors.onSurfaceVariant
                                    )
                                }
                                if (index < report.attachments.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        color = colors.outlineVariant
                                    )
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}
