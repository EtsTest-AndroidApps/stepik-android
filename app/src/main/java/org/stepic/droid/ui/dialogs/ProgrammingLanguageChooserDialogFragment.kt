package org.stepic.droid.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.shawnlin.numberpicker.NumberPicker
import org.stepic.droid.R
import org.stepic.droid.util.resolveColorAttribute

class ProgrammingLanguageChooserDialogFragment : DialogFragment() {
    companion object {
        private const val LANGUAGES_KEY = "languages_key"
        fun newInstance(languages: Array<String>): ProgrammingLanguageChooserDialogFragment {
            val args = Bundle()
            args.putStringArray(LANGUAGES_KEY, languages)
            val fragment = ProgrammingLanguageChooserDialogFragment()
            fragment.arguments = args
            return fragment
        }
    }

    interface Callback {
        fun onLanguageChosen(programmingLanguage: String)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val picker = NumberPicker(context)
        val languageNames = arguments?.getStringArray(LANGUAGES_KEY) ?: emptyArray()
        picker.minValue = 0
        picker.maxValue = languageNames.size - 1
        picker.displayedValues = languageNames
        picker.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
        picker.wrapSelectorWheel = false
        picker.setBackgroundColor(0x0)
        picker.textColor = requireContext().resolveColorAttribute(R.attr.colorOnSurface)
        picker.selectedTextColor = requireContext().resolveColorAttribute(R.attr.colorOnSurface)
        picker.dividerColor = 0x0

        try {
            picker.textSize = 50f //Warning: reflection!
        } catch (exception: Exception) {
            //reflection failed -> ignore
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.choose_language)
            .setView(picker)
            .setPositiveButton(R.string.choose_action) { _, _ ->
                (parentFragment as Callback).onLanguageChosen(picker.displayedValues[picker.value])
            }
            .setNegativeButton(R.string.cancel, null)
            .create()
    }
}
