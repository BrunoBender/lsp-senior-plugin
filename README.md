# LSP Senior

Plugin para IntelliJ IDEA que adiciona suporte à **Linguagem Senior de Programação (LSP)**, para arquivos `.lsp` e `.lspt`.

## Recursos

- 🎨 **Colorização de sintaxe** (via bundle TextMate embutido), com categorias
  distintas para controle (`Se`, `Senao`, `Enquanto`...), tipos (`Alfa`, `Numero`,
  `Data`...), declaração (`Definir`), funções e operadores.
- 🔗 **Bracket matching** para `{ }`, `( )`, `[ ]` e `Inicio` / `Fim`
- ⌨️ **Auto-close** de pares, incluindo `Inicio` / `Fim`
- 📂 **Code folding** de blocos `{ }` e `Inicio` / `Fim`
- 🍞 **Breadcrumbs** no topo do editor com o caminho de blocos (`Regra` / `Se` /
  `Senao` / `Enquanto` / `Para` / `Inicio`) até o cursor
- 🧭 **Ir para a declaração** (Cmd/Ctrl+B, Cmd/Ctrl+Click):
  - **Funções** → implementação `Funcao nome()` em qualquer arquivo do projeto.
  - **Variáveis** → declaração `Definir [Tipo] nome`, local ao arquivo e
    respeitando o escopo de bloco (a mesma variável em blocos diferentes não se confunde).
- 🔎 **Localizar usos** — ação "LSP: Localizar Usos" (menu de contexto ou ⌥⇧F7 / Alt+Shift+F7):
  - **Funções** → usos em todo o projeto.
  - **Variáveis** → usos no arquivo, dentro do escopo do bloco.
- 🧩 **Snippets** para estruturas comuns (`se`, `enquanto`, `para`, `funcao`, etc.)
- 🖼️ **Ícone** próprio para arquivos `.lsp` / `.lspt`
- 🔤 **Corretor ortográfico desativado** em `.lsp` / `.lspt` (evita falsos positivos
  em identificadores e texto em português)

## Requisitos

- IntelliJ IDEA 2025.1 ou superior
- O plugin TextMate (já incluso na IDE) é uma dependência

## Build

```bash
./gradlew clean buildPlugin
```

O `.zip` instalável é gerado em `build/distributions/`.

### Build assinado (opcional)

```bash
export PRIVATE_KEY_PASSWORD='sua-senha'
./gradlew clean signPlugin
```

## Instalação manual

1. **Settings → Plugins → ⚙️ → Install Plugin from Disk…**
2. Selecione o `.zip` em `build/distributions/`
3. **Reinicie a IDE** (obrigatório — o bundle TextMate só carrega no startup)

## Estrutura

```
src/main/
├── kotlin/dev/lspsenior/
│   ├── LspBundleProvider.kt          # registra o bundle TextMate
│   ├── LspIconProvider.kt            # ícone dos arquivos .lsp/.lspt
│   ├── LspFoldingBuilder.kt          # code folding
│   ├── LspBreadcrumbsProvider.kt     # breadcrumbs (caminho de blocos)
│   ├── LspSpellcheckingStrategy.kt   # desativa o corretor em .lsp/.lspt
│   ├── LspFunctionIndex.kt           # índice das implementações de funções
│   ├── LspSymbols.kt                 # resolução de símbolos (escopo, varredura)
│   ├── LspNavTarget.kt               # alvo de navegação (offset exato)
│   ├── LspReferenceContributor.kt    # referências: ir para a declaração
│   └── LspFindUsagesAction.kt        # ação "LSP: Localizar Usos"
└── resources/
    ├── META-INF/plugin.xml
    ├── icons/lsp_file.svg
    └── textmate-bundle/              # grammar, language-configuration, snippets
```

## Atalhos

| Ação | Atalho |
|------|--------|
| Ir para a declaração / implementação | Cmd/Ctrl+B, Cmd/Ctrl+Click |
| Localizar usos | ⌥⇧F7 (Alt+Shift+F7) ou menu de contexto |

## Stack

Kotlin · Gradle · `org.jetbrains.intellij.platform` 2.17.0 (requer Gradle 9.0+)
