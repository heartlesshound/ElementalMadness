package com.example.elementalsmash

data class GridPos(val row: Int, val col: Int)

data class LevelDefinition(
    val backgroundTexture: String,
    val boardTexture: String,
    val gridRows: Int,
    val gridCols: Int,
    val holeCells: List<GridPos>,
    val allowedRunes: List<RuneType>
)

enum class RuneType {
    EARTH, FIRE, WATER, WIND;

    companion object {
        fun fromString(s: String): RuneType =
            when (s.lowercase()) {
                "earth" -> EARTH
                "fire" -> FIRE
                "water" -> WATER
                "wind" -> WIND
                else -> EARTH
            }
    }
}
