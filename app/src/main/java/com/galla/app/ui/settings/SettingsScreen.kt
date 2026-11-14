package com.galla.app.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.galla.app.BuildConfig
import com.galla.app.core.model.Settings
import com.galla.app.core.model.ThemeChoice
import com.galla.app.core.util.Dates
import com.galla.app.data.repo.ShopException
import com.galla.app.ui.components.AppBar
import com.galla.app.ui.components.Btn
import com.galla.app.ui.components.BtnSize
import com.galla.app.ui.components.BtnStyle
import com.galla.app.ui.components.ButtonGroup
import com.galla.app.ui.components.Chevron
import com.galla.app.ui.components.FieldKind
import com.galla.app.ui.components.GField
import com.galla.app.ui.components.GGroup
import com.galla.app.ui.components.GSwitch
import com.galla.app.ui.components.GallaLogo
import com.galla.app.ui.components.Gap
import com.galla.app.ui.components.GroupItem
import com.galla.app.ui.components.GroupOption
import com.galla.app.ui.components.RowContent
import com.galla.app.ui.components.ScreenColumn
import com.galla.app.ui.components.SectionHeader
import com.galla.app.ui.components.ShapeAvatar
import com.galla.app.ui.components.ShapeIcon
import com.galla.app.ui.icons.Ic
import com.galla.app.ui.nav.ItemDetail
import com.galla.app.ui.nav.LocalContainer
import com.galla.app.ui.nav.LocalMessenger
import com.galla.app.ui.nav.LocalNav
import com.galla.app.ui.theme.supportsDynamicColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Settings: the shop's name, the look, and the owner's data. Because everything lives on
 * this phone, "Save a backup" is the important one here.
 */
