package com.example.mockathon

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mockathon.Const.GEMINI_API_KEYS
import com.example.mockathon.Const.REMOVE_BG_API_KEYS
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class ClothingViewModel : ViewModel() {

    // ✅ Gemini 모델 2개 (키별로 생성)
    private val geminiModels = GEMINI_API_KEYS.map { key ->
        GenerativeModel("gemini-2.5-flash", key)
    }

    val repository = ClosetRepository()
    val weatherRepository = WeatherRepository()

    var resultText by mutableStateOf("분석 결과가 여기에 나타납니다.")
    var selectedImage by mutableStateOf<Bitmap?>(null)

    var weatherInfo by mutableStateOf<WeatherInfo?>(null)
    var isWeatherLoading by mutableStateOf(false)
    var weatherError by mutableStateOf(false)

    var isRemovingBackground by mutableStateOf(false)
    var closetStyleTags by mutableStateOf<List<String>>(emptyList())

    fun fetchWeather(lat: Double, lon: Double) {
        viewModelScope.launch {
            isWeatherLoading = true
            weatherError = false
            weatherInfo = weatherRepository.getWeather(lat, lon)
            if (weatherInfo == null) weatherError = true
            isWeatherLoading = false
        }
    }

    // ✅ Gemini 폴백 — 첫 번째 키 실패 시 두 번째 키로 재시도
    private suspend fun <T> generateWithFallback(block: suspend (GenerativeModel) -> T): T {
        var lastException: Exception? = null
        for ((index, model) in geminiModels.withIndex()) {
            try {
                return retryOnServerError { block(model) }
            } catch (e: Exception) {
                Log.w("ViewModel", "Gemini 키 ${index + 1} 실패: ${e.message}")
                lastException = e
            }
        }
        throw lastException!!
    }

    private suspend fun <T> retryOnServerError(maxRetries: Int = 3, block: suspend () -> T): T {
        var lastException: Exception? = null
        repeat(maxRetries) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                val is503 = e.message?.contains("503") == true ||
                        e.message?.contains("UNAVAILABLE") == true ||
                        e.message?.contains("high demand") == true
                if (is503) {
                    lastException = e
                    val delayMs = 1000L * (1 shl attempt)
                    Log.w("ViewModel", "503 오류 → ${attempt + 1}번째 재시도 (${delayMs}ms 후)")
                    kotlinx.coroutines.delay(delayMs)
                } else throw e
            }
        }
        throw lastException!!
    }

    val isAnalyzing: Boolean get() = resultText == "분석 중..."
    val isAnalysisFinished: Boolean get() = resultText != "분석 중..." && resultText.isNotEmpty()
    val isClothing: Boolean get() = !resultText.contains("이전", ignoreCase = true)
    val isError: Boolean get() = resultText.startsWith("에러 발생:")

    // ✅ Remove.bg 폴백 — 첫 번째 키 실패 시 두 번째 키로 재시도
    fun removeBackgroundAndAnalyze(bitmap: Bitmap) {
        isRemovingBackground = true
        resultText = "배경 제거 중..."

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val softwareBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false)

            val baos = ByteArrayOutputStream()
            softwareBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
            val imageBytes = baos.toByteArray()

            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            var resultBitmap: Bitmap? = null
            var lastError: String? = null

            // ✅ Remove.bg 키 순서대로 시도
            for ((index, apiKey) in REMOVE_BG_API_KEYS.withIndex()) {
                try {
                    val requestBody = MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart(
                            "image_file", "image.png",
                            imageBytes.toRequestBody("image/png".toMediaType())
                        )
                        .addFormDataPart("size", "auto")
                        .build()

                    val request = Request.Builder()
                        .url("https://api.remove.bg/v1.0/removebg")
                        .header("X-Api-Key", apiKey)
                        .post(requestBody)
                        .build()

                    val response = client.newCall(request).execute()

                    if (response.isSuccessful) {
                        val responseBytes = response.body?.bytes()
                        if (responseBytes != null) {
                            resultBitmap = BitmapFactory.decodeByteArray(responseBytes, 0, responseBytes.size)
                            Log.d("ViewModel", "Remove.bg 키 ${index + 1} 성공")
                            break
                        }
                    } else {
                        lastError = "Remove.bg 키 ${index + 1} 실패: ${response.code} ${response.message}"
                        Log.w("ViewModel", lastError!!)
                    }
                } catch (e: Exception) {
                    lastError = "Remove.bg 키 ${index + 1} 오류: ${e.message}"
                    Log.w("ViewModel", lastError!!, e)
                }
            }

            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                isRemovingBackground = false
                if (resultBitmap != null) {
                    selectedImage = resultBitmap
                    analyzeImage(resultBitmap)
                } else {
                    // 두 키 모두 실패 시 원본으로 폴백
                    Log.e("ViewModel", "Remove.bg 전체 실패 → 원본으로 폴백. 마지막 오류: $lastError")
                    analyzeImage(softwareBitmap)
                }
            }
        }
    }

    fun analyzeImage(bitmap: Bitmap) {
        selectedImage = bitmap
        resultText = "분석 중..."
        performAiAnalysis(bitmap)
    }

    private fun performAiAnalysis(bitmap: Bitmap) {
        viewModelScope.launch {
            try {
                // ✅ Gemini 폴백 적용
                val response = generateWithFallback { model ->
                    model.generateContent(content {
                        image(bitmap)
                        text("""
                            이 옷 사진을 분석해서 아래 형식에 맞춰 정확히 5줄로만 출력해줘. 다른 말은 하지 마.
                            
                            1줄: 옷 종류 (예: #긴팔후드티, #반팔티셔츠, #청바지, #반바지, #니트 등)
                            2줄: 색깔 (예: #검은색, #흰색, #네이비 등)
                            3줄: 계절 — 반드시 #봄 #여름 #가을 #겨울 중에서 해당하는 것만 골라서 출력해. 복수 가능 (예: #가을 #겨울)
                            4줄: 스타일 (예: #캐주얼, #스트릿, #오피스룩, #스포츠 등)
                            5줄: 컨셉 — 재밌고 공감가게 (예: #남친이벤트용, #길가다번호따임, #공대생, #여친생김)
                            
                            예시 출력:
                            #긴팔후드티
                            #검은색
                            #가을 #겨울
                            #캐주얼
                            #공대생
                            
                            만약 사진이 옷이 아니면 재치있게 한 줄로 설명하고 반드시 "이전을 눌러 다시 업로드 해주세요"라는 말을 포함해줘.
                        """.trimIndent())
                    })
                }
                resultText = response.text ?: "분석 실패"
            } catch (e: Exception) {
                resultText = if (e.message?.contains("503") == true || e.message?.contains("UNAVAILABLE") == true)
                    "서버가 혼잡해요. 잠시 후 다시 시도해주세요."
                else "에러 발생: ${e.message}"
            }
        }
    }

    var isSaving by mutableStateOf(false)

    fun saveToCloset(bitmap: Bitmap, tags: String, onComplete: (Boolean) -> Unit) {
        isSaving = true
        repository.saveToFirebase(bitmap, tags, "closet") { success ->
            isSaving = false
            onComplete(success)
        }
    }

    fun saveToWishlist(bitmap: Bitmap, tags: String, onComplete: (Boolean) -> Unit) {
        isSaving = true
        repository.saveToFirebase(bitmap, tags, "wishlist") { success ->
            isSaving = false
            onComplete(success)
        }
    }

    fun loadWishlist(onResult: (List<Map<String, Any>>) -> Unit, onError: (() -> Unit)? = null) {
        repository.getWishlist(
            onResult = onResult,
            onError = { e -> Log.e("ViewModel", "찜칸 불러오기 실패", e); onError?.invoke() }
        )
    }

    fun resetToInitial() {
        selectedImage = null
        resultText = "분석 결과가 여기에 나타납니다."
    }

    fun loadAllClothes(onResult: (List<Map<String, Any>>) -> Unit, onError: (() -> Unit)? = null) {
        repository.getAllClothes(
            onResult = onResult,
            onError = { e -> Log.e("ViewModel", "옷장 불러오기 실패", e); onError?.invoke() }
        )
    }

    fun loadClosetStyleTags() {
        repository.getAllClothes(
            onResult = { clothes ->
                closetStyleTags = clothes.flatMap { item ->
                    val tags = item["tags"]?.toString() ?: ""
                    val line = tags.lines().getOrNull(3) ?: ""
                    line.split("#").map { it.trim() }.filter { it.isNotEmpty() }
                }.distinct()
            },
            onError = { Log.e("ViewModel", "스타일 태그 로드 실패") }
        )
    }

    fun deleteClothes(
        ids: Set<String>,
        itemsSnapshot: List<Map<String, Any>>,
        onComplete: (successCount: Int, failCount: Int) -> Unit
    ) {
        viewModelScope.launch {
            var successCount = 0
            var failCount = 0
            ids.forEach { id ->
                val imageUrl = itemsSnapshot.find { it["id"] == id }?.get("imageUrl")?.toString() ?: ""
                var done = false
                repository.deleteItem("closet", id, imageUrl) { success ->
                    if (success) successCount++ else failCount++
                    done = true
                }
                while (!done) kotlinx.coroutines.delay(50)
            }
            onComplete(successCount, failCount)
        }
    }

    fun deleteWishlist(
        ids: Set<String>,
        itemsSnapshot: List<Map<String, Any>>,
        onComplete: (successCount: Int, failCount: Int) -> Unit
    ) {
        viewModelScope.launch {
            var successCount = 0
            var failCount = 0
            ids.forEach { id ->
                val imageUrl = itemsSnapshot.find { it["id"] == id }?.get("imageUrl")?.toString() ?: ""
                var done = false
                repository.deleteItem("wishlist", id, imageUrl) { success ->
                    if (success) successCount++ else failCount++
                    done = true
                }
                while (!done) kotlinx.coroutines.delay(50)
            }
            onComplete(successCount, failCount)
        }
    }

    fun getOutfitRecommendation(
        category: String,
        sleeveType: String?,
        pantsType: String?,
        includeWishlist: Boolean = false,
        onResult: (OutfitRecommendation?, String?) -> Unit
    ) {
        viewModelScope.launch {
            repository.getAllClothes(
                onResult = { closetClothes ->
                    val loadWishlistFn: (onResult: (List<Map<String, Any>>) -> Unit, onError: ((Exception) -> Unit)?) -> Unit =
                        if (includeWishlist) repository::getWishlist
                        else { onResult, _ -> onResult(emptyList()) }

                    loadWishlistFn(
                        { wishlistClothes ->
                            val allIds = closetClothes.map { it["id"] }.toSet()
                            val uniqueWishlist = wishlistClothes.filter { it["id"] !in allIds }
                            val allClothes = closetClothes + uniqueWishlist

                            if (allClothes.isEmpty()) {
                                onResult(null, "옷장이 비어있어요!\n먼저 옷을 등록해주세요.")
                                return@loadWishlistFn
                            }

                            val topKeywords = listOf(
                                "티셔츠", "후드티", "셔츠", "니트", "맨투맨", "블라우스",
                                "자켓", "코트", "패딩", "가디건", "상의", "탑", "스웨터", "긴팔", "반팔"
                            )
                            val bottomKeywords = listOf(
                                "바지", "청바지", "슬랙스", "치마", "스커트", "쇼츠",
                                "반바지", "레깅스", "하의", "팬츠", "데님"
                            )

                            val seasonKeywords = when (category) {
                                "여름" -> listOf("여름", "반팔", "민소매", "린넨", "반바지", "쇼츠")
                                "겨울" -> listOf("겨울", "패딩", "코트", "두꺼운", "기모", "니트", "긴팔")
                                "봄", "가을" -> listOf("봄", "가을", "가디건", "얇은", "맨투맨", "긴팔")
                                else -> emptyList()
                            }

                            fun matchesSeason(tags: String): Boolean {
                                if (seasonKeywords.isEmpty()) return true
                                return seasonKeywords.any { tags.contains(it) }
                            }

                            var tops = allClothes.filter { item ->
                                val tags = item["tags"]?.toString() ?: ""
                                topKeywords.any { tags.contains(it) } && matchesSeason(tags)
                            }
                            var bottoms = allClothes.filter { item ->
                                val tags = item["tags"]?.toString() ?: ""
                                bottomKeywords.any { tags.contains(it) } && matchesSeason(tags)
                            }

                            if (tops.isEmpty()) tops = allClothes.filter { item ->
                                val tags = item["tags"]?.toString() ?: ""
                                topKeywords.any { tags.contains(it) }
                            }
                            if (bottoms.isEmpty()) bottoms = allClothes.filter { item ->
                                val tags = item["tags"]?.toString() ?: ""
                                bottomKeywords.any { tags.contains(it) }
                            }

                            if (sleeveType != null) {
                                val filtered = tops.filter { it["tags"]?.toString()?.contains(sleeveType) == true }
                                if (filtered.isNotEmpty()) tops = filtered
                            }
                            if (pantsType != null) {
                                val filtered = bottoms.filter { it["tags"]?.toString()?.contains(pantsType) == true }
                                if (filtered.isNotEmpty()) bottoms = filtered
                            }

                            when {
                                tops.isEmpty() && bottoms.isEmpty() -> {
                                    onResult(null, "상의와 하의가 모두 없어요.\n옷을 더 등록해주세요!")
                                    return@loadWishlistFn
                                }
                                tops.isEmpty() -> {
                                    onResult(null, "상의로 인식된 옷이 없어요.\n티셔츠, 셔츠, 니트 등을 등록해보세요!")
                                    return@loadWishlistFn
                                }
                                bottoms.isEmpty() -> {
                                    onResult(null, "하의로 인식된 옷이 없어요.\n바지, 치마, 청바지 등을 등록해보세요!")
                                    return@loadWishlistFn
                                }
                            }

                            val topList = tops.mapIndexed { i, item ->
                                "상의[$i] id=${item["id"]} tags=${item["tags"]}"
                            }.joinToString("\n")

                            val bottomList = bottoms.mapIndexed { i, item ->
                                "하의[$i] id=${item["id"]} tags=${item["tags"]}"
                            }.joinToString("\n")

                            val weatherContext = weatherInfo?.let {
                                "현재 날씨: ${it.emoji} ${it.description}, 기온: ${it.tempCelsius}°C"
                            } ?: "날씨 정보 없음"

                            val sleeveContext = sleeveType?.let { "상의는 반드시 $it 이어야 해." } ?: ""
                            val pantsContext = pantsType?.let { "하의는 반드시 $it 이어야 해." } ?: ""

                            val prompt = """
                                너는 패션 코디 전문가야.
                                아래 옷 목록에서 "$category" 스타일에 가장 잘 어울리는 상의 1개와 하의 1개를 골라줘.
                                
                                [오늘의 날씨]
                                $weatherContext
                                날씨와 기온을 반드시 고려해서 추천해줘.
                                
                                [추가 조건]
                                $sleeveContext
                                $pantsContext
                                
                                [상의 목록]
                                $topList
                                
                                [하의 목록]
                                $bottomList
                                
                                반드시 아래 형식으로만 답해줘. 다른 말은 하지 마:
                                TOP_ID: (선택한 상의의 id값)
                                BOTTOM_ID: (선택한 하의의 id값)
                                REASON: (추천 이유를 2~3문장으로, 날씨 언급 포함해서 재치있고 친근하게)
                            """.trimIndent()

                            viewModelScope.launch {
                                try {
                                    // ✅ Gemini 폴백 적용
                                    val response = generateWithFallback { model ->
                                        model.generateContent(prompt)
                                    }
                                    val text = response.text ?: ""

                                    if (text.isEmpty()) {
                                        onResult(null, "AI 응답이 비어있어요. 다시 시도해주세요.")
                                        return@launch
                                    }

                                    val topId = Regex("TOP_ID:\\s*(.+)").find(text)?.groupValues?.get(1)?.trim()
                                    val bottomId = Regex("BOTTOM_ID:\\s*(.+)").find(text)?.groupValues?.get(1)?.trim()
                                    val reason = Regex("REASON:\\s*(.+)", RegexOption.DOT_MATCHES_ALL)
                                        .find(text)?.groupValues?.get(1)?.trim() ?: "이 조합 진짜 찰떡이에요!"

                                    val selectedTop = tops.find { it["id"] == topId }
                                        ?: run { Log.w("ViewModel", "TOP_ID 매칭 실패 → 랜덤"); tops.random() }
                                    val selectedBottom = bottoms.find { it["id"] == bottomId }
                                        ?: run { Log.w("ViewModel", "BOTTOM_ID 매칭 실패 → 랜덤"); bottoms.random() }

                                    onResult(
                                        OutfitRecommendation(
                                            topImageUrl = selectedTop["imageUrl"]?.toString() ?: "",
                                            bottomImageUrl = selectedBottom["imageUrl"]?.toString() ?: "",
                                            topTags = selectedTop["tags"]?.toString() ?: "",
                                            bottomTags = selectedBottom["tags"]?.toString() ?: "",
                                            reason = reason
                                        ),
                                        null
                                    )
                                } catch (e: Exception) {
                                    Log.e("ViewModel", "Gemini 추천 실패 (전체 키 소진)", e)
                                    onResult(null, if (e.message?.contains("503") == true || e.message?.contains("UNAVAILABLE") == true)
                                        "서버가 혼잡해요. 잠시 후 다시 시도해주세요."
                                    else "AI 추천 중 오류가 발생했어요.\n잠시 후 다시 시도해주세요.")
                                }
                            }
                        },
                        { onResult(null, "데이터를 불러오지 못했어요.\n네트워크 연결을 확인해주세요.") }
                    )
                },
                onError = { onResult(null, "옷장 데이터를 불러오지 못했어요.\n네트워크 연결을 확인해주세요.") }
            )
        }
    }
}
