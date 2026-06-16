package com.dalyly

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.dalyly.screens.TechnicianApprovalScreen
import com.dalyly.screens.ReportsScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sqrt

// --- Beautiful UI Component Screens ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboardScreen(viewModel: AppViewModel, onNavigateToBooking: (String?) -> Unit = {}) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val providers by viewModel.providers.collectAsStateWithLifecycle()
    val registrations by viewModel.registrations.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val reviews by viewModel.reviews.collectAsStateWithLifecycle()
    val reports by viewModel.reports.collectAsStateWithLifecycle()
    val bookings by viewModel.bookings.collectAsStateWithLifecycle()
    val bookingFields by viewModel.bookingFields.collectAsStateWithLifecycle()
    val adminSupervisors by viewModel.adminSupervisors.collectAsStateWithLifecycle()
    val activityLogs by viewModel.activityLogs.collectAsStateWithLifecycle()
    val notificationConfigs by viewModel.notificationConfigs.collectAsStateWithLifecycle()
    val notificationLogs by viewModel.notificationLogs.collectAsStateWithLifecycle()
    val coupons by viewModel.coupons.collectAsStateWithLifecycle()
    val adCampaigns by viewModel.adCampaigns.collectAsStateWithLifecycle()

    val selectedCatId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedDistrict by viewModel.selectedDistrict.collectAsStateWithLifecycle()
    val minRatingFilter by viewModel.minRatingFilter.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isCategoriesLoading by remember { mutableStateOf(true) }
    var showRegisterDialog by remember { mutableStateOf(false) }
    var showAdminDialog by remember { mutableStateOf(false) }
    
    // Admin Credential States
    var adminPasswordInput by remember { mutableStateOf("") }
    var adminUsernameInput by remember { mutableStateOf("") }
    var isAdminAuthenticated by remember { mutableStateOf(false) }
    var adminRole by remember { mutableStateOf("") } // "owner", "global", "supervisor"
    var loggedInSupervisorCatId by remember { mutableStateOf<String?>(null) }
    var loggedInAdminName by remember { mutableStateOf("") }

    // Booking Dialog States
    var showBookingDialog by remember { mutableStateOf(false) }
    var selectedProviderForBooking by remember { mutableStateOf<ProviderEntity?>(null) }

    // Ordinary User Registration Local Storage
    val userSharedPrefs = remember { context.getSharedPreferences("WAM_USER", android.content.Context.MODE_PRIVATE) }
    var isUserRegistered by remember { mutableStateOf(userSharedPrefs.getBoolean("is_registered", false)) }
    var userNameRegistered by remember { mutableStateOf(userSharedPrefs.getString("name", "") ?: "") }
    var userPhoneRegistered by remember { mutableStateOf(userSharedPrefs.getString("phone", "") ?: "") }
    var userDistrictRegistered by remember { mutableStateOf(userSharedPrefs.getString("district", "") ?: "") }
    var showUserSignupDialog by remember { mutableStateOf(false) }

    // Biometric & OTP Backdoor Simulation States
    var showBackdoorDialog by remember { mutableStateOf(false) }
    var backdoorStep by remember { mutableStateOf(1) } // 1: Fingerprint, 2: SMS OTP
    var otpInputCode by remember { mutableStateOf("") }
    var isFingerprintPulsating by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(600)
        isCategoriesLoading = false
    }

    // Filtered providers list
    val filteredProviders = remember(providers, selectedCatId, searchQuery, selectedDistrict, minRatingFilter) {
        providers.filter { provider ->
            val catMatch = selectedCatId == null || provider.mainCategoryId == selectedCatId
            val queryMatch = searchQuery.isEmpty() || 
                    provider.fullName.contains(searchQuery, ignoreCase = true) || 
                    provider.address.contains(searchQuery, ignoreCase = true) ||
                    provider.district.contains(searchQuery, ignoreCase = true)
            val distMatch = selectedDistrict == null || provider.district == selectedDistrict
            val ratingMatch = provider.averageRating >= minRatingFilter
            catMatch && queryMatch && distMatch && ratingMatch
        }
    }

    // Districts of Yemen list
    val yemenDistricts = listOf(
        "أمانة العاصمة", "صنعاء", "عدن", "تعز", "الحديدة", "حضرموت", "إب", "ذمار", "مأرب", "الحوطة"
    )

    val dynamicColorScheme = remember(settings.appThemeColor) {
        when (settings.appThemeColor) {
            "obsidian" -> darkColorScheme(
                primary = Color(0xFFBB86FC),
                secondary = Color(0xFF03DAC6),
                background = Color(0xFF121212),
                surface = Color(0xFF1E1E1E),
                primaryContainer = Color(0xFF3700B3),
                onPrimaryContainer = Color(0xFFFFFFFF),
                surfaceVariant = Color(0xFF2C2C2C)
            )
            "emerald" -> lightColorScheme(
                primary = Color(0xFF007A5E),
                secondary = Color(0xFF8B5A2B),
                background = Color(0xFFF4FAF6),
                surface = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFD3EFE5),
                onPrimaryContainer = Color(0xFF003728),
                surfaceVariant = Color(0xFFE5EDE9)
            )
            "ocean" -> lightColorScheme(
                primary = Color(0xFF0288D1),
                secondary = Color(0xFF0097A7),
                background = Color(0xFFF0F7FA),
                surface = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFE1F5FE),
                onPrimaryContainer = Color(0xFF01579B),
                surfaceVariant = Color(0xFFE0F2F1)
            )
            else -> lightColorScheme(
                primary = Color(0xFF1E3A8A),
                secondary = Color(0xFFD97706),
                background = Color(0xFFF8FAFC),
                surface = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFEFF6FF),
                onPrimaryContainer = Color(0xFF1E40AF),
                surfaceVariant = Color(0xFFF1F5F9)
            )
        }
    }

    LaunchedEffect(settings.isMandatoryRegistration, isUserRegistered) {
        if (settings.isMandatoryRegistration && !isUserRegistered) {
            showUserSignupDialog = true
        }
    }

    MaterialTheme(colorScheme = dynamicColorScheme) {
        Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (language == "ar") "دليل خدمات يمن" else "WAM Yemen Directory",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (language == "ar") "تزامن فوري ممتد عبر السحاب" else "Instant Cloud Realtime Synchronization",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                },
                actions = {
                    // Language Switcher Toggle
                    IconButton(onClick = { viewModel.toggleLanguage() }) {
                        Text(text = if (language == "ar") "EN" else "عربي", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    }
                    // Admin Control workspace trigger
                    IconButton(onClick = { showAdminDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = "Admin Area",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showRegisterDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = "Join") },
                text = { Text(if (language == "ar") "انضم كمهني + " else "Join as Professional") },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.testTag("fab_register")
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Welcome Marquee Message Widget
            WelcomeMarqueeBanner(language = language, activeCount = providers.size)

            // Main Scrollable Client Panel
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Filters & Search section
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        placeholder = { Text(if (language == "ar") "🔍 ابحث بالتفصيل (اسم، مهنة، عنوان، أو رقم)..." else "Search name, profession, or phone...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_field"),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    )
                }

                // WAM 2026 Premium Instant Booking Launcher Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onNavigateToBooking(null)
                            },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🇾🇪", fontSize = 28.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (language == "ar") "طلب صيانة وحجز خدمة فورية" else "Request Maintenance & Instant Booking",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = if (language == "ar") "تثبيت معتمد وتوزيع آلي لموفري الخدمات" else "Vetted intelligent delivery to matching technicians",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            Button(
                                onClick = {
                                    onNavigateToBooking(null)
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(if (language == "ar") "اطلب الآن" else "Book", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }
                }

                // Dynamic District Selector Horizontal Filter Line
                item {
                    Column {
                        Text(
                            text = if (language == "ar") "تصفية حسب المحافظة / المدينة:" else "Filter by City / Province:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedDistrict == null,
                                    onClick = { viewModel.selectDistrict(null) },
                                    label = { Text(if (language == "ar") "كل المحافظات 🌍" else "All Cities") }
                                )
                            }
                            items(yemenDistricts) { dist ->
                                FilterChip(
                                    selected = selectedDistrict == dist,
                                    onClick = { viewModel.selectDistrict(dist) },
                                    label = { Text(dist) }
                                )
                            }
                        }
                    }
                }

                // Minimum Star Ratings Row selection
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (language == "ar") "الحد الأدنى للتقييم:" else "Minimum Star Rating:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            (0..5).forEach { star ->
                                val selected = minRatingFilter == star
                                FilterChip(
                                    selected = selected,
                                    onClick = { viewModel.setMinRating(star) },
                                    label = { Text(if (star == 0) (if (language == "ar") "الجميع" else "Any") else "$star ⭐") }
                                )
                            }
                        }
                    }
                }

                // Categories Quick Grid Cards (No Room Local storage, fully Firebase Live)
                item {
                    Text(
                        text = if (language == "ar") "نطاقات وأقسام دليل الخدمات" else "Yemen Services Category Grid",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                    )
                }

                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            Card(
                                modifier = Modifier
                                    .width(105.dp)
                                    .height(115.dp)
                                    .clickable { viewModel.selectCategory(null) },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedCatId == null) MaterialTheme.colorScheme.primaryContainer 
                                                     else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                border = BorderStroke(
                                    width = if (selectedCatId == null) 2.dp else 1.dp,
                                    color = if (selectedCatId == null) MaterialTheme.colorScheme.primary 
                                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(8.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (selectedCatId == null) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                                else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("🌐", fontSize = 22.sp)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (language == "ar") "الجميع" else "All",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (selectedCatId == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        if (isCategoriesLoading) {
                            items(5) {
                                SkeletonCategoryCard()
                            }
                        } else {
                            val activeSortedCats = categories.filter { it.isEnabled }.sortedBy { it.displayOrder }
                            items(activeSortedCats) { cat ->
                                CategoryGridCard(
                                    category = cat,
                                    isSelected = selectedCatId == cat.id,
                                    language = language,
                                    onClick = { viewModel.selectCategory(cat.id) }
                                )
                            }
                        }
                    }
                }

                // Interactive Live Map Section (Only render if enabled by Admin)
                if (settings.isMapEnabled) {
                    item {
                        Text(
                            text = if (language == "ar") "🌍 الخارطة التفاعلية الفورية للجمهورية اليمنية" else "🌍 Realtime Interactive Map of Yemen",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                        )
                    }
                    item {
                        InteractiveVectorYemenMap(
                            providers = filteredProviders,
                            language = language,
                            onProviderClick = { p ->
                                viewModel.onSearchQueryChanged(p.fullName)
                                Toast.makeText(context, if (language == "ar") "تم تحديد: ${p.fullName}" else "Provider Selected: ${p.fullName}", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }

                // Header for Providers List
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (language == "ar") "📋 مزودي الخدمات المتوفرين (${filteredProviders.size})" else "📋 Available Service Providers (${filteredProviders.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudSync, contentDescription = "Sync", modifier = Modifier.size(16.dp).rotate(30f), tint = Color(0xFF00A884))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (language == "ar") "مزامنة لحظية" else "Cloud Synced",
                                fontSize = 10.sp,
                                color = Color(0xFF00A884)
                            )
                        }
                    }
                }

                // Actual providers vertical listing
                if (filteredProviders.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(24.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.SearchOff, contentDescription = "No items", modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (language == "ar") "لا توجد نتائج مطابقة لبحثك في الخارطة الحالية!" else "No service providers found matching your filter!",
                                    textAlign = TextAlign.Center,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (language == "ar") "جرب تغيير فلتر المحافظة أو كتابة كلمة أخرى" else "Try changing the city filter or write different words",
                                    textAlign = TextAlign.Center,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                } else {
                    // Sorting recommended/VIP first index list
                    val sortedProviders = filteredProviders.sortedWith(compareByDescending<ProviderEntity> { it.isRecommended }.thenByDescending { it.averageRating })
                    items(sortedProviders) { provider ->
                        ProviderCard(
                            provider = provider,
                            categories = categories,
                            reviews = reviews.filter { it.providerId == provider.id },
                            language = language,
                            onReviewClick = {
                                // Handled in individual card dialog
                            },
                            onReportClick = { reason ->
                                viewModel.postReport(provider.id, reason)
                                Toast.makeText(context, if (language == "ar") "تم إرسال بلاغك وسيتم المراجعة فوراً!" else "Flag registered, support team will check!", Toast.LENGTH_SHORT).show()
                            },
                            onPostReviewSubmit = { name, rating, comment ->
                                viewModel.postReview(provider.id, name, rating, comment)
                                Toast.makeText(context, if (language == "ar") "شكراً لمشاركتك التقييم اللحظي!" else "Review synced successfully!", Toast.LENGTH_SHORT).show()
                            },
                            onBookClick = {
                                onNavigateToBooking(provider.id)
                            }
                        )
                    }
                }

                // Bottom safe-padding spacer
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    // --- DIALOGS & MODALS WRAPPERS ---

    // 1. Join / Register Professional Dialog
    if (showRegisterDialog) {
        Dialog(onDismissRequest = { showRegisterDialog = false }) {
            var regName by remember { mutableStateOf("") }
            var regPhone by remember { mutableStateOf("") }
            var regSelectedCat by remember { mutableStateOf<CategoryEntity?>(categories.firstOrNull()) }
            var regDistrict by remember { mutableStateOf("أمانة العاصمة") }
            var regAddress by remember { mutableStateOf("") }
            var regLat by remember { mutableStateOf("15.3186") }
            var regLng by remember { mutableStateOf("44.2045") }
            var catMenuExpanded by remember { mutableStateOf(false) }
            var distMenuExpanded by remember { mutableStateOf(false) }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .fillMaxHeight(0.85f),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (language == "ar") "📝 نموذج تسجيل مهني جديد" else "📝 Professional Registration Form",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { showRegisterDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (language == "ar") "سيظهر طلبك في لوحة التحكم الخاصة بالمشرفين للمصادقة وتأكيد الحماية ثم نشره فوراً." 
                               else "Your request will be sent to the administrator review queue first before going live globally.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = regName,
                        onValueChange = { regName = it },
                        label = { Text(if (language == "ar") "الاسم الكامل الثنائي أو الثلاثي" else "Full Name (First & Last)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = regPhone,
                        onValueChange = { regPhone = it },
                        label = { Text(if (language == "ar") "رقم الهاتف / الواتساب" else "Phone / WhatsApp Number") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Category Selector Box
                    Text(if (language == "ar") "المهنة أو القسم الأساسي:" else "Primary Profession Category:", fontSize = 11.sp)
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Button(
                            onClick = { catMenuExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                        ) {
                            Text(regSelectedCat?.let { if (language == "ar") it.nameAr else it.nameEn } ?: (if (language == "ar") "اختر مهنة" else "Choose Category"))
                        }
                        DropdownMenu(expanded = catMenuExpanded, onDismissRequest = { catMenuExpanded = false }) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text("${cat.imageUrl ?: "🔧"} ${if (language == "ar") cat.nameAr else cat.nameEn}") },
                                    onClick = {
                                        regSelectedCat = cat
                                        catMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // District Selector Box
                    Text(if (language == "ar") "المحافظة الأساسية:" else "Primary Yemen City/District:", fontSize = 11.sp)
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Button(
                            onClick = { distMenuExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                        ) {
                            Text(regDistrict)
                        }
                        DropdownMenu(expanded = distMenuExpanded, onDismissRequest = { distMenuExpanded = false }) {
                            yemenDistricts.forEach { dist ->
                                DropdownMenuItem(
                                    text = { Text(dist) },
                                    onClick = {
                                        regDistrict = dist
                                        distMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = regAddress,
                        onValueChange = { regAddress = it },
                        label = { Text(if (language == "ar") "تفاصيل العنوان الكامل (الشارع، الحارة، أقرب معلم)" else "Detailed Address Context (Street, landmarks)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // GPS Coordinates Setup
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = regLat,
                            onValueChange = { regLat = it },
                            label = { Text("Latitude") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = regLng,
                            onValueChange = { regLng = it },
                            label = { Text("Longitude") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (language == "ar") "ملاحظة: يمكنك إدخال إحداثيات موقعك لتظهر بدقة في الخارطة التفاعلية." else "GPS coordinates allow customers to view your workplace location in Yemen's interactive vector canvas.",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (regName.isNotEmpty() && regPhone.isNotEmpty() && regAddress.isNotEmpty() && regSelectedCat != null) {
                                viewModel.registerProfessional(
                                    fullName = regName,
                                    phone = regPhone,
                                    catId = regSelectedCat!!.id,
                                    district = regDistrict,
                                    address = regAddress,
                                    lat = regLat.toDoubleOrNull() ?: 15.3186,
                                    lng = regLng.toDoubleOrNull() ?: 44.2045
                                )
                                showRegisterDialog = false
                                Toast.makeText(context, if (language == "ar") "تم تقديم طلبك بنجاح وسيتزامن فوراً مع المشرفين!" else "Registration request sent successfully!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, if (language == "ar") "فضلاً تعبئة جميع الخانات الإلزامية!" else "Please fit all fields correctly!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (language == "ar") "إرسال الطلب للمصادقة اللحظية 🚀" else "Register & Submit Now")
                    }
                }
            }
        }
    }

    // 2. Admin Workspace Authenticator Dashboard dialog
    if (showAdminDialog) {
        Dialog(onDismissRequest = { 
            showAdminDialog = false
            isAdminAuthenticated = false
            adminPasswordInput = ""
        }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
                    .fillMaxHeight(0.9f),
                shape = RoundedCornerShape(24.dp)
            ) {
                if (!isAdminAuthenticated) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = "Security", modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(if (language == "ar") "لوحة تحكم مشرفي النظام" else "Administrator Portal Access", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(if (language == "ar") "يتطلب إذناً رقمياً مصدقاً لتغيير البينات الحية" else "Secured environment with realtime Firestore write access", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = adminPasswordInput,
                            onValueChange = { adminPasswordInput = it },
                            label = { Text(if (language == "ar") "رمز المرور (الإفتراضي: 123)" else "Passcode (Default: 123)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(20.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { showAdminDialog = false }) {
                                Text(if (language == "ar") "تراجع" else "Cancel")
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Button(onClick = {
                                if (adminPasswordInput == "123") {
                                    isAdminAuthenticated = true
                                } else {
                                    Toast.makeText(context, if (language == "ar") "الرمز غير صحيح!" else "Access Denied: Incorrect PIN!", Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Text(if (language == "ar") "دخول" else "Authenticate")
                            }
                        }
                    }
                } else {
                    // Authenticated Admin Tabs Layout
                    AdminDashboardPanel(
                        viewModel = viewModel,
                        categories = categories,
                        providers = providers,
                        registrations = registrations,
                        reports = reports,
                        settings = settings,
                        language = language,
                        onDismiss = { 
                            showAdminDialog = false 
                            isAdminAuthenticated = false
                            adminPasswordInput = ""
                        }
                    )
                }
            }
        }
    }
    }
}

// --- Dynamic welcome horizontal news banner ---
@Composable
fun WelcomeMarqueeBanner(language: String, activeCount: Int) {
    val marqueeTransition = rememberInfiniteTransition(label = "WelcomeMarquee")
    val translationX by marqueeTransition.animateFloat(
        initialValue = 400f,
        targetValue = -400f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "translation"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                    )
                )
            )
            .padding(vertical = 5.dp)
            .height(22.dp)
    ) {
        Text(
            text = if (language == "ar") "أهلاً بك في دليل يمن اللحظي الموثق ✔️ حالياً لدينا: $activeCount كفائة مهنية نشطة وخاضعة للحماية والسيطرة الفنية الكاملة" 
                   else "Welcome to WAM Yemen Directory ✔️ Active live count: $activeCount vetted professionals ready and synced globally",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
                .fillMaxWidth()
                .offset(x = translationX.dp),
            maxLines = 1,
            overflow = TextOverflow.Visible
        )
    }
}

// --- Skeleton components for lazy row load state ---
@Composable
fun SkeletonCategoryCard() {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EasyInExactlyEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Card(
        modifier = Modifier
            .width(105.dp)
            .height(115.dp)
            .padding(2.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
    ) {
        Box(modifier = Modifier.fillMaxSize())
    }
}

val EasyInExactlyEasing = Easing { fraction ->
    fraction * fraction
}

// --- Dynamic Category item card helper ---
@Composable
fun CategoryGridCard(
    category: CategoryEntity,
    isSelected: Boolean,
    language: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(115.dp)
            .height(115.dp)
            .clickable { onClick() }
            .testTag("category_card_${category.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                             else MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary 
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(category.imageUrl ?: "🔧", fontSize = 24.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (language == "ar") category.nameAr else category.nameEn,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// --- Interactive beautiful Custom vector map of Yemen for spatial context ---
@Composable
fun InteractiveVectorYemenMap(
    providers: List<ProviderEntity>,
    language: String,
    onProviderClick: (ProviderEntity) -> Unit
) {
    val context = LocalContext.current
    
    // City locations coordinates scale mapping for rendering yemen spatial coordinates
    // Yemen geographic boundaries approximately: Lat 12 to 19, Lng 42 to 54
    val cityAnchors = listOf(
        AnchorPoint("صنعاء / Sana'a", 15.3694, 44.1910, Color(0xFF00A884)),
        AnchorPoint("عدن / Aden", 12.7855, 45.0186, Color(0xFFE91E63)),
        AnchorPoint("تعز / Taiz", 13.5795, 44.0206, Color(0xFF3F51B5)),
        AnchorPoint("الحديدة / Hodeida", 14.8024, 42.9482, Color(0xFFFF9800)),
        AnchorPoint("المكلا / Mukalla", 14.5424, 49.1242, Color(0xFF9C27B0)),
        AnchorPoint("إب / Ibb", 13.9714, 44.1802, Color(0xFF4CAF50)),
        AnchorPoint("مأرب / Marib", 15.4633, 45.3253, Color(0xFFFF5722))
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = maxWidth
            val canvasHeight = maxHeight
            val density = LocalDensity.current.density

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(providers) {
                        detectDragGestures { change, dragAmount ->
                            // Optional dragging simulation
                        }
                    }
            ) {
                val w = size.width
                val h = size.height

                // Drawing abstract borders representing the Gulf of Aden & Yemen shape contour
                val outlinePath = Path().apply {
                    moveTo(w * 0.15f, h * 0.4f)
                    quadraticBezierTo(w * 0.3f, h * 0.2f, w * 0.5f, h * 0.25f)
                    quadraticBezierTo(w * 0.7f, h * 0.15f, w * 0.9f, h * 0.35f)
                    quadraticBezierTo(w * 0.95f, h * 0.6f, w * 0.8f, h * 0.75f)
                    quadraticBezierTo(w * 0.6f, h * 0.65f, w * 0.4f, h * 0.8f)
                    quadraticBezierTo(w * 0.2f, h * 0.7f, w * 0.15f, h * 0.4f)
                    close()
                }

                // Map styling background filling
                drawPath(
                    path = outlinePath,
                    color = Color.DarkGray.copy(alpha = 0.08f),
                    style = strokeOrFill(false)
                )
                drawPath(
                    path = outlinePath,
                    color = Color.LightGray.copy(alpha = 0.25f),
                    style = Stroke(width = 4f)
                )

                // Gulf of Aden lines representation
                drawLine(
                    color = Color(0xFF03A9F4).copy(alpha = 0.2f),
                    start = Offset(w * 0.1f, h * 0.85f),
                    end = Offset(w * 0.9f, h * 0.88f),
                    strokeWidth = 3f
                )
            }

            // Draw major City Anchor text elements and dynamic provider counts overlay overlay
            cityAnchors.forEach { anchor ->
                val xPercent = (anchor.lng - 42.0) / (52.0 - 42.0)
                val yPercent = 1.0 - (anchor.lat - 12.0) / (18.0 - 12.0)
                
                val xPos = (xPercent * canvasWidth.value).dp
                val yPos = (yPercent * canvasHeight.value).dp

                // Real-time calculation of provider count in this spatial zone
                val localCount = providers.filter { 
                    val distance = sqrt((it.locationLat - anchor.lat) * (it.locationLat - anchor.lat) + (it.locationLng - anchor.lng) * (it.locationLng - anchor.lng))
                    distance < 1.0
                }.size

                Box(
                    modifier = Modifier
                        .offset(x = xPos - 10.dp, y = yPos - 10.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(anchor.color.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = localCount.toString(), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }

                Text(
                    text = anchor.cityName.substringBefore(" /"),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    modifier = Modifier.offset(x = xPos + 12.dp, y = yPos - 6.dp)
                )
            }

            // Draw active providers as clickable dots
            providers.forEach { provider ->
                val xPercent = (provider.locationLng - 42.0) / (52.0 - 42.0)
                val yPercent = 1.0 - (provider.locationLat - 12.0) / (18.0 - 12.0)
                val xPos = (xPercent * canvasWidth.value).dp
                val yPos = (yPercent * canvasHeight.value).dp

                Box(
                    modifier = Modifier
                        .offset(x = xPos - 8.dp, y = yPos - 8.dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            if (provider.isRecommended) Color(0xFFFFD700) else Color(0xFF00E676)
                        )
                        .border(
                            1.dp,
                            if (provider.isVerified) Color.White else Color.Transparent,
                            CircleShape
                        )
                        .clickable { onProviderClick(provider) },
                    contentAlignment = Alignment.Center
                ) {
                    if (provider.isRecommended) {
                        Text("⭐", fontSize = 8.sp)
                    } else {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                }
            }

            // Overlay Helper Guide
            Card(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
            ) {
                Row(
                    modifier = Modifier.padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFFFD700)))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(if (language == "ar") "VIP متميز" else "VIP Stars", fontSize = 9.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF00E676)))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(if (language == "ar") "نشط حالياً" else "Active", fontSize = 9.sp)
                    }
                }
            }
        }
    }
}

class AnchorPoint(val cityName: String, val lat: Double, val lng: Double, val color: Color)

private fun strokeOrFill(isFill: Boolean): androidx.compose.ui.graphics.drawscope.DrawStyle {
    return if (isFill) androidx.compose.ui.graphics.drawscope.Fill else Stroke(width = 1f)
}

// --- Provider Vetted Presentation item card ---
@Composable
fun ProviderCard(
    provider: ProviderEntity,
    categories: List<CategoryEntity>,
    reviews: List<ReviewEntity>,
    language: String,
    onReviewClick: () -> Unit,
    onReportClick: (String) -> Unit,
    onPostReviewSubmit: (String, Int, String) -> Unit,
    onBookClick: () -> Unit
) {
    val context = LocalContext.current
    var showReviewDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }

    // Resolve Category Name
    val resolvedCat = categories.find { it.id == provider.mainCategoryId }
    val categoryName = resolvedCat?.let { if (language == "ar") it.nameAr else it.nameEn } ?: (if (language == "ar") "خدمات عامة" else "General Services")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("provider_card_${provider.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (provider.isRecommended) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                             else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (provider.isRecommended) 2.dp else 1.dp,
            color = if (provider.isRecommended) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: VIP Sparkle Badging / Verification
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = provider.fullName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    if (provider.isVerified) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF00A884).copy(alpha = 0.15f),
                            modifier = Modifier.size(18.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Check, contentDescription = "Verified Vetted", tint = Color(0xFF00A884), modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }

                if (provider.isRecommended) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text("VIP ⭐", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800)) },
                        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Color(0xFFFF9800).copy(alpha = 0.15f))
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Contact Phone + Category Badges Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = {},
                    label = { Text(categoryName, fontSize = 10.sp, fontWeight = FontWeight.Medium) },
                    leadingIcon = { Text(resolvedCat?.imageUrl ?: "🔧") }
                )

                AssistChip(
                    onClick = {},
                    label = { Text(provider.district, fontSize = 10.sp) },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = "Location", modifier = Modifier.size(12.dp)) }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Address Details Text
            Text(
                text = "${if (language == "ar") "العنوان التفصيلي:" else "Detailed Location:"} ${provider.address}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Star Ratings & Interactive Review list Summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${provider.averageRating} ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFFFFB300)
                    )
                    Row {
                        (1..5).forEach { star ->
                            Icon(
                                imageVector = if (star <= provider.averageRating) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Star $star",
                                modifier = Modifier.size(14.dp),
                                tint = Color(0xFFFFB300)
                            )
                        }
                    }
                    Text(
                        text = " (${provider.ratingCount})",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                // Interactive Contact Dial Buttons (Direct WhatsApp or direct Phone dial)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${provider.phone}"))
                            context.startActivity(intent)
                        },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = "Call", modifier = Modifier.size(16.dp))
                    }

                    IconButton(
                        onClick = {
                            val encodedMsg = Uri.encode(if (language == "ar") "السلام عليكم، هل أنت متاح لتقديم خدمة؟" else "Hello, are you available for a client task?")
                            val whatsappUrl = "https://api.whatsapp.com/send?phone=${provider.phone}&text=$encodedMsg"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(whatsappUrl))
                            context.startActivity(intent)
                        },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFF25D366).copy(alpha = 0.15f))
                    ) {
                        Text("💬", fontSize = 16.sp)
                    }
                }
            }

            // Divider and Action Buttons (Add Review, Report)
            Divider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { showReviewDialog = true }) {
                    Icon(Icons.Default.RateReview, contentDescription = "Add Review", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (language == "ar") "اكتب تقييماً" else "Write dynamic review", fontSize = 11.sp)
                }

                Button(
                    onClick = { onBookClick() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = "Schedule Service", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (language == "ar") "احجز الخدمة" else "Book Now", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                }

                TextButton(
                    onClick = { showReportDialog = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = "Report", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (language == "ar") "أبلغ عن مخالفة" else "Flag Listing", fontSize = 11.sp)
                }
            }

            // Reviews presentation fold if reviews are loaded live from Firestore
            if (reviews.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = if (language == "ar") "آخر التعليقات والتقييمات اللحظية:" else "Latest Live Comments:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        reviews.take(2).forEach { rev ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${rev.reviewerName}: ${rev.comment}",
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(text = "${rev.rating}⭐", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFB300))
                            }
                        }
                    }
                }
            }
        }
    }

    // Interactive review writing popup dialog
    if (showReviewDialog) {
        Dialog(onDismissRequest = { showReviewDialog = false }) {
            var reviewerName by remember { mutableStateOf("") }
            var reviewerComment by remember { mutableStateOf("") }
            var starSelect by remember { mutableStateOf(5) }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (language == "ar") "✏️ تقييم: ${provider.fullName}" else "✏️ Review: ${provider.fullName}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = reviewerName,
                        onValueChange = { reviewerName = it },
                        label = { Text(if (language == "ar") "اسمك الكريم" else "Your Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = reviewerComment,
                        onValueChange = { reviewerComment = it },
                        label = { Text(if (language == "ar") "تعليقك أو انطباعك عن الخدمة المقدمة" else "Your Comment") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(if (language == "ar") "اختر النجوم:" else "Select Rating:", fontSize = 11.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        (1..5).forEach { star ->
                            IconButton(onClick = { starSelect = star }) {
                                Icon(
                                    imageVector = if (star <= starSelect) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "$star Stars",
                                    tint = Color(0xFFFFB300),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showReviewDialog = false }) {
                            Text(if (language == "ar") "إلغاء" else "Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            if (reviewerName.isNotEmpty() && reviewerComment.isNotEmpty()) {
                                onPostReviewSubmit(reviewerName, starSelect, reviewerComment)
                                showReviewDialog = false
                            } else {
                                Toast.makeText(context, "الرجاء كتابة اسمك والتعليق!", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Text(if (language == "ar") "إرسال" else "Post Live")
                        }
                    }
                }
            }
        }
    }

    // Interactive Report Listing flagger popup
    if (showReportDialog) {
        Dialog(onDismissRequest = { showReportDialog = false }) {
            var rReason by remember { mutableStateOf("") }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (language == "ar") "⚠️ إبلاغ عن مخالفة لأخلاقيات العمل" else "⚠️ Report Professional Listing",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = rReason,
                        onValueChange = { rReason = it },
                        label = { Text(if (language == "ar") "ما هو سبب الإبلاغ بالتفصيل؟ (مثال: هاتف خاطئ، معاملة غير لائقة)" else "Why are you flagging this listing?") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showReportDialog = false }) {
                            Text(if (language == "ar") "تراجع" else "Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (rReason.isNotEmpty()) {
                                    onReportClick(rReason)
                                    showReportDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text(if (language == "ar") "موافق وإرسال" else "Submit Flag")
                        }
                    }
                }
            }
        }
    }
}

