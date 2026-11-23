package com.financia.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.financia.app.core.security.BiometricAuthHelper
import com.financia.app.ui.FinancIAViewModel
import com.financia.app.ui.components.UpdateDialog
import com.financia.app.ui.screens.AdvisorChatScreen
import com.financia.app.ui.screens.AuthScreen
import com.financia.app.ui.screens.DashboardScreen
import com.financia.app.ui.screens.SmartEntryScreen
import com.financia.app.ui.theme.FinancIATheme
import dagger.hilt.android.AndroidEntryPoint

enum class MainTab(val title: String) {
    DASHBOARD("Visão Geral"),
    SMART_ADD("Lançar IA"),
    ADVISOR("Consultor IA")
}

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FinancIATheme {
                FinancIAAppRoot(activity = this)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancIAAppRoot(
    activity: FragmentActivity,
    viewModel: FinancIAViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val monthTransactions by viewModel.currentMonthTransactions.collectAsState()
    val monthlyBudget by viewModel.monthlyBudget.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()

    var isBiometricEnabled by remember {
        mutableStateOf(BiometricAuthHelper.isBiometricEnabled(context))
    }
    var isAppUnlocked by rememberSaveable {
        mutableStateOf(!BiometricAuthHelper.isBiometricEnabled(context))
    }

    // Dispara biometria ao abrir o app se estiver ativada e usuário logado
    LaunchedEffect(currentUser, isBiometricEnabled) {
        if (currentUser != null && isBiometricEnabled && !isAppUnlocked) {
            BiometricAuthHelper.showBiometricPrompt(
                activity = activity,
                onSuccess = { isAppUnlocked = true },
                onError = {}
            )
        }
    }

    // Modal de Atualização Automática OTA via GitHub
    if (uiState.showUpdateDialog && uiState.updateInfo != null) {
        UpdateDialog(
            updateInfo = uiState.updateInfo!!,
            isDownloading = uiState.isDownloadingUpdate,
            downloadProgress = uiState.updateProgress,
            downloadProgressText = uiState.updateProgressText,
            downloadedFile = uiState.downloadedApkFile,
            updateError = uiState.updateError,
            canInstall = viewModel.canRequestPackageInstalls(),
            onDismiss = viewModel::dismissUpdateDialog,
            onDownload = viewModel::startUpdateDownload,
            onInstall = viewModel::installDownloadedApk,
            onRequestPermission = viewModel::requestInstallPermission
        )
    }

    if (currentUser == null) {
        AuthScreen(
            isLoading = uiState.isLoadingAuth,
            errorMessage = uiState.authError,
            onSignIn = viewModel::signIn,
            onSignUp = viewModel::signUp,
            onGoogleSignIn = viewModel::signInWithGoogle,
            onGoogleSignInError = viewModel::setAuthError,
            onGuestSignIn = viewModel::signInAsGuest,
            onClearError = viewModel::clearAuthError
        )
        return
    }

    // Tela de Bloqueio Biométrico caso o app esteja trancado
    if (isBiometricEnabled && !isAppUnlocked) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Biometria",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "FinancIA Protegido",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Confirme sua digital ou PIN para acessar seus dados financeiros.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        BiometricAuthHelper.showBiometricPrompt(
                            activity = activity,
                            onSuccess = { isAppUnlocked = true },
                            onError = { err ->
                                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                ) {
                    Text("Desbloquear com Biometria")
                }
            }
        }
        return
    }

    var currentTab by rememberSaveable { mutableStateOf(MainTab.DASHBOARD) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "FinancIA • ${currentTab.title}",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    // Botão para Ativar/Desativar Bloqueio Biométrico
                    IconButton(
                        onClick = {
                            if (!BiometricAuthHelper.isBiometricAvailable(context)) {
                                Toast.makeText(
                                    context,
                                    "Configure uma digital ou bloqueio de tela nas configurações do Android.",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                val nextState = !isBiometricEnabled
                                BiometricAuthHelper.setBiometricEnabled(context, nextState)
                                isBiometricEnabled = nextState
                                isAppUnlocked = true
                                val msg = if (nextState) {
                                    "Proteção por Biometria ATIVADA!"
                                } else {
                                    "Proteção por Biometria DESATIVADA."
                                }
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isBiometricEnabled) Icons.Default.Fingerprint else Icons.Default.Lock,
                            contentDescription = "Bloqueio Biométrico",
                            tint = if (isBiometricEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }

                    // Botão de Atualização Automática OTA
                    IconButton(onClick = { viewModel.checkForGitHubUpdates(silent = false) }) {
                        BadgedBox(
                            badge = {
                                if (uiState.updateInfo?.hasUpdate == true) {
                                    Badge()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = "Verificar Atualização do App"
                            )
                        }
                    }

                    IconButton(onClick = viewModel::signOut) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Sair da conta"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentTab == MainTab.DASHBOARD,
                    onClick = { currentTab = MainTab.DASHBOARD },
                    icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) },
                    label = { Text("Início") }
                )
                NavigationBarItem(
                    selected = currentTab == MainTab.SMART_ADD,
                    onClick = { currentTab = MainTab.SMART_ADD },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                    label = { Text("Lançar IA") }
                )
                NavigationBarItem(
                    selected = currentTab == MainTab.ADVISOR,
                    onClick = { currentTab = MainTab.ADVISOR },
                    icon = { Icon(Icons.Default.ChatBubbleOutline, contentDescription = null) },
                    label = { Text("Consultor") }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentTab) {
                MainTab.DASHBOARD -> {
                    DashboardScreen(
                        transactions = monthTransactions,
                        selectedMonth = uiState.selectedMonth,
                        selectedYear = uiState.selectedYear,
                        searchQuery = uiState.searchQuery,
                        selectedCategory = uiState.selectedCategoryFilter,
                        monthlyBudget = monthlyBudget,
                        isPrivacyMode = uiState.isPrivacyMode,
                        onPreviousMonth = viewModel::previousMonth,
                        onNextMonth = viewModel::nextMonth,
                        onSearchQueryChange = viewModel::setSearchQuery,
                        onSelectCategory = viewModel::setCategoryFilter,
                        onTogglePrivacyMode = viewModel::togglePrivacyMode,
                        onSaveMonthlyBudget = viewModel::updateMonthlyBudget,
                        onEditTransaction = viewModel::updateExistingTransaction,
                        onDeleteTransaction = viewModel::deleteTransaction,
                        onOpenSmartAdd = { currentTab = MainTab.SMART_ADD }
                    )
                }

                MainTab.SMART_ADD -> {
                    SmartEntryScreen(
                        isProcessingAi = uiState.isProcessingAi,
                        aiStatusMessage = uiState.aiStatusMessage,
                        pendingAiDraft = uiState.pendingAiDraft,
                        onAnalyzeText = viewModel::processNaturalLanguageInput,
                        onAnalyzeReceiptBitmap = viewModel::processReceiptPhoto,
                        onAutoCategorize = viewModel::autoCategorizeTitle,
                        onSaveTransaction = viewModel::addManualTransaction,
                        onTransactionSavedNavigateHome = {
                            viewModel.clearPendingAiDraft()
                            currentTab = MainTab.DASHBOARD
                        }
                    )
                }

                MainTab.ADVISOR -> {
                    AdvisorChatScreen(
                        messages = chatMessages,
                        isThinking = uiState.isAdvisorThinking,
                        onSendMessage = viewModel::sendAdvisorMessage,
                        onClearChat = viewModel::clearAdvisorChat
                    )
                }
            }
        }
    }
}
