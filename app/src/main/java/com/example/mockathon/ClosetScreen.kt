package com.example.mockathon

import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

val SEASON_FILTERS = listOf("전체", "봄", "여름", "가을", "겨울")

fun matchesFilter(tags: String, styleFilter: String, seasonFilter: String): Boolean {
    val lowerTags = tags.lowercase()
    val styleMatch = if (styleFilter == "전체") true else {
        val line4 = tags.lines().getOrNull(3)?.lowercase() ?: ""
        line4.contains(styleFilter.lowercase())
    }
    val seasonMatch = if (seasonFilter == "전체") true else lowerTags.contains(seasonFilter.lowercase())
    return styleMatch && seasonMatch
}

@Composable
fun ClosetScreen(
    viewModel: ClothingViewModel,
    onBack: () -> Unit,
    onItemClick: (Map<String, Any>) -> Unit
) {
    var clothesList by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isDeleteMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var showDialog by remember { mutableStateOf(false) }
    var deleteErrorMessage by remember { mutableStateOf<String?>(null) }
    var loadError by remember { mutableStateOf(false) }
    var selectedStyle by remember { mutableStateOf("전체") }
    var selectedSeason by remember { mutableStateOf("전체") }
    val dynamicStyleFilters = remember(clothesList) {
        val styles = clothesList.mapNotNull { item ->
            val tags = item["tags"]?.toString() ?: ""
            val line = tags.lines().getOrNull(3)  // 4번째 줄
            line?.trim()?.replace("#", "")?.trim()?.takeIf { it.isNotEmpty() }
        }.distinct()
        listOf("전체") + styles
    }
    LaunchedEffect(Unit) {
        viewModel.loadAllClothes(
            onResult = { clothesList = it },
            onError = { loadError = true }
        )
    }

    val filteredList = clothesList.filter { item ->
        val tags = item["tags"]?.toString() ?: ""
        matchesFilter(tags, selectedStyle, selectedSeason)
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
                "내 옷장",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E1F1A),
                modifier = Modifier.weight(1f)
            )
            if (selectedStyle != "전체" || selectedSeason != "전체") {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFB07A6E))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("${filteredList.size}개", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDeleteMode) Color(0xFF2E1F1A) else Color(0xFFEDE0D8))
                    .clickable { isDeleteMode = !isDeleteMode; selectedIds = emptySet() }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    if (isDeleteMode) "취소" else "🗑️",
                    fontSize = 13.sp,
                    color = if (isDeleteMode) Color.White else Color(0xFF2E1F1A),
                    fontWeight = FontWeight.Medium
                )
            }
            if (isDeleteMode && selectedIds.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFCC5555))
                        .clickable { showDialog = true }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("삭제 (${selectedIds.size})", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 필터 영역
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(vertical = 10.dp)
        ) {
            FilterSection("스타일", dynamicStyleFilters, selectedStyle) { selectedStyle = it }
            FilterSection("계절", SEASON_FILTERS, selectedSeason) { selectedSeason = it }
            if (selectedStyle != "전체" || selectedSeason != "전체") {
                TextButton(
                    onClick = { selectedStyle = "전체"; selectedSeason = "전체" },
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    Text("✕ 필터 초기화", fontSize = 12.sp, color = Color(0xFFB07A6E))
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
                        Text("옷장을 불러오지 못했어요.\n네트워크를 확인해주세요.", fontSize = 15.sp, color = Color(0xFF888888), textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF2E1F1A))
                                .clickable { loadError = false; viewModel.loadAllClothes(onResult = { clothesList = it }, onError = { loadError = true }) }
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                        ) { Text("다시 시도", color = Color.White, fontWeight = FontWeight.Medium) }
                    }
                }
            }
            clothesList.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("👕", fontSize = 52.sp)
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("아직 등록된 옷이 없어요", fontSize = 16.sp, color = Color(0xFF888888))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("새 옷 등록하기를 눌러 추가해보세요", fontSize = 13.sp, color = Color(0xFFBBBBBB))
                    }
                }
            }
            filteredList.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔍", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("해당 조건의 옷이 없어요", fontSize = 16.sp, color = Color(0xFF888888))
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFEDE0D8))
                                .clickable { selectedStyle = "전체"; selectedSeason = "전체" }
                                .padding(horizontal = 20.dp, vertical = 10.dp)
                        ) { Text("필터 초기화", color = Color(0xFF2E1F1A), fontWeight = FontWeight.Medium) }
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
                    items(filteredList) { item ->
                        val id = item["id"] as? String ?: return@items
                        val imageUrl = item["imageUrl"] as? String ?: ""
                        val tags = item["tags"]?.toString() ?: ""
                        if (id.isEmpty()) return@items
                        val isSelected = selectedIds.contains(id)

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                                .clickable {
                                    if (isDeleteMode) {
                                        selectedIds = if (isSelected) selectedIds - id else selectedIds + id
                                    } else onItemClick(item)
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
                                    if (isDeleteMode && isSelected) {
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
                            if (isDeleteMode) {
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
                                    if (isSelected) Text("✓", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("삭제할까요?", fontWeight = FontWeight.Bold) },
            text = { Text("선택한 ${selectedIds.size}개의 옷을 삭제합니다.") },
            confirmButton = {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFCC5555))
                        .clickable {
                            viewModel.deleteClothes(selectedIds, clothesList) { _, failCount ->
                                viewModel.loadAllClothes(onResult = { clothesList = it }, onError = { deleteErrorMessage = "새로고침 실패" })
                                isDeleteMode = false; selectedIds = emptySet(); showDialog = false
                                if (failCount > 0) deleteErrorMessage = "${failCount}개 삭제 실패"
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) { Text("삭제", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("취소", color = Color(0xFF888888)) }
            }
        )
    }

    deleteErrorMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { deleteErrorMessage = null },
            title = { Text("오류") },
            text = { Text(msg) },
            confirmButton = { TextButton(onClick = { deleteErrorMessage = null }) { Text("확인") } }
        )
    }
}

@Composable
fun FilterSection(label: String, filters: List<String>, selectedFilter: String, onFilterSelected: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label, fontSize = 11.sp, fontWeight = FontWeight.Bold,
            color = Color(0xFFB07A6E), letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 16.dp, end = 10.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(filters) { filter ->
                val isSelected = selectedFilter == filter
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) Color(0xFF2E1F1A) else Color(0xFFF5EEE9))
                        .clickable { onFilterSelected(filter) }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        filter, fontSize = 12.sp,
                        color = if (isSelected) Color.White else Color(0xFF888888),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun DetailView(item: Map<String, Any>, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAF7F4))
            .verticalScroll(rememberScrollState())  // ✅ 추가
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFEDE0D8))
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) { Text("←", fontSize = 18.sp, color = Color(0xFF2E1F1A)) }
            Spacer(modifier = Modifier.width(14.dp))
            Text("옷 상세", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E1F1A))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .wrapContentHeight()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFFEDE0D8))
        ) {
            AsyncImage(
                model = item["imageUrl"] as? String ?: "",
                contentDescription = null,
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.FillWidth
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .padding(16.dp)
        ) {
            Text(item["tags"]?.toString() ?: "", fontSize = 14.sp, color = Color(0xFF555555), lineHeight = 22.sp)
        }
        Spacer(modifier = Modifier.height(40.dp))
    }
}