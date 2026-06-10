package com.example.data

import androidx.room.*

@Entity(tableName = "classes")
data class ClassItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val subject: String,
    val grade: String,
    val instructor: String,
    val location: String,
    val capacity: Int,
    val colorHex: String,
    val totalSessions: Int,
    val startDate: String,
    val endDate: String,
    val isArchived: Boolean = false
)

@Entity(tableName = "class_schedules")
data class ClassSchedule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val classId: Long,
    val dayOfWeek: Int, // 1 = Monday, 2 = Tuesday, etc. (Match Farsi calendar or normal, standard is 1=Saturday, 2=Sunday... let's use 1=Saturday to 7=Friday for Persian)
    val startTime: String, // "HH:mm"
    val endTime: String    // "HH:mm"
)

@Entity(tableName = "students")
data class Student(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String,
    val parentPhone: String,
    val notes: String = ""
)

@Entity(tableName = "class_student_cross_ref", primaryKeys = ["classId", "studentId"])
data class ClassStudentCrossRef(
    val classId: Long,
    val studentId: Long
)

@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val classId: Long,
    val sessionNumber: Int,
    val status: String, // "HELD", "CANCELLED", "RESCHEDULED"
    val dateTimeStr: String, // "1405-03-21 16:30" or "YYYY-MM-DD HH:MM"
    val topicTaught: String = "",
    val homework: String = "",
    val resources: String = ""
)

@Entity(tableName = "attendance")
data class Attendance(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val studentId: Long,
    val status: String // "PRESENT", "ABSENT", "DELAYED", "LEAVE"
)

@Entity(tableName = "payments")
data class Payment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val classId: Long,
    val studentId: Long,
    val amountPaid: Double,
    val amountDue: Double,
    val date: String,
    val paymentMethod: String, // "CASH", "CARD", "TRANSFER"
    val notes: String = ""
)

@Entity(tableName = "todo_items")
data class ToDoItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val dueDateStr: String,
    val isCompleted: Boolean = false,
    val type: String, // "PERSONAL", "PARENT_CONTACT", "CLASS_PREP"
    val studentId: Long? = null,
    val classId: Long? = null
)

@Entity(tableName = "settings")
data class SettingsItem(
    @PrimaryKey val key: String,
    val value: String
)
