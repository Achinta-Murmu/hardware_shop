package com.galla.app.ui.scan

import android.Manifest
import android.content.pm.PackageManager
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.galla.app.core.util.Codes
import com.galla.app.core.util.Qty
import com.galla.app.hardware.QrAnalyzer
import com.galla.app.ui.components.Btn
import com.galla.app.ui.components.BtnSize
import com.galla.app.ui.components.BtnStyle
import com.galla.app.ui.components.GIcon
import com.galla.app.ui.components.Gap
import com.galla.app.ui.components.IconBtn
import com.galla.app.ui.components.IconBtnStyle
import com.galla.app.ui.components.ShapeIcon
import com.galla.app.ui.components.rememberHaptics
import com.galla.app.ui.icons.Ic
import com.galla.app.ui.nav.ItemDetail
import com.galla.app.ui.nav.LocalContainer
import com.galla.app.ui.nav.LocalNav
import com.galla.app.ui.nav.Scan
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/** What the strip at the bottom of the scanner is saying. */
private data class ScanNote(val text: String, val detail: String = "", val good: Boolean = true)

/**
 * The scanner. In "sell" mode it keeps scanning: every sticker it sees goes on the bill, so
 * a whole basket can be scanned without touching the screen. In "find" mode the first
 * sticker opens that item.
 *
 * The camera picture is read on the phone and never stored or sent anywhere.
 */
@Composable
fun ScanScreen(route: Scan) {
    val app = LocalContainer.current
    val nav = LocalNav.current
    val context = LocalContext.current
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    val selling = route.mode == "sell"

    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var asked by remember { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok
        asked = true
    }
    LaunchedEffect(Unit) {
        if (!granted) permission.launch(Manifest.permission.CAMERA)
    }

    var note by remember { mutableStateOf(ScanNote(if (selling) "Point at an item's sticker" else "Point at a sticker to open the item")) }
    var added by remember { mutableIntStateOf(0) }
    // The camera sees the same sticker many times a second. Remember the last one so it counts once.
    var lastText by remember { mutableStateOf("") }
    var lastAt by remember { mutableStateOf(0L) }
    var done by remember { mutableStateOf(false) }

    suspend fun handle(text: String) {
        if (done) return
        val now = SystemClock.elapsedRealtime()
        if (text == lastText && now - lastAt < 2000L) return
        lastText = text
        lastAt = now

        val code = Codes.codeFromScan(text)
        val item = if (code == null) null else app.shop.findByCode(code)
        when {
            code == null -> {
                haptics.reject()
                note = ScanNote("Not a Galla sticker", "This QR code was not printed by this app.", good = false)
            }
            item == null || item.archived -> {
                haptics.reject()
                note = ScanNote("Item not in this shop", "The sticker $code does not match any item here.", good = false)
            }
            !selling -> {
                done = true
                haptics.confirm()
                nav.replaceTop(ItemDetail(item.id))
            }
            else -> {
                val problem = app.cart.addOne(item)
                if (problem == null) {
                    added++
                    haptics.confirm()
                    note = ScanNote(item.name, "${Qty.withUnit(app.cart.quantityOf(item.id), item.unit)} on the bill")
                } else {
                    haptics.reject()
                    note = ScanNote(item.name, problem, good = false)
                }
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (granted) {
            CameraPreview(onCode = { text -> scope.launch { handle(text) } }, modifier = Modifier.fillMaxSize())
            // The frame to aim with.
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(248.dp)
                    .border(3.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(36.dp)),
            )
        } else {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ShapeIcon(Ic.photo_camera, size = 96.dp)
                Text("Galla needs the camera to scan", style = MaterialTheme.typography.headlineSmall, color = Color.White, textAlign = TextAlign.Center)
                Text(
                    if (asked) "Camera access is off. Turn it on for Galla in the phone's Settings, under Apps."
                    else "The camera is only used to read stickers. Nothing is recorded.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                )
                Gap(4.dp)
                Btn("Allow camera", { permission.launch(Manifest.permission.CAMERA) }, icon = Ic.photo_camera)
            }
        }

        // Close button
        IconBtn(
            Ic.close,
            "Close the scanner",
            { nav.back() },
            modifier = Modifier
                .statusBarsPadding()
                .padding(12.dp),
            style = IconBtnStyle.SURFACE,
        )

        // The strip at the bottom: what was just scanned, and Done.
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(12.dp)
                .fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = if (note.good) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.errorContainer,
            contentColor = if (note.good) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onErrorContainer,
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GIcon(if (!note.good) Ic.error else if (added > 0) Ic.check_circle else Ic.qr_code_scanner, fill = true, size = 28.dp)
                    Column(Modifier.weight(1f)) {
                        Text(note.text, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                        if (note.detail.isNotEmpty()) Text(note.detail, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (selling) {
                    Gap(12.dp)
                    Btn(
                        text = if (added == 0) "Back to the bill" else "Done · $added scanned",
                        onClick = { nav.back() },
                        modifier = Modifier.fillMaxWidth(),
                        style = if (added == 0) BtnStyle.TONAL else BtnStyle.FILLED,
                        size = BtnSize.MD,
                    )
                }
            }
        }
    }
}

/**
 * The live camera picture (CameraX), with every frame also handed to [QrAnalyzer].
 * It starts when this appears on screen and stops when it leaves.
 */
@Composable
private fun CameraPreview(onCode: (String) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestOnCode by rememberUpdatedState(onCode)
    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }

    DisposableEffect(lifecycleOwner) {
        val executor = Executors.newSingleThreadExecutor()
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener(
            {
                runCatching {
                    val provider = future.get()
                    val preview = Preview.Builder().build()
                    preview.setSurfaceProvider(previewView.surfaceProvider)
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                    analysis.setAnalyzer(executor, QrAnalyzer { text -> latestOnCode(text) })
                    provider.unbindAll()
                    provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                }
            },
            ContextCompat.getMainExecutor(context),
        )
        onDispose {
            runCatching { future.get().unbindAll() }
            executor.shutdown()
        }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}
