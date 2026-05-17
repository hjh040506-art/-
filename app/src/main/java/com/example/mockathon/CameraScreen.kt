package com.example.mockathon

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.content.FileProvider
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(viewModel: ClothingViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    var capturedImage by remember { mutableStateOf<Bitmap?>(null) }
    var showSheet by remember { mutableStateOf(true) }
    val sheetState = rememberModalBottomSheetState()
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val bitmap = if (Build.VERSION.SDK_INT < 28) {
                MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            } else {
                val source = ImageDecoder.createSource(context.contentResolver, it)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            }
            capturedImage = bitmap
            viewModel.removeBackgroundAndAnalyze(bitmap)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraUri?.let { uri ->
                val bitmap = if (Build.VERSION.SDK_INT < 28) {
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                } else {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    }
                }
                capturedImage = bitmap
                viewModel.removeBackgroundAndAnalyze(bitmap)
            }
        }
    }

    val displayBitmap = viewModel.selectedImage ?: capturedImage

    if (displayBitmap != null) {
        ResultView(
            bitmap = displayBitmap,
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 50.dp, top = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("        새 옷 등록 방식 선택\n옷이 잘 보이게 찍어주세요👕!", fontSize = 18.sp)
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(onClick = {
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            File(context.cacheDir, "camera_${System.currentTimeMillis()}.jpg")
                        )
                        cameraUri = uri
                        cameraLauncher.launch(uri)
                    }) {
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