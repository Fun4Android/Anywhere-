package com.absinthe.anywhere_.utils

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.absinthe.anywhere_.R
import com.absinthe.anywhere_.constants.AnywhereType
import com.absinthe.anywhere_.constants.Const
import com.absinthe.anywhere_.constants.GlobalValues
import com.absinthe.anywhere_.model.database.AnywhereEntity
import com.absinthe.anywhere_.ui.editor.impl.SWITCH_OFF
import com.absinthe.anywhere_.ui.shortcuts.ShortcutsActivity
import com.absinthe.libraries.utils.extensions.dp
import com.blankj.utilcode.util.Utils

object ShortcutsUtils {

  val SHORTCUT_MANAGER: ShortcutManager? = if (AppUtils.atLeastNMR1()) {
    Utils.getApp().getSystemService(ShortcutManager::class.java)
  } else {
    null
  }

  @RequiresApi(api = Build.VERSION_CODES.N_MR1)
  fun addShortcut(ae: AnywhereEntity) {
    val intent = Intent(Utils.getApp(), ShortcutsActivity::class.java).apply {
      when (ae.type) {
        AnywhereType.Card.IMAGE -> {
          action = ShortcutsActivity.ACTION_START_IMAGE
          putExtra(Const.INTENT_EXTRA_SHORTCUTS_CMD, ae.param1)
        }
        else -> {
          action = ShortcutsActivity.ACTION_START_ENTITY
          putExtra(Const.INTENT_EXTRA_SHORTCUTS_ID, ae.id)
        }
      }
    }

    val info = ShortcutInfo.Builder(Utils.getApp(), ae.id)
      .setShortLabel(ae.appName.ifEmpty { " " })
      .setIcon(Icon.createWithBitmap(UxUtils.getAppIcon(Utils.getApp(), ae, 45.dp).toBitmap()))
      .setIntent(intent)
      .build()
    if (SHORTCUT_MANAGER!!.dynamicShortcuts.size <= 3) {
      SHORTCUT_MANAGER.addDynamicShortcuts(listOf(info))
    }

    val list = GlobalValues.shortcutsList.toMutableList()
    list.add(ae.id)
    GlobalValues.shortcutsList = list
  }

  @RequiresApi(api = Build.VERSION_CODES.N_MR1)
  fun updateShortcut(ae: AnywhereEntity) {
    val intent = Intent(Utils.getApp(), ShortcutsActivity::class.java).apply {
      when (ae.type) {
        AnywhereType.Card.IMAGE -> {
          action = ShortcutsActivity.ACTION_START_IMAGE
          putExtra(Const.INTENT_EXTRA_SHORTCUTS_CMD, ae.param1)
        }
        else -> {
          action = ShortcutsActivity.ACTION_START_ENTITY
          putExtra(Const.INTENT_EXTRA_SHORTCUTS_ID, ae.id)
        }
      }
    }

    val icon = if (ae.type == AnywhereType.Card.SWITCH_SHELL) {
      if (ae.param3 == SWITCH_OFF) {
        ContextCompat.getDrawable(Utils.getApp(), R.drawable.ic_red_dot)!!.toBitmap()
      } else {
        ContextCompat.getDrawable(Utils.getApp(), R.drawable.ic_green_dot)!!.toBitmap()
      }
    } else {
      UxUtils.getAppIcon(Utils.getApp(), ae, 45.dp).toBitmap()
    }
    val info = ShortcutInfo.Builder(Utils.getApp(), ae.id)
      .setShortLabel(ae.appName.ifEmpty { " " })
      .setIcon(Icon.createWithBitmap(icon))
      .setIntent(intent)
      .build()
    SHORTCUT_MANAGER!!.updateShortcuts(listOf(info))
  }

  @RequiresApi(api = Build.VERSION_CODES.N_MR1)
  fun removeShortcut(ae: AnywhereEntity) {
    val list = GlobalValues.shortcutsList.toMutableList()
    list.remove(ae.id)
    GlobalValues.shortcutsList = list
    SHORTCUT_MANAGER!!.removeDynamicShortcuts(listOf(ae.id))
  }

  @RequiresApi(api = Build.VERSION_CODES.N_MR1)
  fun clearAllShortcuts() {
    SHORTCUT_MANAGER!!.removeAllDynamicShortcuts()
    GlobalValues.shortcutsList = listOf()
  }

  @RequiresApi(api = Build.VERSION_CODES.O)
  fun addPinnedShortcut(ae: AnywhereEntity, icon: Drawable, name: String) {
    if (SHORTCUT_MANAGER!!.isRequestPinShortcutSupported) {
      // Assumes there's already a shortcut with the ID "my-shortcut".
      // The shortcut must be enabled.
      val intent = Intent(Utils.getApp(), ShortcutsActivity::class.java).apply {
        action = ShortcutsActivity.ACTION_START_ENTITY
        putExtra(Const.INTENT_EXTRA_SHORTCUTS_ID, ae.id)
      }

      val finalName = name.ifEmpty { " " }
      val pinShortcutInfo = ShortcutInfo.Builder(Utils.getApp(), ae.id)
        .setShortLabel(finalName)
        .setIcon(Icon.createWithBitmap(icon.toBitmap()))
        .setIntent(intent)
        .build()

      // Create the PendingIntent object only if your app needs to be notified
      // that the user allowed the shortcut to be pinned. Note that, if the
      // pinning operation fails, your app isn't notified. We assume here that the
      // app has implemented a method called createShortcutResultIntent() that
      // returns a broadcast intent.
      val pinnedShortcutCallbackIntent =
        SHORTCUT_MANAGER.createShortcutResultIntent(pinShortcutInfo)

      // Configure the intent so that your app's broadcast receiver gets
      // the callback successfully.For details, see PendingIntent.getBroadcast().
      val successCallback = PendingIntent.getBroadcast(
        Utils.getApp(),  /* request code */0,
        pinnedShortcutCallbackIntent, FlagDelegate.PENDING_INTENT_FLAG_MUTABLE
      )
      SHORTCUT_MANAGER.requestPinShortcut(
        pinShortcutInfo,
        successCallback.intentSender
      )
      ToastUtil.makeText(R.string.toast_try_to_add_pinned_shortcut)
    }
  }

  fun addHomeShortcutPreO(ae: AnywhereEntity, icon: Drawable, name: String) {
    val shortcutIntent = Intent().apply {
      component = ComponentName(Utils.getApp(), ShortcutsActivity::class.java)
      flags = Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or Intent.FLAG_ACTIVITY_NEW_TASK
      action = ShortcutsActivity.ACTION_START_ENTITY
      putExtra(Const.INTENT_EXTRA_SHORTCUTS_ID, ae.id)
    }
    val resultIntent = Intent().apply {
      putExtra(Intent.EXTRA_SHORTCUT_ICON, icon.toBitmap())
      putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent)
      putExtra(Intent.EXTRA_SHORTCUT_NAME, name)
      action = "com.android.launcher.action.INSTALL_SHORTCUT"
    }
    Utils.getApp().sendBroadcast(resultIntent)
    ToastUtil.makeText(R.string.toast_try_to_add_pinned_shortcut)
  }
}
