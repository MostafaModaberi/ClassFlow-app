package com.example.data

import kotlinx.coroutines.flow.Flow

class AppRepository(private val appDao: AppDao) {

    // Classes
    val allClasses: Flow<List<ClassItem>> = appDao.getAllClassesFlow()

    suspend fun insertClass(classItem: ClassItem): Long = appDao.insertClass(classItem)
    suspend fun updateClass(classItem: ClassItem) = appDao.updateClass(classItem)
    suspend fun deleteClass(classItem: ClassItem) = appDao.deleteClass(classItem)
    suspend fun getClassById(id: Long): ClassItem? = appDao.getClassById(id)

    // Schedules
    val allSchedules: Flow<List<ClassSchedule>> = appDao.getAllSchedulesFlow()
    suspend fun getSchedulesForClass(classId: Long): List<ClassSchedule> = appDao.getSchedulesForClass(classId)
    suspend fun insertSchedule(schedule: ClassSchedule): Long = appDao.insertSchedule(schedule)
    suspend fun deleteSchedulesForClass(classId: Long) = appDao.deleteSchedulesForClass(classId)

    // Students
    val allStudents: Flow<List<Student>> = appDao.getAllStudentsFlow()
    suspend fun insertStudent(student: Student): Long = appDao.insertStudent(student)
    suspend fun updateStudent(student: Student) = appDao.updateStudent(student)
    suspend fun deleteStudent(student: Student) = appDao.deleteStudent(student)
    suspend fun getStudentById(id: Long): Student? = appDao.getStudentById(id)

    // Cross Ref / Relations
    suspend fun addStudentToClass(classId: Long, studentId: Long) {
        appDao.insertClassStudentCrossRef(ClassStudentCrossRef(classId, studentId))
    }
    suspend fun removeStudentFromClass(classId: Long, studentId: Long) = appDao.removeStudentFromClass(classId, studentId)
    suspend fun removeAllStudentsFromClass(classId: Long) = appDao.removeAllStudentsFromClass(classId)
    fun getStudentsForClass(classId: Long): Flow<List<Student>> = appDao.getStudentsForClassFlow(classId)
    suspend fun getStudentsForClassDirect(classId: Long): List<Student> = appDao.getStudentsForClassDirect(classId)
    fun getClassesForStudent(studentId: Long): Flow<List<ClassItem>> = appDao.getClassesForStudentFlow(studentId)

    // Sessions
    val allSessions: Flow<List<Session>> = appDao.getAllSessionsFlow()
    fun getSessionsForClass(classId: Long): Flow<List<Session>> = appDao.getSessionsForClassFlow(classId)
    suspend fun getSessionsForClassDirect(classId: Long): List<Session> = appDao.getSessionsForClassDirect(classId)
    suspend fun insertSession(session: Session): Long = appDao.insertSession(session)
    suspend fun updateSession(session: Session) = appDao.updateSession(session)
    suspend fun deleteSession(session: Session) = appDao.deleteSession(session)

    // Attendance
    val allAttendance: Flow<List<Attendance>> = appDao.getAllAttendanceFlow()
    suspend fun getAttendanceForSession(sessionId: Long): List<Attendance> = appDao.getAttendanceForSession(sessionId)
    suspend fun insertAttendance(attendance: Attendance) = appDao.insertAttendance(attendance)
    suspend fun deleteAttendanceForSession(sessionId: Long) = appDao.deleteAttendanceForSession(sessionId)

    // Payments
    val allPayments: Flow<List<Payment>> = appDao.getAllPaymentsFlow()
    fun getPaymentsForClass(classId: Long): Flow<List<Payment>> = appDao.getPaymentsForClassFlow(classId)
    suspend fun getPaymentsForClassDirect(classId: Long): List<Payment> = appDao.getPaymentsForClassDirect(classId)
    fun getPaymentsForStudent(studentId: Long): Flow<List<Payment>> = appDao.getPaymentsForStudentFlow(studentId)
    suspend fun insertPayment(payment: Payment): Long = appDao.insertPayment(payment)
    suspend fun deletePaymentById(id: Long) = appDao.deletePaymentById(id)

    // ToDoItems
    val allToDoItems: Flow<List<ToDoItem>> = appDao.getAllToDoItemsFlow()
    suspend fun insertToDoItem(item: ToDoItem): Long = appDao.insertToDoItem(item)
    suspend fun updateToDoItem(item: ToDoItem) = appDao.updateToDoItem(item)
    suspend fun deleteToDoItem(item: ToDoItem) = appDao.deleteToDoItem(item)

    // Settings
    suspend fun getSetting(key: String): String? = appDao.getSetting(key)
    suspend fun saveSetting(key: String, value: String) {
        appDao.insertSetting(SettingsItem(key, value))
    }
}
