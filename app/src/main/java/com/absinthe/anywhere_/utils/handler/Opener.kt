package com.absinthe.anywhere_.utils.handler

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.FileUriExposedException
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import cn.vove7.andro_accessibility_api.AppScope
import cn.vove7.andro_accessibility_api.api.*
import cn.vove7.andro_accessibility_api.utils.NeedAccessibilityException
import com.absinthe.anywhere_.AnywhereApplication
import com.absinthe.anywhere_.BaseActivity
import com.absinthe.anywhere_.R
import com.absinthe.anywhere_.a11y.A11yEntity
import com.absinthe.anywhere_.a11y.A11yType
import com.absinthe.anywhere_.constants.AnywhereType
import com.absinthe.anywhere_.constants.Const
import com.absinthe.anywhere_.constants.GlobalValues
import com.absinthe.anywhere_.constants.OnceTag
import com.absinthe.anywhere_.listener.OnAppDefrostListener
import com.absinthe.anywhere_.model.*
import com.absinthe.anywhere_.model.database.AnywhereEntity
import com.absinthe.anywhere_.model.database.isBrightWhenShowImage
import com.absinthe.anywhere_.model.database.isExecWithRoot
import com.absinthe.anywhere_.model.manager.QRCollection
import com.absinthe.anywhere_.services.WorkflowIntentService
import com.absinthe.anywhere_.ui.dialog.DynamicParamsDialogFragment.OnParamsInputListener
import com.absinthe.anywhere_.ui.editor.EXTRA_ENTITY
import com.absinthe.anywhere_.ui.editor.impl.SWITCH_OFF
import com.absinthe.anywhere_.ui.editor.impl.SWITCH_ON
import com.absinthe.anywhere_.ui.qrcode.QRCodeCollectionActivity
import com.absinthe.anywhere_.utils.AppTextUtils.getItemCommand
import com.absinthe.anywhere_.utils.AppTextUtils.getPkgNameByCommand
import com.absinthe.anywhere_.utils.AppUtils
import com.absinthe.anywhere_.utils.AppUtils.isActivityExported
import com.absinthe.anywhere_.utils.CommandUtils
import com.absinthe.anywhere_.utils.ShortcutsUtils
import com.absinthe.anywhere_.utils.ToastUtil
import com.absinthe.anywhere_.utils.manager.ActivityStackManager
import com.absinthe.anywhere_.utils.manager.DialogManager
import com.absinthe.anywhere_.view.app.AnywhereDialogFragment
import com.blankj.utilcode.util.IntentUtils
import com.catchingnow.icebox.sdk_client.IceBox
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import jonathanfinerty.once.Once
import kotlinx.coroutines.*
import timber.log.Timber
import java.lang.ref.WeakReference

private const val TYPE_NONE = -1
private const val TYPE_ENTITY = 0
private const val TYPE_CMD = 1

object Opener {

  private var context: WeakReference<Context>? = null
  private var listener: OnOpenListener? = null
  private var item: AnywhereEntity? = null
  private var command: String? = null
  private var type: Int = TYPE_NONE
  private var extraItem: ExtraBean.ExtraItem? = null
  private var extraItems: Array<ExtraBean.ExtraItem>? = null

  fun with(context: Context): Opener {
    this.context = WeakReference(context)
    type = TYPE_NONE
    item = null
    command = null
    listener = null
    extraItem = null
    extraItems = null
    return this
  }

  fun load(item: AnywhereEntity): Opener {
    type = TYPE_ENTITY
    this.item = item
    return this
  }

  fun load(cmd: String): Opener {
    type = TYPE_CMD
    this.command = cmd
    return this
  }

  fun setDynamicExtra(item: ExtraBean.ExtraItem?): Opener {
    extraItem = item
    return this
  }

  fun setDynamicExtras(items: Array<ExtraBean.ExtraItem>?): Opener {
    extraItems = items
    return this
  }

  fun setOpenedListener(listener: OnOpenListener): Opener {
    this.listener = listener
    return this
  }

  @Throws(NullPointerException::class)
  fun open() {
    context?.get()?.let {
      when (type) {
        TYPE_CMD -> {
          openFromCommand(it)
        }
        TYPE_ENTITY -> {
          if (item?.isExecWithRoot() == true) {
            command = getItemCommand(item!!)
            openFromCommand(it)
          } else {
            openFromEntity(it)
          }
        }
      }
    } ?: let {
      throw NullPointerException("Got a null context instance from Opener.")
    }
  }

