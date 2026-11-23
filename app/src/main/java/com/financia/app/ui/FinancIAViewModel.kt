package com.financia.app.ui

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financia.app.data.model.AiExtractedTransaction
import com.financia.app.data.model.ChatMessage
import com.financia.app.data.model.FinancialTransaction
import com.financia.app.data.model.TransactionCategory
import com.financia.app.data.model.TransactionType
import com.financia.app.data.repository.AiFinancialRepository
import com.financia.app.data.repository.AuthRepository
import com.financia.app.data.repository.TransactionRepository
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FinancIAUiState(
    val isLoadingAuth: Boolean = false,
    val authError: String? = null,
    val isProcessingAi: Boolean = false,
    val aiStatusMessage: String? = null,
    val pendingAiDraft: AiExtractedTransaction? = null,
    val isAdvisorThinking: Boolean = false,
    val selectedCategoryFilter: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FinancIAViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val transactionRepository: TransactionRepository,
    private val aiRepository: AiFinancialRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FinancIAUiState())
    val uiState: StateFlow<FinancIAUiState> = _uiState.asStateFlow()

    val currentUser: StateFlow<FirebaseUser?> = authRepository.authStateFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), authRepository.currentUser)

    val transactions: StateFlow<List<FinancialTransaction>> = currentUser
        .flatMapLatest { user ->
            if (user != null) {
                transactionRepository.observeTransactions(user.uid)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chatMessages: StateFlow<List<ChatMessage>> = currentUser
        .flatMapLatest { user ->
            if (user != null) {
                transactionRepository.observeChatMessages(user.uid)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Authentication ---

    fun signIn(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _uiState.update { it.copy(authError = "Preencha e-mail e senha.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingAuth = true, authError = null) }
            authRepository.signInWithEmail(email, pass)
                .onFailure { err ->
                    _uiState.update {
                        it.copy(authError = err.localizedMessage ?: "Falha na autenticação.")
                    }
                }
            _uiState.update { it.copy(isLoadingAuth = false) }
        }
    }

    fun signUp(name: String, email: String, pass: String) {
        if (email.isBlank() || pass.length < 6) {
            _uiState.update { it.copy(authError = "Informe um e-mail válido e senha com 6+ caracteres.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingAuth = true, authError = null) }
            authRepository.signUpWithEmail(name, email, pass)
                .onFailure { err ->
                    _uiState.update {
                        it.copy(authError = err.localizedMessage ?: "Não foi possível criar a conta.")
                    }
                }
            _uiState.update { it.copy(isLoadingAuth = false) }
        }
    }

    fun signInAsGuest() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingAuth = true, authError = null) }
            authRepository.signInAnonymously()
                .onFailure { err ->
                    _uiState.update {
                        it.copy(authError = err.localizedMessage ?: "Erro ao entrar como convidado.")
                    }
                }
            _uiState.update { it.copy(isLoadingAuth = false) }
        }
    }

    fun signOut() {
        authRepository.signOut()
    }

    fun clearAuthError() {
        _uiState.update { it.copy(authError = null) }
    }

    // --- Transactions ---

    fun setCategoryFilter(category: String?) {
        _uiState.update { it.copy(selectedCategoryFilter = category) }
    }

    fun addManualTransaction(
        title: String,
        amount: Double,
        isIncome: Boolean,
        category: String,
        paymentMethod: String,
        notes: String,
        createdByAi: Boolean = false
    ) {
        if (title.isBlank() || amount <= 0.0) return
        viewModelScope.launch {
            val tx = FinancialTransaction(
                title = title.trim(),
                amount = amount,
                type = if (isIncome) TransactionType.INCOME.name else TransactionType.EXPENSE.name,
                category = category,
                paymentMethod = paymentMethod,
                notes = notes.trim(),
                createdByAi = createdByAi
            )
            transactionRepository.addTransaction(tx)
            _uiState.update { it.copy(pendingAiDraft = null, aiStatusMessage = "Lançamento salvo na nuvem!") }
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(id)
        }
    }

    // --- AI Smart Entry (Natural Language, Voice & Receipt Photo) ---

    fun processNaturalLanguageInput(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessingAi = true,
                    aiStatusMessage = "Analisando com Gemini IA..."
                )
            }
            val result = aiRepository.parseNaturalLanguageTransaction(text)
            val extracted = result.getOrElse {
                aiRepository.fallbackHeuristicParse(text)
            }
            _uiState.update {
                it.copy(
                    isProcessingAi = false,
                    pendingAiDraft = extracted,
                    aiStatusMessage = "Campos preenchidos pela IA! Revise e confirme."
                )
            }
        }
    }

    fun processReceiptPhoto(bitmap: Bitmap) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessingAi = true,
                    aiStatusMessage = "Lendo comprovante com Visão Computacional Gemini..."
                )
            }
            aiRepository.parseReceiptImage(bitmap)
                .onSuccess { extracted ->
                    _uiState.update {
                        it.copy(
                            isProcessingAi = false,
                            pendingAiDraft = extracted,
                            aiStatusMessage = "Comprovante analisado com sucesso! Revise os dados."
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isProcessingAi = false,
                            aiStatusMessage = "Não foi possível ler o comprovante: ${err.localizedMessage ?: "tente novamente"}"
                        )
                    }
                }
        }
    }

    fun autoCategorizeTitle(title: String, onCategorySuggested: (TransactionCategory) -> Unit) {
        if (title.length < 3) return
        viewModelScope.launch {
            aiRepository.suggestCategory(title)
                .onSuccess { onCategorySuggested(it) }
                .onFailure {
                    val fallback = aiRepository.fallbackHeuristicParse(title)
                    onCategorySuggested(TransactionCategory.fromLabelOrName(fallback.category))
                }
        }
    }

    fun clearPendingAiDraft() {
        _uiState.update { it.copy(pendingAiDraft = null, aiStatusMessage = null) }
    }

    // --- AI Financial Advisor Chat ---

    fun sendAdvisorMessage(question: String) {
        if (question.isBlank()) return
        viewModelScope.launch {
            val userMsg = ChatMessage(role = "user", content = question.trim())
            transactionRepository.saveChatMessage(userMsg)
            _uiState.update { it.copy(isAdvisorThinking = true) }

            val currentTx = transactions.value
            val answer = aiRepository.askFinancialAdvisor(question, currentTx)
                .getOrElse { err ->
                    "Desculpe, encontrei um problema ao consultar o Gemini: ${err.localizedMessage ?: "verifique se o Firebase AI Logic está ativado no console."}"
                }

            val aiMsg = ChatMessage(role = "model", content = answer)
            transactionRepository.saveChatMessage(aiMsg)
            _uiState.update { it.copy(isAdvisorThinking = false) }
        }
    }

    fun clearAdvisorChat() {
        viewModelScope.launch {
            transactionRepository.clearChatHistory()
        }
    }
}
