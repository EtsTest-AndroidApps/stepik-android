package org.stepic.droid.ui.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.stepic.droid.R

class AllowMobileDataDialogFragment : DialogFragment() {
    companion object {
        const val TAG = "AllowMobileDataDialogFragment"

        fun newInstance(): AllowMobileDataDialogFragment =
            AllowMobileDataDialogFragment()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.allow_mobile_download_title)
            .setMessage(R.string.allow_mobile_message)
            .setPositiveButton(R.string.yes) { _, _ ->
                //mobile allowed
                (targetFragment as Callback).onMobileDataStateChanged(true)
            }
            .setNegativeButton(R.string.no) { _, _ ->
                //only wifi allowed
                (targetFragment as Callback).onMobileDataStateChanged(false)
            }
            .create()

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        (targetFragment as Callback).onMobileDataStateChanged(false)
    }

    /**
     * The callback should be implemented by target fragment
     */
    interface Callback {
        fun onMobileDataStateChanged(isMobileAllowed: Boolean)
    }
}