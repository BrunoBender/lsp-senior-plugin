# LSP Senior — Suporte à Linguagem Senior de Programação

Trabalha com regras, telas e relatórios do **ERP Senior**? Este plugin transforma o IntelliJ IDEA num editor de verdade para a **Linguagem Senior de Programação (LSP)**, com colorização, navegação, autocomplete e documentação que você não tem dentro do SGI.

Funciona automaticamente em arquivos **`.lsp`** e **`.lspt`** — é só abrir. O plugin entende a estrutura do código (blocos `Se`, `Enquanto`, `Regra`, `Inicio`/`Fim`), o escopo das variáveis e as funções do seu projeto, e traz embutido o catálogo de **funções nativas** e **variáveis reservadas** da linguagem.

---

## ✨ Recursos

### ⚡ Escrita e autocomplete
- **Autocomplete inteligente** — palavras-chave, tipos, booleanos, funções do seu projeto e centenas de **funções nativas com a assinatura ao lado** (ex.: `Concatena(Alfa Str1, Alfa Str2, Alfa Str3, Alfa End Destino)`). Ao aceitar uma função, o **cursor entra entre os parênteses** para você informar os argumentos.
- **Variáveis com o tipo** — as variáveis do escopo aparecem já exibindo o tipo (`Alfa`, `Numero`, `Data`...).
- **Documentação inline** (`Ctrl+Q`) — assinatura completa + descrição oficial das funções nativas.
- **Live Templates** para padrões do dia a dia (cursor SQL, validação de permissão, auditoria, paginação, resposta JSON...).

### 🎨 Leitura e colorização
- **Colorização de sintaxe** por categoria: controle, tipos, declaração, funções, operadores, comentários e textos.
- **Variáveis reservadas em destaque** (`NomEmp`, `DatSis`, `Web_HTML`...), reconhecidas sem diferenciar maiúsculas de minúsculas.
- **Corretor ortográfico desativado** nesses arquivos — sem sublinhados em identificadores e texto em português.

### 🧭 Navegação
- **Ir para a declaração** (`Ctrl/Cmd+B` ou `Ctrl/Cmd+Click`):
  - **Funções** → implementação `Funcao nome()` em qualquer arquivo do projeto.
  - **Variáveis** → vai até o `Definir`, respeitando o **escopo do bloco**.
- **Localizar usos** (`Alt+Shift+F7`) e **realce de ocorrências** no arquivo.
- **Renomear** (`Shift+F6`) funções e variáveis com segurança de escopo.
- **Structure View** (outline navegável da hierarquia de blocos) e **breadcrumbs** — o caminho de blocos (`Regra` / `Se` / `Enquanto` / `Inicio`...) que contêm o cursor aparece no topo do editor. Essenciais em arquivos com centenas de linhas.

### 🧩 Edição
- **Bracket matching** e **auto-close** para `{ }`, `( )`, `[ ]` e `Inicio` / `Fim`.
- **Code folding** de blocos `{ }`, `Inicio` / `Fim` e banners de comentário.
- **Ícone próprio** para arquivos `.lsp` / `.lspt`.

---

## 📦 Requisitos

- IntelliJ IDEA **2025.3 ou superior** (Community ou Ultimate)
- O plugin **TextMate** (já vem incluso na IDE)

---

## ⚠️ Após instalar, reinicie a IDE

Parte dos recursos (como o bundle de colorização) **só carrega na inicialização**. Se instalou e o código não ficou colorido, faça um **restart completo** do IntelliJ — não basta o "load without restart".

---

## 💬 Feedback

Encontrou um problema ou quer sugerir uma função/palavra reservada que faltou? Abra uma issue no repositório do projeto. Contribuições são bem-vindas.
