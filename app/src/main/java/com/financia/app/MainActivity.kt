package com.financia.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
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
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FinancIATheme {
                FinancIAAppRoot()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancIAAppRoot(
    viewModel: FinancIAViewModel = hiltViewModel()
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()

    // Modal de Atualização Automática OTA via GitHub (exibe tanto no login quanto logado)
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
            onGuestSignIn = viewModel::signInAsGuest,
            onClearError = viewModel::clearAuthError
        )
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
                                contentDescription = "Verificar Atualização no GitHub"
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
                        transactions = transactions,
                        selectedCategory = uiState.selectedCategoryFilter,
                        onSelectCategory = viewModel::setCategoryFilter,
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
