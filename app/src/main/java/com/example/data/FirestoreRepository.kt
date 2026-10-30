package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.example.model.Goal
import com.example.model.Meal
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

private const val TAG = "FirestoreRepo"

class FirestoreRepository(val firestore: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    fun observeMeals(userId: String): Flow<List<Meal>> {
        return firestore.collection("users")
            .document(userId)
            .collection("meals")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(1000)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Meal::class.java)?.let { meal ->
                        meal.copy(id = if (meal.id.isNotEmpty()) meal.id else doc.id)
                    }
                }
            }
    }

    suspend fun getMeal(userId: String, mealId: String): Meal? {
        val snapshot = firestore.collection("users")
            .document(userId)
            .collection("meals")
            .document(mealId)
            .get()
            .await()
        return snapshot.toObject(Meal::class.java)?.let { meal ->
            meal.copy(id = if (meal.id.isNotEmpty()) meal.id else snapshot.id)
        }
    }

    suspend fun saveMeal(userId: String, meal: Meal): Result<String> {
        return try {
            val mealId = meal.id.ifEmpty { firestore.collection("users").document(userId).collection("meals").document().id }
            val docRef = firestore.collection("users")
                .document(userId)
                .collection("meals")
                .document(mealId)

            val payload = mutableMapOf<String, Any?>(
                "id" to mealId,
                "userId" to userId,
                "name" to meal.name,
                "mealType" to meal.mealType,
                "eatenAt" to meal.eatenAt,
                "thumbnail" to meal.thumbnail,
                "items" to meal.items,
                "micronutrients" to meal.micronutrients,
                "calories" to meal.calories,
                "protein_g" to meal.protein_g,
                "carbs_g" to meal.carbs_g,
                "fat_g" to meal.fat_g,
                "fiber_g" to meal.fiber_g,
                "sugar_g" to meal.sugar_g,
                "sodium_mg" to meal.sodium_mg,
                "health_score" to meal.health_score,
                "health_reason" to meal.health_reason,
                "confidence" to meal.confidence,
                "notes" to meal.notes,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            docRef.set(payload).await()
            Result.success(mealId)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving meal", e)
            Result.failure(e)
        }
    }

    suspend fun updateMeal(userId: String, meal: Meal): Result<Unit> {
        return try {
            val docRef = firestore.collection("users")
                .document(userId)
                .collection("meals")
                .document(meal.id)

            val payload = mutableMapOf<String, Any?>(
                "name" to meal.name,
                "mealType" to meal.mealType,
                "items" to meal.items,
                "micronutrients" to meal.micronutrients,
                "calories" to meal.calories,
                "protein_g" to meal.protein_g,
                "carbs_g" to meal.carbs_g,
                "fat_g" to meal.fat_g,
                "fiber_g" to meal.fiber_g,
                "sugar_g" to meal.sugar_g,
                "sodium_mg" to meal.sodium_mg,
                "health_score" to meal.health_score,
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            docRef.update(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating meal", e)
            Result.failure(e)
        }
    }

    suspend fun deleteMeal(userId: String, mealId: String): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(userId)
                .collection("meals")
                .document(mealId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting meal", e)
            Result.failure(e)
        }
    }

    fun observeGoals(userId: String): Flow<Goal> = callbackFlow {
        val docRef = firestore.collection("users")
            .document(userId)
            .collection("goals")
            .document("current")

        val registration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Listen failed for goals", error)
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val goal = snapshot.toObject(Goal::class.java) ?: Goal(userId = userId)
                trySend(goal)
            } else {
                trySend(Goal(userId = userId))
            }
        }
        awaitClose { registration.remove() }
    }

    suspend fun saveGoals(userId: String, goal: Goal): Result<Unit> {
        return try {
            val docRef = firestore.collection("users")
                .document(userId)
                .collection("goals")
                .document("current")

            val payload = mapOf(
                "userId" to userId,
                "calories" to goal.calories,
                "protein_g" to goal.protein_g,
                "goalType" to goal.goalType,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving goals", e)
            Result.failure(e)
        }
    }
}
