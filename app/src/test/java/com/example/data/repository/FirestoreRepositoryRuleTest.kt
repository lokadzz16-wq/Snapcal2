package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.FirestoreRepository
import com.example.model.FoodItem
import com.example.model.Goal
import com.example.model.Meal
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class FirestoreRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun saveMeal_authenticatedOwner_createsDocument() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = FirestoreRepository(firestore)

        val mealId = UUID.randomUUID().toString()
        val meal = Meal(
            id = mealId,
            userId = uid,
            name = "Grilled Salmon",
            mealType = "dinner",
            calories = 520.0,
            protein_g = 42.0,
            carbs_g = 10.0,
            fat_g = 22.0
        )

        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveMeal(uid, meal)
        }
        assertTrue("Meal save should succeed", result.isSuccess)
    }

    @Test
    fun observeMeals_authenticatedOwner_returnsMeals() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = FirestoreRepository(firestore)

        val mealId = UUID.randomUUID().toString()
        val meal = Meal(
            id = mealId,
            userId = uid,
            name = "Avocado Toast",
            mealType = "breakfast",
            calories = 380.0,
            protein_g = 12.0,
            carbs_g = 40.0,
            fat_g = 18.0
        )

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveMeal(uid, meal)
        }

        val mealsList = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeMeals(uid).first()
        }
        assertTrue("Observed meals list should not be empty", mealsList.isNotEmpty())
        assertTrue("Observed meals should contain created meal", mealsList.any { it.id == mealId })
    }

    @Test
    fun getMeal_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = FirestoreRepository(firestore)

        val mealId = UUID.randomUUID().toString()
        val meal = Meal(
            id = mealId,
            userId = aliceUid,
            name = "Secret Recipe",
            mealType = "lunch",
            calories = 400.0,
            protein_g = 30.0,
            carbs_g = 20.0,
            fat_g = 15.0
        )
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.saveMeal(aliceUid, meal)
        }

        // Bob signs in and tries to read Alice's meal document
        signInTestUser(BOB_EMAIL)
        var permissionDenied = false
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("users").document(aliceUid).collection("meals").document(mealId).get().await()
            }
        } catch (e: Exception) {
            val firestoreException = e.unwrapFirestoreException()
            if (firestoreException?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                permissionDenied = true
            }
        }
        assertTrue("Cross user meal read should fail with permission denied", permissionDenied)
    }

    @Test
    fun saveGoals_and_observeGoals_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = FirestoreRepository(firestore)

        val goal = Goal(
            userId = uid,
            calories = 2100,
            protein_g = 140,
            goalType = "cut"
        )

        val saveRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveGoals(uid, goal)
        }
        assertTrue("Goal save should succeed: ${saveRes.exceptionOrNull()?.message ?: ""}", saveRes.isSuccess)

        val observedGoal = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeGoals(uid).first()
        }
        assertEquals(2100, observedGoal.calories)
        assertEquals(140, observedGoal.protein_g)
        assertEquals("cut", observedGoal.goalType)
    }

    companion object {
        private const val ALICE_EMAIL = "alice@test.com"
        private const val BOB_EMAIL = "bob@test.com"
        private const val DEFAULT_TIMEOUT_MS = 5000L
        private const val FLOW_TIMEOUT_MS = 3000L
    }
}
