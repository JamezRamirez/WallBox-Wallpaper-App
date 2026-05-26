package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class WallpaperRepository(private val wallpaperDao: WallpaperDao) {

    val allWallpapers: Flow<List<Wallpaper>> = wallpaperDao.getAllWallpapers()
    val favorites: Flow<List<Wallpaper>> = wallpaperDao.getFavorites()

    fun getWallpapersByCategory(category: String): Flow<List<Wallpaper>> {
        return wallpaperDao.getWallpapersByCategory(category)
    }

    suspend fun getWallpaperById(id: Int): Wallpaper? {
        return wallpaperDao.getWallpaperById(id)
    }

    suspend fun toggleFavorite(id: Int) {
        val wallpaper = wallpaperDao.getWallpaperById(id)
        if (wallpaper != null) {
            val updated = wallpaper.copy(isFavorite = !wallpaper.isFavorite)
            wallpaperDao.updateWallpaper(updated)
        }
    }

    suspend fun insertWallpaper(wallpaper: Wallpaper) {
        wallpaperDao.insertWallpaper(wallpaper)
    }

    suspend fun deleteWallpaper(wallpaper: Wallpaper) {
        wallpaperDao.deleteWallpaper(wallpaper)
    }

    suspend fun seedDefaultWallpapersIfEmpty() {
        val count = wallpaperDao.getWallpaperCount()
        if (count < 20) {
            // Clear old sample wallpapers to ensure we refresh with 5 premium wallpapers per category
            wallpaperDao.deleteAllWallpapers()
            val defaults = listOf(
                // === SPACE ===
                Wallpaper(
                    url = "https://images.unsplash.com/photo-1506318137071-a8e063b4bec0?auto=format&fit=crop&q=80&w=1080",
                    title = "Deep Cosmos (Kosmos)",
                    category = "Space"
                ),
                Wallpaper(
                    url = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?auto=format&fit=crop&q=80&w=1080",
                    title = "Galaxy Glow (Galaktika)",
                    category = "Space"
                ),
                Wallpaper(
                    url = "https://images.unsplash.com/photo-1444703686981-a3abbc4d4fe3?auto=format&fit=crop&q=80&w=1080",
                    title = "Starry Sky (Yulduzli Osmon)",
                    category = "Space"
                ),
                Wallpaper(
                    url = "https://images.unsplash.com/photo-1462331940025-496dfbfc7564?auto=format&fit=crop&q=80&w=1080",
                    title = "Supernova Blast (Kosmik Portlash)",
                    category = "Space"
                ),
                Wallpaper(
                    url = "https://images.unsplash.com/photo-1538370965046-79c0d6907d47?auto=format&fit=crop&q=80&w=1080",
                    title = "Orion Nebula (Orion Buluti)",
                    category = "Space"
                ),

                // === NATURE ===
                Wallpaper(
                    url = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?auto=format&fit=crop&q=80&w=1080",
                    title = "Silent Mountain (Sokin Tog')",
                    category = "Nature"
                ),
                Wallpaper(
                    url = "https://images.unsplash.com/photo-1501854140801-50d01698950b?auto=format&fit=crop&q=80&w=1080",
                    title = "Forest Rays (O'rmon Nurlari)",
                    category = "Nature"
                ),
                Wallpaper(
                    url = "https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?auto=format&fit=crop&q=80&w=1080",
                    title = "Morning Fog (Ertongi Tuman)",
                    category = "Nature"
                ),
                Wallpaper(
                    url = "https://images.unsplash.com/photo-1454496522488-7a8e488e8606?auto=format&fit=crop&q=80&w=1080",
                    title = "Majestic Peaks (Ulug'vor Tog'lar)",
                    category = "Nature"
                ),
                Wallpaper(
                    url = "https://images.unsplash.com/photo-1470240731273-7821a6eeb6bd?auto=format&fit=crop&q=80&w=1080",
                    title = "Waterfall Paradise (Sharshara Jannati)",
                    category = "Nature"
                ),

                // === ABSTRACT ===
                Wallpaper(
                    url = "https://images.unsplash.com/photo-1541701494587-cb58502866ab?auto=format&fit=crop&q=80&w=1080",
                    title = "Neon Fluid (Neon Suyuqlik)",
                    category = "Abstract"
                ),
                Wallpaper(
                    url = "https://images.unsplash.com/photo-1604871000636-074fa5117945?auto=format&fit=crop&q=80&w=1080",
                    title = "Aura Grid (Aura Panjarasi)",
                    category = "Abstract"
                ),
                Wallpaper(
                    url = "https://images.unsplash.com/photo-1550684848-fac1c5b4e853?auto=format&fit=crop&q=80&w=1080",
                    title = "Dark Marble (To'q Marmar)",
                    category = "Abstract"
                ),
                Wallpaper(
                    url = "https://images.unsplash.com/photo-1508739773434-c26b3d09e071?auto=format&fit=crop&q=80&w=1080",
                    title = "Neon Portal (Neon Yo'lak)",
                    category = "Abstract"
                ),
                Wallpaper(
                    url = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?auto=format&fit=crop&q=80&w=1080",
                    title = "Vaporwave Dream (Kvantli Orzular)",
                    category = "Abstract"
                ),

                // === MINIMALIST ===
                Wallpaper(
                    url = "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?auto=format&fit=crop&q=80&w=1080",
                    title = "Ethereal Sand (Sokin Qumlar)",
                    category = "Minimalist"
                ),
                Wallpaper(
                    url = "https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?auto=format&fit=crop&q=80&w=1080",
                    title = "Urban Lines (Shahar Chiziqlari)",
                    category = "Minimalist"
                ),
                Wallpaper(
                    url = "https://images.unsplash.com/photo-1613490493576-7fde63acd811?auto=format&fit=crop&q=80&w=1080",
                    title = "Clean Arches (Silliq Arkalar)",
                    category = "Minimalist"
                ),
                Wallpaper(
                    url = "https://images.unsplash.com/photo-1513151233558-d860c5398176?auto=format&fit=crop&q=80&w=1080",
                    title = "Geometry Pastel (Geometriya Pastel)",
                    category = "Minimalist"
                ),
                Wallpaper(
                    url = "https://images.unsplash.com/photo-1497366216548-37526070297c?auto=format&fit=crop&q=80&w=1080",
                    title = "Concrete Shadow (Beton Soya)",
                    category = "Minimalist"
                ),

                // === ANIME ===
                Wallpaper(
                    url = "https://images.unsplash.com/photo-1578632767115-351597cf2477?auto=format&fit=crop&q=80&w=1080",
                    title = "City Sunset (Shahar Shom Vaqti)",
                    category = "Anime"
                ),
                Wallpaper(
                    url = "https://images.unsplash.com/photo-1511512578047-dfb367046420?auto=format&fit=crop&q=80&w=1080",
                    title = "Cyberpunk Alley (Kiberpank Ko'cha)",
                    category = "Anime"
                ),
                Wallpaper(
                    url = "https://images.unsplash.com/photo-1522383225653-ed111181a951?auto=format&fit=crop&q=80&w=1080",
                    title = "Cherry Blossom (Gilos Gullashi)",
                    category = "Anime"
                ),
                Wallpaper(
                    url = "https://images.unsplash.com/photo-1542751371-adc38448a05e?auto=format&fit=crop&q=80&w=1080",
                    title = "Pixel Train (Piksel Poezd)",
                    category = "Anime"
                ),
                Wallpaper(
                    url = "https://images.unsplash.com/photo-1550745165-9bc0b252726f?auto=format&fit=crop&q=80&w=1080",
                    title = "Retro Gamer (Retro O'yinlar)",
                    category = "Anime"
                )
            )
            for (wallpaper in defaults) {
                wallpaperDao.insertWallpaper(wallpaper)
            }
        }
    }
}
