package com.example.mockathon

import android.util.Log
import com.example.mockathon.Const.OPENWEATHER_API_KEY
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

data class WeatherInfo(
    val tempCelsius: Double,     // 기온 (°C)
    val description: String,     // 날씨 상태 (예: "맑음", "흐림", "비")
    val emoji: String            // 날씨 이모지
)

class WeatherRepository {

    suspend fun getWeather(lat: Double, lon: Double): WeatherInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val url = "https://api.openweathermap.org/data/2.5/weather" +
                        "?lat=$lat&lon=$lon&appid=$OPENWEATHER_API_KEY&units=metric&lang=kr"
                val response = URL(url).readText()
                val json = JSONObject(response)

                val temp = json.getJSONObject("main").getDouble("temp")
                val weatherArray = json.getJSONArray("weather")
                val weatherId = weatherArray.getJSONObject(0).getInt("id")
                val descriptionRaw = weatherArray.getJSONObject(0).getString("description")

                // 날씨 ID → 한글 설명 + 이모지
                val (description, emoji) = when {
                    weatherId in 200..299 -> "천둥번개" to "⛈️"
                    weatherId in 300..399 -> "이슬비" to "🌦️"
                    weatherId in 500..504 -> "비" to "🌧️"
                    weatherId == 511      -> "진눈깨비" to "🌨️"
                    weatherId in 520..531 -> "소나기" to "🌦️"
                    weatherId in 600..699 -> "눈" to "❄️"
                    weatherId in 700..799 -> "안개" to "🌫️"
                    weatherId == 800      -> "맑음" to "☀️"
                    weatherId == 801      -> "구름 조금" to "🌤️"
                    weatherId == 802      -> "구름 많음" to "⛅"
                    weatherId in 803..804 -> "흐림" to "☁️"
                    else -> descriptionRaw to "🌡️"
                }

                WeatherInfo(
                    tempCelsius = Math.round(temp * 10.0) / 10.0,
                    description = description,
                    emoji = emoji
                )
            } catch (e: Exception) {
                Log.e("WeatherRepository", "날씨 조회 실패", e)
                null
            }
        }
    }
}