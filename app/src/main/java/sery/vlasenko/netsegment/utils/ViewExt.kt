package sery.vlasenko.netsegment.utils

import android.view.View
import android.widget.RadioGroup
import android.widget.Toast
import androidx.core.view.children
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import sery.vlasenko.netsegment.R

fun Fragment.showToast(message: String, length: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(requireContext(), message, length).show()
}

fun RadioGroup.disable() {
    children.forEach {
        it.isClickable = false
    }
}

fun RadioGroup.enable() {
    children.forEach {
        it.isClickable = true
    }
}

fun Fragment.buildSnack(
    view: View,
    text: String = getString(R.string.snackbar_error),
    clickListener: View.OnClickListener? = null
): Snackbar {
    val actionText = getString(R.string.snackbar_action)

    val snackBar = Snackbar.make(view, text, Snackbar.LENGTH_INDEFINITE)
        .setAction(actionText, clickListener)

    return snackBar
}

fun Fragment.buildSnackAndShow(
    view: View,
    text: String = getString(R.string.snackbar_error),
    clickListener: View.OnClickListener? = null
) {
    val actionText = getString(R.string.snackbar_action)

    val snackBar = Snackbar.make(view, text, Snackbar.LENGTH_INDEFINITE)
        .setAction(actionText, clickListener)

    snackBar.show()
}

