package com.progresswidget

/**
 * Pre-computed dot grid for the globe (Africa/Europe/Middle East view).
 * Each entry: [normX, normY, isLand]
 * normX, normY are in range [-1, 1] relative to globe center and radius.
 * isLand = 1f for land, 0f for ocean.
 *
 * Generated from Natural Earth 110m land data using orthographic projection
 * centered at lon=15, lat=15.
 */
object GlobeDotMap {

    // Landmass mask (31x31 grid) for Africa/Europe/Middle East orthographic view
    // 1 = land, 0 = ocean
    private val LAND_MASK = arrayOf(
        "0000000000111111100000000000000",
        "0000000011111111111110000000000",
        "0000000111111111111111100000000",
        "0000001111111111111111110000000",
        "0000011111111101111111111000000",
        "0000111111111000111111111100000",
        "0000111111110000011111111110000",
        "0000011111100000001111111111000",
        "0000001111000000000111111111100",
        "0000000110000000000011111111110",
        "0000000000000000000001111111110",
        "0000000000000000000000111111111",
        "0000000000000000000000111111111",
        "0000000000000111111100011111110",
        "0000000001111111111111111111100",
        "0000000111111111111111111111000",
        "0000001111111111111111111110000",
        "0000011111111111111111111100000",
        "0000111111111111111111111000000",
        "0000111111111111111111110000000",
        "0000111111111111111111100000000",
        "0000011111111111111111000000000",
        "0000011111111111111110000000000",
        "0000001111111111111100000000000",
        "0000001111111111111000000000000",
        "0000000111111111110000000000000",
        "0000000011111111000000000000000",
        "0000000001111100000000000000000",
        "0000000000111000000000000000000",
        "0000000000010000000000000000000",
        "0000000000000000000000000000000"
    )

    private val COLS = 31
    private val ROWS = 31

    data class GlobeDot(
        val normX: Float,   // -1 to 1
        val normY: Float,   // -1 to 1
        val isLand: Boolean
    )

    val dots: List<GlobeDot> by lazy {
        val list = mutableListOf<GlobeDot>()
        for (row in 0 until ROWS) {
            for (col in 0 until COLS) {
                val nx = -1f + col * (2f / (COLS - 1))
                val ny = -1f + row * (2f / (ROWS - 1))
                // Only include dots inside the circle
                if (nx * nx + ny * ny > 1.05f) continue
                val isLand = LAND_MASK[row][col] == '1'
                list.add(GlobeDot(nx, ny, isLand))
            }
        }
        list
    }
}
