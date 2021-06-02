package com.example.appyhighvideocall.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.lang.Exception

class Repository {

    private val fireStore by lazy {
        Firebase.firestore
    }

    suspend fun getCurrentActiveToken() : TokenInfo? {
        return withContext(Dispatchers.IO) {
            try {
                fireStore.collection("tokens").get().await().toObjects(TokenInfo::class.java)[0]
            }catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }


}