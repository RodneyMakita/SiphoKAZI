package com.example

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

// Planner study tasks
data class StudyTask(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val subject: String, // "Mathematics", "Physical Sciences", "Life Sciences", "Agricultural Sciences", "Rest", "Other"
    val isCompleted: Boolean = false,
    val dueDate: String, // e.g. "Today" or specific date
    val timeSlot: String // e.g. "14:00 - 15:00"
) {
    fun toJsonObject(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("subject", subject)
        put("isCompleted", isCompleted)
        put("dueDate", dueDate)
        put("timeSlot", timeSlot)
    }

    companion object {
        fun fromJsonObject(obj: JSONObject): StudyTask = StudyTask(
            id = obj.optString("id", UUID.randomUUID().toString()),
            title = obj.getString("title"),
            subject = obj.optString("subject", "Other"),
            isCompleted = obj.optBoolean("isCompleted", false),
            dueDate = obj.optString("dueDate", "Today"),
            timeSlot = obj.optString("timeSlot", "Flexible")
        )
    }
}

// User points, Level, Badges, Streaks
data class UserStats(
    val points: Int = 0,
    val level: Int = 1,
    val streakCount: Int = 1,
    val lastStudyTimestamp: Long = System.currentTimeMillis(),
    val completedQuizCount: Int = 0,
    val badgesUnlocked: List<String> = listOf("First Step") // badge titles
) {
    fun toJsonObject(): JSONObject = JSONObject().apply {
        put("points", points)
        put("level", level)
        put("streakCount", streakCount)
        put("lastStudyTimestamp", lastStudyTimestamp)
        put("completedQuizCount", completedQuizCount)
        put("badgesUnlocked", JSONArray(badgesUnlocked))
    }

    companion object {
        fun fromJsonObject(obj: JSONObject): UserStats {
            val badgeArr = obj.optJSONArray("badgesUnlocked")
            val badges = mutableListOf<String>()
            if (badgeArr != null) {
                for (i in 0 until badgeArr.length()) {
                    badges.add(badgeArr.getString(i))
                }
            } else {
                badges.add("First Step")
            }
            return UserStats(
                points = obj.optInt("points", 0),
                level = obj.optInt("level", 1),
                streakCount = obj.optInt("streakCount", 1),
                lastStudyTimestamp = obj.optLong("lastStudyTimestamp", System.currentTimeMillis()),
                completedQuizCount = obj.optInt("completedQuizCount", 0),
                badgesUnlocked = badges
            )
        }
    }
}

// User-uploaded documents and personalized prepared notes
data class CustomStudyDocument(
    val id: String = UUID.randomUUID().toString(),
    val fileName: String,
    val fileType: String, // "PDF", "PPT", "Excel", "Text Notes"
    val timestamp: Long = System.currentTimeMillis(),
    val generatedSummary: String,
    val generatedQuizRaw: String = "" // JSON-encoded quiz questions from Gemini
) {
    fun toJsonObject(): JSONObject = JSONObject().apply {
        put("id", id)
        put("fileName", fileName)
        put("fileType", fileType)
        put("timestamp", timestamp)
        put("generatedSummary", generatedSummary)
        put("generatedQuizRaw", generatedQuizRaw)
    }

    companion object {
        fun fromJsonObject(obj: JSONObject): CustomStudyDocument = CustomStudyDocument(
            id = obj.optString("id", UUID.randomUUID().toString()),
            fileName = obj.getString("fileName"),
            fileType = obj.optString("fileType", "Text Notes"),
            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
            generatedSummary = obj.getString("generatedSummary"),
            generatedQuizRaw = obj.optString("generatedQuizRaw", "")
        )
    }
}

// Study & rest reminders settings
data class StudyReminder(
    val enabled: Boolean = true,
    val title: String,
    val timeOfDay: String, // e.g. "08:00"
    val type: String // "Study Alert", "Rest Alert", "Motivation Alert"
) {
    fun toJsonObject(): JSONObject = JSONObject().apply {
        put("enabled", enabled)
        put("title", title)
        put("timeOfDay", timeOfDay)
        put("type", type)
    }

    companion object {
        fun fromJsonObject(obj: JSONObject): StudyReminder = StudyReminder(
            enabled = obj.optBoolean("enabled", true),
            title = obj.getString("title"),
            timeOfDay = obj.optString("timeOfDay", "16:00"),
            type = obj.optString("type", "Study Alert")
        )
    }
}

