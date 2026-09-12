package br.gov.interpretaai.platform

import android.app.Activity
import android.app.ActivityManager
import android.app.NotificationManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.UserManager
import android.provider.Settings
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.Toast
import br.gov.interpretaai.MainActivity

class KioskController(private val activity: Activity) {
    private val policy = activity.getSystemService(DevicePolicyManager::class.java)
    private val admin = ComponentName(activity, InterpretaDeviceAdminReceiver::class.java)
    private val notifications = activity.getSystemService(NotificationManager::class.java)
    private val activityManager = activity.getSystemService(ActivityManager::class.java)
    private var previousInterruptionFilter: Int? = null

    val isDeviceOwner: Boolean get() = policy.isDeviceOwnerApp(activity.packageName)
    val canControlDoNotDisturb: Boolean get() = notifications.isNotificationPolicyAccessGranted

    fun enterImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.window.insetsController?.apply {
                hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            activity.window.decorView.systemUiVisibility =
                android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
                    android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        }
    }

    fun provisionPoliciesIfDeviceOwner() {
        if (!isDeviceOwner) return
        policy.setLockTaskPackages(admin, arrayOf(activity.packageName))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            policy.setLockTaskFeatures(admin, DevicePolicyManager.LOCK_TASK_FEATURE_NONE)
        }
        policy.addUserRestriction(admin, UserManager.DISALLOW_CREATE_WINDOWS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            policy.addUserRestriction(admin, UserManager.DISALLOW_SYSTEM_ERROR_DIALOGS)
        }
        policy.addUserRestriction(admin, UserManager.DISALLOW_ADD_USER)
        val homeFilter = IntentFilter(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addCategory(Intent.CATEGORY_DEFAULT)
        }
        policy.addPersistentPreferredActivity(
            admin,
            homeFilter,
            ComponentName(activity, MainActivity::class.java)
        )
    }

    fun startFocusMode() {
        enterImmersiveMode()
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val wasUnlocked = activityManager.lockTaskModeState == ActivityManager.LOCK_TASK_MODE_NONE
        val fullKiosk = policy.isLockTaskPermitted(activity.packageName)
        if (wasUnlocked) {
            runCatching { activity.startLockTask() }
                .onFailure {
                    Toast.makeText(
                        activity,
                        "Não foi possível iniciar o foco. Abra novamente e tente outra vez.",
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }
        }
        if (canControlDoNotDisturb) {
            if (previousInterruptionFilter == null) {
                previousInterruptionFilter = notifications.currentInterruptionFilter
            }
            notifications.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
        }
        if (wasUnlocked) {
            Toast.makeText(
                activity,
                if (fullKiosk) "Foco total ativado. O tablet está preso ao InterpretaAI."
                else "Um adulto deve confirmar a fixação do InterpretaAI.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun stopFocusMode() {
        runCatching { activity.stopLockTask() }
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (canControlDoNotDisturb && previousInterruptionFilter != null) {
            notifications.setInterruptionFilter(previousInterruptionFilter!!)
        }
        previousInterruptionFilter = null
        enterImmersiveMode()
    }

    fun requestDoNotDisturbAccess() {
        activity.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
    }
}
