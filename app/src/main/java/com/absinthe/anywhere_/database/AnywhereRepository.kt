package com.absinthe.anywhere_.database

import android.app.Application
import androidx.lifecycle.LiveData
import com.absinthe.anywhere_.constants.Const
import com.absinthe.anywhere_.constants.GlobalValues
import com.absinthe.anywhere_.model.database.AnywhereEntity
import com.absinthe.anywhere_.model.database.PageEntity
import com.absinthe.anywhere_.utils.AppUtils
import com.absinthe.anywhere_.utils.ShortcutsUtils
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AnywhereRepository(application: Application) {

  var allAnywhereEntities: LiveData<List<AnywhereEntity>>
    private set
  val allPageEntities: LiveData<List<PageEntity>>

  private val mAnywhereDao: AnywhereDao =
    AnywhereRoomDatabase.getDatabase(application).anywhereDao()

  private val sortedEntities: LiveData<List<AnywhereEntity>>
    get() = when (GlobalValues.sortMode) {
      Const.SORT_MODE_TIME_ASC -> mAnywhereDao.allAnywhereEntitiesOrderByTimeAsc
      Const.SORT_MODE_NAME_ASC -> mAnywhereDao.allAnywhereEntitiesOrderByNameAsc
      Const.SORT_MODE_NAME_DESC -> mAnywhereDao.allAnywhereEntitiesOrderByNameDesc
      Const.SORT_MODE_TIME_DESC -> mAnywhereDao.allAnywhereEntitiesOrderByTimeDesc
      else -> mAnywhereDao.allAnywhereEntitiesOrderByTimeDesc
    }

  init {
    allPageEntities = mAnywhereDao.allPageEntities
    allAnywhereEntities = sortedEntities
  }

  fun refresh() {
    allAnywhereEntities = sortedEntities
  }

  @OptIn(DelicateCoroutinesApi::class)
  fun insert(ae: AnywhereEntity) = GlobalScope.launch(Dispatchers.IO) {
    mAnywhereDao.insert(ae)
    GlobalValues.needBackup = true
  }

  @OptIn(DelicateCoroutinesApi::class)
  fun insert(list: List<AnywhereEntity>) = GlobalScope.launch(Dispatchers.IO) {
    mAnywhereDao.insert(list)
    GlobalValues.needBackup = true
  }

  @OptIn(DelicateCoroutinesApi::class)
  fun update(ae: AnywhereEntity) = GlobalScope.launch(Dispatchers.IO) {
    mAnywhereDao.update(ae)
    GlobalValues.needBackup = true
  }

  @OptIn(DelicateCoroutinesApi::class)
  fun update(list: List<AnywhereEntity>) = GlobalScope.launch(Dispatchers.IO) {
    mAnywhereDao.update(list)
    GlobalValues.needBackup = true
  }

  @OptIn(DelicateCoroutinesApi::class)
  fun delete(ae: AnywhereEntity, delayTime: Long = 0L) = GlobalScope.launch(Dispatchers.IO) {
    delay(delayTime)
    mAnywhereDao.delete(ae)
    if (AppUtils.atLeastNMR1()) {
      ShortcutsUtils.removeShortcut(ae)
    }
    GlobalValues.needBackup = true
  }

  @OptIn(DelicateCoroutinesApi::class)
  fun delete(list: List<AnywhereEntity>, delayTime: Long = 0L) =
    GlobalScope.launch(Dispatchers.IO) {
      delay(delayTime)
      mAnywhereDao.delete(list)
      if (AppUtils.atLeastNMR1()) {
        list.forEach { ShortcutsUtils.removeShortcut(it) }
      }
      GlobalValues.needBackup = true
    }

  @OptIn(DelicateCoroutinesApi::class)
  fun insertPage(pe: PageEntity) = GlobalScope.launch(Dispatchers.IO) {
    mAnywhereDao.insertPage(pe)
    GlobalValues.needBackup = true
  }

  @OptIn(DelicateCoroutinesApi::class)
  fun insertPage(pageList: List<PageEntity>) = GlobalScope.launch(Dispatchers.IO) {
    mAnywhereDao.insertPage(pageList)
    GlobalValues.needBackup = true
  }

  @OptIn(DelicateCoroutinesApi::class)
  fun updatePage(pe: PageEntity) = GlobalScope.launch(Dispatchers.IO) {
    mAnywhereDao.updatePage(pe)
    GlobalValues.needBackup = true
  }

  @OptIn(DelicateCoroutinesApi::class)
  fun deletePage(pe: PageEntity) = GlobalScope.launch(Dispatchers.IO) {
    mAnywhereDao.deletePage(pe)
    GlobalValues.needBackup = true
  }

  fun getEntityById(id: String): AnywhereEntity? {
    return mAnywhereDao.getEntityById(id)
  }

  fun getPageEntityByTitle(title: String?): PageEntity? {
    if (title == null) {
      return null
    }
    return mAnywhereDao.getPageEntityByTitle(title)
  }
}
