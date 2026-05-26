package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Wallpaper
import com.example.data.WallpaperRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class WallpaperViewModel(private val repository: WallpaperRepository) : ViewModel() {

    private val _selectedCategory = MutableStateFlow("Barchasi")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    val categories = listOf("Barchasi", "Space", "Nature", "Minimalist", "Abstract", "Anime")

    // Seed default wallpapers if empty on startup
    init {
        viewModelScope.launch {
            repository.seedDefaultWallpapersIfEmpty()
        }
    }

    // All wallpapers from DB
    val wallpapersState: StateFlow<List<Wallpaper>> = repository.allWallpapers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val favoritesState: StateFlow<List<Wallpaper>> = repository.favorites
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Filtered wallpapers based on category & search query
    val filteredWallpapers: StateFlow<List<Wallpaper>> = combine(
        wallpapersState,
        _selectedCategory,
        _searchQuery
    ) { list, category, query ->
        var result = list
        if (category != "Barchasi") {
            result = result.filter { it.category.equals(category, ignoreCase = true) }
        }
        if (query.isNotBlank()) {
            result = result.filter { 
                it.title.contains(query, ignoreCase = true) || 
                it.category.contains(query, ignoreCase = true) ||
                (query.equals("Kosmos", ignoreCase = true) && it.category.equals("Space", ignoreCase = true)) ||
                (query.equals("Tog'", ignoreCase = true) && it.title.contains("Tog'", ignoreCase = true)) ||
                (query.equals("Neon", ignoreCase = true) && it.title.contains("Neon", ignoreCase = true))
            }
        }
        result
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleFavorite(id: Int) {
        viewModelScope.launch {
            repository.toggleFavorite(id)
        }
    }

    fun addWallpaper(title: String, url: String, category: String) {
        viewModelScope.launch {
            val name = if (title.trim().isEmpty()) "Fonsiz rasm" else title
            val cleanCategory = if (category in categories) category else "Nature"
            val newWallpaper = Wallpaper(
                title = name,
                url = url,
                category = cleanCategory
            )
            repository.insertWallpaper(newWallpaper)
        }
    }

    fun deleteWallpaper(wallpaper: Wallpaper) {
        viewModelScope.launch {
            repository.deleteWallpaper(wallpaper)
        }
    }

    fun authenticateAdmin(password: String): Boolean {
        // Secure custom password
        val success = password == "WallBoxPro2026"
        if (success) {
            _isAdmin.value = true
        }
        return success
    }

    fun logoutAdmin() {
        _isAdmin.value = false
    }
}

class WallpaperViewModelFactory(private val repository: WallpaperRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WallpaperViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WallpaperViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
