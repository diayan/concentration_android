package com.diayansiat.cognition.models

enum class BoardSize(val numCards: Int) {
    EASY(8),
    MEDIUM(18),
    HARD(24);

    companion object {
        //the mode of the game i.e EASY, MEDIUM and HARD based on the number of cards passed into this method
        fun getByValue(value: Int) = values().first{ it.numCards == value }
    }

    fun getWidth(): Int{
        //this refers to the board size ie EASY, MEDIUM or HARD
        return when(this) {
            EASY -> 2
            MEDIUM -> 3
            HARD -> 4
        }
    }

    fun getHeight(): Int {
        return numCards / getWidth()
    }

    //number of pairs of cards
    fun getNumCardPairs(): Int{
        return numCards / 2
    }
}