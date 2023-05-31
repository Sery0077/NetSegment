package sery.vlasenko.netsegment.ui.server.dialog_start_test

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import sery.vlasenko.netsegment.R
import sery.vlasenko.netsegment.utils.throttleClick

class DialogStartTest : DialogFragment(R.layout.dialog_start_test) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val etIterationCount = view.findViewById<EditText>(R.id.dialog_start_test_count_iterations)
        val btnStartTest = view.findViewById<Button>(R.id.dialog_start_test_start_test_button)

        btnStartTest.throttleClick {
            dismiss()
            (parentFragment as? DialogStartTestClickListener)?.onStartTestClick(
                etIterationCount.text.toString().toInt()
            )
        }

        super.onViewCreated(view, savedInstanceState)
    }

    interface DialogStartTestClickListener {
        fun onStartTestClick(iterationCount: Int)
    }

}