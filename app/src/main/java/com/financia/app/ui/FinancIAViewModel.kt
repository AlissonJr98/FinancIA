package com.financia.app.ui

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financia.app.core.update.AppUpdateInfo
import com.financia.app.core.update.GitHubUpdateChecker
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
import java.io.File
import java.util.Locale
import javax.inject.Inject

data class FinancIAUiState(
    val isLoadingAuth: Boolean = false,
    val authError: String? = null,
    val isProcessingAi: Boolean = false,
    val aiStatusMessage: String? = null,
    val pendingAiDraft: AiExtractedTransaction? = null,
    val isAdvisorThinking: Boolean = false,
    val selectedCategoryFilter: String? = null,
    // OTA Update States
    val updateInfo: AppUpdateInfo? = null,
    val showUpdateDialog: Boolean = false,
    val isDownloadingUpdate: Boolean = false,
    val updateProgress: Float = 0f,
    val updateProgressText: String = "",
    val downloadedApkFile: File? = null,
    val updateError: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FinancIAViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val transactionRepository: TransactionRepository,
    private val aiRepository: AiFinancialRepository,
    private val updateChecker: GitHubUpdateChecker
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

    init {
        checkForGitHubUpdates(silent = true)
    }

    // --- GitHub OTA Auto-Update ---

    fun checkForGitHubUpdates(silent: Boolean = false) {
        viewModelScope.launch {
            updateChecker.checkForUpdates()
                .onSuccess { info ->
                    if (!silent || info.hasUpdate) {
                        _uiState.update {
                            it.copy(
                                updateInfo = info,
                                showUpdateDialog = true,
                                updateError = null
                            )
                        }
                    } else {
                        _uiState.update { it.copy(updateInfo = info) }
                    }
                }
                .onFailure { err ->
                    if (!silent) {
                        _uiState.update {
                            it.copy(
                                updateError = err.localizedMessage ?: "Não foi possível verificar atualizações no GitHub.",
                                showUpdateDialog = true
                            )
                        }
                    }
                }
        }
    }

    fun startUpdateDownload(apkUrl: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isDownloadingUpdate = true,
                    updateProgress = 0f,
                    updateProgressText = "Iniciando download do GitHub...",
                    updateError = null,
                    downloadedApkFile = null
                )
            }

            updateChecker.downloadApkInApp(
                apkUrl = apkUrl,
                onProgress = { progress, downloadedMb, totalMb ->
                    val pct = (progress * 100).toInt()
                    val text = String.format(Locale.US, "%.1f MB / %.1f MB (%d%%)", downloadedMb, totalMb, pct)
                    _uiState.update {
                        it.copy(
                            updateProgress = progress,
                            updateProgressText = text
                        )
                    }
                },
                onSuccess = { file ->
                    _uiState.update {
                        it.copy(
                            isDownloadingUpdate = false,
                            updateProgress = 1f,
                            downloadedApkFile = file
                        )
                    }
                    if (updateChecker.canRequestPackageInstalls()) {
                        updateChecker.installApk(file)
                    }
                },
                onError = { errorMsg ->
                    _uiState.update {
                        it.copy(
                            isDownloadingUpdate = false,
                            updateError = errorMsg
                        )
                    }
                }
            )
        }
    }

    fun canRequestPackageInstalls(): Boolean = updateChecker.canRequestPackageInstalls()

    fun requestInstallPermission() {
        updateChecker.openInstallPermissionSettings()
    }

    fun installDownloadedApk() {
        val file = _uiState.value.downloadedApkFile ?: return
        updateChecker.installApk(file)
    }

    fun dismissUpdateDialog() {
        _uiState.update { it.copy(showUpdateDialog = false, updateError = null) }
    }

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
