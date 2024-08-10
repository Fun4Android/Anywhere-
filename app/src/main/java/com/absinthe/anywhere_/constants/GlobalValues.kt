package com.absinthe.anywhere_.constants

import androidx.core.text.HtmlCompat
import androidx.lifecycle.MutableLiveData
import com.absinthe.anywhere_.BuildConfig
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.tencent.mmkv.MMKV

object GlobalValues {

  var spName = if (BuildConfig.DEBUG) {
    Const.SP_NAME_DEBUG
  } else {
    Const.SP_NAME
  }

  val mmkv: MMKV = MMKV.mmkvWithID(spName)

  var sIsDebugMode = false
  var shouldListenClipBoard = true

  var cardModeLiveData = MutableLiveData<Any>()

  var shortcutsList = listOf<String>()
    get() = try {
      Gson().fromJson<List<String>>(
        mmkv.decodeString(Const.SHORTCUTS_LIST),
        object : TypeToken<List<String>>() {}.type
      )
        ?: listOf()
    } catch (e: JsonSyntaxException) {
      listOf()
    }
    set(value) {
      field = value
      mmkv.encode(Const.SHORTCUTS_LIST, Gson().toJson(value))
      shortcutListChanged = true
    }
  var shortcutListChanged = false

  var cardMode
    get() = mmkv.decodeString(Const.PREF_CARD_MODE) ?: Const.PREF_CARD_MODE_MEDIUM
    set(value) {
      mmkv.encode(Const.PREF_CARD_MODE, value)
    }

  var closeAfterLaunch
    get() = mmkv.decodeBool(Const.PREF_CLOSE_AFTER_LAUNCH, false)
    set(value) {
      mmkv.encode(Const.PREF_CLOSE_AFTER_LAUNCH, value)
    }

  var isPages
    get() = mmkv.decodeBool(Const.PREF_PAGES)
    set(value) {
      mmkv.encode(Const.PREF_PAGES, value)
    }

  var isCollectorPlus
    get() = mmkv.decodeBool(Const.PREF_COLLECTOR_PLUS)
    set(value) {
      mmkv.encode(Const.PREF_COLLECTOR_PLUS, value)
    }

  var isExcludeFromRecent
    get() = mmkv.decodeBool(Const.PREF_EXCLUDE_FROM_RECENT)
    set(value) {
      mmkv.encode(Const.PREF_EXCLUDE_FROM_RECENT, value)
    }

  var showShellResultMode
    get() = mmkv.decodeString(Const.PREF_SHOW_SHELL_RESULT_MODE) ?: Const.SHELL_RESULT_TOAST
    set(value) {
      mmkv.encode(Const.PREF_SHOW_SHELL_RESULT_MODE, value)
    }

  var isAutoBackup
    get() = mmkv.decodeBool(Const.PREF_WEBDAV_AUTO_BACKUP, true)
    set(value) {
      mmkv.encode(Const.PREF_WEBDAV_AUTO_BACKUP, value)
    }

  var workingMode
    get() = mmkv.decodeString(Const.PREF_WORKING_MODE, Const.WORKING_MODE_URL_SCHEME)
      ?: Const.WORKING_MODE_URL_SCHEME
    set(value) {
      mmkv.encode(Const.PREF_WORKING_MODE, value)
    }

  var actionBarType
    get() = mmkv.decodeString(Const.PREF_ACTION_BAR_TYPE, "").orEmpty()
    set(value) {
      mmkv.encode(Const.PREF_ACTION_BAR_TYPE, value)
    }

  var darkMode
    get() = mmkv.decodeString(Const.PREF_DARK_MODE, "").orEmpty()
    set(value) {
      mmkv.encode(Const.PREF_DARK_MODE, value)
    }

  var backgroundUri
    get() = mmkv.decodeString(Const.PREF_CHANGE_BACKGROUND, "").orEmpty()
    set(value) {
      mmkv.encode(Const.PREF_CHANGE_BACKGROUND, value)
    }

  var sCardBackgroundMode
    get() = mmkv.decodeString(Const.PREF_CARD_BACKGROUND, "off") ?: "off"
    set(value) {
      mmkv.encode(Const.PREF_CARD_BACKGROUND, value)
    }

  var sortMode
    get() = mmkv.decodeString(Const.PREF_SORT_MODE, "").orEmpty()
    set(value) {
      mmkv.encode(Const.PREF_SORT_MODE, value)
    }

  var iconPack
    get() = mmkv.decodeString(Const.PREF_ICON_PACK, "").orEmpty()
    set(value) {
      mmkv.encode(Const.PREF_ICON_PACK, value)
    }

  var category
    get() = mmkv.decodeString(Const.PREF_CURR_CATEGORY, AnywhereType.Category.DEFAULT_CATEGORY)
      ?: AnywhereType.Category.DEFAULT_CATEGORY
    set(value) {
      mmkv.encode(Const.PREF_CURR_CATEGORY, value)
    }

