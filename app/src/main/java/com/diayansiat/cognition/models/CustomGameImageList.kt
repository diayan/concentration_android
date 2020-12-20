package com.diayansiat.cognition.models

import com.google.firebase.firestore.PropertyName

data class CustomGameImageList(
    @PropertyName("images") val images: List<String>? = null
)
