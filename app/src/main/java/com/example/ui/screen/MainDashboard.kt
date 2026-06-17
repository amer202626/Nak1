package com.example.ui.screen

import android.app.Activity
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import coil.compose.rememberAsyncImagePainter
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.*
import com.example.ui.theme.ActiveGreen
import com.example.ui.theme.InactiveRed
import com.example.ui.theme.VerifiedBlue
import com.example.ui.viewmodel.AppViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Screen Enumeration
fun getMaskedText(text: String, isConnected: Boolean): String {
    if (!isConnected) {
        return text.replace("777644670", "wam777644").replace("+967777644670", "wam777644")
    }
    return text
}

fun decodeBase64ToBitmap(base64Str: String): Bitmap? {
    return try {
        val pureBase64 = if (base64Str.contains(",")) base64Str.substringAfter(",") else base64Str
        val decodedBytes = Base64.decode(pureBase64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    } catch (e: Exception) {
        null
    }
}

@Composable
fun CategoryImage(imageUrl: String, modifier: Modifier = Modifier) {
    if (imageUrl.isEmpty()) {
        Text("📁", modifier = modifier, fontSize = 16.sp)
    } else if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://") || imageUrl.startsWith("/") || imageUrl.startsWith("content://")) {
        Image(
            painter = rememberAsyncImagePainter(imageUrl),
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else if (imageUrl.length > 30) {
        val bitmap = remember(imageUrl) { decodeBase64ToBitmap(imageUrl) }
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = modifier,
                contentScale = ContentScale.Crop
            )
        } else {
            Text("📁", modifier = modifier, fontSize = 16.sp)
        }
    } else {
        Text(imageUrl, modifier = modifier, fontSize = 16.sp)
    }
}

class TtsHelper(context: android.content.Context) {
    private var tts: TextToSpeech? = null
    private var isReady = false

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = java.util.Locale("ar")
                isReady = true
            }
        }
    }

    fun speak(text: String) {
        if (isReady && text.isNotEmpty()) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.shutdown()
    }
}

@Composable
fun rememberTtsHelper(): TtsHelper {
    val context = LocalContext.current
    val ttsHelper = remember { TtsHelper(context) }
    DisposableEffect(Unit) {
        onDispose {
            ttsHelper.shutdown()
        }
    }
    return ttsHelper
}

@Composable
fun TtsSpeakerButton(
    textToSpeak: String,
    modifier: Modifier = Modifier,
    ttsHelper: TtsHelper
) {
    IconButton(
        onClick = { ttsHelper.speak(textToSpeak) },
        modifier = modifier.size(32.dp)
    ) {
        Icon(
            imageVector = Icons.Default.VolumeUp,
            contentDescription = "Speaker read aloud",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun SpeechRecognitionButton(
    onResult: (String) -> Unit,
    modifier: Modifier = Modifier,
    language: String = "ar"
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val text = results?.getOrNull(0) ?: ""
            if (text.isNotEmpty()) {
                onResult(text)
            }
        }
    }

    IconButton(
        onClick = {
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-YE")
                    putExtra(RecognizerIntent.EXTRA_PROMPT, if (language == "ar") "تحدث الآن..." else "Speak now...")
                }
                launcher.launch(intent)
            } catch (e: Exception) {
                Toast.makeText(context, if (language == "ar") "التعرف على الصوت غير مدعوم على جهازك" else "Speech recognition not supported on this device", Toast.LENGTH_SHORT).show()
            }
        },
        modifier = modifier.size(32.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = "Mic speak search",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
    }
}

