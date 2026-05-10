package com.example.mockathon

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun WishlistScreen(viewModel: ClothingViewModel, onBack: () -> Unit) {
    var wishlistItems by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isEditMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf(false) }
    var deleteErrorMessage by remember { mutableStateOf<String?>(null) }

    fun refreshWishlist() {
        viewModel.loadWishlist(
            onResult = { wishlistItems = it },
            onError = { loadError = true }
        )
    }

    LaunchedEffect(Unit) { refreshWishlist() }

    // 삭제 실패 다이얼로그
    deleteErrorMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { deleteErrorMessage = null },
            title = { Text("삭제 오류") },
            text = { Text(msg) },
            confirmButton = { TextButton(onClick = { deleteErrorMessage = null }) { Text("확인") } }
        )
    }

    // 삭제 확인 다이얼로그
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("삭제할까요?", fontWeight = FontWeight.Bold) },
            text = { Text("선택한 ${selectedIds.size}개의 아이템을 삭제합니다.") },
            confirmButton = {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFCC5555))
                        .clickable {
                            viewModel.deleteWishlist(selectedIds, wishlistItems) { _, failCount ->
                                refreshWishlist()
                                isEditMode = false
                                selectedIds = emptySet()
                                showDeleteDialog = false
                                if (failCount > 0) deleteErrorMessage = "${failCount}개 삭제에 실패했어요."
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) { Text("삭제", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("취소", color = Color(0xFF888888)) }
            }
        )
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
            Text(
                "찜 목록",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E1F1A),
                modifier = Modifier.weight(1f)
            )

            // 삭제 모드 버튼들
            if (isEditMode) {
                if (selectedIds.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFCC5555))
                            .clickable { showDeleteDialog = true }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text("삭제 (${selectedIds.size})", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2E1F1A))
                        .clickable { isEditMode = false; selectedIds = emptySet() }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("취소", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Medium)
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFEDE0D8))
                        .clickable { isEditMode = true }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("🗑️", fontSize = 13.sp)
                }
            }
        }

        // 콘텐츠
        when {
            loadError -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("⚠️", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "찜 목록을 불러오지 못했어요.\n네트워크를 확인해주세요.",
                            fontSize = 15.sp,
                            color = Color(0xFF888888),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF2E1F1A))
                                .clickable { loadError = false; refreshWishlist() }
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                        ) { Text("다시 시도", color = Color.White, fontWeight = FontWeight.Medium) }
                    }
                }
            }

            wishlistItems.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("⭐", fontSize = 52.sp)
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("아직 찜한 아이템이 없어요", fontSize = 16.sp, color = Color(0xFF888888))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("마음에 드는 옷을 찜칸에 추가해보세요", fontSize = 13.sp, color = Color(0xFFBBBBBB))
                    }
                }
            }

            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(wishlistItems) { item ->
                        val itemId = item["id"] as? String ?: ""
                        val imageUrl = item["imageUrl"] as? String ?: ""
                        val tags = item["tags"] as? String ?: ""
                        val isSelected = selectedIds.contains(itemId)

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                                .clickable(enabled = isEditMode) {
                                    selectedIds = if (isSelected) selectedIds - itemId else selectedIds + itemId
                                }
                        ) {
                            Column {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .wrapContentHeight()
                                        .background(Color(0xFFF0E8E3))
                                ) {
                                    AsyncImage(
                                        model = imageUrl,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxWidth(),
                                        contentScale = ContentScale.FillWidth
                                    )
                                    if (isEditMode && isSelected) {
                                        Box(modifier = Modifier.fillMaxSize().background(Color(0x55B07A6E)))
                                    }
                                }
                                Box(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = tags.lines().take(2).joinToString(" "),
                                        fontSize = 11.sp,
                                        color = Color(0xFF888888),
                                        maxLines = 2,
                                        lineHeight = 16.sp
                                    )
                                }
                            }

                            // 체크 뱃지
                            if (isEditMode) {
                                Box(
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color(0xFFB07A6E) else Color.White)
                                        .border(2.dp, Color(0xFFB07A6E), CircleShape)
                                        .align(Alignment.TopEnd),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Text("✓", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}