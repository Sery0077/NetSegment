package sery.vlasenko.netsegment.ui.result

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.DialogFragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import sery.vlasenko.netsegment.R

class DialogFragmentResult: BottomSheetDialogFragment(R.layout.bottom_sheet_result) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val btn = view.findViewById<Button>(R.id.bottom_sheet_save_result)

        btn.setOnClickListener {
            dismiss()
            (parentFragment as? ResultClickListener)?.onSaveResultClicked()
        }
    }

    interface ResultClickListener {
        fun onSaveResultClicked()
    }

}