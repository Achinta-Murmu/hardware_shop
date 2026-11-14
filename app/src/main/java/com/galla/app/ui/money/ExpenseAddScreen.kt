package com.galla.app.ui.money

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.galla.app.core.model.ExpenseCategories
import com.galla.app.core.util.Money
import com.galla.app.data.repo.ShopException
import com.galla.app.ui.components.AppBar
import com.galla.app.ui.components.Btn
import com.galla.app.ui.components.BtnSize
import com.galla.app.ui.components.Chip
import com.galla.app.ui.components.Eyebrow
import com.galla.app.ui.components.FieldKind
import com.galla.app.ui.components.GField
import com.galla.app.ui.components.Gap
import com.galla.app.ui.components.ScreenColumn
import com.galla.app.ui.components.rememberHaptics
import com.galla.app.ui.icons.Ic
import com.galla.app.ui.nav.LocalContainer
import com.galla.app.ui.nav.LocalMessenger
import com.galla.app.ui.nav.LocalNav
import kotlinx.coroutines.launch

/** Money spent on something that is not stock: rent, electricity, tea. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpenseAddScreen() {
    val app = LocalContainer.current
    val nav = LocalNav.current
    val messenger = LocalMessenger.current
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()

    var amount by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(ExpenseCategories.all.first()) }
    var note by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    ScreenColumn {
        AppBar(onBack = { nav.back() }, title = "Add an expense")
        Text(
            "Money the shop spent on anything that is not stock. For stock from a supplier, use Stock in instead.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Gap(14.dp)
        GField(
            value = amount,
            onValueChange = {
                amount = it
                error = null
            },
            label = "Amount",
            modifier = Modifier.padding(horizontal = 16.dp),
            kind = FieldKind.MONEY,
            prefix = "₹",
            error = error,
        )
        Gap(10.dp)
        Eyebrow("What was it for?", Modifier.padding(horizontal = 20.dp))
        Gap(2.dp)
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ExpenseCategories.all.forEach { option -> Chip(option, { category = option }, selected = category == option) }
        }
        Gap(8.dp)
        GField(
            value = note,
            onValueChange = { note = it },
            label = "Note (optional)",
            modifier = Modifier.padding(horizontal = 16.dp),
            last = true,
        )
        Gap(18.dp)
        Btn(
            text = "Save expense",
            onClick = {
                val paise = Money.parse(amount)
                if (paise == null || paise <= 0L) {
                    error = "Enter the amount spent."
                    haptics.reject()
                } else {
                    scope.launch {
                        try {
                            app.shop.addExpense(category, note, paise)
                            haptics.confirm()
                            messenger.show("${Money.format(paise)} for $category saved")
                            nav.back()
                        } catch (e: ShopException) {
                            messenger.show(e.message ?: "Could not save the expense.")
                        }
                    }
                }
            },
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            icon = Ic.check,
            size = BtnSize.LG,
        )
    }
}