// SharedPreferences State Manager
object AppStateManager {
    private const val PREFS_NAME = "siphokazi_prefs"
    private const val KEY_TASKS = "tasks"
    private const val KEY_STATS = "stats"
    private const val KEY_DOCS = "docs"
    private const val KEY_REMINDERS = "reminders"

    fun saveTasks(context: Context, tasks: List<StudyTask>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val array = JSONArray()
        tasks.forEach { array.put(it.toJsonObject()) }
        prefs.edit().putString(KEY_TASKS, array.toString()).apply()
    }

    fun loadTasks(context: Context): List<StudyTask> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_TASKS, null) ?: return getMockTasks()
        val list = mutableListOf<StudyTask>()
        try {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                list.add(StudyTask.fromJsonObject(array.getJSONObject(i)))
            }
        } catch (e: Exception) {
            return getMockTasks()
        }
        return list
    }

    fun saveStats(context: Context, stats: UserStats) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_STATS, stats.toJsonObject().toString()).apply()
    }

    fun loadStats(context: Context): UserStats {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_STATS, null) ?: return UserStats()
        return try {
            UserStats.fromJsonObject(JSONObject(raw))
        } catch (e: Exception) {
            UserStats()
        }
    }

    fun saveDocs(context: Context, docs: List<CustomStudyDocument>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val array = JSONArray()
        docs.forEach { array.put(it.toJsonObject()) }
        prefs.edit().putString(KEY_DOCS, array.toString()).apply()
    }

    fun loadDocs(context: Context): List<CustomStudyDocument> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_DOCS, null) ?: return emptyList()
        val list = mutableListOf<CustomStudyDocument>()
        try {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                list.add(CustomStudyDocument.fromJsonObject(array.getJSONObject(i)))
            }
        } catch (e: Exception) {
            // Empty list fallback
        }
        return list
    }

    fun saveReminders(context: Context, reminders: List<StudyReminder>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val array = JSONArray()
        reminders.forEach { array.put(it.toJsonObject()) }
        prefs.edit().putString(KEY_REMINDERS, array.toString()).apply()
    }

    fun loadReminders(context: Context): List<StudyReminder> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_REMINDERS, null) ?: return getMockReminders()
        val list = mutableListOf<StudyReminder>()
        try {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                list.add(StudyReminder.fromJsonObject(array.getJSONObject(i)))
            }
        } catch (e: Exception) {
            return getMockReminders()
        }
        return list
    }

    private fun getMockTasks() = listOf(
        StudyTask(title = "Review Physics Newton's Laws Summary", subject = "Physical Sciences", dueDate = "Today", timeSlot = "14:00 - 14:45"),
        StudyTask(title = "Practice Math Grade 11 Algebra Quiz", subject = "Mathematics", dueDate = "Today", timeSlot = "15:00 - 15:45"),
        StudyTask(title = "Agricultural Science - Animal Nutrition", subject = "Agricultural Sciences", dueDate = "Today", timeSlot = "16:00 - 16:30"),
        StudyTask(title = "Take a short walk and rest", subject = "Rest", dueDate = "Today", timeSlot = "16:30 - 17:00"),
        StudyTask(title = "Life Sciences Meiosis Interactive Flashcards", subject = "Life Sciences", dueDate = "Today", timeSlot = "17:30 - 18:15")
    )

    private fun getMockReminders() = listOf(
        StudyReminder(title = "Start morning revision", timeOfDay = "08:30", type = "Study Alert"),
        StudyReminder(title = "Quick energy break", timeOfDay = "10:30", type = "Rest Alert"),
        StudyReminder(title = "Afternoon subject study block", timeOfDay = "14:00", type = "Study Alert"),
        StudyReminder(title = "Review what was completed", timeOfDay = "19:00", type = "Study Alert")
    )
}
