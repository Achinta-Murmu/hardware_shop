package com.galla.app

import android.content.Intent
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Configurator
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.Test
import java.util.regex.Pattern
import org.junit.runner.RunWith

/**
 * A robot user. It opens the app on an emulator, walks through every screen the way a
 * person would (finding things by the words on screen and tapping them), and saves a
 * screenshot of each to /sdcard/Download/galla-shots. The cloud build pulls those pictures
 * so the screens can be looked at without a phone.
 *
 * It uses UI Automator, Android's tool for driving any app from the outside. A step that
 * fails is logged with the words that were on screen, and the walk continues.
 */
@RunWith(AndroidJUnit4::class)
class WalkthroughTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val device: UiDevice = UiDevice.getInstance(instrumentation)
    private val dir = "/sdcard/Download/galla-shots"
    private val report = StringBuilder()
    private val app = "com.galla.app"

    private fun log(line: String) {
        Log.i("GALLA", line)
        report.appendLine(line)
    }

    private fun pause(ms: Long) = Thread.sleep(ms)

    private fun shot(name: String) {
        dismissSystemDialogs()
        pause(900)
        device.executeShellCommand("screencap -p $dir/$name.png")
    }

    private fun find(selector: BySelector, timeout: Long = 6000): UiObject2? = device.wait(Until.findObject(selector), timeout)

    /** Slow cloud emulators pop up "System UI isn't responding". Tap "Wait" so it gets out of the way. */
    private fun dismissSystemDialogs() {
        repeat(3) {
            if (!device.hasObject(By.textContains("responding"))) return
            device.findObject(By.text("Wait"))?.click()
            pause(1500)
        }
    }

    private fun tap(selector: BySelector, what: String) {
        // The app redraws all the time (wavy bars, a turning shape), so a node found a moment
        // ago can be "stale" by the time it is tapped. Look it up again and retry.
        repeat(4) {
            var node = find(selector)
            if (node == null) {
                // A system pop-up may have appeared in the meantime: clear it and look once more.
                dismissSystemDialogs()
                node = find(selector)
            }
            if (node == null) error("not on screen: $what")
            try {
                node.click()
                pause(1300)
                return
            } catch (stale: StaleObjectException) {
                pause(400)
            }
        }
        error("kept changing, could not tap: $what")
    }

    private fun tapText(text: String) = tap(By.pkg(app).textContains(text), text)
    private fun tapDesc(text: String) = tap(By.pkg(app).descContains(text), text)

    /** Navigation bar items sit at the bottom, so of everything with this label take the lowest. */
    private fun tapTab(label: String) {
        device.wait(Until.hasObject(By.pkg(app).text(label)), 6000)
        val node = device.findObjects(By.pkg(app).text(label)).maxByOrNull { it.visibleBounds.centerY() }
            ?: error("no tab: $label")
        node.click()
        pause(1800)
    }

    private fun swipeUp() {
        val x = device.displayWidth / 2
        device.swipe(x, (device.displayHeight * 0.72).toInt(), x, (device.displayHeight * 0.30).toInt(), 25)
        pause(700)
    }

    /** Scrolls the page back to its top. */
    private fun toTop() {
        val x = device.displayWidth / 2
        repeat(3) {
            device.swipe(x, (device.displayHeight * 0.30).toInt(), x, (device.displayHeight * 0.85).toInt(), 12)
            pause(500)
        }
    }

    /** Swipes up until the text is on screen, then taps it. */
    private fun scrollAndTap(text: String) {
        repeat(8) {
            if (find(By.pkg(app).textContains(text), 700) != null) {
                tapText(text)
                return
            }
            swipeUp()
        }
        error("not found after scrolling: $text")
    }

    /** Types into the text box that is number `index` on screen, counted from the top. */
    private fun type(index: Int, text: String) {
        device.wait(Until.hasObject(By.clazz("android.widget.EditText")), 5000)
        repeat(4) {
            try {
                val boxes = device.findObjects(By.clazz("android.widget.EditText")).sortedBy { it.visibleBounds.centerY() }
                val box = boxes.getOrNull(index) ?: error("no text box number $index (found ${boxes.size})")
                box.text = text
                pause(600)
                return
            } catch (stale: StaleObjectException) {
                pause(500)
            }
        }
        error("kept changing, could not type: $text")
    }

    /** True when the navigation bar with the four tabs is showing. */
    private fun atATab(): Boolean =
        device.hasObject(By.pkg(app).text("Money")) && device.hasObject(By.pkg(app).text("Sell")) && device.hasObject(By.pkg(app).text("Items"))

    /**
     * Gets back to one of the four main tabs, whatever the last step left open, so one
     * failed step cannot spoil all the steps after it.
     */
    private fun home() {
        repeat(6) {
            dismissSystemDialogs()
            if (atATab()) return
            if (!device.hasObject(By.pkg(app))) return // the app is not in front: nothing safe to press
            closeKeyboard()
            if (atATab()) return
            device.pressBack()
            pause(1200)
        }
    }

    private fun back() = tapDesc("Back")

    /** Presses Back only if the keyboard is really open. With no keyboard, Back would leave the screen. */
    private fun closeKeyboard() {
        val shown = device.executeShellCommand("dumpsys input_method").contains("mInputShown=true")
        if (shown) {
            device.pressBack()
            pause(900)
        }
    }

    private fun step(name: String, fromTab: Boolean = true, action: () -> Unit) {
        try {
            dismissSystemDialogs()
            if (fromTab) home()
            action()
            log("ok    $name")
        } catch (t: Throwable) {
            val visible = runCatching {
                device.findObjects(By.textStartsWith("")).mapNotNull { it.text }.filter { it.isNotBlank() }.take(30).joinToString(" | ")
            }.getOrDefault("")
            log("FAIL  $name: ${t.message?.lineSequence()?.firstOrNull()}  [on screen: $visible]")
            runCatching { device.executeShellCommand("screencap -p $dir/fail-${name.take(2)}.png") }
        }
    }

    @Test
    fun walkThroughEveryScreen() {
        // The app has animations that never stop (the turning shape, wavy bars), so never wait long for "idle".
        Configurator.getInstance().setWaitForIdleTimeout(1200)
        device.executeShellCommand("mkdir -p $dir")

        val context = instrumentation.targetContext
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)!!
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(launch)
        device.wait(Until.hasObject(By.pkg(context.packageName).depth(0)), 15000)
        pause(4000)
        dismissSystemDialogs()

        step("01 welcome", fromTab = false) { shot("01-welcome") }
        step("02 open the shop", fromTab = false) {
            type(0, "Sharma Hardware")
            closeKeyboard()
            scrollAndTap("Open my shop")
            // Sample data takes a few seconds to build.
            device.wait(Until.hasObject(By.pkg(app).textContains("Today's sales")), 40000)
            pause(2500)
            shot("02-today")
        }
        step("03 today, lower half") { swipeUp(); shot("03-today-more") }
        step("04 items") { tapTab("Items"); pause(1500); shot("04-items") }
        step("05 items, low only") { tapText("Low "); pause(1000); shot("05-items-low"); tapText("All "); pause(1000) }
        step("06 item detail") { tapTab("Items"); tapText("Brass Bib Tap"); pause(1800); shot("06-item") }
        step("07 item detail, lower half", fromTab = false) { swipeUp(); shot("07-item-more") }
        step("08 add to bill", fromTab = false) { scrollAndTap("Add to the bill"); pause(1200); back() }
        step("09 new item form") {
            tapTab("Items")
            tapDesc("Add item"); pause(2000)
            type(0, "Cement Bag 50 kg")
            closeKeyboard()
            shot("09-new-item")
            back()
        }
        step("10 sell, with the bill") { tapTab("Sell"); pause(1500); shot("10-sell") }
        step("11 sell, searching") {
            tapTab("Sell")
            type(0, "wire")
            pause(1500)
            shot("11-sell-search")
            tapText("Copper Wire"); pause(1200)
            closeKeyboard()
        }
        step("12 sell, two lines") { tapTab("Sell"); tapDesc("One more"); pause(800); shot("12-sell-bill") }
        step("13 save the sale") { tapTab("Sell"); tapText("Save sale"); pause(3000); shot("13-receipt") }
        step("14 next customer", fromTab = false) { scrollAndTap("Next customer"); pause(1500); shot("14-sell-empty") }
        step("15 scanner") {
            tapTab("Sell")
            tapDesc("Scan a sticker"); pause(2500)
            // The phone asks for the camera the first time. (The button's wording differs in case between Android versions.)
            find(By.text(Pattern.compile("(?i)while using the app")), 4000)?.click()
            pause(3500)
            shot("15-scanner")
            tapDesc("Close the scanner")
        }
        step("16 money, day") { tapTab("Money"); pause(2000); shot("16-money") }
        step("17 money, lower half") { tapTab("Money"); swipeUp(); shot("17-money-more") }
        step("18 money, month") { tapTab("Money"); toTop(); tapText("Month"); pause(1800); shot("18-money-month") }
        step("19 add expense") { tapTab("Money"); toTop(); tapDesc("Add an expense"); pause(1500); shot("19-expense"); back() }
        step("20 restock list") { tapTab("Today"); pause(1500); tapText("restocking"); pause(1500); shot("20-restock") }
        step("21 stock in") {
            tapTab("Today"); pause(1200); tapText("restocking"); pause(1500)
            tapText("Teflon Tape"); pause(1500); type(0, "40"); closeKeyboard(); shot("21-stock-in")
        }
        step("22 settings") { tapTab("Today"); pause(1200); tapDesc("Settings"); pause(1500); shot("22-settings") }
        step("22b settings, lower half", fromTab = false) { swipeUp(); shot("22b-settings-more") }
        step("23 dark theme") {
            tapTab("Today"); pause(1200); tapDesc("Settings"); pause(1500)
            tapText("Dark"); pause(1800); shot("23-settings-dark"); back(); pause(1800); shot("24-today-dark")
        }
        step("24b dark item") { tapTab("Items"); tapText("Brass Bib Tap"); pause(1800); shot("24b-item-dark") }
        step("24c dark sell") { tapTab("Sell"); pause(1500); shot("24c-sell-dark") }
        step("25 dark items") { tapTab("Items"); pause(1500); shot("25-items-dark") }
        step("26 dark money") { tapTab("Money"); pause(1800); shot("26-money-dark") }

        println("GALLA-WALKTHROUGH\n$report")
    }
}
