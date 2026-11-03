package app.olauncher.fork

import app.olauncher.data.Constants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Olauncher+ fork drift guard. Extra home slots mirror upstream's slot 1..8 code by convention,
 * so these tests read upstream's sources and fail when an upstream change would make the fork
 * silently diverge (e.g. a new per-slot pref, a renamed key, a flag collision, a restyled slot).
 */
class ExtraHomeSlotsTest {

    private fun source(path: String) = File(path).readText()

    private val prefsSource by lazy { source("src/main/java/app/olauncher/data/Prefs.kt") }

    @Test
    fun perSlotKeysMatchUpstream() {
        val upstreamPrefixes = Regex("""private val (\w+)_8 = "(\w+)_8"""")
            .findAll(prefsSource).map { it.groupValues[2] }.toSet()
        assertEquals(
            "Upstream per-slot pref keys changed; update ExtraHomeSlots.KEY_* and ExtraSlotStore.save",
            upstreamPrefixes, ExtraHomeSlots.ALL_KEYS.toSet()
        )
    }

    @Test
    fun prefsFileNameMatchesUpstream() {
        assertTrue(prefsSource.contains("""PREFS_FILENAME = "${ExtraHomeSlots.PREFS_FILENAME}""""))
    }

    @Test
    fun homeAppFlagsEqualSlotNumbersAndExtraSlotsAreFree() {
        val flags = Constants::class.java.declaredFields
            .filter { it.name.startsWith("FLAG_") && it.type == Int::class.javaPrimitiveType }
            .associate { it.isAccessible = true; it.name to it.getInt(null) }
        for (slot in 1..ExtraHomeSlots.UPSTREAM_MAX) assertEquals(slot, flags["FLAG_SET_HOME_APP_$slot"])
        assertEquals("Upstream gained home slots", null, flags["FLAG_SET_HOME_APP_${ExtraHomeSlots.UPSTREAM_MAX + 1}"])
        val collisions = flags.filterValues { it in ExtraHomeSlots.EXTRA_SLOTS }
        assertTrue("Flags collide with extra slots: $collisions", collisions.isEmpty())
    }

    @Test
    fun layoutsDeclareExactlyMaxSlotsAndBindingListMatches() {
        for (layout in listOf("layout", "layout-land")) {
            val ids = Regex("""@\+id/homeApp(\d+)""").findAll(source("src/main/res/$layout/fragment_home.xml"))
                .map { it.groupValues[1].toInt() }.toList()
            assertEquals("$layout slot views", (1..ExtraHomeSlots.MAX).toList(), ids)
        }
        val bound = Regex("""listOf\(([^)]*homeApp[^)]*)\)""")
            .find(source("src/main/java/app/olauncher/fork/ExtraHomeSlots.kt"))!!.groupValues[1]
            .let { Regex("""homeApp(\d+)""").findAll(it).map { m -> m.groupValues[1].toInt() }.toList() }
        assertEquals(ExtraHomeSlots.EXTRA_SLOTS.toList(), bound)
    }

    @Test
    fun forkSlotStyleMirrorsUpstreamSlot8() {
        val styleSource = source("src/main/res/values/fork_styles.xml")
        val style = Regex("""<item name="([^"]+)">([^<]+)</item>""").findAll(styleSource)
            .associate { it.groupValues[1] to it.groupValues[2] }
        val parent = Regex("""name="ForkHomeAppSlot" parent="([^"]+)"""").find(styleSource)!!.groupValues[1]
        for (layout in listOf("layout", "layout-land")) {
            val element = Regex("""<TextView\s+android:id="@\+id/homeApp8"(.*?)/>""", RegexOption.DOT_MATCHES_ALL)
                .find(source("src/main/res/$layout/fragment_home.xml"))!!.groupValues[1]
            val attrs = Regex("""([\w:]+)="([^"]*)"""").findAll(element)
                .associate { it.groupValues[1] to it.groupValues[2] }
                .filterKeys { it != "android:tag" && !it.startsWith("tools:") }
            assertEquals("$layout slot style parent", "@style/$parent", attrs["style"])
            assertEquals("$layout slot attributes", attrs - "style", style)
        }
    }
}
