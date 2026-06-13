package com.example.ui.screen

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.ui.viewmodel.AppViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MainDashboard(viewModel: AppViewModel) {
    val context = LocalContext.current
    val settingsState by viewModel.appSettings.collectAsState()
    val scope = rememberCoroutineScope()

    // Fallback theme colors
    val primaryColorString = settingsState?.primaryColor?.ifEmpty { "#ECEFF1" } ?: "#ECEFF1"
    val secondaryColorString = settingsState?.secondaryColor?.ifEmpty { "#37474F" } ?: "#37474F"

    val primaryHexColor = remember(primaryColorString) { parseColorSafely(primaryColorString, Color(0xFFECEFF1)) }
    val secondaryHexColor = remember(secondaryColorString) { parseColorSafely(secondaryColorString, Color(0xFF37474F)) }

    // Parse CSV theme variables
    val csvColors = settingsState?.customColorsCSV ?: ""
    val themePairs = remember(csvColors) {
        val list = mutableListOf<Triple<String, Color, Color>>()
        if (csvColors.isNotEmpty()) {
            csvColors.split(",").forEach { item ->
                val parts = item.split(":")
                if (parts.size == 3) {
                    list.add(Triple(
                        parts[0].trim(),
                        parseColorSafely(parts[1].trim(), Color(0xFFECEFF1)),
                        parseColorSafely(parts[2].trim(), Color(0xFF37474F))
                    ))
                }
            }
        }
        if (list.isEmpty()) {
            list.add(Triple("🌌 كوزميك سيلفر", Color(0xFFECEFF1), Color(0xFF37474F)))
        }
        list
    }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = secondaryHexColor,
            onPrimary = primaryHexColor,
            background = primaryHexColor,
            surface = primaryHexColor,
            primaryContainer = secondaryHexColor.copy(alpha = 0.12f),
            secondaryContainer = primaryHexColor.copy(alpha = 0.2f),
            onBackground = secondaryHexColor,
            onSurface = secondaryHexColor
        )
    ) {
        Scaffold(
            topBar = {
                CustomDashboardTopBar(
                    title = settingsState?.appName ?: "دليل خدمات اليمن",
                    statusColor = secondaryHexColor,
                    contentColor = primaryHexColor,
                    onAdminClick = { viewModel.currentScreen = "admin_panel" },
                    onHomeClick = { viewModel.currentScreen = "home" },
                    isOnline = viewModel.isOnline()
                )
            },
            bottomBar = {
                CustomBottomNavigation(
                    currentScreen = viewModel.currentScreen,
                    themeColor = secondaryHexColor,
                    contentColor = primaryHexColor,
                    onNavigate = { screen ->
                        viewModel.currentScreen = screen
                    }
                )
            },
            floatingActionButton = {
                if (settingsState?.isAiFloating == true && viewModel.currentScreen != "gemini_chat") {
                    val scaleX = settingsState?.aiIconX ?: 0.85f
                    val scaleY = settingsState?.aiIconY ?: 0.85f
                    val size = settingsState?.aiIconSize ?: 48

                    FloatingActionButton(
                        onClick = { viewModel.currentScreen = "gemini_chat" },
                        containerColor = secondaryHexColor,
                        contentColor = primaryHexColor,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(size.dp)
                            .testTag("ai_assistant_fab")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = "المساعد الذكي WAM"
                        )
                    }
                }
            },
            containerColor = primaryHexColor
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(primaryHexColor)
            ) {
                when (viewModel.currentScreen) {
                    "home" -> HomeScreen(viewModel, secondaryHexColor, primaryHexColor)
                    "providers_list" -> ProvidersListScreen(viewModel, secondaryHexColor, primaryHexColor)
                    "provider_details" -> ProviderDetailsScreen(viewModel, secondaryHexColor, primaryHexColor)
                    "register_provider" -> RegisterProviderScreen(viewModel, secondaryHexColor, primaryHexColor)
                    "admin_panel" -> AdminPanelScreen(viewModel, secondaryHexColor, primaryHexColor, themePairs)
                    "gemini_chat" -> GeminiChatScreen(viewModel, secondaryHexColor, primaryHexColor)
                    "user_chat" -> UserChatScreen(viewModel, secondaryHexColor, primaryHexColor)
                }
            }
        }
    }
}

// --- Top bar ---
@Composable
fun CustomDashboardTopBar(
    title: String,
    statusColor: Color,
    contentColor: Color,
    onAdminClick: () -> Unit,
    onHomeClick: () -> Unit,
    isOnline: Boolean
) {
    Surface(
        color = statusColor,
        contentColor = contentColor,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onHomeClick) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "الرئيسية",
                    modifier = Modifier.size(28.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isOnline) Color(0xFF4CAF50) else Color(0xFFF44336))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isOnline) "نشط - متصل" else "غير متصل - محلي",
                        fontSize = 11.sp,
                        color = contentColor.copy(alpha = 0.8f)
                    )
                }
            }

            IconButton(onClick = onAdminClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "لوحة التحكم",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

// --- Dynamic Navigation Bar ---
@Composable
fun CustomBottomNavigation(
    currentScreen: String,
    themeColor: Color,
    contentColor: Color,
    onNavigate: (String) -> Unit
) {
    Surface(
        color = themeColor,
        contentColor = contentColor,
        shadowElevation = 16.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                icon = Icons.Default.Home,
                label = "الرئيسية",
                active = currentScreen == "home",
                activeColor = contentColor,
                inactiveColor = contentColor.copy(alpha = 0.6f),
                onClick = { onNavigate("home") },
                modifier = Modifier.testTag("nav_home")
            )

            BottomNavItem(
                icon = Icons.Default.Person,
                label = "تسجيل فني",
                active = currentScreen == "register_provider",
                activeColor = contentColor,
                inactiveColor = contentColor.copy(alpha = 0.6f),
                onClick = { onNavigate("register_provider") },
                modifier = Modifier.testTag("nav_register")
            )

            BottomNavItem(
                icon = Icons.Default.Face,
                label = "مساعد ذكي",
                active = currentScreen == "gemini_chat",
                activeColor = contentColor,
                inactiveColor = contentColor.copy(alpha = 0.6f),
                onClick = { onNavigate("gemini_chat") },
                modifier = Modifier.testTag("nav_assistant")
            )

            BottomNavItem(
                icon = Icons.Default.Settings,
                label = "الإدارة",
                active = currentScreen == "admin_panel",
                activeColor = contentColor,
                inactiveColor = contentColor.copy(alpha = 0.6f),
                onClick = { onNavigate("admin_panel") },
                modifier = Modifier.testTag("nav_admin")
            )
        }
    }
}

