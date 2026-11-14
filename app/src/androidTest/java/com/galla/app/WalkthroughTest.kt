package com.galla.app

import android.content.Intent
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Configurator
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.Test
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
        var node = find(selector)
        if (node == null) {
            // A system pop-up may have appeared in the meantime: clear it and look once more.
            dismissSystemDialogs()
            node = find(selector)
        }
        if (node == null) error("not on screen: $what")
        node.click()
        pause(1300)
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
        val boxes = device.findObjects(By.clazz("android.widget.EditText")).sortedBy { it.visibleBounds.centerY() }
        val box = boxes.getOrNull(index) ?: error("no text box number $index (found ${boxes.size})")
        box.text = text
        pause(600)
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

    private fun step(name: String, action: () -> Unit) {
        try {
            dismissSystemDialogs()
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

        step("01 welcome") { shot("01-welcome") }
        step("02 open the shop") {
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
        step("05 items, low only") { tapText("Low "); pause(1000); shot("05-items-low"); tapText("All ") }
        step("06 item detail") { tapText("Brass Bib Tap"); pause(1800); shot("06-item") }
        step("07 item detail, lower half") { swipeUp(); shot("07-item-more") }
        step("08 add to bill") { scrollAndTap("Add to the bill"); pause(1200); back() }
        step("09 new item form") {
            tapText("Add item"); pause(1500)
            type(0, "Cement Bag 50 kg")
            closeKeyboard()
            shot("09-new-item")
            back()
        }
        step("10 sell, with the bill") { tapTab("Sell"); pause(1500); shot("10-sell") }
        step("11 sell, searching") {
            type(0, "wire")
            pause(1500)
            shot("11-sell-search")
            tapText("Copper Wire"); pause(1200)
            closeKeyboard()
        }
        step("12 sell, two lines") { tapDesc("One more"); pause(800); shot("12-sell-bill") }
        step("13 save the sale") { tapText("Save sale"); pause(3000); shot("13-receipt") }
        step("14 next customer") { scrollAndTap("Next customer"); pause(1500); shot("14-sell-empty") }
        step("15 scanner") {
            tapDesc("Scan a sticker"); pause(2500)
            // The phone asks for the camera the first time.
            (find(By.textContains("While using"), 3000) ?: find(By.textContains("Only this time"), 1500))?.click()
            pause(3500)
            shot("15-scanner")
            tapDesc("Close the scanner")
        }
        step("16 money, day") { tapTab("Money"); pause(2000); shot("16-money") }
        step("17 money, lower half") { swipeUp(); shot("17-money-more") }
        step("18 money, month") {
            device.swipe(device.displayWidth / 2, (device.displayHeight * 0.3).toInt(), device.displayWidth / 2, (device.displayHeight * 0.8).toInt(), 25)
            pause(800)
            tapText("Month"); pause(1800); shot("18-money-month")
        }
        step("19 add expense") { tapDesc("Add an expense"); pause(1500); shot("19-expense"); back() }
        step("20 restock list") { tapTab("Today"); pause(1500); tapText("restocking"); pause(1500); shot("20-restock") }
        step("21 stock in") { tapText("Teflon Tape"); pause(1500); type(0, "40"); closeKeyboard(); shot("21-stock-in"); back(); back() }
        step("22 settings") { tapDesc("Settings"); pause(1500); shot("22-settings") }
        step("23 dark theme") { tapText("Dark"); pause(1500); shot("23-settings-dark"); back(); pause(1500); shot("24-today-dark") }
        step("25 dark items") { tapTab("Items"); pause(1500); shot("25-items-dark") }
        step("26 dark money") { tapTab("Money"); pause(1800); shot("26-money-dark") }

        println("GALLA-WALKTHROUGH\n$report")
    }
}
