package com.priyanshu.localpdf.models

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset

enum class PagePosition {
    TOP_LEFT, TOP_RIGHT, MIDDLE_TOP, MIDDLE_BOTTOM, BOTTOM_LEFT, BOTTOM_RIGHT, BOTTOM_CENTER
}

enum class EncryptionLevel {
    LOW_40, MEDIUM_128, HIGH_256
}

enum class CompressionLevel {
    LOW, MEDIUM, HIGH
}

data class PdfPageData(
    val bitmap: Bitmap, 
    var rotation: Float = 0f, 
    val originalIndex: Int
)

data class Quadrilateral(
    val topLeft: Offset, 
    val topRight: Offset, 
    val bottomLeft: Offset, 
    val bottomRight: Offset
)
