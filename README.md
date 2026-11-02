# Nim

Launcher minimalista para Android, só texto. Sem ícones, sem widgets: um
relógio, seus favoritos e uma gaveta A–Z com busca.

Feito em Kotlin + Jetpack Compose, para uso pessoal.

## O que faz

- **Home** com relógio, data e favoritos embaixo, onde o polegar alcança.
- **Gaveta A–Z** (arrastar para cima) com busca que já abre o teclado.
  Ranqueia prefixo do nome > prefixo de palavra > iniciais > contém, e ignora
  acentos: `cam` acha Câmera, `gm` acha Google Maps.
- **Abre sozinho** quando a busca reduz a lista a um único app.
- **Toque longo** num app: favoritar, info do app ou desinstalar.
- **Perfil de trabalho**: apps do outro perfil levam uma marca de pasta.
- **Atalhos fixados**: aceita "Adicionar à tela inicial" do Chrome (PWAs) e
  mostra os atalhos na gaveta.
- **Configurações** (toque longo no vazio da home): reordenar favoritos por
  arrasto e usar o fundo do Nim como papel de parede.

Fundo `#121212`, texto `#E8E6E3`. Só duas cores no app inteiro.

## Requisitos

- Android 8.0+ (minSdk 26, targetSdk 37)
- JDK 17 e Android SDK 37 (Android Studio já traz os dois)

## Build e instalação

```sh
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Depois aperte HOME no aparelho e escolha o Nim, ou defina pelo adb:

```sh
adb shell cmd package set-home-activity com.gadfaria.nim/.MainActivity
```

## Testes

```sh
./gradlew :app:testDebugUnitTest
```

Cobrem as camadas da busca (`data/AppSearch.kt`, Kotlin puro).

## Estrutura

```
app/src/main/java/com/gadfaria/nim/
├─ MainActivity.kt         # activity única, intent de HOME
├─ PinShortcutActivity.kt  # recebe pedidos de atalho fixado
├─ NimViewModel.kt
├─ data/                   # LauncherApps, busca, DataStore, papel de parede
└─ ui/                     # home, gaveta, menu, configurações, tema
```

As decisões de design e as armadilhas encontradas no caminho estão em
[PLAN.md](PLAN.md).
