package com.kova.app.domain.sync

import com.kova.app.domain.model.AccentPalette
import com.kova.app.domain.model.Exercise
import com.kova.app.domain.model.RoutineDay
import com.kova.app.domain.model.ThemeMode
import com.kova.app.domain.model.UserPreferences
import com.kova.app.domain.model.WeightUnit
import com.kova.app.domain.model.WorkoutSession
import com.kova.app.domain.model.WorkoutSet
import org.json.JSONArray
import org.json.JSONObject

object KovaBackupCodec {

    fun encode(snapshot: KovaBackupSnapshot): String {
        val root = JSONObject().apply {
            put("schemaVersion", snapshot.schemaVersion)
            put("exportedAtEpochMillis", snapshot.exportedAtEpochMillis)
            put("appVersion", snapshot.appVersion)
            put("preferences", encodePreferences(snapshot.preferences))
            put("routineDays", JSONArray().apply {
                snapshot.routineDays.forEach { put(encodeRoutineDay(it)) }
            })
            put("exercises", JSONArray().apply {
                snapshot.exercises.forEach { put(encodeExercise(it)) }
            })
            put("workoutSessions", JSONArray().apply {
                snapshot.workoutSessions.forEach { put(encodeSession(it)) }
            })
            put("workoutSets", JSONArray().apply {
                snapshot.workoutSets.forEach { put(encodeSet(it)) }
            })
        }
        return root.toString(2)
    }

    fun decode(jsonString: String): Result<KovaBackupSnapshot> = runCatching {
        val root = JSONObject(jsonString)
        val version = root.optInt("schemaVersion", -1)
        require(version in 1..KOVA_BACKUP_SCHEMA_VERSION) {
            "Versión de copia de seguridad no compatible ($version)."
        }

        val prefs = decodePreferences(root.getJSONObject("preferences"))
        val routines = root.getJSONArray("routineDays").toList(::decodeRoutineDay)
        val exercises = root.getJSONArray("exercises").toList(::decodeExercise)
        val sessions = root.getJSONArray("workoutSessions").toList(::decodeSession)
        val sets = root.getJSONArray("workoutSets").toList(::decodeSet)

        KovaBackupSnapshot(
            schemaVersion = version,
            exportedAtEpochMillis = root.optLong("exportedAtEpochMillis", System.currentTimeMillis()),
            appVersion = root.optString("appVersion", "1.0.0"),
            preferences = prefs,
            routineDays = routines,
            exercises = exercises,
            workoutSessions = sessions,
            workoutSets = sets
        )
    }

    private fun encodePreferences(p: UserPreferences) = JSONObject().apply {
        put("hasCompletedOnboarding", p.hasCompletedOnboarding)
        put("trackingStartEpochDay", p.trackingStartEpochDay)
        put("weeklyGoalDays", p.weeklyGoalDays)
        put("weightUnit", p.weightUnit.name)
        put("themeMode", p.themeMode.name)
        put("accentPalette", p.accentPalette.name)
        put("defaultRestSeconds", p.defaultRestSeconds)
        put("autoStartRestTimer", p.autoStartRestTimer)
        put("soundAndHapticsEnabled", p.soundAndHapticsEnabled)
    }

    private fun decodePreferences(o: JSONObject) = UserPreferences(
        hasCompletedOnboarding = o.optBoolean("hasCompletedOnboarding", true),
        trackingStartEpochDay = o.optLong("trackingStartEpochDay", java.time.LocalDate.now().toEpochDay()),
        weeklyGoalDays = o.optInt("weeklyGoalDays", 4).coerceIn(1, 7),
        weightUnit = runCatching { WeightUnit.valueOf(o.optString("weightUnit", "KG")) }.getOrDefault(WeightUnit.KG),
        themeMode = runCatching { ThemeMode.valueOf(o.optString("themeMode", "SYSTEM")) }.getOrDefault(ThemeMode.SYSTEM),
        accentPalette = runCatching { AccentPalette.valueOf(o.optString("accentPalette", "VOLT")) }.getOrDefault(AccentPalette.VOLT),
        defaultRestSeconds = o.optInt("defaultRestSeconds", 90).coerceIn(15, 600),
        autoStartRestTimer = o.optBoolean("autoStartRestTimer", true),
        soundAndHapticsEnabled = o.optBoolean("soundAndHapticsEnabled", true)
    )

    private fun encodeRoutineDay(r: RoutineDay) = JSONObject().apply {
        put("id", r.id)
        put("name", r.name)
        put("colorHex", r.colorHex)
        put("targetExerciseCount", r.targetExerciseCount)
        put("sortOrder", r.sortOrder)
        put("isArchived", r.isArchived)
        put("createdAtEpochMillis", r.createdAtEpochMillis)
    }

    private fun decodeRoutineDay(o: JSONObject) = RoutineDay(
        id = o.getLong("id"),
        name = o.getString("name"),
        colorHex = o.optString("colorHex", "#C6F635"),
        targetExerciseCount = o.optInt("targetExerciseCount", 5),
        sortOrder = o.optInt("sortOrder", 0),
        isArchived = o.optBoolean("isArchived", false),
        createdAtEpochMillis = o.optLong("createdAtEpochMillis", System.currentTimeMillis())
    )

    private fun encodeExercise(e: Exercise) = JSONObject().apply {
        put("id", e.id)
        put("routineDayId", e.routineDayId)
        put("name", e.name)
        put("isMandatory", e.isMandatory)
        put("isBodyweight", e.isBodyweight)
        put("notes", e.notes)
        if (e.defaultRestSeconds != null) put("defaultRestSeconds", e.defaultRestSeconds)
        if (e.manualOneRmKg != null) put("manualOneRmKg", e.manualOneRmKg)
        if (e.manualOneRmDateEpochDay != null) put("manualOneRmDateEpochDay", e.manualOneRmDateEpochDay)
        put("sortOrder", e.sortOrder)
        put("isArchived", e.isArchived)
        put("createdAtEpochMillis", e.createdAtEpochMillis)
    }

