package org.stepic.droid.ui.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.shawnlin.numberpicker.NumberPicker
import org.stepic.droid.R
import org.stepic.droid.base.App
import org.stepic.droid.preferences.SharedPreferenceHelper
import org.stepic.droid.ui.util.TimeIntervalUtil
import org.stepic.droid.util.SuppressFBWarnings
import org.stepic.droid.util.resolveColorAttribute
import timber.log.Timber
import javax.inject.Inject

@SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE")
class TimeIntervalPickerDialogFragment : DialogFragment() {
    companion object {
        const val TAG = "time_interval_picker_dialog"

        private const val CHOSEN_POSITION_KEY = "CHOSEN_POSITION_KEY"

        fun newInstance(): TimeIntervalPickerDialogFragment =
            TimeIntervalPickerDialogFragment()

        interface Callback {
            fun onTimeIntervalPicked(chosenInterval: Int)
        }
    }

    @Inject
    lateinit var sharedPreferences: SharedPreferenceHelper

    private lateinit var picker: NumberPicker

    private lateinit var callback: Callback

    init {
        App.component().inject(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(CHOSEN_POSITION_KEY, picker.value)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        picker = NumberPicker(context)
        picker.minValue = 0
        picker.maxValue = TimeIntervalUtil.values.size - 1
        picker.displayedValues = TimeIntervalUtil.values
        picker.value = savedInstanceState?.getInt(CHOSEN_POSITION_KEY) ?: sharedPreferences.timeNotificationCode
        picker.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
        picker.wrapSelectorWheel = false
        picker.setBackgroundColor(0x0)
        picker.textColor = requireContext().resolveColorAttribute(R.attr.colorOnSurface)
        picker.selectedTextColor = requireContext().resolveColorAttribute(R.attr.colorOnSurface)
        picker.dividerColor = 0x0

        try {
            picker.textSize = 50f //Warning: reflection!
        } catch (exception: Exception) {
            Timber.e("reflection failed -> ignore")
        }

        callback = (targetFragment as? Callback)
            ?: activity as Callback

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.choose_notification_time_interval)
            .setView(picker)
            .setPositiveButton(R.string.ok) { _, _ ->
                callback.onTimeIntervalPicked(picker.value)
            }
            .setNegativeButton(R.string.cancel, null)
            .create()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        // explicitly click Negative or cancel by back button || touch outside
        callback.onTimeIntervalPicked(sharedPreferences.timeNotificationCode)
    }
}