// --- Authenticated multi-tab view workspace panel ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardPanel(
    viewModel: AppViewModel,
    categories: List<CategoryEntity>,
    providers: List<ProviderEntity>,
    registrations: List<RegistrationEntity>,
    reports: List<ReportEntity>,
    settings: AppSettingEntity,
    language: String,
    onDismiss: () -> Unit
) {
    var adminTabSelection by remember { mutableStateOf(0) }
    val tabs = listOf(
        if (language == "ar") "الإعدادات ⚙️" else "Config",
        if (language == "ar") "النشطين 👥" else "Active",
        if (language == "ar") "الطلبات 📝" else "Requests",
        if (language == "ar") "الأقسام 🔨" else "Categories",
        if (language == "ar") "البلاغات ⚠️" else "Flags",
        if (language == "ar") "القبول 🔧" else "Approvals",
        if (language == "ar") "التقارير 📊" else "Reports"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = adminTabSelection) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = adminTabSelection == index,
                    onClick = { adminTabSelection = index },
                    text = { Text(title, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp)
        ) {
            when (adminTabSelection) {
                0 -> {
                    // TAB 1: Global configurations editing
                    var supportP by remember { mutableStateOf(settings.supportPhone) }
                    var supportE by remember { mutableStateOf(settings.supportEmail) }
                    var supportW by remember { mutableStateOf(settings.supportWhatsApp) }
                    var isMActive by remember { mutableStateOf(settings.isMapEnabled) }
                    var radiusSearch by remember { mutableStateOf(settings.radiusSearchLimit.toString()) }
                    var vEnabled by remember { mutableStateOf(settings.voiceSearchEnabled) }

                    val scrollableState = rememberScrollState()

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollableState),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(if (language == "ar") "🛠️ تحكم بالخصائص الكونية للدليل" else "🛠️ Adjust Service Globals", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        
                        OutlinedTextField(value = supportP, onValueChange = { supportP = it }, label = { Text("Support Phone") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = supportE, onValueChange = { supportE = it }, label = { Text("Support Email") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = supportW, onValueChange = { supportW = it }, label = { Text("WhatsApp Contact Url") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = radiusSearch, onValueChange = { radiusSearch = it }, label = { Text("Radius Limit (KM)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (language == "ar") "عرض خارطة اليمن الجغرافية بالتطبيق:" else "Enable Yemen geographic Map widget:", fontSize = 11.sp)
                            Switch(checked = isMActive, onCheckedChange = { isMActive = it })
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (language == "ar") "البحث الصوتي الذكي باللغة العربية:" else "Global Voice search capability:", fontSize = 11.sp)
                            Switch(checked = vEnabled, onCheckedChange = { vEnabled = it })
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                viewModel.updateAppSettings(
                                    AppSettingEntity(
                                        supportPhone = supportP,
                                        supportEmail = supportE,
                                        supportWhatsApp = supportW,
                                        radiusSearchLimit = radiusSearch.toIntOrNull() ?: 20,
                                        isMapEnabled = isMActive,
                                        voiceSearchEnabled = vEnabled
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (language == "ar") "حفظ إعدادات النظام" else "Commit Global Config")
                        }
                    }
                }
                1 -> {
                    // TAB 2: Manage Active Vetted service providers list
                    var editingActiveProvider by remember { mutableStateOf<ProviderEntity?>(null) }

                    Column(modifier = Modifier.fillMaxSize()) {
                        Text(if (language == "ar") "👥 إدارة وتعديل مزودي الخدمات النشطين" else "👥 Admin Vetted Professional Directory", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                            items(providers) { provider ->
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(provider.fullName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                IconButton(onClick = { editingActiveProvider = provider }) {
                                                    Icon(Icons.Default.Edit, contentDescription = "Edit details", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                                }
                                                IconButton(onClick = { viewModel.deleteProviderActive(provider.id) }) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                                }
                                            }
                                        }
                                        Text("الهاتف: ${provider.phone} | المنطقة: ${provider.district}", fontSize = 11.sp)
                                        Text("العنوان: ${provider.address}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        // Feature Controls: Verified, VIP star badges, subscribed tier
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Checkbox(
                                                    checked = provider.isVerified,
                                                    onCheckedChange = { viewModel.updateFeatures(provider.id, it, provider.isPinned, provider.isRecommended, provider.isSubscribed) }
                                                )
                                                Text("موثق ✔", fontSize = 9.sp)
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Checkbox(
                                                    checked = provider.isRecommended,
                                                    onCheckedChange = { viewModel.updateFeatures(provider.id, provider.isVerified, provider.isPinned, it, provider.isSubscribed) }
                                                )
                                                Text("VIP ⭐", fontSize = 9.sp)
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Checkbox(
                                                    checked = provider.isSubscribed,
                                                    onCheckedChange = { viewModel.updateFeatures(provider.id, provider.isVerified, provider.isPinned, provider.isRecommended, it) }
                                                )
                                                Text("مشترك 💰", fontSize = 9.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Active Provider Edit dialog nested
                    if (editingActiveProvider != null) {
                        val p = editingActiveProvider!!
                        var editName by remember { mutableStateOf(p.fullName) }
                        var editPhone by remember { mutableStateOf(p.phone) }
                        var editDist by remember { mutableStateOf(p.district) }
                        var editAddr by remember { mutableStateOf(p.address) }
                        var editLat by remember { mutableStateOf(p.locationLat.toString()) }
                        var editLng by remember { mutableStateOf(p.locationLng.toString()) }

                        Dialog(onDismissRequest = { editingActiveProvider = null }) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(0.8f)
                                    .padding(8.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    Text("✏️ تعديل تفاصيل كفاءة المهني النشط", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("الاسم") }, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = editPhone, onValueChange = { editPhone = it }, label = { Text("الهاتف") }, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = editDist, onValueChange = { editDist = it }, label = { Text("المحافظة") }, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = editAddr, onValueChange = { editAddr = it }, label = { Text("العنوان بالتفصيل") }, modifier = Modifier.fillMaxWidth())
                                    
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedTextField(value = editLat, onValueChange = { editLat = it }, label = { Text("Lat") }, modifier = Modifier.weight(1f))
                                        OutlinedTextField(value = editLng, onValueChange = { editLng = it }, label = { Text("Lng") }, modifier = Modifier.weight(1f))
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                        TextButton(onClick = { editingActiveProvider = null }) {
                                            Text("إلغاء")
                                        }
                                        Button(onClick = {
                                            if (editName.isNotEmpty() && editPhone.isNotEmpty()) {
                                                viewModel.editProviderDetails(
                                                    p.copy(
                                                        fullName = editName,
                                                        phone = editPhone,
                                                        district = editDist,
                                                        address = editAddr,
                                                        locationLat = editLat.toDoubleOrNull() ?: p.locationLat,
                                                        locationLng = editLng.toDoubleOrNull() ?: p.locationLng
                                                    )
                                                )
                                                editingActiveProvider = null
                                            }
                                        }) {
                                            Text("تأكيد وحفظ")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // TAB 3: Outstanding professional registration admission review queue
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text(if (language == "ar") "📝 طلبات التسجيل المعلقة بانتظار الموافقة" else "📝 Outgoing Admissions Review Queue", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        if (registrations.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(if (language == "ar") "لا توجد طلبات جديدة بانتظار المصادقة!" else "Admissions Queue is empty!", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(registrations) { reg ->
                                    Card(modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(reg.fullName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text("الهاتف: ${reg.phone} | المنطقة: ${reg.district}", fontSize = 12.sp)
                                            Text("العنوان: ${reg.address}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                            Spacer(modifier = Modifier.height(10.dp))

                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                                TextButton(
                                                    onClick = { viewModel.deleteRegistration(reg.id) },
                                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                                ) {
                                                    Text(if (language == "ar") "رفض وحذف" else "Decline")
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Button(
                                                    onClick = { viewModel.approveRegistration(reg) }
                                                ) {
                                                    Text(if (language == "ar") "موافقة ونشر فوري ✔" else "Approve & Publish")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                3 -> {
                    // TAB 4: Categories Manager Configuration CRUD (No Room write, direct Firestore write)
                    var editingCatState by remember { mutableStateOf<CategoryEntity?>(null) }
                    var addCatAr by remember { mutableStateOf("") }
                    var addCatEn by remember { mutableStateOf("") }
                    var addCatIcon by remember { mutableStateOf("🔧") }

                    LaunchedEffect(editingCatState) {
                        if (editingCatState != null) {
                            addCatAr = editingCatState!!.nameAr
                            addCatEn = editingCatState!!.nameEn
                            addCatIcon = editingCatState!!.imageUrl ?: "🔧"
                        } else {
                            addCatAr = ""
                            addCatEn = ""
                            addCatIcon = "🔧"
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (editingCatState != null) "✏️ تعديل كودات القسم المحدد" else "🔨 إضافة قسم مهن جدید",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        OutlinedTextField(value = addCatAr, onValueChange = { addCatAr = it }, label = { Text("الاسم عربي") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = addCatEn, onValueChange = { addCatEn = it }, label = { Text("Name English") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = addCatIcon, onValueChange = { addCatIcon = it }, label = { Text("Emoji Icon") }, modifier = Modifier.fillMaxWidth())

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (editingCatState != null) {
                                Button(
                                    onClick = { editingCatState = null },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                                ) {
                                    Text("تراجع")
                                }
                            }
                            Button(
                                onClick = {
                                    if (addCatAr.isNotEmpty()) {
                                        if (editingCatState != null) {
                                            viewModel.editCategory(
                                                editingCatState!!.copy(
                                                    nameAr = addCatAr,
                                                    nameEn = addCatEn,
                                                    imageUrl = addCatIcon
                                                )
                                            )
                                            editingCatState = null
                                        } else {
                                            viewModel.addCategory(addCatAr, addCatEn, addCatIcon)
                                        }
                                        addCatAr = ""
                                        addCatEn = ""
                                        addCatIcon = "🔧"
                                    }
                                },
                                modifier = Modifier.weight(1.5f)
                            ) {
                                Text(if (editingCatState != null) "حفظ التعديلات" else "إضافة قسم")
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text("📋 الأقسام النشطة الحالية (اضغط للتعديل أو الحذف):", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        
                        categories.forEach { cat ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { editingCatState = cat }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(cat.imageUrl ?: "🔧", fontSize = 20.sp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(cat.nameAr, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text(cat.nameEn, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                        }
                                    }
                                    IconButton(onClick = { viewModel.deleteCategory(cat.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
                4 -> {
                    // TAB 5: Flagged listings list
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text(if (language == "ar") "⚠️ بلاغات كسر القوانين والمخالفات الفنية" else "⚠️ Flagged Listing Reports Queue", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        if (reports.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(if (language == "ar") "نظيفة تماماً، لا توجد بلاغات!" else "Clear! No outstanding flags!", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(reports) { rep ->
                                    val targetP = providers.find { it.id == rep.providerId }
                                    Card(modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text("${if (language == "ar") "المهني المستهدف:" else "Target Listing:"} ${targetP?.fullName ?: "Unknown / Deleted"}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("${if (language == "ar") "سبب الإبلاغ:" else "Report Reason:"} ${rep.reason}", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                            Spacer(modifier = Modifier.height(10.dp))

                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                                TextButton(
                                                    onClick = { 
                                                        // Dismiss report flag
                                                        FirebaseFirestore.getInstance().collection("reports").document(rep.id).delete()
                                                    }
                                                ) {
                                                    Text(if (language == "ar") "تجاهل الشكوى" else "Dismiss Report")
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Button(
                                                    onClick = { 
                                                        // Action deletion block
                                                        if (targetP != null) {
                                                            viewModel.deleteProviderActive(targetP.id)
                                                        }
                                                        FirebaseFirestore.getInstance().collection("reports").document(rep.id).delete()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                                ) {
                                                    Text(if (language == "ar") "حذف وإلغاء المزود ❌" else "Block & Delete Listing")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                5 -> {
                    TechnicianApprovalScreen(viewModel = viewModel)
                }
                6 -> {
                    ReportsScreen(viewModel = viewModel)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Button(onClick = { onDismiss() }) {
                Text(if (language == "ar") "إغلاق نافذة المشرف" else "Exit Panel")
            }
        }
    }
}