    private fun decodeExercise(o: JSONObject) = Exercise(
        id = o.getLong("id"),
        routineDayId = o.getLong("routineDayId"),
        name = o.getString("name"),
        isMandatory = o.optBoolean("isMandatory", true),
        isBodyweight = o.optBoolean("isBodyweight", false),
        notes = o.optString("notes", ""),
        defaultRestSeconds = if (o.has("defaultRestSeconds") && !o.isNull("defaultRestSeconds")) o.getInt("defaultRestSeconds") else null,
        manualOneRmKg = if (o.has("manualOneRmKg") && !o.isNull("manualOneRmKg")) {
            o.optDouble("manualOneRmKg").takeIf { it.isFinite() && it > 0.0 && it <= 5000.0 }
        } else null,
        manualOneRmDateEpochDay = if (o.has("manualOneRmDateEpochDay") && !o.isNull("manualOneRmDateEpochDay")) {
            o.optLong("manualOneRmDateEpochDay")
        } else null,
        sortOrder = o.optInt("sortOrder", 0),
        isArchived = o.optBoolean("isArchived", false),
        createdAtEpochMillis = o.optLong("createdAtEpochMillis", System.currentTimeMillis())
    )

    private fun encodeSession(s: WorkoutSession) = JSONObject().apply {
        put("id", s.id)
        if (s.routineDayId != null) put("routineDayId", s.routineDayId)
        put("routineDayNameSnapshot", s.routineDayNameSnapshot)
        put("routineColorHexSnapshot", s.routineColorHexSnapshot)
        put("dateEpochDay", s.dateEpochDay)
        put("startedAtEpochMillis", s.startedAtEpochMillis)
        if (s.finishedAtEpochMillis != null) put("finishedAtEpochMillis", s.finishedAtEpochMillis)
        put("targetExerciseCountSnapshot", s.targetExerciseCountSnapshot)
        put("mandatoryCountSnapshot", s.mandatoryCountSnapshot)
        put("completedExerciseCount", s.completedExerciseCount)
        put("allMandatoryCompleted", s.allMandatoryCompleted)
        put("isDayGoalMet", s.isDayGoalMet)
        put("sessionNote", s.sessionNote)
    }

    private fun decodeSession(o: JSONObject) = WorkoutSession(
        id = o.getLong("id"),
        routineDayId = if (o.has("routineDayId") && !o.isNull("routineDayId")) o.getLong("routineDayId") else null,
        routineDayNameSnapshot = o.getString("routineDayNameSnapshot"),
        routineColorHexSnapshot = o.optString("routineColorHexSnapshot", "#C6F635"),
        dateEpochDay = o.getLong("dateEpochDay"),
        startedAtEpochMillis = o.getLong("startedAtEpochMillis"),
        finishedAtEpochMillis = if (o.has("finishedAtEpochMillis") && !o.isNull("finishedAtEpochMillis")) o.getLong("finishedAtEpochMillis") else null,
        targetExerciseCountSnapshot = o.optInt("targetExerciseCountSnapshot", 5),
        mandatoryCountSnapshot = o.optInt("mandatoryCountSnapshot", 0),
        completedExerciseCount = o.optInt("completedExerciseCount", 0),
        allMandatoryCompleted = o.optBoolean("allMandatoryCompleted", false),
        isDayGoalMet = o.optBoolean("isDayGoalMet", false),
        sessionNote = o.optString("sessionNote", "")
    )

    private fun encodeSet(w: WorkoutSet) = JSONObject().apply {
        put("id", w.id)
        put("sessionId", w.sessionId)
        if (w.exerciseId != null) put("exerciseId", w.exerciseId)
        put("exerciseNameSnapshot", w.exerciseNameSnapshot)
        put("isMandatorySnapshot", w.isMandatorySnapshot)
        put("isBodyweightSnapshot", w.isBodyweightSnapshot)
        put("setIndex", w.setIndex)
        put("weightKg", w.weightKg)
        put("reps", w.reps)
        put("isCompleted", w.isCompleted)
        if (w.completedAtEpochMillis != null) put("completedAtEpochMillis", w.completedAtEpochMillis)
    }

    private fun decodeSet(o: JSONObject) = WorkoutSet(
        id = o.getLong("id"),
        sessionId = o.getLong("sessionId"),
        exerciseId = if (o.has("exerciseId") && !o.isNull("exerciseId")) o.getLong("exerciseId") else null,
        exerciseNameSnapshot = o.getString("exerciseNameSnapshot"),
        isMandatorySnapshot = o.optBoolean("isMandatorySnapshot", true),
        isBodyweightSnapshot = o.optBoolean("isBodyweightSnapshot", false),
        setIndex = o.optInt("setIndex", 1).coerceAtLeast(1),
        weightKg = o.optDouble("weightKg", 0.0).takeIf { it.isFinite() && it >= 0.0 && it <= 5000.0 } ?: 0.0,
        reps = o.optInt("reps", 0).coerceIn(0, 1000),
        isCompleted = o.optBoolean("isCompleted", true),
        completedAtEpochMillis = if (o.has("completedAtEpochMillis") && !o.isNull("completedAtEpochMillis")) o.getLong("completedAtEpochMillis") else null
    )

    private inline fun <T> JSONArray.toList(mapper: (JSONObject) -> T): List<T> {
        val result = ArrayList<T>(length())
        for (i in 0 until length()) {
            result += mapper(getJSONObject(i))
        }
        return result
    }
}
