package app.olauncher.fork

import android.content.Context
import android.view.LayoutInflater
import app.olauncher.R
import app.olauncher.databinding.DialogTextSizeBinding
import app.olauncher.helper.OlDialog
import app.olauncher.helper.createDialog

/**
 * Olauncher+ fork: "Apps on home screen" picker.
 *
 * Upstream uses a popup menu listing 0..8; with 0..MAX it overflows the screen and can't be
 * scrolled. This reuses upstream's text-size stepper (− value +) instead. Like the text-size
 * dialog, the new value is applied when the dialog is dismissed.
 */
fun Context.createHomeAppsCountDialog(current: Int, onPicked: (Int) -> Unit): OlDialog {
    var count = current
    var stepper: DialogTextSizeBinding? = null
    val dialog = createDialog(R.string.apps_on_home_screen, R.string.okay) { container ->
        DialogTextSizeBinding.inflate(LayoutInflater.from(container.context), container, false).also { stepper = it }.root
    }
    stepper?.apply {
        fun step(delta: Int) {
            count = (count + delta).coerceIn(0, ExtraHomeSlots.MAX)
            textSizeCurrent.text = count.toString()
        }
        step(0)
        textSizeMinus.setOnClickListener { step(-1) }
        textSizePlus.setOnClickListener { step(+1) }
    }
    dialog.setOnDismissListener { if (count != current) onPicked(count) }
    return dialog
}
