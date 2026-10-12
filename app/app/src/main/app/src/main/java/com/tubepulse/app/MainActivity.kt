package com.tubepulse.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Locale

data class ExtractedKeyword(
    val keyword: String,
    val frequency: Int,
    val source: String,
    val estimatedVolume: String,
    val competitionLevel: String
)

data class LiveChannelAnalysis(
    val channelTitle: String,
    val channelDescription: String,
    val totalVideosScanned: Int,
    val generatedKeywords: List<ExtractedKeyword>,
    val dynamicSeoScore: Int,
    val dynamicTips: List<String>
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color(0xFF0F172A),
                    surface = Color(0xFF1E293B),
                    primary = Color(0xFFE11D48),
                    onPrimary = Color.White
                )
            ) {
                TubePulseDynamicApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TubePulseDynamicApp() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var analysisResult by remember { mutableStateOf<LiveChannelAnalysis?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE11D48)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Logo", tint = Color.White)
                        }
                        Column {
                            Text("TubePulse AI", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("توليد الكلمات المفتاحية التلقائي", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showDialog = true }) {
                        Icon(
                            imageVector = if (analysisResult != null) Icons.Default.CheckCircle else Icons.Default.AddLink,
                            contentDescription = "ربط القناة",
                            tint = if (analysisResult != null) Color(0xFF10B981) else Color(0xFFE11D48)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF0F172A)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (analysisResult != null) Color(0xFF10B981) else Color(0xFFEF4444))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = analysisResult?.let { "متصل بـ: ${it.channelTitle}" } ?: "لم يتم قراءة أي قناة بعد",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Button(
                            onClick = { showDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(if (analysisResult != null) "فحص قناة أخرى" else "قراءة قناة", fontSize = 12.sp)
                        }
                    }
                }
            }

            if (isLoading) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = Color(0xFFE11D48))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("جاري استخراج وتحليل كلمات الفيديوهات والمحتوى...", color = Color.LightGray, fontSize = 13.sp)
                    }
                }
            }

            errorMessage?.let { error ->
                item {
                    Text(text = error, color = Color(0xFFEF4444), fontSize = 13.sp)
                }
            }

            analysisResult?.let { result ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("📈 كفاءة السيو المحسوبة للمحتوى", fontWeight = FontWeight.Bold, color = Color.White)
                                Text("${result.dynamicSeoScore}/100", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFF10B981))
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { result.dynamicSeoScore / 100f },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = Color(0xFF10B981),
                                trackColor = Color(0xFF334155)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "تم توليد الكلمات ديناميكياً من فحص ${result.totalVideosScanned} فيديو ووصف القناة.",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🔑 الكلمات المفتاحية المولدة (${result.generatedKeywords.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )

                        TextButton(
                            onClick = {
                                val allKw = result.generatedKeywords.joinToString(", ") { it.keyword }
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Keywords", allKw))
                                Toast.makeText(context, "تم نسخ جميع الكلمات المفتاحية!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF38BDF8))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("نسخ الكل", color = Color(0xFF38BDF8), fontSize = 12.sp)
                        }
                    }
                }

                items(result.generatedKeywords) { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = item.keyword, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "تكرار الظهور: ${item.frequency} مرات | المصدر: ${item.source}",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        when (item.competitionLevel) {
                                            "منخفضة" -> Color(0xFF065F46)
                                            "متوسطة" -> Color(0xFF854D0E)
                                            else -> Color(0xFF991B1B)
                                        }
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = item.competitionLevel,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("💡 نصائح مخصصة لقناتك تم توليدها آلياً", fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            result.dynamicTips.forEach { tip ->
                                Row(modifier = Modifier.padding(vertical = 3.dp)) {
                                    Text("• ", color = Color(0xFF38BDF8))
                                    Text(tip, color = Color(0xFFCBD5E1), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        var queryInput by remember { mutableStateOf("") }
        var apiKeyInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("قراءة وتحليل محتوى القناة") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("أدخل اسم القناة أو الرابط أو Handle (مثال: @ChannelName):", fontSize = 12.sp, color = Color.Gray)
                    OutlinedTextField(
                        value = queryInput,
                        onValueChange = { queryInput = it },
                        label = { Text("اسم القناة أو المعرف") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("YouTube Data API Key:", fontSize = 12.sp, color = Color.Gray)
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text("مفتاح API") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (queryInput.isNotBlank() && apiKeyInput.isNotBlank()) {
                            showDialog = false
                            isLoading = true
                            errorMessage = null
                            coroutineScope.launch {
                                try {
                                    val res = analyzeLiveYouTubeChannel(queryInput.trim(), apiKeyInput.trim())
                                    if (res != null) {
                                        analysisResult = res
                                    } else {
                                        errorMessage = "تعذر العثور على القناة، تأكد من صحة الاسم ومفتاح API."
                                    }
                                } catch (e: Exception) {
                                    errorMessage = "حدث خطأ أثناء الاتصال: ${e.localizedMessage}"
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
                ) {
                    Text("بدء القراءة والتوليد")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("إلغاء") }
            }
        )
    }
}

suspend fun analyzeLiveYouTubeChannel(channelQuery: String, apiKey: String): LiveChannelAnalysis? = withContext(Dispatchers.IO) {
    val client = OkHttpClient()

    val cleanQuery = channelQuery.removePrefix("@").trim()
    val searchUrl = "https://www.googleapis.com/youtube/v3/search?part=snippet&q=$cleanQuery&type=channel&maxResults=1&key=$apiKey"
    val searchReq = Request.Builder().url(searchUrl).build()
    val searchResp = client.newCall(searchReq).execute()
    val searchJson = JSONObject(searchResp.body?.string() ?: "{}")
    val searchItems = searchJson.optJSONArray("items") ?: return@withContext null

    if (searchItems.length() == 0) return@withContext null
    val channelId = searchItems.getJSONObject(0).getJSONObject("id").getString("channelId")
    val channelTitle = searchItems.getJSONObject(0).getJSONObject("snippet").getString("title")

    val channelDetailsUrl = "https://www.googleapis.com/youtube/v3/channels?part=snippet,brandingSettings&id=$channelId&key=$apiKey"
    val chResp = client.newCall(Request.Builder().url(channelDetailsUrl).build()).execute()
    val chJson = JSONObject(chResp.body?.string() ?: "{}")
    val chItem = chJson.optJSONArray("items")?.optJSONObject(0)
    val officialKeywords = chItem?.optJSONObject("brandingSettings")?.optJSONObject("channel")?.optString("keywords", "") ?: ""
    val channelDesc = chItem?.optJSONObject("snippet")?.optString("description", "") ?: ""

    val videosSearchUrl = "https://www.googleapis.com/youtube/v3/search?part=snippet&channelId=$channelId&order=date&type=video&maxResults=15&key=$apiKey"
    val vResp = client.newCall(Request.Builder().url(videosSearchUrl).build()).execute()
    val vJson = JSONObject(vResp.body?.string() ?: "{}")
    val vItems = vJson.optJSONArray("items")

    val videoIds = mutableListOf<String>()
    if (vItems != null) {
        for (i in 0 until vItems.length()) {
            val vid = vItems.getJSONObject(i).getJSONObject("id").optString("videoId")
            if (vid.isNotEmpty()) videoIds.add(vid)
        }
    }

    val frequencyMap = mutableMapOf<String, Pair<Int, String>>()

    if (officialKeywords.isNotBlank()) {
        val splitOfficial = officialKeywords.split(" ", "\"").map { it.trim() }.filter { it.length > 2 }
        splitOfficial.forEach { kw ->
            frequencyMap[kw] = Pair((frequencyMap[kw]?.first ?: 0) + 3, "وسوم القناة الرسمية")
        }
    }

    var scannedCount = 0
    if (videoIds.isNotEmpty()) {
        val idsParam = videoIds.joinToString(",")
        val videosDetailsUrl = "https://www.googleapis.com/youtube/v3/videos?part=snippet&id=$idsParam&key=$apiKey"
        val vdResp = client.newCall(Request.Builder().url(videosDetailsUrl).build()).execute()
        val vdJson = JSONObject(vdResp.body?.string() ?: "{}")
        val vdItems = vdJson.optJSONArray("items")

        if (vdItems != null) {
            scannedCount = vdItems.length()
            for (i in 0 until vdItems.length()) {
                val snippet = vdItems.getJSONObject(i).getJSONObject("snippet")
                val title = snippet.getString("title")

                val tags = snippet.optJSONArray("tags")
                if (tags != null) {
                    for (t in 0 until tags.length()) {
                        val tag = tags.getString(t).trim()
                        if (tag.length > 2) {
                            val cur = frequencyMap[tag]?.first ?: 0
                            frequencyMap[tag] = Pair(cur + 2, "وسوم الفيديوهات")
                        }
                    }
                }

                extractKeywordsFromText(title).forEach { w ->
                    val cur = frequencyMap[w]?.first ?: 0
                    frequencyMap[w] = Pair(cur + 1, "عناوين المقاطع")
                }
            }
        }
    }

    val stopWords = setOf("مع", "في", "من", "إلى", "على", "عن", "هذا", "هذه", "كيف", "طريقة", "the", "and", "for", "with", "video", "shorts", "how")

    val finalGeneratedKeywords = frequencyMap.entries
        .filter { (kw, _) -> !stopWords.contains(kw.lowercase(Locale.ROOT)) && kw.length > 2 }
        .sortedByDescending { it.value.first }
        .take(15)
        .map { entry ->
            val count = entry.value.first
            ExtractedKeyword(
                keyword = entry.key,
                frequency = count,
                source = entry.value.second,
                estimatedVolume = "${count * 12}k/شهرياً",
                competitionLevel = when {
                    count >= 5 -> "عالية"
                    count in 3..4 -> "متوسطة"
                    else -> "منخفضة"
                }
            )
        }

    val dynamicSeoScore = calculateDynamicSeo(scannedCount, finalGeneratedKeywords.size)
    val dynamicTips = mutableListOf<String>()

    if (officialKeywords.isBlank()) {
        dynamicTips.add("القناة لا تملك كلمات مفتاحية عامة في إعدادات القناة (Channel Keywords)، يفضل إضافتها.")
    } else {
        dynamicTips.add("القناة تحتوي على كلمات دلالية مخصصة تم استخدامها في تقييم الظهور.")
    }

    if (finalGeneratedKeywords.any { it.competitionLevel == "منخفضة" }) {
        val lowComp = finalGeneratedKeywords.first { it.competitionLevel == "منخفضة" }.keyword
        dynamicTips.add("الكلمة '$lowComp' منافستها منخفضة، يوصى بإنتاج فيديوهات قصيرة (Shorts) تركز عليها.")
    }

    dynamicTips.add("تم العثور على ${finalGeneratedKeywords.size} كلمة مفتاحية متكررة تشكل الهوية الأساسية للقناة.")

    return@withContext LiveChannelAnalysis(
        channelTitle = channelTitle,
        channelDescription = channelDesc,
        totalVideosScanned = scannedCount,
        generatedKeywords = finalGeneratedKeywords,
        dynamicSeoScore = dynamicSeoScore,
        dynamicTips = dynamicTips
    )
}

fun extractKeywordsFromText(text: String): List<String> {
    return text.replace(Regex("[^a-zA-Z0-9أ-ي\\s]"), " ")
        .split("\\s+".toRegex())
        .map { it.trim() }
        .filter { it.length > 3 }
}

fun calculateDynamicSeo(videosCount: Int, keywordsCount: Int): Int {
    if (videosCount == 0) return 30
    var score = 50 + (keywordsCount * 3)
    if (videosCount >= 10) score += 10
    return score.coerceIn(40, 98)
}
