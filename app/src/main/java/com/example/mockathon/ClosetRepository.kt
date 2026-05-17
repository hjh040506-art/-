package com.example.mockathon

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.util.UUID

class ClosetRepository(context: Context) {

    private val deviceId: String =
        android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        )

    private val storage = FirebaseStorage.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private fun collection(name: String) =
        firestore.collection("devices").document(deviceId).collection(name)

    fun saveToFirebase(bitmap: Bitmap, tags: String, collectionName: String, onComplete: (Boolean) -> Unit) {
        val storageRef = storage.reference.child("$deviceId/$collectionName/${UUID.randomUUID()}.png")

        storageRef.putBytes(compressBitmap(bitmap))
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    val item = hashMapOf(
                        "imageUrl" to uri.toString(),
                        "tags" to tags,
                        "timestamp" to System.currentTimeMillis()
                    )
                    collection(collectionName).add(item)
                        .addOnSuccessListener { onComplete(true) }
                        .addOnFailureListener { e -> Log.e("Repository", "저장 실패", e); onComplete(false) }
                }
            }
            .addOnFailureListener { e -> Log.e("Repository", "업로드 실패", e); onComplete(false) }
    }

    fun getAllClothes(onResult: (List<Map<String, Any>>) -> Unit, onError: ((Exception) -> Unit)? = null) {
        collection("closet")
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
            .addOnFailureListener { e -> onError?.invoke(e) ?: onResult(emptyList()) }
    }

    fun getWishlist(onResult: (List<Map<String, Any>>) -> Unit, onError: ((Exception) -> Unit)? = null) {
        // ✅ collection() 헬퍼만 사용, 옛날 경로 완전 제거
        collection("wishlist")
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

    fun deleteItem(collectionName: String, docId: String, imageUrl: String, onComplete: (Boolean) -> Unit) {
        if (imageUrl.isEmpty()) {
            deleteFirestoreDoc(collectionName, docId, onComplete)
            return
        }
        try {
            storage.getReferenceFromUrl(imageUrl).delete()
                .addOnSuccessListener { deleteFirestoreDoc(collectionName, docId, onComplete) }
                .addOnFailureListener { e ->
                    Log.w("Repository", "Storage 삭제 실패, Firestore는 계속 삭제: $imageUrl", e)
                    deleteFirestoreDoc(collectionName, docId, onComplete)
                }
        } catch (e: Exception) {
            Log.w("Repository", "Storage URL 파싱 실패, Firestore만 삭제: $imageUrl", e)
            deleteFirestoreDoc(collectionName, docId, onComplete)
        }
        // ✅ 마지막 중복 호출 제거
    }

    private fun deleteFirestoreDoc(collectionName: String, docId: String, onComplete: (Boolean) -> Unit) {
        // ✅ collection() 헬퍼만 사용, 옛날 경로 완전 제거
        collection(collectionName).document(docId).delete()
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { e ->
                Log.e("Repository", "삭제 실패: $docId", e)
                onComplete(false)
            }
    }

    private fun compressBitmap(bitmap: Bitmap): ByteArray {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
        return baos.toByteArray()
    }
}

private fun Any?.isNullOrEmpty(): Boolean {
    return this == null || this.toString().isEmpty()
}