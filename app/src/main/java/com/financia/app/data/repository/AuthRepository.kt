package com.financia.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    val authStateFlow: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser> = runCatching {
        val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
        val user = result.user ?: error("Usuário não encontrado após login.")
        ensureUserProfile(user)
        user
    }

    suspend fun signUpWithEmail(name: String, email: String, password: String): Result<FirebaseUser> = runCatching {
        val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        val user = result.user ?: error("Erro ao criar conta.")
        if (name.isNotBlank()) {
            val profileUpdates = userProfileChangeRequest {
                displayName = name.trim()
            }
            user.updateProfile(profileUpdates).await()
        }
        ensureUserProfile(user, customName = name.trim())
        user
    }

    suspend fun signInWithGoogleIdToken(idToken: String): Result<FirebaseUser> = runCatching {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        val user = result.user ?: error("Não foi possível autenticar com a Conta Google.")
        ensureUserProfile(user)
        user
    }

    suspend fun signInAnonymously(): Result<FirebaseUser> = runCatching {
        val result = auth.signInAnonymously().await()
        val user = result.user ?: error("Não foi possível iniciar sessão de convidado.")
        ensureUserProfile(user, customName = "Convidado")
        user
    }

    private suspend fun ensureUserProfile(user: FirebaseUser, customName: String? = null) {
        val profileData = mutableMapOf<String, Any>(
            "displayName" to (customName ?: user.displayName ?: "Usuário FinancIA").take(120),
            "currency" to "BRL",
            "updatedAt" to System.currentTimeMillis()
        )
        user.email?.takeIf { it.isNotBlank() }?.let {
            profileData["email"] = it.take(320)
        }
        runCatching {
            firestore.collection("users")
                .document(user.uid)
                .set(profileData, SetOptions.merge())
                .await()
        }
    }

    fun observeMonthlyBudget(userId: String): Flow<Double> = callbackFlow {
        val subscription = firestore.collection("users")
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(0.0)
                    return@addSnapshotListener
                }
                val budget = snapshot?.getDouble("monthlyBudget") ?: 0.0
                trySend(budget)
            }
        awaitClose { subscription.remove() }
    }

    @Suppress("UNCHECKED_CAST")
    fun observeCategoryBudgets(userId: String): Flow<Map<String, Double>> = callbackFlow {
        val subscription = firestore.collection("users")
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyMap())
                    return@addSnapshotListener
                }
                val rawMap = snapshot?.get("categoryBudgets") as? Map<String, Any> ?: emptyMap()
                val parsed = rawMap.mapNotNull { (k, v) ->
                    val num = (v as? Number)?.toDouble() ?: return@mapNotNull null
                    k to num
                }.toMap()
                trySend(parsed)
            }
        awaitClose { subscription.remove() }
    }

    suspend fun updateMonthlyBudget(budget: Double): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Usuário não autenticado.")
        val data = mapOf(
            "monthlyBudget" to budget.coerceAtLeast(0.0),
            "updatedAt" to System.currentTimeMillis()
        )
        firestore.collection("users")
            .document(uid)
            .set(data, SetOptions.merge())
            .await()
    }

    suspend fun updateCategoryBudget(categoryName: String, limit: Double, existing: Map<String, Double>): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Usuário não autenticado.")
        val newMap = existing.toMutableMap().apply {
            if (limit <= 0.0) remove(categoryName) else put(categoryName, limit)
        }
        val data = mapOf(
            "categoryBudgets" to newMap,
            "updatedAt" to System.currentTimeMillis()
        )
        firestore.collection("users")
            .document(uid)
            .set(data, SetOptions.merge())
            .await()
    }

    fun signOut() {
        auth.signOut()
    }
}
