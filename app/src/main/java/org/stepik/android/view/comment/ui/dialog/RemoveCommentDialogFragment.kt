package org.stepik.android.view.comment.ui.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.stepic.droid.R
import ru.nobird.android.view.base.ui.extension.argument

class RemoveCommentDialogFragment : DialogFragment() {
    companion object {
        const val TAG = "RemoveCommentDialogFragment"

        fun newInstance(commentId: Long): DialogFragment =
            RemoveCommentDialogFragment().also {
                it.commentId = commentId
            }
    }

    private var commentId by argument<Long>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.title_confirmation)
            .setMessage(R.string.comment_action_remove_description)
            .setPositiveButton(R.string.delete_label) { _, _ ->
                (activity as? Callback
                    ?: parentFragment as? Callback
                    ?: targetFragment as? Callback)
                    ?.onDeleteComment(commentId)
            }
            .setNegativeButton(R.string.cancel, null)
            .create()

    interface Callback {
        fun onDeleteComment(commentId: Long)
    }
}