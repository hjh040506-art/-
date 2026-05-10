package com.example.mockathon

import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: ClothingViewModel by viewModels()

            var currentScreen by remember { mutableStateOf(0) }
            var selectedItem by remember { mutableStateOf<Map<String, Any>?>(null) }

            when (currentScreen) {
                0 -> MainScreen(onNavigate = { currentScreen = it })
                1 -> ClosetScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = 0 },
                    onItemClick = { item ->
                        selectedItem = item
                        currentScreen = 5
                    }
                )
                2 -> CameraScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = 0 }
                )
                3 -> WishlistScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = 0 }
                )
                4 -> RecommendScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = 0 }
                )
                5 -> DetailView(
                    item = selectedItem!!,
                    onBack = { currentScreen = 1 }
                )
            }
        }
    }
}

@Composable
fun ClothingScreen(viewModel: ClothingViewModel, onBack: () -> Unit) {
    val selectedBitmap = viewModel.selectedImage
    if (selectedBitmap == null) {
        InitialView(viewModel, onBack = onBack)
    } else {
        ResultView(
            bitmap = selectedBitmap,
            viewModel = viewModel,
            onClose = {
                viewModel.selectedImage = null
                onBack()
            }
        )
    }
}

@Composable
fun InitialView(viewModel: ClothingViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            viewModel.removeBackgroundAndAnalyze(bitmap)  // ✅ 누끼 후 분석
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF996666))
            .padding(16.dp)
    ) {
        androidx.compose.material3.Button(
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Text("이전")
        }
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "최대한 깔끔한 배경에\n 촬영해 주세요",
                fontSize = 24.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            androidx.compose.material3.Button(
                onClick = { launcher.launch("image/*") },
                modifier = Modifier.size(width = 200.dp, height = 100.dp)
            ) {
                Text("옷 사진 가져오기", fontSize = 20.sp)
            }
        }
    }
}

@Composable
fun ResultView(
    bitmap: Bitmap,
    viewModel: ClothingViewModel,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var savedToCloset by remember { mutableStateOf(false) }
    var savedToWishlist by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAF7F4))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFEDE0D8))
                    .clickable {
                        viewModel.resetToInitial()
                        onClose()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("←", fontSize = 18.sp, color = Color(0xFF2E1F1A))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "옷 분석 결과",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E1F1A)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()             // ✅ 이미지 높이에 맞게 자동 조절
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFFEDE0D8))
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "선택된 옷 사진",
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.FillWidth  // ✅ 가로 꽉 채우고 세로 비율 유지
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .padding(20.dp)
            ) {
                when {
                    viewModel.isRemovingBackground -> {          // ✅ 배경 제거 로딩
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFFB07A6E),
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("배경 제거 중...", fontSize = 15.sp, color = Color(0xFF888888))
                        }
                    }
                    viewModel.isAnalyzing -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFFB07A6E),
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("AI가 분석 중이에요...", fontSize = 15.sp, color = Color(0xFF888888))
                        }
                    }
                    viewModel.isError -> {
                        Text(text = viewModel.resultText, color = Color(0xFFCC5555), fontSize = 14.sp)
                    }
                    viewModel.isAnalysisFinished && viewModel.isClothing -> {
                        Column {
                            Text("✦ 분석 완료", fontSize = 12.sp, letterSpacing = 2.sp, color = Color(0xFFB07A6E), fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = viewModel.resultText, fontSize = 15.sp, color = Color(0xFF2E1F1A), lineHeight = 24.sp)
                        }
                    }
                    viewModel.isAnalysisFinished && !viewModel.isClothing -> {
                        Text(text = viewModel.resultText, fontSize = 15.sp, color = Color(0xFF555555), lineHeight = 24.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (savedToCloset) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFE8F5E9))
                        .padding(14.dp)
                ) {
                    Text("✅ 옷장에 저장됐어요!", color = Color(0xFF4CAF50), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (savedToWishlist) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFFFF8E1))
                        .padding(14.dp)
                ) {
                    Text("⭐ 찜칸에 등록됐어요!", color = Color(0xFFF9A825), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (viewModel.isAnalysisFinished && viewModel.isClothing && (!savedToCloset || !savedToWishlist)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!savedToCloset) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF2E1F1A))
                                .clickable(enabled = !viewModel.isSaving) {
                                    viewModel.saveToCloset(bitmap, viewModel.resultText) { success ->
                                        if (success) savedToCloset = true
                                        else Toast.makeText(context, "저장 실패.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (viewModel.isSaving) "저장 중..." else "👕 옷장 저장",
                                color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    if (!savedToWishlist) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFEDE0D8))
                                .clickable(enabled = !viewModel.isSaving) {
                                    viewModel.saveToWishlist(bitmap, viewModel.resultText) { success ->
                                        if (success) savedToWishlist = true
                                        else Toast.makeText(context, "추가 실패.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (viewModel.isSaving) "처리 중..." else "⭐ 찜칸 등록",
                                color = Color(0xFF2E1F1A), fontSize = 15.sp, fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun MainScreen(onNavigate: (Int) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAF7F4))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "✦ My Closet", fontSize = 13.sp, letterSpacing = 3.sp, color = Color(0xFFB07A6E), fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "오늘은\n뭐 입지?", fontSize = 38.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E1F1A), lineHeight = 46.sp)

            Spacer(modifier = Modifier.height(48.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF2E1F1A))
                    .clickable { onNavigate(4) },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Text("✨", fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("오늘 코디 추천", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("AI가 날씨에 맞게 골라드려요", fontSize = 12.sp, color = Color(0xFFBBAA99))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier.weight(1f).height(100.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xFFEDE0D8)).clickable { onNavigate(1) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("👕", fontSize = 26.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("내 옷장", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2E1F1A))
                    }
                }
                Box(
                    modifier = Modifier.weight(1f).height(100.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xFFF5EBE0)).clickable { onNavigate(3) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("⭐", fontSize = 26.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("찜 목록", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2E1F1A))
                    }
                }
                Box(
                    modifier = Modifier.weight(1f).height(100.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xFFD4C4BC)).clickable { onNavigate(2) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📸", fontSize = 26.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("옷 등록", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2E1F1A))
                    }
                }
            }
        }
    }
}
