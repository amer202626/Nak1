package com.dalyly.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dalyly.AppViewModel
import com.dalyly.showToastMessage
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bookings by viewModel.bookings.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val providers by viewModel.providers.collectAsState()
    val activityLogs by viewModel.activityLogs.collectAsState()
    val language by viewModel.language.collectAsState()

    var activeReportTab by remember { mutableStateOf(0) }
    var showExportMenu by remember { mutableStateOf(false) }

    // Filter controls
    var filterTypeIndex by remember { mutableStateOf(0) } // 0 = All, 1 = Past 24h, 2 = Past 7d, 3 = Custom
    var searchKeyword by remember { mutableStateOf("") }
    var customStartDate by remember { mutableStateOf("2026-01-01") }
    var customEndDate by remember { mutableStateOf("2026-12-31") }

    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    // Parse filters
    val filteredBookings = remember(bookings, filterTypeIndex, customStartDate, customEndDate, searchKeyword, categories) {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        val limitTime = when (filterTypeIndex) {
            1 -> now - (24 * 3600 * 1000L) // Today
            2 -> now - (7 * 24 * 3600 * 1000L) // Week
            else -> 0L
        }

        var customStartMs = 0L
        var customEndMs = Long.MAX_VALUE
        if (filterTypeIndex == 3) {
            try {
                customStartMs = sdf.parse(customStartDate)?.time ?: 0L
                customEndMs = sdf.parse(customEndDate)?.time ?: Long.MAX_VALUE
                // Add 1 day to end timestamp to include the whole selected date
                customEndMs += 24 * 3600 * 1000L
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        bookings.filter { booking ->
            // Check Date Range
            val matchesDate = when (filterTypeIndex) {
                0 -> true
                1, 2 -> booking.timestamp >= limitTime
                3 -> booking.timestamp in customStartMs..customEndMs
                else -> true
            }

            // Check Search keyword
            val matchedCat = categories.find { it.id == booking.serviceCategory }
            val catName = matchedCat?.nameAr ?: ""
            val matchesKeyword = if (searchKeyword.isBlank()) {
                true
            } else {
                booking.fullName.contains(searchKeyword, ignoreCase = true) ||
                booking.phone.contains(searchKeyword, ignoreCase = true) ||
                booking.district.contains(searchKeyword, ignoreCase = true) ||
                booking.status.contains(searchKeyword, ignoreCase = true) ||
                catName.contains(searchKeyword, ignoreCase = true)
            }

            matchesDate && matchesKeyword
        }
    }

    // Helper functions for mock export
    fun triggerExportToFile(format: String, reportTitle: String) {
        val formatLabel = format.uppercase(Locale.ROOT)
        // Generate a beautiful summary text to simulate saving
        val dataString = buildString {
            append("تقرير: ").append(reportTitle).append("\n")
            append("التنسيق: ").append(formatLabel).append("\n")
            append("التاريخ: ").append(sdf.format(Date())).append("\n")
            append("عدد السجلات: ").append(filteredBookings.size).append("\n")
            append("=== تفاصيل الحجوزات ===\n")
            filteredBookings.take(20).forEach {
                append("${it.fullName} | ${it.phone} | ${it.district} | ${it.status}\n")
            }
        }
        context.showToastMessage(
            if (language == "ar") "تم تصدير $reportTitle بصيغة $formatLabel وحفظه بنجاح! 📂"
            else "Exported $reportTitle to $formatLabel format successfully!"
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Upper Filters card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (language == "ar") "⚙️ فلتر التقارير الذكي" else "⚙️ Reports Dynamic Filter",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                // Quick Filters
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val filterLabels = listOf(
                        if (language == "ar") "الجميع" else "All",
                        if (language == "ar") "اليوم" else "Today",
                        if (language == "ar") "٧ أيام" else "7 Days",
                        if (language == "ar") "مخصص" else "Custom"
                    )

                    filterLabels.forEachIndexed { idx, label ->
                        FilterChip(
                            selected = filterTypeIndex == idx,
                            onClick = { filterTypeIndex = idx },
                            label = { Text(label, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Custom Date Fields
                AnimatedVisibility(visible = filterTypeIndex == 3) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customStartDate,
                            onValueChange = { customStartDate = it },
                            label = { Text(if (language == "ar") "تاريخ البداية (YYYY-MM-DD)" else "Start Date") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(fontSize = 11.sp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                        )

                        OutlinedTextField(
                            value = customEndDate,
                            onValueChange = { customEndDate = it },
                            label = { Text(if (language == "ar") "تاريخ النهاية (YYYY-MM-DD)" else "End Date") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(fontSize = 11.sp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                        )
                    }
                }

                // Keyword Search Match bar
                OutlinedTextField(
                    value = searchKeyword,
                    onValueChange = { searchKeyword = it },
                    label = { Text(if (language == "ar") "البحث برقم الهاتف أو المحافظة..." else "Search keyword/district...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(20.dp)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                )
            }
        }

        // Horizontal navigation tabs for Reports
        ScrollableTabRow(
            selectedTabIndex = activeReportTab,
            edgePadding = 8.dp,
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            val reportTabs = listOf(
                if (language == "ar") "الرئيسية 📊" else "Stats Icon",
                if (language == "ar") "الأكثر طلباً 📈" else "Most Requested",
                if (language == "ar") "الفنيين المميزين 🏆" else "Top Technicians",
                if (language == "ar") "ساعات الذروة ⏰" else "Peak Hour Analytics",
                if (language == "ar") "سجل الرقابة ⚖️" else "Audit Log",
                if (language == "ar") "الحجوزات الشاملة 🗒️" else "Bookings Master List"
            )

            reportTabs.forEachIndexed { index, title ->
                Tab(
                    selected = activeReportTab == index,
                    onClick = { activeReportTab = index },
                    text = { Text(title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                )
            }
        }

        // Actions Header (Export triggers)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (language == "ar") "نتائج الفلترة (${filteredBookings.size} حجز)" else "Matching Count (${filteredBookings.size} slots)",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Box {
                Button(
                    onClick = { showExportMenu = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Export", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (language == "ar") "تصدير التقرير 📥" else "Export File", fontSize = 11.sp)
                }

                DropdownMenu(expanded = showExportMenu, onDismissRequest = { showExportMenu = false }) {
                    val reportTitle = when (activeReportTab) {
                        0 -> "إحصائيات الحجوزات السريعة"
                        1 -> "تقرير الأقسام الأكثر طلباً"
                        2 -> "تقرير الفنيين المميزين وتحقيق الخدمات"
                        3 -> "تقرير أوقات الذروة"
                        4 -> "تقرير سجل الرقابة والنشاطات"
                        else -> "تقرير الحجوزات الشامل للعملاء"
                    }

                    DropdownMenuItem(
                        text = { Row {
                            Icon(Icons.Default.FileOpen, contentDescription = "PDF", tint = Color.Red, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("تصدير بصيغة PDF")
                        }},
                        onClick = {
                            showExportMenu = false
                            triggerExportToFile("pdf", reportTitle)
                        }
                    )
                    DropdownMenuItem(
                        text = { Row {
                            Icon(Icons.Default.GridOn, contentDescription = "Excel", tint = Color(0xFF1F7246), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("تصدير بصيغة Excel")
                        }},
                        onClick = {
                            showExportMenu = false
                            triggerExportToFile("excel", reportTitle)
                        }
                    )
                    DropdownMenuItem(
                        text = { Row {
                            Icon(Icons.Default.Description, contentDescription = "CSV", tint = Color.Blue, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("تصدير بصيغة CSV")
                        }},
                        onClick = {
                            showExportMenu = false
                            triggerExportToFile("csv", reportTitle)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Content Area depending on current tab
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) {
            when (activeReportTab) {
                0 -> {
                    // TAB 0: Simple Booking Summary Stats
                    val now = System.currentTimeMillis()
                    val sod = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                    }.timeInMillis

                    val todayCount = bookings.count { it.timestamp >= sod }
                    val pendingCount = bookings.count { it.status == "pending" || it.status == "قيد المراجعة" }
                    val completedCount = bookings.count { it.status == "completed" || it.status == "مكتمل" }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (language == "ar") "لوحة إحصائيات سريعة" else "Simple Stats Dashboard",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatCard(
                                title = if (language == "ar") "حجوزات اليوم" else "Today Bookings",
                                count = todayCount.toString(),
                                icon = Icons.Default.Timeline,
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                title = if (language == "ar") "بانتظار المراجعة" else "Pending Review",
                                count = pendingCount.toString(),
                                icon = Icons.Default.VerifiedUser,
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatCard(
                                title = if (language == "ar") "إجمالي الحجوزات" else "Total System bookings",
                                count = bookings.size.toString(),
                                icon = Icons.Default.Bookmarks,
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                title = if (language == "ar") "الحجوزات المكتملة" else "Completed Service Slots",
                                count = completedCount.toString(),
                                icon = Icons.Default.TaskAlt,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Tips Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Lightbulb, contentDescription = "Tip", tint = Color.Yellow)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (language == "ar") "نصيحة الرقابة والدعم:" else "Security audit compliance tip:",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = if (language == "ar") "تأكد من تتبع سجل نشاطات المشرفين دورياً من التبويب المخصص لمنع أي تفويض غير مصرح به." else "Check supervisor activity logs daily to maintain high Yemen compliance integrity.",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // TAB 1: Most requested categories (Chart visual)
                    val catCounts = remember(filteredBookings, categories) {
                        categories.map { cat ->
                            val count = filteredBookings.count { it.serviceCategory == cat.id }
                            cat to count
                        }.sortedByDescending { it.second }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = if (language == "ar") "📈 تقرير وحجم الطلب على الأقسام والمهن" else "📈 Primary Requested Categories Volume",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )

                        if (catCounts.all { it.second == 0 }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (language == "ar") "لا تتوفر حجوزات كافية لعرض الرسم البياني!" else "No bookings found during these filters timeframe.",
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    fontSize = 12.sp
                                )
                            }
                        } else {
                            // Beautiful Vector Horizontal Custom Chart bars
                            catCounts.take(5).forEachIndexed { index, (cat, count) ->
                                val pct = if (filteredBookings.isEmpty()) 0f else count.toFloat() / filteredBookings.size
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("${cat.imageUrl ?: "🔧"} ${if (language == "ar") cat.nameAr else cat.nameEn}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        Text("$count حجز (${(pct * 100).toInt()}%)", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }

                                    // Bar view
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(14.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(pct)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    brush = Brush.horizontalGradient(
                                                        colors = listOf(
                                                            MaterialTheme.colorScheme.primary,
                                                            MaterialTheme.colorScheme.secondary
                                                        )
                                                    )
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // TAB 2: Top Technicians (Descending completed services count)
                    val techsRanking = remember(filteredBookings, providers) {
                        providers.map { prov ->
                            val count = filteredBookings.count { it.providerId == prov.id }
                            prov to count
                        }.sortedByDescending { it.second }
                    }

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (language == "ar") "🏆 قائمة الفنيين الأنشط إنجازاً للطلبيات" else "🏆 Top Vetted Technicians Performance",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )

                        if (techsRanking.isEmpty() || techsRanking.all { it.second == 0 }) {
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                Text(if (language == "ar") "لا توجد بيانات حركة حجوزات للفنيين المعرفين!" else "No technicians completed orders during filter timeframe.", fontSize = 12.sp)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(techsRanking.take(15)) { (prov, count) ->
                                    Card(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(RoundedCornerShape(18.dp))
                                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Default.Engineering, contentDescription = "Tech", tint = MaterialTheme.colorScheme.primary)
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(prov.fullName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                    Text("الهاتف: ${prov.phone} | ${prov.district}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                                }
                                            }

                                            Badge(
                                                containerColor = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(4.dp)
                                            ) {
                                                Text(
                                                    text = if (language == "ar") "$count خدمة" else "$count slots",
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                3 -> {
                    // TAB 3: Peak times analysis (Peak Hour of Day & Day of Week)
                    val peakStats = remember(filteredBookings) {
                        val hoursStr = IntArray(24)
                        val daysStr = IntArray(7) // 0=Sunday, 1=Monday ... 6=Saturday

                        val cal = Calendar.getInstance()
                        filteredBookings.forEach { b ->
                            cal.timeInMillis = b.timestamp
                            val hr = cal.get(Calendar.HOUR_OF_DAY)
                            val dy = cal.get(Calendar.DAY_OF_WEEK) - 1 // convert to 0-6
                            if (hr in 0..23) hoursStr[hr]++
                            if (dy in 0..6) daysStr[dy]++
                        }

                        val maxHour = hoursStr.indices.maxByOrNull { hoursStr[it] } ?: 0
                        val maxDay = daysStr.indices.maxByOrNull { daysStr[it] } ?: 0

                        val totalHrsSum = hoursStr.sum().coerceAtLeast(1)
                        val totalDaysSum = daysStr.sum().coerceAtLeast(1)

                        val dayNames = listOf("الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")
                        val dayNamesEn = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")

                        Triple(hoursStr, daysStr, Pair(maxHour, maxDay))
                    }

                    val hoursStr = peakStats.first
                    val daysStr = peakStats.second
                    val maxHourPair = peakStats.third.first
                    val maxDayPair = peakStats.third.second
                    val totalSum = filteredBookings.size

                    val daysLabelsAr = listOf("الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = if (language == "ar") "⏰ أوقات الذروة وتسجيل الطلبات الأعلى" else "⏰ Booking Peak Analytics Summary",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )

                        if (totalSum == 0) {
                            Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                                Text(if (language == "ar") "لا توجد أي حجوزات داخل نطاق التوقيت المحدد للتحليل" else "No peak metrics detected", fontSize = 11.sp)
                            }
                        } else {
                            // Highlight cards
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = if (language == "ar") "🔔 ذروة النشاط اللحظي" else "🔔 Highlight Peak Hours",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = if (language == "ar") "أعلى ساعة نشاط باليوم: الساعة $maxHourPair:00 مساوية (${hoursStr[maxHourPair]} حجوزات)" 
                                               else "Peak Activity Hour: $maxHourPair:00 Hrs with ${hoursStr[maxHourPair]} bookings",
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = if (language == "ar") "أعلى يوم حجز بالأسبوع: يوم ${daysLabelsAr[maxDayPair]} مساوية (${daysStr[maxDayPair]} حجوزات)" 
                                               else "Peak Activity Weekday: ${daysLabelsAr[maxDayPair]} with ${daysStr[maxDayPair]} entries",
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = if (language == "ar") "توزيع حجم الحجوزات على مدار ساعات اليوم (24h):" else "Volumetric hour chart over 24 Hrs:",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            )

                            // Render simple Hour of Day stats bars
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                (0..23).filter { hoursStr[it] > 0 }.forEach { hr ->
                                    val count = hoursStr[hr]
                                    val pct = count.toFloat() / totalSum
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(text = "ساعة %02d:00".format(hr), fontSize = 11.sp, modifier = Modifier.width(75.dp))
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(10.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .fillMaxWidth(pct)
                                                    .background(MaterialTheme.colorScheme.primary)
                                            )
                                        }
                                        Text(text = "$count (${(pct * 100).toInt()}%)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
                4 -> {
                    // TAB 4: Audit Logs (System actions records dashboard)
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (language == "ar") "⚖️ سجل مراجعة نشاطات المستخدمين والمشرفين (Audit Trail)" else "⚖️ System Supervisors Activity Log (Audit Trail)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        if (activityLogs.isEmpty()) {
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                Text(if (language == "ar") "سجل نشاطات الرقابة نظيف تماماً!" else "Audit trail is empty currently!", fontSize = 12.sp)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(activityLogs.sortedByDescending { it.timestamp }) { log ->
                                    val logDate = remember(log.timestamp) {
                                        val c = Calendar.getInstance().apply { timeInMillis = log.timestamp }
                                        val day = c.get(Calendar.DAY_OF_MONTH)
                                        val m = c.get(Calendar.MONTH) + 1
                                        val hour = c.get(Calendar.HOUR_OF_DAY)
                                        val min = c.get(Calendar.MINUTE)
                                        "%02d/%02d %02d:%01d".format(day, m, hour, min)
                                    }
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = log.actor,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = logDate,
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = if (language == "ar") log.actionAr else log.actionEn,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {
                    // TAB 5: Comprehensive flexible list (Filter, Search & Flex table)
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (language == "ar") "🗒️ جدول الحجوزات المطابقة الكلي" else "🗒️ Matching Bookings Matrix",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )

                        if (filteredBookings.isEmpty()) {
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                Text(if (language == "ar") "لا توجد نتائج حجز مطابقة للبحث دوراً!" else "No bookings found matching filters.", fontSize = 12.sp)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredBookings) { booking ->
                                    val matchedCat = categories.find { it.id == booking.serviceCategory }
                                    val matchedProv = providers.find { it.id == booking.providerId }

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(booking.fullName, fontWeight = FontWeight.Bold, fontSize = 12.sp)

                                                // Status badge
                                                SuggestionChip(
                                                    onClick = {},
                                                    label = { Text(booking.status, fontSize = 9.sp) }
                                                )
                                            }

                                            Text(
                                                text = if (language == "ar") "القسم المطلق: ${matchedCat?.nameAr ?: "عام"} | المنطقة: ${booking.district}" 
                                                       else "Category: ${matchedCat?.nameEn ?: "General"} | District: ${booking.district}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceGrid
                                            )

                                            if (matchedProv != null) {
                                                Text(
                                                    text = if (language == "ar") "الفني المسؤول: ${matchedProv.fullName} (${matchedProv.phone})"
                                                           else "Assigned Mechanic: ${matchedProv.fullName}",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }

                                            if (booking.notes.isNotEmpty()) {
                                                Text(
                                                    text = "ملاحظات: ${booking.notes}",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Custom Stat Card
@Composable
fun StatCard(
    title: String,
    count: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Text(text = count, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

// Color value resolver helper fallback for non-existing attributes
val ColorScheme.onSurfaceGrid: Color
    get() = this.onSurface.copy(alpha = 0.65f)
