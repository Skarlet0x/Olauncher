package app.olauncher.fork

import android.content.Context
import android.widget.TextView
import androidx.core.content.edit
import app.olauncher.databinding.FragmentHomeBinding

/**
 * Olauncher+ fork: home-screen app slots beyond upstream's 8.
 *
 * Upstream hard-codes one named property per slot (appName1..appName8, ...). Rather than
 * copying that pattern, extra slots live here and reuse upstream's storage key scheme
 * ("APP_NAME_<slot>", ...). Upstream code reaches them through small hooks marked
 * "Olauncher+ fork" — mostly the `else` branches of Prefs' index-based getters.
 *
 * Invariants (enforced by ExtraHomeSlotsTest):
 * - KEY_* prefixes match the keys upstream uses for slots 1..8.
 * - Home-slot selection flags equal the slot number (Constants.FLAG_SET_HOME_APP_n == n),
 *   and no other flag collides with EXTRA_SLOTS.
 */
object ExtraHomeSlots {
    const val UPSTREAM_MAX = 8
    const val MAX = 10
    val EXTRA_SLOTS = (UPSTREAM_MAX + 1)..MAX

    // Must match Prefs.PREFS_FILENAME and the per-slot key names in Prefs.
    internal const val PREFS_FILENAME = "app.olauncher"
    const val KEY_NAME = "APP_NAME"
    const val KEY_PACKAGE = "APP_PACKAGE"
    const val KEY_ACTIVITY_CLASS_NAME = "APP_ACTIVITY_CLASS_NAME"
    const val KEY_USER = "APP_USER"
    const val KEY_IS_SHORTCUT = "IS_SHORTCUT"
    const val KEY_SHORTCUT_ID = "SHORTCUT_ID"
    val ALL_KEYS = listOf(KEY_NAME, KEY_PACKAGE, KEY_ACTIVITY_CLASS_NAME, KEY_USER, KEY_IS_SHORTCUT, KEY_SHORTCUT_ID)

    fun key(prefix: String, slot: Int) = "${prefix}_$slot"
}

/** Storage for the extra slots, in the same SharedPreferences file as upstream's Prefs. */
class ExtraSlotStore(context: Context) {
    private val prefs = context.getSharedPreferences(ExtraHomeSlots.PREFS_FILENAME, 0)

    fun getString(prefix: String, slot: Int): String =
        if (slot in ExtraHomeSlots.EXTRA_SLOTS) prefs.getString(ExtraHomeSlots.key(prefix, slot), "").orEmpty() else ""

    fun getBoolean(prefix: String, slot: Int): Boolean =
        slot in ExtraHomeSlots.EXTRA_SLOTS && prefs.getBoolean(ExtraHomeSlots.key(prefix, slot), false)

    fun setString(prefix: String, slot: Int, value: String?) {
        if (slot in ExtraHomeSlots.EXTRA_SLOTS) prefs.edit { putString(ExtraHomeSlots.key(prefix, slot), value) }
    }

    /** Mirrors what upstream's saveHomeApp writes for slots 1..8. */
    fun save(slot: Int, name: String, packageName: String, user: String, activityClassName: String?, isShortcut: Boolean, shortcutId: String) {
        if (slot !in ExtraHomeSlots.EXTRA_SLOTS) return
        prefs.edit {
            putString(ExtraHomeSlots.key(ExtraHomeSlots.KEY_NAME, slot), name)
            putString(ExtraHomeSlots.key(ExtraHomeSlots.KEY_PACKAGE, slot), packageName)
            putString(ExtraHomeSlots.key(ExtraHomeSlots.KEY_USER, slot), user)
            putString(ExtraHomeSlots.key(ExtraHomeSlots.KEY_ACTIVITY_CLASS_NAME, slot), activityClassName)
            putBoolean(ExtraHomeSlots.key(ExtraHomeSlots.KEY_IS_SHORTCUT, slot), isShortcut)
            putString(ExtraHomeSlots.key(ExtraHomeSlots.KEY_SHORTCUT_ID, slot), shortcutId)
        }
    }

    /** Mirrors upstream clearing name + package when a home app is no longer installed. */
    fun clear(slot: Int) {
        setString(ExtraHomeSlots.KEY_NAME, slot, "")
        setString(ExtraHomeSlots.KEY_PACKAGE, slot, "")
    }
}

/** The extra slot views, in slot order (homeApp9 = slot 9, ...). */
fun FragmentHomeBinding.extraSlotViews(): List<TextView> = listOf(homeApp9, homeApp10)

/** Slot number of an extra slot view, or null if [viewId] is not one. */
fun FragmentHomeBinding.extraSlotOf(viewId: Int): Int? =
    extraSlotViews().indexOfFirst { it.id == viewId }.takeIf { it >= 0 }?.let { it + ExtraHomeSlots.UPSTREAM_MAX + 1 }

/**
 * Shows extra slots up to [homeAppsNum], mirroring upstream's populateHomeScreen.
 * [bind] renders a slot's app into its view and returns false if the app is gone.
 */
fun FragmentHomeBinding.populateExtraSlots(homeAppsNum: Int, store: ExtraSlotStore, bind: (TextView, Int) -> Boolean) {
    extraSlotViews().forEachIndexed { index, view ->
        val slot = ExtraHomeSlots.UPSTREAM_MAX + 1 + index
        if (slot > homeAppsNum) return
        view.visibility = android.view.View.VISIBLE
        if (!bind(view, slot)) store.clear(slot)
    }
}
