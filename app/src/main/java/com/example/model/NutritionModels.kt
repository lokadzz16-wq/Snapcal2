package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

data class Nutrients(
    val calories: Double = 0.0,
    val protein_g: Double = 0.0,
    val carbs_g: Double = 0.0,
    val fat_g: Double = 0.0,
    val fiber_g: Double = 0.0,
    val sugar_g: Double = 0.0,
    val sodium_mg: Double = 0.0,
    val saturated_fat_g: Double = 0.0
) {
    operator fun times(grams: Double): Nutrients = Nutrients(
        calories = calories * grams,
        protein_g = protein_g * grams,
        carbs_g = carbs_g * grams,
        fat_g = fat_g * grams,
        fiber_g = fiber_g * grams,
        sugar_g = sugar_g * grams,
        sodium_mg = sodium_mg * grams,
        saturated_fat_g = saturated_fat_g * grams
    )

    fun toMap(): Map<String, Double> = mapOf(
        "calories" to calories,
        "protein_g" to protein_g,
        "carbs_g" to carbs_g,
        "fat_g" to fat_g,
        "fiber_g" to fiber_g,
        "sugar_g" to sugar_g,
        "sodium_mg" to sodium_mg,
        "saturated_fat_g" to saturated_fat_g
    )

    companion object {
        fun fromMap(map: Map<String, Any?>?): Nutrients {
            if (map == null) return Nutrients()
            return Nutrients(
                calories = (map["calories"] as? Number)?.toDouble() ?: 0.0,
                protein_g = (map["protein_g"] as? Number)?.toDouble() ?: 0.0,
                carbs_g = (map["carbs_g"] as? Number)?.toDouble() ?: 0.0,
                fat_g = (map["fat_g"] as? Number)?.toDouble() ?: 0.0,
                fiber_g = (map["fiber_g"] as? Number)?.toDouble() ?: 0.0,
                sugar_g = (map["sugar_g"] as? Number)?.toDouble() ?: 0.0,
                sodium_mg = (map["sodium_mg"] as? Number)?.toDouble() ?: 0.0,
                saturated_fat_g = (map["saturated_fat_g"] as? Number)?.toDouble() ?: 0.0
            )
        }
    }
}

data class FoodItem(
    val id: String = generateRandomId(),
    val name: String = "Food item",
    val grams: Double = 100.0,
    val portion_description: String = "100 g",
    val per_gram: Nutrients = Nutrients()
) {
    val currentNutrients: Nutrients
        get() = per_gram * grams

    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "name" to name,
        "grams" to grams,
        "portion_description" to portion_description,
        "per_gram" to per_gram.toMap()
    )

    companion object {
        fun generateRandomId(): String {
            return UUID.randomUUID().toString().replace("-", "").take(8)
        }

        fun fromAiValues(
            name: String,
            rawGrams: Double,
            portionDesc: String,
            calories: Double,
            protein: Double,
            carbs: Double,
            fat: Double,
            fiber: Double = 0.0,
            sugar: Double = 0.0,
            sodium: Double = 0.0,
            saturatedFat: Double = 0.0
        ): FoodItem {
            val validGrams = if (rawGrams > 0.0 && rawGrams.isFinite()) max(1.0, rawGrams) else 100.0
            val cleanName = name.trim().ifEmpty { "Food item" }
            val cleanDesc = portionDesc.trim().ifEmpty { "${validGrams.roundToInt()} g" }
            val perGram = Nutrients(
                calories = max(0.0, calories) / validGrams,
                protein_g = max(0.0, protein) / validGrams,
                carbs_g = max(0.0, carbs) / validGrams,
                fat_g = max(0.0, fat) / validGrams,
                fiber_g = max(0.0, fiber) / validGrams,
                sugar_g = max(0.0, sugar) / validGrams,
                sodium_mg = max(0.0, sodium) / validGrams,
                saturated_fat_g = max(0.0, saturatedFat) / validGrams
            )
            return FoodItem(
                id = generateRandomId(),
                name = cleanName,
                grams = validGrams,
                portion_description = cleanDesc,
                per_gram = perGram
            )
        }

        fun fromMap(map: Map<String, Any?>?): FoodItem {
            if (map == null) return FoodItem()
            @Suppress("UNCHECKED_CAST")
            val perGramMap = map["per_gram"] as? Map<String, Any?>
            return FoodItem(
                id = map["id"] as? String ?: generateRandomId(),
                name = map["name"] as? String ?: "Food item",
                grams = (map["grams"] as? Number)?.toDouble() ?: 100.0,
                portion_description = map["portion_description"] as? String ?: "",
                per_gram = Nutrients.fromMap(perGramMap)
            )
        }
    }
}

