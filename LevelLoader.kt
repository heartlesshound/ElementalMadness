package com.example.elementalsmash

import android.content.Context
import org.json.JSONObject

object LevelLoader {

    fun load(context: Context, assetPath: String): LevelDefinition {
        val jsonText = context.assets.open(assetPath).bufferedReader().use { it.readText() }
        val root = JSONObject(jsonText)

        val background = root.getString("background")
        val board = root.getString("board")

        val gridRows = root.getInt("gridRows")
        val gridCols = root.getInt("gridCols")

        val holesJson = root.getJSONArray("holes")
        val holes = mutableListOf<GridPos>()
        for (i in 0 until holesJson.length()) {
            val pair = holesJson.getJSONArray(i)
            holes += GridPos(pair.getInt(0), pair.getInt(1))
        }

        val allowedJson = root.getJSONArray("allowedRunes")
        val allowed = mutableListOf<RuneType>()
        for (i in 0 until allowedJson.length()) {
            allowed += RuneType.fromString(allowedJson.getString(i))
        }

        return LevelDefinition(
            backgroundTexture = background,
            boardTexture = board,
            gridRows = gridRows,
            gridCols = gridCols,
            holeCells = holes,
            allowedRunes = allowed
        )
    }
}
