package com.example.data

import android.graphics.Bitmap
import android.util.Log
import com.example.i18n.SnapLanguage
import com.example.model.FoodItem
import com.example.model.Micronutrient
import com.google.firebase.Firebase
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "GeminiNutritionService"

sealed class MealAnalysisResult {
    data class Success(
        val mealName: String,
        val items: List<FoodItem>,
        val micronutrients: List<Micronutrient>,
        val healthScore: Int,
        val healthReason: String,
        val confidence: String,
        val notes: List<String>
    ) : MealAnalysisResult()

    data class NoFood(val reason: String) : MealAnalysisResult()
    data class Error(val errorCode: String, val technicalMessage: String = "") : MealAnalysisResult()
}

class GeminiNutritionService {

    private val mealSchema = Schema.obj(
        properties = mapOf(
            "is_food" to Schema.boolean(description = "True if food or drink is present"),
            "not_food_reason" to Schema.string(description = "Friendly reason if no food detected"),
            "meal_name" to Schema.string(description = "Name of the meal"),
            "items" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "name" to Schema.string(),
                        "portion_grams" to Schema.double(),
                        "portion_description" to Schema.string(),
                        "calories" to Schema.double(),
                        "protein_g" to Schema.double(),
                        "carbs_g" to Schema.double(),
                        "fat_g" to Schema.double(),
                        "fiber_g" to Schema.double(),
                        "sugar_g" to Schema.double(),
                        "sodium_mg" to Schema.double(),
                        "saturated_fat_g" to Schema.double()
                    )
                )
            ),
            "micronutrients" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "name" to Schema.string(),
                        "amount" to Schema.string()
                    )
                )
            ),
            "health_score" to Schema.double(description = "Score between 1 and 10"),
            "health_reason" to Schema.string(),
            "confidence" to Schema.enumeration(listOf("low", "medium", "high")),
            "notes" to Schema.array(items = Schema.string())
        )
    )

    private val singleFoodSchema = Schema.obj(
        properties = mapOf(
            "name" to Schema.string(),
            "portion_grams" to Schema.double(),
            "portion_description" to Schema.string(),
            "calories" to Schema.double(),
            "protein_g" to Schema.double(),
            "carbs_g" to Schema.double(),
            "fat_g" to Schema.double(),
            "fiber_g" to Schema.double(),
            "sugar_g" to Schema.double(),
            "sodium_mg" to Schema.double(),
            "saturated_fat_g" to Schema.double()
        )
    )

    private val mealModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = mealSchema
            },
            systemInstruction = content {
                text(
                    """
You are SnapCal, an expert registered dietitian and food-recognition specialist.
You receive a photo of a meal (and optionally user context) and estimate its nutrition.

Method:
1. Identify EVERY distinct food and drink visible, listing components separately (e.g. "grilled salmon", "white rice", "steamed broccoli", "butter sauce").
2. Estimate each portion in grams using visual cues: plate/bowl diameter (a standard dinner plate is ~26 cm), utensils, hands, packaging and food thickness. Also give a human description like "1 cup" or "1 medium fillet".
3. Account for likely hidden calories: cooking oils, butter, dressings, sauces, breading and frying method. If oil is likely, include it as its own item or fold it into the cooked item and say so in notes.
4. Compute each item's figures from standard nutrition database values per 100 g (USDA FoodData Central style) scaled to the estimated grams. Values are for the whole portion, not per 100 g.
5. Be conservative and honest. Do not invent precision. Lower confidence when the food is ambiguous, partly hidden or the portion is hard to judge.
6. User context overrides your visual guesses (e.g. "no sauce", "olive oil", "half eaten").

Output rules:
- is_food=false with empty items if the image does not clearly show food or drink; put a short friendly reason in not_food_reason.
- micronutrients: only vitamins/minerals present in notable amounts (>=10% daily value), as name + amount with unit, max 8.
- health_score 1-10 (10 = very nutritious, balanced, minimally processed) with a one-sentence reason.
- notes: 1-4 short assumptions (cooking method, hidden oil, sauce, portion uncertainty).
- Respond only with JSON matching the schema.
Required JSON schema (include every field; use an empty string for not_food_reason when food is present):
                    """.trimIndent()
                )
            }
        )
    }

    private val singleFoodModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = singleFoodSchema
            },
            systemInstruction = content {
                text("You are a nutrition database. Return conservative standard values (USDA style, scaled from per-100 g data) for the requested food and portion. Respond only with JSON matching the schema.")
            }
        )
    }

    suspend fun analyzeMeal(
        bitmap: Bitmap,
        contextText: String,
        language: SnapLanguage
    ): MealAnalysisResult {
        return try {
            val langRule = when (language) {
                SnapLanguage.AR -> "Write all human-readable string values (meal_name, item names, portion_description, not_food_reason, micronutrient names, health_reason and notes) in Arabic. Keep every JSON key, enum value and numeric value unchanged; use Latin digits."
                SnapLanguage.FR -> "Write all human-readable string values (meal_name, item names, portion_description, not_food_reason, micronutrient names, health_reason and notes) in French. Keep every JSON key, enum value and numeric value unchanged; use Latin digits."
                SnapLanguage.EN -> "Write all human-readable string values (meal_name, item names, portion_description, not_food_reason, micronutrient names, health_reason and notes) in English. Keep every JSON key, enum value and numeric value unchanged; use Latin digits."
            }

            val trimmedContext = contextText.trim().take(500)
            val userPrompt = if (trimmedContext.isEmpty()) {
                "Analyze this meal. $langRule"
            } else {
                "Analyze this meal. $langRule User context: \"$trimmedContext\""
            }

            val response = withTimeout(60_000L) {
                mealModel.generateContent(
                    content {
                        image(bitmap)
                        text(userPrompt)
                    }
                )
            }

            val jsonText = response.text
            if (jsonText.isNullOrBlank()) {
                return MealAnalysisResult.Error("err.server", "Empty response from Gemini")
            }

            val json = JSONObject(jsonText)
            val isFood = json.optBoolean("is_food", true)
            val notFoodReason = json.optString("not_food_reason", "")

            val itemsArray = json.optJSONArray("items")
            if (!isFood || itemsArray == null || itemsArray.length() == 0) {
                return MealAnalysisResult.NoFood(notFoodReason)
            }

            val mealName = json.optString("meal_name", "").ifEmpty { "My meal" }
            val foodItems = mutableListOf<FoodItem>()

            for (i in 0 until itemsArray.length()) {
                val itemObj = itemsArray.getJSONObject(i)
                val item = FoodItem.fromAiValues(
                    name = itemObj.optString("name", "Food item"),
                    rawGrams = itemObj.optDouble("portion_grams", 100.0),
                    portionDesc = itemObj.optString("portion_description", ""),
                    calories = itemObj.optDouble("calories", 0.0),
                    protein = itemObj.optDouble("protein_g", 0.0),
                    carbs = itemObj.optDouble("carbs_g", 0.0),
                    fat = itemObj.optDouble("fat_g", 0.0),
                    fiber = itemObj.optDouble("fiber_g", 0.0),
                    sugar = itemObj.optDouble("sugar_g", 0.0),
                    sodium = itemObj.optDouble("sodium_mg", 0.0),
                    saturatedFat = itemObj.optDouble("saturated_fat_g", 0.0)
                )
                foodItems.add(item)
            }

            val micros = mutableListOf<Micronutrient>()
            val microsArray = json.optJSONArray("micronutrients")
            if (microsArray != null) {
                for (i in 0 until microsArray.length()) {
                    val mObj = microsArray.getJSONObject(i)
                    micros.add(
                        Micronutrient(
                            name = mObj.optString("name", ""),
                            amount = mObj.optString("amount", "")
                        )
                    )
                }
            }

            val rawScore = json.optDouble("health_score", 7.0)
            val roundedScore = rawScore.toInt().coerceIn(1, 10)
            val healthReason = json.optString("health_reason", "")
            val confidence = json.optString("confidence", "high").lowercase()

            val notes = mutableListOf<String>()
            val notesArray = json.optJSONArray("notes")
            if (notesArray != null) {
                for (i in 0 until notesArray.length()) {
                    notes.add(notesArray.getString(i))
                }
            }

            MealAnalysisResult.Success(
                mealName = mealName,
                items = foodItems,
                micronutrients = micros,
                healthScore = roundedScore,
                healthReason = healthReason,
                confidence = confidence,
                notes = notes
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Meal analysis timed out", e)
            MealAnalysisResult.Error("err.network", "Request timed out")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Meal analysis error", e)
            val msg = e.localizedMessage.orEmpty()
            val errorCode = when {
                msg.contains("429") || msg.contains("RESOURCE_EXHAUSTED") -> "err.rate_limit"
                msg.contains("402") -> "err.credits"
                msg.contains("403") || msg.contains("PERMISSION_DENIED") -> "err.denied"
                msg.contains("400") || msg.contains("INVALID_ARGUMENT") -> "err.bad_image"
                msg.contains("Unable to resolve host") || msg.contains("network") -> "err.network"
                else -> "err.server"
            }
            MealAnalysisResult.Error(errorCode, msg)
        }
    }

    suspend fun estimateSingleFood(
        name: String,
        grams: Double?,
        language: SnapLanguage
    ): Result<FoodItem> {
        return try {
            val cleanName = name.trim().take(120)
            val portionText = if (grams != null && grams > 0.0) "${grams.toInt().coerceAtMost(3000)} g" else "a typical single serving"

            val langName = when (language) {
                SnapLanguage.AR -> "Arabic"
                SnapLanguage.FR -> "French"
                SnapLanguage.EN -> "English"
            }
            val prompt = "Food: $cleanName. Portion: $portionText. Write name and portion_description in $langName; keep JSON keys and numbers unchanged with Latin digits."

            val response = withTimeout(30_000L) {
                singleFoodModel.generateContent(prompt)
            }

            val jsonText = response.text ?: throw IllegalStateException("Empty text")
            val obj = JSONObject(jsonText)

            val item = FoodItem.fromAiValues(
                name = obj.optString("name", cleanName),
                rawGrams = obj.optDouble("portion_grams", grams ?: 100.0),
                portionDesc = obj.optString("portion_description", portionText),
                calories = obj.optDouble("calories", 0.0),
                protein = obj.optDouble("protein_g", 0.0),
                carbs = obj.optDouble("carbs_g", 0.0),
                fat = obj.optDouble("fat_g", 0.0),
                fiber = obj.optDouble("fiber_g", 0.0),
                sugar = obj.optDouble("sugar_g", 0.0),
                sodium = obj.optDouble("sodium_mg", 0.0),
                saturatedFat = obj.optDouble("saturated_fat_g", 0.0)
            )
            Result.success(item)
        } catch (e: Exception) {
            Log.e(TAG, "Single food lookup error", e)
            Result.failure(e)
        }
    }
}
