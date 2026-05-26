package com.example

import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.Wallpaper
import com.example.data.WallpaperDatabase
import com.example.data.WallpaperRepository
import com.example.ui.WallpaperViewModel
import com.example.ui.WallpaperViewModelFactory
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricTeal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = WallpaperDatabase.getDatabase(this)
        val repository = WallpaperRepository(database.wallpaperDao())
        val factory = WallpaperViewModelFactory(repository)
        val viewModel = ViewModelProvider(this, factory)[WallpaperViewModel::class.java]

        setContent {
            MyApplicationTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}

enum class WallpaperType {
    HOME, LOCK, BOTH
}

enum class AppTab {
    GALLERY, FAVORITES, ADMIN
}

enum class SimMode {
    NONE, LOCK, HOME
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: WallpaperViewModel) {
    val currentTab = remember { mutableStateOf(AppTab.GALLERY) }
    val selectedWallpaper = remember { mutableStateOf<Wallpaper?>(null) }
    val context = LocalContext.current

    val wallpapers by viewModel.filteredWallpapers.collectAsStateWithLifecycle()
    val favorites by viewModel.favoritesState.collectAsStateWithLifecycle()
    val allWallpapersCount by viewModel.wallpapersState.collectAsStateWithLifecycle()
    val activeCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val queryText by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Discovery",
                            fontWeight = FontWeight.Light,
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 24.sp,
                            letterSpacing = 1.5.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = if (isAdmin) "Muallif Boshqaruvi" else "WallBox Premium",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp,
                            color = if (isAdmin) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    if (isAdmin) {
                        IconButton(onClick = { viewModel.logoutAdmin() }) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "Muallif chiqishi",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    } else {
                        IconButton(onClick = {
                            Toast.makeText(context, "Muallif rejimi uchun pastdagi 'Muallif' bo'limiga kiring", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Wallbox logotip",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    Box(modifier = Modifier.padding(end = 12.dp)) {
                        Text(
                            text = "${allWallpapersCount.size} dona",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF1C1B1F),
                tonalElevation = 0.dp,
                windowInsets = WindowInsets.navigationBars
            ) {
                NavigationBarItem(
                    selected = currentTab.value == AppTab.GALLERY,
                    onClick = { currentTab.value = AppTab.GALLERY },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFE8DEF8),
                        unselectedIconColor = Color(0xFFC9C5D0),
                        selectedTextColor = Color(0xFFE8DEF8),
                        unselectedTextColor = Color(0xFFC9C5D0),
                        indicatorColor = Color(0xFF4A4458)
                    ),
                    icon = { Icon(if (currentTab.value == AppTab.GALLERY) Icons.Filled.PhotoLibrary else Icons.Outlined.PhotoLibrary, contentDescription = "Galereya") },
                    label = { Text("Galereya", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                    modifier = Modifier.testTag("tab_gallery")
                )
                NavigationBarItem(
                    selected = currentTab.value == AppTab.FAVORITES,
                    onClick = { currentTab.value = AppTab.FAVORITES },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFE8DEF8),
                        unselectedIconColor = Color(0xFFC9C5D0),
                        selectedTextColor = Color(0xFFE8DEF8),
                        unselectedTextColor = Color(0xFFC9C5D0),
                        indicatorColor = Color(0xFF4A4458)
                    ),
                    icon = { Icon(if (currentTab.value == AppTab.FAVORITES) Icons.Filled.Favorite else Icons.Outlined.Favorite, contentDescription = "Yoqtirilganlar") },
                    label = { Text("Sevimlilar", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                    modifier = Modifier.testTag("tab_favorites")
                )
                NavigationBarItem(
                    selected = currentTab.value == AppTab.ADMIN,
                    onClick = { currentTab.value = AppTab.ADMIN },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFE8DEF8),
                        unselectedIconColor = Color(0xFFC9C5D0),
                        selectedTextColor = Color(0xFFE8DEF8),
                        unselectedTextColor = Color(0xFFC9C5D0),
                        indicatorColor = Color(0xFF4A4458)
                    ),
                    icon = { Icon(if (currentTab.value == AppTab.ADMIN) Icons.Filled.AdminPanelSettings else Icons.Outlined.AdminPanelSettings, contentDescription = "Muallif panel") },
                    label = { Text("Muallif", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                    modifier = Modifier.testTag("tab_admin")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (currentTab.value) {
                AppTab.GALLERY -> {
                    SearchBarWidget(
                        query = queryText,
                        onQueryChange = { viewModel.setSearchQuery(it) }
                    )

                    QuickSearchSuggestions(
                        selectedKeyword = queryText,
                        onKeywordSelect = { viewModel.setSearchQuery(it) }
                    )

                    CategoryChips(
                        categories = viewModel.categories,
                        activeCategory = activeCategory,
                        onCategorySelect = { viewModel.selectCategory(it) }
                    )

                    if (wallpapers.isEmpty()) {
                        EmptyState(
                            title = "Rasmlar topilmadi",
                            msg = "Boshqa so'z yozishni yoki boshqa ruknlarni tanlashni sinab ko'ring."
                        )
                    } else {
                        WallpaperGrid(
                            list = wallpapers,
                            onWallpaperClick = { selectedWallpaper.value = it },
                            onFavoriteClick = { viewModel.toggleFavorite(it.id) }
                        )
                    }
                }

                AppTab.FAVORITES -> {
                    if (favorites.isEmpty()) {
                        EmptyState(
                            title = "Sevimli rasmlaringiz yo'q",
                            msg = "O'zingizga yoqqan rasmlardagi yurak belgisini bosib, ularni bu yerda tezkor ko'ra olasiz."
                        )
                    } else {
                        WallpaperGrid(
                            list = favorites,
                            onWallpaperClick = { selectedWallpaper.value = it },
                            onFavoriteClick = { viewModel.toggleFavorite(it.id) }
                        )
                    }
                }

                AppTab.ADMIN -> {
                    if (isAdmin) {
                        AdminPanelScreen(
                            viewModel = viewModel,
                            wallpapers = allWallpapersCount
                        )
                    } else {
                        AdminLoginScreen(
                            onSubmit = { pwd ->
                                val ok = viewModel.authenticateAdmin(pwd)
                                if (ok) {
                                    Toast.makeText(context, "Xush kelibsiz, Muallif!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Parol noto'g'ri!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    selectedWallpaper.value?.let { wallpaper ->
        WallpaperDetailDialog(
            wallpaper = wallpaper,
            onDismiss = { selectedWallpaper.value = null },
            onFavoriteToggle = { viewModel.toggleFavorite(wallpaper.id) }
        )
    }
}

@Composable
fun SearchBarWidget(query: String, onQueryChange: (String) -> Unit) {
    val keyboardController = LocalSoftwareKeyboardController.current

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Fondan qidirish...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
        singleLine = true,
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Qidiruv", tint = MaterialTheme.colorScheme.primary) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Tozalash", tint = MaterialTheme.colorScheme.error)
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
        shape = RoundedCornerShape(24.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = Color.Transparent,
            focusedContainerColor = Color(0xFF252529),
            unfocusedContainerColor = Color(0xFF252529),
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("search_input")
    )
}

fun getCategoryDisplayName(category: String): String {
    return when (category) {
        "Barchasi" -> "Barchasi"
        "Space" -> "Kosmos"
        "Nature" -> "Tabiat"
        "Minimalist" -> "Minimal"
        "Abstract" -> "Abstrakt"
        "Anime" -> "Anime"
        else -> category
    }
}

@Composable
fun QuickSearchSuggestions(
    selectedKeyword: String,
    onKeywordSelect: (String) -> Unit
) {
    val keywords = listOf("Kosmos", "Tog'", "Neon")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = "Namunaviy qidiruvlar:",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            keywords.forEach { keyword ->
                val isActive = selectedKeyword.equals(keyword, ignoreCase = true)
                Surface(
                    onClick = {
                        if (isActive) {
                            onKeywordSelect("")
                        } else {
                            onKeywordSelect(keyword)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isActive) MaterialTheme.colorScheme.primary else Color(0xFF252529),
                    contentColor = if (isActive) MaterialTheme.colorScheme.onPrimary else Color(0xFFC9C5D0),
                    modifier = Modifier.height(28.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Text(
                            text = "#$keyword",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryChips(
    categories: List<String>,
    activeCategory: String,
    onCategorySelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { category ->
            val isSelected = category == activeCategory

            Surface(
                onClick = { onCategorySelect(category) },
                shape = RoundedCornerShape(16.dp), // MD3 high-fidelity rounded-2xl
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF252529),
                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color(0xFFE6E1E5),
                modifier = Modifier
                    .height(38.dp)
                    .testTag("category_pill_$category")
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = getCategoryDisplayName(category),
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun WallpaperGrid(
    list: List<Wallpaper>,
    onWallpaperClick: (Wallpaper) -> Unit,
    onFavoriteClick: (Wallpaper) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(list, key = { it.id }) { item ->
            WallpaperCard(
                wallpaper = item,
                onClick = { onWallpaperClick(item) },
                onFavoriteToggle = { onFavoriteClick(item) }
            )
        }
    }
}

@Composable
fun WallpaperCard(
    wallpaper: Wallpaper,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF252529)),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.7f)
            .clickable(onClick = onClick)
            .testTag("wallpaper_item_${wallpaper.id}")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = wallpaper.url,
                contentDescription = wallpaper.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .align(Alignment.TopStart)
            ) {
                Text(
                    text = wallpaper.category,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                            startY = 0f
                        )
                    )
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = wallpaper.title,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                            .clickable { onFavoriteToggle() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (wallpaper.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Yurak",
                            tint = if (wallpaper.isFavorite) MaterialTheme.colorScheme.tertiary else Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyState(title: String, msg: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.PhotoFilter,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = msg,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun AdminLoginScreen(onSubmit: (String) -> Unit) {
    var password by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.LockOpen,
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Muallif Kirishi",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Ushbu bo'lim faqat ilova egasi uchun mo'ljallangan. Foydalanuvchilar rasmlarni galereyadan yuklab oladilar. Muallif bo'lsangiz, maxsus parolni kiriting (Standart parol: admin yoki 1234)",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Maxfiy parol") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        keyboardController?.hide()
                        onSubmit(password)
                    }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_password_input")
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        keyboardController?.hide()
                        onSubmit(password)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("login_submit_button")
                ) {
                    Text("Tizimga kirish", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AdminPanelScreen(
    viewModel: WallpaperViewModel,
    wallpapers: List<Wallpaper>
) {
    var titleText by remember { mutableStateOf("") }
    var urlText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Nature") }

    val context = LocalContext.current
    val categoriesForAdding = viewModel.categories.filter { it != "Barchasi" }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            urlText = uri.toString()
            Toast.makeText(context, "Telefoningizdan rasm tanlandi!", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Yangi Oboy Qo'shish",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        tint = MaterialTheme.colorScheme.primary,
                        contentDescription = null
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    label = { Text("Fon Sarlavhasi (Masalan: Starry Space)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_title_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Bo'limni tanlang:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 8.dp)
                ) {
                    categoriesForAdding.forEach { categoryItem ->
                        val itemSelected = selectedCategory == categoryItem
                        Surface(
                            onClick = { selectedCategory = categoryItem },
                            shape = RoundedCornerShape(12.dp),
                            color = if (itemSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            contentColor = if (itemSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 14.dp)) {
                                Text(categoryItem, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = urlText,
                    onValueChange = { urlText = it },
                    label = { Text("Rasm Manzili (URL yoki yuklangan)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_url_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Yoki telefoningizdan tanlang:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("admin_gallery_picker")
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tanlash", fontSize = 11.sp)
                    }
                }

                if (urlText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Rasm holati: Yuklashga tayyor",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (urlText.trim().isEmpty()) {
                            Toast.makeText(context, "Rasm manzili kiritilishi yoki fayl yuklanishi shart!", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.addWallpaper(titleText, urlText, selectedCategory)
                            Toast.makeText(context, "Yangi fon muvaffaqiyatli qo'shildi!", Toast.LENGTH_SHORT).show()
                            titleText = ""
                            urlText = ""
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("admin_add_submit_button")
                ) {
                    Text("Gallereyaga Qo'shish", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Mavjud rasmlar listi (${wallpapers.size} dona)",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        wallpapers.forEach { wp ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        AsyncImage(
                            model = wp.url,
                            contentDescription = wp.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = wp.title,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Bo'lim: ${wp.category}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            viewModel.deleteWallpaper(wp)
                            Toast.makeText(context, "O'chirildi: ${wp.title}", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Rasm o'chirish",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperDetailDialog(
    wallpaper: Wallpaper,
    onDismiss: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var simMode by remember { mutableStateOf(SimMode.NONE) }
    var showSetDialog by remember { mutableStateOf(false) }
    var isOperating by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AsyncImage(
                model = wallpaper.url,
                contentDescription = wallpaper.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(if (simMode == SimMode.HOME) 12.dp else 0.dp)
            )

            when (simMode) {
                SimMode.LOCK -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.15f))
                            .statusBarsPadding()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "08:49",
                                fontSize = 72.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Seshanba, 26-may",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(start = 48.dp, end = 48.dp, bottom = 120.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color.White)
                            }
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White)
                            }
                        }
                    }
                }

                SimMode.HOME -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                    ) {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.85f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 24.dp, end = 24.dp, top = 60.dp)
                                .height(48.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Google bo'ylab qidirish...", color = Color.Gray, fontSize = 13.sp)
                            }
                        }

                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .padding(start = 24.dp, end = 24.dp, top = 140.dp)
                                .size(160.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("Seshanba", color = ElectricCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("26 may", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Havo ochiq • 23°C", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(bottom = 120.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                SimAppIcon(icon = Icons.Default.Phone, label = "Telefon", color = Color(0xFF4CAF50))
                                SimAppIcon(icon = Icons.Default.Send, label = "Telegram", color = Color(0xFF2196F3))
                                SimAppIcon(icon = Icons.Default.Public, label = "Internet", color = Color(0xFF9C27B0))
                                SimAppIcon(icon = Icons.Default.Image, label = "Gallereya", color = ElectricCyan)
                            }
                        }
                    }
                }

                SimMode.NONE -> {}
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent),
                            startY = 0f
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.4f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Orqaga",
                            tint = Color.White
                        )
                    }

                    Row(
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val activeColor = MaterialTheme.colorScheme.primary
                        TextButton(
                            onClick = { simMode = SimMode.NONE },
                            colors = ButtonDefaults.textButtonColors(
                                containerColor = if (simMode == SimMode.NONE) activeColor else Color.Transparent,
                                contentColor = if (simMode == SimMode.NONE) Color.Black else Color.White
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Asli", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        TextButton(
                            onClick = { simMode = SimMode.LOCK },
                            colors = ButtonDefaults.textButtonColors(
                                containerColor = if (simMode == SimMode.LOCK) activeColor else Color.Transparent,
                                contentColor = if (simMode == SimMode.LOCK) Color.Black else Color.White
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Qulflash", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        TextButton(
                            onClick = { simMode = SimMode.HOME },
                            colors = ButtonDefaults.textButtonColors(
                                containerColor = if (simMode == SimMode.HOME) activeColor else Color.Transparent,
                                contentColor = if (simMode == SimMode.HOME) Color.Black else Color.White
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Uy ekrani", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                            startY = 0f
                        )
                    )
                    .navigationBarsPadding()
                    .padding(start = 24.dp, end = 24.dp, bottom = 24.dp, top = 40.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = wallpaper.title,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                            Text(
                                text = "Kategoriya • ${wallpaper.category}",
                                color = Color.White.copy(alpha = 0.65f),
                                fontSize = 13.sp
                            )
                        }

                        IconButton(
                            onClick = onFavoriteToggle,
                            modifier = Modifier.background(Color.White.copy(alpha = 0.15f), CircleShape)
                        ) {
                            Icon(
                                imageVector = if (wallpaper.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Sevimlilarga qo'shish",
                                tint = if (wallpaper.isFavorite) MaterialTheme.colorScheme.tertiary else Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                isOperating = true
                                scope.launch {
                                    val success = downloadWallpaperToGallery(context, wallpaper.url, wallpaper.title)
                                    isOperating = false
                                    if (success) {
                                        Toast.makeText(context, "Telefonga muvaffaqiyatli yuklab olindi (Galereyaga saqlandi)!", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "Yuklab olishda xatolik yuz berdi!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("download_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Yuklab olish", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Button(
                            onClick = { showSetDialog = true },
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("set_wallpaper_button")
                        ) {
                            Icon(Icons.Default.Wallpaper, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("O'rnatish", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                        }
                    }
                }
            }

            if (isOperating) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(24.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "Bajarilmoqda...",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSetDialog) {
        AlertDialog(
            onDismissRequest = { showSetDialog = false },
            title = { Text("Fonda o'rnatish", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = { Text("Ushbu rasmning qayerda fon rasmi bo'lishini xohlaysiz?") },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            showSetDialog = false
                            isOperating = true
                            scope.launch {
                                val ok = setWallpaperOnDevice(context, wallpaper.url, WallpaperType.HOME)
                                isOperating = false
                                if (ok) {
                                    Toast.makeText(context, "Asosiy ekranga o'rnatildi!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Fonda o'rnatib bo'lmadi!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Asosiy ekran (Home Screen)")
                    }

                    Button(
                        onClick = {
                            showSetDialog = false
                            isOperating = true
                            scope.launch {
                                val ok = setWallpaperOnDevice(context, wallpaper.url, WallpaperType.LOCK)
                                isOperating = false
                                if (ok) {
                                    Toast.makeText(context, "Qulflash ekraniga o'rnatildi!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Fonda o'rnatib bo'lmadi!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Qulflash ekrani (Lock Screen)")
                    }

                    Button(
                        onClick = {
                            showSetDialog = false
                            isOperating = true
                            scope.launch {
                                val ok = setWallpaperOnDevice(context, wallpaper.url, WallpaperType.BOTH)
                                isOperating = false
                                if (ok) {
                                    Toast.makeText(context, "Ikkalasiga ham o'rnatildi!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Fonda o'rnatib bo'lmadi!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Ikkalasiga ham")
                    }

                    TextButton(
                        onClick = { showSetDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Bekor qilish", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        )
    }
}

@Composable
fun SimAppIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(color, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Normal)
    }
}

suspend fun setWallpaperOnDevice(context: Context, imageUrl: String, type: WallpaperType): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            
            val bitmap = if (imageUrl.startsWith("content://") || imageUrl.startsWith("file://")) {
                context.contentResolver.openInputStream(Uri.parse(imageUrl))?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)
                }
            } else {
                val url = URL(imageUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.doInput = true
                connection.connect()
                connection.inputStream.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)
                }
            }

            if (bitmap != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val flag = when (type) {
                        WallpaperType.HOME -> WallpaperManager.FLAG_SYSTEM
                        WallpaperType.LOCK -> WallpaperManager.FLAG_LOCK
                        WallpaperType.BOTH -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                    }
                    wallpaperManager.setBitmap(bitmap, null, true, flag)
                } else {
                    wallpaperManager.setBitmap(bitmap)
                }
                true
            } else {
                false
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    }
}

suspend fun downloadWallpaperToGallery(context: Context, imageUrl: String, title: String): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val bitmap = if (imageUrl.startsWith("content://") || imageUrl.startsWith("file://")) {
                context.contentResolver.openInputStream(Uri.parse(imageUrl))?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)
                }
            } else {
                val url = URL(imageUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.doInput = true
                connection.connect()
                connection.inputStream.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)
                }
            } ?: return@withContext false

            val filename = "WallBox_${title.replace("\\s+".toRegex(), "_").filter { it.isLetterOrDigit() || it == '_' }.ifEmpty { "wallpaper" }}_${System.currentTimeMillis()}.jpg"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/WallBox")
                }
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                    }
                    true
                } else {
                    false
                }
            } else {
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + "/WallBox"
                val dir = File(imagesDir)
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, filename)
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                }
                MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), null, null)
                true
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    }
}
