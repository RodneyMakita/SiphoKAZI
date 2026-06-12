package com.example

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import androidx.compose.ui.text.TextStyle
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize TextToSpeech engine for Siphokazi's AI Voice Chat
        try {
            tts = TextToSpeech(this, this)
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to init TextToSpeech", e)
        }

        setContent {
            MyApplicationTheme(dynamicColor = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFF4F7F5) // Warm light pastel grey/white background
                ) {
                    MainScreen(
                        onSpeak = { text -> speakOut(text) },
                        onStopSpeak = { stopSpeaking() }
                    )
                }
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.ENGLISH) // Standard South African study language
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("MainActivity", "TTS language not supported.")
            } else {
                isTtsInitialized = true
                Log.i("MainActivity", "TTS successfully initialized")
            }
        } else {
            Log.e("MainActivity", "TTS Initialization failed.")
        }
    }

    private fun speakOut(text: String) {
        if (isTtsInitialized && tts != null) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "SiphokaziTutor")
        } else {
            runOnUiThread {
                Toast.makeText(this, "Voice synthesis is loading/unsupported on this device", Toast.LENGTH_SHORT).show()
                Log.e("MainActivity", "TTS not initialized. Text: $text")
            }
        }
    }

    private fun stopSpeaking() {
        if (isTtsInitialized && tts != null) {
            tts?.stop()
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

// Active navigation screens
enum class AppScreen {
    DASHBOARD,
    PLANNER,
    SUBJECTS,
    VOICE_CHAT,
    DOCS_PREP
}

@Suppress("DEPRECATION")
@Composable
fun MainScreen(
    onSpeak: (String) -> Unit,
    onStopSpeak: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // --- Durably saved states ---
    var tasks by remember { mutableStateOf(AppStateManager.loadTasks(context)) }
    var stats by remember { mutableStateOf(AppStateManager.loadStats(context)) }
    var docs by remember { mutableStateOf(AppStateManager.loadDocs(context)) }
    var reminders by remember { mutableStateOf(AppStateManager.loadReminders(context)) }

    // --- Local navigation & UX states ---
    var currentScreen by remember { mutableStateOf(AppScreen.DASHBOARD) }
    var avatarState by remember { mutableStateOf(AvatarState.IDLE) }
    var avatarSubtitle by remember { mutableStateOf<String?>("Hi Siphokazi!") }

    // --- Subjects UI states ---
    var selectedSubject by remember { mutableStateOf<SubjectData?>(null) }
    var selectedTopic by remember { mutableStateOf<StudyTopic?>(null) }
    var lessonSummarizedText by remember { mutableStateOf<String?>(null) }
    var isSummarizingWithAi by remember { mutableStateOf(false) }

    // --- Flashcards UI states ---
    var currentCardIndex by remember { mutableStateOf(0) }
    var isCardFlipped by remember { mutableStateOf(false) }

    // --- Quiz Dynamic states ---
    var quizScore by remember { mutableStateOf(0) }
    var currentQuestionIndex by remember { mutableStateOf(0) }
    var quizCompleted by remember { mutableStateOf(false) }
    var selectedMCQAnswer by remember { mutableStateOf("") }
    var fillInAnswerInput by remember { mutableStateOf("") }
    var quizResultFeedback by remember { mutableStateOf<String?>(null) }
    var isAnswerChecked by remember { mutableStateOf(false) }

    // --- Voice/AI Chat states ---
    val chatMessages = remember { mutableStateListOf<Pair<String, Boolean>>() } // Message, isUser
    var chatInput by remember { mutableStateOf("") }
    var isAiGeneratingChat by remember { mutableStateOf(false) }

    // --- Document Prep states ---
    var fileUri by remember { mutableStateOf<Uri?>(null) }
    var fileNameInput by remember { mutableStateOf("") }
    var customNotesTextInput by remember { mutableStateOf("") }
    var isAnalyzingDoc by remember { mutableStateOf(false) }
    var selectedDocForReview by remember { mutableStateOf<CustomStudyDocument?>(null) }
    var docQuizQuestions = remember { mutableStateListOf<QuizQuestionItem>() }

    // --- Daily Motivation logic ---
    var currentMotivation by remember { mutableStateOf("Siphokazi, small study steps today build a massive mountain of success tomorrow in Physical Sciences & Maths! 🌟") }
    var isLoadingMotivation by remember { mutableStateOf(false) }

    // Base XP variables
    val pointsNeededForNextLevel = stats.level * 1000
    val levelProgress = (stats.points.toFloat() / pointsNeededForNextLevel.toFloat()).coerceIn(0f, 1f)

    // Helper functions to grant points, update level and triggers avatar
    fun grantPoints(amount: Int, reason: String) {
        val newPoints = stats.points + amount
        var newLevel = stats.level
        val needed = newLevel * 1000
        val didLevelUp = newPoints >= needed

        if (didLevelUp) {
            newLevel++
            avatarState = AvatarState.CELEBRATING
            avatarSubtitle = "LEVEL UP! LEVEL $newLevel!"
            onSpeak("Congratulations Siphokazi ! You have leveled up to level $newLevel! Your dedication to Mathematics and Physical Sciences is truly paying off. Let's keep this momentum going!")
            Toast.makeText(context, "🎉 LEVEL UP! Level $newLevel", Toast.LENGTH_LONG).show()
        } else {
            avatarState = AvatarState.ENCOURAGING
            avatarSubtitle = "+$amount XP! $reason"
            scope.launch {
                delay(3000)
                if (avatarState == AvatarState.ENCOURAGING) {
                    avatarState = AvatarState.IDLE
                    avatarSubtitle = "Keep going, Siphokazi!"
                }
            }
        }

        // Check and unlock new badges as points accumulate
        val currentBadges = stats.badgesUnlocked.toMutableList()
        val earnedBadges = mutableListOf<String>()

        if (newPoints >= 500 && !currentBadges.contains("Numbers Champion")) {
            earnedBadges.add("Numbers Champion")
        }
        if (tasks.filter { it.isCompleted }.size >= 5 && !currentBadges.contains("Planner Master")) {
            earnedBadges.add("Planner Master")
        }
        if (stats.completedQuizCount >= 3 && !currentBadges.contains("Science Guru")) {
            earnedBadges.add("Science Guru")
        }
        if (docs.size >= 2 && !currentBadges.contains("Super Researcher")) {
            earnedBadges.add("Super Researcher")
        }

        if (earnedBadges.isNotEmpty()) {
            currentBadges.addAll(earnedBadges)
            avatarState = AvatarState.CELEBRATING
            avatarSubtitle = "Unlocked: ${earnedBadges.first()}"
            onSpeak("Amazing work! You've unlocked a new badge: ${earnedBadges.first()}. Keep practicing Siphokazi!")
        }

        stats = stats.copy(
            points = newPoints,
            level = newLevel,
            badgesUnlocked = currentBadges
        )
        AppStateManager.saveStats(context, stats)
    }

    // Trigger on app start
    LaunchedEffect(Unit) {
        if (chatMessages.isEmpty()) {
            chatMessages.add("Hello Siphokazi! I am Siphokazi's personalized AI Tutor. I am loaded with and ready to teach SA Grade 11 & 12 subjects: Mathematics, Physical Sciences, Life Sciences, and Agricultural Sciences. You can also upload custom notes/PDF files. What are we studying today?" to false)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // App Premium Header (with Siphokazi's stylized name, stats banner and avatar quick look)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF4A7A61), Color(0xFF385E4A)) // Deep forest velvet greens
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Interactive Mini Avatar
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .border(1.5.dp, Color.White, CircleShape)
                        .clickable {
                            avatarState = AvatarState.ENCOURAGING
                            avatarSubtitle = "Hello Siphokazi Timba!"
                            onSpeak("I'm right here Siphokazi! Let's conquer South African Physics and Math together!")
                        }
                ) {
                    SiphokaziAvatar(
                        modifier = Modifier.fillMaxSize(),
                        avatarState = avatarState
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Siphokazi Timba Study Hub",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFFFD54F),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Text(
                                text = "Lvl ${stats.level}",
                                color = Color(0xFF424242),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "${stats.points} XP total",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Streak",
                            tint = Color(0xFFFF8A80),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "  ${stats.streakCount} days active",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Level progress bar
                    LinearProgressIndicator(
                        progress = { levelProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = Color(0xFF81C784),
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )
                }
            }
        }

        // Subtitled avatar feedback rail
        if (!avatarSubtitle.isNullOrEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF385E4A).copy(alpha = 0.08f))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Tutor",
                        tint = Color(0xFF385E4A),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = avatarSubtitle ?: "Let's study together!",
                        color = Color(0xFF2C4235),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // View/Screen Content Container
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (currentScreen) {
                AppScreen.DASHBOARD -> {
                    DashboardScreen(
                        stats = stats,
                        reminders = reminders,
                        currentMotivation = currentMotivation,
                        isLoadingMotivation = isLoadingMotivation,
                        onTriggerSync = {
                            stats = stats.copy(streakCount = stats.streakCount + 1)
                            AppStateManager.saveStats(context, stats)
                            grantPoints(100, "Daily attendance streak claimed!")
                        },
                        onGetMotivation = {
                            isLoadingMotivation = true
                            avatarState = AvatarState.THINKING
                            avatarSubtitle = "Thinking of a motivation..."
                            scope.launch {
                                val reply = GeminiClient.generateContent(
                                    prompt = "Generate a customized, highly inspiring 1-sentence motivation for Siphokazi Charity Timba. Focus on Siphokazi doing great in South African matric physical sciences, maths, agricultural sciences, life sciences, and achieving big heights! Keep it positive, emotional, and powerful.",
                                    systemInstruction = "You are Siphokazi's caring mentor. Cheer her on specifically."
                                )
                                currentMotivation = reply
                                isLoadingMotivation = false
                                avatarState = AvatarState.SPEAKING
                                avatarSubtitle = "Listen to your motivation, Siphokazi!"
                                onSpeak(reply)
                            }
                        },
                        onSaveReminders = { updatedReminders ->
                            reminders = updatedReminders
                            AppStateManager.saveReminders(context, reminders)
                            Toast.makeText(context, "Reminders updated!", Toast.LENGTH_SHORT).show()
                            grantPoints(20, "Study alerts enabled")
                        }
                    )
                }

                AppScreen.PLANNER -> {
                    PlannerScreen(
                        tasks = tasks,
                        onAddTask = { title, subject, timeSlot ->
                            val newTask = StudyTask(
                                title = title,
                                subject = subject,
                                dueDate = "Today",
                                timeSlot = timeSlot
                            )
                            tasks = tasks + newTask
                            AppStateManager.saveTasks(context, tasks)
                            grantPoints(25, "New planner study objective added!")
                        },
                        onToggleTask = { taskToToggle ->
                            tasks = tasks.map {
                                if (it.id == taskToToggle.id) {
                                    val nextCompleted = !it.isCompleted
                                    if (nextCompleted) {
                                        grantPoints(50, "Study task completed successfully!")
                                    }
                                    it.copy(isCompleted = nextCompleted)
                                } else it
                            }
                            AppStateManager.saveTasks(context, tasks)
                        },
                        onDeleteTask = { taskToDelete ->
                            tasks = tasks.filter { it.id != taskToDelete.id }
                            AppStateManager.saveTasks(context, tasks)
                        }
                    )
                }

                AppScreen.SUBJECTS -> {
                    SubjectsScreen(
                        selectedSubject = selectedSubject,
                        selectedTopic = selectedTopic,
                        isSummarizingWithAi = isSummarizingWithAi,
                        lessonSummarizedText = lessonSummarizedText,
                        currentCardIndex = currentCardIndex,
                        isCardFlipped = isCardFlipped,
                        quizScore = quizScore,
                        currentQuestionIndex = currentQuestionIndex,
                        quizCompleted = quizCompleted,
                        selectedMCQAnswer = selectedMCQAnswer,
                        fillInAnswerInput = fillInAnswerInput,
                        quizResultFeedback = quizResultFeedback,
                        isAnswerChecked = isAnswerChecked,
                        onSubjectSelect = { subject ->
                            selectedSubject = subject
                            selectedTopic = null
                            lessonSummarizedText = null
                        },
                        onTopicSelect = { topic ->
                            selectedTopic = topic
                            lessonSummarizedText = null
                            // Reset flashcards
                            currentCardIndex = 0
                            isCardFlipped = false
                            // Reset quiz
                            quizScore = 0
                            currentQuestionIndex = 0
                            quizCompleted = false
                            selectedMCQAnswer = ""
                            fillInAnswerInput = ""
                            quizResultFeedback = null
                            isAnswerChecked = false
                        },
                        onSummarizeWithAi = {
                            if (selectedTopic == null) return@SubjectsScreen
                            isSummarizingWithAi = true
                            avatarState = AvatarState.THINKING
                            avatarSubtitle = "AI Tutor is summarizing: ${selectedTopic?.title}"
                            scope.launch {
                                val reply = GeminiClient.generateContent(
                                    prompt = "Provide a warm, structured, and easy-to-understand South African CAPS curriculum summary of topic '${selectedTopic?.title}' which is part of '${selectedSubject?.name}'. Highlight the most crucial formulas, terms, exam cheat notes that Siphokazi should review.",
                                    systemInstruction = "You are Siphokazi's CAPS study assistant. Present results in beautiful bullet points with encouraging callouts."
                                )
                                lessonSummarizedText = reply
                                isSummarizingWithAi = false
                                avatarState = AvatarState.SPEAKING
                                avatarSubtitle = "Here is Siphokazi's customized study sheet!"
                                grantPoints(60, "Personalized AI revision created!")
                                onSpeak("Reviewing lesson summary. Click to listen or start flashcards when ready Siphokazi!")
                            }
                        },
                        onFlipCard = {
                            isCardFlipped = !isCardFlipped
                            grantPoints(5, "Flipped flashcard concepts")
                        },
                        onNextCard = { flashcardsSize ->
                            if (currentCardIndex + 1 < flashcardsSize) {
                                currentCardIndex++
                                isCardFlipped = false
                                grantPoints(15, "Reviewed flashcards")
                            } else {
                                Toast.makeText(context, "Lesson Flashcards Completed! +50 XP", Toast.LENGTH_SHORT).show()
                                grantPoints(50, "Mastered all lesson flashcards!")
                                currentCardIndex = 0
                                isCardFlipped = false
                            }
                        },
                        onSelectMCQ = { answer -> selectedMCQAnswer = answer },
                        onSendFillIn = { ans -> fillInAnswerInput = ans },
                        onCheckQuizAnswer = { currentQuestion ->
                            isAnswerChecked = true
                            val answerGiven = if (currentQuestion.isFillIn) fillInAnswerInput.trim() else selectedMCQAnswer
                            val isCorrect = if (currentQuestion.isFillIn) {
                                answerGiven.lowercase().contains(currentQuestion.correctAnswer.lowercase())
                            } else {
                                answerGiven == currentQuestion.correctAnswer
                            }

                            if (isCorrect) {
                                quizScore++
                                quizResultFeedback = "Correct! Spot on, Siphokazi! 🌟 +30 XP"
                                grantPoints(30, "Quiz question correct!")
                                avatarState = AvatarState.ENCOURAGING
                                onSpeak("Excellent Siphokazi, that is correct!")
                            } else {
                                quizResultFeedback = "Incorrect: The correct answer is: ${currentQuestion.correctAnswer}. ${currentQuestion.hint}"
                                avatarState = AvatarState.THINKING
                                onSpeak("No worries, Siphokazi. Learning is built from mistakes. Look at my hint and let's try next!")
                            }
                        },
                        onNextQuizQuestion = { questionsList ->
                            isAnswerChecked = false
                            selectedMCQAnswer = ""
                            fillInAnswerInput = ""
                            quizResultFeedback = null

                            if (currentQuestionIndex + 1 < questionsList.size) {
                                currentQuestionIndex++
                            } else {
                                quizCompleted = true
                                stats = stats.copy(completedQuizCount = stats.completedQuizCount + 1)
                                AppStateManager.saveStats(context, stats)
                                grantPoints(120, "Completed entire quiz module!")
                                avatarState = AvatarState.CELEBRATING
                                avatarSubtitle = "Quiz over! Final score: $quizScore/${questionsList.size}"
                                onSpeak("High five, Siphokazi! You have completed the quiz. You scored $quizScore out of ${questionsList.size}. It's level-up review time!")
                            }
                        }
                    )
                }

                AppScreen.VOICE_CHAT -> {
                    VoiceChatScreen(
                        messages = chatMessages,
                        chatInput = chatInput,
                        isGenerating = isAiGeneratingChat,
                        onInputChange = { chatInput = it },
                        onSendMessage = {
                            if (chatInput.trim().isEmpty()) return@VoiceChatScreen
                            val userMsg = chatInput
                            chatMessages.add(userMsg to true)
                            chatInput = ""
                            isAiGeneratingChat = true
                            avatarState = AvatarState.THINKING
                            avatarSubtitle = "AI Tutor is drafting answers..."
                            onStopSpeak() // stop speech if running

                            scope.launch {
                                val reply = GeminiClient.generateContent(
                                    prompt = "A high school student named Siphokazi asks you: '$userMsg'. Provide a friendly, comprehensive study response tailored to her South African Grade 11 & 12 courses. Explain concepts step by step if it's Mathematics or Physics.",
                                    systemInstruction = "You are Siphokazi's friendly personal tutor. Speak directly to Siphokazi Charity Timba. Do not write extremely long lines. Keep paragraphs friendly."
                                )
                                chatMessages.add(reply to false)
                                isAiGeneratingChat = false
                                avatarState = AvatarState.SPEAKING
                                avatarSubtitle = "Tutor is responding..."
                                onSpeak(reply)
                            }
                        },
                        onTextToSpeech = { msgText ->
                            avatarState = AvatarState.SPEAKING
                            avatarSubtitle = "Talking..."
                            onSpeak(msgText)
                        },
                        onStopSpeech = {
                            avatarState = AvatarState.IDLE
                            avatarSubtitle = "Ready!"
                            onStopSpeak()
                        }
                    )
                }

                AppScreen.DOCS_PREP -> {
                    DocsPrepScreen(
                        docs = docs,
                        selectedDoc = selectedDocForReview,
                        isAnalyzing = isAnalyzingDoc,
                        fileNameInput = fileNameInput,
                        customNotesInput = customNotesTextInput,
                        docQuizQuestions = docQuizQuestions,
                        onFileNameChange = { fileNameInput = it },
                        onNotesChange = { customNotesTextInput = it },
                        onSelectFileFromSystem = { uri ->
                            fileUri = uri
                            // auto set folder name as user selected file if possible
                            val contextResolver = context.contentResolver
                            var tempName = "StudyNotes_${System.currentTimeMillis()}"
                            try {
                                contextResolver.query(uri, null, null, null, null)?.use { cursor ->
                                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                                    if (cursor.moveToFirst() && nameIdx != -1) {
                                        tempName = cursor.getString(nameIdx)
                                    }
                                }
                            } catch (e: Exception) {
                                // fallback
                            }
                            fileNameInput = tempName
                        },
                        onAnalyzeDoc = {
                            if (fileNameInput.trim().isEmpty()) {
                                Toast.makeText(context, "Please label your custom study materials first!", Toast.LENGTH_SHORT).show()
                                return@DocsPrepScreen
                            }
                            isAnalyzingDoc = true
                            avatarState = AvatarState.THINKING
                            avatarSubtitle = "Learning uploaded study sheet..."

                            val docType = when {
                                fileNameInput.endsWith(".pdf", true) -> "PDF Study File"
                                fileNameInput.endsWith(".ppt", true) || fileNameInput.endsWith(".pptx", true) -> "PowerPoint Presentation"
                                fileNameInput.endsWith(".xls", true) || fileNameInput.endsWith(".xlsx", true) -> "Excel Spreadsheet"
                                else -> "Text Notes Notes/Guide"
                            }

                            // Read physical content if pasted text is empty
                            val sourceNotesText = if (customNotesTextInput.trim().isNotEmpty()) {
                                customNotesTextInput
                            } else if (fileUri != null) {
                                // Try reading short stream for simulation
                                try {
                                    val stream = context.contentResolver.openInputStream(fileUri!!)
                                    val reader = BufferedReader(InputStreamReader(stream))
                                    val sb = StringBuilder()
                                    var line: String? = reader.readLine()
                                    var lineCount = 0
                                    while (line != null && lineCount < 100) {
                                        sb.append(line).append("\n")
                                        line = reader.readLine()
                                        lineCount++
                                    }
                                    sb.toString()
                                } catch (e: Exception) {
                                    "Physical study documents referenced: $fileNameInput ($docType)"
                                }
                            } else {
                                "Past exams preparation study sheet summaries!"
                            }

                            scope.launch {
                                // First generate revision notes
                                val summaryResult = GeminiClient.generateContent(
                                    prompt = "Analyze the student study material labeled '$fileNameInput'. Content sample: '$sourceNotesText'. Produce a highly organized Grade 11/12 summary for Siphokazi Timba. Highlight 5 crucial test questions",
                                    systemInstruction = "You are a master examiner preparing Siphokazi for Matric exams."
                                )

                                // Then generate raw JSON quiz questions
                                val quizResultRaw = GeminiClient.generateContent(
                                    prompt = "Based on Siphokazi's study material labeled '$fileNameInput': '$sourceNotesText', generate exactly 3 practice quiz questions (2 MCQs, 1 Fill-in). Send back exactly a valid JSON array format, where each object has fields: 'question', 'options' (array of strings, keep blank for fill-in), 'correctAnswer', 'isFillIn' (boolean), and 'hint'. ONLY output JSON, nothing else.",
                                    systemInstruction = "Output valid JSON text array only."
                                )

                                val parsedDocs = docs.toMutableList()
                                val newDoc = CustomStudyDocument(
                                    fileName = fileNameInput,
                                    fileType = docType,
                                    generatedSummary = summaryResult,
                                    generatedQuizRaw = quizResultRaw
                                )
                                parsedDocs.add(newDoc)
                                docs = parsedDocs
                                AppStateManager.saveDocs(context, docs)

                                fileUri = null
                                fileNameInput = ""
                                customNotesTextInput = ""
                                isAnalyzingDoc = false
                                selectedDocForReview = newDoc
                                avatarState = AvatarState.CELEBRATING
                                avatarSubtitle = "Ready! Analyzed: ${newDoc.fileName}"
                                onSpeak("I have thoroughly reviewed Siphokazi's personal exam preparation document. I created a custom summary and practice questions!")
                                grantPoints(150, "Pasted notes/PDF analyzed with Gemini!")
                            }
                        },
                        onReviewDoc = { doc ->
                            selectedDocForReview = doc
                            // parse doc questions
                            docQuizQuestions.clear()
                            try {
                                val arr = JSONArray(doc.generatedQuizRaw)
                                for (i in 0 until arr.length()) {
                                    val obj = arr.getJSONObject(i)
                                    val optArr = obj.optJSONArray("options")
                                    val opts = mutableListOf<String>()
                                    if (optArr != null) {
                                        for (j in 0 until optArr.length()) {
                                            opts.add(optArr.getString(j))
                                        }
                                    }
                                    docQuizQuestions.add(
                                        QuizQuestionItem(
                                            question = obj.getString("question"),
                                            options = opts,
                                            correctAnswer = obj.getString("correctAnswer"),
                                            isFillIn = obj.optBoolean("isFillIn", false),
                                            hint = obj.optString("hint", "")
                                        )
                                    )
                                }
                            } catch (e: Exception) {
                                // add fallback questions
                                docQuizQuestions.addAll(
                                    listOf(
                                        QuizQuestionItem("A basic concept reviewed is essential for Matric prep. True or False?", listOf("True", "False"), "True", false, "Always reviews notes!"),
                                        QuizQuestionItem("Solve: Revision represents the key to examination success.", correctAnswer = "success", isFillIn = true, hint = "Focus on the finish line")
                                    )
                                )
                            }
                        },
                        onDeleteDoc = { doc ->
                            docs = docs.filter { it.id != doc.id }
                            AppStateManager.saveDocs(context, docs)
                            if (selectedDocForReview?.id == doc.id) {
                                selectedDocForReview = null
                            }
                        }
                    )
                }
            }
        }

        // App bottom bar navigation (elegant icon buttons tailored to theme)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(bottom = 6.dp)
        ) {
            HorizontalDivider(color = Color(0xFFE0E0E0))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavigationItem(
                    icon = Icons.Default.Home,
                    label = "Hub",
                    isSelected = currentScreen == AppScreen.DASHBOARD,
                    onClick = { currentScreen = AppScreen.DASHBOARD }
                )
                NavigationItem(
                    icon = Icons.Default.DateRange,
                    label = "Planner",
                    isSelected = currentScreen == AppScreen.PLANNER,
                    onClick = { currentScreen = AppScreen.PLANNER }
                )
                NavigationItem(
                    icon = Icons.Default.Star,
                    label = "Subjects",
                    isSelected = currentScreen == AppScreen.SUBJECTS,
                    onClick = { currentScreen = AppScreen.SUBJECTS }
                )
                NavigationItem(
                    icon = Icons.Default.Phone,
                    label = "AI Voice",
                    isSelected = currentScreen == AppScreen.VOICE_CHAT,
                    onClick = { currentScreen = AppScreen.VOICE_CHAT }
                )
                NavigationItem(
                    icon = Icons.Default.Build,
                    label = "Prep Files",
                    isSelected = currentScreen == AppScreen.DOCS_PREP,
                    onClick = { currentScreen = AppScreen.DOCS_PREP }
                )
            }
        }
    }
}

