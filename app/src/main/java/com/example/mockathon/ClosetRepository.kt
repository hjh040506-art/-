package com.example.mockathon

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.util.UUID

class ClosetRepository {
    private val storage = FirebaseStorage.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    fun saveToFirebase(bitmap: Bitmap, tags: String, collectionName: String, onComplete: (Boolean) -> Unit) {
        val data = compressBitmap(bitmap)
        val storageRef = storage.reference.child("$collectionName/${UUID.randomUUID()}.jpg")

        storageRef.putBytes(data)
            .addOnSuccessListener {
                storageRef.downloadUrl
                    .addOnSuccessListener { uri ->
                        val item = hashMapOf(
                            "imageUrl" to uri.toString(),
                            "tags" to tags,
                            "timestamp" to System.currentTimeMillis()
                        )
                        firestore.collection(collectionName).add(item)
                            .addOnSuccessListener { onComplete(true) }
                            .addOnFailureListener { e ->
                                Log.e("Repository", "Firestore 저장 실패", e)
                                onComplete(false)
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.e("Repository", "다운로드 URL 취득 실패", e)
                        onComplete(false)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("Repository", "Storage 업로드 실패", e)
                onComplete(false)
            }
    }

    fun getAllClothes(
        onResult: (List<Map<String, Any>>) -> Unit,
        onError: ((Exception) -> Unit)? = null
    ) {
        firestore.collection("closet")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                val list = result.documents.mapNotNull { doc ->
                    val data = doc.data?.toMutableMap() ?: return@mapNotNull null
                    if (data["imageUrl"].isNullOrEmpty()) return@mapNotNull null
                    data["id"] = doc.id
                    data
                }
                onResult(list)
            }
            .addOnFailureListener { e ->
                Log.e("Repository", "옷장 불러오기 실패", e)
                onError?.invoke(e) ?: onResult(emptyList())
            }
    }

    fun getWishlist(
        onResult: (List<Map<String, Any>>) -> Unit,
        onError: ((Exception) -> Unit)? = null
    ) {
        firestore.collection("wishlist")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                val list = result.documents.mapNotNull { doc ->
                    val data = doc.data?.toMutableMap() ?: return@mapNotNull null
                    if (data["imageUrl"].isNullOrEmpty()) return@mapNotNull null
                    data["id"] = doc.id
                    data
                }
                onResult(list)
            }
            .addOnFailureListener { e ->
                Log.e("Repository", "찜칸 불러오기 실패", e)
                onError?.invoke(e) ?: onResult(emptyList())
            }
    }

    // Storage 이미지 먼저 삭제 → Firestore 문서 삭제
    fun deleteItem(
        collectionName: String,
        docId: String,
        imageUrl: String,
        onComplete: (Boolean) -> Unit
    ) {
        if (imageUrl.isEmpty()) {
            deleteFirestoreDoc(collectionName, docId, onComplete)
            return
        }
        try {
            storage.getReferenceFromUrl(imageUrl).delete()
                .addOnSuccessListener {
                    deleteFirestoreDoc(collectionName, docId, onComplete)
                }
                .addOnFailureListener { e ->
                    // Storage 삭제 실패해도 Firestore는 삭제 (고아 문서 방지)
                    Log.w("Repository", "Storage 삭제 실패, Firestore는 계속 삭제: $imageUrl", e)
                    deleteFirestoreDoc(collectionName, docId, onComplete)
                }
        } catch (e: Exception) {
            Log.w("Repository", "Storage URL 파싱 실패, Firestore만 삭제: $imageUrl", e)
            deleteFirestoreDoc(collectionName, docId, onComplete)
        }
    }

    private fun deleteFirestoreDoc(
        collectionName: String,
        docId: String,
        onComplete: (Boolean) -> Unit
    ) {
        firestore.collection(collectionName).document(docId).delete()
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { e ->
                Log.e("Repository", "Firestore 삭제 실패: $docId", e)
                onComplete(false)
            }
    }

    private fun compressBitmap(bitmap: Bitmap): ByteArray {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
        return baos.toByteArray()
    }
}

private fun Any?.isNullOrEmpty(): Boolean {
    return this == null || this.toString().isEmpty()
}