package com.example.mockathon
//UI와 로직사이에서 데이터 정리
import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mockathon.Const.GEMINI_API_KEY
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class ClothingViewModel : ViewModel() {
    private val generativeModel = GenerativeModel("gemini-2.5-flash", GEMINI_API_KEY)
    val repository = ClosetRepository() // Repository 연결
    var resultText by mutableStateOf("분석 결과가 여기에 나타납니다.")
        private set
    var selectedImage by mutableStateOf<Bitmap?>(null)
        private set
    // 분석 상태를 판단하는 속성들을 추가합니다.
    val isAnalyzing: Boolean get() = resultText == "분석 중..."
    // 분석이 끝났는지 판단
    val isAnalysisFinished: Boolean get() = resultText != "분석 중..." && resultText.isNotEmpty()
    // 옷인지 아닌지 판단 ("이전"이라는 단어가 들어있으면 옷이 아님)
    val isClothing: Boolean get() = !resultText.contains("이전", ignoreCase = true)
    private val db = FirebaseFirestore.getInstance()
    fun analyzeImage(bitmap: Bitmap) {
        selectedImage = bitmap
        resultText = "분석 중..."
        performAiAnalysis(bitmap)
    }
    private fun performAiAnalysis(bitmap: Bitmap) {
        viewModelScope.launch {
            try {
                val response = generativeModel.generateContent(content {
                    image(bitmap)
                    text("이 옷의 종류, 색깔, 계절, 스타일, 컨셉을 키워드만 따서 출력해줘,예를 들면 #긴팔후드티\\n #검은색\\n #가을,겨울\\n #캐주얼\\n, #데이트룩 \" +\n" +
                            "\"이런식으로 총 5줄이 되어야해 추가적으로 #컵셉 부분은 좀 재밌게 예를들면 #남친이벤트용 #여친생김, #길가다번호따임, #찐따, #공대생\" +\n" +
                            "\"만약 업로드한 사진이 옷이 아니야 그러면 재치있게 에를 들어 이건 옷이 아니라 강아지잖아요 이전을 눌러 다시 업로드 해주세요 이런식으로 출력해줘 이전을 누르라는 말은 꼭 들어가야해\"") // 기존 프롬프트 유지
                })
                resultText = response.text ?: "분석 실패"
            } catch (e: Exception) {
                resultText = "에러 발생: ${e.message}"
            }
        }
    }

    // 이제 한 줄로 끝납니다!
    var isSaving by mutableStateOf(false) // 저장 중인지 확인하는 상태

    fun saveToCloset(bitmap: Bitmap, tags: String, onComplete: (Boolean) -> Unit) {
        isSaving = true // 저장 시작
        repository.saveClothing(bitmap, tags) { success ->
            isSaving = false // 저장 종료
            onComplete(success) // 결과 전달
        }
    }
    fun resetToInitial() {
        selectedImage = null
        resultText = "분석 결과가 여기에 나타납니다."
    }
    fun loadAllClothes(onResult: (List<Map<String, Any>>) -> Unit) {
        db.collection("closet").get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.map { doc ->
                    // 데이터에 'id' 필드가 없어도 문서 ID를 넣어줌
                    val data = doc.data?.toMutableMap() ?: mutableMapOf()
                    data["id"] = doc.id
                    data
                }
                onResult(list)
            }
    }
    fun deleteClothes(ids: Set<String>, onComplete: () -> Unit) {
        val db = FirebaseFirestore.getInstance()
        var count = 0
        ids.forEach { id ->
            db.collection("closet").document(id).delete()
                .addOnSuccessListener {
                    count++
                    if (count == ids.size) { // 모든 삭제 완료 후
                        onComplete()
                    }
                }
        }
    }
}


