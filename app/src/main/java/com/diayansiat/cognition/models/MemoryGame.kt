package com.diayansiat.cognition.models

import com.diayansiat.cognition.utils.DEFAULT_ICONS

class MemoryGame(
    private val boardSize: BoardSize,
    private val customGameImages: List<String>?
) {

    val cards: List<MemoryCard>
    var numPairsFound = 0
    private var indexOfSingleSelectedCard: Int? = null
    private var numCardFlips = 0

    init {
        if (customGameImages == null) {
            val chosenImages = DEFAULT_ICONS.shuffled().take(boardSize.getNumCardPairs())
            val randomizedImages = (chosenImages + chosenImages).shuffled()
            //for each image in randomized images, a memory card is going to be created with a default value of isMatched and isFaceUp
            cards = randomizedImages.map { MemoryCard(it) }
        }else {
            val randomizedImages = (customGameImages + customGameImages).shuffled()
            cards = randomizedImages.map { MemoryCard(it.hashCode(), it) } //hashcode converts objects to unique integers
        }
    }

    fun flipCard(position: Int): Boolean {
        numCardFlips++
        val card = cards[position]
        var foundMatch = false
        //Three cases
        //O cards previously flipped => flip over selected card
        //1 card previously flipped => flip over selected card + check if the image matches previously selected card
        //2 cards previously flipped => restore previously selected cards and flip over the currently selected one
        if (indexOfSingleSelectedCard == null) {
            //0 or 2 cards selected?
            restoreCards()
            indexOfSingleSelectedCard = position
        } else {
            foundMatch = checkForMatch(indexOfSingleSelectedCard!!, position)
            indexOfSingleSelectedCard = null
        }
        card.isFaceUP = !card.isFaceUP
        return foundMatch
    }

    private fun checkForMatch(position1: Int, position2: Int): Boolean {
        if (cards[position1].identifier != cards[position2].identifier) {
            return false
        }
        cards[position1].isMatched = true
        cards[position2].isMatched = true
        numPairsFound++
        return true
    }

    private fun restoreCards() {
        for (card in cards) {
            if (!card.isMatched) {
                card.isFaceUP = false
            }
        }
    }

    fun haveWonGame(): Boolean {
        return numPairsFound == boardSize.getNumCardPairs()
    }

    fun isCardFacedUp(position: Int): Boolean {
        return cards[position].isFaceUP
    }

    fun getNumMoves(): Int {
        return numCardFlips / 2
    }
}