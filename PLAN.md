# Nim — Launcher minimalista para Android

> Plano do MVP para uso pessoal. Escrito em 27/08/2026.

**Stack decidida:** Kotlin + Jetpack Compose
**Escopo do MVP:** home minimalista com favoritos + gaveta A–Z com busca
**Ambiente:** Android Studio + emulador

---

## Decisão de design que carrega o resto

Launcher minimalista de verdade = **sem ícones**. Só texto.

Isso não é só estética: elimina carregamento e cache de ícones, adaptive icons,
monochrome themes e o pacote inteiro de dor de performance na gaveta. A lista de
apps vira `String` + `ComponentName`, e a UI fica trivial no Compose.

Se depois você quiser ícones, dá pra adicionar. Começar com eles custa caro.

---

## Stack

| Peça | Escolha | Por quê |
|---|---|---|
| UI | Compose (BOM mais recente) | Single Activity, zero XML de layout |
| Lista de apps | `LauncherApps` | Melhor que `PackageManager.queryIntentActivities` — dá work profile, callbacks de install/uninstall e `startMainActivity` com origem correta |
| Estado | ViewModel + `StateFlow` | Sem lib extra |
| Persistência | DataStore (Preferences) | Favoritos e settings são meia dúzia de chaves; Room é exagero |
| DI | Nenhuma | Manual. Hilt em app de 8 arquivos é peso morto |
| minSdk / targetSdk | 26 / 37 | API 37 (Android 17) é a instalada — ver "Estado atual do ambiente" |

---

## Os detalhes que fazem "ser um launcher"

Nim não é um app normal. Os dois primeiros itens são `AndroidManifest.xml` e
decidem se ele funciona; os outros dois decidem como ele se parece.

### 1. Intent filter de HOME

```xml
<activity
    android:name=".MainActivity"
    android:launchMode="singleTask"
    android:stateNotNeeded="true"
    android:excludeFromRecents="true"
    android:screenOrientation="portrait"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.HOME" />
        <category android:name="android.intent.category.DEFAULT" />
    </intent-filter>
</activity>
```

### 2. `<queries>` — a pegadinha nº 1

A partir da API 30 o Android esconde os apps instalados. Sem declarar isso,
sua lista vem **vazia e sem erro nenhum**:

```xml
<queries>
    <intent>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent>
</queries>
```

### 3. Fundo sólido — decisão revista no M1

O plano original era `windowShowWallpaper=true` com fundo transparente. **Foi
descartado:** o Nim tem fundo escuro sólido, sem papel de parede.

Isso não é só preferência — apaga uma classe inteira de problema. Com o
wallpaper atrás, a cor do texto não pode vir de `isSystemInDarkTheme()` (o que
está atrás do texto é a *imagem*, não o tema do sistema), e a alternativa
correta era ler `WallpaperManager.getWallpaperColors()`, registrar
`OnColorsChangedListener`, e ainda assim pôr sombra em toda tipografia porque
a dica de cor é global e um trecho de alto contraste engole o texto. Tudo isso
saiu do projeto. O contraste agora é conhecido em tempo de compilação.

```xml
<item name="android:windowShowWallpaper">false</item>
<item name="android:windowBackground">@color/nim_background</item>
```

`windowBackground` tem que ser a **mesma cor** do Compose, senão a abertura
pisca um flash de outra cor antes do primeiro frame.

### 4. A paleta: nem `#000` nem `#FFF`

| | Valor | Contraste medido |
|---|---|---|
| Fundo | `#121212` | — |
| Texto | `#E8E6E3` | 14.6:1 |
| Texto secundário | mesmo tom a 60% (`#92918F` sobre o fundo) | 5.7:1 |

Preto puro abre um buraco duro em OLED e branco puro vibra em cima dele.
`#121212` é o valor que o Material usa para superfície escura; o branco é
levemente quente. Aferido por amostragem de pixel na captura do emulador, não
no olho.

Só duas cores em todo o app. Sem ícones, hierarquia se faz com tamanho, peso e
opacidade — nunca com uma tinta nova.

### Mais dois comportamentos

