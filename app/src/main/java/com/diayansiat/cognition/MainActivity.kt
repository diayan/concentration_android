package com.diayansiat.cognition

import android.animation.ArgbEvaluator
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.diayansiat.cognition.models.BoardSize
import com.diayansiat.cognition.models.CustomGameImageList
import com.diayansiat.cognition.models.MemoryGame
import com.diayansiat.cognition.utils.EXTRA_BOARD_SIZE
import com.diayansiat.cognition.utils.EXTRA_GAME_NAME
import com.github.jinatonic.confetti.CommonConfetti
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*

private const val CREATE_BOARD_REQUEST_CODE = 2481

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var memoryGame: MemoryGame
    private lateinit var rvBoard: RecyclerView
    private lateinit var adapter: MemoryBoardAdapter
    private lateinit var tvNumMoves: TextView
    private lateinit var tvNumPairs: TextView

    private val db = Firebase.firestore
    private var gameName: String? = null
    private var customGameImages: List<String>? = null
    private var boardSize: BoardSize = BoardSize.MEDIUM

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        rvBoard = findViewById(R.id.rvBoard)
        tvNumMoves = findViewById(R.id.tvNumMoves)
        tvNumPairs = findViewById(R.id.tvNumPairs)
        setupGameBoard()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.mi_refresh -> {
                //warn user that they are about to lose their progress if they make 1 match in three moves
                if (memoryGame.getNumMoves() > 0 && !memoryGame.haveWonGame()) {
                    showAlertDialog("Quit your current game?", null, View.OnClickListener {
                        setupGameBoard()
                    })
                } else {
                    //restart the game again
                    setupGameBoard()
                }
                return true
            }

            R.id.mi_new_size -> {
                showNewSizeDialog()
                return true
            }

            R.id.mi_custom -> {
                showCreationDialog()
            }

            R.id.mi_download -> {
                showDownloadDialog()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    //this allows user to enter name of the game they want to download
    private fun showDownloadDialog() {
        val boardDownloadView =
            LayoutInflater.from(this).inflate(R.layout.dialog_download_board, null)
        showAlertDialog("Fetch memory game", boardDownloadView, View.OnClickListener {
            //grab the text of the game name that the user wants to download
            val etDownloadGame = boardDownloadView.findViewById<EditText>(R.id.etDownloadGame)
            val gameToDownload = etDownloadGame.text.toString().trim()
            downloadGame(gameToDownload)
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == CREATE_BOARD_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            //get game name
            val customGameName = data?.getStringExtra(EXTRA_GAME_NAME)
            if (customGameName == null) {
                Log.e(TAG, "Got null custom game name")
                return
            } else {
                downloadGame(customGameName)
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun downloadGame(customGameName: String) {
        db.collection("games").document(customGameName).get()
            .addOnSuccessListener { documentSnapshot ->
                val customGameImageList =
                    documentSnapshot.toObject(CustomGameImageList::class.java) //convert data from fire store to an object
                if (customGameImageList?.images == null) {
                    Log.e(TAG, "Invalid custom game data from fire store")
                    Snackbar.make(
                        clRoot,
                        "Sorry we couldn't find any such game '$customGameName'",
                        Snackbar.LENGTH_LONG
                    ).show()
                    return@addOnSuccessListener
                }
                val numCards = customGameImageList.images.size * 2
                boardSize = BoardSize.getByValue(numCards)
                gameName = customGameName
                customGameImages = customGameImageList.images
                //pre-fetch all the images to avoid them taking longer to download
                for (imageUrl in customGameImageList.images) {
                    Picasso.get().load(imageUrl).fetch()
                }
                Snackbar.make(
                    clRoot,
                    "You are now playing '$customGameName'!",
                    Snackbar.LENGTH_LONG
                ).show()
                setupGameBoard()

            }.addOnFailureListener { exception ->
                Log.e(TAG, "Exception when retrieving game", exception)
            }
    }

    //creating a custom game
    private fun showCreationDialog() {
        //create a radio group for the user to choose the game they want to play
        val boardSizeView = LayoutInflater.from(this).inflate(R.layout.dialog_board_size, null)
        val radioGroupSize = boardSizeView.findViewById<RadioGroup>(R.id.radioGroup)

        showAlertDialog("Create your own memory board.", boardSizeView, View.OnClickListener {
            //List of all board sizes. Set a new value of board size when the user taps ok
            val desiredBoardSize = when (radioGroupSize.checkedRadioButtonId) {
                R.id.rbEasy -> BoardSize.EASY
                R.id.rbMedium -> BoardSize.MEDIUM
                R.id.rbHard -> BoardSize.HARD
                else -> BoardSize.HARD
            }
            //navigate to new activity where users can choose their game
            val intent = Intent(this, CreateBoardActivity::class.java)
            intent.putExtra(EXTRA_BOARD_SIZE, desiredBoardSize)
            startActivityForResult(intent, CREATE_BOARD_REQUEST_CODE)
        })
    }

    private fun showNewSizeDialog() {
        //create a radio group for the user to choose the game they want to play
        val boardSizeView = LayoutInflater.from(this).inflate(R.layout.dialog_board_size, null)
        val radioGroupSize = boardSizeView.findViewById<RadioGroup>(R.id.radioGroup)
        when (boardSize) {
            BoardSize.EASY -> radioGroupSize.check(R.id.rbEasy)
            BoardSize.MEDIUM -> radioGroupSize.check(R.id.rbMedium)
            BoardSize.HARD -> radioGroupSize.check(R.id.rbHard)
        }

        showAlertDialog("Choose new size.", boardSizeView, View.OnClickListener {
            //List of all board sizes. Set a new value of board size when the user taps ok
            boardSize = when (radioGroupSize.checkedRadioButtonId) {
                R.id.rbEasy -> BoardSize.EASY
                R.id.rbMedium -> BoardSize.MEDIUM
                R.id.rbHard -> BoardSize.HARD
                else -> BoardSize.HARD
            }
            gameName = null
            customGameImages = null
            setupGameBoard()
        })
    }

    private fun showAlertDialog(
        title: String,
        view: View?,
        positiveClickListener: View.OnClickListener
    ) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(view)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Ok") { _, _ ->
                //Action when user taps "ok"
                positiveClickListener.onClick(null)
            }.show()
    }

    private fun setupGameBoard() {
        //change title of app to the game name
        supportActionBar?.title = gameName ?: getString(R.string.app_name)
        when (boardSize) {
            BoardSize.EASY -> {
                tvNumMoves.text = "Easy: 4 x 2"
                tvNumPairs.text = "Pairs: 0 / 4"
            }
            BoardSize.MEDIUM -> {
                tvNumMoves.text = "Easy: 6 x 3"
                tvNumPairs.text = "Pairs: 0 / 9"
            }
            BoardSize.HARD -> {
                tvNumMoves.text = "Easy: 6 x 4"
                tvNumPairs.text = "Pairs: 0 / 12"
            }
        }

        tvNumPairs.setTextColor(ContextCompat.getColor(this, R.color.color_progress_none))
        memoryGame = MemoryGame(boardSize, customGameImages)

        adapter = MemoryBoardAdapter(
            this,
            boardSize,
            memoryGame.cards,
            object : MemoryBoardAdapter.CardClickListener {
                override fun onCardClicked(position: Int) {
                    updateGameWithFlip(position)
                }
            })
        rvBoard.adapter = adapter
        rvBoard.setHasFixedSize(true)
        rvBoard.layoutManager = GridLayoutManager(this, boardSize.getWidth())
    }

    private fun updateGameWithFlip(position: Int) {
        if (memoryGame.haveWonGame()) {
            //Alert the user of invalid move
            Snackbar.make(clRoot, "You have won the game!", Snackbar.LENGTH_LONG).show()
            return
        }
        if (memoryGame.isCardFacedUp(position)) {
            //Alert the user of an invalid move
            Snackbar.make(clRoot, "Invalid move!", Snackbar.LENGTH_SHORT).show()
            return
        }
        //flip over the cards
        if (memoryGame.flipCard(position)) {
            Log.i(TAG, "Found a match! Num pairs found: ${memoryGame.numPairsFound}")
            //change color of text field as it grows
            val color = ArgbEvaluator().evaluate(
                memoryGame.numPairsFound.toFloat() / boardSize.getNumCardPairs(),
                ContextCompat.getColor(this, R.color.color_progress_none),
                ContextCompat.getColor(this, R.color.color_progress_full)
            ) as Int
            tvNumPairs.setTextColor(color)
            tvNumPairs.text = "Pairs: ${memoryGame.numPairsFound} / ${boardSize.getNumCardPairs()}"
            if (memoryGame.haveWonGame()) {
                Snackbar.make(clRoot, "You won! Congratulations!", Snackbar.LENGTH_LONG).show()
                for (i in 0..4) {
                    CommonConfetti.rainingConfetti(
                        clRoot,
                        intArrayOf(Color.RED, Color.GREEN, Color.MAGENTA, Color.YELLOW)
                    ).oneShot()
                }
            }
        }
        tvNumMoves.text = "Moves: ${memoryGame.getNumMoves()}"
        adapter.notifyDataSetChanged()
    }
}