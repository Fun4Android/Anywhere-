package com.absinthe.anywhere_.utils.manager

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.core.content.ContextCompat.checkSelfPermission
import com.absinthe.anywhere_.R
import com.absinthe.anywhere_.constants.Const
import com.absinthe.anywhere_.utils.ToastUtil
import com.absinthe.anywhere_.utils.handler.URLSchemeHandler
import com.absinthe.anywhere_.utils.manager.DialogManager.showGotoShizukuManagerDialog
import com.absinthe.libraries.utils.utils.XiaomiUtilities
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuProvider
import timber.log.Timber

/**
 * Shizuku Helper
 *
 * Init Shizuku API.
 */
object ShizukuHelper {

  fun checkPermission(activity: Activity): Boolean {
    val code = Const.REQUEST_CODE_SHIZUKU_PERMISSION
    try {
      return if (!Shizuku.isPreV11() && Shizuku.getVersion() >= 11) {
        // Sui and Shizuku >= 11 use self-implemented permission
        when {
          Shizuku.checkSelfPermission() == PERMISSION_GRANTED -> {
            true
          }
          Shizuku.shouldShowRequestPermissionRationale() -> {
            ToastUtil.makeText("User denied permission")
            false
          }
          else -> {
            Shizuku.requestPermission(code)
            false
          }
        }
      } else {
        // Shizuku < 11 uses runtime permission
        when {
          checkSelfPermission(activity, ShizukuProvider.PERMISSION) == PERMISSION_GRANTED -> {
            true
          }
          shouldShowRequestPermissionRationale(activity, ShizukuProvider.PERMISSION) -> {
            ToastUtil.makeText("User denied permission")
            false
          }
          else -> {
            if (XiaomiUtilities.isMIUI()) {
              showPermissionDialog(activity)
            } else {
              requestPermissions(activity, arrayOf(ShizukuProvider.PERMISSION), code)
            }
            false
          }
        }
      }
    } catch (e: Throwable) {
      Timber.e(e)
    }
    return false
  }

  private fun showPermissionDialog(activity: Activity) {
    showGotoShizukuManagerDialog(activity) {
      val shizukuPackageName = "moe.shizuku.privileged.api"
      val activityIntent = Intent().addCategory(Intent.CATEGORY_LAUNCHER).setPackage(shizukuPackageName)
      val launcherActivity = activity.packageManager.queryIntentActivities(activityIntent, 0)[0].activityInfo.name
      val intent = if (launcherActivity != null) Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setClassName(shizukuPackageName, launcherActivity).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) else null
      if (intent != null) {
        activity.startActivityForResult(intent, Const.REQUEST_CODE_SHIZUKU_PERMISSION)
      } else {
        ToastUtil.makeText(R.string.toast_not_install_shizuku)
        URLSchemeHandler.parse(activity, URLManager.SHIZUKU_MARKET_URL)
      }
    }
  }
}