- Botão **voltar** na home não faz nada — não pode dar `finish()`.
- `onNewIntent` = usuário apertou HOME já estando no launcher → fecha a gaveta
  e volta o scroll pro topo.

---

## Telas

```
┌─────────────────┐  swipe up  ┌─────────────────┐
│                 │ ─────────► │ [ busca______ ] │
│      14:32      │            │ Calendar        │
│   qui, 27 ago   │            │ Camera          │
│                 │            │ Chrome          │
│                 │            │ …               │
│  Mensagens      │ ◄───────── │ (A–Z, scroll)   │
│  Chrome         │  swipe down└─────────────────┘
│  Spotify        │
│  Câmera         │   long press em qualquer item →
└─────────────────┘   favoritar · info do app · desinstalar
```

**Busca:** normaliza (minúsculas + remove acentos) e ranqueia
**prefixo > iniciais > contém**.
`"wpp"` acha "WhatsApp". `"cam"` acha "Câmera" antes de "Scanner de Câmera".

---

## Estrutura de arquivos

```
app/src/main/java/com/gadfaria/nim/
├─ MainActivity.kt          # single activity, HOME intent
├─ data/
│  ├─ AppInfo.kt            # label, ComponentName, UserHandle
│  ├─ AppRepository.kt      # LauncherApps + Callback de install/uninstall
│  └─ Settings.kt           # DataStore: favoritos, prefs
├─ ui/
│  ├─ NimApp.kt             # host: home ⇄ drawer
│  ├─ HomeScreen.kt         # relógio + favoritos
│  ├─ DrawerScreen.kt       # busca + lista A–Z
│  └─ theme/                # tipografia e as duas cores
└─ NimViewModel.kt
```

---

## Milestones

| # | Entrega | Como sei que funcionou |
|---|---|---|
| **M0** ✅ | Android Studio + SDK + emulador (API 37) | `adb devices` lista o emulador |
| **M1** ✅ | Projeto Nim vira HOME app | Aperto HOME no emulador → escolho Nim → aparece minha tela |
| **M2** ✅ | Gaveta com apps reais, A–Z | Lista completa, toque abre o app |
| **M3** 🔶 | Busca | 2–3 letras filtram, Enter lança o primeiro resultado — feito; falta o ranqueamento |
| **M4** | Favoritos na home + relógio | Long press favorita, persiste após reiniciar |
| **M5** | Polimento: gesto de swipe, tema escuro, transições | Uso ele no dia a dia sem irritação |

M0 é o único passo lento (download do SDK + imagem do emulador, alguns GB).
M1–M4 são rápidos.

---

## Fora do MVP (de propósito)

Widgets · pastas · notificações e badges · ícones · gestos configuráveis ·
backup de settings · tela de configurações completa.

Tudo isso fica fácil depois que M1–M4 estiverem de pé — e nada disso é
necessário pra você já usar o Nim como launcher padrão.

---

## Estado atual do ambiente

Verificado em 27/08/2026 nesta máquina — **M0 concluído**:

- [x] Java / JDK — OpenJDK 25 embutido no Android Studio (`Contents/jbr`)
- [x] Android Studio 2026.1
- [x] Android SDK em `~/Library/Android/sdk`
      — platform-tools 37.0.1 · build-tools 36.0.0 · platform + sources android-37.0
      · cmdline-tools 23.0.0 · emulator 37.1.11
- [x] `adb`, `emulator` e `avdmanager` no PATH (`ANDROID_HOME` + `JAVA_HOME` no `.zshrc`)
- [x] Imagem de sistema `android-37.0;google_apis;arm64-v8a` (nativa, sem emulação de ABI)
- [x] AVD **`nim`** — perfil Pixel 10, 4 GB RAM, heap 512 MB, GPU host, teclado do host ligado

Critério do M0 atendido: `adb devices` lista `emulator-5554 device`, rodando
Android 17 / API 37.

Como subir o emulador de novo:

```sh
emulator -avd nim &
adb wait-for-device
```

---

## Próximo passo

**M3, o que falta** — ranquear os resultados: prefixo > iniciais > contém.
Hoje o filtro é só "contém", então `"g"` traz Messages e Settings junto com
Gmail. `"wpp"` ainda não acha "WhatsApp" — isso depende das iniciais.

