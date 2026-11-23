# ✨ FinancIA — Controle Financeiro Inteligente com IA

![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-Auth%20%7C%20Firestore%20%7C%20AI%20Logic-FFCA28?logo=firebase&logoColor=black)
![Gemini AI](https://img.shields.io/badge/Gemini%202.5%20Flash-Multimodal%20AI-8E75B2?logo=googlegemini&logoColor=white)
![Min SDK](https://img.shields.io/badge/Min%20SDK-26%20(Android%208.0+)-3DDC84?logo=android&logoColor=white)

O **FinancIA** é um aplicativo Android nativo de gestão financeira pessoal construído com **Kotlin**, **Jetpack Compose** e **Firebase**, potencializado pelo **Gemini (Firebase AI Logic)** para tornar o registro de gastos e a análise de orçamento simples, rápidos e inteligentes.

---

## 📲 Download Direto do APK (Instalar no Celular)

Você pode baixar e instalar o APK oficial diretamente no seu celular Android através dos links abaixo:

- 📦 **[Baixar FinancIA.apk (Última Release Oficial v1.0.0)](https://github.com/AlissonJr98/FinancIA/releases/latest/download/FinancIA.apk)**
- 📦 **[Download Alternativo Direto do Repositório (`FinancIA.apk`)](https://github.com/AlissonJr98/FinancIA/raw/main/FinancIA.apk)**
- 🔗 **[Página de Releases no GitHub](https://github.com/AlissonJr98/FinancIA/releases)**

### 🔄 Atualização Automática OTA Integrada
O aplicativo conta com um sistema nativo de **Atualização Automática Over-The-Air (OTA)** conectado a este repositório GitHub:
- Sempre que o app é aberto (ou quando você toca no ícone de atualização na barra superior), ele verifica o arquivo `version.json` e a última Release do GitHub (`AlissonJr98/FinancIA`).
- Quando uma versão mais nova é publicada no GitHub, o app exibe um alerta com as novidades, baixa o novo `.apk` com barra de progresso dentro do próprio aplicativo e abre o instalador nativo do Android automaticamente!

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

- **Linguagem**: Kotlin `2.2.10`
- **Interface (UI)**: Jetpack Compose + Material Design 3
- **Arquitetura**: MVVM (`FinancIAViewModel` + `StateFlow` + `Repositories`)
- **Injeção de Dependência**: Dagger Hilt + KSP
- **Atualização OTA**: `GitHubUpdateChecker` + `FileProvider` + GitHub Actions (`release.yml`)
- **Backend Serverless & IA**:
  - `com.google.firebase:firebase-auth` (Autenticação)
  - `com.google.firebase:firebase-firestore` (Banco de Dados NoSQL em Tempo Real)
  - `com.google.firebase:firebase-ai` (Firebase AI Logic / Gemini Developer API)

---

## ⚙️ Como Lançar uma Nova Atualização Automática

Para enviar uma atualização para todos os celulares que têm o **FinancIA** instalado:
1. Aumente o `versionCode` e o `versionName` (ex: `"1.0.1"`) em `app/build.gradle.kts` e `version.json`.
2. Gere o novo APK ou envie uma tag `git tag v1.0.1 && git push origin v1.0.1` (o GitHub Actions em `.github/workflows/release.yml` compila e publica a Release automaticamente).
3. Na próxima vez que você abrir o app no celular, o modal de atualização aparecerá automaticamente!

---

Desenvolvido por **[Alisson Jr](https://github.com/AlissonJr98)**.
