# LSP Senior — Suporte à Linguagem Senior de Programação

Trabalha com regras, telas e relatórios do **ERP Senior**? Este plugin transforma o IntelliJ IDEA num editor de verdade para a **Linguagem Senior de Programação (LSP)**, com colorização, navegação e produtividade que você não tem dentro do SGI.

Funciona automaticamente em arquivos **`.lsp`** e **`.lspt`** — é só abrir.

---

## ✨ Recursos

### 🎨 Colorização de sintaxe
Destaque de cores por categoria, deixando o código fácil de ler:
- **Controle** — `Se`, `Senao`, `Enquanto`, `Para`, `Regra`...
- **Tipos** — `Alfa`, `Numero`, `Data`, `Lista`, `Booleano`...
- **Booleanos** — `cVerdadeiro`, `cFalso`
- **Funções** do ERP e do RH, **operadores**, **comentários** e **textos**

### 🧭 Navegação inteligente
- **Ir para a declaração** (`Ctrl/Cmd+B` ou `Ctrl/Cmd+Click`):
  - **Funções** → pula direto para a implementação `Funcao nome()` em qualquer arquivo do projeto.
  - **Variáveis** → vai até o `Definir`, respeitando o **escopo do bloco** (a mesma variável em blocos diferentes não se confunde).
- **Localizar usos** (`Alt+Shift+F7` ou menu de contexto):
  - **Funções** → todos os usos no projeto.
  - **Variáveis** → usos no arquivo, dentro do escopo.

### ⚡ Produtividade
- **Bracket matching** e **auto-close** para `{ }`, `( )`, `[ ]` e `Inicio` / `Fim`
- **Code folding** de blocos `{ }` e `Inicio` / `Fim`
- **Snippets** para estruturas comuns (`se`, `enquanto`, `para`, `funcao`...)
- **Ícone próprio** para arquivos `.lsp` / `.lspt`
- **Corretor ortográfico desativado** nesses arquivos — sem sublinhados vermelhos em identificadores e texto em português

---

## 📦 Requisitos

- IntelliJ IDEA **2025.1 ou superior** (Community ou Ultimate)
- O plugin **TextMate** (já vem incluso na IDE)

---

## ⚠️ Após instalar, reinicie a IDE

A colorização depende de um bundle que **só carrega na inicialização**. Se instalou e o código não ficou colorido, faça um **restart completo** do IntelliJ — não basta o "load without restart".

---

## 💬 Feedback

Encontrou um problema ou quer sugerir uma função/palavra reservada que faltou? Abra uma issue no repositório do projeto. Contribuições são bem-vindas.
