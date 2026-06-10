package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AppRepository

    // Settings Profile
    val teacherName = MutableStateFlow("استاد گرامی")
    val schoolName = MutableStateFlow("آموزشگاه آزاد")
    val themeColor = MutableStateFlow("#4F46E5") // Default Hex (Indigo)
    val isOnboarded = MutableStateFlow(false)
    val isDarkMode = MutableStateFlow(false)
    val selectedFont = MutableStateFlow("estedad") // Options: "estedad", "shabnam"

    // Raw Flows from Database
    val classes = MutableStateFlow<List<ClassItem>>(emptyList())
    val allSchedules = MutableStateFlow<List<ClassSchedule>>(emptyList())
    val students = MutableStateFlow<List<Student>>(emptyList())
    val allSessions = MutableStateFlow<List<Session>>(emptyList())
    val allAttendance = MutableStateFlow<List<Attendance>>(emptyList())
    val allPayments = MutableStateFlow<List<Payment>>(emptyList())
    val allToDos = MutableStateFlow<List<ToDoItem>>(emptyList())

    // Search Query
    val searchQuery = MutableStateFlow("")

    init {
        val database = AppDatabase.getDatabase(application)
        repository = AppRepository(database.appDao())

        // Load Settings
        viewModelScope.launch {
            repository.getSetting("teacher_name")?.let { teacherName.value = it }
            repository.getSetting("school_name")?.let { schoolName.value = it }
            repository.getSetting("theme_color")?.let { themeColor.value = it }
            repository.getSetting("is_onboarded")?.let { isOnboarded.value = it == "true" }
            repository.getSetting("is_dark_mode")?.let { isDarkMode.value = it == "true" }
            repository.getSetting("selected_font")?.let { selectedFont.value = it }

            // Observe Database changes
            observeData()

            // Seed Mock Data if empty (Very important to prevent dead-end or blank UI on startup)
            seedMockDataIfEmpty()
        }
    }

    private fun observeData() {
        viewModelScope.launch {
            repository.allClasses.collect { classes.value = it }
        }
        viewModelScope.launch {
            repository.allSchedules.collect { allSchedules.value = it }
        }
        viewModelScope.launch {
            repository.allStudents.collect { students.value = it }
        }
        viewModelScope.launch {
            repository.allSessions.collect { allSessions.value = it }
        }
        viewModelScope.launch {
            repository.allAttendance.collect { allAttendance.value = it }
        }
        viewModelScope.launch {
            repository.allPayments.collect { allPayments.value = it }
        }
        viewModelScope.launch {
            repository.allToDoItems.collect { allToDos.value = it }
        }
    }

    fun toggleDarkMode() {
        viewModelScope.launch {
            val newVal = !isDarkMode.value
            isDarkMode.value = newVal
            repository.saveSetting("is_dark_mode", newVal.toString())
        }
    }

    fun updateFont(font: String) {
        viewModelScope.launch {
            selectedFont.value = font
            repository.saveSetting("selected_font", font)
        }
    }

    // Complete Onboarding
    fun completeOnboarding(name: String, school: String, color: String) {
        viewModelScope.launch {
            teacherName.value = name
            schoolName.value = school
            themeColor.value = color
            isOnboarded.value = true

            repository.saveSetting("teacher_name", name)
            repository.saveSetting("school_name", school)
            repository.saveSetting("theme_color", color)
            repository.saveSetting("is_onboarded", "true")
        }
    }

    // Update simple settings profile
    fun updateProfile(name: String, school: String, color: String) {
        viewModelScope.launch {
            teacherName.value = name
            schoolName.value = school
            themeColor.value = color
            repository.saveSetting("teacher_name", name)
            repository.saveSetting("school_name", school)
            repository.saveSetting("theme_color", color)
        }
    }

    // Reset whole application and recreate database
    fun resetApp() {
        viewModelScope.launch {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val db = AppDatabase.getDatabase(getApplication())
                db.clearAllTables()
            }
            teacherName.value = "استاد گرامی"
            schoolName.value = "آموزشگاه آزاد"
            themeColor.value = "#4F46E5"
            isOnboarded.value = false
            isDarkMode.value = false
            selectedFont.value = "estedad"
            
            classes.value = emptyList()
            allSchedules.value = emptyList()
            students.value = emptyList()
            allSessions.value = emptyList()
            allAttendance.value = emptyList()
            allPayments.value = emptyList()
            allToDos.value = emptyList()
        }
    }

    // --- Class CRUD ---
    fun createClass(
        name: String,
        subject: String,
        grade: String,
        location: String,
        capacity: Int,
        colorHex: String,
        totalSessions: Int,
        startDate: String,
        endDate: String,
        schedules: List<Pair<Int, String>> // DayIndex (1-7), Time (e.g. "16:00-17:30")
    ) {
        viewModelScope.launch {
            val classId = repository.insertClass(
                ClassItem(
                    name = name,
                    subject = subject,
                    grade = grade,
                    instructor = teacherName.value,
                    location = location,
                    capacity = capacity,
                    colorHex = colorHex,
                    totalSessions = totalSessions,
                    startDate = startDate,
                    endDate = endDate,
                    isArchived = false
                )
            )

            // Insert associated schedules
            schedules.forEach { (day, timeRange) ->
                val times = timeRange.split("-")
                val start = times.getOrNull(0) ?: "16:00"
                val end = times.getOrNull(1) ?: "17:30"
                repository.insertSchedule(
                    ClassSchedule(
                        classId = classId,
                        dayOfWeek = day,
                        startTime = start,
                        endTime = end
                    )
                )
            }
        }
    }

    fun updateClass(classItem: ClassItem) {
        viewModelScope.launch {
            repository.updateClass(classItem)
        }
    }

    fun toggleClassArchive(classId: Long) {
        viewModelScope.launch {
            classes.value.find { it.id == classId }?.let { item ->
                repository.updateClass(item.copy(isArchived = !item.isArchived))
            }
        }
    }

    fun deleteClass(classItem: ClassItem) {
        viewModelScope.launch {
            repository.deleteClass(classItem)
            repository.deleteSchedulesForClass(classItem.id)
            repository.removeAllStudentsFromClass(classItem.id)
        }
    }

    // --- Student CRUD ---
    fun createStudent(name: String, phone: String, parentPhone: String, notes: String, classItemIds: List<Long>) {
        viewModelScope.launch {
            val studentId = repository.insertStudent(
                Student(
                    name = name,
                    phone = phone,
                    parentPhone = parentPhone,
                    notes = notes
                )
            )

            // Connect student to selected classes
            classItemIds.forEach { classId ->
                repository.addStudentToClass(classId, studentId)
            }

            // Create reminder task if parent contact number exists
            if (parentPhone.isNotBlank()) {
                repository.insertToDoItem(
                    ToDoItem(
                        title = "تماس با ولی دانش‌آموز $name",
                        dueDateStr = "فردا",
                        type = "PARENT_CONTACT",
                        studentId = studentId
                    )
                )
            }
        }
    }

    fun updateStudent(student: Student) {
        viewModelScope.launch {
            repository.updateStudent(student)
        }
    }

    fun deleteStudent(student: Student) {
        viewModelScope.launch {
            repository.deleteStudent(student)
        }
    }

    fun addStudentToClass(classId: Long, studentId: Long) {
        viewModelScope.launch {
            repository.addStudentToClass(classId, studentId)
        }
    }

    fun removeStudentFromClass(classId: Long, studentId: Long) {
        viewModelScope.launch {
            repository.removeStudentFromClass(classId, studentId)
        }
    }

    // --- Session CRUD & Smart Continue ---
    fun createSession(
        classId: Long,
        dateTimeStr: String,
        topicTaught: String,
        homework: String,
        resources: String,
        attendanceMap: Map<Long, String> // StudentId -> Status
    ) {
        viewModelScope.launch {
            // Get current session count of this class to determine sessionNumber
            val sessionsForClass = repository.getSessionsForClassDirect(classId)
            val nextSessionNum = sessionsForClass.size + 1

            val sessionId = repository.insertSession(
                Session(
                    classId = classId,
                    sessionNumber = nextSessionNum,
                    status = "HELD",
                    dateTimeStr = dateTimeStr,
                    topicTaught = topicTaught,
                    homework = homework,
                    resources = resources
                )
            )

            // Save Attendance
            attendanceMap.forEach { (studentId, status) ->
                repository.insertAttendance(
                    Attendance(
                        sessionId = sessionId,
                        studentId = studentId,
                        status = status
                    )
                )
            }
        }
    }

    // Smart Continuation Search (Gets the latest session of a class to suggest continuing topic)
    fun getSmartContinuationTopic(classId: Long): String {
        val classSessions = allSessions.value.filter { it.classId == classId && it.status == "HELD" }
            .sortedBy { it.sessionNumber }
        val lastSession = classSessions.lastOrNull()
        return if (lastSession != null && lastSession.topicTaught.isNotBlank()) {
            "ادامه مبحث: ${lastSession.topicTaught}"
        } else {
            ""
        }
    }

    // Save session cancellation/change status
    fun updateSessionStatus(session: Session, status: String) {
        viewModelScope.launch {
            repository.updateSession(session.copy(status = status))
        }
    }

    // --- Payments CRUD ---
    fun createPayment(classId: Long, studentId: Long, paid: Double, due: Double, date: String, method: String, notes: String) {
        viewModelScope.launch {
            repository.insertPayment(
                Payment(
                    classId = classId,
                    studentId = studentId,
                    amountPaid = paid,
                    amountDue = due,
                    date = date,
                    paymentMethod = method,
                    notes = notes
                )
            )
        }
    }

    fun deletePayment(id: Long) {
        viewModelScope.launch {
            repository.deletePaymentById(id)
        }
    }

    // --- ToDos CRM ---
    fun createToDoItem(title: String, dueDateStr: String, type: String, studentId: Long? = null, classId: Long? = null) {
        viewModelScope.launch {
            repository.insertToDoItem(
                ToDoItem(
                    title = title,
                    dueDateStr = dueDateStr,
                    isCompleted = false,
                    type = type,
                    studentId = studentId,
                    classId = classId
                )
            )
        }
    }

    fun toggleToDo(item: ToDoItem) {
        viewModelScope.launch {
            repository.updateToDoItem(item.copy(isCompleted = !item.isCompleted))
        }
    }

    fun deleteToDo(item: ToDoItem) {
        viewModelScope.launch {
            repository.deleteToDoItem(item)
        }
    }

    // --- Smart Suggest Free Times (پیشنهاد زمان خالی) ---
    // Scans existing schedules and suggests available 1.5 hour blocks
    fun suggestFreeTimes(): List<Triple<Int, String, String>> {
        // Simple day of week mapping: 1=Saturday, 2=Sunday, 3=Monday, 4=Tuesday, 5=Wednesday, 6=Thursday, 7=Friday
        val days = listOf(1, 2, 3, 4, 5, 6, 7)
        // Let's propose standard slots:
        val standardSlots = listOf(
            Pair("10:00", "11:30"),
            Pair("14:00", "15:30"),
            Pair("16:00", "17:30"),
            Pair("18:00", "19:30")
        )

        val result = mutableListOf<Triple<Int, String, String>>()
        val booked = allSchedules.value

        for (day in days) {
            for (slot in standardSlots) {
                // Check if this slot overlaps with ANY booked class
                val hasOverlap = booked.any { b ->
                    b.dayOfWeek == day && (
                        (slot.first >= b.startTime && slot.first < b.endTime) ||
                        (slot.second > b.startTime && slot.second <= b.endTime) ||
                        (b.startTime >= slot.first && b.startTime < slot.second)
                    )
                }
                if (!hasOverlap && result.size < 6) {
                    result.add(Triple(day, slot.first, slot.second))
                }
            }
        }

        return result
    }

    // Helper translation map for Day of Week
    fun getDayName(dayIndex: Int): String {
        return when (dayIndex) {
            1 -> "شنبه"
            2 -> "یکشنبه"
            3 -> "دوشنبه"
            4 -> "سه‌شنبه"
            5 -> "چهارشنبه"
            6 -> "پنج‌شنبه"
            7 -> "جمعه"
            else -> "مبهم"
        }
    }

    // --- Mock Seeding ---
    private suspend fun seedMockDataIfEmpty() {
        // Only seed if empty database
        val existingClasses = repository.allClasses.first()
        if (existingClasses.isNotEmpty()) return

        // 1. Create a class
        val c1Id = repository.insertClass(
            ClassItem(
                name = "ریاضی کنکور",
                subject = "ریاضیات",
                grade = "دوازدهم تجربی",
                instructor = teacherName.value,
                location = "آموزشگاه قلم‌به‌دست",
                capacity = 15,
                colorHex = "#4F46E5", // Indigo
                totalSessions = 10,
                startDate = "1405/03/01",
                endDate = "1405/05/15"
            )
        )
        // Class 1 Schedule: دوشنبه ۱۶:۰۰ تا ۱۷:۳۰
        repository.insertSchedule(ClassSchedule(classId = c1Id, dayOfWeek = 3, startTime = "16:00", endTime = "17:30"))

        val c2Id = repository.insertClass(
            ClassItem(
                name = "فیزیک نهایی",
                subject = "فیزیک",
                grade = "یازدهم ریاضی",
                instructor = teacherName.value,
                location = "مدرسه البرز",
                capacity = 8,
                colorHex = "#0D9488", // Teal
                totalSessions = 12,
                startDate = "1405/03/05",
                endDate = "1405/06/10"
            )
        )
        // Class 2 Schedule: سه‌شنبه ۱۸:۰۰ تا ۱۹:۳۰
        repository.insertSchedule(ClassSchedule(classId = c2Id, dayOfWeek = 4, startTime = "18:00", endTime = "19:30"))

        // 2. Feed Students
        val s1Id = repository.insertStudent(Student(name = "نیما احمدی", phone = "09121111111", parentPhone = "09122222222", notes = "دانش‌آموز فعال و مستعد"))
        val s2Id = repository.insertStudent(Student(name = "سارا رضایی", phone = "09193333333", parentPhone = "09194444444", notes = "نیاز به تکرار مجدد مسائل سرعت"))
        val s3Id = repository.insertStudent(Student(name = "علی محمدی", phone = "09355555555", parentPhone = "09356666666", notes = "ضعف در ریاضی پایه"))

        // Class 1 cross ref (Nima, Sara)
        repository.addStudentToClass(c1Id, s1Id)
        repository.addStudentToClass(c1Id, s2Id)

        // Class 2 cross ref (Ali, Sara)
        repository.addStudentToClass(c2Id, s3Id)
        repository.addStudentToClass(c2Id, s2Id)

        // 3. Create initial sessions
        val ses1 = repository.insertSession(
            Session(
                classId = c1Id,
                sessionNumber = 1,
                status = "HELD",
                dateTimeStr = "1405/03/01 16:00",
                topicTaught = "مبادی تابع دهم و یازدهم",
                homework = "حل تمرین‌های ۱ تا ۱۵ جزوه مبحث تابع",
                resources = "کتاب کار فصل ۱"
            )
        )
        repository.insertAttendance(Attendance(sessionId = ses1, studentId = s1Id, status = "PRESENT"))
        repository.insertAttendance(Attendance(sessionId = ses1, studentId = s2Id, status = "PRESENT"))

        val ses2 = repository.insertSession(
            Session(
                classId = c1Id,
                sessionNumber = 2,
                status = "HELD",
                dateTimeStr = "1405/03/08 16:00",
                topicTaught = "توابع صعودی و نزولی",
                homework = "تست‌های کنکور فصل تابع تجربی ۱۰ سال اخیر",
                resources = "جزوه فرمول‌های کلیدی"
            )
        )
        repository.insertAttendance(Attendance(sessionId = ses2, studentId = s1Id, status = "PRESENT"))
        repository.insertAttendance(Attendance(sessionId = ses2, studentId = s2Id, status = "DELAYED")) // Delayed

        val ses3 = repository.insertSession(
            Session(
                classId = c2Id,
                sessionNumber = 1,
                status = "HELD",
                dateTimeStr = "1405/03/05 18:00",
                topicTaught = "قوانین حرکت نیوتن",
                homework = "رسم ۳ مسئله با برخورد سیم‌ها",
                resources = "تخته مجازی و پی‌دی‌اف جلسه ۱"
            )
        )
        repository.insertAttendance(Attendance(sessionId = ses3, studentId = s3Id, status = "PRESENT"))
        repository.insertAttendance(Attendance(sessionId = ses3, studentId = s2Id, status = "LEAVE")) // Leave

        // 4. Create Payments
        repository.insertPayment(Payment(classId = c1Id, studentId = s1Id, amountPaid = 4500000.0, amountDue = 1500000.0, date = "1405/03/01", paymentMethod = "TRANSFER", notes = "علی‌الحساب قسط اول"))
        repository.insertPayment(Payment(classId = c1Id, studentId = s2Id, amountPaid = 3900000.0, amountDue = 2100000.0, date = "1405/03/02", paymentMethod = "CARD", notes = "پرداخت آنلاین درگاه"))
        repository.insertPayment(Payment(classId = c2Id, studentId = s3Id, amountPaid = 2000000.0, amountDue = 2000000.0, date = "1405/03/05", paymentMethod = "CASH", notes = "نقدی کلاس نیمه گروهی"))

        // 5. Create ToDos
        repository.insertToDoItem(ToDoItem(title = "طراحی آزمون میان‌دوره ریاضی", dueDateStr = "فردا", type = "CLASS_PREP", classId = c1Id))
        repository.insertToDoItem(ToDoItem(title = "تماس با ولی علی محمدی برای پیگیری تکالیف", dueDateStr = "شنبه ۲۳ خرداد", type = "PARENT_CONTACT", studentId = s3Id))
        repository.insertToDoItem(ToDoItem(title = "تهیه اسلایدهای کار با بردارها در فیزیک", dueDateStr = "چهارشنبه", type = "CLASS_PREP", classId = c2Id))
    }
}