  @Throws(NullPointerException::class)
  fun openWithPackageName(packageName: String) {
    context?.get()?.let {
      openByCommand(it, command ?: throw NullPointerException("null package name."), packageName)
    } ?: let {
      throw NullPointerException("Got a null context instance from Opener.")
    }
  }

  private fun openFromEntity(context: Context) {
    Timber.d("openFromEntity")
    item?.let {
      openAnywhereEntity(context, it)
    }
  }

  private fun openFromCommand(context: Context) {
    Timber.d("openFromCommand")
    command?.let {
      when {
        it.startsWith(AnywhereType.Prefix.DYNAMIC_PARAMS_PREFIX) -> {
          openDynamicParamCommand(context, it)
        }
        it.startsWith(AnywhereType.Prefix.SHELL_PREFIX) -> {
          openShellCommand(context, it)
        }
        else -> {
          openByCommand(context, it, getPkgNameByCommand(it))
        }
      }
    }
  }

  private fun openAnywhereEntity(context: Context, item: AnywhereEntity) {
    when (item.type) {
      AnywhereType.Card.URL_SCHEME -> openUrlSchemeEntity(context, item)
      AnywhereType.Card.ACTIVITY -> openActivityEntity(context, item)
      AnywhereType.Card.QR_CODE -> openQrCodeEntity(context, item)
      AnywhereType.Card.IMAGE -> openImageEntity(context, item)
      AnywhereType.Card.SHELL -> openShellEntity(context, item)
      AnywhereType.Card.SWITCH_SHELL -> openSwitchShellEntity(context, item)
      AnywhereType.Card.FILE -> openFileEntity(context, item)
      AnywhereType.Card.BROADCAST -> openBroadcastEntity(context, item)
      AnywhereType.Card.WORKFLOW -> openWorkflowEntity(context, item)
      AnywhereType.Card.ACCESSIBILITY -> openA11yEntity(context, item)
    }
  }

