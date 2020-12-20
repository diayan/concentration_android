package com.diayansiat.cognition

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.diayansiat.cognition.models.BoardSize
import kotlin.math.min

class ImagePickerAdapter(
    private val context: Context,
    private val imageUris: List<Uri>,
    private val boardSize: BoardSize,
    private val imageClickListener: ImageClickListener
) : RecyclerView.Adapter<ImagePickerAdapter.ViewHolder>() {

    interface ImageClickListener{
        fun onPlaceHolderClicked()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.card_image_item, parent, false)
        parent.width / boardSize.getWidth()
        val cardHeight =  parent.height / boardSize.getHeight()
        val cardSideLength = min(cardHeight, cardHeight)
        val layoutParams = view.findViewById<ImageView>(R.id.icCustomImageView).layoutParams
        layoutParams.width = cardSideLength
        layoutParams.height = cardSideLength
        return ViewHolder(view)
    }

    override fun getItemCount() = boardSize.getNumCardPairs()

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position  < imageUris.size) {

            holder.bind(imageUris[position])
        }else {
            holder.bind()
        }
    }

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val ivCustomImage = itemView.findViewById<ImageView>(R.id.icCustomImageView)

        fun bind(uri: Uri) {
            ivCustomImage.setImageURI(uri)
            ivCustomImage.setOnClickListener(null)
        }

        fun bind(){
            ivCustomImage.setOnClickListener {
                imageClickListener.onPlaceHolderClicked()
            }
        }
    }
}
