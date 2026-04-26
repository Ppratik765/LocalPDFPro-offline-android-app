package com.priyanshu.localpdf.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.priyanshu.localpdf.engine.*
import com.priyanshu.localpdf.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.hypot

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ToolConfigScreen(
    toolId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleScope = LocalLifecycleOwner.current.lifecycleScope
    val tool = allPdfTools.find { it.id == toolId }

    var selectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var outputName by remember { mutableStateOf("${tool?.title?.replace(" ", "_") ?: "output"}_file") }

    var htmlContent by remember { mutableStateOf("") }
    var watermarkText by remember { mutableStateOf("CONFIDENTIAL") }
    var splitStart by remember { mutableStateOf("1") }
    var splitEnd by remember { mutableStateOf("1") }
    var pageNumPosition by remember { mutableStateOf(PagePosition.BOTTOM_RIGHT) }
    var encryptionLevel by remember { mutableStateOf(EncryptionLevel.HIGH_256) }
    var compressionLevel by remember { mutableStateOf(CompressionLevel.MEDIUM) }
    var passInput by remember { mutableStateOf("") }
    var metadata by remember { mutableStateOf(mapOf("Title" to "", "Author" to "", "Subject" to "", "Keywords" to "")) }

    var pdfPages by remember { mutableStateOf<List<PdfPageData>>(emptyList()) }
    // Triple holds: Uri, Rotation, and Thumbnail Bitmap for preview
    var imageItems by remember { mutableStateOf<List<Triple<Uri, Float, Bitmap?>>>(emptyList()) }

    var previewAvailable by remember { mutableStateOf(true) }
    var isLoadingPreviews by remember { mutableStateOf(false) }

    var cornerActiveUri by remember { mutableStateOf<Uri?>(null) }
    var imageCorners by remember { mutableStateOf<Map<Uri, Quadrilateral>>(emptyMap()) }

    val customUriStr by StatsManager.customUriFlow.collectAsState()
    val taskState by TaskProgressManager.taskState.collectAsState()

    val folderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            val docFile = DocumentFile.fromTreeUri(context, uri)
            if (docFile != null && docFile.canWrite()) {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, flags)
                StatsManager.setCustomOutputUri(uri.toString())
                Toast.makeText(context, "Destination Updated", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun extractPreviews(uri: Uri) {
        isLoadingPreviews = true
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val fd = context.contentResolver.openFileDescriptor(uri, "r") ?: throw Exception("FD null")
                val renderer = PdfRenderer(fd)
                val maxPages = minOf(renderer.pageCount, 50)
                val tempPages = mutableListOf<PdfPageData>()
                for (i in 0 until maxPages) {
                    val page = renderer.openPage(i)
                    val bmp = Bitmap.createBitmap(page.width / 2, page.height / 2, Bitmap.Config.ARGB_8888)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    tempPages.add(PdfPageData(bmp, 0f, i))
                    page.close()
                }
                withContext(Dispatchers.Main) {
                    pdfPages = tempPages
                    previewAvailable = true
                    isLoadingPreviews = false
                    if (toolId == "edit_metadata") metadata = PdfEngine.readMetadata(context, uri)
                }
                renderer.close()
                fd.close()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    previewAvailable = false
                    isLoadingPreviews = false
                }
            }
        }
    }

    val singleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedUris = listOf(it)
            if (toolId == "visual_organize" || toolId == "edit_metadata") extractPreviews(it)
        }
    }

    val multipleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            selectedUris = uris
            if (toolId == "images_to_pdf") {
                isLoadingPreviews = true
                lifecycleScope.launch(Dispatchers.IO) {
                    val list = uris.map { uri ->
                        val bmp = try {
                            val ips = context.contentResolver.openInputStream(uri)
                            val opts = BitmapFactory.Options().apply { inSampleSize = 4 } // Load fast thumbnail
                            val b = BitmapFactory.decodeStream(ips, null, opts)
                            ips?.close()
                            b
                        } catch(e: Exception) { null }
                        Triple(uri, 0f, bmp)
                    }
                    withContext(Dispatchers.Main) {
                        imageItems = list
                        isLoadingPreviews = false
                    }
                }
            } else {
                imageItems = uris.map { Triple(it, 0f, null) }
                if (toolId == "visual_organize") extractPreviews(uris.first())
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(tool?.title ?: "Tool")
                        if (taskState.isActive) {
                            Text(
                                "Remaining: ${if (taskState.estimatedSecondsRemaining > 0) "${taskState.estimatedSecondsRemaining}s" else "Calculating..."}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, null) }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState())) {

            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Settings", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(value = outputName, onValueChange = { outputName = it }, label = { Text("Output Filename") }, modifier = Modifier.fillMaxWidth())

                    when (toolId) {
                        "compress" -> {
                            Text("Compression Level:", style = MaterialTheme.typography.labelLarge)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                CompressionLevel.entries.forEach { level ->
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { compressionLevel = level }) {
                                        RadioButton(selected = compressionLevel == level, onClick = { compressionLevel = level })
                                        Text(level.name)
                                    }
                                }
                            }
                        }
                        "protect", "unlock" -> {
                            OutlinedTextField(
                                value = passInput,
                                onValueChange = { passInput = it },
                                label = { Text(if (toolId == "protect") "Set Password" else "Enter Document Password") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation()
                            )
                            if (toolId == "protect") {
                                Text("Encryption Strength:", style = MaterialTheme.typography.labelLarge)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    EncryptionLevel.entries.forEach { level ->
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { encryptionLevel = level }) {
                                            RadioButton(selected = encryptionLevel == level, onClick = { encryptionLevel = level })
                                            Text(level.name.split("_")[1] + "-bit")
                                        }
                                    }
                                }
                            }
                        }
                        "page_numbers" -> {
                            var expanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                                OutlinedTextField(
                                    value = pageNumPosition.name.replace("_", " "),
                                    onValueChange = {}, readOnly = true,
                                    label = { Text("Position") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor()
                                )
                                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    PagePosition.entries.forEach { pos ->
                                        DropdownMenuItem(text = { Text(pos.name.replace("_", " ")) }, onClick = { pageNumPosition = pos; expanded = false })
                                    }
                                }
                            }
                        }
                        "html_to_pdf" -> {
                            OutlinedTextField(
                                value = htmlContent, onValueChange = { htmlContent = it },
                                label = { Text("Paste HTML Code Here") },
                                modifier = Modifier.fillMaxWidth().height(250.dp),
                                maxLines = 20
                            )
                        }
                        "edit_metadata" -> {
                            if (selectedUris.isNotEmpty()) {
                                OutlinedTextField(value = metadata["Title"] ?: "", onValueChange = { metadata = metadata.toMutableMap().apply{ put("Title", it) } }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = metadata["Author"] ?: "", onValueChange = { metadata = metadata.toMutableMap().apply{ put("Author", it) } }, label = { Text("Author") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = metadata["Subject"] ?: "", onValueChange = { metadata = metadata.toMutableMap().apply{ put("Subject", it) } }, label = { Text("Subject") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = metadata["Keywords"] ?: "", onValueChange = { metadata = metadata.toMutableMap().apply{ put("Keywords", it) } }, label = { Text("Keywords") }, modifier = Modifier.fillMaxWidth())
                            } else {
                                Text("Select a PDF to view and edit metadata.", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        when (toolId) {
                            "images_to_pdf" -> multipleLauncher.launch("image/*")
                            "merge" -> multipleLauncher.launch("application/pdf")
                            "word_to_pdf" -> singleLauncher.launch("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                            "ppt_to_pdf" -> singleLauncher.launch("application/vnd.openxmlformats-officedocument.presentationml.presentation")
                            else -> singleLauncher.launch("application/pdf")
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Select Files")
                }

                OutlinedButton(
                    onClick = { folderLauncher.launch(null) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(if (customUriStr != null) "Change Dest" else "Set Dest Folder")
                }
            }

            if (selectedUris.isNotEmpty() && (toolId == "visual_organize" || toolId == "images_to_pdf" || toolId == "split")) {
                Text("Pages / Files (Long press to drag, Tap to rotate)", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp))
                Box(modifier = Modifier.height(400.dp)) {
                    if (isLoadingPreviews) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    } else if (previewAvailable) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            userScrollEnabled = false
                        ) {
                            if (toolId == "visual_organize" || toolId == "split") {
                                itemsIndexed(pdfPages, key = { _, page -> page.originalIndex }) { index, page ->
                                    Box(modifier = Modifier.animateItem()) {
                                        PreviewItem(
                                            bitmap = page.bitmap,
                                            rotation = page.rotation,
                                            label = "${index + 1}",
                                            onRotate = {
                                                pdfPages = pdfPages.toMutableList().apply {
                                                    this[index] = page.copy(rotation = (page.rotation + 90f) % 360f)
                                                }
                                            },
                                            onDrag = { from, to ->
                                                pdfPages = pdfPages.toMutableList().apply {
                                                    val item = removeAt(from)
                                                    add(to, item)
                                                }
                                            },
                                            index = index,
                                            totalItems = pdfPages.size
                                        )
                                    }
                                }
                            } else if (toolId == "images_to_pdf" || toolId == "merge") {
                                itemsIndexed(imageItems, key = { _, item -> item.first.toString() }) { index, item ->
                                    val uri = item.first
                                    val rot = item.second
                                    val bmp = item.third
                                    Box(modifier = Modifier.animateItem()) {
                                        PreviewItem(
                                            bitmap = bmp,
                                            uri = uri,
                                            rotation = rot,
                                            label = "${index + 1}",
                                            onRotate = {
                                                imageItems = imageItems.toMutableList().apply {
                                                    this[index] = Triple(uri, (rot + 90f) % 360f, bmp)
                                                }
                                            },
                                            onDrag = { from, to ->
                                                imageItems = imageItems.toMutableList().apply {
                                                    val i = removeAt(from)
                                                    add(to, i)
                                                }
                                                selectedUris = imageItems.map { it.first }
                                            },
                                            index = index,
                                            totalItems = imageItems.size
                                        )

                                        if (toolId == "images_to_pdf") {
                                            IconButton(
                                                onClick = { cornerActiveUri = uri },
                                                modifier = Modifier.align(Alignment.TopEnd).size(24.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                                            ) {
                                                Icon(Icons.Rounded.Crop, null, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (selectedUris.isEmpty() && toolId != "html_to_pdf") {
                        Toast.makeText(context, "Please select file first", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if ((toolId == "protect" || toolId == "unlock") && passInput.isBlank()) {
                        Toast.makeText(context, "Please enter a password", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    lifecycleScope.launch {
                        val finalOutputName = if (outputName.endsWith(".pdf")) outputName else "$outputName.pdf"

                        val result: Result<String> = when (toolId) {
                            "merge" -> PdfEngine.mergePdf(context, selectedUris, finalOutputName)
                            "images_to_pdf" -> {
                                val corners = imageCorners
                                val rotations = imageItems.associate { it.first to it.second }
                                PdfEngine.imagesToPdf(context, selectedUris, finalOutputName, corners, rotations)
                            }
                            "pdf_to_jpg" -> PdfEngine.pdfToJpg(context, selectedUris.first(), finalOutputName)
                            "pdf_to_word" -> PdfEngine.pdfToWord(context, selectedUris.first(), finalOutputName)
                            "pdf_to_ppt" -> PdfEngine.pdfToPpt(context, selectedUris.first(), finalOutputName)
                            "watermark" -> PdfEngine.watermark(context, selectedUris.first(), watermarkText, finalOutputName)
                            "split" -> PdfEngine.splitPdf(context, selectedUris.first(), splitStart.toIntOrNull() ?: 1, splitEnd.toIntOrNull() ?: 1, finalOutputName)
                            "compress" -> PdfEngine.compressPdf(context, selectedUris.first(), compressionLevel, finalOutputName)
                            "protect" -> PdfEngine.protectPdf(context, selectedUris.first(), passInput, encryptionLevel, finalOutputName)
                            "unlock" -> PdfEngine.unlockPdf(context, selectedUris.first(), passInput, finalOutputName)
                            "html_to_pdf" -> PdfEngine.htmlToPdf(context, htmlContent, finalOutputName)
                            "word_to_pdf" -> PdfEngine.wordToPdf(context, selectedUris.first(), finalOutputName)
                            "ppt_to_pdf" -> PdfEngine.pptToPdf(context, selectedUris.first(), finalOutputName)
                            "visual_organize" -> {
                                val order = pdfPages.map { it.originalIndex }
                                PdfEngine.visualOrganize(context, selectedUris.first(), order, finalOutputName)
                            }
                            "page_numbers" -> PdfEngine.addPageNumbers(context, selectedUris.first(), pageNumPosition, finalOutputName)
                            "edit_metadata" -> PdfEngine.editMetadata(context, selectedUris.first(), metadata, finalOutputName)
                            "extract_images" -> PdfEngine.extractImages(context, selectedUris.first(), finalOutputName)
                            "flatten_pdf" -> PdfEngine.flattenPdf(context, selectedUris.first(), finalOutputName)
                            "grayscale_pdf" -> PdfEngine.grayscalePdf(context, selectedUris.first(), finalOutputName)
                            else -> Result.failure(Exception("Unknown Tool"))
                        }

                        result.onSuccess {
                            Toast.makeText(context, "Task Completed Successfully", Toast.LENGTH_LONG).show()
                            onBack()
                        }.onFailure { e ->
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = !taskState.isActive
            ) {
                if (taskState.isActive) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Process Now")
                }
            }
        }
    }

    if (cornerActiveUri != null) {
        CornerAdjustmentDialog(
            uri = cornerActiveUri!!,
            onDismiss = { cornerActiveUri = null },
            onSave = { quad ->
                imageCorners = imageCorners.toMutableMap().apply { put(cornerActiveUri!!, quad) }
                cornerActiveUri = null
            }
        )
    }
}

@Composable
fun PreviewItem(
    bitmap: Bitmap? = null,
    uri: Uri? = null,
    rotation: Float,
    label: String,
    onRotate: () -> Unit,
    onDrag: (Int, Int) -> Unit,
    index: Int,
    totalItems: Int
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .aspectRatio(0.8f)
            .graphicsLayer {
                translationX = dragOffset.x
                translationY = dragOffset.y
                scaleX = if (isDragging) 1.1f else 1f
                scaleY = if (isDragging) 1.1f else 1f
                shadowElevation = if (isDragging) 16.dp.toPx() else 0f
            }
            .clip(RoundedCornerShape(8.dp))
            .background(if (isDragging) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { isDragging = true },
                    onDragEnd = { isDragging = false; dragOffset = Offset.Zero },
                    onDragCancel = { isDragging = false; dragOffset = Offset.Zero },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffset += dragAmount

                        val swapThreshold = 150f
                        if (dragOffset.x > swapThreshold && index < totalItems - 1) {
                            onDrag(index, index + 1)
                            dragOffset = Offset.Zero
                        } else if (dragOffset.x < -swapThreshold && index > 0) {
                            onDrag(index, index - 1)
                            dragOffset = Offset.Zero
                        } else if (dragOffset.y > swapThreshold && index < totalItems - 3) {
                            onDrag(index, index + 3)
                            dragOffset = Offset.Zero
                        } else if (dragOffset.y < -swapThreshold && index >= 3) {
                            onDrag(index, index - 3)
                            dragOffset = Offset.Zero
                        }
                    }
                )
            }
            .clickable { onRotate() }
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().graphicsLayer { rotationZ = rotation }
            )
        } else if (uri != null) {
            Icon(Icons.Rounded.Image, null, modifier = Modifier.align(Alignment.Center).size(32.dp).graphicsLayer { rotationZ = rotation })
        }

        Box(modifier = Modifier.align(Alignment.BottomEnd).background(MaterialTheme.colorScheme.primary).padding(horizontal = 6.dp, vertical = 2.dp)) {
            Text(label, color = Color.White, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun CornerAdjustmentDialog(
    uri: Uri,
    onDismiss: () -> Unit,
    onSave: (Quadrilateral) -> Unit
) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(uri) {
        withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
            } catch(e: Exception) {}
        }
    }

    var quad by remember { mutableStateOf(Quadrilateral(Offset(50f, 50f), Offset(400f, 50f), Offset(50f, 600f), Offset(400f, 600f))) }
    var draggedCorner by remember { mutableStateOf<Int?>(null) }

    fun Offset.distanceTo(other: Offset) = hypot((x - other.x).toDouble(), (y - other.y).toDouble()).toFloat()

    if (bitmap != null) {
        Dialog(onDismissRequest = onDismiss) {
            Surface(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Drag corners to wrap", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(modifier = Modifier.size(300.dp).background(Color.Black)) {
                        Image(bitmap!!.asImageBitmap(), null, modifier = Modifier.fillMaxSize(), alpha = 0.5f)
                        Canvas(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val dists = listOf(
                                        offset.distanceTo(quad.topLeft),
                                        offset.distanceTo(quad.topRight),
                                        offset.distanceTo(quad.bottomRight),
                                        offset.distanceTo(quad.bottomLeft)
                                    )
                                    val min = dists.minOrNull() ?: Float.MAX_VALUE
                                    if (min < 150f) draggedCorner = dists.indexOf(min)
                                },
                                onDragEnd = { draggedCorner = null },
                                onDragCancel = { draggedCorner = null },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    when (draggedCorner) {
                                        0 -> quad = quad.copy(topLeft = quad.topLeft + dragAmount)
                                        1 -> quad = quad.copy(topRight = quad.topRight + dragAmount)
                                        2 -> quad = quad.copy(bottomRight = quad.bottomRight + dragAmount)
                                        3 -> quad = quad.copy(bottomLeft = quad.bottomLeft + dragAmount)
                                    }
                                }
                            )
                        }) {
                            val path = Path().apply {
                                moveTo(quad.topLeft.x, quad.topLeft.y)
                                lineTo(quad.topRight.x, quad.topRight.y)
                                lineTo(quad.bottomRight.x, quad.bottomRight.y)
                                lineTo(quad.bottomLeft.x, quad.bottomLeft.y)
                                close()
                            }
                            drawPath(path, Color.Cyan, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))
                            drawCircle(Color.Red, radius = 20f, center = quad.topLeft)
                            drawCircle(Color.Red, radius = 20f, center = quad.topRight)
                            drawCircle(Color.Red, radius = 20f, center = quad.bottomRight)
                            drawCircle(Color.Red, radius = 20f, center = quad.bottomLeft)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { onSave(quad) }) { Text("Apply Crop") }
                }
            }
        }
    }
}