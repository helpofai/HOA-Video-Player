package com.helpofai.videoplayer.feature.recommendation

import com.helpofai.videoplayer.core.model.Video

object RecommendationEngine {
    fun getRecommendations(allVideos: List<Video>): List<Video> {
        val playedVideos = allVideos.filter { it.playCount > 0 }
        if (playedVideos.isEmpty()) {
            // Cold start: recommend recently added
            return allVideos.sortedByDescending { it.dateAdded }.take(10)
        }

        val recommendations = mutableSetOf<Video>()
        
        // 1. From most visited folder, suggest unplayed or least played videos
        val favoriteFolder = playedVideos
            .groupBy { java.io.File(it.path).parentFile?.name ?: "Internal" }
            .maxByOrNull { entry -> entry.value.sumOf { it.playCount } }?.key
            
        if (favoriteFolder != null) {
            val folderSuggestions = allVideos.filter { 
                (java.io.File(it.path).parentFile?.name ?: "Internal") == favoriteFolder && it.playCount == 0 
            }.take(3)
            recommendations.addAll(folderSuggestions)
        }
        
        // 2. Based on favorite category
        val favoriteCategory = playedVideos
            .groupBy { it.category }
            .maxByOrNull { entry -> entry.value.sumOf { it.playCount } }?.key
            
        if (favoriteCategory != null && favoriteCategory != "General") {
            val categorySuggestions = allVideos.filter { 
                it.category == favoriteCategory && it.playCount == 0 
            }.take(3)
            recommendations.addAll(categorySuggestions)
        }
        
        // 3. Similar Duration to average watch length
        val avgDuration = playedVideos.map { it.duration }.average()
        if (!avgDuration.isNaN()) {
            val durationTolerance = 5 * 60 * 1000L // 5 minutes
            val durationSuggestions = allVideos.filter { 
                it.playCount == 0 && Math.abs(it.duration - avgDuration) < durationTolerance 
            }.take(2)
            recommendations.addAll(durationSuggestions)
        }
        
        // 4. Rediscover Favorites (Favorite videos not watched in last 7 days)
        val staleFavorites = allVideos.filter { 
            it.isFavorite && (System.currentTimeMillis() - it.lastPlayedTimestamp) > (7L * 24 * 60 * 60 * 1000) 
        }.take(2)
        recommendations.addAll(staleFavorites)
        
        // 5. Fill up to 10 with recently added unplayed
        val extraNeeded = 10 - recommendations.size
        if (extraNeeded > 0) {
            recommendations.addAll(allVideos.filter { it.playCount == 0 && it !in recommendations }.sortedByDescending { it.dateAdded }.take(extraNeeded))
        }
        
        return recommendations.toList()
    }
}
