package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // Classes
    @Query("SELECT * FROM classes ORDER BY isArchived ASC, id DESC")
    fun getAllClassesFlow(): Flow<List<ClassItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClass(classItem: ClassItem): Long

    @Update
    suspend fun updateClass(classItem: ClassItem)

    @Delete
    suspend fun deleteClass(classItem: ClassItem)

    @Query("SELECT * FROM classes WHERE id = :id")
    suspend fun getClassById(id: Long): ClassItem?

    // Class schedules
    @Query("SELECT * FROM class_schedules")
    fun getAllSchedulesFlow(): Flow<List<ClassSchedule>>

    @Query("SELECT * FROM class_schedules WHERE classId = :classId")
    suspend fun getSchedulesForClass(classId: Long): List<ClassSchedule>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: ClassSchedule): Long

    @Query("DELETE FROM class_schedules WHERE classId = :classId")
    suspend fun deleteSchedulesForClass(classId: Long)

    // Students
    @Query("SELECT * FROM students ORDER BY name ASC")
    fun getAllStudentsFlow(): Flow<List<Student>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: Student): Long

    @Update
    suspend fun updateStudent(student: Student)

    @Delete
    suspend fun deleteStudent(student: Student)

    @Query("SELECT * FROM students WHERE id = :id")
    suspend fun getStudentById(id: Long): Student?

    // Class Students Cross Reference
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClassStudentCrossRef(crossRef: ClassStudentCrossRef)

    @Query("DELETE FROM class_student_cross_ref WHERE classId = :classId AND studentId = :studentId")
    suspend fun removeStudentFromClass(classId: Long, studentId: Long)

    @Query("DELETE FROM class_student_cross_ref WHERE classId = :classId")
    suspend fun removeAllStudentsFromClass(classId: Long)

    @Query("SELECT * FROM students INNER JOIN class_student_cross_ref ON students.id = class_student_cross_ref.studentId WHERE class_student_cross_ref.classId = :classId")
    fun getStudentsForClassFlow(classId: Long): Flow<List<Student>>

    @Query("SELECT * FROM students INNER JOIN class_student_cross_ref ON students.id = class_student_cross_ref.studentId WHERE class_student_cross_ref.classId = :classId")
    suspend fun getStudentsForClassDirect(classId: Long): List<Student>

    @Query("SELECT * FROM classes INNER JOIN class_student_cross_ref ON classes.id = class_student_cross_ref.classId WHERE class_student_cross_ref.studentId = :studentId")
    fun getClassesForStudentFlow(studentId: Long): Flow<List<ClassItem>>

    // Sessions
    @Query("SELECT * FROM sessions ORDER BY dateTimeStr ASC")
    fun getAllSessionsFlow(): Flow<List<Session>>

    @Query("SELECT * FROM sessions WHERE classId = :classId ORDER BY sessionNumber ASC")
    fun getSessionsForClassFlow(classId: Long): Flow<List<Session>>

    @Query("SELECT * FROM sessions WHERE classId = :classId ORDER BY sessionNumber ASC")
    suspend fun getSessionsForClassDirect(classId: Long): List<Session>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: Session): Long

    @Update
    suspend fun updateSession(session: Session)

    @Delete
    suspend fun deleteSession(session: Session)

    // Attendance
    @Query("SELECT * FROM attendance")
    fun getAllAttendanceFlow(): Flow<List<Attendance>>

    @Query("SELECT * FROM attendance WHERE sessionId = :sessionId")
    suspend fun getAttendanceForSession(sessionId: Long): List<Attendance>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: Attendance)

    @Query("DELETE FROM attendance WHERE sessionId = :sessionId")
    suspend fun deleteAttendanceForSession(sessionId: Long)

    // Payments
    @Query("SELECT * FROM payments ORDER BY date DESC")
    fun getAllPaymentsFlow(): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE classId = :classId ORDER BY date DESC")
    fun getPaymentsForClassFlow(classId: Long): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE classId = :classId ORDER BY date DESC")
    suspend fun getPaymentsForClassDirect(classId: Long): List<Payment>

    @Query("SELECT * FROM payments WHERE studentId = :studentId ORDER BY date DESC")
    fun getPaymentsForStudentFlow(studentId: Long): Flow<List<Payment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: Payment): Long

    @Query("DELETE FROM payments WHERE id = :id")
    suspend fun deletePaymentById(id: Long)

    // ToDoItems
    @Query("SELECT * FROM todo_items ORDER BY isCompleted ASC, id DESC")
    fun getAllToDoItemsFlow(): Flow<List<ToDoItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertToDoItem(item: ToDoItem): Long

    @Update
    suspend fun updateToDoItem(item: ToDoItem)

    @Delete
    suspend fun deleteToDoItem(item: ToDoItem)

    // Settings
    @Query("SELECT value FROM settings WHERE `key` = :key")
    suspend fun getSetting(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: SettingsItem)
}