enum class ActiveScreen {
    HOME,
    LOGIN,
    REGISTER_PROVIDER,
    ADMIN_DASHBOARD,
    CHAT_ROOM,
    ABOUT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboard(viewModel: AppViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // State bindings
    val settings by viewModel.appSettings.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val filteredProviders by viewModel.filteredProviders.collectAsStateWithLifecycle()
    val allProviders by viewModel.allProviders.collectAsStateWithLifecycle()
    val recommendedProviders by viewModel.recommendedProviders.collectAsStateWithLifecycle()
    val pendingProviders by viewModel.pendingProviders.collectAsStateWithLifecycle()
    val pendingCount by viewModel.pendingCount.collectAsStateWithLifecycle()
    val activeBanners by viewModel.activeBanners.collectAsStateWithLifecycle()
    val allBanners by viewModel.allBanners.collectAsStateWithLifecycle()
    val reports by viewModel.reports.collectAsStateWithLifecycle()
    val chats by viewModel.chats.collectAsStateWithLifecycle()
    val currentChatMessages by viewModel.currentChatMessages.collectAsStateWithLifecycle()
    val activityLogs by viewModel.activityLogs.collectAsStateWithLifecycle()
    val activeTerms by viewModel.activeTerms.collectAsStateWithLifecycle()
    val allTerms by viewModel.allTerms.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCatId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
    val selectedDistrict by viewModel.selectedDistrict.collectAsStateWithLifecycle()
    val minRating by viewModel.minRating.collectAsStateWithLifecycle()
    val radiusLimit by viewModel.radiusLimit.collectAsStateWithLifecycle()
    val language by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val userRole by viewModel.userRole.collectAsStateWithLifecycle()
    val chatRoomId by viewModel.activeChatId.collectAsStateWithLifecycle()

    // Screen tracking (Home is default)
    var currentScreen by remember { mutableStateOf(ActiveScreen.HOME) }

    // Dialog flags
    var showBackdoorLogin by remember { mutableStateOf(false) }
    var showBackdoorSettings by remember { mutableStateOf(false) }
    var showChatbotDialog by remember { mutableStateOf(false) }
    var showReviewDialogForProvider by remember { mutableStateOf<ProviderEntity?>(null) }
    var showReportDialogForProvider by remember { mutableStateOf<ProviderEntity?>(null) }
    var showBookingDialogForProvider by remember { mutableStateOf<ProviderEntity?>(null) }

    // Backdoor tap counter triggers
    val handleHomeOrLogoTap: () -> Unit = {
        viewModel.registerBackdoorTap {
            showBackdoorLogin = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier
                            .clickable { handleHomeOrLogoTap() }
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "👑", 
                            fontSize = 24.sp,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(
                            text = if (language == "ar") (settings?.appName ?: "دليل خدمات اليمن") else "WAM Yemen",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    // Top App Bar Navigation Icons: (RTL order - Home, Login, Sign-up, Lang, Refresh)
                    IconButton(
                        onClick = { currentScreen = ActiveScreen.HOME },
                        modifier = Modifier.testTag("nav_home")
                    ) {
                        Icon(imageVector = Icons.Default.Home, contentDescription = "Home")
                    }
                    IconButton(
                        onClick = {
                            if (userRole == "Admin" || userRole == "SuperAdmin") {
                                currentScreen = ActiveScreen.ADMIN_DASHBOARD
                            } else {
                                currentScreen = ActiveScreen.LOGIN
                            }
                        },
                        modifier = Modifier.testTag("nav_login")
                    ) {
                        Icon(
                            imageVector = if (userRole != "Guest") Icons.Default.Dashboard else Icons.Default.Lock,
                            contentDescription = "Login/Dashboard"
                        )
                    }
                    IconButton(
                        onClick = { currentScreen = ActiveScreen.REGISTER_PROVIDER },
                        modifier = Modifier.testTag("nav_register")
                    ) {
                        Icon(imageVector = Icons.Default.PersonAdd, contentDescription = "Register Service Provider")
                    }
                    IconButton(
                        onClick = { viewModel.toggleLanguage() },
                        modifier = Modifier.testTag("nav_language")
                    ) {
                        Icon(imageVector = Icons.Default.Language, contentDescription = "Language toggle")
                    }
                    IconButton(
                        onClick = {
                            Toast.makeText(context, if (language == "ar") "تم تحديث البيانات والمزامنة الفورية بنجاح!" else "Synced with local database!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("nav_sync")
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh & Sync")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    actionIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        bottomBar = {
            // Simplified custom footer aligned to instructions
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                    .padding(vertical = 10.dp, horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left element: Info Button (About) - scaled 50%
                    IconButton(
                        onClick = { currentScreen = ActiveScreen.ABOUT },
                        modifier = Modifier
                            .testTag("btn_about")
                            .size(36.dp) // 50% smaller impact
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info, 
                            contentDescription = "About App",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Middle element: Dynamic Ad text
                    val isConnected = viewModel.isConnected()
                    Text(
                        text = getMaskedText(settings?.footerText ?: "MAW 777644670", isConnected),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                val supportNum = getMaskedText(settings?.supportPhone ?: "777644670", isConnected)
                                if (supportNum == "wam777644") {
                                    Toast.makeText(context, if (language == "ar") "الدعم المباشر ومكالمة الهاتف تتطلب الاتصال بالإنترنت!" else "Live support is only active when online!", Toast.LENGTH_SHORT).show()
                                } else {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$supportNum"))
                                    context.startActivity(intent)
                                }
                            }
                    )

                    // Right element: Virtual Assistant Floating Button Action
                    Button(
                        onClick = { showChatbotDialog = true },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("btn_assistant_trigger")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = "Assistant",
                            modifier = Modifier.size(16.dp).padding(end = 4.dp)
                        )
                        Text(
                            text = if (language == "ar") "خدمات" else "AI Help", 
                            fontSize = 11.sp, 
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                // Opacity controlled app version at absolute bottom center
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "V2.6.2026",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        floatingActionButton = {
            // Live Chat Floating Action Button - small, above Left Info option
            FloatingActionButton(
                onClick = { 
                    currentScreen = ActiveScreen.CHAT_ROOM 
                },
                modifier = Modifier
                    .size(42.dp) // small custom size
                    .testTag("fab_live_chat"),
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            ) {
                Icon(
                    imageVector = Icons.Default.Chat, 
                    contentDescription = "Inbox Live Chat", 
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    ) { innerPadding ->
        
        AnimatedContent(
            targetState = currentScreen,
            modifier = Modifier.padding(innerPadding),
            transitionSpec = {
                slideInVertically(initialOffsetY = { 300 }) + fadeIn() togetherWith
                        slideOutVertically(targetOffsetY = { -300 }) + fadeOut()
            },
            label = "ScreenSwitch"
        ) { screen ->
            when (screen) {
                ActiveScreen.HOME -> HomeScreen(
                    viewModel = viewModel,
                    categories = categories,
                    banners = activeBanners,
                    recommended = recommendedProviders,
                    providers = filteredProviders,
                    searchQuery = searchQuery,
                    selectedCatId = selectedCatId,
                    selectedDistrict = selectedDistrict,
                    language = language,
                    minRating = minRating,
                    onOpenReview = { showReviewDialogForProvider = it },
                    onOpenReport = { showReportDialogForProvider = it },
                    onOpenBooking = { showBookingDialogForProvider = it }
                )
                ActiveScreen.LOGIN -> LoginScreen(
                    viewModel = viewModel,
                    language = language,
                    onSuccess = { currentScreen = ActiveScreen.ADMIN_DASHBOARD }
                )
                ActiveScreen.REGISTER_PROVIDER -> RegisterProviderScreen(
                    viewModel = viewModel,
                    categories = categories,
                    terms = activeTerms,
                    language = language,
                    onSuccess = { currentScreen = ActiveScreen.HOME }
                )
                ActiveScreen.ADMIN_DASHBOARD -> AdminDashboardScreen(
                    viewModel = viewModel,
                    categories = categories,
                    allProviders = allProviders,
                    pendingProviders = pendingProviders,
                    banners = allBanners,
                    reports = reports,
                    chats = chats,
                    logs = activityLogs,
                    terms = allTerms,
                    language = language,
                    settings = settings,
                    onOpenBackdoorSettings = { showBackdoorSettings = true },
                    onLogout = { currentScreen = ActiveScreen.HOME }
                )
                ActiveScreen.CHAT_ROOM -> LiveChatScreen(
                    viewModel = viewModel,
                    chats = chats,
                    messages = currentChatMessages,
                    language = language
                )
                ActiveScreen.ABOUT -> AboutAppScreen(
                    settings = settings,
                    language = language,
                    isConnected = viewModel.isConnected()
                )
            }
        }

        // Dialogs Management

        // Backdoor Password Dialogue
        if (showBackdoorLogin) {
            var passInput by remember { mutableStateOf("") }
            Dialog(onDismissRequest = { showBackdoorLogin = false }) {
                Card(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🔑 البوابة السرية للمالك", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "هذه البوابة مخفية تماماً وخاصة بمالك التطبيق الفني لتعديل التهيئة المتقدمة.",
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = passInput,
                            onValueChange = { passInput = it },
                            label = { Text("رمز المرور السري الخاص") },
                            modifier = Modifier.fillMaxWidth().testTag("backdoor_pass_input"),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            TextButton(onClick = { showBackdoorLogin = false }) {
                                Text("إلغاء")
                            }
                            Button(
                                onClick = {
                                    viewModel.loginViaSecretBackdoor(passInput,
                                        success = {
                                            showBackdoorLogin = false
                                            showBackdoorSettings = true
                                            currentScreen = ActiveScreen.ADMIN_DASHBOARD
                                        },
                                        fail = {
                                            Toast.makeText(context, "الرمز السري خاطئ!", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                },
                                modifier = Modifier.testTag("backdoor_login_submit")
                            ) {
                                Text("دخول ومزامنة")
                            }
                        }
                    }
                }
            }
        }

        // Backdoor Settings Panel Dialogue Overlay
        if (showBackdoorSettings && settings != null) {
            BackdoorSettingsDialog(
                settings = settings!!,
                onDismiss = { showBackdoorSettings = false },
                onSave = { updated ->
                    viewModel.updateAppSettings(updated)
                    showBackdoorSettings = false
                }
            )
        }

        // Virtual Assistant Chatbot Dialog
        if (showChatbotDialog) {
            val assistantMessagesList by viewModel.assistantMessages.collectAsStateWithLifecycle()
            val isThinking by viewModel.isAssistantThinking.collectAsStateWithLifecycle()

            Dialog(onDismissRequest = { showChatbotDialog = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.8f)
                        .padding(8.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.SmartToy, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("المساعد الذكي WAM", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(if (viewModel.isConnected()) "متصل بالذكاء الاصطناعي" else "وضع عدم الاتصال المحلي", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f), fontSize = 10.sp)
                                }
                            }
                            IconButton(onClick = { showChatbotDialog = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onPrimary)
                            }
                        }

                        // Chat messages
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .padding(12.dp),
                            reverseLayout = false
                        ) {
                            items(assistantMessagesList) { msg ->
                                val isUser = msg.second
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(
                                                topStart = 16.dp,
                                                topEnd = 16.dp,
                                                bottomStart = if (isUser) 16.dp else 0.dp,
                                                bottomEnd = if (isUser) 0.dp else 16.dp
                                            ))
                                            .background(
                                                if (isUser) MaterialTheme.colorScheme.secondary
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                            .padding(12.dp)
                                            .widthIn(max = 240.dp)
                                    ) {
                                        Text(
                                            text = msg.first,
                                            fontSize = 13.sp,
                                            color = if (isUser) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            if (isThinking) {
                                item {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("جاري معالجة طلبك...", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                    }
                                }
                            }
                        }

                        // Input field
                        var promptText by remember { mutableStateOf("") }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    // Voice search / search imitation
                                    promptText = "ما هي الأقسام المتوفرة؟"
                                }
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = "Voice prompt", tint = MaterialTheme.colorScheme.primary)
                            }
                            
                            OutlinedTextField(
                                value = promptText,
                                onValueChange = { promptText = it },
                                placeholder = { Text("اسأل عن خدمات الكهرباء والسباكة في اليمن...", fontSize = 12.sp) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("assistant_input_field"),
                                shape = RoundedCornerShape(24.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                ),
                                singleLine = true
                            )
                            
                            Spacer(modifier = Modifier.width(6.dp))
                            
                            IconButton(
                                onClick = {
                                    if (promptText.trim().isNotEmpty()) {
                                        viewModel.askAssistant(promptText)
                                        promptText = ""
                                    }
                                },
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                                    .size(40.dp)
                                    .testTag("assistant_send_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

        // Review Provider Dialog
        if (showReviewDialogForProvider != null) {
            val p = showReviewDialogForProvider!!
            var ratingValue by remember { mutableStateOf(5) }
            var commentText by remember { mutableStateOf("") }

            Dialog(onDismissRequest = { showReviewDialogForProvider = null }) {
                Card(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("⭐ تقييم المهني: ${p.fullName}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            for (star in 1..5) {
                                IconButton(onClick = { ratingValue = star }) {
                                    Icon(
                                        imageVector = if (star <= ratingValue) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = "Rating $star",
                                        tint = Color(0xFFFFD700)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = commentText,
                            onValueChange = { commentText = it },
                            label = { Text("اكتب رأيك بصراحة وأمانة...") },
                            modifier = Modifier.fillMaxWidth().height(100.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "ملاحظة: ستحصل فوراً على +15 نقطة ولاء في التطبيق بمجرد مشاركة التقييم!",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { showReviewDialogForProvider = null }) {
                                Text("إلغاء")
                            }
                            Button(onClick = {
                                viewModel.addReview(p.id, ratingValue, commentText)
                                showReviewDialogForProvider = null
                                Toast.makeText(context, "تم التقاط التقييم وإضافة 15 نقطة ولاء بنجاح!", Toast.LENGTH_SHORT).show()
                            }) {
                                Text("إرسال التقييم")
                            }
                        }
                    }
                }
            }
        }

        // Booking Dialog
        if (showBookingDialogForProvider != null) {
            val p = showBookingDialogForProvider!!
            var fullNameInput by remember { mutableStateOf("") }
            var phoneInput by remember { mutableStateOf("") }
            var districtInput by remember { mutableStateOf("") }
            val bookingTtsHelper = rememberTtsHelper()

            Dialog(onDismissRequest = { showBookingDialogForProvider = null }) {
                Card(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (language == "ar") "📅 حجز موعد مع الفني: ${p.fullName}" else "📅 Book Appt with: ${p.fullName}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                modifier = Modifier.weight(1f)
                            )
                            TtsSpeakerButton(
                                textToSpeak = if (language == "ar") "حجز موعد مع الفني ${p.fullName}. الرجاء تعبئة بيانات الاسم الكامل، رقم الهاتف، والمديرية." else "Book an appointment with ${p.fullName}. Please enter your full name, phone number, and district.",
                                ttsHelper = bookingTtsHelper
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        // Full Name field
                        Text(if (language == "ar") "الاسم الكامل للمستفيد:" else "Beneficiary Full Name:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = fullNameInput,
                            onValueChange = { fullNameInput = it },
                            placeholder = { Text(if (language == "ar") "مثال: أحمد علي الوديع" else "e.g., John Doe", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth().testTag("book_fullName"),
                            singleLine = true,
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    SpeechRecognitionButton(onResult = { fullNameInput = it }, language = language)
                                    TtsSpeakerButton(textToSpeak = "الاسم الكامل للمستفيد", ttsHelper = bookingTtsHelper)
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Phone field
                        Text(if (language == "ar") "رقم الهاتف والواتساب:" else "Phone & WhatsApp:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = { phoneInput = it },
                            placeholder = { Text("77xxxxxxx", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth().testTag("book_phone"),
                            singleLine = true,
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    SpeechRecognitionButton(onResult = { phoneInput = it }, language = language)
                                    TtsSpeakerButton(textToSpeak = "رقم الهاتف والواتساب", ttsHelper = bookingTtsHelper)
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // District field
                        Text(if (language == "ar") "اسم المديرية والموقع:" else "District Name & Region:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = districtInput,
                            onValueChange = { districtInput = it },
                            placeholder = { Text(if (language == "ar") "مثال: صنعاء - معين" else "e.g., Sana'a - Ma'een", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth().testTag("book_district"),
                            singleLine = true,
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    SpeechRecognitionButton(onResult = { districtInput = it }, language = language)
                                    TtsSpeakerButton(textToSpeak = "اسم المديرية والموقع", ttsHelper = bookingTtsHelper)
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(18.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { showBookingDialogForProvider = null }) {
                                Text(if (language == "ar") "إلغاء الحجز" else "Cancel")
                            }
                            Button(
                                onClick = {
                                    if (fullNameInput.isEmpty() || phoneInput.isEmpty() || districtInput.isEmpty()) {
                                        Toast.makeText(context, if (language == "ar") "يرجى تعبئة كافة البيانات الإلزامية!" else "Please complete all fields!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.submitBooking(
                                            providerId = p.id,
                                            providerName = p.fullName,
                                            fullName = fullNameInput,
                                            phone = phoneInput,
                                            district = districtInput
                                        )
                                        showBookingDialogForProvider = null
                                        Toast.makeText(context, if (language == "ar") "تم وضع طلب حجز الخدمة بنجاح، وستتم المزامنة الفورية مع لوحة تحكم المشرف!" else "Appointment requested successfully!", Toast.LENGTH_LONG).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text(if (language == "ar") "تأكيد وإرسال الحجز 🚀" else "Confirm Booking")
                            }
                        }
                    }
                }
            }
        }

        // Report Provider Dialog
        if (showReportDialogForProvider != null) {
            val p = showReportDialogForProvider!!
            var reasonOption by remember { mutableStateOf("احتيال أو سعر مبالغ به") }
            var reportDetails by remember { mutableStateOf("") }

            Dialog(onDismissRequest = { showReportDialogForProvider = null }) {
                Card(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("🛡️ إرسال بلاغ ضد: ${p.fullName}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(12.dp))

                        Text("يرجى تحديد سبب البلاغ المباشر:", fontSize = 12.sp)
                        val items = listOf("احتيال أو سعر مبالغ به", "خدمة رديئة أو غير مهذبة", "تأخر أو عدم حضور", "إزعاج مستمر")
                        items.forEach { item ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { reasonOption = item }
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = (reasonOption == item),
                                    onClick = { reasonOption = item }
                                )
                                Text(item, fontSize = 13.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = reportDetails,
                            onValueChange = { reportDetails = it },
                            label = { Text("اكتب تفاصيل إضافية لتسريع الفحص...") },
                            modifier = Modifier.fillMaxWidth().height(80.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { showReportDialogForProvider = null }) {
                                Text("إلغاء")
                            }
                            Button(
                                onClick = {
                                    viewModel.submitReportToAdmin(p.id, reasonOption, reportDetails)
                                    showReportDialogForProvider = null
                                    Toast.makeText(context, "تم رفع البلاغ وسيقوم المشرفون بالتدقيق الفوري والمحاسبة.", Toast.LENGTH_LONG).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("تأكيد البلاغ", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN 1: Home View Directory Screen
// ==========================================
@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    categories: List<CategoryEntity>,
    banners: List<BannerEntity>,
    recommended: List<ProviderEntity>,
    providers: List<ProviderEntity>,
    searchQuery: String,
    selectedCatId: String?,
    selectedDistrict: String?,
    language: String,
    minRating: Int,
    onOpenReview: (ProviderEntity) -> Unit,
    onOpenReport: (ProviderEntity) -> Unit,
    onOpenBooking: (ProviderEntity) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val settings by viewModel.appSettings.collectAsStateWithLifecycle()
    val isMapsEnabled = settings?.isMapsEnabled ?: true

    // Marquee continuous message horizontal animation setup
    val marqueeTransition = rememberInfiniteTransition(label = "WelcomeMarquee")
    val translationX by marqueeTransition.animateFloat(
        initialValue = 400f,
        targetValue = -1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "MarqueeTranslation"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(14.dp)
    ) {
        
        // Dynamic banner/marquee welcome text row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                .padding(vertical = 8.dp)
                .height(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (language == "ar") "أهلاً ومرحباً بكم مع تطبيق كل خدمات اليمن - خدمات سباكة، كهرباء، دهان، نجارة، حدادة متميزة 👑" else "Welcome to WAM Services Yemen - Directory of Trusted Local Specialists!",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.offset(x = translationX.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Visible
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Large Ad Slider Banners
        if (banners.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .padding(bottom = 14.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.background)
                            )
                        )
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = banners.first().title, 
                            fontWeight = FontWeight.Bold, 
                            fontSize = 15.sp, 
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (language == "ar") "إعلان ممول - عروض وتخفيضات أصحاب المهن" else "Sponsored Promotion",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Fast keyword Search box
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.onSearchQueryChanged(it) },
            placeholder = { Text(if (language == "ar") "ابحث عن سباك، كهربائي، صنعاء..." else "Search plumbing, electrical, Sana'a...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = {
                IconButton(onClick = {
                    // Fast voice query simulator matches requirement
                    viewModel.onSearchQueryChanged("سباك")
                    Toast.makeText(context, if (language == "ar") "🎤 تم التقاط المدخل الفوري: سباك" else "Captured input : plumber", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(Icons.Default.Mic, contentDescription = "Voice Search")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("fast_search_bar"),
            shape = RoundedCornerShape(28.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Horizontal Category lists (speed dials widget)
        Text(
            text = if (language == "ar") "أقسام الخدمات السريعة" else "Services Categories",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                FilterChip(
                    selected = (selectedCatId == null),
                    onClick = { viewModel.selectCategory(null) },
                    label = { Text(if (language == "ar") "الكل 🌐" else "All") }
                )
            }
            items(categories.filter { it.parentId == null }) { cat ->
                FilterChip(
                    selected = (selectedCatId == cat.id),
                    onClick = { viewModel.selectCategory(cat.id) },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            CategoryImage(imageUrl = cat.imageUrl, modifier = Modifier.size(16.dp))
                            Text(cat.nameAr)
                        }
                    }
                )
            }
        }

        // VIP Elite recommendation section
        if (recommended.isNotEmpty() && selectedCatId == null) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (language == "ar") "👑 قسم النخبة VIP" else "Elite Professionals VIP",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700),
                        fontSize = 16.sp,
                    )
                    Text(
                        text = if (language == "ar") "الأفضل تقييماً" else "Top Reviewed",
                        fontSize = 11.sp,
                        color = Color(0xFFFFD700).copy(alpha = 0.8f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(recommended) { vip ->
                        Card(
                            modifier = Modifier
                                .width(170.dp)
                                .clickable {
                                    // Set focus query to VIP provider name
                                    viewModel.onSearchQueryChanged(vip.fullName)
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, Color(0xFFFFD700)) // Golden VIP border
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("VIP", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                    Icon(Icons.Default.Verified, contentDescription = null, tint = VerifiedBlue, modifier = Modifier.size(14.dp))
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(vip.fullName.take(1), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(vip.fullName, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(vip.district, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("${vip.averageRating}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Active Provider List Cards (Vertical)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${if (language == "ar") "مقدمو الخدمات المتوفرون" else "Available Service Providers"} (${providers.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        if (providers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Person, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        if (language == "ar") "عذراً! لا يوجد مهنيون يطابقون خيارات البحث حالياً." else "No active providers match criteria.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            providers.forEach { provider ->
                ProviderCard(
                    provider = provider,
                    language = language,
                    isMapsEnabled = isMapsEnabled,
                    onDialCall = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${provider.phone}"))
                        context.startActivity(intent)
                    },
                    onWhatsApp = {
                        val cleanPhone = provider.phone.trim().replace("+", "")
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone"))
                        context.startActivity(intent)
                    },
                    onDirections = {
                        // Open Google maps directly
                        val link = "geo:${provider.locationLat},${provider.locationLng}?q=${provider.locationLat},${provider.locationLng}(${provider.fullName})"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                        context.startActivity(intent)
                    },
                    onStartChat = {
                        viewModel.selectActiveChat("chat_${provider.id}")
                        Toast.makeText(context, if (language == "ar") "تم فتح نافذة الدردشة المباشرة مع الفني!" else "Chat room opened, click chat bubble to enter inbox!", Toast.LENGTH_SHORT).show()
                    },
                    onOpenReview = { onOpenReview(provider) },
                    onOpenReport = { onOpenReport(provider) },
                    onBook = { onOpenBooking(provider) }
                )
            }
        }
    }
}

// Service Provider Composable Item Card
@Composable
fun ProviderCard(
    provider: ProviderEntity,
    language: String,
    isMapsEnabled: Boolean,
    onDialCall: () -> Unit,
    onWhatsApp: () -> Unit,
    onDirections: () -> Unit,
    onStartChat: () -> Unit,
    onOpenReview: () -> Unit,
    onOpenReport: () -> Unit,
    onBook: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .testTag("provider_item_card_${provider.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Name, verification, rating
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular leading placeholder
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(provider.fullName.take(1), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(provider.fullName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        if (provider.isVerified) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Verified, 
                                contentDescription = "Verified Blue Badge", 
                                tint = VerifiedBlue,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                    Text(
                        text = "${provider.district} - ${provider.address}", 
                        fontSize = 11.sp, 
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Availability tag (Green Bullet)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(ActiveGreen.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(ActiveGreen))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (language == "ar") "متاح" else "Ready", fontSize = 10.sp, color = ActiveGreen, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Details pricing / examination fee
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${provider.averageRating}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(" (${provider.totalReviews} ${if (language == "ar") "تقييم" else "reviews"})", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
                
                Text(
                    text = "${if (language == "ar") "سعر المعاينة" else "Inspection Fee"}: 1,500 ${if (language == "ar") "ريال" else "YER"}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Expandable portfolio section
            var expanded by remember { mutableStateOf(false) }
            val workPics = provider.workImagesCSV.split(",").filter { it.isNotEmpty() }
            
            if (workPics.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { expanded = !expanded }
                    ) {
                        Text(
                            if (expanded) {
                                if (language == "ar") "🔼 إخفاء سابقة الأعمال" else "🔼 Hide Portfolio"
                            } else {
                                if (language == "ar") "🔽 عرض سابقة الأعمال (${workPics.size})" else "🔽 View Portfolio (${workPics.size})"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                AnimatedVisibility(visible = expanded) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(workPics) { imgPath ->
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                            ) {
                                coil.compose.AsyncImage(
                                    model = java.io.File(imgPath),
                                    contentDescription = "Work sample",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Utility Buttons (Call, WhatsApp, Maps, Local chat, Rate)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onDialCall,
                    modifier = Modifier.weight(1f).height(36.dp).testTag("action_dial"),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (language == "ar") "اتصال" else "Call", fontSize = 11.sp)
                }

                Button(
                    onClick = onWhatsApp,
                    modifier = Modifier.weight(1f).height(36.dp).testTag("action_whatsapp"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("💬", fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("واتساب", fontSize = 11.sp)
                }

                Button(
                    onClick = onBook,
                    modifier = Modifier.weight(1.1f).height(36.dp).testTag("action_book"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (language == "ar") "حجز موعد" else "Book", fontSize = 11.sp, color = MaterialTheme.colorScheme.onTertiary)
                }

                if (isMapsEnabled) {
                    IconButton(
                        onClick = onDirections,
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = "GPS Location Directions", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    }
                }

                IconButton(
                    onClick = onStartChat,
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Icon(Icons.Default.ChatBubbleOutline, contentDescription = "App Inbox Message", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                }

                IconButton(
                    onClick = onOpenReview,
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Icon(Icons.Default.StarBorder, contentDescription = "Rate stars", tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                }

                IconButton(
                    onClick = onOpenReport,
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.errorContainer)
                ) {
                    Icon(Icons.Default.Report, contentDescription = "Report abuse", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// ==========================================
// SCREEN 2: Login Portal Screen
// ==========================================
@Composable
fun LoginScreen(
    viewModel: AppViewModel,
    language: String,
    onSuccess: () -> Unit
) {
    var adminUser by remember { mutableStateOf("") }
    var adminPass by remember { mutableStateOf("") }
    var rememberMeChecked by remember { mutableStateOf(true) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.AdminPanelSettings, 
            contentDescription = null, 
            modifier = Modifier.size(64.dp), 
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            if (language == "ar") "تسجيل دخول المشرف العام" else "General Supervisor Control Login", 
            fontWeight = FontWeight.Bold, 
            fontSize = 18.sp
        )
        Text(
            "WAM2026 Admin Portal", 
            fontSize = 11.sp, 
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = adminUser,
            onValueChange = { adminUser = it },
            label = { Text("اسم المستخدم (Username)") },
            modifier = Modifier.fillMaxWidth().testTag("admin_user_field"),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedLabelColor = Color(0xFFFFD700),
                unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                focusedBorderColor = Color(0xFFFFD700),
                unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
            )
        )
        
        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = adminPass,
            onValueChange = { adminPass = it },
            label = { Text("كلمة المرور (Password)") },
            modifier = Modifier.fillMaxWidth().testTag("admin_pass_field"),
            singleLine = true,
            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedLabelColor = Color(0xFFFFD700),
                unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                focusedBorderColor = Color(0xFFFFD700),
                unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = rememberMeChecked, onCheckedChange = { rememberMeChecked = it })
            Text(if (language == "ar") "تذكر تواجدي وتسجيل دخولي" else "Remember me on this app", fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                viewModel.loginAsAdmin(adminUser, adminPass,
                    success = {
                        Toast.makeText(context, if (language == "ar") "مرحباً يا أدمن: تم المزامنة والدخول بنجاح!" else "Welcome admin!", Toast.LENGTH_SHORT).show()
                        onSuccess()
                    },
                    fail = {
                        Toast.makeText(context, if (language == "ar") "الاسم أو الرمز السري غير صحيح!" else "Invalid Supervisor credentials!", Toast.LENGTH_SHORT).show()
                    }
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("admin_login_submit_btn")
        ) {
            Text(if (language == "ar") "تأكيد الدخول" else "Login")
        }
    }
}

// ==========================================
// SCREEN 3: Provider Register Sign-up Form Screen
// ==========================================
@Composable
fun RegisterProviderScreen(
    viewModel: AppViewModel,
    categories: List<CategoryEntity>,
    terms: List<RegistrationTermEntity>,
    language: String,
    onSuccess: () -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var district by remember { mutableStateOf("") }
    var mainCategoryId by remember { mutableStateOf("c1") }
    var subCategoryId by remember { mutableStateOf("sub1") }
    
    var genderIsMale by remember { mutableStateOf(true) }
    var profileImageUri by remember { mutableStateOf("") }
    var idCardImageUri by remember { mutableStateOf("") }
    var workImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    
    var localPicUploaded by remember { mutableStateOf(false) }
    var nationalCardUploaded by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val settings by viewModel.appSettings.collectAsStateWithLifecycle()
    val maxWorkPics = settings?.maxWorkImages ?: 5

    val profileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            profileImageUri = uri.toString()
            localPicUploaded = true
        }
    }

    val idCardLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            idCardImageUri = uri.toString()
            nationalCardUploaded = true
        }
    }

    val workImagesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        workImageUris = uris.take(maxWorkPics)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(20.dp)
    ) {
        Text(
            if (language == "ar") "👤 تقديم طلب تسجيل مهني" else "Register a Professional Account",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            if (language == "ar") "سجل معلوماتك بدقة للمطابقة والتحقق الفوري مع الشارة الزرقاء المميزة." else "Fill registration parameters accurately to achieve verification.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        
        val ttsHelper = rememberTtsHelper()

        Spacer(modifier = Modifier.height(20.dp))

        // Full Name
        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text(if (language == "ar") "الاسم الثلاثي الكامل" else "Full Name (as ID)") },
            modifier = Modifier.fillMaxWidth().testTag("reg_fullName"),
            singleLine = true,
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SpeechRecognitionButton(onResult = { fullName = it }, language = language)
                    TtsSpeakerButton(textToSpeak = "الاسم الثلاثي الكامل", ttsHelper = ttsHelper)
                }
            }
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Phones
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text(if (language == "ar") "رقم الهاتف / واتساب" else "Phone / Whatsapp") },
            modifier = Modifier.fillMaxWidth().testTag("reg_phone"),
            singleLine = true,
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SpeechRecognitionButton(onResult = { phone = it }, language = language)
                    TtsSpeakerButton(textToSpeak = "رقم الهاتف والواتساب", ttsHelper = ttsHelper)
                }
            }
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Dropdown main category selection
        Text(if (language == "ar") "اختر تخصصك المهني الرئيسي:" else "Select Primary Profession:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories.filter { it.parentId == null }) { cat ->
                val selected = (mainCategoryId == cat.id)
                FilterChip(
                    selected = selected,
                    onClick = { 
                        mainCategoryId = cat.id 
                        // Automatically shift default sub category
                        val subs = categories.filter { it.parentId == cat.id }
                        if (subs.isNotEmpty()) subCategoryId = subs.first().id
                    },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            CategoryImage(imageUrl = cat.imageUrl, modifier = Modifier.size(16.dp))
                            Text(cat.nameAr)
                        }
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))

        // Addressing
        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text(if (language == "ar") "عنوان العمل التفصيلي" else "Work Address") },
            modifier = Modifier.fillMaxWidth().testTag("reg_address"),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(12.dp))

        // District
        OutlinedTextField(
            value = district,
            onValueChange = { district = it },
            label = { Text(if (language == "ar") "المنطقة والموقع السكني" else "Residential District") },
            modifier = Modifier.fillMaxWidth().testTag("reg_district"),
            singleLine = true,
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SpeechRecognitionButton(onResult = { district = it }, language = language)
                    TtsSpeakerButton(textToSpeak = "المنطقة السكنية أو المديرية", ttsHelper = ttsHelper)
                }
            }
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Gender Row
        Spacer(modifier = Modifier.height(12.dp))
        Text(if (language == "ar") "تحديد الجنس (لمراعاة الخصوصية):" else "Gender (for privacy parameters):", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = genderIsMale, onClick = { genderIsMale = true })
                Text(if (language == "ar") "ذكر (صورة سيلفي)" else "Male (Selfie Portrait)", fontSize = 12.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = !genderIsMale, onClick = { genderIsMale = false })
                Text(if (language == "ar") "أنثى (صورة تعبيرية للمهنة)" else "Female (Profession Image)", fontSize = 12.sp)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Image pick buttons: Simulated Camera and galleries
        Text(if (language == "ar") "رفع المستندات الثبوتية والمظهر" else "Documents and ID Verification Upload", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { 
                    profileLauncher.launch("image/*")
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    if (genderIsMale) {
                        if (language == "ar") "📷 صـورة شخصية" else "Portrait Selfie"
                    } else {
                        if (language == "ar") "🎨 صورة تعبيرية للمهنة" else "Profession Image"
                    }
                )
            }

            Button(
                onClick = { 
                    idCardLauncher.launch("image/*")
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                modifier = Modifier.weight(1f)
            ) {
                Text(if (language == "ar") "🖼️ بطاقة الهوية" else "Gallery ID card")
            }
        }

        // Upload checkers
        if (localPicUploaded) {
            Text("✅ صورتك الشخصية جاهزة للرفع والضغط والتصغير", fontSize = 11.sp, color = ActiveGreen, modifier = Modifier.padding(bottom = 4.dp))
        } else if (!genderIsMale) {
            Text("ℹ️ يُسمح للفتيات برفع صورة تعبيرية للمهنة بدلاً من السيلفي للخصوصية", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 4.dp))
        }
        if (nationalCardUploaded) {
            Text("✅ صورة الهوية الوطنية جاهزة ومشفرة للتحقق", fontSize = 11.sp, color = ActiveGreen)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Portfolio Work Images Choice
        Text(if (language == "ar") "نماذج من أعمالك وصور خدمتك السابقة:" else "Work Portfolio Samples:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Button(
            onClick = { workImagesLauncher.launch("image/*") },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (language == "ar") "🖼️ اختر نماذج الأعمال (الحد الأقصى: $maxWorkPics)" else "🖼️ Choose Work Samples (Max: $maxWorkPics)")
        }
        
        if (workImageUris.isNotEmpty()) {
            Text(
                text = if (language == "ar") "تم تحديد ${workImageUris.size} صور للضغط التلقائي الـ WebP:" else "Selected ${workImageUris.size} images for automated WebP compression:",
                fontSize = 11.sp,
                color = ActiveGreen,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(workImageUris) { uri ->
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
                    ) {
                        Text("WebP", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Terms guidelines checklist
        Text(if (language == "ar") "شروط وقوانين التسجيل:" else "Registration Terms:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        terms.forEachIndexed { idx, term ->
            Text("${idx+1}. ${term.termText}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), modifier = Modifier.padding(vertical = 2.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (fullName.isEmpty() || phone.isEmpty() || address.isEmpty() || district.isEmpty()) {
                    Toast.makeText(context, if (language == "ar") "فضلاً أكمل كافة الحقول الإجبارية!" else "Please satisfy all inputs!", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.submitRegistration(
                        fullName = fullName,
                        phone = phone,
                        mainCatId = mainCategoryId,
                        subCatId = subCategoryId,
                        address = address,
                        district = district,
                        profileUri = profileImageUri,
                        idCardUri = idCardImageUri,
                        workUris = workImageUris.map { it.toString() }
                    ) {
                        Toast.makeText(context, if (language == "ar") "تم تقديم طلبك بنجاح للمراجعة المشرفة الفورية!" else "Form submitted for instant supervisor review!", Toast.LENGTH_LONG).show()
                        onSuccess()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("reg_submit_btn")
        ) {
            Text(if (language == "ar") "تقديم طلب الانضمام للمراجعة الفورية" else "Submit Application")
        }
    }
}

// ==========================================
// SCREEN 4: Interactive Live Chat Inbox Screen
// ==========================================
@Composable
fun LiveChatScreen(
    viewModel: AppViewModel,
    chats: List<ChatEntity>,
    messages: List<MessageEntity>,
    language: String
) {
    val context = LocalContext.current
    var chatMessageText by remember { mutableStateOf("") }
    val ttsHelper = rememberTtsHelper()
    
    // Choose active chat session or placeholder
    val activeChatId by viewModel.activeChatId.collectAsStateWithLifecycle()
    
    Row(modifier = Modifier.fillMaxSize()) {
        // Chat threads channels list
        Column(
            modifier = Modifier
                .width(130.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                .border(width = 0.5.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                .padding(4.dp)
        ) {
            Text(if (language == "ar") "💬 المراسلات" else "Inbox Sessions", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(4.dp))
            Spacer(modifier = Modifier.height(4.dp))
            
            LazyColumn {
                items(chats) { c ->
                    val isSelected = (c.chatId == activeChatId)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { viewModel.selectActiveChat(c.chatId) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.background
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(6.dp)) {
                            Text(c.chatId.takeLast(6), fontWeight = FontWeight.Bold, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                            Text(c.lastMessage, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }

        // Active chat message exchange list
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            if (activeChatId == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(if (language == "ar") "اختر جلسة مراسلة من القائمة اليسرى للبدء" else "Choose a messaging channel to view history", fontSize = 12.sp, textAlign = TextAlign.Center)
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Chat thread header title
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ChatBubble, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (language == "ar") "محادثة مشفّرة فورية" else "Active Thread : ${activeChatId!!.takeLast(6)}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    // Conversation texts Lazy
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(12.dp)
                    ) {
                        items(messages) { m ->
                            val isMeSender = (m.senderId == "visitor_user" || m.senderId == "WAM2026")
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = if (isMeSender) Arrangement.End else Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isMeSender) {
                                    TtsSpeakerButton(textToSpeak = m.message, ttsHelper = ttsHelper)
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(
                                            topStart = 12.dp,
                                            topEnd = 12.dp,
                                            bottomStart = if (isMeSender) 12.dp else 0.dp,
                                            bottomEnd = if (isMeSender) 0.dp else 12.dp
                                        ))
                                        .background(
                                            if (isMeSender) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.secondaryContainer
                                        )
                                        .padding(10.dp)
                                        .widthIn(max = 180.dp)
                                ) {
                                    Text(m.message, fontSize = 12.sp, color = if (isMeSender) Color.Black else Color.White)
                                }
                                if (!isMeSender) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    TtsSpeakerButton(textToSpeak = m.message, ttsHelper = ttsHelper)
                                }
                            }
                        }
                    }

                    // Input action row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = chatMessageText,
                            onValueChange = { chatMessageText = it },
                            placeholder = { Text(if (language == "ar") "اكتب رسالة فورية..." else "Type instant message...", fontSize = 12.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_input_message"),
                            shape = RoundedCornerShape(20.dp),
                            singleLine = true,
                            trailingIcon = {
                                SpeechRecognitionButton(onResult = { chatMessageText = it }, language = language)
                            }
                        )
                        
                        Spacer(modifier = Modifier.width(6.dp))
                        
                        IconButton(
                            onClick = {
                                if (chatMessageText.trim().isNotEmpty()) {
                                    viewModel.sendChatMessage(
                                        receiverId = activeChatId!!.replace("chat_", ""),
                                        text = chatMessageText
                                    )
                                    chatMessageText = ""
                                }
                            },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                .size(36.dp)
                                .testTag("chat_send_btn")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.Black, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN 5: About Screen Composable Panel
// ==========================================
@Composable
fun AboutAppScreen(
    settings: AppSettingEntity?,
    language: String,
    isConnected: Boolean
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text("WAM", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 24.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            settings?.appName ?: "دليل خدمات اليمن",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "Yemen Pro Services Directory Hub",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(if (language == "ar") "معلومات الدعم الفني والمراسلة:" else "Technical Support Parameters:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(10.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                    Text("📞 هاتف الدعم: ", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(getMaskedText(settings?.supportPhone ?: "777644670", isConnected), fontSize = 12.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                    Text("📧 الإيميل: ", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(settings?.supportEmail ?: "support@wam2026.com", fontSize = 12.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                    Text("💬 واتساب: ", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(getMaskedText(settings?.supportWhatsApp ?: "+967777644670", isConnected), fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            if (language == "ar") "يربط هذا التطبيق المستخدمين بمختلف الحرفيين والكهربائيين والسباكين ذوي الكفاءة العالية في المحافظات اليمنية مع مزامنة فورية كاملة وحماية وضمانة للمستخدم بموجب البلاغات والتقييمات الذكية." 
            else "WAM Services seamlessly bridges the gap between home users and best vetted plumbing, electrical, and mechanical providers in Yemen.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

// ==========================================
// SCREEN 6: Super Admin / Control Dashboard Screen
// ==========================================
@Composable
fun AdminDashboardScreen(
    viewModel: AppViewModel,
    categories: List<CategoryEntity>,
    allProviders: List<ProviderEntity>,
    pendingProviders: List<PendingProviderEntity>,
    banners: List<BannerEntity>,
    reports: List<ReportEntity>,
    chats: List<ChatEntity>,
    logs: List<ActivityLogEntity>,
    terms: List<RegistrationTermEntity>,
    language: String,
    settings: AppSettingEntity?,
    onOpenBackdoorSettings: () -> Unit,
    onLogout: () -> Unit
) {
    var selectedTabIdx by remember { mutableStateOf(0) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var enteredPassword by remember { mutableStateOf("") }
    
    val bookings by viewModel.bookings.collectAsStateWithLifecycle(initialValue = emptyList())
    val notifications by viewModel.notifications.collectAsStateWithLifecycle(initialValue = emptyList())
    
    val tabTitles = listOf(
        "الإحصائيات", 
        "الطلبات المعلقة", 
        "أضف يدوياً", 
        "قائمة المزودين", 
        "الأقسام والبنود", 
        "التقارير وسجل النظام", 
        "تنظيف الصور",
        "الحجوزات 📅",
        "الإشعارات 🔔",
        "الدردشات الفورية 💬"
    )
    
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        // Tab row
        ScrollableTabRow(
            selectedTabIndex = selectedTabIdx,
            edgePadding = 8.dp,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = (selectedTabIdx == index),
                    onClick = { selectedTabIdx = index },
                    text = { Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp)
        ) {
            when (selectedTabIdx) {
                0 -> {
                    // TAB 1: Live Stats Charts
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            Text("📊 الإحصائيات الفورية والعمليات الحية", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(14.dp))
                            
                            // Stats grid cards
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Card(modifier = Modifier.weight(1f)) {
                                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("المهنيين النشطين", fontSize = 11.sp)
                                        Text("${allProviders.size}", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                Card(modifier = Modifier.weight(1f)) {
                                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("طلبات المراجعة", fontSize = 11.sp)
                                        Text("${pendingProviders.size}", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color(0xFFFFD700))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Card(modifier = Modifier.weight(1f)) {
                                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("البلاغات المرفوعة", fontSize = 11.sp)
                                        Text("${reports.size}", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                                Card(modifier = Modifier.weight(1f)) {
                                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("عقد المراسلات", fontSize = 11.sp)
                                        Text("${chats.size}", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = VerifiedBlue)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Owner Special Quick Access Panel
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("🛠️ البوابة السرية للمالك والمصمم", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("متاحة لتعديل ألوان وثيمات التطبيق، تغيير الاسم الترويجي والتذييل وقنوات الدعم المباشر ومزامنتها لحظياً لجداول Firestore.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(onClick = onOpenBackdoorSettings, modifier = Modifier.fillMaxWidth().testTag("trigger_backdoor_control")) {
                                        Text("افتح البوابة السرية للتحكم الفوري")
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Button(
                                onClick = {
                                    viewModel.logout()
                                    onLogout()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("تسجيل الخروج من الإدارة", color = Color.White)
                            }
                        }
                    }
                }
                1 -> {
                    // TAB 2: Approve / Reject requests
                    Text("⏳ مراجعة طلبات الانضمام المعلقة (${pendingProviders.filter { it.status == "pending" }.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    if (pendingProviders.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("لا توجد طلبات معلقة بانتظار المراجعة حالياً.")
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(pendingProviders) { pending ->
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(pending.fullName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("الهاتف: ${pending.phone} | المنطقة: ${pending.district}", fontSize = 11.sp)
                                        Text("العنوان المهني: ${pending.address}", fontSize = 11.sp)
                                        Text("الحالة الحالية: ${pending.status}", fontSize = 11.sp, color = if (pending.status == "pending") Color(0xFFFFD700) else Color.Red)
                                        
                                        if (pending.status == "pending") {
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Button(
                                                    onClick = { viewModel.approveRegistration(pending.id) },
                                                    modifier = Modifier.weight(1f),
                                                    colors = ButtonDefaults.buttonColors(containerColor = ActiveGreen)
                                                ) {
                                                    Text("موافقة وقبول", color = Color.White)
                                                }
                                                Button(
                                                    onClick = { viewModel.rejectRegistration(pending.id, "المستندات أو الهوية غير واضحة") },
                                                    modifier = Modifier.weight(1f),
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                                ) {
                                                    Text("رفض مع السبب", color = Color.White)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // TAB 3: Manually add provider
                    var pName by remember { mutableStateOf("") }
                    var pPhone by remember { mutableStateOf("") }
                    var pAddr by remember { mutableStateOf("") }
                    var pDist by remember { mutableStateOf("") }
                    var pVipChecked by remember { mutableStateOf(false) }
                    
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text("👤 إضافة مزود خدمة يدوياً ودون موافقة فنية مسبقة", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(value = pName, onValueChange = { pName = it }, label = { Text("الاسم المهني الثلاثي") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(value = pPhone, onValueChange = { pPhone = it }, label = { Text("رقم الهاتف") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(value = pAddr, onValueChange = { pAddr = it }, label = { Text("عنوان العمل") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(value = pDist, onValueChange = { pDist = it }, label = { Text("المنطقة السكنية") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = pVipChecked, onCheckedChange = { pVipChecked = it })
                            Text("منح وسام النخبة VIP والتثبيت بالقمة مباشرة 🌟", fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                if (pName.isNotEmpty() && pPhone.isNotEmpty()) {
                                    viewModel.addProviderManually(pName, pPhone, "c1", "sub1", pAddr, pDist, pVipChecked) {
                                        pName = ""
                                        pPhone = ""
                                        pAddr = ""
                                        pDist = ""
                                        Toast.makeText(context, "تم إضافة الفني بنجاح إلى الدليل مباشرة!", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "الاسم والرقم إجباريين!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("إضافة مباشر للدليل 🚀")
                        }
                    }
                }
                3 -> {
                    // TAB 4: Manage Active Providers List
                    Text("👥 إدارة المهنيين النشطين في الدليل", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(allProviders) { provider ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(provider.fullName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        IconButton(onClick = { viewModel.deleteProviderActive(provider.id) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                    Text("الهاتف: ${provider.phone} | المنطقة: ${provider.district}", fontSize = 11.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    // Control VIP checkboxes
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
                4 -> {
                    // TAB 5: Manage Categories and registration terms
                    var catAr by remember { mutableStateOf("") }
                    var catEn by remember { mutableStateOf("") }
                    var catIcon by remember { mutableStateOf("🔧") }
                    
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text("🔨 إضافة وتحديث أقسام الدليل", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedTextField(value = catAr, onValueChange = { catAr = it }, label = { Text("الاسم عربي") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = catEn, onValueChange = { catEn = it }, label = { Text("إنجليزي") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = catIcon, onValueChange = { catIcon = it }, label = { Text("أيقونة") }, modifier = Modifier.width(60.dp))
                        }
                        Button(
                            onClick = {
                                if (catAr.isNotEmpty()) {
                                    viewModel.addCategory(catAr, catEn, catIcon)
                                    catAr = ""
                                    catEn = ""
                                    Toast.makeText(context, "تمت إضافة وتحديث القسم بنجاح!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("حفظ التحديث لقوائم الأقسام")
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text("📋 بنود التسجيل وشروط الموثوقية", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        LazyColumn(modifier = Modifier.weight(1f).padding(top = 4.dp)) {
                            items(terms) { term ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("• ${term.termText}", fontSize = 11.sp, modifier = Modifier.weight(1f))
                                    IconButton(onClick = { viewModel.deleteTerm(term.id) }) {
                                        Icon(Icons.Default.RemoveCircle, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
                5 -> {
                    // TAB 6: Reports & Privacy Logs Database Control
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        // User reports
                        item {
                            Text("🛡️ تقارير تدقيق وبلاغات المستخدمين المعلقة (${reports.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        items(reports) { rep ->
                            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("رقم المهني: ${rep.providerId}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("السبب: ${rep.reason}", fontSize = 11.sp)
                                    Text("التفاصيل: ${rep.details}", fontSize = 11.sp)
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                        TextButton(onClick = { viewModel.removeReport(rep.id) }) {
                                            Text("حل وتفريغ البلاغ", color = ActiveGreen, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Local backup and clean tools
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("💾 النسخ الاحتياطي وحماية سجل المراسلات", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Button(
                                onClick = {
                                    viewModel.performBackup { path ->
                                        Toast.makeText(context, "تم حفظ النسخة الاحتياطية بنجاح في المسار:\n$path", Toast.LENGTH_LONG).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تصدير نسخة احتياطية حية")
                            }
                            
                            Spacer(modifier = Modifier.height(6.dp))

                            Button(
                                onClick = {
                                    showClearHistoryDialog = true
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("مسح سجل المراسلات بالكامل ونهائياً", color = Color.White)
                            }
                        }
                    }
                }
                6 -> {
                    ImageManagementTab(
                        allActiveProviders = allProviders,
                        pendingProviders = pendingProviders,
                        language = language
                    )
                }
                7 -> {
                    // TAB 7: Manage Bookings Screen
                    var bookingSearchQuery by remember { mutableStateOf("") }
                    val adminTtsHelper = rememberTtsHelper()
                    
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📅 إدارة مواعيد وحجوزات الخدمات المباشرة",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            TtsSpeakerButton(
                                textToSpeak = "إدارة مواعيد وحجوزات الخدمات المباشرة. هنا تظهر طلبات حجز المستفيدين مع الفنيين للتأكيد الفوري.",
                                ttsHelper = adminTtsHelper
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = bookingSearchQuery,
                            onValueChange = { bookingSearchQuery = it },
                            placeholder = { Text("ابحث بالصوت أو النص (اسم المستفيد، الهاتف، الفني)...", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth().testTag("booking_search_input"),
                            singleLine = true,
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    SpeechRecognitionButton(onResult = { bookingSearchQuery = it }, language = language)
                                }
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        val filteredBookings = bookings.filter {
                            it.fullName.contains(bookingSearchQuery, ignoreCase = true) ||
                            it.phone.contains(bookingSearchQuery, ignoreCase = true) ||
                            it.providerName.contains(bookingSearchQuery, ignoreCase = true) ||
                            it.district.contains(bookingSearchQuery, ignoreCase = true)
                        }
                        
                        if (filteredBookings.isEmpty()) {
                            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("لا توجد طلبات حجز مطابقة لخيارات البحث حالياً.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredBookings) { booking ->
                                    val statusColor = when (booking.status.lowercase()) {
                                        "pending" -> Color(0xFFF2A30F)
                                        "confirmed" -> Color(0xFF25D366)
                                        "completed" -> Color(0xFF2196F3)
                                        else -> Color(0xFFE53935)
                                    }
                                    
                                    val statusText = when (booking.status.lowercase()) {
                                        "pending" -> "قيد المراجعة ⏳"
                                        "confirmed" -> "مؤكد ومقبول 🟢"
                                        "completed" -> "تم الإنجاز بنجاح 🔵"
                                        else -> "تم الإلغاء 🔴"
                                    }
                                    
                                    Card(
                                        modifier = Modifier.fillMaxWidth().testTag("admin_booking_item_${booking.id}"),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("طلب حجز #${booking.id.takeLast(4)}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(statusText, color = statusColor, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(end = 4.dp))
                                                    TtsSpeakerButton(
                                                        textToSpeak = "طلب حجز من المستفيد ${booking.fullName} للفني ${booking.providerName} بالمنطقة ${booking.district}، حالة الحجز هي ${booking.status}",
                                                        ttsHelper = adminTtsHelper
                                                    )
                                                }
                                            }
                                            
                                            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant)
                                            
                                            Text("👤 المستفيد: ${booking.fullName}", fontSize = 12.sp)
                                            Text("📞 هاتف وتساب: ${booking.phone}", fontSize = 12.sp)
                                            Text("📍 المنطقة/المديرية: ${booking.district}", fontSize = 12.sp)
                                            Text("🛠️ الفني المطلوب: ${booking.providerName}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                            
                                            Spacer(modifier = Modifier.height(10.dp))
                                            
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                if (booking.status == "pending") {
                                                    Button(
                                                        onClick = { viewModel.updateBookingStatus(booking.id, "confirmed") },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                                        modifier = Modifier.weight(1f).height(30.dp),
                                                        contentPadding = PaddingValues(0.dp)
                                                    ) {
                                                        Text("تأكيد حجز 🟢", fontSize = 10.sp, color = Color.Black)
                                                    }
                                                }
                                                
                                                if (booking.status == "confirmed") {
                                                    Button(
                                                        onClick = { viewModel.updateBookingStatus(booking.id, "completed") },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                                                        modifier = Modifier.weight(1f).height(30.dp),
                                                        contentPadding = PaddingValues(0.dp)
                                                    ) {
                                                        Text("تم الإنجاز 🔵", fontSize = 10.sp, color = Color.White)
                                                    }
                                                }
                                                
                                                if (booking.status != "cancelled" && booking.status != "completed") {
                                                    Button(
                                                        onClick = { viewModel.updateBookingStatus(booking.id, "cancelled") },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                                                        modifier = Modifier.weight(1f).height(30.dp),
                                                        contentPadding = PaddingValues(0.dp)
                                                    ) {
                                                        Text("إلغاء الموعد 🔴", fontSize = 10.sp, color = Color.White)
                                                    }
                                                }
                                                
                                                IconButton(
                                                    onClick = { viewModel.deleteBooking(booking.id) },
                                                    modifier = Modifier.size(30.dp).background(MaterialTheme.colorScheme.errorContainer, CircleShape)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "حذف نهائي", tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(14.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                8 -> {
                    // TAB 8: Notifications Send and Registry Dashboard
                    var notificationTitle by remember { mutableStateOf("") }
                    var notificationBody by remember { mutableStateOf("") }
                    var notificationType by remember { mutableStateOf("general") }
                    var targetIdInput by remember { mutableStateOf("") }
                    val adminNotificationTts = rememberTtsHelper()
                    
                    Column(modifier = Modifier.fillMaxSize()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("📢 إرسال وتعميم إشعار فوري جديد", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.weight(1f))
                                    TtsSpeakerButton(
                                        textToSpeak = "أداة إرسال إشعار فوري جديد لكافة أجهزة المستخدمين في نفس اللحظة بمزامنة حية.",
                                        ttsHelper = adminNotificationTts
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                OutlinedTextField(
                                    value = notificationTitle,
                                    onValueChange = { notificationTitle = it },
                                    label = { Text("عنوان الإشعار", fontSize = 11.sp) },
                                    modifier = Modifier.fillMaxWidth().testTag("notif_title_input"),
                                    singleLine = true,
                                    trailingIcon = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            SpeechRecognitionButton(onResult = { notificationTitle = it }, language = language)
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                
                                OutlinedTextField(
                                    value = notificationBody,
                                    onValueChange = { notificationBody = it },
                                    label = { Text("محتوى البث أو الرسالة بالإنجليزي أو العربي", fontSize = 11.sp) },
                                    modifier = Modifier.fillMaxWidth().height(70.dp).testTag("notif_body_input"),
                                    trailingIcon = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            SpeechRecognitionButton(onResult = { notificationBody = it }, language = language)
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                
                                Text("نوع البث والتوجيه الجغرافي:", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("general", "targeted_user").forEach { type ->
                                        val selected = (notificationType == type)
                                        FilterChip(
                                            selected = selected,
                                            onClick = { notificationType = type },
                                            label = { Text(if (type == "general") "عام للجميع 🌍" else "مخصص لفرد 👤", fontSize = 10.sp) }
                                        )
                                    }
                                }
                                
                                if (notificationType != "general") {
                                    OutlinedTextField(
                                        value = targetIdInput,
                                        onValueChange = { targetIdInput = it },
                                        label = { Text("رقم المعرف المستهدف (ID)", fontSize = 11.sp) },
                                        modifier = Modifier.fillMaxWidth().testTag("notif_target_input"),
                                        singleLine = true
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Button(
                                    onClick = {
                                        if (notificationTitle.isEmpty() || notificationBody.isEmpty()) {
                                            Toast.makeText(context, "الرجاء تعبئة العنوان والمحتوى للإرسال!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            viewModel.sendSystemNotification(
                                                title = notificationTitle,
                                                body = notificationBody,
                                                type = notificationType,
                                                targetId = if (notificationType == "general") "" else targetIdInput
                                            )
                                            notificationTitle = ""
                                            notificationBody = ""
                                            targetIdInput = ""
                                            Toast.makeText(context, "تم إرسال ونشر الإشعار لحظياً لكافة الجداول وحسابات العملاء المفتوحة!", Toast.LENGTH_LONG).show()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(36.dp).testTag("send_notif_button")
                                ) {
                                    Text("بث وتعميم الإشعار فورا 🚀", fontSize = 11.sp)
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("📜 سجل التنبيهات المنشورة اليوم", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            if (notifications.isNotEmpty()) {
                                TextButton(onClick = { viewModel.clearAllNotifications() }) {
                                    Text("حذف السجل بالكامل 🗑️", fontSize = 10.sp, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                        
                        if (notifications.isEmpty()) {
                            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("سجل الإشعارات فارغ ولا توجد تنبيهات سابقة.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(notifications) { notif ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth().testTag("notif_item_${notif.id}"),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Announcement, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(notif.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                                TtsSpeakerButton(
                                                    textToSpeak = "${notif.title}. ${notif.body}",
                                                    ttsHelper = adminNotificationTts
                                                )
                                                IconButton(
                                                    onClick = { viewModel.deleteNotification(notif.id) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(Icons.Default.Close, contentDescription = "مسح", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), modifier = Modifier.size(12.dp))
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(notif.body, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("نوع التوجيه: ${notif.type} | معرف الهدف: ${if (notif.targetId.isEmpty()) "عام للكل" else notif.targetId}", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                9 -> {
                    // TAB 9: Manage instant chat screen
                    val chatTtsHelper = rememberTtsHelper()
                    
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "💬 أداة الرصد والمحاكاة الفورية للمحادثات المباشرة",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            TtsSpeakerButton(
                                textToSpeak = "أداة الرصد والمحاكاة الفورية لمحادثات المشرفين والشركاء المفتوحة.",
                                ttsHelper = chatTtsHelper
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        if (chats.isEmpty()) {
                            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("لا توجد غرف محادثة نشطة في قاعدة البيانات للتتبع الفوري.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(chats) { chat ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth().testTag("admin_chat_tracking_item_${chat.chatId}"),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("غرفة محادثة لقناة: ${chat.chatId}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text("أعضاء الغرفة: ${chat.participantsCSV.take(30)}...", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                                                Text("آخر رسالة: ${chat.lastMessage}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                                            }
                                            
                                            Spacer(modifier = Modifier.width(8.dp))
                                            
                                            Button(
                                                onClick = {
                                                    viewModel.selectActiveChat(chat.chatId)
                                                    Toast.makeText(context, "تم تحديد قناة المحادثة ${chat.chatId} بنجاح. انقر أيقونة الدردشة الفورية بالأسفل لفتح النافذة ومراسلة الشركاء!", Toast.LENGTH_LONG).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                modifier = Modifier.height(32.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                                            ) {
                                                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("مراسلة 💬", fontSize = 10.sp, color = Color.Black)
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

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { 
                showClearHistoryDialog = false 
                enteredPassword = ""
            },
            title = {
                Text(
                    text = if (language == "ar") "تأكيد مسح كافة البيانات" else "Confirm Data Clear",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Column {
                    Text(
                        text = if (language == "ar") 
                            "تحذير! هذه العملية ستقوم بحذف وسحب كافة سجلات المراسلات والمحادثات بالكامل من قاعدة البيانات. يرجى إدخال كلمة مرور المشرف للتأكيد لمتابعة الحفظ:" 
                            else "Warning! This will permanently delete all chat history. Please enter the supervisor password to proceed:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = enteredPassword,
                        onValueChange = { enteredPassword = it },
                        label = { Text(if (language == "ar") "كلمة المرور" else "Password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (enteredPassword == "maher736462") {
                            viewModel.clearHistory()
                            Toast.makeText(context, if (language == "ar") "تم مسح كابلات وسجل المحادثات بالكامل بنجاح!" else "All chat records successfully purged!", Toast.LENGTH_SHORT).show()
                            showClearHistoryDialog = false
                            enteredPassword = ""
                        } else {
                            Toast.makeText(context, if (language == "ar") "خطأ! كلمة المرور غير صحيحة، تم إلغاء العملية لحماية البيانات." else "Error! Incorrect password. Process canceled to protect data.", Toast.LENGTH_LONG).show()
                            showClearHistoryDialog = false
                            enteredPassword = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (language == "ar") "تأكيد المسح" else "Confirm Purge")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showClearHistoryDialog = false 
                        enteredPassword = ""
                    }
                ) {
                    Text(if (language == "ar") "إلغاء الأمر" else "Cancel")
                }
            }
        )
    }
}

// Helper Composable for Image Space Management & Unlinked File Cleanup
@Composable
fun ImageManagementTab(
    allActiveProviders: List<ProviderEntity>,
    pendingProviders: List<PendingProviderEntity>,
    language: String
) {
    val context = LocalContext.current
    var totalCacheSize by remember { mutableStateOf(0L) }
    var allImageFiles by remember { mutableStateOf<List<java.io.File>>(emptyList()) }
    var unlinkedFiles by remember { mutableStateOf<List<java.io.File>>(emptyList()) }
    var isScanning by remember { mutableStateOf(true) }

    val refreshScan = {
        isScanning = true
        val cacheFolder = java.io.File(context.filesDir, "compressed_images")
        val files = if (cacheFolder.exists()) {
            cacheFolder.listFiles()?.toList() ?: emptyList()
        } else emptyList()

        // Gather all active and pending images URIs/strings from database
        val registeredUris = allActiveProviders.flatMap { p ->
            val works = p.workImagesCSV.split(",").filter { it.isNotEmpty() }
            works + listOf(p.profileImageUrl, p.idCardImageUrl)
        }.filter { it.isNotEmpty() }.toSet()

        val pendingUris = pendingProviders.flatMap { p ->
            val works = p.workImagesCSV.split(",").filter { it.isNotEmpty() }
            works + listOf(p.profileImageUrl, p.idCardImageUrl)
        }.filter { it.isNotEmpty() }.toSet()

        val databaseUrisSet = (registeredUris + pendingUris).map { java.io.File(it).absolutePath }.toSet()

        val unlinked = files.filter { file ->
            !databaseUrisSet.contains(file.absolutePath)
        }

        allImageFiles = files
        unlinkedFiles = unlinked
        totalCacheSize = files.sumOf { it.length() }
        isScanning = false
    }

    LaunchedEffect(Unit) {
        refreshScan()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            if (language == "ar") "🖼️ تقرير وجرد استهلاك مساحة الصور" else "🖼️ Image Space Consumption & Storage Report",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            if (language == "ar") "يقوم التطبيق بضغط وتحويل كافة الملفات الشخصية والبطاقات وصور المهارات لتنسيق WebP تلقائياً لتوفير المساحة ومسح التالف." else "The engine compresses portfolios/selfies automatically to WebP format to save memory.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (isScanning) {
            Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (language == "ar") "إجمالي عدد الصور المخزنة:" else "Total cached file assets:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("${allImageFiles.size} صور", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (language == "ar") "مساحة استهلاك المجلد الإجمالي:" else "Total storage size used:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        val sizeKb = totalCacheSize / 1024
                        val sizeString = if (sizeKb > 1024) "${sizeKb / 1024} MB" else "$sizeKb KB"
                        Text(sizeString, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (language == "ar") "صور غير مرتبطة بقاعدة البيانات (بقايا):" else "Unlinked legacy artifacts:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = InactiveRed)
                        Text("${unlinkedFiles.size} صور", fontSize = 13.sp, color = InactiveRed, fontWeight = FontWeight.Black)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = refreshScan,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
            ) {
                Text(if (language == "ar") "🔄 إعادة فحص الذاكرة والهاتف" else "🔄 Rescan Local Directory")
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    var deletedCount = 0
                    unlinkedFiles.forEach { file ->
                        if (file.delete()) deletedCount++
                    }
                    Toast.makeText(context, if (language == "ar") "🗑️ تم مسح $deletedCount صور زائدة لتسريع وتحرير التخزين!" else "Deleted $deletedCount unlinked images successfully!", Toast.LENGTH_LONG).show()
                    refreshScan()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError),
                enabled = unlinkedFiles.isNotEmpty()
            ) {
                Text(if (language == "ar") "🗑️ تفريغ وحذف الصور غير المستخدمة" else "🗑️ Delete Unlinked Assets & Keep Database Clean")
            }
        }
    }
}

// ==========================================
// SCREEN 7: Backdoor Configurations Settings Dialog (MANDATORY Secret Portal)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackdoorSettingsDialog(
    settings: AppSettingEntity,
    onDismiss: () -> Unit,
    onSave: (AppSettingEntity) -> Unit
) {
    var appName by remember { mutableStateOf(settings.appName) }
    var welcomeMsg by remember { mutableStateOf(settings.welcomeMessage) }
    var footerText by remember { mutableStateOf(settings.footerText) }
    var primaryColor by remember { mutableStateOf(settings.primaryColor) }
    var secondaryColor by remember { mutableStateOf(settings.secondaryColor) }
    var sPhone by remember { mutableStateOf(settings.supportPhone) }
    var sEmail by remember { mutableStateOf(settings.supportEmail) }
    var sWa by remember { mutableStateOf(settings.supportWhatsApp) }
    var fontScaling by remember { mutableStateOf(settings.fontSize) }
    var searchLim by remember { mutableStateOf(settings.radiusSearchLimit) }
    var voiceSearchEnable by remember { mutableStateOf(settings.voiceSearchEnabled) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(8.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(18.dp).verticalScroll(rememberScrollState())) {
                Text("⚙️ لوحة الإعدادات السرية للمالك", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))

                // App Branding Name
                OutlinedTextField(value = appName, onValueChange = { appName = it }, label = { Text("تحديث اسم التطبيق الرئيسي") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(10.dp))

                // Welcome message
                OutlinedTextField(value = welcomeMsg, onValueChange = { welcomeMsg = it }, label = { Text("رسالة الترحيب المتحركة") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(10.dp))

                // Footer promotional text
                OutlinedTextField(value = footerText, onValueChange = { footerText = it }, label = { Text("التذييل الدعائي") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(10.dp))

                // Support dials
                OutlinedTextField(value = sPhone, onValueChange = { sPhone = it }, label = { Text("رقم هاتف الدعم") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(value = sEmail, onValueChange = { sPhone = it }, label = { Text("إيميل الدعم") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(value = sWa, onValueChange = { sWa = it }, label = { Text("رابط دعم واتساب") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(14.dp))

                // Themes quick selections (Cosmic Silver, Gold Luxury, Emerald)
                Text("منتقي الثيمات والألوان الفاخرة:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = {
                            primaryColor = "#ECEFF1"
                            secondaryColor = "#37474F"
                        },
                        modifier = Modifier.weight(1f).height(32.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFECEFF1), contentColor = Color.Black),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("🌌 كوزميك", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            primaryColor = "#FFD700"
                            secondaryColor = "#8A7300"
                        },
                        modifier = Modifier.weight(1f).height(32.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700), contentColor = Color.Black),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("✨ ذهبي", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            primaryColor = "#2E7D32"
                            secondaryColor = "#0D5215"
                        },
                        modifier = Modifier.weight(1f).height(32.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32), contentColor = Color.White),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("🟢 زمردي", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Custom Hex inputs
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = primaryColor, onValueChange = { primaryColor = it }, label = { Text("اللون الأساسي Hex") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = secondaryColor, onValueChange = { secondaryColor = it }, label = { Text("اللون الثانوي Hex") }, modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Radius limitations (5 to 50 KM)
                Text("حدود مسافة البحث عبر الخرائط: $searchLim كم", fontSize = 12.sp)
                Slider(
                    value = searchLim.toFloat(),
                    onValueChange = { searchLim = it.toInt() },
                    valueRange = 5f..50f,
                    steps = 9
                )

                Spacer(modifier = Modifier.height(12.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = voiceSearchEnable, onCheckedChange = { voiceSearchEnable = it })
                    Text("تمكين ميزة البحث الصوتي 🎤", fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = onDismiss) {
                        Text("إلغاء")
                    }
                    Button(onClick = {
                        onSave(
                            settings.copy(
                                appName = appName,
                                welcomeMessage = welcomeMsg,
                                footerText = footerText,
                                primaryColor = primaryColor,
                                secondaryColor = secondaryColor,
                                supportPhone = sPhone,
                                supportEmail = sEmail,
                                supportWhatsApp = sWa,
                                fontSize = fontScaling,
                                radiusSearchLimit = searchLim,
                                voiceSearchEnabled = voiceSearchEnable
                            )
                        )
                    }) {
                        Text("تطبيق وحفظ التغيير")
                    }
                }
            }
        }
    }
}