---

## O que o M2 acrescentou

`LauncherApps` em vez de `PackageManager.queryIntentActivities` — a diferença
aparece em três pontos:

- **`profiles`** dá os perfis de trabalho. O mesmo `ComponentName` pode existir
  duas vezes, um por perfil, e lançar no perfil errado abre o app errado. Por
  isso o `UserHandle` entra na chave do `AppInfo`, não só o componente.
- **`Callback`** avisa de instalação, desinstalação e renomeação. Verificado no
  emulador: desabilitei o YouTube com a gaveta aberta e ele sumiu da lista sem
  nenhum toque. Um launcher que só lê a lista no `onCreate` mostra apps que já
  não existem.
- **`startMainActivity`** recebe `sourceBounds`, então a animação de abertura
  sai do item tocado em vez do centro da tela. Os bounds vêm do
  `onGloballyPositioned` da linha.

A ordenação usa `Collator` com força PRIMARY: um sort por código de caractere
jogaria "Câmera" depois de "Z".

O gesto vertical (sobe abre, desce fecha) é o mínimo pra gaveta ser usável
agora; a transição de verdade é do M5. Fechar também acontece pelo botão
voltar e por apertar HOME dentro do Nim — este último devolve o scroll ao topo.

## Busca e o abrir automático

O campo fica no topo da gaveta, recebe foco e abre o teclado junto com ela: a
gaveta existe para digitar, não para rolar 300 nomes.

**Um resultado só abre o app sozinho**, depois de 150 ms sem digitação. O
debounce é curto de propósito: só precisa cobrir o intervalo entre duas teclas
de quem digita rápido, não dar tempo de pensar. Zerar dispararia no meio da
palavra — `"gmail"` abriria já no `"gm"`.

Duas armadilhas que isso cria, ambas fechadas:

- **O laço.** Se a busca sobrevivesse ao lançamento, voltar do app reabriria a
  gaveta com um resultado só e dispararia de novo, prendendo o usuário. Por
  isso lançar *sempre* fecha a gaveta e limpa a busca — inclusive no toque
  normal e no Enter.
- **A rolagem reancorada.** A `LazyColumn` com `key` mantém no topo o item que
  lá estava quando o conteúdo muda. Filtrando até sobrar um app, ele vira a
  âncora; ao limpar a busca a lista tenta mantê-lo no topo e bate no fim da
  rolagem — a gaveta reabria no meio do alfabeto. O reset de rolagem tem que
  acontecer **na abertura e a cada mudança da busca**, nunca no fechamento: ali
  ele roda antes da reancoragem e é desfeito por ela.

A normalização (minúsculas, sem acento) é calculada uma vez por app no
carregamento, não a cada tecla — com centenas de apps, normalizar a lista
inteira por caractere digitado é a diferença entre busca instantânea e busca
engasgada.

## Notas de build

- **AGP 9 tem Kotlin embutido.** Aplicar `org.jetbrains.kotlin.android` agora é
  erro de configuração — o plugin foi removido do projeto. A versão do KGP vem
  junto com o AGP; só o plugin do Compose continua declarado.
- Toolchain fixada em `gradle/libs.versions.toml`: AGP 9.3.2 · Gradle 9.7.1 ·
  Compose BOM 2026.08.00 · `compileSdk`/`targetSdk` 37 · `minSdk` 26 · Java 17.
- Build e instalação pelo terminal:
  ```sh
  ./gradlew :app:assembleDebug
  adb install -r app/build/outputs/apk/debug/app-debug.apk
  adb shell cmd package set-home-activity com.gadfaria.nim/.MainActivity
  ```
- Para devolver o launcher do sistema no emulador:
  ```sh
  adb shell cmd package set-home-activity com.google.android.apps.nexuslauncher/.NexusLauncherActivity
  ```

---

## Pendências conhecidas (para o M5)

- A troca home ⇄ gaveta é um `Crossfade`, que dissolve as duas telas ao mesmo
  tempo — durante a transição dá pra ler a lista de apps por trás do relógio.
  Funciona, mas é feio para um launcher que se vende como limpo.
