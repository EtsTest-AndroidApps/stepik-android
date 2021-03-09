package org.stepic.droid.ui.adapters

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_social.view.*
import org.stepic.droid.R
import org.stepic.droid.social.SocialMedia
import ru.nobird.android.view.base.ui.extension.inflate

class SocialLinksAdapter(
    private val socialLinks: Array<SocialMedia> = SocialMedia.values(),
    private val onClick: (SocialMedia) -> Unit
) : RecyclerView.Adapter<SocialLinksAdapter.SocialViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SocialViewHolder =
        SocialViewHolder(parent.inflate(R.layout.item_social, false))

    override fun getItemCount(): Int =
        socialLinks.size

    override fun onBindViewHolder(holder: SocialViewHolder, position: Int) {
        val socialType = socialLinks[position]
        holder.image.setImageDrawable(AppCompatResources.getDrawable(holder.itemView.context, socialType.drawable))
        holder.itemView.setOnClickListener { onClick(socialType)}
    }

    inner class SocialViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.social_item
    }
}