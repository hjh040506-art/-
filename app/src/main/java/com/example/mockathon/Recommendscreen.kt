package com.example.mockathon

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

data class OutfitRecommendation(
    val topImageUrl: String,
    val bottomImageUrl: String,
    val topTags: String,
    val bottomTags: String,
    val reason: String
)

@SuppressLint("MissingPermission")
@Composable
fun RecommendScreen(viewModel: ClothingViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var customInput by remember { mutableStateOf("") }
    var recommendation by remember { mutableStateOf<OutfitRecommendation?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedSleeve by remember { mutableStateOf<String?>(null) }
    var selectedPants by remember { mutableStateOf<String?>(null) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            val request = CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                .build()
            fusedClient.getCurrentLocation(request, null)
                .addOnSuccessListener { location ->
                    location?.let { viewModel.fetchWeather(it.latitude, it.longitude) }
                        ?: run { viewModel.weatherError = true }
                }
                .addOnFailureListener { viewModel.weatherError = true }
        } else {
            viewModel.weatherError = true
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadClosetStyleTags()
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            val request = CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                .build()
            fusedClient.getCurrentLocation(request, null)
                .addOnSuccessListener { location ->
                    location?.let { viewModel.fetchWeather(it.latitude, it.longitude) }
                        ?: run { viewModel.weatherError = true }
                }
                .addOnFailureListener { viewModel.weatherError = true }
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAF7F4))
    ) {
        // 상단 바
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFAF7F4))
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFEDE0D8))
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Text("←", fontSize = 18.sp, color = Color(0xFF2E1F1A))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(text = "✦ STYLE AI", fontSize = 11.sp, letterSpacing = 2.sp, color = Color(0xFFB07A6E), fontWeight = FontWeight.Medium)
                Text(text = "오늘의 코디 추천", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E1F1A))
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 날씨 카드
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF2E1F1A))
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                when {
                    viewModel.isWeatherLoading || (!viewModel.weatherError && viewModel.weatherInfo == null) -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color(0xFFEDE0D8))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("날씨 불러오는 중...", fontSize = 14.sp, color = Color(0xFFBBAA99))
                        }
                    }
                    viewModel.weatherError -> {
                        Text("📍 날씨를 불러오지 못했어요", fontSize = 14.sp, color = Color(0xFFBBAA99))
                    }
                    else -> {
                        val w = viewModel.weatherInfo!!
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(w.emoji, fontSize = 36.sp)
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(text = "${w.tempCelsius}°C", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(text = w.description, fontSize = 13.sp, color = Color(0xFFBBAA99))
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0x33FFFFFF))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("날씨 반영됨", fontSize = 11.sp, color = Color(0xFFEDE0D8))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 스타일 카테고리
            Text(
                text = "어떤 스타일로 입을까요?",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF2E1F1A),
                modifier = Modifier.padding(start = 20.dp, bottom = 12.dp)
            )
            when {
                viewModel.closetStyleTags.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFEDE0D8))
                            .padding(16.dp)
                    ) {
                        Text("옷장에 옷을 등록하면\n스타일 카테고리가 자동으로 나타나요 👕", fontSize = 13.sp, color = Color(0xFF888888), lineHeight = 20.sp)
                    }
                }
                else -> {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(viewModel.closetStyleTags) { category ->
                            val isSelected = selectedCategory == category && selectedCategory != "기타"
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) Color(0xFF2E1F1A) else Color(0xFFEDE0D8))
                                    .clickable {
                                        selectedCategory = category
                                        customInput = ""
                                        recommendation = null
                                        errorMessage = null
                                    }
                                    .padding(horizontal = 18.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = category,
                                    color = if (isSelected) Color.White else Color(0xFF666666),
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                        // 기타 칩
                        item {
                            val isSelected = selectedCategory == "기타"
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) Color(0xFF2E1F1A) else Color(0xFFEDE0D8))
                                    .clickable {
                                        selectedCategory = "기타"
                                        customInput = ""
                                        recommendation = null
                                        errorMessage = null
                                    }
                                    .padding(horizontal = 18.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = "기타",
                                    color = if (isSelected) Color.White else Color(0xFF666666),
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            // 기타 선택 시 직접 입력칸
            if (selectedCategory == "기타") {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "어떤 상황인지 입력해주세요",
                    fontSize = 12.sp,
                    letterSpacing = 0.5.sp,
                    color = Color(0xFFB07A6E),
                    modifier = Modifier.padding(start = 20.dp, bottom = 8.dp)
                )
                OutlinedTextField(
                    value = customInput,
                    onValueChange = {
                        customInput = it
                        recommendation = null
                        errorMessage = null
                    },
                    placeholder = { Text("예) 소개팅, 헬스장, 편의점", color = Color(0xFFBBAA99)) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2E1F1A),
                        unfocusedBorderColor = Color(0xFFD4C4BC),
                        cursorColor = Color(0xFF2E1F1A),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 상의 길이 선택
            Text(
                text = "상의 길이",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF2E1F1A),
                modifier = Modifier.padding(start = 20.dp, bottom = 10.dp)
            )
            Row(
                modifier = Modifier.padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("긴팔", "반팔").forEach { type ->
                    val isSelected = selectedSleeve == type
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) Color(0xFF2E1F1A) else Color(0xFFEDE0D8))
                            .clickable {
                                selectedSleeve = if (isSelected) null else type
                                recommendation = null
                            }
                            .padding(horizontal = 18.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = type,
                            color = if (isSelected) Color.White else Color(0xFF666666),
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
                val isNoneSleeveSelected = selectedSleeve == null
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isNoneSleeveSelected) Color(0xFF2E1F1A) else Color(0xFFEDE0D8))
                        .clickable { selectedSleeve = null; recommendation = null }
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "상관없음",
                        color = if (isNoneSleeveSelected) Color.White else Color(0xFF666666),
                        fontSize = 13.sp,
                        fontWeight = if (isNoneSleeveSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 하의 길이 선택
            Text(
                text = "하의 길이",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF2E1F1A),
                modifier = Modifier.padding(start = 20.dp, bottom = 10.dp)
            )
            Row(
                modifier = Modifier.padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("긴바지", "반바지").forEach { type ->
                    val isSelected = selectedPants == type
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) Color(0xFF2E1F1A) else Color(0xFFEDE0D8))
                            .clickable {
                                selectedPants = if (isSelected) null else type
                                recommendation = null
                            }
                            .padding(horizontal = 18.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = type,
                            color = if (isSelected) Color.White else Color(0xFF666666),
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
                val isNonePantsSelected = selectedPants == null
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isNonePantsSelected) Color(0xFF2E1F1A) else Color(0xFFEDE0D8))
                        .clickable { selectedPants = null; recommendation = null }
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "상관없음",
                        color = if (isNonePantsSelected) Color.White else Color(0xFF666666),
                        fontSize = 13.sp,
                        fontWeight = if (isNonePantsSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 추천받기 버튼
            val hasInput = (selectedCategory != null && selectedCategory != "기타") ||
                    (selectedCategory == "기타" && customInput.isNotBlank())
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(58.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (hasInput && !isLoading) Color(0xFFB07A6E) else Color(0xFFD4C4BC))
                    .clickable(enabled = hasInput && !isLoading) {
                        val category = if (selectedCategory == "기타") customInput.trim() else selectedCategory
                        category?.let {
                            isLoading = true
                            errorMessage = null
                            recommendation = null
                            viewModel.getOutfitRecommendation(
                                category = it,
                                sleeveType = selectedSleeve,
                                pantsType = selectedPants
                            ) { result, error ->
                                isLoading = false
                                recommendation = result
                                errorMessage = error
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("AI가 코디 중...", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text(
                        text = if (!hasInput) "카테고리를 먼저 선택해주세요"
                        else "${if (selectedCategory == "기타") customInput.trim() else selectedCategory!!} 코디 추천받기",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            errorMessage?.let { msg ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFFFF0EE))
                        .padding(16.dp)
                ) {
                    Text(text = msg, color = Color(0xFFCC4444), fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            recommendation?.let { outfit ->
                OutfitCard(
                    outfit = outfit,
                    category = if (selectedCategory == "기타") customInput.trim() else selectedCategory ?: "",
                    onRefresh = {
                        val category = if (selectedCategory == "기타") customInput.trim() else selectedCategory
                        category?.let {
                            isLoading = true
                            recommendation = null
                            viewModel.getOutfitRecommendation(
                                category = it,
                                sleeveType = selectedSleeve,
                                pantsType = selectedPants
                            ) { result, error ->
                                isLoading = false
                                recommendation = result
                                errorMessage = error
                            }
                        }
                    }
                )
            }

            if (recommendation == null && !isLoading && errorMessage == null) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("✨", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("스타일을 선택하고", fontSize = 15.sp, color = Color(0xFF999999))
                        Text("추천받기를 눌러보세요!", fontSize = 15.sp, color = Color(0xFF999999))
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun OutfitCard(
    outfit: OutfitRecommendation,
    category: String,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        // 헤더
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "✦ TODAY'S PICK", fontSize = 11.sp, letterSpacing = 2.sp, color = Color(0xFFB07A6E), fontWeight = FontWeight.Medium)
                Text(text = "$category 코디", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E1F1A))
            }
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFEDE0D8))
                    .clickable { onRefresh() },
                contentAlignment = Alignment.Center
            ) {
                Text("↺", fontSize = 20.sp, color = Color(0xFF2E1F1A))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 상의
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .background(Color(0xFFEDE0D8))
                ) {
                    AsyncImage(
                        model = outfit.topImageUrl,
                        contentDescription = "상의",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .padding(12.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xCC2E1F1A))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .align(Alignment.TopStart)
                    ) {
                        Text("상의", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                Text(
                    text = outfit.topTags,
                    fontSize = 11.sp,
                    color = Color(0xFF888888),
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }

        // + 연결
        Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFF2E1F1A)),
                contentAlignment = Alignment.Center
            ) {
                Text("+", fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        // 하의
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .background(Color(0xFFEDE0D8))
                ) {
                    AsyncImage(
                        model = outfit.bottomImageUrl,
                        contentDescription = "하의",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .padding(12.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xCC2E1F1A))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .align(Alignment.TopStart)
                    ) {
                        Text("하의", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                Text(
                    text = outfit.bottomTags,
                    fontSize = 11.sp,
                    color = Color(0xFF888888),
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }

        // ✅ AI 추천 이유 — 하의 아래
        Spacer(modifier = Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF2E1F1A))
                .padding(18.dp)
        ) {
            Column {
                Text(text = "✦ AI 추천 이유", fontSize = 11.sp, letterSpacing = 1.sp, color = Color(0xFFB07A6E), fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = outfit.reason, fontSize = 14.sp, color = Color(0xFFEDE0D8), lineHeight = 22.sp)
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}
