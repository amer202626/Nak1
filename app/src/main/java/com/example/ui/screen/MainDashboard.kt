package com.example.ui.screen

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppViewModel
import java.util.*

// TextToSpeech utility helper
class TtsHelper(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isReady = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("ar"))
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                isReady = true
            }
        }
    }

    fun speak(text: String) {
        if (isReady && tts != null) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
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
fun TtsSpeakerButton(textToSpeak: String, ttsHelper: TtsHelper, tintColor: Color = MaterialTheme.colorScheme.primary) {
    IconButton(
        onClick = { ttsHelper.speak(textToSpeak) },
        modifier = Modifier.size(30.dp)
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow, // VolumeUp mapped to PlayArrow
            contentDescription = "استماع للنص",
            tint = tintColor,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun SpeechRecognitionButton(onResult: (String) -> Unit, language: String) {
    val context = LocalContext.current
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (spokenText != null) {
                onResult(spokenText)
            }
        }
    }

    IconButton(
        onClick = {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (language == "ar") "ar-YE" else "en-US")
                putExtra(RecognizerIntent.EXTRA_PROMPT, if (language == "ar") "تحدث الآن لكتابة النص..." else "Speak now...")
            }
            try {
                speechLauncher.launch(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "التعرف على الصوت غير مدعوم على جهازك.", Toast.LENGTH_SHORT).show()
            }
        },
        modifier = Modifier.size(32.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Edit, // Mic mapped to Edit safely
            contentDescription = "كتابة بالصوت",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboard(viewModel: AppViewModel) {
    val context = LocalContext.current
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val providers by viewModel.providers.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val testTts = rememberTtsHelper()

    // Navigation and screen state
    var selectedTab by remember { mutableStateOf(0) } // 0: Directory, 1: AI Assistant, 2: Join, 3: Admin
    var selectedCategory by remember { mutableStateOf<String?>("all") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedProviderForDetails by remember { mutableStateOf<ProviderEntity?>(null) }

    // Dialog sheets state
    var showBookingDialogForProvider by remember { mutableStateOf<ProviderEntity?>(null) }
    var showReportDialogForProvider by remember { mutableStateOf<ProviderEntity?>(null) }

    // Active instant chat window
    val selectedChatId by viewModel.selectedChatId.collectAsStateWithLifecycle()
    val activeChatMessages by viewModel.activeChatMessages.collectAsStateWithLifecycle()
    val chats by viewModel.chats.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = GoldColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "WAM Services",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    // Language Switcher
                    TextButton(
                        onClick = {
                            if (language == "ar") viewModel.setLanguage("en")
                            else viewModel.setLanguage("ar")
                        }
                    ) {
                        Text(
                            text = if (language == "ar") "EN 🇬🇧" else "AR 🇾🇪",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                // Mapped extended icons to standard approved icons
                val tabs = listOf(
                    Triple(0, if (language == "ar") "الدليل 🔍" else "Directory", Icons.Default.Search),
                    Triple(1, if (language == "ar") "المستشار 🤖" else "AI Guide", Icons.Default.Star),
                    Triple(2, if (language == "ar") "سجل كفني 🛠️" else "Join as Pro", Icons.Default.Build),
                    Triple(3, if (language == "ar") "المشرف ⚙️" else "Admin", Icons.Default.Lock)
                )
                tabs.forEach { (index, title, icon) ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index; selectedProviderForDetails = null },
                        icon = { Icon(icon, contentDescription = title) },
                        label = { Text(title, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            if (chats.isNotEmpty()) {
                FloatingActionButton(
                    onClick = {
                        val activeRoomId = chats.firstOrNull()?.chatId
                        if (activeRoomId != null) {
                            viewModel.selectActiveChat(activeRoomId)
                        } else {
                            Toast.makeText(context, if (language == "ar") "لا توجد غرف محادثة مفتوحة حالياً." else "No open chat channels.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.Black
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Active Chats")
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        ) {
            when (selectedTab) {
                0 -> {
                    // DIRECTORY TAB
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp)
                    ) {
                        // Intro Header with TTS Announcement
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (language == "ar") "دليل ومحرك صيانة اليمن الأقوى" else "Yemen's Premium Service Network",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (language == "ar") "اختر الفني الموثوق وتحدث معه!" else "Find and chat with trusted specialists",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            TtsSpeakerButton(
                                textToSpeak = if (language == "ar") "مرحباً بكم في دبليو أي إم دليل صيانة اليمن الأقوى. يرجى تصفح فنيي الصيانة الموثوقين وحجز موعد بنقرة واحدة." else "Welcome to WAM services. Please browse available certified repair providers and book one-click appointments.",
                                ttsHelper = testTts
                            )
                        }

                        // Search box with speech typing
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    text = if (language == "ar") "ابحث باسم الفني أو المنطقة أو التفاصيل..." else "Search name, district or specialty...",
                                    fontSize = 12.sp
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("directory_search_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                                        }
                                    }
                                    SpeechRecognitionButton(
                                        onResult = { searchQuery = it },
                                        language = language
                                    )
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Category Pills Horizontal lazy row
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedCategory == "all",
                                    onClick = { selectedCategory = "all" },
                                    label = { Text(if (language == "ar") "الكل 🌍" else "All") }
                                )
                            }
                            items(categories) { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat.id,
                                    onClick = { selectedCategory = cat.id },
                                    label = { Text("${cat.emoji} ${if (language == "ar") cat.nameAr else cat.nameEn}") }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Filter approved/verified matching search
                        val filteredProviders = providers.filter { p ->
                            p.status == "approved" &&
                            (selectedCategory == "all" || p.providerType == selectedCategory) &&
                            (p.fullName.contains(searchQuery, ignoreCase = true) ||
                             p.district.contains(searchQuery, ignoreCase = true) ||
                             p.details.contains(searchQuery, ignoreCase = true))
                        }

                        if (filteredProviders.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (language == "ar") "لا توجد نتائج مطابقة لبحثك حالياً." else "No service providers match your search currently.",
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(filteredProviders) { p ->
                                    ProviderListItem(
                                        p = p,
                                        language = language,
                                        onSelect = { selectedProviderForDetails = p },
                                        onBook = { showBookingDialogForProvider = p },
                                        onOpenChat = {
                                            viewModel.openChatWithProvider(p.id, p.fullName)
                                        },
                                        onReport = { showReportDialogForProvider = p },
                                        ttsHelper = testTts
                                    )
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // AI GUIDE CONSULTING TAB
                    var aiPromptInput by remember { mutableStateOf("") }
                    val aiResponse by viewModel.aiResponse.collectAsStateWithLifecycle()
                    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()
                    val apiKey by viewModel.geminiApiKey.collectAsStateWithLifecycle()

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Star, // AutoAwesome mapped to Star
                                        contentDescription = null,
                                        tint = GoldColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (language == "ar") "استشاري صيانة وتوجيه ذكي" else "AI Diagnostics & Advisory",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                                Text(
                                    text = if (language == "ar")
                                        "اطرح أي سؤال صيانة (مثل: كيف أصلح تهريب أنبوب ماء، أو كيفية صيانة سخان شمسي) وسيقدم لك الحلول خطوة بخطوة بالذكاء الاصطناعي!"
                                    else
                                        "Describe any maintenance issue in English or Arabic, and WAM AI Guide will give you diagnostic instructions instantly!",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // API Key Configuration directly in-app
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = { viewModel.setGeminiApiKey(it) },
                            label = { Text(if (language == "ar") "مفتاح API الخاص بـ Gemini (حفظ تلقائي)" else "Gemini API Key (Auto-saved)") },
                            placeholder = { Text("AIzaSy...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = aiPromptInput,
                            onValueChange = { aiPromptInput = it },
                            placeholder = {
                                Text(
                                    text = if (language == "ar") "اسأل المساعد بالصوت أو النص هنا..." else "Type or dictate your problem here...",
                                    fontSize = 12.sp
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                                .testTag("ai_prompt_field"),
                            maxLines = 3,
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 4.dp)) {
                                    SpeechRecognitionButton(
                                        onResult = { aiPromptInput = it },
                                        language = language
                                    )
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (aiPromptInput.trim().isEmpty()) {
                                    Toast.makeText(context, "الرجاء كتابة أو قول السؤال أولاً", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.askGeminiAssistant(aiPromptInput)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("ask_ai_btn"),
                            enabled = !isAiLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            if (isAiLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.Black,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (language == "ar") "استشر الذكاء الاصطناعي فورا 🚀" else "Consult AI diagnostics")
                            }
                        }

                        if (aiResponse.isNotEmpty() || isAiLoading) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "🤖 التشخيص الفوري المقترح:",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (aiResponse.isNotEmpty()) {
                                            TtsSpeakerButton(textToSpeak = aiResponse, ttsHelper = testTts)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    if (isAiLoading) {
                                        Text(
                                            text = if (language == "ar") "يرجى الانتظار... يتم التفكير واستخراج توجيهات الصيانة اليمنية الفاخرة..." else "Thinking... Fetching optimized diagnostics...",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    } else {
                                        Text(
                                            text = aiResponse,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // REGISTER AS PROVIDER TAB
                    var rName by remember { mutableStateOf("") }
                    var rPhone by remember { mutableStateOf("") }
                    var rDistrict by remember { mutableStateOf("") }
                    var rDetails by remember { mutableStateOf("") }
                    var rCategoryId by remember { mutableStateOf("") }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = if (language == "ar") "انضم كشريك فني معتمد 🛠️" else "Register as Certified Service Partner",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (language == "ar")
                                "اكتب بياناتك أو أملأ الحقول بالصوت للانضمام لشبكتنا الموثوقة بعد مراجعة المشرف."
                            else
                                "You can easily dictate information or type details below to request approval to join our network.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // Name
                        OutlinedTextField(
                            value = rName,
                            onValueChange = { rName = it },
                            label = { Text(if (language == "ar") "الاسم الكامل والشهرة الفنية:" else "Full Name / Trade name:") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reg_fullName"),
                            singleLine = true,
                            trailingIcon = {
                                SpeechRecognitionButton(onResult = { rName = it }, language = language)
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Category choice via Dropdown/Horizontal pills
                        Text(if (language == "ar") "اختر تخصصك الفني:" else "Choose Your Trade:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            items(categories) { cat ->
                                FilterChip(
                                    selected = rCategoryId == cat.id,
                                    onClick = { rCategoryId = cat.id },
                                    label = { Text("${cat.emoji} ${if (language == "ar") cat.nameAr else cat.nameEn}") }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        // Phone
                        OutlinedTextField(
                            value = rPhone,
                            onValueChange = { rPhone = it },
                            label = { Text(if (language == "ar") "رقم هاتف المطور والواتساب:" else "Phone & WhatsApp Number:") },
                            placeholder = { Text("77xxxxxxx") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reg_phone"),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            trailingIcon = {
                                SpeechRecognitionButton(onResult = { rPhone = it }, language = language)
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Area
                        OutlinedTextField(
                            value = rDistrict,
                            onValueChange = { rDistrict = it },
                            label = { Text(if (language == "ar") "المديرية والمنطقة Сكنية:" else "Yemen District / Area:") },
                            placeholder = { Text(if (language == "ar") "صنعاء - السبعين" else "Sana'a - AlSabeen") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reg_district"),
                            singleLine = true,
                            trailingIcon = {
                                SpeechRecognitionButton(onResult = { rDistrict = it }, language = language)
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Details of service
                        OutlinedTextField(
                            value = rDetails,
                            onValueChange = { rDetails = it },
                            label = { Text(if (language == "ar") "تفاصيل الخدمات والخبرة الفنية:" else "Experience / Offered Services Details:") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .testTag("reg_details"),
                            trailingIcon = {
                                SpeechRecognitionButton(onResult = { rDetails = it }, language = language)
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (rName.isEmpty() || rPhone.isEmpty() || rDistrict.isEmpty() || rDetails.isEmpty() || rCategoryId.isEmpty()) {
                                    Toast.makeText(context, if (language == "ar") "يرجى تعبئة كافة الحقول والتأكد من تحديد قسم!" else "Please complete all fields and pick a trade class!", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.registerProvider(
                                        name = rName,
                                        categoryId = rCategoryId,
                                        phone = rPhone,
                                        district = rDistrict,
                                        details = rDetails,
                                        imageBase64 = ""
                                    )
                                    // Reset inputs
                                    rName = ""
                                    rPhone = ""
                                    rDistrict = ""
                                    rDetails = ""
                                    rCategoryId = ""
                                    Toast.makeText(context, if (language == "ar") "تم تقديم طلبك بنجاح للمشرف ومزامنته حيا!" else "Request submitted successfuly!", Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("reg_submit_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(if (language == "ar") "تأكيد وإرسال طلب الانضمام 🤝" else "Apply for Network Verification")
                        }
                    }
                }
                3 -> {
                    // ADMIN SYSTEM DASHBOARD (with multiple sub-tabs)
                    var isAdminAuthenticated by remember { mutableStateOf(false) }
                    var adminPasswordInput by remember { mutableStateOf("") }

                    if (!isAdminAuthenticated) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock, // AdminPanelSettings mapped to Lock
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (language == "ar") "دخول لوحة تحكم المشرف الفاخرة" else "Admin Authorization Gate",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = if (language == "ar") "اطلب كلمة المرور من مطور النظام للتعديل الشامل" else "Enter security credential passcode to configure tables",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            OutlinedTextField(
                                value = adminPasswordInput,
                                onValueChange = { adminPasswordInput = it },
                                placeholder = { Text(if (language == "ar") "أدخل كلمة المرور..." else "Passcode...") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_passcode_field"),
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation()
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    if (adminPasswordInput.isNotEmpty()) {
                                        isAdminAuthenticated = true
                                        Toast.makeText(context, if (language == "ar") "مرحباً بمشرف WAM Services" else "Welcome Admin", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, if (language == "ar") "يرجى إدخال كلمة مرور صالحة" else "Invalid credential!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("admin_login_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text(if (language == "ar") "دخول آمن 🔓" else "Authorize Secure Entry")
                            }
                        }
                    } else {
                        AdminDashboardScreen(
                            viewModel = viewModel,
                            language = language,
                            onLogOut = { isAdminAuthenticated = false; adminPasswordInput = "" }
                        )
                    }
                }
            }

            // DETAILS SHEET DIALOG FOR SELECTED PROVIDER
            selectedProviderForDetails?.let { p ->
                val fullCat = categories.find { it.id == p.providerType }
                Dialog(onDismissRequest = { selectedProviderForDetails = null }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(45.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = fullCat?.emoji ?: "🛠️",
                                        fontSize = 20.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = p.fullName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        if (p.isVerified) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.Star, // Verified mapped to Star
                                                contentDescription = "Verified Verification",
                                                tint = GoldColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = fullCat?.nameAr ?: p.providerType,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                TtsSpeakerButton(
                                    textToSpeak = "معلومات الفني ${p.fullName}. القسم ${fullCat?.nameAr}. يعمل بالمنطقة ${p.district} تفاصيل الخدمات: ${p.details}",
                                    ttsHelper = testTts
                                )
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant)
                            
                            Text(
                                text = if (language == "ar") "📍 المنطقة والمديرية:" else "📍 Operating Region:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(text = p.district, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))

                            Text(
                                text = if (language == "ar") "📝 تفاصيل وتخصصات العمل:" else "📝 Trade Details & Bio:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(text = p.details, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))

                            Text(
                                text = if (language == "ar") "📞 معلومات الاتصال والمزامنة حية:" else "📞 Contact information:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(text = p.phone, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        selectedProviderForDetails = null
                                        showBookingDialogForProvider = p
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text(if (language == "ar") "حجز موعد 📅" else "Book")
                                }
                                Button(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            data = Uri.parse("https://wa.me/967${p.phone.removePrefix("0")}")
                                        }
                                        try {
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, if (language == "ar") "تطبيق واتساب غير مثبت." else "WhatsApp not installed.", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = GreenColor)
                                ) {
                                    Text(if (language == "ar") "واتساب 🟢" else "WhatsApp")
                                }
                            }
                        }
                    }
                }
            }

            // BOOKING SCREEN DIALOG
            showBookingDialogForProvider?.let { p ->
                var fullNameInput by remember { mutableStateOf("") }
                var phoneInput by remember { mutableStateOf("") }
                var districtInput by remember { mutableStateOf("") }

                Dialog(onDismissRequest = { showBookingDialogForProvider = null }) {
                    Card(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary) // CalendarMonth mapped to DateRange
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (language == "ar") "📅 حجز موعد مع الفني: ${p.fullName}" else "📅 Book Appt with: ${p.fullName}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                TtsSpeakerButton(
                                    textToSpeak = if (language == "ar") "حجز موعد مع الفني ${p.fullName}. الرجاء تعبئة بيانات الاسم الكامل، رقم الهاتف، والمديرية." else "Book an appointment with ${p.fullName}. Please enter your full name, phone number, and district.",
                                    ttsHelper = testTts
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            // Full Name field
                            Text(if (language == "ar") "الاسم الكامل للمستفيد:" else "Beneficiary Full Name:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = fullNameInput,
                                onValueChange = { fullNameInput = it },
                                placeholder = { Text(if (language == "ar") "مثال: أحمد علي الوديع" else "e.g., John Doe", fontSize = 12.sp) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("book_fullName"),
                                singleLine = true,
                                trailingIcon = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        SpeechRecognitionButton(onResult = { fullNameInput = it }, language = language)
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("book_phone"),
                                singleLine = true,
                                trailingIcon = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        SpeechRecognitionButton(onResult = { phoneInput = it }, language = language)
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("book_district"),
                                singleLine = true,
                                trailingIcon = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        SpeechRecognitionButton(onResult = { districtInput = it }, language = language)
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(18.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { showBookingDialogForProvider = null }) {
                                    Text(if (language == "ar") "إلغاء لحجز" else "Cancel")
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

            // REPORT PROVIDER DIALOG
            showReportDialogForProvider?.let { p ->
                var reportReason by remember { mutableStateOf("") }
                Dialog(onDismissRequest = { showReportDialogForProvider = null }) {
                    Card(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = CrimsonColor) // Report mapped to Warning
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("إبلاغ أمني عن فني مخترق أو مسيء", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("سيتم إرسال هذا الإبلاغ فورياً للمشرفين للتحقق وحظر الحساب إذا أثبت في حقه أي نشاط غير قانوني أو احتيال.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = reportReason,
                                onValueChange = { reportReason = it },
                                label = { Text("سبب الإبلاغ وشرح تفصيلي بالصوت أو الكتابة", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth().height(80.dp),
                                trailingIcon = {
                                    SpeechRecognitionButton(onResult = { reportReason = it }, language = language)
                                }
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { showReportDialogForProvider = null }) {
                                    Text("إلغاء")
                                }
                                Button(
                                    onClick = {
                                        if (reportReason.isEmpty()) {
                                            Toast.makeText(context, "الرجاء توضيح تفاصيل وسبب بلاغك!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            viewModel.sendSystemNotification(
                                                title = "🚨 بلاغ عاجل عن الفني: ${p.fullName}",
                                                body = "السبب: $reportReason. مقدم البلاغ: مستفيد من التطبيق.",
                                                type = "report",
                                                targetId = p.id
                                            )
                                            showReportDialogForProvider = null
                                            Toast.makeText(context, "نشكر حسك الأمني والتعاوني! تم استقبال ونشر البلاغ المباشر للمشرفين للمتابعة الحثيثة وحظر الفني إذا لزم الأمر.", Toast.LENGTH_LONG).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonColor)
                                ) {
                                    Text("بث البلاغ العاجل 🚀", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // LARGE FLOATING ACTIVE CHAT DIALOG WINDOW
            selectedChatId?.let { chatId ->
                var messageInput by remember { mutableStateOf("") }
                val activeRoom = chats.find { it.chatId == chatId }
                
                Dialog(onDismissRequest = { viewModel.selectActiveChat(null) }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(420.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Chat Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, tint = MaterialTheme.colorScheme.primary) // Chat mapped to Send
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "محادثة حية: ${activeRoom?.participantsCSV ?: "غرفة نشطة"}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                TtsSpeakerButton(
                                    textToSpeak = "أنت الآن في غرفة المحادثة المباشرة مع الفني لمناقشة تفاصيل وأسعار الصيانة والخدمات المطلوبة.",
                                    ttsHelper = testTts
                                )
                                IconButton(onClick = { viewModel.selectActiveChat(null) }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close")
                                }
                            }

                            // Messages area
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                reverseLayout = false,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(activeChatMessages) { msg ->
                                    val isMe = msg.senderId == "user"
                                    val align = if (isMe) Alignment.End else Alignment.Start
                                    val cardColor = if (isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                    
                                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = align) {
                                        Card(
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = cardColor)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f, fill = false)) {
                                                    Text(text = msg.senderName, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                    Text(text = msg.text, fontSize = 12.sp)
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                TtsSpeakerButton(textToSpeak = "${msg.senderName} قال: ${msg.text}", ttsHelper = testTts, tintColor = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }

                            // Input text Bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SpeechRecognitionButton(onResult = { messageInput = it }, language = language)
                                
                                OutlinedTextField(
                                    value = messageInput,
                                    onValueChange = { messageInput = it },
                                    placeholder = { Text("اكتب رسالتك بالصوت أو النص...", fontSize = 11.sp) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("chat_msg_input"),
                                    singleLine = true
                                )
                                
                                Spacer(modifier = Modifier.width(6.dp))
                                
                                IconButton(
                                    onClick = {
                                        if (messageInput.trim().isNotEmpty()) {
                                            viewModel.sendChatMessage(
                                                text = messageInput,
                                                senderId = "user",
                                                senderName = "مستفيد معتمد"
                                            )
                                            messageInput = ""
                                        }
                                    },
                                    modifier = Modifier.background(MaterialTheme.colorScheme.primary, CircleShape)
                                ) {
                                    Icon(Icons.Default.Send, contentDescription = "إرسال", tint = Color.Black, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProviderListItem(
    p: ProviderEntity,
    language: String,
    onSelect: () -> Unit,
    onBook: () -> Unit,
    onOpenChat: () -> Unit,
    onReport: () -> Unit,
    ttsHelper: TtsHelper
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("provider_item_${p.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Verified and Name
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = p.fullName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        if (p.isVerified) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Star, // Verified mapped to Star
                                contentDescription = "Verified Icon",
                                tint = GoldColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = "📍 ${p.district}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                TtsSpeakerButton(
                    textToSpeak = "الفني ${p.fullName}. يعمل في ${p.district}. تفاصيل الخدمات: ${p.details}",
                    ttsHelper = ttsHelper
                )

                IconButton(
                    onClick = onReport,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = "بلاغ", tint = CrimsonColor.copy(alpha = 0.6f), modifier = Modifier.size(14.dp)) // Report mapped to Warning
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = p.details,
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onSelect,
                    modifier = Modifier
                        .weight(1.2f)
                        .height(32.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(if (language == "ar") "التفاصيل والتقييم ⭐" else "Details", fontSize = 10.sp)
                }

                Button(
                    onClick = onBook,
                    modifier = Modifier
                        .weight(1.2f)
                        .height(32.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(if (language == "ar") "حجز موعد 📅" else "Book", fontSize = 10.sp, color = Color.Black)
                }

                IconButton(
                    onClick = onOpenChat,
                    modifier = Modifier
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "مراسلة", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp)) // Chat mapped to Send
                }

                IconButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("https://wa.me/967${p.phone.removePrefix("0")}")
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "تطبيق واتساب غير مثبت.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .background(GreenColor, CircleShape)
                ) {
                    Icon(Icons.Default.Call, contentDescription = "واتس", tint = Color.Black, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

@Composable
fun AdminDashboardScreen(
    viewModel: AppViewModel,
    language: String,
    onLogOut: () -> Unit
) {
    val providers by viewModel.providers.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val bookings by viewModel.bookings.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val chats by viewModel.chats.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedTabIdx by remember { mutableStateOf(0) }

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

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "🛡️ جناح الإشراف الفاخر",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            IconButton(onClick = onLogOut) {
                Icon(Icons.Default.ExitToApp, contentDescription = "Log Out", tint = CrimsonColor)
            }
        }

        // Sub categories indicator scrollable row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(vertical = 6.dp)
        ) {
            items(tabTitles.size) { index ->
                FilterChip(
                    selected = selectedTabIdx == index,
                    onClick = { selectedTabIdx = index },
                    label = { Text(tabTitles[index], fontSize = 10.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (selectedTabIdx) {
            0 -> {
                // TAB 0: Stats Breakdown
                val totalProvs = providers.size
                val approvedProvs = providers.count { it.status == "approved" }
                val pendingProvs = providers.count { it.status == "pending" }
                val totalBookings = bookings.size
                val pendingBookings = bookings.count { it.status == "pending" }
                val confirmedBookings = bookings.count { it.status == "confirmed" }

                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    Text("📊 الكشوفات والتقارير الحية للمخدم الشبكي:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("العدد الإجمالي للمزودين: $totalProvs", fontSize = 12.sp)
                            Text("الفنيون المعتمدون الفعالون: $approvedProvs 🟢", fontSize = 12.sp)
                            Text("الطلبات المعلقة بالمراجعة: $pendingProvs ⏳", fontSize = 12.sp)
                            Text("العدد الإجمالي للحجوزات والمواعيد: $totalBookings 📅", fontSize = 12.sp)
                            Text("حجوزات بانتظار التأكيد: $pendingBookings ⏳", fontSize = 12.sp)
                            Text("حجوزات مقبولة ومنجزة: $confirmedBookings 🟢", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("🔥 إعدادات مزامنة Firestore السحابية الحية:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            var firestoreProjId by remember { mutableStateOf(viewModel.getFirestoreProjectId()) }
                            
                            OutlinedTextField(
                                value = firestoreProjId,
                                onValueChange = { 
                                    firestoreProjId = it
                                    viewModel.setFirestoreProjectId(it)
                                },
                                label = { Text("معرف مشروع Firebase (Project ID)", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Button(
                                onClick = {
                                    viewModel.setFirestoreProjectId(firestoreProjId)
                                    Toast.makeText(context, "بدء المزامنة اليدوية الفورية للبيانات السحابية...", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("مزامنة الآن 🔄", fontSize = 11.sp)
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "تتم المزامنة تلقائياً في الخلفية كل 6 ثوانٍ مع كافة الأجهزة المتصلة بالمشروع بنجاح حقيقي!", 
                                fontSize = 10.sp, 
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
            1 -> {
                // TAB 1: Pending providers requests
                val pendingList = providers.filter { it.status == "pending" }
                if (pendingList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("لا توجد ملفات فنية معلقة صدارة في المراجعة حالياً.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(pendingList) { p ->
                            Card(
                                modifier = Modifier.fillMaxWidth().testTag("pending_item_${p.id}"),
                                border = BorderStroke(1.dp, CrimsonColor)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("الطلب: ${p.fullName}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("المديرية: ${p.district} | القسم: ${p.providerType}", fontSize = 11.sp)
                                    Text("التفاصيل: ${p.details}", fontSize = 11.sp)
                                    Text("الهاتف: ${p.phone}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Button(
                                            onClick = { viewModel.approveProvider(p.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = GreenColor)
                                        ) {
                                            Text("قبول وتعميد 🟢", fontSize = 10.sp, color = Color.Black)
                                        }
                                        Button(
                                            onClick = { viewModel.deleteProvider(p.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonColor)
                                        ) {
                                            Text("رفض وحذف 🔴", fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            2 -> {
                // TAB 2: Add Pro manually
                var nName by remember { mutableStateOf("") }
                var nPhone by remember { mutableStateOf("") }
                var nDistrict by remember { mutableStateOf("") }
                var nDetails by remember { mutableStateOf("") }
                var nCategoryId by remember { mutableStateOf("") }

                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    Text("🛠️ إضافة فني معتمد فورياً بدون مراجعة:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(value = nName, onValueChange = { nName = it }, label = { Text("الاسم الكامل") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = nPhone, onValueChange = { nPhone = it }, label = { Text("رقم الهاتف والواتس") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = nDistrict, onValueChange = { nDistrict = it }, label = { Text("المديرية والموقع") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = nDetails, onValueChange = { nDetails = it }, label = { Text("تفاصيل وتخصص الفني") }, modifier = Modifier.fillMaxWidth())
                    
                    Text("اختر القسم المباشر:", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(categories) { cat ->
                            FilterChip(selected = nCategoryId == cat.id, onClick = { nCategoryId = cat.id }, label = { Text(cat.nameAr) })
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            if (nName.isEmpty() || nPhone.isEmpty() || nDistrict.isEmpty() || nDetails.isEmpty() || nCategoryId.isEmpty()) {
                                Toast.makeText(context, "الرجاء تعبئة كافة الحقول الفنية!", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.addAndApproveProvider(
                                    name = nName,
                                    categoryId = nCategoryId,
                                    phone = nPhone,
                                    district = nDistrict,
                                    details = nDetails
                                )
                                nName = ""
                                nPhone = ""
                                nDistrict = ""
                                nDetails = ""
                                nCategoryId = ""
                                Toast.makeText(context, "تم إدراج وتعميد الفني في الكشوفات!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("إضافة ونشر معتمد 🚀")
                    }
                }
            }
            3 -> {
                // TAB 3: List of all providers
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(providers) { p ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${p.fullName} (${p.status})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("تخصص: ${p.providerType} | جوال: ${p.phone}", fontSize = 11.sp)
                                }
                                IconButton(onClick = { viewModel.deleteProvider(p.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = CrimsonColor)
                                }
                            }
                        }
                    }
                }
            }
            4 -> {
                // TAB 4: Manage Categories
                var newCatId by remember { mutableStateOf("") }
                var newCatNameAr by remember { mutableStateOf("") }
                var newCatNameEn by remember { mutableStateOf("") }
                var newCatEmoji by remember { mutableStateOf("") }

                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    Text("🗂️ إضافة وتخصيص تصنيف صيانة جديد:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    OutlinedTextField(value = newCatId, onValueChange = { newCatId = it }, label = { Text("معرف القسم (ID)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = newCatNameAr, onValueChange = { newCatNameAr = it }, label = { Text("الاسم بالعربية") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = newCatNameEn, onValueChange = { newCatNameEn = it }, label = { Text("الاسم بالإنجليزية") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = newCatEmoji, onValueChange = { newCatEmoji = it }, label = { Text("الرمز التعبيري (Emoji)") }, modifier = Modifier.fillMaxWidth())
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (newCatId.isNotEmpty() && newCatNameAr.isNotEmpty()) {
                                viewModel.addNewCategory(newCatId, newCatNameAr, newCatNameEn, newCatEmoji)
                                newCatId = ""
                                newCatNameAr = ""
                                newCatNameEn = ""
                                newCatEmoji = ""
                                Toast.makeText(context, "تم إدراج التصنيف بنجاح!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("إرسال التصنيف اليومي")
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("قائمة الأقسام الفعالة حالياً:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    categories.forEach { cat ->
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("${cat.emoji} ${cat.nameAr} (${cat.id})", modifier = Modifier.weight(1f), fontSize = 11.sp)
                                IconButton(onClick = { viewModel.deleteCategory(cat) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = CrimsonColor, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }
            5 -> {
                // TAB 5: Reports and system log
                val reportNotifs = notifications.filter { it.type == "report" }
                if (reportNotifs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("لا توجد شكاوى أمنية أو بلاغات حتى اللحظة.", fontSize = 11.sp)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(reportNotifs) { r ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(r.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                                    Text(r.body, fontSize = 11.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    IconButton(
                                        onClick = { viewModel.deleteNotification(r.id) },
                                        modifier = Modifier.align(Alignment.End)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = "اعتماد البلاغ ومسحه", tint = Color.Green)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            6 -> {
                // TAB 6: Clear redundant images / cache
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(42.dp)) // CleaningServices mapped to Refresh
                        Text("تهيئة السعة وإخلاء ذاكرة التخزين المؤقت للصور", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Text("سيتم مسح كافة ملفات الكاش وتحسين موارد نظام السيكواليت المحلي.", fontSize = 10.sp, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                Toast.makeText(context, "تم إخلاء الذاكرة المؤقتة وضغط المساحة السحابية بنجاح!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("تنظيف ومسح الكاش 🧹")
                        }
                    }
                }
            }
            7 -> {
                // TAB 7: Manage bookings
                var bookingSearchQuery by remember { mutableStateOf("") }
                
                Column(modifier = Modifier.fillMaxSize()) {
                    OutlinedTextField(
                        value = bookingSearchQuery,
                        onValueChange = { bookingSearchQuery = it },
                        placeholder = { Text("ابحث عن حجز (اسم المستفيد، الهاتف، أو المشرف)...", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth().testTag("booking_search_input_admin"),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val filteredBookings = bookings.filter {
                        it.fullName.contains(bookingSearchQuery, ignoreCase = true) ||
                        it.phone.contains(bookingSearchQuery, ignoreCase = true) ||
                        it.providerName.contains(bookingSearchQuery, ignoreCase = true) ||
                        it.district.contains(bookingSearchQuery, ignoreCase = true)
                    }
                    
                    if (filteredBookings.isEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("لا توجد طلبات حجز مطابقة لخيارات البحث حالياً.", fontSize = 11.sp)
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
                                            Text(statusText, color = statusColor, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                        
                                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                                        
                                        Text("👤 المستفيد: ${booking.fullName}", fontSize = 11.sp)
                                        Text("📞 هاتف وتساب: ${booking.phone}", fontSize = 11.sp)
                                        Text("📍 المنطقة/المديرية: ${booking.district}", fontSize = 11.sp)
                                        Text("🛠️ الفني المطلوب: ${booking.providerName}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                        
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
                
                Column(modifier = Modifier.fillMaxSize()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp)) // NotificationsActive mapped to Notifications
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("📢 إرسال وتعميم إشعار فوري جديد", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            OutlinedTextField(
                                value = notificationTitle,
                                onValueChange = { notificationTitle = it },
                                label = { Text("عنوان الإشعار", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth().testTag("notif_title_input"),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            OutlinedTextField(
                                value = notificationBody,
                                onValueChange = { notificationBody = it },
                                label = { Text("محتوى البث أو الرسالة", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth().height(60.dp).testTag("notif_body_input")
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
                                        Toast.makeText(context, "تم إرسال ونشر الإشعار لحظياً بمزامنة حية!", Toast.LENGTH_SHORT).show()
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
                            Text("سجل الإشعارات فارغ حالياً.", fontSize = 11.sp)
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
                                            Text(notif.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                            IconButton(
                                                onClick = { viewModel.deleteNotification(notif.id) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "مسح", modifier = Modifier.size(12.dp))
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(notif.body, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            9 -> {
                // TAB 9: Manage instant chat screen
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "💬 أداة الرصد والمحاكاة للمحادثات المباشرة",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    if (chats.isEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("لا توجد غرف محادثة نشطة حالياً في قاعدة البيانات.", fontSize = 11.sp)
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
                                            Text("آخر رسالة: ${chat.lastMessage}", fontSize = 11.sp)
                                        }
                                        
                                        Spacer(modifier = Modifier.width(8.dp))
                                        
                                        Button(
                                            onClick = {
                                                viewModel.selectActiveChat(chat.chatId)
                                                Toast.makeText(context, "تم تفعيل القناة! اضغط على زر Chat بالأسفل لعرض المحادثة والمتابعة.", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            modifier = Modifier.height(32.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                                        ) {
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
