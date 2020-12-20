package com.diayansiat.cognition

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.diayansiat.cognition.models.BoardSize
import com.diayansiat.cognition.models.MemoryCard
import com.squareup.picasso.Picasso
import kotlin.math.min

class MemoryBoardAdapter(
    private val context: Context,
    private val boardSize: BoardSize,
    private val cards: List<MemoryCard>,
    private val onCardClickListener: CardClickListener
) :
    RecyclerView.Adapter<MemoryBoardAdapter.ViewHolder>() {

    companion object {
        private const val MARGIN_SIZE = 10
        private const val TAG = "MemoryBoardAdapter"
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val cardWidth = parent.width / boardSize.getWidth() - (2 * MARGIN_SIZE)
        val cardLength = parent.height / boardSize.getHeight() - (2 * MARGIN_SIZE)
        val cardSideLength = min(cardWidth, cardLength)

        val view = LayoutInflater.from(context).inflate(R.layout.memory_card, parent, false)
        val layoutParams = view.findViewById<CardView>(R.id.cardView).layoutParams as ViewGroup.MarginLayoutParams

        layoutParams.width = cardSideLength
        layoutParams.height = cardSideLength
        layoutParams.setMargins(MARGIN_SIZE, MARGIN_SIZE, MARGIN_SIZE, MARGIN_SIZE)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = boardSize.numCards

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        private val imageButton = itemView.findViewById<ImageButton>(R.id.imageButton)
        fun bind(position: Int) {
            val memoryCard = cards[position]
            if (memoryCard.isFaceUP) {
                if(memoryCard.imageUrl != null) {
                    Picasso.get().load(memoryCard.imageUrl).into(imageButton)
                }else {
                    imageButton.setImageResource(memoryCard.identifier)
                }
            }else {
                imageButton.setImageResource(R.drawable.ic_launcher_background)
            }
            imageButton.alpha = if(memoryCard.isMatched) .4f else 1f
            val colorStateList = if (memoryCard.isMatched ) ContextCompat.getColorStateList(context, R.color.color_gray) else null
            ViewCompat.setBackgroundTintList(imageButton, colorStateList)
            imageButton.setOnClickListener {
                onCardClickListener.onCardClicked(position)
            }
        }
    }

    interface CardClickListener{
        fun onCardClicked(position: Int)
    }
}