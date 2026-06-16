package com.dalyly.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dalyly.AppViewModel
import com.dalyly.CategoryEntity
import com.dalyly.showToastMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingFormScreen(
    viewModel: AppViewModel,
    initialProviderId: String? = null,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val categories by viewModel.categories.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val language by viewModel.language.collectAsState()

    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var selectedDistrict by remember { mutableStateOf("أمانة العاصمة") }
    var additionalNotes by remember { mutableStateOf("") }

    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var districtMenuExpanded by remember { mutableStateOf(false) }
    var showConfirmationDialog by remember { mutableStateOf(false) }

    val yemenDistricts = listOf(
        "أمانة العاصمة", "صنعاء", "عدن", "تعز", "الحديدة", "حضرموت", "إب", "ذمار", "مأرب", "الحوطة"
    )

    // Automatically set initial category if we had a provider id
    LaunchedEffect(categories, initialProviderId) {
        if (selectedCategory == null && categories.isNotEmpty()) {
            if (initialProviderId != null) {
                val provider = viewModel.providers.value.find { it.id == initialProviderId }
                if (provider != null) {
                    selectedCategory = categories.find { it.id == provider.mainCategoryId }
                }
            }
            if (selectedCategory == null) {
                selectedCategory = categories.firstOrNull()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (language == "ar") "طلب حجز خدمة جديدة" else "Request New Booking",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (language == "ar") "يرجى تعبئة التفاصيل أدناه بدقة" else "Please fill absolute booking details fully",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (language == "ar") "سيتم توجيه طلبك للفنيين المعتمدين والمتابعة العاجلة في غضون لحظات." else "Vetted competent mechanics will match your order in absolute security.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            // Customer Name
            OutlinedTextField(
                value = customerName,
                onValueChange = { customerName = it },
                label = { Text(if (language == "ar") "الاسم الثلاثي للعميل" else "Customer Tripartite Name") },
                placeholder = { Text(if (language == "ar") "مثال: علي محمد حسن الوصابي" else "e.g., Ali Mohamed Hassan Al-Wasabi") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Customer Phone
            OutlinedTextField(
                value = customerPhone,
                onValueChange = { customerPhone = it },
                label = { Text(if (language == "ar") "رقم هاتف العميل" else "Customer Phone Number") },
                placeholder = { Text("e.g. 777000000") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = RoundedCornerShape(12.dp)
            )

            // Service Category Dropdown selection
            Text(
                text = if (language == "ar") "اختر الخدمة أو القسم المطلوب:" else "Select Required Service Category:",
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    onClick = { categoryMenuExpanded = true },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedCategory?.let { if (language == "ar") it.nameAr else it.nameEn } ?: (if (language == "ar") "اختر القسم" else "Choose Category"),
                            fontSize = 14.sp
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                    }
                }
                DropdownMenu(
                    expanded = categoryMenuExpanded,
                    onDismissRequest = { categoryMenuExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text("${cat.imageUrl ?: "🔧"} ${if (language == "ar") cat.nameAr else cat.nameEn}") },
                            onClick = {
                                selectedCategory = cat
                                categoryMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // City Area / District dropdown selection
            Text(
                text = if (language == "ar") "منطقة السكن / المحافظة:" else "Residence District Area:",
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    onClick = { districtMenuExpanded = true },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = selectedDistrict, fontSize = 14.sp)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                    }
                }
                DropdownMenu(
                    expanded = districtMenuExpanded,
                    onDismissRequest = { districtMenuExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    yemenDistricts.forEach { dist ->
                        DropdownMenuItem(
                            text = { Text(dist) },
                            onClick = {
                                selectedDistrict = dist
                                districtMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // Additional Notes Input field
            OutlinedTextField(
                value = additionalNotes,
                onValueChange = { additionalNotes = it },
                label = { Text(if (language == "ar") "ملاحظات إضافية أو تفاصيل العطل والخدمة" else "Additional Service Details / Notes") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                maxLines = 4,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Submit Button
            Button(
                onClick = {
                    val split = customerName.trim().split("\\s+".toRegex())
                    if (customerName.trim().isEmpty() || customerPhone.trim().isEmpty()) {
                        context.showToastMessage(if (language == "ar") "برجاء كتابة الاسم ورقم الهاتف!" else "Please write your name and phone number!")
                    } else if (split.size < 3) {
                        context.showToastMessage(if (language == "ar") "برجاء كتابة الاسم الثلاثي كاملاً للتحقق والتوثيق!" else "Please write the composite tripartite name fully!")
                    } else if (selectedCategory == null) {
                        context.showToastMessage(if (language == "ar") "برجاء اختيار فئة الخدمة المطلوبة!" else "Please specify service category!")
                    } else {
                        showConfirmationDialog = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = if (language == "ar") "تقديم طلب الحجز 🗓️" else "Review & Complete Booking",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }

    // Confirmation Dialog
    if (showConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmationDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = "Verify", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (language == "ar") "تأكيد بيانات الحجز" else "Confirm Booking Information",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (language == "ar") "الرجاء مراجعة البيانات بدقة قبل الإرسال:" else "Please check details carefully before dispatching:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    
                    Row {
                        Text(
                            text = if (language == "ar") "الاسم الثلاثي: " else "Tripartite Name: ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.width(95.dp)
                        )
                        Text(text = customerName.trim(), fontSize = 13.sp)
                    }

                    Row {
                        Text(
                            text = if (language == "ar") "رقم الهاتف: " else "Phone-Number: ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.width(95.dp)
                        )
                        Text(text = customerPhone.trim(), fontSize = 13.sp)
                    }

                    Row {
                        Text(
                            text = if (language == "ar") "الخدمة: " else "Service: ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.width(95.dp)
                        )
                        Text(
                            text = selectedCategory?.let { if (language == "ar") it.nameAr else it.nameEn } ?: "",
                            fontSize = 13.sp
                        )
                    }

                    Row {
                        Text(
                            text = if (language == "ar") "منطقة السكن: " else "District Area: ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.width(95.dp)
                        )
                        Text(text = selectedDistrict, fontSize = 13.sp)
                    }

                    if (additionalNotes.isNotEmpty()) {
                        Row {
                            Text(
                                text = if (language == "ar") "ملاحظات: " else "Additions: ",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.width(95.dp)
                            )
                            Text(text = additionalNotes, fontSize = 13.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmationDialog = false
                        viewModel.createBooking(
                            fullName = customerName.trim(),
                            phone = customerPhone.trim(),
                            categoryId = selectedCategory?.id ?: "c1",
                            district = selectedDistrict,
                            notes = additionalNotes.trim(),
                            providerId = initialProviderId,
                            customInputs = emptyMap()
                        )
                        context.showToastMessage(
                            if (language == "ar") "تم تسجيل الحجز بنجاح ويتزامن الآن مع الفنيين!"
                            else "Your service slot has been securely registered!"
                        )
                        onNavigateBack()
                    }
                ) {
                    Text(if (language == "ar") "تأكيد وإرسال" else "Confirm & Send")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmationDialog = false }) {
                    Text(if (language == "ar") "تعديل البيانات" else "Modify Details")
                }
            }
        )
    }
}
