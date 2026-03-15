package com.example.mockathon

import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun ClosetScreen(viewModel: ClothingViewModel, onBack: () -> Unit, onItemClick: (Map<String, Any>) -> Unit) {
    var clothesList by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isDeleteMode by remember { mutableStateOf(false) } // 삭제 모드 여부
    var selectedIds by remember { mutableStateOf(setOf<String>()) } // 선택된 옷들의 ID
    var showDialog by remember { mutableStateOf(false) } // 확인 창 표시 여부

    LaunchedEffect(Unit) {
        viewModel.loadAllClothes { updatedList ->
            clothesList = updatedList
        }
    }

    Scaffold(
        topBar = {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onBack) { Text("이전") }
                Spacer(modifier = Modifier.weight(1f))

                // 휴지통 버튼
                IconButton(onClick = { isDeleteMode = !isDeleteMode; selectedIds = emptySet() }) {
                    Text(if (isDeleteMode) "취소" else "🗑️")
                }
                // 삭제 모드일 때만 나타나는 "지우기" 버튼
                if (isDeleteMode) {
                    Button(onClick = { if (selectedIds.isNotEmpty()) showDialog = true }) { Text("지우기") }
                }
            }
        }
    ) { padding ->
        if (clothesList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("저장된 옷이 없습니다.", fontSize = 18.sp)
            }
        } else {LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.padding(padding)) {
            items(clothesList) { item ->
                val id = item["id"] as? String ?: return@items // ID 없으면 아예 안 그림
                val imageUrl = item["imageUrl"] as? String ?: ""
                val tags = item["tags"]?.toString() ?: "#태그"
                if (id.isEmpty()) return@items
                Box(modifier = Modifier.padding(8.dp).clickable {
                    if (isDeleteMode) {
                        selectedIds = if (selectedIds.contains(id)) selectedIds - id else selectedIds + id
                    } else {
                        onItemClick(item)
                    }
                }) {
                    Column {
                        AsyncImage(
                            model = imageUrl, // 아까 위에서 정의한 안전한 imageUrl 변수를 사용!
                            contentDescription = null,
                            modifier = Modifier.height(150.dp).fillMaxWidth()
                        )
                        Text(tags)
                    }
                    // 삭제 모드일 때 사진 좌측 상단에 동그라미 표시
                    if (isDeleteMode) {
                        RadioButton(selected = selectedIds.contains(id), onClick = null, modifier = Modifier.align(Alignment.TopStart))
                    }
                }
            }
            }
        }
    }

    // 삭제 확인 다이얼로그
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("정말로 지우시겠습니까?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteClothes(selectedIds) {
                        // 삭제 완료 후 리스트 다시 불러오기
                        viewModel.loadAllClothes { clothesList = it }
                        isDeleteMode = false
                        selectedIds = emptySet()
                        showDialog = false
                    }
                }){ Text("확인") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("취소") }
            }
        )
    }
}

@Composable //옷장에서 내 옷을 눌렀을때
fun DetailView(item: Map<String, Any>, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = onBack) { Text("이전") }
        Spacer(modifier = Modifier.height(20.dp))

        AsyncImage(
            model = item["imageUrl"] as? String ?: "",
            contentDescription = "옷 상세 사진",
            modifier = Modifier.size(350.dp),
            contentScale = ContentScale.Crop
        )
        Text(
            text = item["tags"]?.toString() ?: "",
            modifier = Modifier.padding(16.dp)
        )
    }
}