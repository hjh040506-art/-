package com.example.mockathon
//데이터베이스에 접근하는 함수
import android.graphics.Bitmap
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.util.UUID

class ClosetRepository {
    private val storage = FirebaseStorage.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    fun saveClothing(bitmap: Bitmap, tags: String, onComplete: (Boolean) -> Unit) {
        val data = compressBitmap(bitmap)
        val storageRef = storage.reference.child("clothes/${UUID.randomUUID()}.jpg")

        storageRef.putBytes(data).addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                val clothingItem = hashMapOf(
                    "imageUrl" to uri.toString(),
                    "tags" to tags,
                    "timestamp" to System.currentTimeMillis()
                )
                FirebaseFirestore.getInstance().collection("closet").add(clothingItem)
                    .addOnSuccessListener { onComplete(true) } // 성공 시 true
                    .addOnFailureListener { onComplete(false) } // 실패 시 false
            }
        }.addOnFailureListener { onComplete(false) } // Storage 업로드 실패 시
    }

    fun getAllClothes(onResult: (List<Map<String, Any>>) -> Unit) {
        FirebaseFirestore.getInstance().collection("closet")
            .orderBy("timestamp", Query.Direction.DESCENDING) // 최신순 정렬
            .get()
            .addOnSuccessListener { result ->
                val clothesList = result.documents.mapNotNull { it.data }
                onResult(clothesList)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }
    private fun compressBitmap(bitmap: Bitmap): ByteArray {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
        return baos.toByteArray()
    }
}
