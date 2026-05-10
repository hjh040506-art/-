package com.example.mockathon

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(viewModel: ClothingViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    var capturedImage by remember { mutableStateOf<Bitmap?>(null) }
    var showSheet by remember { mutableStateOf(true) }
    val sheetState = rememberModalBottomSheetState()

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val bitmap = if (Build.VERSION.SDK_INT < 28) {
                MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            } else {
                val source = ImageDecoder.createSource(context.contentResolver, it)
                // ✅ Software Bitmap으로 강제 디코딩
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            }
            capturedImage = bitmap
            viewModel.removeBackgroundAndAnalyze(bitmap)
        }
    }


    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            capturedImage = bitmap
            viewModel.removeBackgroundAndAnalyze(bitmap)  // ✅ 누끼 후 분석
        }
    }

    if (capturedImage != null) {
        ResultView(
            bitmap = capturedImage!!,
            viewModel = viewModel,
            onClose = {
                capturedImage = null
                viewModel.resetToInitial()
                onBack()
            }
        )
    } else if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showSheet = false
                onBack()
            },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 50.dp, top = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("새 옷 등록 방식 선택", fontSize = 18.sp)
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(onClick = { cameraLauncher.launch() }) {
                        Text("사진 찍기")
                    }
                    Button(onClick = { galleryLauncher.launch("image/*") }) {
                        Text("갤러리에서 선택")
                    }
                }
            }
        }
    }
}
