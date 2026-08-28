package com.gadfaria.nim.data

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.Rect
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.os.UserManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import java.text.Collator

/**
 * A lista de apps do aparelho, vinda de [LauncherApps].
 *
 * LauncherApps em vez de PackageManager.queryIntentActivities: dá os perfis de
 * trabalho, avisa de instalação/desinstalação por callback e lança pelo
 * startMainActivity, que registra a origem certa (o launcher) na animação.
 *
 * Nada disso funciona sem o bloco <queries> no manifesto — sem ele a lista
 * volta vazia e sem erro nenhum.
 */
class AppRepository(context: Context) {

    private val appContext = context.applicationContext

    private val launcherApps =
        context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

    private val userManager =
        context.getSystemService(Context.USER_SERVICE) as UserManager

    // Ordenação alfabética de verdade: Collator põe "Câmera" junto de "Camera"
    // e não depois de "Z", que é o que um sort por código de caractere faria.
    private val collator = Collator.getInstance().apply { strength = Collator.PRIMARY }

    private val byLabel = Comparator<AppInfo> { a, b ->
        collator.compare(a.label, b.label).takeIf { it != 0 }
            ?: a.componentName.compareTo(b.componentName)
    }

    /**
     * Emite a lista atual e reemite a cada instalação, desinstalação ou
     * renomeação. Um launcher que só lê no onCreate mostra apps fantasmas.
     */
    val apps: Flow<List<AppInfo>> = callbackFlow {
        fun push() = trySend(load())

        val callback = object : LauncherApps.Callback() {
            override fun onPackageAdded(packageName: String?, user: UserHandle?) = Unit.also { push() }
            override fun onPackageRemoved(packageName: String?, user: UserHandle?) = Unit.also { push() }
            override fun onPackageChanged(packageName: String?, user: UserHandle?) = Unit.also { push() }
            override fun onPackagesAvailable(
                packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean,
            ) = Unit.also { push() }
            override fun onPackagesUnavailable(
                packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean,
            ) = Unit.also { push() }
        }

        // Handler explícito: o callback precisa de um Looper e a corrotina pode
        // estar num dispatcher que não tem nenhum.
        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
        push()
        awaitClose { launcherApps.unregisterCallback(callback) }
    }.conflate()

    private fun load(): List<AppInfo> =
        launcherApps.profiles
            .flatMap { user ->
                val serial = userManager.getSerialNumberForUser(user)
                // Pode falhar se o perfil sumir ou entrar em modo silencioso
                // entre o callback e a leitura.
                runCatching { launcherApps.getActivityList(null, user) }
                    .getOrDefault(emptyList())
                    .map { activity ->
                        AppInfo(
                            label = activity.label.toString(),
                            componentName = activity.componentName,
                            user = user,
                            userSerial = serial,
                        )
                    }
            }
            .sortedWith(byLabel)

    /**
     * [sourceBounds] é de onde a animação de abertura sai. Passar null faz o app
     * crescer do centro da tela em vez de do item que foi tocado.
     */
    fun launch(app: AppInfo, sourceBounds: Rect? = null) {
        runCatching {
            launcherApps.startMainActivity(app.componentName, app.user, sourceBounds, null)
        }
    }

    /** Abre a tela de detalhes do sistema. Via LauncherApps para acertar o perfil. */
    fun openAppInfo(app: AppInfo, sourceBounds: Rect? = null) {
        runCatching {
            launcherApps.startAppDetailsActivity(app.componentName, app.user, sourceBounds, null)
        }
    }

    /**
     * Pede a desinstalação. Quem confirma — e quem recusa, no caso de app de
     * sistema — é o diálogo do Android; o Nim não apaga nada por conta própria.
     */
    fun requestUninstall(app: AppInfo) {
        runCatching {
            val intent = Intent(
                Intent.ACTION_DELETE,
                Uri.parse("package:${'$'}{app.componentName.packageName}"),
            )
                .putExtra(Intent.EXTRA_USER, app.user)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            appContext.startActivity(intent)
        }
    }
}
