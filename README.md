# LSP Senior

Plugin para IntelliJ IDEA que adiciona suporte à **Linguagem Senior de Programação (LSP)**, para arquivos `.lsp` e `.lspt`.

## Recursos

- 🎨 **Colorização de sintaxe** (via bundle TextMate embutido)
- 🔗 **Bracket matching** para `{ }`, `( )`, `[ ]` e `Inicio` / `Fim`
- ⌨️ **Auto-close** de pares, incluindo `Inicio` / `Fim`
- 📂 **Code folding** de blocos `{ }` e `Inicio` / `Fim`
- 🧩 **Snippets** para estruturas comuns (`se`, `enquanto`, `para`, `funcao`, etc.)
- 🖼️ **Ícone** próprio para arquivos `.lsp` / `.lspt`

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
│   ├── LspBundleProvider.kt   # registra o bundle TextMate
│   ├── LspIconProvider.kt     # ícone dos arquivos .lsp/.lspt
│   └── LspFoldingBuilder.kt   # code folding
└── resources/
    ├── META-INF/plugin.xml
    ├── icons/lsp_file.svg
    └── textmate-bundle/        # grammar, language-configuration, snippets
```

## Stack

Kotlin · Gradle · `org.jetbrains.intellij.platform` 2.17.0 (requer Gradle 9.0+)