data class Micronutrient(
    val name: String = "",
    val amount: String = ""
) {
    fun toMap(): Map<String, String> = mapOf(
        "name" to name,
        "amount" to amount
    )

    companion object {
        fun fromMap(map: Map<String, Any?>?): Micronutrient {
            if (map == null) return Micronutrient()
            return Micronutrient(
                name = map["name"] as? String ?: "",
                amount = map["amount"] as? String ?: ""
            )
        }
    }
}

@IgnoreExtraProperties
data class Meal(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val mealType: String = "snack", // breakfast, lunch, dinner, snack
    val eatenAt: String = "",
    val thumbnail: String? = null,
    val items: List<Map<String, Any>> = emptyList(),
    val micronutrients: List<Map<String, String>> = emptyList(),
    val calories: Double = 0.0,
    val protein_g: Double = 0.0,
    val carbs_g: Double = 0.0,
    val fat_g: Double = 0.0,
    val fiber_g: Double = 0.0,
    val sugar_g: Double = 0.0,
    val sodium_mg: Double = 0.0,
    val health_score: Int? = null,
    val health_reason: String? = null,
    val confidence: String = "high", // low, medium, high
    val notes: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun parsedItems(): List<FoodItem> {
        return items.map { FoodItem.fromMap(it) }
    }

    fun parsedMicros(): List<Micronutrient> {
        return micronutrients.map { Micronutrient.fromMap(it) }
    }
}

@IgnoreExtraProperties
data class Goal(
    val userId: String = "",
    val calories: Int = 2000,
    val protein_g: Int = 100,
    val goalType: String = "maintain", // cut, maintain, bulk
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class CalorieStatusResult(
    val state: String, // over, short, left, met
    val diff: Int,
    val isFlagged: Boolean
)

fun calculateTotals(items: List<FoodItem>): Nutrients {
    var cal = 0.0
    var pro = 0.0
    var carb = 0.0
    var fat = 0.0
    var fib = 0.0
    var sug = 0.0
    var sod = 0.0
    var sat = 0.0

    for (item in items) {
        val n = item.currentNutrients
        cal += n.calories
        pro += n.protein_g
        carb += n.carbs_g
        fat += n.fat_g
        fib += n.fiber_g
        sug += n.sugar_g
        sod += n.sodium_mg
        sat += n.saturated_fat_g
    }
    return Nutrients(
        calories = cal,
        protein_g = pro,
        carbs_g = carb,
        fat_g = fat,
        fiber_g = fib,
        sugar_g = sug,
        sodium_mg = sod,
        saturated_fat_g = sat
    )
}

fun calculateCalorieStatus(goalType: String, consumed: Double, target: Int): CalorieStatusResult {
    val diff = (consumed - target).roundToInt()
    val absDiff = abs(diff)

    return when (goalType) {
        "cut" -> {
            if (diff > 0) {
                CalorieStatusResult(state = "over", diff = diff, isFlagged = true)
            } else if (absDiff <= (target * 0.05).roundToInt()) {
                CalorieStatusResult(state = "met", diff = diff, isFlagged = false)
            } else {
                CalorieStatusResult(state = "left", diff = absDiff, isFlagged = false)
            }
        }
        "bulk" -> {
            if (diff >= 0) {
                CalorieStatusResult(state = "met", diff = diff, isFlagged = false)
            } else {
                CalorieStatusResult(state = "short", diff = absDiff, isFlagged = true)
            }
        }
        else -> { // maintain
            if (absDiff <= (target * 0.10).roundToInt()) {
                CalorieStatusResult(state = "met", diff = diff, isFlagged = false)
            } else if (diff > 0) {
                CalorieStatusResult(state = "over", diff = diff, isFlagged = true)
            } else {
                CalorieStatusResult(state = "left", diff = absDiff, isFlagged = false)
            }
        }
    }
}
