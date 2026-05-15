package com.example.madhusiri.data

import com.example.madhusiri.BuildConfig
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject

class GeminiBeeAdvisor {
    private val modelFallbacks = listOf(
        "gemini-2.5-flash",
        "gemini-2.0-flash",
    )

    fun generateTip(
        cropName: String,
        pesticideName: String,
        sprayTime: String,
    ): Result<String> {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "YOUR_GEMINI_API_KEY_HERE") {
            return Result.failure(
                IllegalStateException("Add GEMINI_API_KEY in local.properties to use AI tips."),
            )
        }

        return runCatching {
            val prompt = """
                You are Madhu-Siri, a bee-friendly farming advisor for India.
                Give a short practical safety tip for pesticide spraying.
                Crop: $cropName
                Pesticide: $pesticideName
                Planned spray time: $sprayTime

                Reply in this exact structure:
                Risk Level: Low/Medium/High
                Best Time:
                Bee Safety Steps:
                Farmer Message:
            """.trimIndent()

            val requestBody = JSONObject()
                .put(
                    "contents",
                    JSONArray()
                        .put(
                            JSONObject()
                                .put(
                                    "parts",
                                    JSONArray().put(JSONObject().put("text", prompt)),
                                ),
                        ),
                )
                .toString()

            var lastErrorCode = 0
            modelFallbacks.forEachIndexed { index, model ->
                repeat(2) { attempt ->
                    val response = requestGemini(
                        model = model,
                        apiKey = apiKey,
                        requestBody = requestBody,
                    )
                    if (response.isSuccess) {
                        return@runCatching response.getOrThrow()
                    }

                    val error = response.exceptionOrNull()
                    lastErrorCode = (error as? GeminiRequestException)?.code ?: 0
                    val shouldRetry = lastErrorCode == 503 || lastErrorCode == 429
                    if (!shouldRetry || (index == modelFallbacks.lastIndex && attempt == 1)) {
                        throw error ?: IllegalStateException("AI advisor is unavailable.")
                    }
                    Thread.sleep(700L * (attempt + 1))
                }
            }

            throw GeminiRequestException(lastErrorCode, "AI advisor is unavailable right now.")
        }.recoverCatching { error ->
            if (error is GeminiRequestException && (error.code == 503 || error.code == 429)) {
                offlineBeeSafetyTip(cropName, pesticideName, sprayTime)
            } else {
                throw error
            }
        }
    }

    private fun requestGemini(
        model: String,
        apiKey: String,
        requestBody: String,
    ): Result<String> = runCatching {
        val endpoint = URL(
            "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey",
        )
        val connection = endpoint.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.connectTimeout = 15000
        connection.readTimeout = 30000
        connection.doOutput = true

        OutputStreamWriter(connection.outputStream).use { writer ->
            writer.write(requestBody)
        }

        val responseText = if (connection.responseCode in 200..299) {
            connection.inputStream.bufferedReader().use { it.readText() }
        } else {
            val errorMessage = connection.errorStream
                ?.bufferedReader()
                ?.use { it.readText() }
                ?.let(::parseGeminiError)
                ?: "The AI advisor could not answer right now."
            throw GeminiRequestException(connection.responseCode, errorMessage)
        }

        JSONObject(responseText)
            .getJSONArray("candidates")
            .getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
            .getJSONObject(0)
            .getString("text")
            .trim()
    }

    private fun parseGeminiError(errorText: String): String {
        return runCatching {
            JSONObject(errorText)
                .getJSONObject("error")
                .optString("message")
                .ifBlank { "The AI advisor could not answer right now." }
        }.getOrDefault("The AI advisor could not answer right now.")
    }

    private fun offlineBeeSafetyTip(
        cropName: String,
        pesticideName: String,
        sprayTime: String,
    ): String {
        return """
            Risk Level: Medium
            Best Time: Spray after sunset or early morning when bees are not actively foraging.
            Bee Safety Steps: Send a 2 km spray alert, avoid spraying during flowering, reduce drift, and keep hives closed for 4 hours if nearby.
            Farmer Message: Spraying planned for $cropName using $pesticideName at $sprayTime. Nearby beekeepers should protect hives temporarily.
        """.trimIndent()
    }
}

private class GeminiRequestException(
    val code: Int,
    message: String,
) : Exception(message)
