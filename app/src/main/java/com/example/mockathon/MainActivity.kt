package com.example.mockathon // 본인의 패키지명 확인 필수!
//UI함수
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
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

            // 상태 변수들을 이곳에 선언합니다.
            var currentScreen by remember { mutableStateOf(0) }
            // 클릭된 아이템을 저장하는 변수
            var selectedItem by remember { mutableStateOf<Map<String, Any>?>(null) }

            // 화면 분기 처리
            when (currentScreen) {
                0 -> MainScreen(
                    onNavigateToCloset = { currentScreen = 2 },
                    onNavigateToAnalyze = { currentScreen = 1 }
                )
                1 -> ClothingScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = 0 } // 뒤로 가기 버튼을 누르면 메인(0번)으로 이동!
                )
                2 -> ClosetScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = 0 }, // 이전 버튼 누르면 메인(0번)으로!
                    onItemClick = { item ->
                        selectedItem = item
                        currentScreen = 3
                    }
                )
                3 -> selectedItem?.let { item ->
                    DetailView(item = item) { currentScreen = 2 } // 뒤로가면 옷장으로
                }
            }
        }
    }
}

@Composable
fun ClothingScreen(viewModel: ClothingViewModel, onBack: () -> Unit) {
    if (viewModel.selectedImage == null) {
        // InitialView 안에 "뒤로가기" 버튼을 추가하거나 처리해야 합니다.
        InitialView(viewModel,onBack = onBack)
    } else {
        ResultView(viewModel)
    }
}

// 1. 초기 화면 함수
@Composable
fun InitialView(viewModel: ClothingViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            viewModel.analyzeImage(bitmap)
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF996666))
            .padding(16.dp)
    ) {
        // 1. 이전 버튼 (왼쪽 상단 고정)
        Button(
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Text("이전")
        }

        // 2. 텍스트와 버튼 (화면 정중앙 배치)
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
            Button(
                onClick = { launcher.launch("image/*") },
                modifier = Modifier.size(width = 200.dp, height = 100.dp)
            ) {
                Text("옷 사진 가져오기", fontSize = 20.sp)
            }
        }
    }
}

// 2. 결과 화면 함수
@Composable
fun ResultView(viewModel: ClothingViewModel) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF996666))
            .padding(16.dp)
    ) {
        Button(onClick = { viewModel.resetToInitial() }) {
            Text("이전")
        }

        Spacer(modifier = Modifier.height(50.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            viewModel.selectedImage?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "선택된 옷 사진",
                    modifier = Modifier.size(350.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = viewModel.resultText,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // [이제 코드가 훨씬 직관적입니다!]
                when {
                    viewModel.isAnalyzing -> {
                        CircularProgressIndicator(color = Color.White)
                    }
                    viewModel.isAnalysisFinished && viewModel.isClothing -> {
                        // 분석이 완료되었고 옷일 때만 저장 버튼 표시
                        Button(
                            onClick = {
                                viewModel.saveToCloset(bitmap, viewModel.resultText) { success ->
                                    if (success) {
                                        Toast.makeText(context, "저장 완료!", Toast.LENGTH_SHORT).show()
                                        viewModel.resetToInitial()
                                    } else {
                                        Toast.makeText(context, "저장 실패.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            enabled = !viewModel.isSaving
                        ) {
                            Text(if (viewModel.isSaving) "저장 중..." else "옷장에 저장하기")
                        }
                    }
                    viewModel.isAnalysisFinished && !viewModel.isClothing -> {
                        // 옷이 아닐 때 안내 문구
                        Text("사진을 다시 확인해주세요.", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable //앱 등장 화면
fun MainScreen(onNavigateToCloset: () -> Unit, onNavigateToAnalyze: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        // 기존의 분석 시작 버튼 등 중앙 배치 코드...
        Column(modifier = Modifier.align(Alignment.Center)) {
            Button(onClick = onNavigateToAnalyze) {
                Text("새 옷 등록하기")
            }
        }

        // 왼쪽 하단에 옷장 버튼 추가
        Button(
            onClick = onNavigateToCloset,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Text("내 옷장 👕")
        }
    }
}
