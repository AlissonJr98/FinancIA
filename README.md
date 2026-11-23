# ✨ FinancIA — Controle Financeiro Inteligente com IA

![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-Auth%20%7C%20Firestore%20%7C%20AI%20Logic-FFCA28?logo=firebase&logoColor=black)
![Gemini AI](https://img.shields.io/badge/Gemini%202.5%20Flash-Multimodal%20AI-8E75B2?logo=googlegemini&logoColor=white)
![Min SDK](https://img.shields.io/badge/Min%20SDK-26%20(Android%208.0+)-3DDC84?logo=android&logoColor=white)

O **FinancIA** é um aplicativo Android nativo de gestão financeira pessoal construído com **Kotlin**, **Jetpack Compose** e **Firebase**, potencializado pelo **Gemini (Firebase AI Logic)** para tornar o registro de gastos e a análise de orçamento simples, rápidos e inteligentes.

---

## 🚀 Principais Funcionalidades

### 1. 🪄 Lançamento Mágico por Linguagem Natural (Texto e Voz)
- Digite ou fale frases naturais em português como:
  - *"Gastei 45,90 no mercado no Pix"*
  - *"Uber 24,50 no cartão de crédito"*
  - *"Recebi 3500 de salário"*
- O motor de IA (**Gemini 2.5 Flash** com *Structured Outputs JSON*) interpreta automaticamente:
  - **Descrição** (`title`)
  - **Valor** (`amount`)
  - **Tipo** (`EXPENSE` ou `INCOME`)
  - **Categoria** (`Alimentação`, `Transporte`, `Moradia`, `Saúde`, `Lazer`, `Assinaturas`, `Salário`, etc.)
  - **Forma de Pagamento** (`Pix`, `Cartão de Crédito`, `Cartão de Débito`, `Dinheiro`, `Boleto`)

### 2. 📸 Leitura de Comprovantes e Notas Fiscais por Foto (Visão Computacional)
- Tire uma foto com a câmera ou selecione uma imagem da galeria de um cupom fiscal/recibo.
- A IA Multimodal do Gemini analisa a imagem e preenche automaticamente o estabelecimento, valor total, categoria sugerida e resumo dos itens.

### 3. 🏷️ Auto-Categorização Inteligente
- Ao digitar o título de um lançamento manual, toque no botão **`✨ IA`** ao lado do campo de descrição para sugerir a categoria ideal instantaneamente.

### 4. 💬 Consultor Financeiro Pessoal (Chat IA Contextual)
- Converse com o assistente financeiro integrado ao seu histórico em tempo real no **Cloud Firestore**.
- Receba diagnósticos do seu saldo atual, análise das categorias onde você mais gasta e dicas práticas para economizar.

### 5. ☁️ Sincronização em Tempo Real & Segurança na Nuvem
- **Firebase Authentication**: Login com E-mail/Senha, Cadastro ou **Modo Convidado** com 1 toque.
- **Cloud Firestore**: Sincronização instantânea na nuvem (`southamerica-east1`) com suporte offline nativo e regras de segurança (`firestore.rules`) isoladas por usuário (`request.auth.uid == userId`).

---

## 🏗️ Arquitetura e Tecnologias

- **Linguagem**: Kotlin `2.0.21`
- **Interface (UI)**: Jetpack Compose + Material Design 3
- **Arquitetura**: MVVM (`FinancIAViewModel` + `StateFlow` + `Repositories`)
- **Injeção de Dependência**: Dagger Hilt + KSP
- **Backend Serverless & IA**:
  - `com.google.firebase:firebase-auth` (Autenticação)
  - `com.google.firebase:firebase-firestore` (Banco de Dados NoSQL em Tempo Real)
  - `com.google.firebase:firebase-ai` (Firebase AI Logic / Gemini Developer API)
- **Serialização**: `kotlinx.serialization-json`

### 📂 Estrutura de Pastas

```text
FinancIA/
├── app/
│   ├── google-services.json                 # Configuração do projeto Firebase
│   ├── build.gradle.kts                     # Dependências do módulo Android
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── java/com/financia/app/
│           ├── FinancIAApplication.kt       # Classe Application (@HiltAndroidApp)
│           ├── MainActivity.kt              # Navegação principal (Início, Lançar IA, Consultor)
│           ├── di/
│           │   └── AppModule.kt             # Módulo Hilt (FirebaseAuth, Firestore, FirebaseAI)
│           ├── data/
│           │   ├── model/Models.kt          # Modelos de domínio e categorias
│           │   └── repository/
│           │       ├── AuthRepository.kt
│           │       ├── TransactionRepository.kt
│           │       └── AiFinancialRepository.kt
│           └── ui/
│               ├── FinancIAViewModel.kt     # Gerenciamento de estado (MVVM)
│               ├── theme/Theme.kt           # Tema Material 3 (Emerald & AI Purple)
│               └── screens/
│                   ├── AuthScreen.kt        # Tela de Login / Cadastro / Convidado
│                   ├── DashboardScreen.kt   # Resumo de saldo, categorias e histórico
│                   ├── SmartEntryScreen.kt  # Lançamento por Texto, Voz e Foto com IA
│                   └── AdvisorChatScreen.kt # Chat com o Consultor Financeiro Gemini
├── firestore.rules                          # Regras de segurança do Cloud Firestore
├── firestore.indexes.json                   # Índices do Firestore
├── firebase.json                            # Configuração do Firebase CLI
└── gradle/libs.versions.toml                # Catálogo de versões Gradle
```

---

## 📲 Como Compilar e Instalar no Celular

### Opção 1: Pelo Android Studio
1. Clone este repositório:
   ```bash
   git clone https://github.com/AlissonJr98/FinancIA.git
   ```
2. Abra a pasta `FinancIA` no **Android Studio**.
3. Conecte seu celular Android via cabo USB (ou depuração por Wi-Fi) com a **Depuração USB** ativada nas *Opções do Desenvolvedor*.
4. Clique em **Run (`▶`)** para compilar e instalar diretamente no aparelho.

### Opção 2: Gerando o APK de Instalação
No Android Studio, acesse o menu **Build > Build Bundle(s) / APK(s) > Build APK(s)**. O arquivo `.apk` será gerado em:
`app/build/outputs/apk/debug/app-debug.apk`
Basta enviar o `.apk` para o celular e instalar!

---

## ⚙️ Configuração do Firebase AI Logic (Gemini)

O projeto já está vinculado ao Firebase (`financia-fb35f`). Para garantir que as chamadas ao modelo `gemini-2.5-flash` estejam habilitadas na nuvem:
1. Acesse o [Console do Firebase AI Logic](https://console.firebase.google.com/project/financia-fb35f/ailogic).
2. Ative o provedor **Gemini Developer API** (possui camada gratuita).
3. Caso o serviço esteja offline ou sem conexão, o app conta com um **motor heurístico local de fallback** que continua extraindo valores, categorias e formas de pagamento automaticamente!

---

Desenvolvido por **[Alisson Jr](https://github.com/AlissonJr98)**.