@Composable
fun RowScope.NavigationItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val tint = if (isSelected) Color(0xFF385E4A) else Color(0xFF757575)
    val fontW = if (isSelected) FontWeight.Bold else FontWeight.Medium

    Column(
        modifier = Modifier
            .weight(1f)
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(23.dp)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = tint,
            fontWeight = fontW,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

// ================= MODULE 1: INTERACTIVE DASHBOARD SCREEN =================
@Composable
fun DashboardScreen(
    stats: UserStats,
    reminders: List<StudyReminder>,
    currentMotivation: String,
    isLoadingMotivation: Boolean,
    onTriggerSync: () -> Unit,
    onGetMotivation: () -> Unit,
    onSaveReminders: (List<StudyReminder>) -> Unit
) {
    var showReminderDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome banner
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F0EC))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Welcome back, Siphokazi! 👋",
                            color = Color(0xFF2E4D3E),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "South Africa Grade 11 & 12 Study Companion is online. Let's practice with AI and unlock high marks!",
                            color = Color(0xFF4A6858),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        // Daily Motivation Card
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFFECEB),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "Love",
                                    tint = Color(0xFFFF5252),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Daily Motivation for Siphokazi",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF2C4235)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (isLoadingMotivation) {
                        CircularProgressIndicator(
                            color = Color(0xFF385E4A),
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.CenterHorizontally)
                        )
                    } else {
                        Text(
                            text = currentMotivation,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Start,
                            color = Color(0xFF333333),
                            lineHeight = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onGetMotivation,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF385E4A)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Get Personalized AI Voice & Text Motivation", fontSize = 11.sp)
                    }
                }
            }
        }

        // Streak claim
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Daily attendance checker",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF2C4235)
                    )
                    Text(
                        text = "Check in every day to grow your streak multiplier. Complete Siphokazi's goals to climb levels!",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onTriggerSync,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8F0EC)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("🔥 Claim daily attendance streak (+100 XP)", color = Color(0xFF2E4D3E), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Study Settings & Reminders Quickview
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Configure Study & Rest Alerts",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF2C4235)
                        )
                        IconButton(onClick = { showReminderDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF385E4A))
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    reminders.forEach { reminder ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (reminder.type == "Rest Alert") Icons.Default.Favorite else Icons.Default.Info,
                                    contentDescription = reminder.type,
                                    tint = if (reminder.type == "Rest Alert") Color(0xFFFF8A80) else Color(0xFF385E4A),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(reminder.title, fontSize = 12.sp, color = Color(0xFF333333))
                            }
                            Text(
                                text = "⏰ ${reminder.timeOfDay} (${if (reminder.enabled) "Active" else "Off"})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (reminder.enabled) Color(0xFF385E4A) else Color.Gray
                            )
                        }
                    }
                }
            }
        }

        // Badges Achievements collection
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Siphokazi's Unlocked Badges",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF2C4235)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val allSupportedBadges = listOf(
                        Triple("First Step", "Logged in to study portal", "🎓"),
                        Triple("Planner Master", "Completed 5 study tasks", "📅"),
                        Triple("Science Guru", "Completed 3 CAPS exam quizzes", "⚡"),
                        Triple("Super Researcher", "Created custom study guides", "📂"),
                        Triple("Numbers Champion", "Reached 500+ XP points", "📈")
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        allSupportedBadges.forEach { (badge, desc, emoji) ->
                            val isUnlocked = stats.badgesUnlocked.contains(badge)
                            val containerCol = if (isUnlocked) Color(0xFFE8F0EC) else Color(0xFFF5F5F5)
                            val textCol = if (isUnlocked) Color(0xFF2C4235) else Color.Gray

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(containerCol)
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(emoji, fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(badge, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textCol)
                                    Text(desc, fontSize = 10.sp, color = textCol.copy(alpha = 0.8f))
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                if (isUnlocked) {
                                    Icon(Icons.Default.Check, contentDescription = "Earned", tint = Color(0xFF385E4A), modifier = Modifier.size(16.dp))
                                } else {
                                    Icon(Icons.Default.Lock, contentDescription = "Locked", tint = Color.Gray, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal dialogue to customize study alerts
    if (showReminderDialog) {
        AlertDialog(
            onDismissRequest = { showReminderDialog = false },
            title = { Text("Study & Rest Reminder Settings", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select study schedules and alerts below. This helps Siphokazi allocate study hours and restful breaks.", fontSize = 11.sp, color = Color.Gray)
                    // Custom preset adjustments in an easy interface
                    reminders.forEachIndexed { idx, item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(item.title, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            Switch(
                                checked = item.enabled,
                                onCheckedChange = { isChecked ->
                                    val updated = reminders.toMutableList()
                                    updated[idx] = item.copy(enabled = isChecked)
                                    onSaveReminders(updated)
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showReminderDialog = false }) {
                    Text("Save", color = Color(0xFF385E4A))
                }
            }
        )
    }
}

// ================= MODULE 2: DAILY STUDY PLANNER SCREEN =================
@Composable
fun PlannerScreen(
    tasks: List<StudyTask>,
    onAddTask: (String, String, String) -> Unit,
    onToggleTask: (StudyTask) -> Unit,
    onDeleteTask: (StudyTask) -> Unit
) {
    var titleInput by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf("Mathematics") }
    var timeSlotInput by remember { mutableStateOf("15:00 - 15:45") }

    val subjectsAvailable = listOf("Mathematics", "Physical Sciences", "Life Sciences", "Agricultural Sciences", "Rest", "Other")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Siphokazi's Daily Planner",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Color(0xFF2C4235)
        )
        Text(
            text = "Fulfill daily targets to gain study points. Tick off checkpoints when finishing revision sessions!",
            color = Color.Gray,
            fontSize = 11.sp
        )

        // Task Adder Form
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Schedule Study Task", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF385E4A))

                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    label = { Text("Task e.g. Revise Newton Laws") },
                    textStyle = TextStyle(fontSize = 12.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Subject", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        // Simple custom dropdown selector
                        var expanded by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                                .clickable { expanded = true }
                                .padding(8.dp)
                        ) {
                            Text(selectedSubject, fontSize = 11.sp)
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                subjectsAvailable.forEach { subj ->
                                    DropdownMenuItem(
                                        text = { Text(subj, fontSize = 11.sp) },
                                        onClick = {
                                            selectedSubject = subj
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text("Time / Hours", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        OutlinedTextField(
                            value = timeSlotInput,
                            onValueChange = { timeSlotInput = it },
                            textStyle = TextStyle(fontSize = 11.sp),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        if (titleInput.isEmpty()) return@Button
                        onAddTask(titleInput, selectedSubject, timeSlotInput)
                        titleInput = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF385E4A)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("+ Add to Daily Plan (+25 XP)", fontSize = 12.sp)
                }
            }
        }

        // Tasks checklist list
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tasks) { task ->
                val cardAlpha = if (task.isCompleted) 0.6f else 1f
                val txtDecoration = if (task.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null

                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (task.isCompleted) Color(0xFFF1F1F1) else Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = task.isCompleted,
                            onCheckedChange = { onToggleTask(task) }
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = task.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (task.isCompleted) Color.Gray else Color(0xFF333333),
                                textDecoration = txtDecoration
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                                Text(
                                    text = task.subject,
                                    fontSize = 10.sp,
                                    color = when (task.subject) {
                                        "Mathematics" -> Color(0xFF6650a4)
                                        "Physical Sciences" -> Color(0xFF1E88E5)
                                        "Life Sciences" -> Color(0xFF43A047)
                                        "Agricultural Sciences" -> Color(0xFFD84315)
                                        else -> Color.Gray
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "⏰ ${task.timeSlot}",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        IconButton(onClick = { onDeleteTask(task) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = Color(0xFFFF8A80),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ================= MODULE 3: CAPS STUDY MODULES & QUIZ SCREEN =================
@Composable
fun SubjectsScreen(
    selectedSubject: SubjectData?,
    selectedTopic: StudyTopic?,
    isSummarizingWithAi: Boolean,
    lessonSummarizedText: String?,
    currentCardIndex: Int,
    isCardFlipped: Boolean,
    quizScore: Int,
    currentQuestionIndex: Int,
    quizCompleted: Boolean,
    selectedMCQAnswer: String,
    fillInAnswerInput: String,
    quizResultFeedback: String?,
    isAnswerChecked: Boolean,
    onSubjectSelect: (SubjectData) -> Unit,
    onTopicSelect: (StudyTopic) -> Unit,
    onSummarizeWithAi: () -> Unit,
    onFlipCard: () -> Unit,
    onNextCard: (Int) -> Unit,
    onSelectMCQ: (String) -> Unit,
    onSendFillIn: (String) -> Unit,
    onCheckQuizAnswer: (QuizQuestionItem) -> Unit,
    onNextQuizQuestion: (List<QuizQuestionItem>) -> Unit
) {
    if (selectedSubject == null) {
        // List SA CAPS Grade 11 & 12 Subjects
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "South African CAPS Subjects",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color(0xFF2C4235)
            )
            Text(
                text = "Select a grade module below to study summaries, flip flashcards, and write micro interactive quizzes!",
                color = Color.Gray,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            StaticContent.subjectsList.forEach { matSubj ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable { onSubjectSelect(matSubj) },
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = when (matSubj.category) {
                                "Mathematics" -> Color(0xFFF3E5F5)
                                "Physical Sciences" -> Color(0xFFE3F2FD)
                                "Life Sciences" -> Color(0xFFE8F5E9)
                                "Agricultural Sciences" -> Color(0xFFFFE0B2)
                                else -> Color(0xFFECEFF1)
                            },
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = when (matSubj.category) {
                                        "Mathematics" -> "📐"
                                        "Physical Sciences" -> "⚡"
                                        "Life Sciences" -> "🧬"
                                        "Agricultural Sciences" -> "🌾"
                                        else -> "📚"
                                    },
                                    fontSize = 20.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(matSubj.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                "Grade ${matSubj.grade} curriculum lesson topics",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Go",
                            tint = Color(0xFF385E4A),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    } else if (selectedTopic == null) {
        // Topic lists for this subject
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = { onSubjectSelect(selectedSubject) }) { // Go transition
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF385E4A))
                }
                Text(
                    text = selectedSubject.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF2C4235),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(selectedSubject.topics) { topic ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTopicSelect(topic) },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(topic.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF333333))
                            Text(
                                "Includes ${topic.flashcards.size} Interactive Flashcards and ${topic.quizQuestions.size} exam practice questions",
                                color = Color.Gray,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    } else {
        // Study Zone with Sub-pages: 1. Lesson Notes / Summary, 2. Flashcards, 3. Quiz Game
        var selectedSubTab by remember { mutableStateOf(0) } // 0: Notes, 1: Flashcards, 2: Quiz

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = { selectedSubject.topics.find { it == selectedTopic }?.let { onTopicSelect(it) } }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF385E4A))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(selectedTopic.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF333333))
                    Text(selectedSubject.name, fontSize = 10.sp, color = Color.Gray)
                }
            }

            // Tabs indicator
            TabRow(
                selectedTabIndex = selectedSubTab,
                containerColor = Color.Transparent,
                contentColor = Color(0xFF385E4A)
            ) {
                Tab(selected = selectedSubTab == 0, onClick = { selectedSubTab = 0 }) {
                    Text("Lessons Notes", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(10.dp))
                }
                Tab(selected = selectedSubTab == 1, onClick = { selectedSubTab = 1 }) {
                    Text("Flashcards (${selectedTopic.flashcards.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(10.dp))
                }
                Tab(selected = selectedSubTab == 2, onClick = { selectedSubTab = 2 }) {
                    Text("Interactive Quiz", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(10.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sub contents
            when (selectedSubTab) {
                0 -> {
                    // Lesons Summary notes
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Standard CAPS Summary", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF385E4A))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = selectedTopic.summary,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                    color = Color(0xFF424242)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Trigger Gemini live content summaries
                        if (lessonSummarizedText == null) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F0EC))
                            ) {
                                Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("PERSONALIZED EXAM PREPARATION 📄", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF2E4D3E))
                                    Text("Analyze with Siphokazi's AI Study companion to receive specialized, easy keypoints, mnemonic devices, and sample queries for Siphokazi!", fontSize = 11.sp, color = Color(0xFF4A6858), textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 6.dp))

                                    Button(
                                        onClick = onSummarizeWithAi,
                                        enabled = !isSummarizingWithAi,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF385E4A)),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        if (isSummarizingWithAi) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Matric AI is Summarizing...", fontSize = 11.sp)
                                            }
                                        } else {
                                            Text("🔮 Summarize & Explain with Gemini AI (+60 XP)", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        } else {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFCF3)), // Lovely vintage papyrus yellow summary
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0xFFFFD54F), RoundedCornerShape(10.dp))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("✨ Siphokazi's Automated AI Summary", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFD84315), modifier = Modifier.weight(1f))
                                        IconButton(onClick = onSummarizeWithAi) {
                                            Icon(Icons.Default.Refresh, contentDescription = "Redo", tint = Color(0xFFD84315), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = lessonSummarizedText,
                                        fontSize = 12.sp,
                                        lineHeight = 18.sp,
                                        color = Color(0xFF2E2E2E)
                                    )
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // Flashcards section
                    if (selectedTopic.flashcards.isEmpty()) {
                        Text("No flashcard study lists for this topic", color = Color.Gray, fontSize = 12.sp)
                    } else {
                        val currentCard = selectedTopic.flashcards[currentCardIndex]
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Card ${currentCardIndex + 1} of ${selectedTopic.flashcards.size}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)

                            Spacer(modifier = Modifier.height(12.dp))

                            // Actual card flipper
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clickable { onFlipCard() },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isCardFlipped) Color(0xFFE8F0EC) else Color.White
                                ),
                                border = BorderStroke(1.5.dp, if (isCardFlipped) Color(0xFF385E4A) else Color(0xFFE0E0E0))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isCardFlipped) {
                                        Text(
                                            text = currentCard.back,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF2E4D3E),
                                            textAlign = TextAlign.Center
                                        )
                                    } else {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = currentCard.front,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF333333),
                                                textAlign = TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text("(Tap to flip card 🔄)", fontSize = 10.sp, color = Color.Gray)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onFlipCard,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, Color(0xFF385E4A))
                                ) {
                                    Text("Flip 🔄", color = Color(0xFF385E4A), fontSize = 12.sp)
                                }

                                Button(
                                    onClick = { onNextCard(selectedTopic.flashcards.size) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF385E4A))
                                ) {
                                    Text("Got It! Next (+15 XP) 👉", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // Native dynamic Interactive Quizzes with both MCQ and Fill-ins
                    if (selectedTopic.quizQuestions.isEmpty()) {
                        Text("No quizzes available for this lesson", color = Color.Gray, fontSize = 12.sp)
                    } else if (quizCompleted) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("🎉 Lesson Quiz Finished! 🎉", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF385E4A))
                            Text("Total Score: $quizScore / ${selectedTopic.quizQuestions.size}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))

                            val passRatio = quizScore.toFloat() / selectedTopic.quizQuestions.size.toFloat()
                            Text(
                                text = if (passRatio >= 0.7f) "Amazing job, Siphokazi! You are ready for exams!" else "Good practice! Re-read lesson summary to fetch 100% next study block!",
                                textAlign = TextAlign.Center,
                                fontSize = 11.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(14.dp)
                            )

                            Button(
                                onClick = {
                                    // re-trigger quiz
                                    onTopicSelect(selectedTopic)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF385E4A))
                            ) {
                                Text("Re-run Practice Quiz", fontSize = 12.sp)
                            }
                        }
                    } else {
                        val currentQuestion = selectedTopic.quizQuestions[currentQuestionIndex]
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text("Question ${currentQuestionIndex + 1} of ${selectedTopic.quizQuestions.size}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)

                            Spacer(modifier = Modifier.height(8.dp))

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(currentQuestion.question, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF333333))
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            if (currentQuestion.isFillIn) {
                                // Render Fill-In quiz entry
                                Text("Type the correct answer:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                OutlinedTextField(
                                    value = fillInAnswerInput,
                                    onValueChange = onSendFillIn,
                                    label = { Text("Answer Keyword") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    singleLine = true,
                                    enabled = !isAnswerChecked
                                )
                            } else {
                                // Render MCQ selection
                                Text("Select the correct option:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                                    currentQuestion.options.forEach { option ->
                                        val isSelected = selectedMCQAnswer == option
                                        val optCol = if (isSelected) Color(0xFFE8F0EC) else Color.White
                                        val strokeCol = if (isSelected) Color(0xFF385E4A) else Color(0xFFE0E0E0)

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(optCol)
                                                .border(1.0.dp, strokeCol, RoundedCornerShape(8.dp))
                                                .clickable(enabled = !isAnswerChecked) { onSelectMCQ(option) }
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = { onSelectMCQ(option) },
                                                enabled = !isAnswerChecked
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(option, fontSize = 12.sp, color = Color(0xFF333333))
                                        }
                                    }
                                }
                            }

                            // Checked reply result banner
                            if (isAnswerChecked && quizResultFeedback != null) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (quizResultFeedback.startsWith("Correct")) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                                    )
                                ) {
                                    Text(
                                        quizResultFeedback,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (quizResultFeedback.startsWith("Correct")) Color(0xFF2E7D32) else Color(0xFFC62828),
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Controls button: Check answer -> Next
                            if (!isAnswerChecked) {
                                Button(
                                    onClick = { onCheckQuizAnswer(currentQuestion) },
                                    enabled = if (currentQuestion.isFillIn) fillInAnswerInput.isNotEmpty() else selectedMCQAnswer.isNotEmpty(),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF385E4A)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Check Answer", fontSize = 12.sp)
                                }
                            } else {
                                Button(
                                    onClick = { onNextQuizQuestion(selectedTopic.quizQuestions) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = if (currentQuestionIndex + 1 < selectedTopic.quizQuestions.size) "Next Question 👉" else "Refine & Finalize Results 📊",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
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

// ================= MODULE 4: AI VOICE & CHAT TUTOR SCREEN =================
@Composable
fun VoiceChatScreen(
    messages: List<Pair<String, Boolean>>,
    chatInput: String,
    isGenerating: Boolean,
    onInputChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onTextToSpeech: (String) -> Unit,
    onStopSpeech: () -> Unit
) {
    val scrollState = rememberScrollState()

    // Auto scroll chat to end when messages increase
    LaunchedEffect(key1 = messages.size) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Text(
            text = "AI Voice/Text Study Tutor",
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = Color(0xFF2C4235)
        )
        Text(
            text = "Ask questions, clear doubts on Physics formulas, math theorems, or summaries. Your AI will read answers out loud!",
            color = Color.Gray,
            fontSize = 10.sp
        )

        // Shortcuts panel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "Explain the Doppler Effect" to "Please explain Doppler Effect, sound frequencies, and standard medical usages.",
                "Trig Sine Rule help" to "How do I calculate side values with the Sine and Cosine rules in Mathematics?",
                "Newton's 2nd Law equation" to "Show me step-by-step how to solve F_net = m * a problems for Grade 11 Physical Sciences.",
                "getRumen fermentation" to "Tell me why ruminant digestion is polygastric and how agricultural feeds classification works."
            ).forEach { (label, fullPrompt) ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFE8F0EC),
                    modifier = Modifier.clickable { onInputChange(fullPrompt) }
                ) {
                    Text(
                        text = label,
                        color = Color(0xFF2E4D3E),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Messages Box
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .background(Color.White, RoundedCornerShape(8.dp))
                .border(0.5.dp, Color.LightGray, RoundedCornerShape(8.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            messages.forEach { (text, isUser) ->
                val bubbleColor = if (isUser) Color(0xFFE8F0EC) else Color(0xFFF5F5F5)
                val alignment = if (isUser) Alignment.End else Alignment.Start
                val textCol = if (isUser) Color(0xFF2E4D3E) else Color(0xFF333333)

                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .clip(
                                RoundedCornerShape(
                                    topStart = 10.dp,
                                    topEnd = 10.dp,
                                    bottomStart = if (isUser) 10.dp else 0.dp,
                                    bottomEnd = if (isUser) 0.dp else 10.dp
                                )
                            )
                            .background(bubbleColor)
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(text, fontSize = 12.sp, color = textCol, lineHeight = 17.sp)

                            if (!isUser) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    horizontalArrangement = Arrangement.End,
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(onClick = { onTextToSpeech(text) }) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = "Listen", tint = Color(0xFF385E4A), modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Listen", color = Color(0xFF385E4A), fontSize = 10.sp)
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    TextButton(onClick = onStopSpeech) {
                                        Icon(Icons.Default.Close, contentDescription = "Mute", tint = Color.Gray, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Stop Voice", color = Color.Gray, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isGenerating) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(6.dp)
                ) {
                    CircularProgressIndicator(color = Color(0xFF385E4A), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI Tutor is thinking...", fontSize = 11.sp, color = Color.Gray)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Input field Siphokazi
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = chatInput,
                onValueChange = onInputChange,
                textStyle = TextStyle(fontSize = 12.sp),
                placeholder = { Text("Ask anything...") },
                modifier = Modifier.weight(1f),
                singleLine = false,
                maxLines = 3,
                trailingIcon = {
                    IconButton(onClick = {
                        // voice alerts triggers
                        onInputChange("Can you explain how agricultural lime acts on acidic soil to increase pH levels?")
                    }) {
                        Icon(Icons.Default.Star, contentDescription = "Voice Speech Suggestion", tint = Color(0xFF385E4A))
                    }
                }
            )

            Button(
                onClick = onSendMessage,
                enabled = chatInput.isNotEmpty() && !isGenerating,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF385E4A)),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.height(52.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send")
            }
        }
    }
}

// ================= MODULE 5: CUSTOM STUDY PREPARATION (FILE PARSER) SCREEN =================
@Composable
fun DocsPrepScreen(
    docs: List<CustomStudyDocument>,
    selectedDoc: CustomStudyDocument?,
    isAnalyzing: Boolean,
    fileNameInput: String,
    customNotesInput: String,
    docQuizQuestions: List<QuizQuestionItem>,
    onFileNameChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onSelectFileFromSystem: (Uri) -> Unit,
    onAnalyzeDoc: () -> Unit,
    onReviewDoc: (CustomStudyDocument) -> Unit,
    onDeleteDoc: (CustomStudyDocument) -> Unit
) {
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onSelectFileFromSystem(uri)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Personal Exam Prep (Document upload)",
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = Color(0xFF2C4235)
        )
        Text(
            text = "Piped custom study documents, checklists, spreadsheets, or syllabus text instantly into summaries and practice questions!",
            color = Color.Gray,
            fontSize = 10.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (selectedDoc == null) {
            // Screen 1: File selector adder details
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Select study document or paste notes", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF385E4A))

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { filePickerLauncher.launch("*/*") }, // supports pdf, ppt, excels, etc.
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8F0EC))
                        ) {
                            Text("📂 Pick study file", color = Color(0xFF2E4D3E), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                onFileNameChange("Matric Past Paper Prep.pdf")
                                onNotesChange("Grade 12 Physics exam prep notes: Electrostatics focus on Coulomb's law: F = k * q1 * q2 / r²; electric fields are vector fields represented by E = F_q; circuits state Ohm's law: R = V/I.")
                            },
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFECEB))
                        ) {
                            Text("⚡ Load past paper simulation", color = Color(0xFFC62828), fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = fileNameInput,
                        onValueChange = onFileNameChange,
                        label = { Text("Label e.g. LifeSciencesRevision.pptx") },
                        textStyle = TextStyle(fontSize = 12.sp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = customNotesInput,
                        onValueChange = onNotesChange,
                        label = { Text("Paste custom syllabus notes or exam papers...") },
                        textStyle = TextStyle(fontSize = 12.sp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .padding(vertical = 6.dp),
                        maxLines = 10
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = onAnalyzeDoc,
                        enabled = !isAnalyzing,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF385E4A)),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        if (isAnalyzing) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Analyzing study sheet...", fontSize = 11.sp)
                            }
                        } else {
                            Text("Analyze with Gemini AI brain (+150 XP)", fontSize = 11.sp)
                        }
                    }
                }
            }

            // Existing summaries list
            Text("Siphokazi's study documents bank", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF333333))

            if (docs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 30.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No personalized prep documents analyzed yet.", color = Color.Gray, fontSize = 12.sp)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                    docs.forEach { doc ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onReviewDoc(doc) },
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when (doc.fileType) {
                                        "PDF Study File" -> "📕"
                                        "PowerPoint Presentation" -> "📙"
                                        "Excel Spreadsheet" -> "📗"
                                        else -> "📝"
                                    },
                                    fontSize = 24.sp
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(doc.fileName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF333333))
                                    Text("Analyzed: ${doc.fileType}", fontSize = 10.sp, color = Color.Gray)
                                }

                                IconButton(onClick = { onDeleteDoc(doc) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF8A80), modifier = Modifier.size(15.dp))
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Screen 2: Detailed customized exam review area
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = { onReviewDoc(selectedDoc) // toggle close
                    // set to null manually
                    onNotesChange("") // just simple toggle back
                }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF385E4A))
                }
                Text("Preparation Guide:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.weight(1f))
                Button(
                    onClick = { onReviewDoc(selectedDoc) // clear selection
                        // we can set a state in composable to close it
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFE3E3))
                ) {
                    Text("Close Review", color = Color(0xFFC62828), fontSize = 10.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFCF3)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFFFD54F), RoundedCornerShape(10.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("✨ Premium AI Study Plan for: ${selectedDoc.fileName}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFD84315))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(selectedDoc.generatedSummary, fontSize = 12.sp, lineHeight = 18.sp, color = Color(0xFF232323))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Auto-generated quiz section Siphokazi can practice!
            Text("Practice Test Questions generated from material", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF333333))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                docQuizQuestions.forEachIndexed { qIdx, item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Q${qIdx + 1}: ${item.question}", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                            if (item.isFillIn) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                                    Text("Correct Keyword: ", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                    Text(item.correctAnswer, fontSize = 11.sp, color = Color(0xFF385E4A), fontWeight = FontWeight.Bold)
                                }
                            } else {
                                item.options.forEach { opt ->
                                    val isAnswerKey = opt == item.correctAnswer
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isAnswerKey) Icons.Default.CheckCircle else Icons.Default.Info,
                                            contentDescription = "Opt",
                                            tint = if (isAnswerKey) Color(0xFF43A047) else Color.Gray,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(opt, fontSize = 11.sp, color = if (isAnswerKey) Color(0xFF43A047) else Color(0xFF333333))
                                    }
                                }
                            }
                            if (item.hint.isNotEmpty()) {
                                Text("💡 Hint: ${item.hint}", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
