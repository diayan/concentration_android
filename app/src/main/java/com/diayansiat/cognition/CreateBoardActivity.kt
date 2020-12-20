package com.diayansiat.cognition


import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.diayansiat.cognition.models.BoardSize
import com.diayansiat.cognition.utils.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.io.ByteArrayOutputStream


class CreateBoardActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "CreateBoardActivity"
        private const val PICK_PHOTO_CODE = 2071
        private const val READ_EXTERNAL_PHOTOS_CODE = 2072
        private const val MIN_GAME_LENGTH = 3
        private const val MAX_GAME_LENGTH = 14
        private const val READ_PHOTOS_PERMISSION = android.Manifest.permission.READ_EXTERNAL_STORAGE
    }

    private lateinit var rvImagePicker: RecyclerView
    private lateinit var etGameName: EditText
    private lateinit var btnSave: Button
    private lateinit var pbUploading: ProgressBar

    private lateinit var adapter: ImagePickerAdapter
    private lateinit var boardSize: BoardSize
    private var numImagesRequired = -1
    private val chosenImageUris = mutableListOf<Uri>()
    private val storage = Firebase.storage
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_board)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        rvImagePicker = findViewById(R.id.rvImagePicker)
        etGameName = findViewById(R.id.etGameName)
        btnSave = findViewById(R.id.btnSave)
        pbUploading = findViewById(R.id.pbUploading)

        boardSize = intent.getSerializableExtra(EXTRA_BOARD_SIZE) as BoardSize
        numImagesRequired = boardSize.getNumCardPairs()
        supportActionBar?.title = "Choose pics (0 / ${numImagesRequired})"

        btnSave.setOnClickListener {
            //so that other players around the world can play your custom game
            saveDataToFirebase()
        }
        //general engineering practice, here we are validating user input to make sure it is not more than 14
        etGameName.filters = arrayOf(InputFilter.LengthFilter(MAX_GAME_LENGTH))

        etGameName.addTextChangedListener(object : TextWatcher{
            override fun afterTextChanged(p0: Editable?) {
               btnSave.isEnabled = shouldEnableSaveButton()
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

        })

        adapter = ImagePickerAdapter(this, chosenImageUris, boardSize, object: ImagePickerAdapter.ImageClickListener{
            override fun onPlaceHolderClicked() {
                //launch intent to pick photos [implicit intents]
                if(isPermissionGranted(this@CreateBoardActivity, READ_PHOTOS_PERMISSION)) {
                    launchIntentForPhotos()
                }else{
                    requestPermission(this@CreateBoardActivity, READ_PHOTOS_PERMISSION, READ_EXTERNAL_PHOTOS_CODE)
                }
            }

        })
        rvImagePicker.adapter = adapter
        rvImagePicker.setHasFixedSize(true)
        rvImagePicker.layoutManager = GridLayoutManager(this, boardSize.getWidth())
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == READ_EXTERNAL_PHOTOS_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                launchIntentForPhotos()
            }else{
                Toast.makeText(this, "In order to make a custom game, you need to provide access to your photos", Toast.LENGTH_LONG).show()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != PICK_PHOTO_CODE || resultCode != Activity.RESULT_OK || data == null) {
            Log.w(TAG, "Did not get data back from the launched activity. User likely canceled from the selection flow")
            return
        }
        val selectedUri = data.data //handles single selections
        val clipData = data.clipData //handles multiple images
        if (clipData != null) {
            Log.i(TAG, "ClipData num images ${clipData.itemCount}: $clipData")
            for (i in 0 until clipData.itemCount) {
                val clipItem = clipData.getItemAt(i)
                if (chosenImageUris.size < numImagesRequired) {
                    chosenImageUris.add(clipItem.uri)
                }
            }
        }else if (selectedUri != null) {
            Log.i(TAG, "data: $selectedUri")
            chosenImageUris.add(selectedUri)
        }
        adapter.notifyDataSetChanged()
        supportActionBar?.title = "Choose pics (${chosenImageUris.size} / $numImagesRequired)"
        btnSave.isEnabled = shouldEnableSaveButton()
    }

    private fun shouldEnableSaveButton(): Boolean {
        // Check if we should enable save button or not
        if (chosenImageUris.size != numImagesRequired){
            return false
        }

        if (etGameName.text.isBlank() || etGameName.text.length < MIN_GAME_LENGTH) {
            return false
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun launchIntentForPhotos() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(Intent.createChooser(intent, "Choose pics"), PICK_PHOTO_CODE)
    }

    private fun saveDataToFirebase() {
        //disable button after user clicks on save
        btnSave.isEnabled = false
        val customGameName = etGameName.text.toString()
        //check and see if game name already exists, if it does, don't create with same name
        db.collection("games").document(customGameName).get().addOnSuccessListener { documentSnapshot ->
            if(documentSnapshot != null && documentSnapshot.data != null) {
                AlertDialog.Builder(this)
                    .setTitle("Name taken")
                    .setMessage("A game with this name $customGameName already exists. Please choose another.")
                    .setPositiveButton("OK", null)
                    .show()
                //enable save button again if the name of the game already exists
                btnSave.isEnabled = true
            }else {
                //Go ahead and create it
                handleImageUploading(customGameName)
            }
        }.addOnFailureListener { exception ->
            btnSave.isEnabled = true
            Log.e(TAG, "Encountered error while saving memory game.", exception)
            Toast.makeText(this, "Encountered error while saving memory game.", Toast.LENGTH_LONG).show()
        }
    }

    private fun handleImageUploading(gameName: String) {
        pbUploading.visibility = View.VISIBLE //make progress bar visible
        var didEncounterError = false
        val uploadedImageUrls = mutableListOf<String>()

        Log.i(TAG, "SaveDataToFirebase")
        //Going to downscale the images cause images on your phone are typically large, so to upload, we downscale
        for((index, photoUri) in chosenImageUris.withIndex()){
            val imageByteArray = getImageByteArray(photoUri)
            val filePath = "images/$gameName/${System.currentTimeMillis()-index}.jpeg" //where the image will be store in firestore
            val photoReference = storage.reference.child(filePath)
            photoReference.putBytes(imageByteArray)
                .continueWithTask { photoUploadTask ->
                    Log.i(TAG, "Upload bytes: ${photoUploadTask.result?.bytesTransferred}")

                    //once upload is completed, get corresponding download url
                    photoReference.downloadUrl
                }.addOnCompleteListener { downloadUrlTask ->
                    if(!downloadUrlTask.isSuccessful) {
                        Log.i(TAG, "Exception with firebase storage: ", downloadUrlTask.exception)
                        Toast.makeText(this, "Failed to upload image", Toast.LENGTH_LONG).show()

                        didEncounterError = true
                        return@addOnCompleteListener
                    }

                    if(didEncounterError) {
                        pbUploading.visibility = View.GONE
                        return@addOnCompleteListener
                    }

                    //otherwise we got a download url
                    val downloadUrl = downloadUrlTask.result.toString()
                    uploadedImageUrls.add(downloadUrl)
                    //update progress as images are being uploaded. multiplied by 100 because it has to be between 0 and 100
                    pbUploading.progress = uploadedImageUrls.size * 100 / chosenImageUris.size
                    Log.i(TAG, "Finished uploading $photoUri, num uploaded ${uploadedImageUrls.size}")

                    if (uploadedImageUrls.size == chosenImageUris.size) {
                        handleAllImagesUploaded(gameName, uploadedImageUrls)
                    }
                }
        }    }

    private fun handleAllImagesUploaded(gameName: String, imageUrls: MutableList<String>) {
        //this is where we are now going to upload the urls and the game name to the fire store
        db.collection("games").document(gameName)
            .set(mapOf("images" to imageUrls))
            .addOnCompleteListener { gameCreationTask ->
                //set upload progress bar to gone once the network operation is complete
                pbUploading.visibility = View.GONE
                if(!gameCreationTask.isSuccessful) {
                    Log.e(TAG, "Exception with game creation", gameCreationTask.exception)
                    Toast.makeText(this, "Failed game creation", Toast.LENGTH_LONG).show()
                    return@addOnCompleteListener
                }
                Log.i(TAG, "Successfully created game $gameName")
                AlertDialog.Builder(this)
                    .setTitle("Upload complete! Let's play your game '$gameName'")
                    .setPositiveButton("Ok") { _, _ ->
                        val resultData = Intent()
                        resultData.putExtra(EXTRA_GAME_NAME, gameName)
                        setResult(Activity.RESULT_OK, resultData)
                        finish()
                    }.show()
            }

    }

    private fun getImageByteArray(photoUri: Uri): ByteArray {
        //get original bitmap of the photoo
        val originalBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(contentResolver, photoUri)
            ImageDecoder.decodeBitmap(source)
        }else {
            MediaStore.Images.Media.getBitmap(contentResolver, photoUri)
        }

        Log.i(TAG, "Original width: ${originalBitmap.width} and height: ${originalBitmap.height}")
        val scaledBitmap = BitmapScaler.scaleToFitHeight(originalBitmap, 250)
        Log.i(TAG, "Scaled width: ${scaledBitmap.width} and height: ${scaledBitmap.height}")

        val byteOutputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 60, byteOutputStream)
        return byteOutputStream.toByteArray()

    }
}