package com.priyanshu.localpdf.models

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector

enum class ToolCategory(val title: String) {
    MOST_USED("Most Used"),
    CONVERT_TO_PDF("Convert To PDF"),
    CONVERT_FROM_PDF("Convert From PDF"),
    SECURITY("Security"),
    PRO_FEATURES("Pro Features")
}

data class PdfTool(
    val id: String,
    val title: String,
    val description: String,
    val category: ToolCategory,
    val icon: ImageVector,
    val isAvailable: Boolean = true // All available for offline processing
)

val allPdfTools = listOf(
    // MOST USED
    PdfTool("merge", "Merge PDF", "Combine multiple files", ToolCategory.MOST_USED, Icons.Rounded.Merge),
    PdfTool("visual_organize", "Visual Organize", "Rearrange pages", ToolCategory.MOST_USED, Icons.Rounded.GridView),
    PdfTool("split", "Split PDF", "Extract ranges", ToolCategory.MOST_USED, Icons.Rounded.CallSplit),
    PdfTool("compress", "Compress PDF", "Reduce file size", ToolCategory.MOST_USED, Icons.Rounded.Compress),
    PdfTool("page_numbers", "Page Numbers", "Add numbers to pages", ToolCategory.MOST_USED, Icons.Rounded.Numbers),

    // CONVERT TO PDF
    PdfTool("images_to_pdf", "Images to PDF", "Smart Scanner & Img to PDF", ToolCategory.CONVERT_TO_PDF, Icons.Rounded.DocumentScanner),
    PdfTool("word_to_pdf", "Word to PDF", "Convert .docx to PDF", ToolCategory.CONVERT_TO_PDF, Icons.Rounded.Description),
    PdfTool("ppt_to_pdf", "PPT to PDF", "Convert slides to PDF", ToolCategory.CONVERT_TO_PDF, Icons.Rounded.Slideshow),
    PdfTool("html_to_pdf", "HTML to PDF", "Webpage offline saving", ToolCategory.CONVERT_TO_PDF, Icons.Rounded.Language),

    // CONVERT FROM PDF
    PdfTool("pdf_to_jpg", "PDF to JPG", "Extract pages as images", ToolCategory.CONVERT_FROM_PDF, Icons.Rounded.Image),
    PdfTool("pdf_to_word", "PDF to Word", "Extract text to .docx", ToolCategory.CONVERT_FROM_PDF, Icons.Rounded.Assignment),
    PdfTool("pdf_to_ppt", "PDF to PPT", "Extract to Presentation", ToolCategory.CONVERT_FROM_PDF, Icons.Rounded.PresentToAll),

    // SECURITY
    PdfTool("protect", "Protect PDF", "AES-256 Encryption", ToolCategory.SECURITY, Icons.Rounded.Lock),
    PdfTool("unlock", "Unlock PDF", "Remove passwords", ToolCategory.SECURITY, Icons.Rounded.LockOpen),

    // PRO FEATURES
    PdfTool("ocr_searchable", "OCR Searchable", "Make text searchable via AI", ToolCategory.PRO_FEATURES, Icons.Rounded.FindInPage),
    PdfTool("watermark", "Watermark", "Overlay text/logo", ToolCategory.PRO_FEATURES, Icons.Rounded.BrandingWatermark),
    PdfTool("edit_metadata", "Edit Metadata", "Modify hidden attributes", ToolCategory.PRO_FEATURES, Icons.Rounded.Info),
    PdfTool("extract_images", "Extract Images", "Get raw image resources", ToolCategory.PRO_FEATURES, Icons.Rounded.ImageSearch),
    PdfTool("flatten_pdf", "Flatten PDF", "Lock interactive forms", ToolCategory.PRO_FEATURES, Icons.Rounded.LayersClear),
    PdfTool("grayscale_pdf", "Grayscale PDF", "Remove colors", ToolCategory.PRO_FEATURES, Icons.Rounded.FilterBAndW)
)
