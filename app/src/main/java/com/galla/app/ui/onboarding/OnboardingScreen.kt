package com.galla.app.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon
import com.galla.app.ui.components.Btn
import com.galla.app.ui.components.BtnSize
import com.galla.app.ui.components.FieldKind
import com.galla.app.ui.components.GField
import com.galla.app.ui.components.GSwitch
import com.galla.app.ui.components.GallaLogo
import com.galla.app.ui.components.Gap
import com.galla.app.ui.components.ShapeIcon
import com.galla.app.ui.icons.Ic
import com.galla.app.ui.nav.LocalContainer
import kotlinx.coroutines.launch

/**
 * The first screen, shown once. There is nothing to sign up for: the owner names the shop
 * (or skips that) and starts. "Sample items" fills the app with a made-up hardware shop so
 * it can be explored straight away.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OnboardingScreen() {
    val app = LocalContainer.current
    var shopName by rememberSaveable { mutableStateOf("") }
    var sample by rememberSaveable { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Gap(28.dp)
        GallaLogo(72.dp)
        Gap(22.dp)
        Text("Your shop,\nin your pocket.", style = MaterialTheme.typography.displayLarge)
        Gap(12.dp)
        Text(
            "Galla keeps your items, stock, sales and profit in one place. No login. No internet. Everything stays on this phone.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Gap(24.dp)
        Point(Ic.qr_code_scanner, MaterialShapes.Cookie9Sided, "Sell in seconds", "Scan the sticker, save the sale.")
        Point(Ic.inventory_2, MaterialShapes.Clover4Leaf, "Never run out", "See what is low before it is gone.")
        Point(Ic.savings, MaterialShapes.Sunny, "Know your profit", "Today and this month, at a glance.")
        Gap(20.dp)
        GField(
            value = shopName,
            onValueChange = { shopName = it },
            label = "Shop name",
            kind = FieldKind.NAME,
            supporting = "Shown on receipts and stickers. You can change it later.",
            last = true,
        )
        Gap(8.dp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Start with sample items", style = MaterialTheme.typography.titleSmall)
                Text(
                    "A made-up hardware shop to try things on. You can erase it later.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            GSwitch(checked = sample, onCheckedChange = { sample = it }, label = "Start with sample items")
        }
        Gap(22.dp)
        Btn(
            text = if (busy) "Setting up your shop" else "Open my shop",
            onClick = {
                busy = true
                // Runs on the app's own scope so turning the phone does not cancel it half way.
                app.appScope.launch {
                    if (sample && app.shop.itemCount() == 0) runCatching { app.sample.load() }
                    app.settings.update { it.copy(onboarded = true, shopName = shopName.trim()) }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            trailing = Ic.arrow_forward,
            size = BtnSize.LG,
            enabled = !busy,
        )
        Gap(28.dp)
    }
}

/** One selling point: an icon on a Material shape, a bold line and a plain line. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Point(icon: Ic, shape: RoundedPolygon, title: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ShapeIcon(icon, size = 52.dp, shape = shape)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
