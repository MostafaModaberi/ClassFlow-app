package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.*
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel
import java.text.DecimalFormat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val isDark by viewModel.isDarkMode.collectAsStateWithLifecycle()
            val selectedFont by viewModel.selectedFont.collectAsStateWithLifecycle()
            val themeHex by viewModel.themeColor.collectAsStateWithLifecycle()
            val brandColor = remember(themeHex) {
                try {
                    Color(android.graphics.Color.parseColor(themeHex))
                } catch (e: Exception) {
                    Color(0xFF4F46E5) // Fallback Indigo
                }
            }
            MyApplicationTheme(
                darkTheme = isDark,
                brandPrimary = brandColor,
                selectedFont = selectedFont
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    MainScreen()
                }
            }
        }
    }
}

// Decimal format for prices (e.g. 4,500,000)
val priceFormatter = DecimalFormat("#,###")

fun formatPrice(value: Double): String {
    return priceFormatter.format(value)
}

// Custom GlassCard modifier - changed to high-contrast flat elevated card
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    borderColor: Color? = null,
    backgroundColor: Color? = null,
    brandColor: Color = Color(0xFF4F46E5),
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val computedBg = backgroundColor ?: (if (isDark) Color(0xFF151D2F) else Color.White)
    val computedBorder = borderColor ?: (if (isDark) Color(0xFF2E3E5B) else brandColor.copy(alpha = 0.10f))

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = computedBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, computedBorder, RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            content()
        }
    }
}