  private fun openByCommand(context: Context, cmd: String, packageName: String?) {
    if (cmd.isEmpty()) {
      return
    }

    if (packageName.isNullOrEmpty()) {
      CommandUtils.execCmd(cmd)
    } else {
      try {
        if (IceBox.getAppEnabledSetting(context, packageName) != 0) {
          val result = DefrostHandler.defrost(context, packageName, object : OnAppDefrostListener {
            override fun onAppDefrost() {
              CommandUtils.execCmd(cmd)
            }
          })
          if (!result) {
            ToastUtil.makeText(context, R.string.toast_not_choose_defrost_mode)
          }
        } else {
          CommandUtils.execCmd(cmd)
        }
      } catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
        CommandUtils.execCmd(cmd)
      } catch (e: IndexOutOfBoundsException) {
        e.printStackTrace()
        ToastUtil.makeText(R.string.toast_wrong_cmd)
      }
    }
    listener?.onOpened()
  }

  private fun openDynamicParamCommand(context: Context, command: String) {
    var newCommand = command.removePrefix(AnywhereType.Prefix.DYNAMIC_PARAMS_PREFIX)
    val splitIndex = newCommand.indexOf(']')
    val param = newCommand.substring(0, splitIndex)
    newCommand = newCommand.substring(splitIndex + 1)

    DialogManager.showDynamicParamsDialog(
      (context as BaseActivity<*>),
      param,
      object : OnParamsInputListener {
        override fun onFinish(text: String?) {
          openByCommand(context, newCommand + text, getPkgNameByCommand(newCommand))
          listener?.onOpened()
        }

        override fun onCancel() {
          listener?.onOpened()
        }
      })
  }

  private fun openShellCommand(context: Context, command: String) {
    val newCommand = command.removePrefix(AnywhereType.Prefix.SHELL_PREFIX)
    val result = CommandUtils.execAdbCmd(newCommand)

    when (GlobalValues.showShellResultMode) {
      Const.SHELL_RESULT_TOAST -> {
        listener?.onOpened()
      }
      Const.SHELL_RESULT_DIALOG -> {
        DialogManager.showShellResultDialog(
          context,
          result,
          { _, _ -> listener?.onOpened() },
          { listener?.onOpened() })
      }
      else -> {
        listener?.onOpened()
      }
    }
  }

  private fun openUrlSchemeEntity(context: Context, item: AnywhereEntity) {
    if (!item.param3.isNullOrEmpty()) {
      val ctx = if (context is AppCompatActivity) {
        context
      } else {
        ActivityStackManager.topActivity ?: return
      }
      DialogManager.showDynamicParamsDialog(
        ctx,
        item.param3.orEmpty(),
        object : OnParamsInputListener {
          override fun onFinish(text: String?) {
            try {
              URLSchemeHandler.parse(context, item.param1 + text, item.param2) {
                listener?.onOpened()
              }
            } catch (e: Exception) {
              Timber.e(e)
              if (e is ActivityNotFoundException) {
                ToastUtil.makeText(R.string.toast_no_react_url)
              } else if (AppUtils.atLeastN()) {
                if (e is FileUriExposedException) {
                  ToastUtil.makeText(R.string.toast_file_uri_exposed)
                }
              }
              listener?.onOpened()
            }
          }

          override fun onCancel() {
            listener?.onOpened()
          }
        })
    } else {
      try {
        URLSchemeHandler.parse(context, item.param1, item.param2) {
          listener?.onOpened()
        }
      } catch (e: Exception) {
        Timber.e(e)
        if (e is ActivityNotFoundException) {
          ToastUtil.makeText(R.string.toast_no_react_url)
        } else if (AppUtils.atLeastN()) {
          if (e is FileUriExposedException) {
            ToastUtil.makeText(R.string.toast_file_uri_exposed)
          }
        }
        listener?.onOpened()
      }
    }
  }

  private fun openActivityEntity(context: Context, item: AnywhereEntity) {
    val className = if (item.param2.orEmpty().startsWith(".")) {
      item.param1 + item.param2
    } else {
      item.param2
    }

    if (item.param2.isNullOrBlank() || isActivityExported(
        context,
        ComponentName(item.param1, className.orEmpty())
      )
    ) {
      val extraBean: ExtraBean? = try {
        Gson().fromJson(item.param3, ExtraBean::class.java)
      } catch (e: JsonSyntaxException) {
        null
      }
      val action = if (extraBean == null || extraBean.action.isEmpty()) {
        null
      } else {
        extraBean.action
      }

      val intent = if (item.param1.isNotBlank()) {
        if (item.param2.isNullOrBlank()) {
          IntentUtils.getLaunchAppIntent(item.param1)?.apply {
            if (context !is Activity) {
              addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
          } ?: let {
            Intent().apply {
              if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
              }
            }
          }
        } else {
          Intent(action).apply {
            component = ComponentName(item.param1, className.orEmpty())
            if (context !is Activity) {
              addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
          }
        }
      } else {
        Intent(action).apply {
          if (context !is Activity) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          }
        }
      }

      extraBean?.let {
        if (it.data.isNotEmpty()) {
          intent.data = it.data.toUri()
        }

        val extras = it.extras.toMutableList()
        extraItem?.let { extra ->
          extras.add(extra)
        }

        extraItems?.let { items ->
          for (extra in items) {
            extras.add(extra)
          }
        }

        for (extra in extras) {
          when (extra.type) {
            TYPE_STRING, TYPE_STRING_LABEL -> intent.putExtra(extra.key, extra.value)
            TYPE_BOOLEAN, TYPE_BOOLEAN_LABEL -> intent.putExtra(
              extra.key,
              extra.value.toBoolean()
            )
            TYPE_URI, TYPE_URI_LABEL -> intent.putExtra(extra.key, extra.value.toUri())
            TYPE_INT, TYPE_INT_LABEL -> {
              try {
                extra.value.toInt()
              } catch (ignore: NumberFormatException) {
                null
              }?.let { value ->
                intent.putExtra(extra.key, value)
              }
            }
            TYPE_LONG, TYPE_LONG_LABEL -> {
              try {
                extra.value.toLong()
              } catch (ignore: NumberFormatException) {
                null
              }?.let { value ->
                intent.putExtra(extra.key, value)
              }
            }
            TYPE_FLOAT, TYPE_FLOAT_LABEL -> {
              try {
                extra.value.toFloat()
              } catch (ignore: NumberFormatException) {
                null
              }?.let { value ->
                intent.putExtra(extra.key, value)
              }
            }
            TYPE_DOUBLE, TYPE_DOUBLE_LABEL -> {
              try {
                extra.value.toDouble()
              } catch (ignore: NumberFormatException) {
                null
              }?.let { value ->
                intent.putExtra(extra.key, value)
              }
            }
          }
        }
      }

      try {
        context.startActivity(intent)
      } catch (e: Exception) {
        ToastUtil.makeText(e.toString())
      }
      listener?.onOpened()
    } else {
      openByCommand(context, getItemCommand(item), item.packageName)
    }
  }

  private fun openQrCodeEntity(context: Context, item: AnywhereEntity) {
    val qrId = if (context is QRCodeCollectionActivity) {
      item.id
    } else {
      item.param2.orEmpty()
    }
    QRCollection.getQREntity(qrId)?.launch()
    listener?.onOpened()
  }

  private fun openImageEntity(context: Context, item: AnywhereEntity) {
    val ctx = if (context is AppCompatActivity) {
      context
    } else {
      ActivityStackManager.topActivity ?: return
    }
    Timber.d("ctx: $ctx")
    DialogManager.showImageDialog(
      ctx,
      item.param1,
      object : AnywhereDialogFragment.OnDismissListener {
        override fun onDismiss() {
          listener?.onOpened()
          if (item.isBrightWhenShowImage()) {
            ctx.window.attributes = ctx.window.attributes.also {
              it.screenBrightness = -1.0f
            }
          }
        }
      })
    if (item.isBrightWhenShowImage()) {
      ctx.window.attributes = ctx.window.attributes.also {
        it.screenBrightness = 1.0f
      }
    }
  }

  private fun openShellEntity(context: Context, item: AnywhereEntity) {
    val result = CommandUtils.execAdbCmd(item.param1)
    DialogManager.showShellResultDialog(
      context,
      result,
      { _, _ -> listener?.onOpened() },
      { listener?.onOpened() })
  }

  private fun openSwitchShellEntity(context: Context, item: AnywhereEntity) {
    openByCommand(context, getItemCommand(item), item.packageName)
    val ae = item.copy().apply {
      param3 = if (param3 == SWITCH_OFF) SWITCH_ON else SWITCH_OFF
    }
    AnywhereApplication.sRepository.update(ae)

    if (AppUtils.atLeastNMR1()) {
      ShortcutsUtils.updateShortcut(ae)
    }
  }

  private fun openFileEntity(context: Context, item: AnywhereEntity) {
    val intent = Intent().apply {
      action = Intent.ACTION_VIEW
      data = item.param1.toUri()
      if (context !is Activity) {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    }

    try {
      context.startActivity(intent)
    } catch (e: Exception) {
      e.printStackTrace()
      ToastUtil.makeText(R.string.toast_no_react_url)
    }
    listener?.onOpened()
  }

  private fun openBroadcastEntity(context: Context, item: AnywhereEntity) {
    val extraBean: ExtraBean? = try {
      Gson().fromJson(item.param1, ExtraBean::class.java)
    } catch (e: JsonSyntaxException) {
      null
    }
    extraBean?.let {
      val action = it.action.ifEmpty { Const.DEFAULT_BR_ACTION }
      val intent = Intent(action).apply {
        if (context !is Activity) {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
      }
      if (extraBean.data.isNotEmpty()) {
        intent.data = extraBean.data.toUri()
      }

      val extras = extraBean.extras.toMutableList()
      extraItem?.let { extra ->
        extras.add(extra)
      }

      extraItems?.let { items ->
        for (extra in items) {
          extras.add(extra)
        }
      }

      for (extra in extras) {
        try {
          when (extra.type) {
            TYPE_STRING, TYPE_STRING_LABEL -> intent.putExtra(extra.key, extra.value)
            TYPE_BOOLEAN, TYPE_BOOLEAN_LABEL -> intent.putExtra(
              extra.key,
              extra.value.toBoolean()
            )
            TYPE_INT, TYPE_INT_LABEL -> intent.putExtra(extra.key, extra.value.toInt())
            TYPE_LONG, TYPE_LONG_LABEL -> intent.putExtra(extra.key, extra.value.toLong())
            TYPE_FLOAT, TYPE_FLOAT_LABEL -> intent.putExtra(extra.key, extra.value.toFloat())
            TYPE_DOUBLE, TYPE_DOUBLE_LABEL -> intent.putExtra(extra.key, extra.value.toDouble())
            TYPE_URI, TYPE_URI_LABEL -> intent.putExtra(extra.key, extra.value.toUri())
          }
        } catch (e: NumberFormatException) {
          Timber.e(e)
        }
      }

      val intentPackage = item.param2
      if (!intentPackage.isNullOrBlank()) {
        intent.setPackage(intentPackage)

        if (!item.param3.isNullOrBlank()) {
          intent.setClassName(intentPackage, item.param3!!)
        }
      }

      runCatching {
        context.sendBroadcast(intent)
      }.onFailure { t ->
        Timber.e(t)
        ToastUtil.makeText(t.toString())
      }
    } ?: let {
      ToastUtil.makeText(R.string.toast_json_error)
    }
    listener?.onOpened()
  }

  private fun openWorkflowEntity(context: Context, item: AnywhereEntity) {
    if (!Once.beenDone(Once.THIS_APP_INSTALL, OnceTag.A11Y_ANNOUNCEMENT)) {
      if (context is Activity) {
        DialogManager.showA11yAnnouncementDialog(context)
      }
      return
    }
    WorkflowIntentService.enqueueWork(context, Intent().apply {
      putExtra(EXTRA_ENTITY, item)
    })
    listener?.onOpened()
  }

  @OptIn(DelicateCoroutinesApi::class)
  private fun openA11yEntity(context: Context, item: AnywhereEntity) {
    if (!Once.beenDone(Once.THIS_APP_INSTALL, OnceTag.A11Y_ANNOUNCEMENT)) {
      if (context is Activity) {
        DialogManager.showA11yAnnouncementDialog(context)
      }
      return
    }
    try {
      requireBaseAccessibility()
      requireGestureAccessibility()

      val a11yEntity = Gson().fromJson(item.param1, A11yEntity::class.java)

      fun action() {
        GlobalScope.launch {
          if (a11yEntity.entryActivity.isBlank()) {
            context.packageManager.getLaunchIntentForPackage(a11yEntity.applicationId)
              ?.let { intent ->
                openActivityEntity(context, AnywhereEntity().also {
                  it.type = AnywhereType.Card.ACTIVITY
                  it.param1 = intent.component?.packageName.orEmpty()
                  it.param2 = intent.component?.className.orEmpty()
                })
              }
          } else {
            openActivityEntity(context, AnywhereEntity().also {
              it.type = AnywhereType.Card.ACTIVITY
              it.param1 = a11yEntity.applicationId
              it.param2 = a11yEntity.entryActivity
            })
          }

          var result = waitForPage(AppScope(a11yEntity.applicationId, a11yEntity.entryActivity))
          var currentActivity = a11yEntity.entryActivity
          if (!result) {
            withContext(Dispatchers.Main) {
              ToastUtil.Toasty.show(context, "Timeout for waiting entry page")
            }
            return@launch
          }
          for (action in a11yEntity.actions) {
            if (action.activityId.isNotEmpty()) {
              currentActivity = action.activityId
              result = waitForPage(AppScope(a11yEntity.applicationId, action.activityId))
            } else {
              result = waitForPage(AppScope(a11yEntity.applicationId, currentActivity))
            }
            if (!result) {
              withContext(Dispatchers.Main) {
                ToastUtil.Toasty.show(context, "Timeout for waiting page")
              }
              return@launch
            }

            delay(action.delay)

            when (action.type) {
              A11yType.TEXT -> {
                if (action.contains) {
                  containsText(*(action.content.split("|").toTypedArray()))
                } else {
                  withText(*(action.content.split("|").toTypedArray()))
                }.findFirst()?.tryClick()
              }
              A11yType.VIEW_ID -> {
                withId(action.content).findFirst()?.tryClick()
              }
              A11yType.LONG_PRESS_TEXT -> {
                if (action.contains) {
                  containsText(*(action.content.split("|").toTypedArray()))
                } else {
                  withText(*(action.content.split("|").toTypedArray()))
                }.findFirst()?.tryLongClick()
              }
              A11yType.LONG_PRESS_VIEW_ID -> {
                withId(action.content).findFirst()?.tryLongClick()
              }
              A11yType.COORDINATE -> {
                val xy = action.content.trim().split(",")

                if (xy.size == 2) {
                  val x = xy[0].trim().toIntOrNull()
                  val y = xy[1].trim().toIntOrNull()

                  if (x != null && y != null) {
                    if (AppUtils.atLeastN()) {
                      click(x, y)
                    }
                  }
                }
              }
              A11yType.LONG_PRESS_COORDINATE -> {
                val xy = action.content.trim().split(",")
                if (xy.size == 2) {
                  val x = xy[0].trim().toIntOrNull()
                  val y = xy[1].trim().toIntOrNull()
                  if (x != null && y != null) {
                    if (AppUtils.atLeastN()) {
                      longClick(x, y)
                    }
                  }
                }
              }
            }
          }
        }
      }

      if (IceBox.getAppEnabledSetting(context, a11yEntity.applicationId) != 0) {
        val result = DefrostHandler.defrost(
          context,
          a11yEntity.applicationId,
          object : OnAppDefrostListener {
            override fun onAppDefrost() {
              action()
            }
          })
        if (!result) {
          ToastUtil.makeText(context, R.string.toast_not_choose_defrost_mode)
        }
      } else {
        action()
      }
      listener?.onOpened()
    } catch (e: NeedAccessibilityException) {
      Timber.e(e)
      ToastUtil.Toasty.show(context, R.string.toast_grant_accessibility)
    } catch (e: Exception) {
      Timber.e(e)
      listener?.onOpened()
    }
  }

  interface OnOpenListener {
    fun onOpened()
  }
}