@Composable
fun SettingsScreen() {
    val app = LocalContainer.current
    val nav = LocalNav.current
    val messenger = LocalMessenger.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by app.settings.settings.collectAsStateWithLifecycle(initialValue = Settings())
    val items by app.shop.items.collectAsStateWithLifecycle(initialValue = emptyList())
    val archived by app.shop.archivedItems.collectAsStateWithLifecycle(initialValue = emptyList())
    val scheme = MaterialTheme.colorScheme

    // What is being typed. Null until the owner types, so the saved name shows first.
    var typedName by remember { mutableStateOf<String?>(null) }
    /** The backup text waiting for a "yes, replace everything". */
    var pendingRestore by remember { mutableStateOf<String?>(null) }
    var confirmErase by remember { mutableStateOf(false) }

    // Saving: Android's own "where should this file go?" screen.
    val saveBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            scope.launch {
                val ok = runCatching {
                    val text = app.backup.export()
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray(Charsets.UTF_8)) }
                            ?: error("Could not open the file")
                    }
                }.isSuccess
                messenger.show(if (ok) "Backup saved" else "Could not save the backup")
            }
        }
    }
    // Restoring: Android's own "pick a file" screen.
    val openBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val text = runCatching {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                    }
                }.getOrNull()
                if (text == null) messenger.show("Could not read that file") else pendingRestore = text
            }
        }
    }

    ScreenColumn {
        AppBar(onBack = { nav.back() }, title = "Settings")

        // Shop
        SectionHeader("Your shop")
        GField(
            value = typedName ?: settings.shopName,
            onValueChange = { text ->
                typedName = text
                scope.launch { app.settings.update { it.copy(shopName = text.trim()) } }
            },
            label = "Shop name",
            modifier = Modifier.padding(horizontal = 16.dp),
            kind = FieldKind.NAME,
            supporting = "Shown on receipts and stickers.",
            last = true,
        )

        // Look
        SectionHeader("Look")
        ButtonGroup(
            options = listOf(
                GroupOption("Auto", Ic.brightness_auto),
                GroupOption("Light", Ic.light_mode),
                GroupOption("Dark", Ic.dark_mode),
            ),
            selectedIndex = when (settings.theme) {
                ThemeChoice.SYSTEM -> 0
                ThemeChoice.LIGHT -> 1
                ThemeChoice.DARK -> 2
            },
            onSelect = { index ->
                val choice = listOf(ThemeChoice.SYSTEM, ThemeChoice.LIGHT, ThemeChoice.DARK)[index]
                scope.launch { app.settings.update { it.copy(theme = choice) } }
            },
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        if (supportsDynamicColor()) {
            Gap(10.dp)
            GGroup {
                GroupItem(0, 1) {
                    RowContent(
                        title = "Phone colours",
                        subtitle = "Use your wallpaper's colours instead of Galla green",
                        leading = { ShapeIcon(Ic.palette, size = 40.dp) },
                        trailing = {
                            GSwitch(
                                checked = settings.dynamicColor,
                                onCheckedChange = { on -> scope.launch { app.settings.update { it.copy(dynamicColor = on) } } },
                                label = "Phone colours",
                            )
                        },
                    )
                }
            }
        }

        // Data
        SectionHeader("Your data")
        Text(
            "Everything is stored on this phone only. Save a backup now and then, so a lost or broken phone does not take the shop's records with it.",
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Gap(10.dp)
        GGroup {
            GroupItem(0, 2, onClick = { saveBackup.launch("galla-backup-${Dates.dateOf(app.now())}.json") }) {
                RowContent(
                    title = "Save a backup",
                    subtitle = "One file with all items, bills and expenses",
                    leading = { ShapeIcon(Ic.download, size = 40.dp) },
                    trailing = { Chevron() },
                )
            }
            GroupItem(1, 2, onClick = { openBackup.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }) {
                RowContent(
                    title = "Restore from a backup",
                    subtitle = "Replaces everything in the app with a saved file",
                    leading = { ShapeIcon(Ic.upload, size = 40.dp, background = scheme.surfaceContainerHighest, tint = scheme.onSurfaceVariant) },
                    trailing = { Chevron() },
                )
            }
        }
        Gap(10.dp)
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (items.isEmpty()) {
                Btn(
                    "Add sample items",
                    {
                        app.appScope.launch { runCatching { app.sample.load() } }
                        messenger.show("Adding a sample shop")
                    },
                    icon = Ic.package_2,
                    style = BtnStyle.TONAL,
                    size = BtnSize.SM,
                )
            }
            Btn("Erase everything", { confirmErase = true }, icon = Ic.delete, style = BtnStyle.DANGER, size = BtnSize.SM)
        }

        // Archived items
        if (archived.isNotEmpty()) {
            SectionHeader("Archived items")
            GGroup {
                archived.forEachIndexed { index, item ->
                    GroupItem(index, archived.size, onClick = { nav.go(ItemDetail(item.id)) }) {
                        RowContent(
                            title = item.name,
                            subtitle = item.code,
                            leading = { ShapeAvatar(item.name, item.category, size = 40.dp) },
                            trailing = {
                                TextButton(onClick = { scope.launch { app.shop.setArchived(item.id, false) } }) { Text("Bring back") }
                            },
                        )
                    }
                }
            }
        }

        // About
        SectionHeader("About")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            GallaLogo(52.dp)
            Column(Modifier.weight(1f)) {
                Text("Galla ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.titleMedium)
                Text(
                    "No login. No internet. The app has no permission to go online, so the shop's records cannot leave this phone unless you save a backup yourself.",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
        Gap(16.dp)
    }

    val restore = pendingRestore
    if (restore != null) {
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text("Replace everything?") },
            text = { Text("All items, bills and expenses in the app now will be replaced by the ones in this backup. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingRestore = null
                    scope.launch {
                        try {
                            val summary = app.backup.import(restore)
                            app.cart.clear()
                            messenger.show("Restored ${summary.items} items and ${summary.sales} bills")
                        } catch (e: ShopException) {
                            messenger.show(e.message ?: "Could not restore that file")
                        } catch (e: Exception) {
                            messenger.show("Could not restore that file")
                        }
                    }
                }) { Text("Replace") }
            },
            dismissButton = { TextButton(onClick = { pendingRestore = null }) { Text("Cancel") } },
        )
    }

    if (confirmErase) {
        AlertDialog(
            onDismissRequest = { confirmErase = false },
            title = { Text("Erase everything?") },
            text = { Text("Every item, bill, purchase and expense will be deleted from this phone. Save a backup first if you might want them back. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmErase = false
                    scope.launch {
                        runCatching { app.backup.eraseEverything() }
                        app.cart.clear()
                        messenger.show("Everything erased")
                    }
                }) { Text("Erase") }
            },
            dismissButton = { TextButton(onClick = { confirmErase = false }) { Text("Cancel") } },
        )
    }
}