// Main screen controller
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val viewModel: MainViewModel = viewModel()

    val isOnboarded by viewModel.isOnboarded.collectAsStateWithLifecycle()
    val themeHex by viewModel.themeColor.collectAsStateWithLifecycle()
    val isDark by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val brandColor = remember(themeHex) {
        try {
            Color(android.graphics.Color.parseColor(themeHex))
        } catch (e: Exception) {
            Color(0xFF4F46E5) // Fallback Indigo
        }
    }

    val isDarkThemeActive = isDark

    if (!isOnboarded) {
        OnboardingScreen(
            onComplete = { name, school, color ->
                viewModel.completeOnboarding(name, school, color)
            }
        )
    } else {
        // App Core Shell
        var selectedTab by remember { mutableStateOf("dashboard") }
        var showAddMenu by remember { mutableStateOf(false) }

        // All Sheets/Dialog states
        var showAddClassDialog by remember { mutableStateOf(false) }
        var showAddStudentDialog by remember { mutableStateOf(false) }
        var showAddSessionDialog by remember { mutableStateOf(false) }
        var showAddPaymentDialog by remember { mutableStateOf(false) }
        var showAddToDoDialog by remember { mutableStateOf(false) }

        // Selection states context
        var selectedClassDetailId by remember { mutableStateOf<Long?>(null) }
        var selectedStudentIdForDetail by remember { mutableStateOf<Long?>(null) }

        val scaffoldBg = if (isDarkThemeActive) Color(0xFF090D16) else brandColor.copy(alpha = 0.02f)
        val containerBg = if (isDarkThemeActive) Color(0xFF151D2F) else Color(0xFFFFFFFF)
        val containerBorder = if (isDarkThemeActive) Color(0xFF2E3E5B) else brandColor.copy(alpha = 0.10f)

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(scaffoldBg),
            containerColor = scaffoldBg,
            bottomBar = {
                // High-contrast Solid Bottom Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .border(1.dp, containerBorder, RoundedCornerShape(20.dp)),
                        colors = CardDefaults.cardColors(containerColor = containerBg),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Tab 1: Dashboard
                            BottomNavItem(
                                icon = Icons.Default.Home,
                                label = "خانه",
                                selected = selectedTab == "dashboard",
                                activeColor = brandColor,
                                onClick = { selectedTab = "dashboard" }
                            )

                            // Tab 2: Classes
                            BottomNavItem(
                                icon = Icons.Default.Book,
                                label = "کلاس‌ها",
                                selected = selectedTab == "classes",
                                activeColor = brandColor,
                                onClick = { selectedTab = "classes" }
                            )

                            // Center Spacer for FAB
                            Spacer(modifier = Modifier.width(56.dp))

                            // Tab 3: Finance
                            BottomNavItem(
                                icon = Icons.Default.AttachMoney,
                                label = "مالی",
                                selected = selectedTab == "finance",
                                activeColor = brandColor,
                                onClick = { selectedTab = "finance" }
                            )

                            // Tab 4: Students
                            BottomNavItem(
                                icon = Icons.Default.Groups,
                                label = "دانش‌آموزان",
                                selected = selectedTab == "students",
                                activeColor = brandColor,
                                onClick = { selectedTab = "students" }
                            )
                        }
                    }

                    // Floating Action Button
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(y = (-20).dp)
                    ) {
                        Button(
                            onClick = { showAddMenu = true },
                            modifier = Modifier
                                .size(56.dp)
                                .testTag("fab_add_button"),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = brandColor),
                            contentPadding = PaddingValues(0.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "ایجاد جدید",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            // Screen content holder
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = paddingValues.calculateBottomPadding())
                    .background(scaffoldBg)
            ) {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        slideInVertically { height -> height } + fadeIn() with
                                slideOutVertically { height -> -height } + fadeOut()
                    },
                    label = "tabChange"
                ) { targetTab ->
                    when (targetTab) {
                        "dashboard" -> DashboardView(
                            viewModel = viewModel,
                            brandColor = brandColor,
                            onClassClick = { classId -> selectedClassDetailId = classId },
                            onGoToNotes = { selectedTab = "notes" },
                            onGoToCalendar = { selectedTab = "calendar" }
                        )
                        "classes" -> ClassesListView(
                            viewModel = viewModel,
                            brandColor = brandColor,
                            onClassClick = { classId -> selectedClassDetailId = classId },
                            onAddClassClick = { showAddClassDialog = true }
                        )
                        "students" -> StudentsListView(
                            viewModel = viewModel,
                            brandColor = brandColor,
                            onStudentClick = { studentId -> selectedStudentIdForDetail = studentId },
                            onAddStudentClick = { showAddStudentDialog = true }
                        )
                        "finance" -> FinanceView(
                            viewModel = viewModel,
                            brandColor = brandColor,
                            onAddPaymentClick = { showAddPaymentDialog = true }
                        )
                        "notes" -> ToDosView(
                            viewModel = viewModel,
                            brandColor = brandColor,
                            onAddToDoClick = { showAddToDoDialog = true }
                        )
                        "calendar" -> CalendarView(
                            viewModel = viewModel,
                            brandColor = brandColor
                        )
                    }
                }

                // Header Overlay containing Navigation triggers to Subpages (Calendar, Notes, Profiles)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 16.dp)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val teacher by viewModel.teacherName.collectAsStateWithLifecycle()
                    val school by viewModel.schoolName.collectAsStateWithLifecycle()

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Monogram logo
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(brandColor)
                                .border(1.5.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = teacher.take(1),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                        Column {
                            val greeting = when (selectedTab) {
                                "dashboard" -> "پیشخوان مدیریت"
                                "classes" -> "کلاس‌های شما"
                                "students" -> "بانک دانش‌آموزان"
                                "finance" -> "گزارش‌های امور مالی"
                                "notes" -> "یادداشت‌ها و کارها"
                                "calendar" -> "زمان‌بندی کلاس‌ها"
                                else -> "کلاس فلو"
                            }
                            Text(
                                text = greeting,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkThemeActive) Color.White else Color(0xFF1E293B)
                            )
                            if (school.isNotBlank() && selectedTab == "dashboard") {
                                Text(
                                    text = school,
                                    fontSize = 11.sp,
                                    color = if (isDarkThemeActive) Color(0xFF94A3B8) else Color(0xFF475569)
                                )
                            }
                        }
                    }

                    // Floating quick menu buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val headerBtnBg = if (isDarkThemeActive) Color(0xFF151D2F) else Color(0xFFFFFFFF)
                        val headerBtnBorder = if (isDarkThemeActive) Color(0xFF2E3E5B) else brandColor.copy(alpha = 0.10f)
                        val headerBtnTint = if (isDarkThemeActive) Color.White else Color(0xFF1E293B)

                        // Dark/Light Theme Switcher
                        IconButton(
                            onClick = { viewModel.toggleDarkMode() },
                            modifier = Modifier
                                .size(40.dp)
                                .background(headerBtnBg, RoundedCornerShape(12.dp))
                                .border(1.dp, headerBtnBorder, RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                imageVector = if (isDarkThemeActive) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "تغییر پوسته",
                                tint = if (isDarkThemeActive) Color(0xFFFBBF24) else headerBtnTint,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Calendar Trigger
                        IconButton(
                            onClick = { selectedTab = "calendar" },
                            modifier = Modifier
                                .size(40.dp)
                                .background(headerBtnBg, RoundedCornerShape(12.dp))
                                .border(1.dp, headerBtnBorder, RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "تقویم زمان‌بندی",
                                tint = headerBtnTint,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Notes & CRM Tasks Trigger
                        IconButton(
                            onClick = { selectedTab = "notes" },
                            modifier = Modifier
                                .size(40.dp)
                                .background(headerBtnBg, RoundedCornerShape(12.dp))
                                .border(1.dp, headerBtnBorder, RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.NoteAlt,
                                contentDescription = "یادداشت‌ها و کارها",
                                tint = headerBtnTint,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Quick Settings Profile Dialog
                        var showSettingsDialog by remember { mutableStateOf(false) }
                        IconButton(
                            onClick = { showSettingsDialog = true },
                            modifier = Modifier
                                .size(40.dp)
                                .background(headerBtnBg, RoundedCornerShape(12.dp))
                                .border(1.dp, headerBtnBorder, RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "تنظیمات کاربری",
                                tint = headerBtnTint,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        if (showSettingsDialog) {
                            UserProfileSettingsDialog(
                                viewModel = viewModel,
                                brandColor = brandColor,
                                onDismiss = { showSettingsDialog = false }
                            )
                        }
                    }
                }
            }
        }

        // Add Quick Action Sheet (Dialog)
        if (showAddMenu) {
            Dialog(onDismissRequest = { showAddMenu = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(32.dp)),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "ایجاد سریع برنامه",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Slate800
                        )

                        Text(
                            text = "برای مدیریت هوشمند مربیگری خود، یکی از ارکان زیر را انتخاب نمایید:",
                            fontSize = 12.sp,
                            color = Slate500,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Grid Options
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            QuickAddChoiceRow(
                                title = "افزودن کلاس جدید",
                                desc = "مشخصات دوره، روزهای برگزاری و مکان",
                                icon = Icons.Default.LibraryBooks,
                                color = brandColor,
                                onClick = {
                                    showAddClassDialog = true
                                    showAddMenu = false
                                }
                            )

                            QuickAddChoiceRow(
                                title = "ثبت دانش‌آموز جدید",
                                desc = "ثبت اطلاعات، اولیا و کلاس‌ها",
                                icon = Icons.Default.GroupAdd,
                                color = Color(0xFF0D9488),
                                onClick = {
                                    showAddStudentDialog = true
                                    showAddMenu = false
                                }
                            )

                            QuickAddChoiceRow(
                                title = "ثبت جلسه و حضور و غیاب",
                                desc = "آمار حضور و غیاب دانش‌آموزان و تکالیف کلاس",
                                icon = Icons.Default.FactCheck,
                                color = Color(0xFFD97706),
                                onClick = {
                                    showAddSessionDialog = true
                                    showAddMenu = false
                                }
                            )

                            QuickAddChoiceRow(
                                title = "دریافت شهریه و ثبت مالی",
                                desc = "ثبت تراکنش‌های دریافتی و تصفیه بدهی‌ها",
                                icon = Icons.Default.Payment,
                                color = Color(0xFF059669),
                                onClick = {
                                    showAddPaymentDialog = true
                                    showAddMenu = false
                                }
                            )

                            QuickAddChoiceRow(
                                title = "یادداشت و وظیفه جدید",
                                desc = " To-do لیست کارهای روزانه و تماس با اولیا",
                                icon = Icons.Default.PlaylistAddCheck,
                                color = Color(0xFFE11D48),
                                onClick = {
                                    showAddToDoDialog = true
                                    showAddMenu = false
                                }
                            )
                        }

                        Button(
                            onClick = { showAddMenu = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Slate100),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("بستن منو", color = Slate800, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Render Action Dialogs
        if (showAddClassDialog) {
            AddClassDialog(
                viewModel = viewModel,
                brandColor = brandColor,
                onDismiss = { showAddClassDialog = false }
            )
        }
        if (showAddStudentDialog) {
            AddStudentDialog(
                viewModel = viewModel,
                onDismiss = { showAddStudentDialog = false }
            )
        }
        if (showAddSessionDialog) {
            AddSessionDialog(
                viewModel = viewModel,
                brandColor = brandColor,
                onDismiss = { showAddSessionDialog = false }
            )
        }
        if (showAddPaymentDialog) {
            AddPaymentDialog(
                viewModel = viewModel,
                brandColor = brandColor,
                onDismiss = { showAddPaymentDialog = false }
            )
        }
        if (showAddToDoDialog) {
            AddToDoDialog(
                viewModel = viewModel,
                brandColor = brandColor,
                onDismiss = { showAddToDoDialog = false }
            )
        }

        // Details dialog triggers
        selectedClassDetailId?.let { classId ->
            ClassDetailDialog(
                classId = classId,
                viewModel = viewModel,
                brandColor = brandColor,
                onDismiss = { selectedClassDetailId = null }
            )
        }

        selectedStudentIdForDetail?.let { studentId ->
            StudentDetailDialog(
                studentId = studentId,
                viewModel = viewModel,
                brandColor = brandColor,
                onDismiss = { selectedStudentIdForDetail = null }
            )
        }
    }
}

@Composable
fun BottomNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val unselectedColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .background(if (selected) activeColor.copy(alpha = 0.12f) else Color.Transparent, RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) activeColor else unselectedColor,
                modifier = Modifier.size(24.dp)
            )

            AnimatedVisibility(
                visible = selected,
                enter = fadeIn() + expandHorizontally(expandFrom = Alignment.Start),
                exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.Start)
            ) {
                Row {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        color = activeColor,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun QuickAddChoiceRow(
    title: String,
    desc: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(Slate100.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .border(1.dp, Slate100, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(color.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(22.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Slate800)
            Text(desc, fontSize = 10.sp, color = Slate500, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(
            imageVector = Icons.Default.ChevronLeft,
            contentDescription = null,
            tint = Slate500,
            modifier = Modifier.size(16.dp)
        )
    }
}

// ---------------- USER INTERFACES ----------------

// Onboarding Presentation Screen
@Composable
fun OnboardingScreen(
    onComplete: (name: String, school: String, color: String) -> Unit
) {
    var teacherName by remember { mutableStateOf("") }
    var schoolName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#4F46E5") } // Default hex

    val colors = listOf(
        Pair("#4F46E5", "سورمه‌ای"),
        Pair("#0D9488", "فیروزه‌ای"),
        Pair("#7C3AED", "بنفش"),
        Pair("#D97706", "نارنجی"),
        Pair("#E11D48", "یاقوتی")
    )

    val activeColor = remember(selectedColor) {
        Color(android.graphics.Color.parseColor(selectedColor))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0F172A), // Slate 900
                        Color(0xFF020617)  // Slate 950 (Deep dark slate cosmic background)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Decorative pulsing back glows for depth
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.TopCenter)
                .offset(y = (-50).dp)
                .background(
                    Brush.radialGradient(
                        listOf(
                            activeColor.copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Stylized Glowing Icon Sphere
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(2.dp, Brush.radialGradient(listOf(activeColor, activeColor.copy(alpha = 0.3f))), CircleShape)
                    .padding(18.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    tint = activeColor,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "کلاس فلو",
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "مدیریت هوشمند، شیشه‌ای و همیشگی کارهای اساتید و مدارس",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Glassmorphic setup card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = Color.White.copy(alpha = 0.15f),
                backgroundColor = Color.White.copy(alpha = 0.06f)
            ) {
                Text(
                    text = "راه‌اندازی اولیه کلاس تخصصی",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = teacherName,
                    onValueChange = { teacherName = it },
                    label = { Text("نام و نام‌خانوادگی مدرس") },
                    placeholder = { Text("مثال: دکتر سهرابی") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboarding_teacher_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                        focusedBorderColor = activeColor,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedPlaceholderColor = Color.White.copy(alpha = 0.4f),
                        unfocusedPlaceholderColor = Color.White.copy(alpha = 0.3f),
                        focusedContainerColor = Color.White.copy(alpha = 0.04f),
                        unfocusedContainerColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = schoolName,
                    onValueChange = { schoolName = it },
                    label = { Text("نام آموزشگاه / مدرسه (اختیاری)") },
                    placeholder = { Text("مثال: فرهنگ مهر") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                        focusedBorderColor = activeColor,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedPlaceholderColor = Color.White.copy(alpha = 0.4f),
                        unfocusedPlaceholderColor = Color.White.copy(alpha = 0.3f),
                        focusedContainerColor = Color.White.copy(alpha = 0.04f),
                        unfocusedContainerColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "رنگ فضای کاری دلخواه شما:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    colors.forEach { (colorHex, name) ->
                        val parsedColor = Color(android.graphics.Color.parseColor(colorHex))
                        val isSelected = selectedColor == colorHex
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(parsedColor)
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = colorHex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (teacherName.isNotBlank()) {
                        onComplete(teacherName, schoolName, selectedColor)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("onboarding_submit_button"),
                shape = RoundedCornerShape(20.dp),
                enabled = teacherName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = activeColor,
                    disabledContainerColor = activeColor.copy(alpha = 0.4f)
                )
            ) {
                Text(
                    text = "شروع به کار در کلاس فلو",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

// Subview 1: Dashboard View
@Composable
fun DashboardView(
    viewModel: MainViewModel,
    brandColor: Color,
    onClassClick: (Long) -> Unit,
    onGoToNotes: () -> Unit,
    onGoToCalendar: () -> Unit
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val classes by viewModel.classes.collectAsStateWithLifecycle()
    val rawStudents by viewModel.students.collectAsStateWithLifecycle()
    val allSessions by viewModel.allSessions.collectAsStateWithLifecycle()
    val allPayments by viewModel.allPayments.collectAsStateWithLifecycle()
    val allToDos by viewModel.allToDos.collectAsStateWithLifecycle()
    val allSchedules by viewModel.allSchedules.collectAsStateWithLifecycle()

    var queryText by remember { mutableStateOf("") }
    
    // Interactive calendar date selection state
    var selectedYear by remember { mutableStateOf(1405) }
    var selectedMonth by remember { mutableStateOf(3) } // Month 3 = Khordad
    var selectedDay by remember { mutableStateOf(20) } // Default 20
    
    // We derive active week starting Anchor Day of Year
    // 16 Khordad 1405 is Day of Year 77. Let's initialize weekly anchor to 16 Khordad
    var currentWeekStartDayOfYear by remember { mutableStateOf(getDayOfYear1405(3, 16)) }
    
    var showMonthDialog by remember { mutableStateOf(false) }
    
    val selectedDayOfWeek = remember(selectedYear, selectedMonth, selectedDay) {
        getDayOfWeek1405(selectedMonth, selectedDay)
    }
    
    // Generate 7 days for the dynamic, scrollable calendar weekly row
    val daysOfTheWeek = remember(currentWeekStartDayOfYear) {
        (0..6).map { offset ->
            val dayOfYear = currentWeekStartDayOfYear + offset
            val date = getDateFromDayOfYear1405(dayOfYear)
            val m = date.first
            val d = date.second
            val dayOfWeek = (((dayOfYear % 7) + 7) % 7 + 1)
            val dayName = when (dayOfWeek) {
                1 -> "شنبه"
                2 -> "یکشنبه"
                3 -> "دوشنبه"
                4 -> "سه‌شنبه"
                5 -> "چهارشنبه"
                6 -> "پنج‌شنبه"
                7 -> "جمعه"
                else -> ""
            }
            DailyCalendarItem(dayOfWeek, dayName, d.toString(), m, 1405, dayOfYear)
        }
    }

    // High contrast styling colors
    val textPrimary = if (isDark) Color.White else Color(0xFF1E293B)
    val textSecondary = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
    val textTertiary = if (isDark) Color(0xFF94A3B8) else Color(0xFF8294AD)
    val cardBg = if (isDark) Color(0xFF151D2F) else Color(0xFFFFFFFF)
    val borderColor = if (isDark) Color(0xFF2E3E5B) else brandColor.copy(alpha = 0.10f)

    // Dynamic Filtered Data for dashboard search
    val activeClasses = remember(classes) { classes.filter { !it.isArchived } }
    val searchResults = remember(queryText, activeClasses) {
        if (queryText.isBlank()) emptyList()
        else {
            val q = queryText.lowercase()
            activeClasses.filter { it.name.lowercase().contains(q) || it.subject.lowercase().contains(q) }
        }
    }

    // Filter classes scheduled for the selected calendar day
    val classesForSelectedDay = remember(selectedDayOfWeek, activeClasses, allSchedules) {
        val schedulesForDay = allSchedules.filter { it.dayOfWeek == selectedDayOfWeek }
        activeClasses.filter { cls -> schedulesForDay.any { s -> s.classId == cls.id } }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = 76.dp) // Leave space for float header inside MainScreen
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Search Bar Row representing Global Search
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = queryText,
                onValueChange = { queryText = it },
                placeholder = { Text("جستجوی سریع کلاس‌ها یا موضوعات کلاسی...", fontSize = 12.sp, color = textTertiary) },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = textTertiary) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_search_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = textPrimary,
                    unfocusedTextColor = textPrimary,
                    focusedPlaceholderColor = textTertiary,
                    unfocusedPlaceholderColor = textTertiary,
                    focusedContainerColor = cardBg,
                    unfocusedContainerColor = cardBg,
                    focusedBorderColor = brandColor,
                    unfocusedBorderColor = borderColor
                )
            )
        }

        // Search Results Box (Renders if typing)
        if (queryText.isNotBlank()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("نتایج جستجو:", fontSize = 12.sp, color = textSecondary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (searchResults.isEmpty()) {
                        Text("موردی یافت نشد.", fontSize = 12.sp, color = textTertiary)
                    } else {
                        searchResults.forEach { cls ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onClassClick(cls.id)
                                        queryText = ""
                                    }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(android.graphics.Color.parseColor(cls.colorHex)))
                                    )
                                    Text(cls.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                                }
                                Text(cls.grade, fontSize = 11.sp, color = textSecondary)
                            }
                        }
                    }
                }
            }
        }

        // --- WEEKLY CALENDAR (امروز ۲۰ خرداد ۱۴۰۵) ---
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { showMonthDialog = true }
                    .background(brandColor.copy(alpha = 0.08f))
                    .border(1.dp, brandColor.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = brandColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "تقویم هوشمند کلاس فلو",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = textPrimary
                        )
                        Text(
                            text = "برای نمایش تقویم ماهانه کلیک کنید",
                            fontSize = 10.sp,
                            color = textTertiary
                        )
                    }
                }
                Text(
                    text = "$selectedDay ${getPersianMonthName(selectedMonth)} $selectedYear",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .background(brandColor, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }

            // Week Navigation Slider Bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { currentWeekStartDayOfYear -= 7 },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "هفته قبل", tint = brandColor)
                }
                
                Text(
                    text = "برنامه هفته جاری: ${getPersianMonthName(daysOfTheWeek.firstOrNull()?.month ?: 3)} 1405",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = textSecondary
                )
                
                IconButton(
                    onClick = { currentWeekStartDayOfYear += 7 },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "هفته بعد", tint = brandColor)
                }
            }

            // Days Horizontal Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                daysOfTheWeek.forEach { item ->
                    val isSelected = selectedDay == item.dayNumStr.toIntOrNull() && selectedMonth == item.month
                    val isToday = item.dayNumStr == "20" && item.month == 3

                    // Clickable flat card for each weekday
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                selectedYear = item.year
                                selectedMonth = item.month
                                selectedDay = item.dayNumStr.toIntOrNull() ?: 20
                            }
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) brandColor else borderColor,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) brandColor else cardBg
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp, horizontal = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = item.dayName.take(2),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else textTertiary
                            )
                            Text(
                                text = item.dayNumStr,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isSelected) Color.White else textPrimary
                            )
                            if (isToday) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color.White else brandColor)
                                )
                            } else {
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }
                    }
                }
            }
        }

        // --- SELECTED CALENDAR DAY'S CLASSES LIST ---
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val selectedDayName = when (selectedDayOfWeek) {
                1 -> "شنبه"
                2 -> "یکشنبه"
                3 -> "دوشنبه"
                4 -> "سه‌شنبه"
                5 -> "چهارشنبه"
                6 -> "پنج‌شنبه"
                7 -> "جمعه"
                else -> "روز انتخابی"
            }

            Text(
                text = "برنامه کلاس‌های روز $selectedDayName ($selectedDay ${getPersianMonthName(selectedMonth)} $selectedYear):",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = textSecondary
            )

            if (classesForSelectedDay.isEmpty()) {
                // Empty state card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, borderColor, RoundedCornerShape(16.dp)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventBusy,
                            contentDescription = null,
                            tint = textTertiary,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "هیچ کلاسی برای روز $selectedDayName ثبت نشده است.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = textSecondary
                        )
                        Text(
                            text = "می‌توانید کلاسی تازه ایجاد یا زمان‌بندی آن را مجدداً بررسی فرمایید.",
                            fontSize = 10.sp,
                            color = textTertiary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                classesForSelectedDay.forEach { cls ->
                    val classColor = remember(cls.colorHex) {
                        try {
                            Color(android.graphics.Color.parseColor(cls.colorHex))
                        } catch (e: Exception) {
                            brandColor
                        }
                    }

                    // Compute Progress metrics
                    val sessionsForThisClass = allSessions.filter { it.classId == cls.id && it.status == "HELD" }
                    val progressCount = sessionsForThisClass.size
                    val totalTargetSessions = cls.totalSessions

                    val classSchedules = allSchedules.filter { it.classId == cls.id && it.dayOfWeek == selectedDayOfWeek }
                    val timeRangeStr = classSchedules.firstOrNull()?.let { "${it.startTime} - ${it.endTime}" } ?: "۱۶:۰۰ - ۱۷:۳۰"

                    // Beautiful flat card with high contrast!
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onClassClick(cls.id) }
                            .border(1.dp, borderColor, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left-side circular indicator
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(classColor.copy(alpha = 0.1f), CircleShape)
                                    .border(1.5.dp, classColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Class,
                                    contentDescription = null,
                                    tint = classColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Middle Info
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = cls.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = textPrimary
                                    )
                                    Text(
                                        text = cls.grade,
                                        fontSize = 11.sp,
                                        color = textSecondary
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = textTertiary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = timeRangeStr,
                                            fontSize = 11.sp,
                                            color = textSecondary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Place,
                                            contentDescription = null,
                                            tint = textTertiary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = cls.location.take(15) + if (cls.location.length > 15) "..." else "",
                                            fontSize = 11.sp,
                                            color = textSecondary
                                        )
                                    }
                                }

                                // Linear Progress Indicator for Session Milestones
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    LinearProgressIndicator(
                                        progress = if (totalTargetSessions > 0) progressCount.toFloat() / totalTargetSessions.toFloat() else 0f,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(5.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = classColor,
                                        trackColor = borderColor
                                    )
                                    Text(
                                        text = "جلسه $progressCount از $totalTargetSessions",
                                        fontSize = 10.sp,
                                        color = textSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Next Class Focus Box (Alert/Shortcut for the teacher)
        val todayClasses = remember(activeClasses, allSchedules) {
            val schedulesToday = allSchedules.filter { it.dayOfWeek == 5 } // Today is Wed (5)
            activeClasses.filter { cls -> schedulesToday.any { s -> s.classId == cls.id } }
        }
        val nextClassToday = todayClasses.firstOrNull()

        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onGoToCalendar() },
            colors = CardDefaults.cardColors(containerColor = brandColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "یادآوری کلاس امروز شما:",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Bold
                        )
                        if (nextClassToday != null) {
                            Text(
                                text = nextClassToday.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "${nextClassToday.subject} • ${nextClassToday.location}",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        } else {
                            Text(
                                text = "امروز کلاس فعال ثبت‌شده‌ای ندارید",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Quick ToDo widget
        val outstandingToDos = remember(allToDos) { allToDos.filter { !it.isCompleted } }
        if (outstandingToDos.isNotEmpty()) {
            val todoItemColorBg = if (isDark) Color(0xFF2C1F02) else Color(0xFFFEF3C7)
            val todoItemBorder = if (isDark) Color(0xFF78350F) else Color(0xFFFDE68A)
            val todoItemText = if (isDark) Color(0xFFFDE68A) else Color(0xFF92400E)

            Card(
                onClick = onGoToNotes,
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = todoItemColorBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, todoItemBorder, RoundedCornerShape(16.dp)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(todoItemBorder, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.StickyNote2,
                                contentDescription = null,
                                tint = todoItemText
                            )
                        }
                        Column {
                            Text(
                                text = "یادداشت پیگیری:",
                                fontSize = 11.sp,
                                color = todoItemText,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = outstandingToDos.first().title,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = textPrimary
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = null,
                        tint = todoItemText
                    )
                }
            }
        }

        if (showMonthDialog) {
            MonthCalendarDialog(
                initialYear = selectedYear,
                initialMonth = selectedMonth,
                initialDay = selectedDay,
                brandColor = brandColor,
                onDismiss = { showMonthDialog = false },
                onDateSelected = { y, m, d ->
                    selectedYear = y
                    selectedMonth = m
                    selectedDay = d
                    currentWeekStartDayOfYear = getWeekStartDayOfYear(m, d)
                    showMonthDialog = false
                }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun StatsCompactCard(
    title: String,
    value: String,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = modifier.border(1.dp, Slate100, RoundedCornerShape(20.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(title, fontSize = 9.sp, color = Slate500, fontWeight = FontWeight.Bold)
            Text(
                value,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(unit, fontSize = 8.sp, color = Slate500)
        }
    }
}

// Subview 2: Classes Tab Manager
@Composable
fun ClassesListView(
    viewModel: MainViewModel,
    brandColor: Color,
    onClassClick: (Long) -> Unit,
    onAddClassClick: () -> Unit
) {
    val rawClasses by viewModel.classes.collectAsStateWithLifecycle()
    val allSessions by viewModel.allSessions.collectAsStateWithLifecycle()
    val allSchedules by viewModel.allSchedules.collectAsStateWithLifecycle()
    var showArchived by remember { mutableStateOf(false) }

    val activeClasses = remember(rawClasses) { rawClasses.filter { !it.isArchived } }
    val archivedClasses = remember(rawClasses) { rawClasses.filter { it.isArchived } }

    val listing = if (showArchived) archivedClasses else activeClasses

    val isDark = isSystemInDarkTheme()
    val cardBg = if (isDark) Color(0xFF151D2F) else Color(0xFFFFFFFF)
    val cardBorder = if (isDark) Color(0xFF2E3E5B) else brandColor.copy(alpha = 0.10f)
    val textPrimary = if (isDark) Color.White else Color(0xFF1E293B)
    val textSecondary = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
    val textTertiary = if (isDark) Color(0xFF94A3B8) else Color(0xFF8294AD)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = 76.dp)
            .padding(horizontal = 16.dp)
    ) {
        // Toggle Active or Archive Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .background(cardBg, RoundedCornerShape(14.dp))
                    .border(1.dp, cardBorder, RoundedCornerShape(14.dp))
                    .padding(4.dp)
            ) {
                Button(
                    onClick = { showArchived = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!showArchived) brandColor else Color.Transparent,
                        contentColor = if (!showArchived) Color.White else textSecondary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("کلاس‌های فعال", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { showArchived = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showArchived) brandColor else Color.Transparent,
                        contentColor = if (showArchived) Color.White else textSecondary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("آرشیو شده", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Button(
                onClick = onAddClassClick,
                colors = ButtonDefaults.buttonColors(containerColor = brandColor.copy(alpha = 0.15f), contentColor = brandColor),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("افزودن کلاس", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            if (listing.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (showArchived) "آرشیوی یافت نشد." else "کلاس فعالی ثبت نشده است.",
                            fontSize = 12.sp,
                            color = textTertiary
                        )
                    }
                }
            } else {
                val chunkedList = listing.chunked(2)
                items(chunkedList) { rowClasses ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowClasses.forEach { cls ->
                            val classColor = remember(cls.colorHex) {
                                try {
                                    Color(android.graphics.Color.parseColor(cls.colorHex))
                                } catch (e: Exception) {
                                    brandColor
                                }
                            }

                            val sessionsCount = remember(allSessions, cls.id) { allSessions.filter { it.classId == cls.id }.size }
                            val totalSessions = cls.totalSessions
                            val progressFraction = if (totalSessions > 0) sessionsCount.toFloat() / totalSessions.toFloat() else 0f

                            val classSchedules = remember(allSchedules, cls.id) { allSchedules.filter { it.classId == cls.id } }
                            val timeLabel = remember(classSchedules) {
                                if (classSchedules.isNotEmpty()) {
                                    val first = classSchedules.first()
                                    "${first.startTime} - ${first.endTime}"
                                } else {
                                    "ساعت نامشخص"
                                }
                            }

                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(235.dp)
                                    .clickable { onClassClick(cls.id) }
                                    .border(1.dp, cardBorder, RoundedCornerShape(16.dp)),
                                colors = CardDefaults.cardColors(containerColor = cardBg),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(classColor)
                                        )
                                        Text(
                                            text = cls.subject.take(12) + if (cls.subject.length > 12) "..." else "",
                                            color = classColor,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .background(classColor.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    Box(
                                        modifier = Modifier.size(80.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            progress = progressFraction,
                                            modifier = Modifier.fillMaxSize(),
                                            color = classColor,
                                            strokeWidth = 6.dp,
                                            trackColor = cardBorder
                                        )
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = "${(progressFraction * 100).toInt()}%",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = textPrimary
                                            )
                                            Text(
                                                text = "$sessionsCount/$totalSessions",
                                                fontSize = 9.sp,
                                                color = textTertiary
                                            )
                                        }
                                    }

                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = cls.name,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${cls.grade} • ${cls.location}",
                                            fontSize = 10.sp,
                                            color = textSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "⏱️ $timeLabel",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = brandColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        if (rowClasses.size < 2) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

// Subview 3: Students Tab Manager
@Composable
fun StudentsListView(
    viewModel: MainViewModel,
    brandColor: Color,
    onStudentClick: (Long) -> Unit,
    onAddStudentClick: () -> Unit
) {
    val rawStudents by viewModel.students.collectAsStateWithLifecycle()
    var searchTemp by remember { mutableStateOf("") }

    val displayStudents = remember(rawStudents, searchTemp) {
        if (searchTemp.isBlank()) rawStudents
        else rawStudents.filter { it.name.contains(searchTemp) || it.phone.contains(searchTemp) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = 76.dp)
            .padding(horizontal = 16.dp)
    ) {
        // Key Stats for Active Students (Moved from Dashboard) - styled for dark and light theme
        val isDark = isSystemInDarkTheme()
        val textPrimary = if (isDark) Color.White else Color(0xFF1E293B)
        val textSecondary = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .border(1.dp, if (isDark) Color(0xFF2E3E5B) else brandColor.copy(alpha = 0.10f), RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF151D2F) else Color(0xFFFFFFFF)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "کل دانش‌آموزان ثبت‌شده",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Text(
                        text = "لیست فراگیران فعال در کلاس فلو",
                        fontSize = 11.sp,
                        color = textSecondary
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = rawStudents.size.toString(),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = brandColor
                    )
                    Text(
                        text = "هنرجو",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = textSecondary
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchTemp,
                onValueChange = { searchTemp = it },
                placeholder = { Text("نام یا شمار تلفن دانش‌آموز...", fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = brandColor
                )
            )

            Button(
                onClick = onAddStudentClick,
                colors = ButtonDefaults.buttonColors(containerColor = brandColor),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.height(44.dp)
            ) {
                Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("دانش‌آموز جدید", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            if (displayStudents.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("دانش‌آموزی ثبت نشده است.", fontSize = 12.sp, color = Slate500)
                    }
                }
            }

            items(displayStudents, key = { it.id }) { student ->
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onStudentClick(student.id) },
                    brandColor = brandColor
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(brandColor.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = student.name.take(1),
                                    color = brandColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Column {
                                Text(
                                    student.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )
                                Text(
                                    "همراه: ${student.phone}",
                                    fontSize = 11.sp,
                                    color = textSecondary
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = null,
                            tint = Slate500,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// Subview 4: Finance Tracker and calculations
@Composable
fun FinanceView(
    viewModel: MainViewModel,
    brandColor: Color,
    onAddPaymentClick: () -> Unit
) {
    val allPayments by viewModel.allPayments.collectAsStateWithLifecycle()
    val classes by viewModel.classes.collectAsStateWithLifecycle()
    val students by viewModel.students.collectAsStateWithLifecycle()

    var activeFinanceTab by remember { mutableStateOf("monthly") } // "weekly", "monthly", "yearly", "overall"

    val isDark = isSystemInDarkTheme()
    val textPrimary = if (isDark) Color.White else Color(0xFF1E293B)
    val textSecondary = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
    val borderCol = if (isDark) Color(0xFF2E3E5B) else brandColor.copy(alpha = 0.10f)

    val filteredPayments = remember(allPayments, activeFinanceTab) {
        when (activeFinanceTab) {
            "weekly" -> {
                allPayments.filter { p ->
                    try {
                        val parts = p.date.split("/")
                        if (parts.size == 3) {
                            val y = parts[0].toIntOrNull() ?: 0
                            val m = parts[1].toIntOrNull() ?: 0
                            val d = parts[2].toIntOrNull() ?: 0
                            y == 1405 && m == 3 && d in 14..20
                        } else false
                    } catch (e: Exception) {
                        false
                    }
                }
            }
            "monthly" -> {
                allPayments.filter { p ->
                    p.date.startsWith("1405/03") || p.date.startsWith("1405/3/")
                }
            }
            "yearly" -> {
                allPayments.filter { p ->
                    p.date.startsWith("1405")
                }
            }
            else -> allPayments
        }
    }

    val receivedTotal = remember(filteredPayments) { filteredPayments.sumOf { it.amountPaid } }
    val dueTotal = remember(filteredPayments) { filteredPayments.sumOf { it.amountDue } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = 76.dp)
            .padding(horizontal = 16.dp)
    ) {
        // Interval Selector Segmented Control Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val tabs = listOf(
                Pair("weekly", "هفتگی"),
                Pair("monthly", "ماهانه"),
                Pair("yearly", "سالانه"),
                Pair("overall", "کلی")
            )
            
            tabs.forEach { (tabId, label) ->
                val selected = activeFinanceTab == tabId
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { activeFinanceTab = tabId }
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) brandColor else (if (isDark) Color(0xFF1E283C) else Slate100))
                        .border(1.dp, if (selected) brandColor else borderCol, RoundedCornerShape(12.dp))
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) Color.White else textPrimary
                    )
                }
            }
        }

        // Stats Summary Box - Redesigned to use high-contrast flat elevated cards matching themed primary brandColor
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF151D2F) else brandColor
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .border(
                    width = 1.dp,
                    color = if (isDark) Color(0xFF2E3E5B) else brandColor.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(20.dp)
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when (activeFinanceTab) {
                        "weekly" -> "تراز مالی هفتگی (۱۴ تا ۲۰ خرداد ۱۴۰۵)"
                        "monthly" -> "تراز مالی ماه جاری (خرداد ۱۴۰۵)"
                        "yearly" -> "تراز مالی سالانه (سال ۱۴۰۵)"
                        else -> "تراز مالی کلی مدرسه و کلاس‌ها"
                    },
                    fontSize = 12.sp,
                    color = if (isDark) Color(0xFF94A3B8) else Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "وصول شده",
                            fontSize = 10.sp,
                            color = if (isDark) Color(0xFFCBD5E1) else Color.White.copy(alpha = 0.85f)
                        )
                        Text(
                            text = "${formatPrice(receivedTotal)} ریال",
                            fontSize = 17.sp,
                            color = if (isDark) Color(0xFF10B981) else Color(0xFF34D399),
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "بدهی باقی‌مانده",
                            fontSize = 10.sp,
                            color = if (isDark) Color(0xFFCBD5E1) else Color.White.copy(alpha = 0.85f)
                        )
                        Text(
                            text = "${formatPrice(dueTotal)} ریال",
                            fontSize = 17.sp,
                            color = if (isDark) Color(0xFFEF4444) else Color(0xFFFF8A8A),
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("آخرین تراکنش‌های این بازه", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textPrimary)

            Button(
                onClick = onAddPaymentClick,
                colors = ButtonDefaults.buttonColors(containerColor = brandColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("ثبت تراکنش شهریه", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            if (filteredPayments.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("تراکنشی در این بازه زمانی یافت نشد.", fontSize = 12.sp, color = Slate500)
                    }
                }
            }

            items(filteredPayments) { pay ->
                val clsName = classes.find { it.id == pay.classId }?.name ?: "کلاس عمومی"
                val studentName = students.find { it.id == pay.studentId }?.name ?: "دانش‌آموز عمومی"

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    brandColor = brandColor
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(studentName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = textPrimary)
                            Text("$clsName • ${pay.date}", fontSize = 10.sp, color = textSecondary)
                            if (pay.notes.isNotBlank()) {
                                Text(pay.notes, fontSize = 10.sp, color = textSecondary, style = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace))
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "وصولی: ${formatPrice(pay.amountPaid)}",
                                fontSize = 11.sp,
                                color = Color(0xFF059669),
                                fontWeight = FontWeight.Bold
                            )
                            if (pay.amountDue > 0) {
                                Text(
                                    "بدهی: ${formatPrice(pay.amountDue)}",
                                    fontSize = 11.sp,
                                    color = Color(0xFFE11D48),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(
                                onClick = { viewModel.deletePayment(pay.id) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = Slate500.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// Subview 5: ToDoc/Notes CRM Manager
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ToDosView(
    viewModel: MainViewModel,
    brandColor: Color,
    onAddToDoClick: () -> Unit
) {
    val allToDos by viewModel.allToDos.collectAsStateWithLifecycle()
    val completedToDos = remember(allToDos) { allToDos.filter { it.isCompleted } }
    val pendingToDos = remember(allToDos) { allToDos.filter { !it.isCompleted } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = 76.dp)
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("یادداشت‌های پیگیری و وظایف من", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Slate800)

            Button(
                onClick = onAddToDoClick,
                colors = ButtonDefaults.buttonColors(containerColor = brandColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PlaylistAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("کار جدید", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            if (allToDos.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("کاری ثبت نشده است. کارهای جدید یا یادآوری تماس ها را ثبت نمایید.", fontSize = 12.sp, color = Slate500)
                    }
                }
            }

            // Pending ToDos Header
            if (pendingToDos.isNotEmpty()) {
                item {
                    Text("کارهای در دست اجرا (${pendingToDos.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate500)
                }

                items(pendingToDos, key = { item: ToDoItem -> "pending_${item.id}" }) { item ->
                    ToDoRowCard(
                        item = item,
                        onToggle = { viewModel.toggleToDo(item) },
                        onDelete = { viewModel.deleteToDo(item) },
                        brandColor = brandColor
                    )
                }
            }

            // Completed ToDos Header
            if (completedToDos.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("کارهای پایان یافته (${completedToDos.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate500)
                }

                items(completedToDos, key = { item: ToDoItem -> "completed_${item.id}" }) { item ->
                    ToDoRowCard(
                        item = item,
                        onToggle = { viewModel.toggleToDo(item) },
                        onDelete = { viewModel.deleteToDo(item) },
                        brandColor = brandColor
                    )
                }
            }
        }
    }
}

@Composable
fun ToDoRowCard(
    item: ToDoItem,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    brandColor: Color
) {
    val isDark = isSystemInDarkTheme()
    val cardBg = if (isDark) {
        if (item.isCompleted) Color(0xFF151D2F).copy(alpha = 0.6f) else Color(0xFF151D2F)
    } else {
        if (item.isCompleted) Color.White.copy(alpha = 0.5f) else Color.White
    }
    val borderColor = if (isDark) Color(0xFF2E3E5B) else brandColor.copy(alpha = 0.10f)
    val textPrimary = if (isDark) Color.White else Color(0xFF1E293B)
    val textSecondary = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Checkbox(
                    checked = item.isCompleted,
                    onCheckedChange = { onToggle() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = brandColor
                    )
                )

                Column {
                    Text(
                        text = item.title,
                        fontSize = 13.sp,
                        fontWeight = if (item.isCompleted) FontWeight.Normal else FontWeight.Bold,
                        color = if (item.isCompleted) textSecondary else textPrimary,
                        style = if (item.isCompleted) androidx.compose.ui.text.TextStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough) else androidx.compose.ui.text.TextStyle()
                    )
                    Text(
                        text = "موعد: ${item.dueDateStr} • ${translateType(item.type)}",
                        fontSize = 10.sp,
                        color = textSecondary
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = textSecondary.copy(alpha = 0.6f))
            }
        }
    }
}

fun translateType(type: String): String {
    return when (type) {
        "PERSONAL" -> "شخصی"
        "PARENT_CONTACT" -> "تماس اولیا"
        "CLASS_PREP" -> "کلاس آکادمیک"
        else -> "عمومی"
    }
}

data class DailyCalendarItem(
    val dayOfWeek: Int,
    val dayName: String,
    val dayNumStr: String,
    val month: Int,
    val year: Int,
    val dayOfYear: Int
)

fun getDayOfYear1405(month: Int, day: Int): Int {
    val lengths = listOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)
    var sum = 0
    val mSafe = month.coerceIn(1, 12)
    for (i in 0 until (mSafe - 1)) {
        sum += lengths[i]
    }
    return sum + (day - 1)
}

fun getDayOfWeek1405(month: Int, day: Int): Int {
    val dayOfYear = getDayOfYear1405(month, day) // 1 Farvardin 1405 is Saturday (1)
    return (((dayOfYear % 7) + 7) % 7 + 1)
}

fun getDateFromDayOfYear1405(dayOfYear: Int): Pair<Int, Int> {
    val lengths = listOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)
    val totalYearDays = lengths.sum()
    var remaining = ((dayOfYear % totalYearDays) + totalYearDays) % totalYearDays
    var m = 1
    while (m <= 12 && remaining >= lengths[m - 1]) {
        remaining -= lengths[m - 1]
        m++
    }
    return Pair(m, remaining + 1)
}

fun getPersianMonthName(month: Int): String {
    return when (month) {
        1 -> "فروردین"
        2 -> "اردیبهشت"
        3 -> "خرداد"
        4 -> "تیر"
        5 -> "مرداد"
        6 -> "شهریور"
        7 -> "مهر"
        8 -> "آبان"
        9 -> "آذر"
        10 -> "دی"
        11 -> "بهمن"
        12 -> "اسفند"
        else -> ""
    }
}

fun getWeekStartDayOfYear(month: Int, day: Int): Int {
    val dayOfYear = getDayOfYear1405(month, day)
    val dayOfWeek = getDayOfWeek1405(month, day)
    return dayOfYear - (dayOfWeek - 1)
}

// Subview 6: Calendar and schedule timelines
@Composable
fun CalendarView(
    viewModel: MainViewModel,
    brandColor: Color
) {
    val schedules by viewModel.allSchedules.collectAsStateWithLifecycle()
    val classes by viewModel.classes.collectAsStateWithLifecycle()

    var activeDayFilter by remember { mutableStateOf(1) } // Default Saturday

    val daysText = listOf(
        Pair(1, "شنبه"),
        Pair(2, "یکشنبه"),
        Pair(3, "دوشنبه"),
        Pair(4, "سه‌شنبه"),
        Pair(5, "چهارشنبه"),
        Pair(6, "پنج‌شنبه"),
        Pair(7, "جمعه")
    )

    val currentDaySchedules = remember(schedules, activeDayFilter) {
        schedules.filter { it.dayOfWeek == activeDayFilter }
    }

    val isDark = isSystemInDarkTheme()
    val cardBg = if (isDark) Color(0xFF151D2F) else Color.White
    val borderColor = if (isDark) Color(0xFF2E3E5B) else brandColor.copy(alpha = 0.10f)
    val textPrimary = if (isDark) Color.White else Color(0xFF1E293B)
    val textSecondary = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = 76.dp)
            .padding(horizontal = 16.dp)
    ) {
        // Horizontal calendar slider
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(daysText) { (idx, name) ->
                val selected = idx == activeDayFilter
                Box(
                    modifier = Modifier
                        .clickable { activeDayFilter = idx }
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (selected) brandColor else cardBg)
                        .border(1.dp, if (selected) brandColor else borderColor, RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        name,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) Color.White else textPrimary
                    )
                }
            }
        }

        Text(
            text = "کلاس‌های مشخص شده روز ${daysText.find { it.first == activeDayFilter }?.second}:",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Slate500,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            if (currentDaySchedules.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 60.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("در این روز کلاسی زمان‌بندی نشده است.", fontSize = 12.sp, color = Slate500)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "سیستم زمان خالی را پیشنهاد می‌دهد:",
                            fontSize = 10.sp,
                            color = Slate500,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                items(currentDaySchedules) { schedule ->
                    val cls = classes.find { it.id == schedule.classId }
                    val clsName = cls?.name ?: "آموزش متداول"
                    val clsLocation = cls?.location ?: "نامشخص"
                    val clsColor = remember(cls) {
                        try {
                            Color(android.graphics.Color.parseColor(cls?.colorHex ?: "#4F46E5"))
                        } catch (e: Exception) {
                            brandColor
                        }
                    }

                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = clsColor.copy(alpha = 0.3f),
                        brandColor = brandColor
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(clsColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PunchClock,
                                        contentDescription = null,
                                        tint = clsColor
                                    )
                                }
                                Column {
                                    Text(clsName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Slate800)
                                    Text("ساعت برگزاری: ${schedule.startTime} الی ${schedule.endTime}", fontSize = 11.sp, color = Slate500)
                                }
                            }

                            Text(
                                "مکان: $clsLocation",
                                fontSize = 11.sp,
                                color = Slate500
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------- DIALOGS AND EDITORS ----------------

// 1. Add Class Dialog with suggestion engine (پیشنهاد زمان خالی)
@Composable
fun AddClassDialog(
    viewModel: MainViewModel,
    brandColor: Color,
    onDismiss: () -> Unit
) {
    var currentStep by remember { mutableStateOf(0) }

    var name by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var grade by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var capacity by remember { mutableStateOf(1) }
    var totalSessions by remember { mutableStateOf(10) }

    var selectedColorHex by remember { mutableStateOf("#4F46E5") }
    val palette = listOf("#4F46E5", "#0D9488", "#7C3AED", "#D97706", "#E11D48")

    // Intelligent Free Hour Proposer!
    var showProposer by remember { mutableStateOf(false) }
    val recommendedSlots = remember { viewModel.suggestFreeTimes() }
    var selectedSchedules = remember { mutableStateListOf<Pair<Int, String>>() }

    val isDark = isSystemInDarkTheme()
    val dialogBg = if (isDark) Color(0xFF151D2F) else Color.White
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
    val dialogBorder = if (isDark) Color(0xFF2E3E5B) else Color(0xFFE2E8F0)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .border(1.dp, dialogBorder, RoundedCornerShape(32.dp)),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = dialogBg)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("ثبت و زمان‌بندی کلاس جدید", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textPrimary)

                // Visual Step Wizard Progress Bar
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (i in 0..2) {
                        val active = i <= currentStep
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .background(if (active) brandColor else (if (isDark) Color(0xFF2E3E5B) else Color(0xFFE2E8F0)), RoundedCornerShape(2.dp))
                        )
                    }
                }
                Text(
                    text = "مرحله ${currentStep + 1} از ۳: " + when(currentStep) {
                        0 -> "مشخصات عمومی کلاس"
                        1 -> "تنظیمات ظرفیت و ویژگی‌ها"
                        else -> "زمان‌بندی هوشمند کلاس"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = brandColor
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Conditionally display content fields based on current step
                when (currentStep) {
                    0 -> {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("نام کلاس", color = textSecondary) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary,
                                focusedBorderColor = brandColor,
                                unfocusedBorderColor = dialogBorder
                            )
                        )

                        OutlinedTextField(
                            value = subject,
                            onValueChange = { subject = it },
                            label = { Text("درس و تخصص", color = textSecondary) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary,
                                focusedBorderColor = brandColor,
                                unfocusedBorderColor = dialogBorder
                            )
                        )

                        OutlinedTextField(
                            value = grade,
                            onValueChange = { grade = it },
                            label = { Text("پایه و مقطع آموزشی", color = textSecondary) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary,
                                focusedBorderColor = brandColor,
                                unfocusedBorderColor = dialogBorder
                            )
                        )

                        OutlinedTextField(
                            value = location,
                            onValueChange = { location = it },
                            label = { Text("مکان / آدرس برگزاری", color = textSecondary) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary,
                                focusedBorderColor = brandColor,
                                unfocusedBorderColor = dialogBorder
                            )
                        )
                    }
                    1 -> {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = capacity.toString(),
                                onValueChange = { capacity = it.toIntOrNull() ?: 1 },
                                label = { Text("ظرفیت (نفر)", color = textSecondary) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = textPrimary,
                                    unfocusedTextColor = textPrimary,
                                    focusedBorderColor = brandColor,
                                    unfocusedBorderColor = dialogBorder
                                )
                            )

                            OutlinedTextField(
                                value = totalSessions.toString(),
                                onValueChange = { totalSessions = it.toIntOrNull() ?: 10 },
                                label = { Text("کل جلسات", color = textSecondary) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = textPrimary,
                                    unfocusedTextColor = textPrimary,
                                    focusedBorderColor = brandColor,
                                    unfocusedBorderColor = dialogBorder
                                )
                            )
                        }

                        Text("انتخاب رنگ کارت کلاس:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textSecondary)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            palette.forEach { c ->
                                val col = Color(android.graphics.Color.parseColor(c))
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(col)
                                        .border(
                                            width = if (selectedColorHex == c) 3.dp else 0.dp,
                                            color = textPrimary,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedColorHex = c }
                                )
                            }
                        }
                    }
                    2 -> {
                        // Suggest times engine
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(brandColor.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                                .clickable { showProposer = !showProposer }
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("پیشنهاد هوشمند زمان‌های خالی کلاسی شما", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = brandColor)
                                    Text("سیستم تحلیل تداخل هفتگی فعال است", fontSize = 10.sp, color = textSecondary)
                                }
                                Icon(
                                    imageVector = if (showProposer) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = brandColor
                                )
                            }
                        }

                        if (showProposer) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                recommendedSlots.forEach { (dayIdx, start, end) ->
                                    val txt = "${viewModel.getDayName(dayIdx)} ساعت $dayIdx ($start تا $end)"
                                    val isSelected = selectedSchedules.any { it.first == dayIdx && it.second == "$start-$end" }

                                    Button(
                                        onClick = {
                                            if (isSelected) {
                                                selectedSchedules.removeAll { it.first == dayIdx && it.second == "$start-$end" }
                                            } else {
                                                selectedSchedules.add(Pair(dayIdx, "$start-$end"))
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) brandColor else (if (isDark) Color(0xFF1E283C) else Slate100),
                                            contentColor = if (isSelected) Color.White else textPrimary
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(txt, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            // If collapsed, display summary of chosen schedule days
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E283C) else Slate100)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("برنامه کلاسی منتخب:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = textPrimary)
                                    if (selectedSchedules.isEmpty()) {
                                        Text("هنوز زمانی انتخاب نشده است. لطفاً کادر بالا را لمس کرده و زمان کلاسی را مشخص نمایید.", fontSize = 10.sp, color = textSecondary)
                                    } else {
                                        selectedSchedules.forEach { (d, t) ->
                                            Text("• ${viewModel.getDayName(d)} ساعت $t", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = brandColor)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Dialog Action Buttons row with wizard logic
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (currentStep > 0) {
                        Button(
                            onClick = { currentStep-- },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF243048) else Slate100),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("قبلی", color = textPrimary)
                        }
                    } else {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF243048) else Slate100),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("لغو", color = textPrimary)
                        }
                    }

                    if (currentStep < 2) {
                        Button(
                            onClick = {
                                if (currentStep == 0 && name.isBlank()) {
                                    // Validation constraint trigger
                                } else {
                                    currentStep++
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = brandColor),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            enabled = if (currentStep == 0) name.isNotBlank() else true
                        ) {
                            Text("بعدی", color = Color.White)
                        }
                    } else {
                        Button(
                            onClick = {
                                if (name.isNotBlank()) {
                                    if (selectedSchedules.isEmpty()) {
                                        selectedSchedules.add(Pair(3, "16:00-17:30"))
                                    }
                                    viewModel.createClass(
                                        name = name,
                                        subject = subject,
                                        grade = grade,
                                        location = location,
                                        capacity = capacity,
                                        colorHex = selectedColorHex,
                                        totalSessions = totalSessions,
                                        startDate = "1405/03/10",
                                        endDate = "1405/06/15",
                                        schedules = selectedSchedules.toList()
                                    )
                                    onDismiss()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = brandColor),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            enabled = name.isNotBlank()
                        ) {
                            Text("ثبت نهایی", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// 2. Add Student Dialog
@Composable
fun AddStudentDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    var currentStep by remember { mutableStateOf(0) }

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var parentPhone by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val classes by viewModel.classes.collectAsStateWithLifecycle()
    val activeClasses = remember(classes) { classes.filter { !it.isArchived } }
    val selectedClassIds = remember { mutableStateListOf<Long>() }

    val isDark = isSystemInDarkTheme()
    val dialogBg = if (isDark) Color(0xFF151D2F) else Color.White
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
    val dialogBorder = if (isDark) Color(0xFF2E3E5B) else Color(0xFFE2E8F0)

    val brandHex = remember { viewModel.schoolName.value } // Fetch teacher brand settings color
    val brandColor = remember(brandHex) {
        try {
            Color(android.graphics.Color.parseColor("#4F46E5"))
        } catch(e: Exception) {
            Color(0xFF4F46E5)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .border(1.dp, dialogBorder, RoundedCornerShape(32.dp)),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = dialogBg)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("ثبت دانش‌آموز جدید", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textPrimary)

                // Progress indicators
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (i in 0..1) {
                        val active = i <= currentStep
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .background(if (active) brandColor else (if (isDark) Color(0xFF2E3E5B) else Color(0xFFE2E8F0)), RoundedCornerShape(2.dp))
                        )
                    }
                }
                Text(
                    text = "مرحله ${currentStep + 1} از ۲: " + when(currentStep) {
                        0 -> "اطلاعات شناسایی و تماس"
                        else -> "انتساب به کلاس‌های فعال"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = brandColor
                )

                Spacer(modifier = Modifier.height(4.dp))

                when (currentStep) {
                    0 -> {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("نام و نام‌خانوادگی", color = textSecondary) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary,
                                focusedBorderColor = brandColor,
                                unfocusedBorderColor = dialogBorder
                            )
                        )

                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("شماره همراه دانش‌آموز", color = textSecondary) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary,
                                focusedBorderColor = brandColor,
                                unfocusedBorderColor = dialogBorder
                            )
                        )

                        OutlinedTextField(
                            value = parentPhone,
                            onValueChange = { parentPhone = it },
                            label = { Text("شماره همراه ولی / والدین", color = textSecondary) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary,
                                focusedBorderColor = brandColor,
                                unfocusedBorderColor = dialogBorder
                            )
                        )

                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("توضیحات و یادداشت‌های آموزشی مربوطه", color = textSecondary) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary,
                                focusedBorderColor = brandColor,
                                unfocusedBorderColor = dialogBorder
                            )
                        )
                    }
                    1 -> {
                        Text("کلاس یا کلاس‌های مرتبط را مشخص کنید:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textSecondary)
                        if (activeClasses.isEmpty()) {
                            Text("کلاسی در سیستم وجود ندارد. لطفاً ابتدا کلاس تعریف کنید.", fontSize = 11.sp, color = textSecondary)
                        } else {
                            activeClasses.forEach { cls ->
                                val isChecked = selectedClassIds.contains(cls.id)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (isChecked) selectedClassIds.remove(cls.id) else selectedClassIds.add(cls.id)
                                        }
                                        .background(if (isChecked) brandColor.copy(alpha = 0.1f) else Color.Transparent, RoundedCornerShape(10.dp))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = null,
                                        colors = CheckboxDefaults.colors(checkedColor = brandColor)
                                    )
                                    Text(cls.name, fontSize = 12.sp, color = textPrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (currentStep > 0) {
                        Button(
                            onClick = { currentStep-- },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF243048) else Slate100),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("قبلی", color = textPrimary)
                        }
                    } else {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF243048) else Slate100),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("لغو", color = textPrimary)
                        }
                    }

                    if (currentStep < 1) {
                        Button(
                            onClick = {
                                if (name.isNotBlank()) {
                                    currentStep++
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = brandColor),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            enabled = name.isNotBlank()
                        ) {
                            Text("بعدی", color = Color.White)
                        }
                    } else {
                        Button(
                            onClick = {
                                if (name.isNotBlank()) {
                                    viewModel.createStudent(name, phone, parentPhone, notes, selectedClassIds.toList())
                                    onDismiss()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = brandColor),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            enabled = name.isNotBlank()
                        ) {
                            Text("ثبت نهایی", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// 3. Add Session + Smart continuation recommender
@Composable
fun AddSessionDialog(
    viewModel: MainViewModel,
    brandColor: Color,
    onDismiss: () -> Unit
) {
    var currentStep by remember { mutableStateOf(0) }

    val classes by viewModel.classes.collectAsStateWithLifecycle()
    val activeClasses = remember(classes) { classes.filter { !it.isArchived } }

    var selectedClassId by remember { mutableStateOf<Long?>(activeClasses.firstOrNull()?.id) }

    var dateTimeStr by remember { mutableStateOf("1405/03/10 16:30") }
    var topicTaught by remember { mutableStateOf("") }
    var homework by remember { mutableStateOf("") }
    var resources by remember { mutableStateOf("") }

    // Smart Continuation suggest topic!
    val smartSuggestTopic = remember(selectedClassId) {
        selectedClassId?.let { viewModel.getSmartContinuationTopic(it) } ?: ""
    }

    val isDark = isSystemInDarkTheme()
    val dialogBg = if (isDark) Color(0xFF151D2F) else Color.White
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
    val dialogBorder = if (isDark) Color(0xFF2E3E5B) else Color(0xFFE2E8F0)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .border(1.dp, dialogBorder, RoundedCornerShape(32.dp)),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = dialogBg)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("ثبت جلسه کلاسی جدید", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textPrimary)

                // Visual steps
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (i in 0..1) {
                        val active = i <= currentStep
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .background(if (active) brandColor else (if (isDark) Color(0xFF2E3E5B) else Color(0xFFE2E8F0)), RoundedCornerShape(2.dp))
                        )
                    }
                }
                Text(
                    text = "مرحله ${currentStep + 1} از ۲: " + when(currentStep) {
                        0 -> "کلاس و زمان برگزاری"
                        else -> "مبحث و تکالیف پیوسته"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = brandColor
                )

                Spacer(modifier = Modifier.height(4.dp))

                when (currentStep) {
                    0 -> {
                        Text("انتخاب کلاس:", fontSize = 11.sp, color = textSecondary, fontWeight = FontWeight.Bold)
                        if (activeClasses.isEmpty()) {
                            Text("کلاس فعالی یافت نشد. اول کلاسی ایجاد کنید.", fontSize = 11.sp, color = textSecondary)
                        } else {
                            activeClasses.forEach { cls ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedClassId = cls.id }
                                        .background(
                                            color = if (selectedClassId == cls.id) brandColor.copy(alpha = 0.1f) else Color.Transparent,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    RadioButton(
                                        selected = selectedClassId == cls.id,
                                        onClick = { selectedClassId = cls.id },
                                        colors = RadioButtonDefaults.colors(selectedColor = brandColor)
                                    )
                                    Text(cls.name, fontSize = 12.sp, color = textPrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        OutlinedTextField(
                            value = dateTimeStr,
                            onValueChange = { dateTimeStr = it },
                            label = { Text("تاریخ و ساعت برگزاری", color = textSecondary) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary,
                                focusedBorderColor = brandColor,
                                unfocusedBorderColor = dialogBorder
                            )
                        )
                    }
                    1 -> {
                        // Recommendation engine shown dynamically
                        if (smartSuggestTopic.isNotBlank()) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF2E2415) else Color(0xFFFEF3C7)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { topicTaught = smartSuggestTopic },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (isDark) Color(0xFF4C3A21) else Color(0xFFFDE68A))
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Recommend, contentDescription = null, tint = Color(0xFFD97706))
                                    Column {
                                        Text("پیشنهاد ادامه تدریس و تسلسل پیوسته:", fontSize = 10.sp, color = if (isDark) Color(0xFFFBBF24) else Color(0xFF92400E), fontWeight = FontWeight.Bold)
                                        Text(smartSuggestTopic, fontSize = 11.sp, color = textPrimary)
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = topicTaught,
                            onValueChange = { topicTaught = it },
                            label = { Text("مبحث تدریس‌شده", color = textSecondary) },
                            placeholder = { Text("مثال: تدریس دنباله هندسی و حل تجربی") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary,
                                focusedBorderColor = brandColor,
                                unfocusedBorderColor = dialogBorder
                            )
                        )

                        OutlinedTextField(
                            value = homework,
                            onValueChange = { homework = it },
                            label = { Text("تکالیف منزل", color = textSecondary) },
                            placeholder = { Text("مثال: تمرین‌های صفحه ۴۰ تا ۴۳ کتاب کار") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary,
                                focusedBorderColor = brandColor,
                                unfocusedBorderColor = dialogBorder
                            )
                        )

                        OutlinedTextField(
                            value = resources,
                            onValueChange = { resources = it },
                            label = { Text("منابع الکترونیک / فایل جزوات کلاسی", color = textSecondary) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary,
                                focusedBorderColor = brandColor,
                                unfocusedBorderColor = dialogBorder
                            )
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (currentStep > 0) {
                        Button(
                            onClick = { currentStep-- },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF243048) else Slate100),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("قبلی", color = textPrimary)
                        }
                    } else {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF243048) else Slate100),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("لغو", color = textPrimary)
                        }
                    }

                    if (currentStep < 1) {
                        Button(
                            onClick = {
                                if (selectedClassId != null) {
                                    currentStep++
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = brandColor),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            enabled = selectedClassId != null
                        ) {
                            Text("بعدی", color = Color.White)
                        }
                    } else {
                        Button(
                            onClick = {
                                selectedClassId?.let { classId ->
                                    viewModel.createSession(
                                        classId = classId,
                                        dateTimeStr = dateTimeStr,
                                        topicTaught = topicTaught,
                                        homework = homework,
                                        resources = resources,
                                        attendanceMap = emptyMap()
                                    )
                                    onDismiss()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = brandColor),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            enabled = selectedClassId != null
                        ) {
                            Text("ثبت نهایی", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// 4. Add Payment dialog
@Composable
fun AddPaymentDialog(
    viewModel: MainViewModel,
    brandColor: Color,
    onDismiss: () -> Unit
) {
    var currentStep by remember { mutableStateOf(0) }

    val classes by viewModel.classes.collectAsStateWithLifecycle()
    val students by viewModel.students.collectAsStateWithLifecycle()

    var selectedClassId by remember { mutableStateOf<Long?>(classes.firstOrNull()?.id) }
    var selectedStudentId by remember { mutableStateOf<Long?>(students.firstOrNull()?.id) }

    var amountPaid by remember { mutableStateOf("") }
    var amountDue by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("TRANSFER") } // CASH, CARD, TRANSFER
    var notes by remember { mutableStateOf("") }

    val methods = listOf(Pair("TRANSFER", "کارت به کارت"), Pair("CARD", "درگاه پوز"), Pair("CASH", "نقدی"))

    val isDark = isSystemInDarkTheme()
    val dialogBg = if (isDark) Color(0xFF151D2F) else Color.White
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
    val dialogBorder = if (isDark) Color(0xFF2E3E5B) else Color(0xFFE2E8F0)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .border(1.dp, dialogBorder, RoundedCornerShape(32.dp)),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = dialogBg)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("ثبت تراکنش و تراکنش مالی جدید", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textPrimary)

                // Visual steps
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (i in 0..1) {
                        val active = i <= currentStep
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .background(if (active) brandColor else (if (isDark) Color(0xFF2E3E5B) else Color(0xFFE2E8F0)), RoundedCornerShape(2.dp))
                        )
                    }
                }
                Text(
                    text = "مرحله ${currentStep + 1} از ۲: " + when(currentStep) {
                        0 -> "کلاس و انتخاب دانش‌آموز"
                        else -> "جزئیات فیش و روش پرداخت"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = brandColor
                )

                Spacer(modifier = Modifier.height(4.dp))

                when (currentStep) {
                    0 -> {
                        // Select Class
                        Text("کلاس مربوطه:", fontSize = 11.sp, color = textSecondary, fontWeight = FontWeight.Bold)
                        classes.forEach { cls ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedClassId = cls.id }
                                    .background(if (selectedClassId == cls.id) brandColor.copy(alpha = 0.1f) else Color.Transparent, RoundedCornerShape(10.dp))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RadioButton(
                                    selected = selectedClassId == cls.id,
                                    onClick = { selectedClassId = cls.id },
                                    colors = RadioButtonDefaults.colors(selectedColor = brandColor)
                                )
                                Text(cls.name, fontSize = 12.sp, color = textPrimary, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Select Student
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("پرداخت‌کننده (دانش‌آموز):", fontSize = 11.sp, color = textSecondary, fontWeight = FontWeight.Bold)
                        students.forEach { st ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedStudentId = st.id }
                                    .background(if (selectedStudentId == st.id) brandColor.copy(alpha = 0.1f) else Color.Transparent, RoundedCornerShape(10.dp))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RadioButton(
                                    selected = selectedStudentId == st.id,
                                    onClick = { selectedStudentId = st.id },
                                    colors = RadioButtonDefaults.colors(selectedColor = brandColor)
                                )
                                Text(st.name, fontSize = 12.sp, color = textPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    1 -> {
                        OutlinedTextField(
                            value = amountPaid,
                            onValueChange = { amountPaid = it },
                            label = { Text("مبلغ وصول‌شده (ریال)", color = textSecondary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary,
                                focusedBorderColor = brandColor,
                                unfocusedBorderColor = dialogBorder
                            )
                        )

                        OutlinedTextField(
                            value = amountDue,
                            onValueChange = { amountDue = it },
                            label = { Text("مبلغ بدهی باقیمانده (ریال)", color = textSecondary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary,
                                focusedBorderColor = brandColor,
                                unfocusedBorderColor = dialogBorder
                            )
                        )

                        // Methods Row
                        Text("روش تسویه مالی:", fontSize = 11.sp, color = textSecondary, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            methods.forEach { (m, label) ->
                                val selected = m == method
                                Box(
                                    modifier = Modifier
                                        .clickable { method = m }
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (selected) brandColor else (if (isDark) Color(0xFF1E283C) else Slate100))
                                        .weight(1f)
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) Color.White else textPrimary
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("کد رهگیری فیش / بابت قسط", color = textSecondary) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary,
                                focusedBorderColor = brandColor,
                                unfocusedBorderColor = dialogBorder
                            )
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (currentStep > 0) {
                        Button(
                            onClick = { currentStep-- },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF243048) else Slate100),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("قبلی", color = textPrimary)
                        }
                    } else {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF243048) else Slate100),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("لغو", color = textPrimary)
                        }
                    }

                    if (currentStep < 1) {
                        Button(
                            onClick = {
                                if (selectedClassId != null && selectedStudentId != null) {
                                    currentStep++
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = brandColor),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            enabled = selectedClassId != null && selectedStudentId != null
                        ) {
                            Text("بعدی", color = Color.White)
                        }
                    } else {
                        Button(
                            onClick = {
                                if (selectedClassId != null && selectedStudentId != null) {
                                    viewModel.createPayment(
                                        classId = selectedClassId!!,
                                        studentId = selectedStudentId!!,
                                        paid = amountPaid.toDoubleOrNull() ?: 0.0,
                                        due = amountDue.toDoubleOrNull() ?: 0.0,
                                        date = "1405/03/10",
                                        method = method,
                                        notes = notes
                                    )
                                    onDismiss()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = brandColor),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            enabled = selectedClassId != null && selectedStudentId != null
                        ) {
                            Text("ثبت نهایی", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// 5. Add ToDi Dialog
@Composable
fun AddToDoDialog(
    viewModel: MainViewModel,
    brandColor: Color,
    onDismiss: () -> Unit
) {
    var currentStep by remember { mutableStateOf(0) }

    var title by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("فردا") }
    var type by remember { mutableStateOf("PERSONAL") } // PERSONAL, PARENT_CONTACT, CLASS_PREP

    val types = listOf(Pair("PERSONAL", "شخصی"), Pair("PARENT_CONTACT", "پیگیری اولیا"), Pair("CLASS_PREP", "برنامه‌ریزی درس"))

    val isDark = isSystemInDarkTheme()
    val dialogBg = if (isDark) Color(0xFF151D2F) else Color.White
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
    val dialogBorder = if (isDark) Color(0xFF2E3E5B) else Color(0xFFE2E8F0)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, dialogBorder, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = dialogBg)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("ثبت یادداشت و پیگیری وظایف", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textPrimary)

                // Visual steps
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (i in 0..1) {
                        val active = i <= currentStep
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .background(if (active) brandColor else (if (isDark) Color(0xFF2E3E5B) else Color(0xFFE2E8F0)), RoundedCornerShape(2.dp))
                        )
                    }
                }
                Text(
                    text = "مرحله ${currentStep + 1} از ۲: " + when(currentStep) {
                        0 -> "جزئیات کار و مهلت"
                        else -> "دسته‌بندی و نشان یادداشت"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = brandColor
                )

                Spacer(modifier = Modifier.height(4.dp))

                when (currentStep) {
                    0 -> {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("عنوان کار یا یادداشت تماس", color = textSecondary) },
                            placeholder = { Text("مثال: زنگ زدن به پدر محمودی برای غیبت") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary,
                                focusedBorderColor = brandColor,
                                unfocusedBorderColor = dialogBorder
                            )
                        )

                        OutlinedTextField(
                            value = dueDate,
                            onValueChange = { dueDate = it },
                            label = { Text("مهلت انجام / زمان پیگیری", color = textSecondary) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary,
                                focusedBorderColor = brandColor,
                                unfocusedBorderColor = dialogBorder
                            )
                        )
                    }
                    1 -> {
                        Text("دسته‌بندی یادداشت:", fontSize = 11.sp, color = textSecondary, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            types.forEach { (t, label) ->
                                val selected = t == type
                                Box(
                                    modifier = Modifier
                                        .clickable { type = t }
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (selected) brandColor else (if (isDark) Color(0xFF1E283C) else Slate100))
                                        .weight(1f)
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) Color.White else textPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (currentStep > 0) {
                        Button(
                            onClick = { currentStep-- },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF243048) else Slate100),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("قبلی", color = textPrimary)
                        }
                    } else {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF243048) else Slate100),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("انصراف", color = textPrimary)
                        }
                    }

                    if (currentStep < 1) {
                        Button(
                            onClick = {
                                if (title.isNotBlank()) {
                                    currentStep++
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = brandColor),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            enabled = title.isNotBlank()
                        ) {
                            Text("بعدی", color = Color.White)
                        }
                    } else {
                        Button(
                            onClick = {
                                if (title.isNotBlank()) {
                                    viewModel.createToDoItem(title, dueDate, type)
                                    onDismiss()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = brandColor),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            enabled = title.isNotBlank()
                        ) {
                            Text("ثبت نهایی", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MonthCalendarDialog(
    initialYear: Int,
    initialMonth: Int,
    initialDay: Int,
    brandColor: Color,
    onDismiss: () -> Unit,
    onDateSelected: (Int, Int, Int) -> Unit
) {
    var viewYear by remember { mutableStateOf(initialYear) }
    var viewMonth by remember { mutableStateOf(initialMonth) }
    
    val monthLengths = listOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)
    val monthName = getPersianMonthName(viewMonth)
    
    // Day of the week for 1st day of the selected month
    val firstDayOfWeek = getDayOfWeek1405(viewMonth, 1) // 1=Sat, ..., 7=Fri
    val paddingCount = firstDayOfWeek - 1
    val monthLength = monthLengths[viewMonth - 1]
    
    val totalGridCells = paddingCount + monthLength
    
    val weekdaysText = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")
    
    val isDark = isSystemInDarkTheme()
    val dialogBg = if (isDark) Color(0xFF151D2F) else Color.White
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val borderCol = if (isDark) Color(0xFF2E3E5B) else Color(0xFFE2E8F0)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .border(1.dp, borderCol, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = dialogBg)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (viewMonth > 1) {
                                viewMonth--
                            } else {
                                viewMonth = 12
                                viewYear--
                            }
                        }
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "ماه قبل", tint = brandColor)
                    }
                    
                    Text(
                        text = "$monthName $viewYear",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = textPrimary
                    )
                    
                    IconButton(
                        onClick = {
                            if (viewMonth < 12) {
                                viewMonth++
                            } else {
                                viewMonth = 1
                                viewYear++
                            }
                        }
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "ماه بعد", tint = brandColor)
                    }
                }
                
                // Weekdays header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    weekdaysText.forEach { label ->
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = brandColor,
                            modifier = Modifier.width(32.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                
                // Grid of Days
                val cellsList = (1..totalGridCells).toList()
                val chunkedCells = cellsList.chunked(7)
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    chunkedCells.forEach { rowCells ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            rowCells.forEach { cellIndex ->
                                if (cellIndex <= paddingCount) {
                                    Box(modifier = Modifier.size(32.dp))
                                } else {
                                    val dayNum = cellIndex - paddingCount
                                    val isSelected = viewYear == initialYear && viewMonth == initialMonth && dayNum == initialDay
                                    
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) brandColor else Color.Transparent)
                                            .clickable {
                                                onDateSelected(viewYear, viewMonth, dayNum)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = dayNum.toString(),
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else textPrimary
                                        )
                                    }
                                }
                            }
                            
                            if (rowCells.size < 7) {
                                for (p in 0 until (7 - rowCells.size)) {
                                    Box(modifier = Modifier.size(32.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF243048) else Slate100),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("بستن", color = textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// 6. Class Detail Dialog
@Composable
fun ClassDetailDialog(
    classId: Long,
    viewModel: MainViewModel,
    brandColor: Color,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val classes by viewModel.classes.collectAsStateWithLifecycle()
    val sessions by viewModel.allSessions.collectAsStateWithLifecycle()
    val rawStudents by viewModel.students.collectAsStateWithLifecycle()

    val cls = classes.find { it.id == classId } ?: return
    val classSessions = sessions.filter { it.classId == classId }
    val classStudents = rawStudents // Direct or M:N check. Simplest is showing associated ones

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header of detail
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(cls.name, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Slate800)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "بستن")
                    }
                }

                Text(
                    text = "${cls.grade} • محل برگزاری: ${cls.location}",
                    fontSize = 12.sp,
                    color = Slate500
                )

                Divider(color = Slate100)

                // Ready Export format (خروجی آماده ارسال)
                val lastSession = classSessions.lastOrNull { it.status == "HELD" }
                val nextSessionNum = classSessions.size + 1
                val remainingSessions = (cls.totalSessions - (lastSession?.sessionNumber ?: 0)).coerceAtLeast(0)

                val exportText = """
                    📢 گزارش جلسه کلاسی:
                    کلاس: ${cls.name}
                    پایه: ${cls.grade}
                    شماره جلسه: ${lastSession?.sessionNumber ?: "اول"}
                    مبحث تدریس شده: ${lastSession?.topicTaught ?: "معرفی دوره و اهداف"}
                    تکلیف محول شده: ${lastSession?.homework ?: "مطالعه جزوه معرفی"}
                    جلسات باقیمانده: $remainingSessions از ${cls.totalSessions}
                    هزینه تسویه کاربری: ۴,۵۰۰,۰۰۰ ریال مابقی
                """.trimIndent()

                Card(
                    colors = CardDefaults.cardColors(containerColor = Slate100),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("خروجی آماده ارسال برای اولیا (کپی سریع):", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = brandColor)
                        Text(
                            exportText,
                            fontSize = 11.sp,
                            color = Slate800,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("ClassFlowReport", exportText)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "گزارش در حافظه کپی شد!", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = brandColor),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.CopyAll, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("کپی متن", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, exportText)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "ارسال به اولیا"))
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("اشتراک شبکه", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Text("تاریخچه کامل جلسات برگزار شده:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Slate800)
                if (classSessions.isEmpty()) {
                    Text("کلاسی در تاریخچه وجود ندارد.", fontSize = 11.sp, color = Slate500)
                } else {
                    classSessions.forEach { ses ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Slate100, RoundedCornerShape(12.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("جلسه ${ses.sessionNumber} • ${ses.dateTimeStr}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Slate800)
                                if (ses.topicTaught.isNotBlank()) {
                                    Text("موضوع: ${ses.topicTaught}", fontSize = 11.sp, color = Slate500)
                                }
                            }

                            Text(
                                "برگزار شد",
                                color = Color(0xFF0D9488),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        viewModel.toggleClassArchive(classId)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (cls.isArchived) brandColor else Color(0xFFE11D48)),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (cls.isArchived) "خروج از آرشیو کلاس" else "آرشیو کردن کلاس", color = Color.White)
                }
            }
        }
    }
}

// 7. Student Detail Dialog
@Composable
fun StudentDetailDialog(
    studentId: Long,
    viewModel: MainViewModel,
    brandColor: Color,
    onDismiss: () -> Unit
) {
    val students by viewModel.students.collectAsStateWithLifecycle()
    val payments by viewModel.allPayments.collectAsStateWithLifecycle()
    val attendanceList by viewModel.allAttendance.collectAsStateWithLifecycle()

    val student = students.find { it.id == studentId } ?: return
    val studentPayments = payments.filter { it.studentId == studentId }

    val isDark = isSystemInDarkTheme()
    val dialogBg = if (isDark) Color(0xFF151D2F) else Color.White
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
    val dividerColor = if (isDark) Color(0xFF2E3E5B) else Slate100

    val studentAttendance = remember(attendanceList, studentId) {
        attendanceList.filter { it.studentId == studentId }
    }
    val totalSessions = studentAttendance.size
    val presentSessions = studentAttendance.count { it.status == "PRESENT" || it.status == "DELAYED" }
    val attendanceRatio = remember(totalSessions, presentSessions) {
        if (totalSessions > 0) (presentSessions.toFloat() / totalSessions.toFloat()) else 1.0f
    }

    val totalPaid = remember(studentPayments) { studentPayments.sumOf { it.amountPaid } }
    val totalDue = remember(studentPayments) { studentPayments.sumOf { it.amountDue } }
    val financialRatio = remember(totalPaid, totalDue) {
        val sum = totalPaid + totalDue
        if (sum > 0) (totalPaid.toFloat() / sum.toFloat()) else 1f
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = dialogBg)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(student.name, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = textPrimary)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "بستن", tint = textSecondary)
                    }
                }

                Text("تلفن همراه: ${student.phone}", fontSize = 12.sp, color = textPrimary)
                if (student.parentPhone.isNotBlank()) {
                    Text("شماره تماس ولی: ${student.parentPhone}", fontSize = 12.sp, color = textPrimary)
                }

                if (student.notes.isNotBlank()) {
                    Text("خلاصه پرونده دانش‌آموز: ${student.notes}", fontSize = 12.sp, color = textSecondary)
                }

                Divider(color = dividerColor)

                // Visual Representation Charts
                Text("گزارش پیشرفت و امور مالی دانش‌آموز:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = textPrimary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Attendance Chart
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E283C) else brandColor.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF2E3E5B) else brandColor.copy(alpha = 0.15f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("شاخص حضور", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                            Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    progress = attendanceRatio,
                                    modifier = Modifier.fillMaxSize(),
                                    strokeWidth = 6.dp,
                                    color = Color(0xFF0D9488),
                                    trackColor = if (isDark) Color(0xFF2E3E5B) else Color(0xFFE2E8F0)
                                )
                                Text("${(attendanceRatio * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = textPrimary)
                            }
                            Text("حضور: $presentSessions از $totalSessions جلسه", fontSize = 9.sp, color = textSecondary)
                        }
                    }

                    // Financial Chart
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E283C) else brandColor.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF2E3E5B) else brandColor.copy(alpha = 0.15f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("تسویه شهریه", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                            Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    progress = financialRatio,
                                    modifier = Modifier.fillMaxSize(),
                                    strokeWidth = 6.dp,
                                    color = brandColor,
                                    trackColor = if (isDark) Color(0xFF2E3E5B) else Color(0xFFE2E8F0)
                                )
                                Text("${(financialRatio * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = textPrimary)
                            }
                            Text("بدهی: ${formatPrice(totalDue)} ریال", fontSize = 9.sp, color = textSecondary)
                        }
                    }
                }

                Divider(color = dividerColor)

                Text("تاریخچه پرداخت‌های دانش‌آموز:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = textPrimary)
                if (studentPayments.isEmpty()) {
                    Text("تراکنش مالی ثبت نشده است.", fontSize = 12.sp, color = textSecondary)
                } else {
                    studentPayments.forEach { pay ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isDark) Color(0xFF1E283C) else Slate100, RoundedCornerShape(12.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("مبلغ تسویه: ${formatPrice(pay.amountPaid)} ریال", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textPrimary)
                                Text("موعد بدهکاری: ${formatPrice(pay.amountDue)} ریال", fontSize = 11.sp, color = textSecondary)
                            }
                            Text(pay.date, fontSize = 11.sp, color = textSecondary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        viewModel.deleteStudent(student)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("حذف پرونده دانش‌آموز", color = Color.White)
                }
            }
        }
    }
}