@Composable
fun BottomNavItem(
    icon: ImageVector,
    label: String,
    active: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (active) activeColor else inactiveColor,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = if (active) activeColor else inactiveColor,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// --- HOME SCREEN ---
@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    themeColor: Color,
    bgColor: Color
) {
    val categoriesState by viewModel.categories.collectAsState()
    val recommendedState by viewModel.recommendedProviders.collectAsState()
    val activeBannersState by viewModel.activeBanners.collectAsState()
    val settingsState by viewModel.appSettings.collectAsState()

    var showVoicePromptDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcoming Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = themeColor.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, themeColor.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = settingsState?.welcomeMessage?.ifEmpty { "أهلاً بك في دليل يمني المتميز" } ?: "أهلاً بك في دليل يمني المتميز",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColor,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "ابحث وتواصل لحظياً مع أفضل المهنيين المختصين والمعتمدين محلياً باليمن.",
                        fontSize = 12.sp,
                        color = themeColor.copy(alpha = 0.7f),
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Banners Carousel
        if (activeBannersState.isNotEmpty()) {
            item {
                Text(
                    text = "📢 عروض وتنويهات ممولة حصرية",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = themeColor,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(activeBannersState) { banner ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = themeColor),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .width(280.dp)
                                .height(95.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = banner.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = bgColor,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }

        // Search Bar with Voice Assist
        item {
            OutlinedTextField(
                value = viewModel.searchQuery,
                onValueChange = { viewModel.searchQuery = it },
                label = { Text("ابحث عن فني، اسم الخدمة، أو الجوار...") },
                leadingIcon = {
                    IconButton(onClick = { showVoicePromptDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "البحث الصوتي الفوري WAM"
                        )
                    }
                },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "بحث"
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_field"),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = themeColor,
                    unfocusedBorderColor = themeColor.copy(alpha = 0.5f)
                ),
                singleLine = true
            )
        }

        // Fast Filter Chips list
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = viewModel.filterOnlyVerified,
                    onClick = { viewModel.filterOnlyVerified = !viewModel.filterOnlyVerified },
                    label = { Text("الموثقة الشارة المعتمدة ✔️") }
                )
                FilterChip(
                    selected = viewModel.filterOnlyVip,
                    onClick = { viewModel.filterOnlyVip = !viewModel.filterOnlyVip },
                    label = { Text("رعاة VIP التاج النادر 👑") }
                )
                FilterChip(
                    selected = viewModel.filterDistrict.isNotEmpty(),
                    onClick = {
                        viewModel.filterDistrict = if (viewModel.filterDistrict.isEmpty()) "حدة" else ""
                    },
                    label = { Text("منطقة حدة 📍") }
                )
            }
        }

        // Categories Browser
        item {
            Text(
                text = "🏛️ تصفح الأقسام والخدمات التخصصية",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = themeColor,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Right
            )
        }

        if (categoriesState.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("جاري مزامنة الأقسام أو إعادة تعيينها...", color = themeColor.copy(alpha = 0.6f))
                }
            }
        } else {
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(categoriesState) { cat ->
                        Card(
                            onClick = {
                                viewModel.selectedCategory = cat
                                viewModel.currentScreen = "providers_list"
                            },
                            colors = CardDefaults.cardColors(containerColor = themeColor.copy(alpha = 0.05f)),
                            border = BorderStroke(1.dp, themeColor.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .width(115.dp)
                                .height(125.dp)
                                .testTag("cat_card_${cat.id}")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                AsyncImage(
                                    model = cat.imageUrl.ifEmpty { "https://cdn-icons-png.flaticon.com/512/3095/3095147.png" },
                                    contentDescription = cat.nameAr,
                                    modifier = Modifier
                                        .size(45.dp)
                                        .clip(CircleShape)
                                        .background(themeColor.copy(alpha = 0.1f))
                                        .padding(4.dp),
                                    contentScale = ContentScale.Fit
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = cat.nameAr,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = themeColor,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }

        // Recommended & Featured VIP Providers
        item {
            Text(
                text = "👑 المهنيين الموصى بهم وعروض النخبة الممتازة",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = themeColor,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Right
            )
        }

        val filteredRecommended = recommendedState.filter {
            val q = viewModel.searchQuery
            val matchesSearch = q.isEmpty() || it.fullName.contains(q) || it.address.contains(q)
            val matchesVer = !viewModel.filterOnlyVerified || it.isVerified
            val matchesVip = !viewModel.filterOnlyVip || it.isPinned
            val matchesDistrict = viewModel.filterDistrict.isEmpty() || it.address.contains(viewModel.filterDistrict)
            matchesSearch && matchesVer && matchesVip && matchesDistrict
        }

        if (filteredRecommended.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "لا يوجد فنيين مميزين مطابقين لبحثك حالياً.",
                        color = themeColor.copy(alpha = 0.6f),
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            items(filteredRecommended) { prov ->
                ProviderCardItem(prov, themeColor, bgColor) {
                    viewModel.selectedProvider = prov
                    viewModel.currentScreen = "provider_details"
                }
            }
        }

        // Bottom Footer from settings
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Surface(
                color = themeColor.copy(alpha = 0.05f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = settingsState?.footerText?.ifEmpty { "دليلي ومساعدك الذكي WAM" } ?: "دليلي ومساعدك الذكي WAM",
                    fontSize = (settingsState?.footerSize ?: 12).sp,
                    color = themeColor.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    // Voice Dialogue Prompt simulate
    if (showVoicePromptDialog) {
        AlertDialog(
            onDismissRequest = { showVoicePromptDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.searchQuery = "سباكة حدة"
                    showVoicePromptDialog = false
                }) {
                    Text("محاكاة الكلمة المتحدثة: 'سباكة حدة'", color = themeColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showVoicePromptDialog = false }) {
                    Text("إلغاء", color = themeColor.copy(alpha = 0.6f))
                }
            },
            title = { Text("🎤 الميكروفون - البحث الصوتي والمساعد الذكي بسلاسه") },
            text = {
                Column {
                    Text("جاري استلام الإدخال الصوتي اليمني...")
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(themeColor)
                    )
                }
            },
            containerColor = bgColor
        )
    }
}

// --- PROVIDERS LIST SCREEN ---
@Composable
fun ProvidersListScreen(
    viewModel: AppViewModel,
    themeColor: Color,
    bgColor: Color
) {
    val category = viewModel.selectedCategory ?: return
    val providersAll by viewModel.activeProviders.collectAsState()

    val filteredProviders = remember(providersAll, category, viewModel.searchQuery, viewModel.filterOnlyVerified, viewModel.filterOnlyVip, viewModel.filterDistrict) {
        providersAll.filter {
            it.mainCategoryId == category.id &&
                    (viewModel.searchQuery.isEmpty() || it.fullName.contains(viewModel.searchQuery, true) || it.address.contains(viewModel.searchQuery, true)) &&
                    (!viewModel.filterOnlyVerified || it.isVerified) &&
                    (!viewModel.filterOnlyVip || it.isPinned) &&
                    (viewModel.filterDistrict.isEmpty() || it.address.contains(viewModel.filterDistrict, true))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { viewModel.currentScreen = "home" }) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "رجوع",
                    tint = themeColor
                )
            }
            Text(
                text = category.nameAr,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = themeColor
            )
            Spacer(modifier = Modifier.width(48.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search in Category
        OutlinedTextField(
            value = viewModel.searchQuery,
            onValueChange = { viewModel.searchQuery = it },
            label = { Text("ابحث في هذا القسم...") },
            trailingIcon = { Icon(Icons.Default.Search, "بحث") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColor)
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredProviders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "لم يتم العثور على مزودي خدمات ينتمون لهذا التصنيف المطابق لمعايير البحث حالياً في دليل اليمن.",
                    textAlign = TextAlign.Center,
                    color = themeColor.copy(alpha = 0.6f)
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredProviders) { provider ->
                    ProviderCardItem(provider, themeColor, bgColor) {
                        viewModel.selectedProvider = provider
                        viewModel.currentScreen = "provider_details"
                    }
                }
            }
        }
    }
}

