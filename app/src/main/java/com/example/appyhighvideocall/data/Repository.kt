package com.example.appyhighvideocall.data

import android.util.Log
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

    //retrieves the active token from the firebase firestore
    suspend fun getCurrentActiveToken() : TokenInfo? {
        return withContext(Dispatchers.IO) {
            Log.d("RepositoryFire", "Thread is: ${Thread.currentThread().name}")
            try {
                fireStore.collection("tokens").get().await().toObjects(TokenInfo::class.java)[0]
            }catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }


}