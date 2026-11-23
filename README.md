# ✨ FinancIA — Controle Financeiro Inteligente com IA

![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-Auth%20%7C%20Firestore%20%7C%20AI%20Logic-FFCA28?logo=firebase&logoColor=black)
![Gemini AI](https://img.shields.io/badge/Gemini%202.5%20Flash-Multimodal%20AI-8E75B2?logo=googlegemini&logoColor=white)
![Versão](https://img.shields.io/badge/Vers%C3%A3o-1.1.0-00897B)

O **FinancIA** é um aplicativo Android nativo de gestão financeira pessoal construído com **Kotlin**, **Jetpack Compose** e **Firebase**, potencializado pelo **Gemini (Firebase AI Logic)** para tornar o registro de gastos e a análise de orçamento simples, rápidos e automatizados.

---

## 📲 Download Direto do APK (Instalar no Celular)

Você pode baixar e instalar o APK oficial diretamente no seu celular Android através dos links abaixo:

- 📦 **[Baixar FinancIA.apk (Última Release Oficial v1.1.0)](https://github.com/AlissonJr98/FinancIA/releases/latest/download/FinancIA.apk)**
- 📦 **[Download Alternativo Direto do Repositório (`FinancIA.apk`)](https://github.com/AlissonJr98/FinancIA/raw/main/FinancIA.apk)**
- 🔗 **[Página de Releases no GitHub](https://github.com/AlissonJr98/FinancIA/releases)**

### 🔄 Atualização Automática OTA Integrada
Se você já instalou a `v1.0.0` no celular, basta abrir o app (ou tocar no ícone de atualização no topo) e ele detectará a **`v1.1.0`** automaticamente, baixará o pacote com barra de progresso dentro do app e atualizará sozinho!

---

## 🚀 Funcionalidades Completas (v1.1.0)

### 1. 🔐 Login com Conta Google, E-mail/Senha, Convidado e Biometria
- **Entrar com Conta Google** com 1 toque (`GoogleSignIn` + Firebase Auth).
- **Desbloqueio por Biometria / Impressão Digital** (`BiometricPrompt`) ativável na barra superior do app.
- **Modo Privacidade (👁️)**: Esconda os valores de saldo, receitas e despesas na tela inicial quando estiver em público.

### 2. 🔔 Automação Zero-Toque (Captura Automática de Notificações Bancárias)
- Serviço inteligente (`BankNotificationService`) que lê notificações de Pix e compras de bancos brasileiros (**Nubank, Inter, Itaú, Bradesco, Santander, C6 Bank, PicPay, Mercado Pago, Banco do Brasil, Caixa, XP, BTG**).
- Extrai automaticamente o estabelecimento, valor, tipo (Receita/Despesa), categoria e forma de pagamento via IA e salva no Firestore sem você precisar abrir o app!

### 3. 🪄 Lançamento Mágico por Texto, Voz e Foto + Recorrência e Parcelamento
- Digite ou fale frases como *"Comprei tênis 300 em 3x no cartão"* ou *"Gastei 45,90 no mercado no Pix"*.
- Tire foto de **Comprovantes e Notas Fiscais** com a câmera ou galeria (Visão Computacional Gemini).
- **Lançamentos Fixos Mensais** (Aluguel, Internet, Salário) e **Parcelamento Automático** (`2x` até `12x` lançados automaticamente nos meses seguintes).

### 4. 💬 Consultor & Agente Financeiro (Chat IA com Function Calling)
- Converse com o assistente Gemini conectado aos seus dados do mês em tempo real.
- **Lançamento Direto pelo Chat**: Peça *"Registra aí que gastei 32 na padaria e 150 de gasolina"* e o próprio chat cria os dois lançamentos no Firestore e confirma para você!

### 5. 📊 Dashboard Avançado, Gráfico de Rosca, Meta Mensal e Exportação Excel/CSV
- **Navegação por Mês/Ano** (`< Novembro 2026 >`) e **Busca Instantânea** por nome, categoria ou observação.
- **Score de Saúde Financeira (0 a 100)** e **Meta de Orçamento Mensal (Budget)** com barra de progresso inteligente (Verde → Amarelo → Vermelho).
- **Gráfico de Rosca (Donut Chart)** interativo com distribuição percentual por categoria.
- **Edição Rápida**: Toque em qualquer lançamento para editar seus dados.
- **Exportação Excel/CSV**: Exporte e compartilhe o relatório completo do mês pelo WhatsApp, E-mail ou Google Sheets com 1 toque.

---

Desenvolvido por **[Alisson Jr](https://github.com/AlissonJr98)**.