  var defrostMode
    get() = mmkv.decodeString(Const.PREF_DEFROST_MODE, "").orEmpty()
    set(value) {
      mmkv.encode(Const.PREF_DEFROST_MODE, value)
    }

  var webdavHost
    get() = mmkv.decodeString(Const.PREF_WEBDAV_HOST, "").orEmpty()
    set(value) {
      mmkv.encode(Const.PREF_WEBDAV_HOST, value)
    }

  var webdavUsername
    get() = mmkv.decodeString(Const.PREF_WEBDAV_USERNAME, "").orEmpty()
    set(value) {
      mmkv.encode(Const.PREF_WEBDAV_USERNAME, value)
    }

  var webdavPassword
    get() = mmkv.decodeString(Const.PREF_WEBDAV_PASSWORD, "").orEmpty()
    set(value) {
      mmkv.encode(Const.PREF_WEBDAV_PASSWORD, value)
    }

  var currentPage
    get() = mmkv.decodeInt(Const.PREF_CURR_PAGE_NUM, 0)
    set(value) {
      mmkv.encode(Const.PREF_CURR_PAGE_NUM, value)
    }

  var dumpInterval
    get() = mmkv.decodeInt(Const.PREF_DUMP_INTERVAL, 1000)
    set(value) {
      mmkv.encode(Const.PREF_DUMP_INTERVAL, value)
    }

  var autoDarkModeStart
    get() = mmkv.decodeLong(Const.PREF_AUTO_DARK_MODE_START, 0)
    set(value) {
      mmkv.encode(Const.PREF_AUTO_DARK_MODE_START, value)
    }

  var autoDarkModeEnd
    get() = mmkv.decodeLong(Const.PREF_AUTO_DARK_MODE_END, 0)
    set(value) {
      mmkv.encode(Const.PREF_AUTO_DARK_MODE_END, value)
    }

  var needBackup
    get() = mmkv.decodeBool(Const.PREF_NEED_BACKUP, true)
    set(value) {
      mmkv.encode(Const.PREF_NEED_BACKUP, value)
    }

  var editorEntryAnim
    get() = mmkv.decodeBool(Const.PREF_EDITOR_ENTRY_ANIM, false)
    set(value) {
      mmkv.encode(Const.PREF_EDITOR_ENTRY_ANIM, value)
    }

  var deprecatedScCreatingMethod
    get() = mmkv.decodeBool(Const.PREF_DEPRECATED_SC_CREATING_METHOD, false)
    set(value) {
      mmkv.encode(Const.PREF_DEPRECATED_SC_CREATING_METHOD, value)
    }

  var showSystemApps
    get() = mmkv.decodeBool(Const.PREF_SHOW_SYSTEM_APPS, false)
    set(value) {
      mmkv.encode(Const.PREF_SHOW_SYSTEM_APPS, value)
    }

  var showDefreezingToast
    get() = mmkv.decodeBool(Const.PREF_SHOW_DEFREEZING_TOAST, true)
    set(value) {
      mmkv.encode(Const.PREF_SHOW_DEFREEZING_TOAST, value)
    }

  var shouldListenClipBoardPref
    get() = mmkv.decodeBool(Const.PREF_LISTEN_CLIP_BOARD, true)
    set(value) {
      mmkv.encode(Const.PREF_LISTEN_CLIP_BOARD, value)
    }

  val info: CharSequence
    get() {
      val sb = StringBuilder()
        .append(getInfoLine("Working Mode", workingMode))
        .append(getInfoLine("Background Uri", backgroundUri))
        .append(getInfoLine("ActionBar Type", actionBarType))
        .append(getInfoLine("Sort Mode", sortMode))
        .append(getInfoLine("Icon Pack", iconPack))
        .append(getInfoLine("Dark Mode", darkMode))
        .append(getInfoLine("Card Background Mode", sCardBackgroundMode))
        .append(getInfoLine("Dump Interval", dumpInterval.toString()))
        .append(getInfoLine("Current Page", currentPage.toString()))
        .append(getInfoLine("Defrost Mode", defrostMode))
        .append(getInfoLine("Current Category", category))
      return HtmlCompat.fromHtml(sb.toString(), HtmlCompat.FROM_HTML_MODE_LEGACY)
    }

  val collectorMode: String
    get() = if (isCollectorPlus) {
      "Collector+"
    } else {
      "Collector"
    }

  private fun getInfoLine(infoName: String, infoValue: String?): CharSequence {
    return StringBuilder()
      .append("<b>").append(infoName).append("</b>")
      .append(": ").append(infoValue).append("<br>")
  }

  fun clearActionBarType() {
    actionBarType = ""
  }

  fun setsCategory(sCategory: String, page: Int) {
    category = sCategory
    currentPage = page
  }
}