@Composable
fun ProviderCardItem(
    provider: ProviderEntity,
    themeColor: Color,
    bgColor: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = themeColor.copy(alpha = 0.04f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, themeColor.copy(alpha = 0.15f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("provider_card_${provider.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(65.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(themeColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (provider.profileImageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = provider.profileImageUrl,
                        contentDescription = provider.fullName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = themeColor.copy(alpha = 0.5f),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (provider.isPinned) {
                        Surface(
                            color = Color(0xFFFFD700).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "راعٍ نخبوي 👑",
                                fontSize = 10.sp,
                                color = Color(0xFFBD9A00),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    if (provider.isVerified) {
                        Surface(
                            color = Color(0xFF007AFF).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "موثق معتمد ✔️",
                                fontSize = 10.sp,
                                color = Color(0xFF0056B3),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    Text(
                        text = provider.fullName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "⭐ ${String.format("%.1f", provider.averageRating)} (${provider.totalReviews} تقييم)",
                        fontSize = 12.sp,
                        color = Color(0xFFE5A93B),
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "📍 ${provider.address} - ${provider.district}",
                    fontSize = 12.sp,
                    color = themeColor.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// --- PROVIDER DETAILS SCREEN ---
@Composable
fun ProviderDetailsScreen(
    viewModel: AppViewModel,
    themeColor: Color,
    bgColor: Color
) {
    val provider = viewModel.selectedProvider ?: return
    val context = LocalContext.current

    val reviewsList by viewModel.getReviewsForProvider(provider.id).collectAsState(initial = emptyList())

    var ratingInput by remember { mutableStateOf(5) }
    var commentInput by remember { mutableStateOf("") }
    var reportReasonInput by remember { mutableStateOf("") }
    var reportDetailsInput by remember { mutableStateOf("") }

    var showReviewDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Back buttons Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.currentScreen = "home" }) {
                    Icon(Icons.Default.Home, "الرئيسية", tint = themeColor)
                }
                Text(
                    text = "دليل خدمات اليمن - كرت الفني",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColor
                )
                IconButton(onClick = { viewModel.currentScreen = "providers_list" }) {
                    Icon(Icons.Default.ArrowBack, "رجوع للقسم", tint = themeColor)
                }
            }
        }

        // Beautiful Avatar header Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = themeColor.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, themeColor.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(themeColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (provider.profileImageUrl.isNotEmpty()) {
                            AsyncImage(
                                model = provider.profileImageUrl,
                                contentDescription = provider.fullName,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.Person, null, tint = themeColor, modifier = Modifier.size(50.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (provider.isPinned) {
                            Text("👑 ", fontSize = 20.sp)
                        }
                        if (provider.isVerified) {
                            Text("✔️ ", fontSize = 18.sp, color = Color(0xFF007AFF))
                        }
                        Text(
                            text = provider.fullName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeColor
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "📍 العنوان: ${provider.address} - المجاورة: ${provider.district}",
                        fontSize = 13.sp,
                        color = themeColor.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "⭐ تقييم الأمان والجودة: ${String.format("%.1f", provider.averageRating)} / 5 (${provider.totalReviews} صوت)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD4AF37)
                    )
                }
            }
        }

        // Works gallery
        if (provider.workImagesCSV.isNotEmpty()) {
            item {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                    Text(
                        text = "🖼️ صور من أعمال هذا المهني وتجهيزاته",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = themeColor,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val images = provider.workImagesCSV.split(",")
                        items(images) { path ->
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.size(100.dp)
                            ) {
                                AsyncImage(
                                    model = path.trim(),
                                    contentDescription = "عمل المهني",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            }
        }

        // Contact buttons Row (Direct call, SMS, Whatsapp, In-app chat)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = themeColor.copy(alpha = 0.02f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, themeColor.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "💬 تواصل فوري ومباشر مع المهني",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColor,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${provider.phone}"))
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("dial_button")
                        ) {
                            Text("اتصال 📞", color = bgColor, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/${provider.phone.replace("+", "").replace(" ", "")}"))
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("واتساب 💬", color = Color.White, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                viewModel.startDirectChatWithProvider(provider)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColor.copy(alpha = 0.7f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("محادثة ✍️", color = bgColor, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Submitting feedbacks & Report buttons Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { showReviewDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE5A93B))
                ) {
                    Text("أضف تقييمك ⭐", color = Color.White)
                }

                Button(
                    onClick = { showReportDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("بلاغ أمن وسلامة 🛡️", color = Color.White)
                }
            }
        }

        // Reviews Lists
        item {
            Text(
                text = "💬 آراء ومراجعات العملاء عن الخدمة",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = themeColor,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Right
            )
        }

        if (reviewsList.isEmpty()) {
            item {
                Text(
                    "لم يتم إضافة أي تقييمات عن هذا الكرت بعد! هل تعاملت معه؟ أضف رأيك الصادق لتراكم رصيد الهدايا من التطبيق.",
                    fontSize = 12.sp,
                    color = themeColor.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            items(reviewsList) { rev ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = themeColor.copy(alpha = 0.03f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, themeColor.copy(alpha = 0.1f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "⭐ " + rev.rating + "/5",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD4AF37),
                                fontSize = 12.sp
                            )
                            Text(
                                text = "عميل معرف دليلي",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = themeColor
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = rev.comment,
                            fontSize = 12.sp,
                            color = themeColor.copy(alpha = 0.8f),
                            textAlign = TextAlign.Right
                        )
                    }
                }
            }
        }
    }

    // FEEDBACK DIALOGUE
    if (showReviewDialog) {
        AlertDialog(
            onDismissRequest = { showReviewDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.submitReview(provider.id, ratingInput, commentInput)
                        commentInput = ""
                        showReviewDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColor)
                ) {
                    Text("حفظ والائتمان بـ 15 نقطة! 🎁", color = bgColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReviewDialog = false }) {
                    Text("إلغاء", color = themeColor.copy(alpha = 0.6f))
                }
            },
            title = { Text("أغلق الخدمة وقيم بصدق وأمانة ⭐") },
            text = {
                Column {
                    Text("حدد النجوم:")
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        (1..5).forEach { stars ->
                            IconButton(onClick = { ratingInput = stars }) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = if (stars <= ratingInput) Color(0xFFE5A93B) else themeColor.copy(alpha = 0.2f)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = commentInput,
                        onValueChange = { commentInput = it },
                        label = { Text("تعليقك وتقييم الأثاث والعمل...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            containerColor = bgColor
        )
    }

    // REPORT DIALOGUE
    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.submitReport(provider.id, reportReasonInput, reportDetailsInput)
                        reportReasonInput = ""
                        reportDetailsInput = ""
                        showReportDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("رفع البلاغ للإدارة 🚨", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text("إلغاء", color = themeColor.copy(alpha = 0.6f))
                }
            },
            title = { Text("إرسال بلاغ طارئ عن مخالصة") },
            text = {
                Column {
                    OutlinedTextField(
                        value = reportReasonInput,
                        onValueChange = { reportReasonInput = it },
                        label = { Text("سبب البلاغ المباشر (مثل: سعر مبالغ، تأخر، أمانة)...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reportDetailsInput,
                        onValueChange = { reportDetailsInput = it },
                        label = { Text("تفاصيل وتوضيح...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            containerColor = bgColor
        )
    }
}

// --- REGISTER PROVIDER SCREEN ---
@Composable
fun RegisterProviderScreen(
    viewModel: AppViewModel,
    themeColor: Color,
    bgColor: Color
) {
    val context = LocalContext.current
    val categoriesState by viewModel.categories.collectAsState()
    val activeTermsState by viewModel.activeTerms.collectAsState()

    var fullNameInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }
    var catSelectedId by remember { mutableStateOf("") }
    var addressInput by remember { mutableStateOf("") }
    var districtInput by remember { mutableStateOf("") }
    var profileImageInput by remember { mutableStateOf("") }
    var idCardInput by remember { mutableStateOf("") }
    var imagesListInput by remember { mutableStateOf("") }

    var termsApproved by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "📝 انضم الآن لأكبر دليل مهني باليمن",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = themeColor,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Right
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "أدخل بياناتك ومعلوماتك وسيتم مراجعتها وتفعيل شارتك ونشر كرتك فوراً.",
                fontSize = 12.sp,
                color = themeColor.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Right
            )
        }

        item {
            OutlinedTextField(
                value = fullNameInput,
                onValueChange = { fullNameInput = it },
                label = { Text("الاسم الكامل الثلاثي واللقب 👤") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColor),
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                value = phoneInput,
                onValueChange = { phoneInput = it },
                label = { Text("رقم هاتفك اليمني (مكالمات + واتساب) 📞") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColor),
                singleLine = true
            )
        }

        item {
            Text(
                text = "اختر تخصصك الرئيسي:",
                color = themeColor,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categoriesState.forEach { cat ->
                    val active = catSelectedId == cat.id
                    Button(
                        onClick = { catSelectedId = cat.id },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (active) themeColor else themeColor.copy(alpha = 0.1f)
                        )
                    ) {
                        Text(cat.nameAr, color = if (active) bgColor else themeColor, fontSize = 11.sp)
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = addressInput,
                onValueChange = { addressInput = it },
                label = { Text("العنوان بالتفصيل (مثل: صنعاء فرع الدائري)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColor)
            )
        }

        item {
            OutlinedTextField(
                value = districtInput,
                onValueChange = { districtInput = it },
                label = { Text("الحارة والمجاورة (مثل:منطقة حدة)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColor),
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                value = profileImageInput,
                onValueChange = { profileImageInput = it },
                label = { Text("مسار أو رابط صورتك الشخصية 🖼️ (اختياري)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColor),
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                value = idCardInput,
                onValueChange = { idCardInput = it },
                label = { Text("مسار أو رابط صورة بطاقتك الشخصية لإثبات الموثوقية الشارة زرقاء 💳") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColor),
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                value = imagesListInput,
                onValueChange = { imagesListInput = it },
                label = { Text("روابط لأعمالك وتجاربك السابقة مفصولة بفاصلة , 📁") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColor),
                singleLine = true
            )
        }

        // Terms selection list
        if (activeTermsState.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = themeColor.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                        Text(
                            text = "📜 الشروط والمواثيق والضوابط المعتمدة بالتطبيق:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = themeColor,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        activeTermsState.forEach { t ->
                            Text(
                                text = "🔹 ${t.termText}",
                                fontSize = 11.sp,
                                color = themeColor.copy(alpha = 0.8f),
                                modifier = Modifier.padding(vertical = 2.dp),
                                textAlign = TextAlign.Right
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "أوافق بالكامل على الشروط وأرغب في تفعيل كرتي بمسار دليلي",
                    fontSize = 11.sp,
                    color = themeColor
                )
                Spacer(modifier = Modifier.width(6.dp))
                Checkbox(
                    checked = termsApproved,
                    onCheckedChange = { termsApproved = it },
                    colors = CheckboxDefaults.colors(checkedColor = themeColor),
                    modifier = Modifier.testTag("terms_checkbox")
                )
            }
        }

        item {
            val formsOk = fullNameInput.isNotEmpty() && phoneInput.isNotEmpty() && catSelectedId.isNotEmpty() && addressInput.isNotEmpty() && termsApproved
            Button(
                onClick = {
                    viewModel.registerProvider(
                        fullNameInput, phoneInput, catSelectedId, "",
                        addressInput, districtInput, 15.3186, 44.2045,
                        profileImageInput, idCardInput, imagesListInput
                    ) {
                        Toast.makeText(context, "تم إرسال طلب تفعيل كرتك بنجاح للجنة التدقيق والرقابة!", Toast.LENGTH_LONG).show()
                        viewModel.currentScreen = "home"
                    }
                },
                enabled = formsOk,
                colors = ButtonDefaults.buttonColors(containerColor = themeColor, disabledContainerColor = themeColor.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("submit_registration_btn")
            ) {
                Text("إرسال طلب الانضمام والتفعيل اللحظي! ✅", color = bgColor, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// --- ADMIN PANEL SCREEN ---
@Composable
fun AdminPanelScreen(
    viewModel: AppViewModel,
    themeColor: Color,
    bgColor: Color,
    themePairs: List<Triple<String, Color, Color>>
) {
    val context = LocalContext.current
    val pendingList by viewModel.pendingProviders.collectAsState()
    val allProvsList by viewModel.allProviders.collectAsState()
    val categoriesList by viewModel.categories.collectAsState()
    val logsList by viewModel.activityLogs.collectAsState()
    val reportLists by viewModel.reports.collectAsState()
    val activeBannersState by viewModel.activeBanners.collectAsState()

    var activeTab by remember { mutableStateOf("settings") } // "settings", "approvals", "providers", "categories", "logs_reports"

    // Settings Inputs
    val currentSet by viewModel.appSettings.collectAsState()
    var editAppName by remember { mutableStateOf("") }
    var editWelcome by remember { mutableStateOf("") }
    var editFooter by remember { mutableStateOf("") }
    var editPhone by remember { mutableStateOf("") }

    var newCatId by remember { mutableStateOf("") }
    var newCatNameAr by remember { mutableStateOf("") }
    var newCatImageUrl by remember { mutableStateOf("") }

    var directName by remember { mutableStateOf("") }
    var directPhone by remember { mutableStateOf("") }
    var directCatId by remember { mutableStateOf("") }
    var directAddress by remember { mutableStateOf("") }
    var directVip by remember { mutableStateOf(false) }

    LaunchedEffect(currentSet) {
        currentSet?.let {
            editAppName = it.appName
            editWelcome = it.welcomeMessage
            editFooter = it.footerText
            editPhone = it.supportPhone
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "🛡️ محطة الإدارة والتحكم الشامل بدليل اليمن",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = themeColor,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Horizontal Tabs Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = { activeTab = "settings" },
                colors = ButtonDefaults.buttonColors(containerColor = if (activeTab == "settings") themeColor else themeColor.copy(alpha = 0.1f)),
                modifier = Modifier.testTag("admin_tab_settings")
            ) {
                Text("خيارات وألوان 🎨", color = if (activeTab == "settings") bgColor else themeColor, fontSize = 11.sp)
            }
            Button(
                onClick = { activeTab = "approvals" },
                colors = ButtonDefaults.buttonColors(containerColor = if (activeTab == "approvals") themeColor else themeColor.copy(alpha = 0.1f)),
                modifier = Modifier.testTag("admin_tab_approvals")
            ) {
                Text("مراجعة الطلبات (${pendingList.filter { it.status == "pending" }.size}) 📝", color = if (activeTab == "approvals") bgColor else themeColor, fontSize = 11.sp)
            }
            Button(
                onClick = { activeTab = "providers" },
                colors = ButtonDefaults.buttonColors(containerColor = if (activeTab == "providers") themeColor else themeColor.copy(alpha = 0.1f)),
                modifier = Modifier.testTag("admin_tab_providers")
            ) {
                Text("شؤون الفنيين 👤", color = if (activeTab == "providers") bgColor else themeColor, fontSize = 11.sp)
            }
            Button(
                onClick = { activeTab = "categories" },
                colors = ButtonDefaults.buttonColors(containerColor = if (activeTab == "categories") themeColor else themeColor.copy(alpha = 0.1f))
            ) {
                Text("أقسام وصور 📂", color = if (activeTab == "categories") bgColor else themeColor, fontSize = 11.sp)
            }
            Button(
                onClick = { activeTab = "logs_reports" },
                colors = ButtonDefaults.buttonColors(containerColor = if (activeTab == "logs_reports") themeColor else themeColor.copy(alpha = 0.1f))
            ) {
                Text("بلاغات وأمان 🚨", color = if (activeTab == "logs_reports") bgColor else themeColor, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tab Content Layout
        Box(modifier = Modifier.weight(1f)) {
            when (activeTab) {
                "settings" -> {
                    currentSet?.let { configuration ->
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            item {
                                Text("تعديل سمة والألوان العامة للتطبيق:", fontWeight = FontWeight.Bold, color = themeColor)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    themePairs.forEach { pair ->
                                        Card(
                                            onClick = {
                                                val primaryHex = String.format("#%06X", 0xFFFFFF and pair.second.toArgb())
                                                val secondaryHex = String.format("#%06X", 0xFFFFFF and pair.third.toArgb())
                                                viewModel.saveSettings(configuration.copy(
                                                    primaryColor = primaryHex,
                                                    secondaryColor = secondaryHex
                                                ))
                                                Toast.makeText(context, "تم تبديل السمة: ${pair.first}", Toast.LENGTH_SHORT).show()
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = CardDefaults.cardColors(containerColor = pair.second),
                                            border = BorderStroke(1.dp, pair.third),
                                            modifier = Modifier.height(45.dp).padding(horizontal = 4.dp)
                                        ) {
                                            Box(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                                                Text(pair.first, color = pair.third, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                OutlinedTextField(
                                    value = editAppName,
                                    onValueChange = { editAppName = it },
                                    label = { Text("اسم المشروع والبرنامج 📂") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            item {
                                OutlinedTextField(
                                    value = editWelcome,
                                    onValueChange = { editWelcome = it },
                                    label = { Text("رسالة الترحيب الكبرى 🌟") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            item {
                                OutlinedTextField(
                                    value = editFooter,
                                    onValueChange = { editFooter = it },
                                    label = { Text("توقيع الفوتر ومستند البرنامج 📑") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            item {
                                OutlinedTextField(
                                    value = editPhone,
                                    onValueChange = { editPhone = it },
                                    label = { Text("رقم هاتف دعم دليل اليمن الطوارئ 📞") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("تفعيل المجيب العائم WAM", color = themeColor)
                                    Switch(
                                        checked = configuration.isAiFloating,
                                        onCheckedChange = {
                                            viewModel.saveSettings(configuration.copy(isAiFloating = it))
                                        }
                                    )
                                }
                            }

                            item {
                                Button(
                                    onClick = {
                                        viewModel.saveSettings(configuration.copy(
                                            appName = editAppName,
                                            welcomeMessage = editWelcome,
                                            footerText = editFooter,
                                            supportPhone = editPhone
                                        ))
                                        Toast.makeText(context, "تم حفظ وتطبيق الخيارات لجميع المستخدمين!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("تأثير الإعدادات وتطبيق فوري 💾", color = bgColor)
                                }
                            }

                            item {
                                Divider(color = themeColor.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 12.dp))
                                Text("خيارات الصيانة وقاعدة البيانات الاحتياطية:", fontWeight = FontWeight.Bold, color = themeColor)
                            }

                            // CSV Export
                            item {
                                var csvResult by remember { mutableStateOf("") }
                                val scope = rememberCoroutineScope()
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                val csv = viewModel.getTableCSV("providers")
                                                csvResult = csv
                                                Toast.makeText(context, "تم تصدير الفنيين لنسق CSV!", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = themeColor.copy(alpha = 0.8f)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("تصدير الفنيين CSV", fontSize = 11.sp, color = bgColor)
                                    }

                                    Button(
                                        onClick = {
                                            viewModel.exportBackup { statusText ->
                                                csvResult = statusText
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = themeColor.copy(alpha = 0.8f)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("نسخ احتياطي كامل", fontSize = 11.sp, color = bgColor)
                                    }
                                }

                                if (csvResult.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Card(colors = CardDefaults.cardColors(containerColor = themeColor.copy(alpha = 0.05f))) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text("مخرجات التصدير والحفظ:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text(csvResult, fontSize = 10.sp, textAlign = TextAlign.Left, color = themeColor)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                "approvals" -> {
                    val activePending = pendingList.filter { it.status == "pending" }
                    if (activePending.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("لا توجد طلبات انضمام للمراجعة في دليل اليمن حالياً 🎉", color = themeColor.copy(alpha = 0.6f))
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(activePending) { pend ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = themeColor.copy(alpha = 0.03f)),
                                    border = BorderStroke(1.dp, themeColor.copy(alpha = 0.15f)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Text(text = "اسم المهني: " + pend.fullName, fontWeight = FontWeight.Bold, color = themeColor)
                                        Text(text = "الهاتف: " + pend.phone, color = themeColor.copy(alpha = 0.8f))
                                        Text(text = "العنوان: " + pend.address + " - " + pend.district, color = themeColor.copy(alpha = 0.8f))
                                        
                                        if (pend.idCardImageUrl.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(text = "صورة الموثوقية:", fontSize = 12.sp, color = themeColor)
                                            Card(modifier = Modifier.size(width = 200.dp, height = 100.dp)) {
                                                AsyncImage(model = pend.idCardImageUrl, contentDescription = null, contentScale = ContentScale.Crop)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    viewModel.rejectProvider(pend.id, "المستندات غير واضحة")
                                                    Toast.makeText(context, "تم رفض التسجيل كرت الفني بنجاح", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("رفض مع تفنيد", color = Color.White, fontSize = 11.sp)
                                            }

                                            Button(
                                                onClick = {
                                                    viewModel.acceptProvider(pend.id)
                                                    Toast.makeText(context, "تم تفعيل كرت الفني ونشره لحظياً في الدليل!", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("قبول وبث لحظي ✔️", color = Color.White, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                "providers" -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = themeColor.copy(alpha = 0.05f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalAlignment = Alignment.End) {
                                    Text("➕ إضافة فني مباشرة من الإدارة (تخصيص فوري):", fontWeight = FontWeight.Bold, color = themeColor)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(value = directName, onValueChange = { directName = it }, label = { Text("اسم الفني ثلاثي") })
                                    OutlinedTextField(value = directPhone, onValueChange = { directPhone = it }, label = { Text("الهاتف") })
                                    OutlinedTextField(value = directAddress, onValueChange = { directAddress = it }, label = { Text("صنعاء، العنوان") })
                                    
                                    // Categories select
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                                        categoriesList.forEach {
                                            val act = directCatId == it.id
                                            Button(onClick = { directCatId = it.id }, colors = ButtonDefaults.buttonColors(containerColor = if (act) themeColor else themeColor.copy(alpha = 0.1f)), modifier = Modifier.padding(2.dp)) {
                                                Text(it.nameAr, color = if (act) bgColor else themeColor, fontSize = 10.sp)
                                            }
                                        }
                                    }
                                    
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("تثبيت VIP ورعاية رئيسية")
                                        Checkbox(checked = directVip, onCheckedChange = { directVip = it })
                                    }

                                    Button(
                                        onClick = {
                                            if (directName.isNotEmpty() && directPhone.isNotEmpty() && directCatId.isNotEmpty()) {
                                                viewModel.addProviderDirectly(directName, directPhone, directCatId, directAddress, "أدمن", directVip)
                                                Toast.makeText(context, "تم حفظ المهني مباشرة!", Toast.LENGTH_SHORT).show()
                                                directName = ""
                                                directPhone = ""
                                                directAddress = ""
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = themeColor)
                                    ) {
                                        Text("إضافة ونشر 💾", color = bgColor)
                                    }
                                }
                            }
                        }

                        items(allProvsList) { prov ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = themeColor.copy(alpha = 0.02f)),
                                border = BorderStroke(1.dp, themeColor.copy(alpha = 0.1f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalAlignment = Alignment.End) {
                                    Text(prov.fullName, fontWeight = FontWeight.Bold, color = themeColor)
                                    Text("معرف: ${prov.id} - هاتف: ${prov.phone}", fontSize = 11.sp)
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("موثق معتمد ✔️", fontSize = 10.sp)
                                            Switch(checked = prov.isVerified, onCheckedChange = {
                                                viewModel.updateProviderFeatures(prov.id, it, prov.isPinned, prov.isRecommended, prov.isSubscribed)
                                            })
                                        }

                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("VIP رعاية 👑", fontSize = 10.sp)
                                            Switch(checked = prov.isPinned, onCheckedChange = {
                                                viewModel.updateProviderFeatures(prov.id, prov.isVerified, it, it, prov.isSubscribed)
                                            })
                                        }

                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("اشتراك دفع 💳", fontSize = 10.sp)
                                            Switch(checked = prov.isSubscribed, onCheckedChange = {
                                                viewModel.updateProviderFeatures(prov.id, prov.isVerified, prov.isPinned, prov.isRecommended, it)
                                            })
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            viewModel.deleteProvider(prov.id)
                                            Toast.makeText(context, "تم إزالة كرت الفني وحذفه نهائياً", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("حذف الكرت 🗑️", color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }

                "categories" -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = themeColor.copy(alpha = 0.05f))) {
                                Column(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalAlignment = Alignment.End) {
                                    Text("أضف قسماً تخصصياً جديداً:", fontWeight = FontWeight.Bold, color = themeColor)
                                    OutlinedTextField(value = newCatId, onValueChange = { newCatId = it }, label = { Text("معرف القسم الفرعي (مثل: c6)") })
                                    OutlinedTextField(value = newCatNameAr, onValueChange = { newCatNameAr = it }, label = { Text("اسم القسم بالعربية") })
                                    OutlinedTextField(value = newCatImageUrl, onValueChange = { newCatImageUrl = it }, label = { Text("مسار صورة الأيقونة") })
                                    
                                    Button(
                                        onClick = {
                                            if (newCatId.isNotEmpty() && newCatNameAr.isNotEmpty()) {
                                                viewModel.addCategory(newCatId, newCatNameAr, "", newCatImageUrl, 99)
                                                Toast.makeText(context, "تم بث وحفظ القسم الجديد لحظياً!", Toast.LENGTH_SHORT).show()
                                                newCatId = ""
                                                newCatNameAr = ""
                                                newCatImageUrl = ""
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = themeColor)
                                    ) {
                                        Text("إضافة قسم", color = bgColor)
                                    }
                                }
                            }
                        }

                        items(categoriesList) { cat ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = {
                                    viewModel.deleteCategory(cat.id)
                                    Toast.makeText(context, "تم حذف القسم", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(Icons.Default.Delete, "حذف", tint = Color(0xFFD32F2F))
                                }
                                Text("${cat.nameAr} (${cat.id})", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                "logs_reports" -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        item {
                            Text("البلاغات والشكاوى الحية من المستخدمين:", fontWeight = FontWeight.Bold, color = themeColor)
                        }

                        if (reportLists.isEmpty()) {
                            item {
                                Text("لا توجد بلاغات تم تسجيلها حالياً في الكواليس! 🌸", color = themeColor.copy(alpha = 0.6f))
                            }
                        } else {
                            items(reportLists) { rep ->
                                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))) {
                                    Column(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalAlignment = Alignment.End) {
                                        Text("بلاغ ضد الفني ذو الرقم: ${rep.providerId}", fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                                        Text("السبب: ${rep.reason}", fontSize = 13.sp)
                                        Text("التوضيح: ${rep.details}", fontSize = 12.sp)
                                        
                                        TextButton(onClick = {
                                            viewModel.deleteReport(rep.id)
                                            Toast.makeText(context, "تم تسوية البلاغ وحذفه", Toast.LENGTH_SHORT).show()
                                        }) {
                                            Text("تصفية وحذف البلاغ", color = Color(0xFFC62828))
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Divider(color = themeColor.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 12.dp))
                            Text("السجل الأمني للتحكم والتدقيق:", fontWeight = FontWeight.Bold, color = themeColor)
                        }

                        items(logsList) { log ->
                            Card(colors = CardDefaults.cardColors(containerColor = themeColor.copy(alpha = 0.02f))) {
                                Column(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalAlignment = Alignment.End) {
                                    Text("الأمر: ${log.action}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("التفصيل: ${log.details}", fontSize = 12.sp)
                                    Text("المشغل الأمني: ${log.adminId} - الوقت: ${log.timestamp}", fontSize = 10.sp, color = themeColor.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- GEMINI INTELLIGENT ASSISTANT (CHATBOARD) SCREEN ---
@Composable
fun GeminiChatScreen(
    viewModel: AppViewModel,
    themeColor: Color,
    bgColor: Color
) {
    val messages by viewModel.geminiMessages.collectAsState()
    var inputStr by remember { mutableStateOf("") }
    val listState = rememberScrollState()
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { viewModel.clearVirtualAssistantChat() }) {
                Icon(Icons.Default.Refresh, "تصفية الدردشة", tint = themeColor)
            }
            Text(
                text = "💬 مساعد WAM الذكي لمشاريع اليمن",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = themeColor
            )
            IconButton(onClick = { viewModel.currentScreen = "home" }) {
                Icon(Icons.Default.ArrowBack, "رجوع", tint = themeColor)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Chat Container
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(1.dp, themeColor.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                .background(themeColor.copy(alpha = 0.02f))
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(listState)
            ) {
                messages.forEach { msg ->
                    val isMe = msg.second
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isMe) Arrangement.Start else Arrangement.End
                    ) {
                        Surface(
                            color = if (isMe) themeColor.copy(alpha = 0.15f) else themeColor,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .widthIn(max = 280.dp)
                        ) {
                            Text(
                                text = msg.first,
                                fontSize = 13.sp,
                                color = if (isMe) themeColor else bgColor,
                                modifier = Modifier.padding(10.dp),
                                textAlign = TextAlign.Right
                            )
                        }
                    }
                }
                
                if (viewModel.geminiIsLoading) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "جاري الفحص المباشر وصياغة الجواب كوزميك فوري... ⌛",
                        fontSize = 11.sp,
                        color = themeColor.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Suggestions buttons
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val suggestions = listOf("ما هي أقسام السباكة؟", "كيف أتصل بـ فني؟", "رقم الدعم الفني؟", "بلاغ عن مهرب")
            suggestions.forEach { sug ->
                Card(
                    onClick = {
                        viewModel.sendGeminiPrompt(sug)
                    },
                    colors = CardDefaults.cardColors(containerColor = themeColor.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = sug,
                        fontSize = 11.sp,
                        color = themeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Input row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    if (inputStr.trim().isNotEmpty()) {
                        viewModel.sendGeminiPrompt(inputStr)
                        inputStr = ""
                        scope.launch {
                            delay(200)
                            listState.animateScrollTo(listState.maxValue)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("send_prompt_btn")
            ) {
                Text("إرسال", color = bgColor)
            }

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedTextField(
                value = inputStr,
                onValueChange = { inputStr = it },
                placeholder = { Text("اكتب استفسارك هنا يا غالي...") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("prompt_field"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColor)
            )
        }
    }
}

// --- USER CHatS PANEL (USER CHAT SCREEN WITH PROFESSIONALS) ---
@Composable
fun UserChatScreen(
    viewModel: AppViewModel,
    themeColor: Color,
    bgColor: Color
) {
    val messages by viewModel.currentChatMessages.collectAsState()
    var inputMsg by remember { mutableStateOf("") }
    val listState = rememberScrollState()
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.width(48.dp))
            Text(
                text = "الدردشة مع: ${viewModel.selectedChatParticipantName}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = themeColor
            )
            IconButton(onClick = { viewModel.currentScreen = "home" }) {
                Icon(Icons.Default.ArrowBack, "رجوع", tint = themeColor)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Chats Box
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(1.dp, themeColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(listState)
            ) {
                if (messages.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("عجّل بالتنسيق! ابعث رسالتك الأولى لبدء التفاهم الفني.", fontSize = 12.sp, color = themeColor.copy(alpha = 0.5f))
                    }
                } else {
                    messages.forEach { m ->
                        val isMe = m.senderId == "USER"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isMe) Arrangement.Start else Arrangement.End
                        ) {
                            Surface(
                                color = if (isMe) themeColor else themeColor.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.padding(vertical = 4.dp).widthIn(max = 240.dp)
                            ) {
                                Text(
                                    text = m.message,
                                    fontSize = 13.sp,
                                    color = if (isMe) bgColor else themeColor,
                                    modifier = Modifier.padding(8.dp),
                                    textAlign = TextAlign.Right
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    if (inputMsg.trim().isNotEmpty()) {
                        viewModel.sendLocalMessage(inputMsg)
                        inputMsg = ""
                        scope.launch {
                            delay(100)
                            listState.animateScrollTo(listState.maxValue)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("أرسل ورّد", color = bgColor)
            }

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedTextField(
                value = inputMsg,
                onValueChange = { inputMsg = it },
                placeholder = { Text("تفاوض هادئ على السعر والمعدات...") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColor)
            )
        }
    }
}

// --- HELPER SAFE COLOR PARSER ---
fun parseColorSafely(hexString: String, fallback: Color): Color {
    return try {
        val cleanHex = if (hexString.startsWith("#")) hexString.substring(1) else hexString
        if (cleanHex.length == 6) {
            Color(android.graphics.Color.parseColor("#$cleanHex"))
        } else if (cleanHex.length == 8) {
            Color(android.graphics.Color.parseColor("#$cleanHex"))
        } else {
            fallback
        }
    } catch (e: Exception) {
        fallback
    }
}