// 8. User Profile Settings Dialog
@Composable
fun UserProfileSettingsDialog(
    viewModel: MainViewModel,
    brandColor: Color,
    onDismiss: () -> Unit
) {
    var teacher by remember { mutableStateOf("") }
    var school by remember { mutableStateOf("") }
    var selectHex by remember { mutableStateOf("") }

    val rawTeacher by viewModel.teacherName.collectAsStateWithLifecycle()
    val rawSchool by viewModel.schoolName.collectAsStateWithLifecycle()
    val rawHex by viewModel.themeColor.collectAsStateWithLifecycle()
    val activeFont by viewModel.selectedFont.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        teacher = rawTeacher
        school = rawSchool
        selectHex = rawHex
    }

    val isDark = isSystemInDarkTheme()
    val dialogBg = if (isDark) Color(0xFF151D2F) else Color(0xFFFFFFFF)
    val textPrimary = if (isDark) Color.White else Color(0xFF1E293B)
    val textSecondary = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
    val textTertiary = if (isDark) Color(0xFF94A3B8) else Color(0xFF8294AD)
    val dialogBorder = if (isDark) Color(0xFF2E3E5B) else brandColor.copy(alpha = 0.10f)

    val palette = listOf("#4F46E5", "#0D9488", "#7C3AED", "#D97706", "#E11D48")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, dialogBorder, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = dialogBg)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("تنظیمات و نمایه کاربری", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textPrimary)

                OutlinedTextField(
                    value = teacher,
                    onValueChange = { teacher = it },
                    label = { Text("نام مدرس") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary,
                        focusedLabelColor = brandColor,
                        unfocusedLabelColor = textSecondary,
                        focusedBorderColor = brandColor,
                        unfocusedBorderColor = dialogBorder
                    )
                )

                OutlinedTextField(
                    value = school,
                    onValueChange = { school = it },
                    label = { Text("نام آموزشگاه اصلی") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary,
                        focusedLabelColor = brandColor,
                        unfocusedLabelColor = textSecondary,
                        focusedBorderColor = brandColor,
                        unfocusedBorderColor = dialogBorder
                    )
                )

                // Select Font Segment
                Text("قلم فارسی برنامه:", fontSize = 11.sp, color = textSecondary, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val isEstedad = activeFont == "estedad"
                    val isShabnam = activeFont == "shabnam"
                    
                    Button(
                        onClick = { viewModel.updateFont("estedad") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isEstedad) brandColor else (if (isDark) Color(0xFF243048) else Color(0xFFF1F5F9)),
                            contentColor = if (isEstedad) Color.White else textPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("قلم استعداد", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { viewModel.updateFont("shabnam") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isShabnam) brandColor else (if (isDark) Color(0xFF243048) else Color(0xFFF1F5F9)),
                            contentColor = if (isShabnam) Color.White else textPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("قلم شبنم", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Text("پوسته پیش‌فرض:", fontSize = 11.sp, color = textSecondary, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    palette.forEach { c ->
                        val col = Color(android.graphics.Color.parseColor(c))
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(col)
                                .border(
                                    width = if (selectHex == c) 3.dp else 0.dp,
                                    color = textPrimary,
                                    shape = CircleShape
                                )
                                .clickable { selectHex = c }
                        )
                    }
                }

                // About Us view with Mostafa Modaberi listed as creator
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E283C) else Color(0xFFF8FAFC))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "کلاب‌پست و مدیریت هوشمند کلاس فلو",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("طراح و توسعه‌دهنده:", fontSize = 11.sp, color = textSecondary)
                            Text("مصطفی مدبری", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = brandColor)
                        }
                        Text(
                            text = "تسهیل تدریس هدفمند و نظارت پیشرفته کلاسی",
                            fontSize = 10.sp,
                            color = textTertiary
                        )
                    }
                }

                // Reset Application Option
                var showResetConfirm by remember { mutableStateOf(false) }
                if (!showResetConfirm) {
                    Button(
                        onClick = { showResetConfirm = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48).copy(alpha = 0.1f)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("پاک‌سازی عمومی و ریست کامل برنامه", color = Color(0xFFE11D48), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFE11D48).copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("مطمئنید؟ تمامی اطلاعات کاملاً پاک خواهند شد:", fontSize = 9.sp, color = Color(0xFFE11D48), fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(onClick = { showResetConfirm = false }) {
                                Text("لغو", fontSize = 10.sp, color = textPrimary)
                            }
                            Button(
                                onClick = {
                                    viewModel.resetApp()
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Text("بله، پاک کن", fontSize = 10.sp, color = Color.White)
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF243048) else Color(0xFFF1F5F9)),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("بستن", color = textPrimary)
                    }

                    Button(
                        onClick = {
                            if (teacher.isNotBlank()) {
                                viewModel.updateProfile(teacher, school, selectHex)
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = brandColor),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("ذخیره", color = Color.White)
                    }
                }
            }
        }
    }
}
