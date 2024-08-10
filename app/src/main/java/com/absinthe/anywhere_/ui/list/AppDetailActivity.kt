package com.absinthe.anywhere_.ui.list

import android.animation.LayoutTransition
import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.LinearLayout
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import com.absinthe.anywhere_.AppBarActivity
import com.absinthe.anywhere_.R
import com.absinthe.anywhere_.adapter.applist.AppListAdapter
import com.absinthe.anywhere_.adapter.applist.AppListDiffCallback
import com.absinthe.anywhere_.adapter.applist.MODE_APP_DETAIL
import com.absinthe.anywhere_.adapter.manager.WrapContentLinearLayoutManager
import com.absinthe.anywhere_.constants.AnywhereType
import com.absinthe.anywhere_.constants.Const
import com.absinthe.anywhere_.databinding.ActivityAppDetailBinding
import com.absinthe.anywhere_.model.database.AnywhereEntity
import com.absinthe.anywhere_.model.viewholder.AppListBean
import com.absinthe.anywhere_.ui.editor.EXTRA_EDIT_MODE
import com.absinthe.anywhere_.ui.editor.EXTRA_ENTITY
import com.absinthe.anywhere_.ui.editor.EditorActivity
import com.absinthe.anywhere_.utils.ToastUtil
import com.blankj.utilcode.util.ActivityUtils
import com.catchingnow.icebox.sdk_client.IceBox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import rikka.widget.borderview.BorderView

const val EXTRA_APP_DETAIL_ENTRY_MODE = "EXTRA_APP_DETAIL_ENTRY_MODE"

class AppDetailActivity : AppBarActivity<ActivityAppDetailBinding>(),
  SearchView.OnQueryTextListener {

  private var mAdapter: AppListAdapter = AppListAdapter(MODE_APP_DETAIL)
  private var isListReady = false
  private val mItems = mutableListOf<AppListBean>()
  private val entryMode by lazy { intent.getIntExtra(EXTRA_APP_DETAIL_ENTRY_MODE, MODE_NORMAL) }

  override fun setViewBinding() = ActivityAppDetailBinding.inflate(layoutInflater)

  override fun getToolBar() = binding.toolbar.toolBar

  override fun getAppBarLayout() = binding.toolbar.appBar

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    intent?.let {
      getToolBar().title = it.getStringExtra(Const.INTENT_EXTRA_APP_NAME)
      initRecyclerView()
      it.getStringExtra(Const.INTENT_EXTRA_PKG_NAME)?.let { packageName ->
        initData(packageName)
      }
    } ?: finish()
  }

  private fun initRecyclerView() {
    mAdapter.setDiffCallback(AppListDiffCallback())
    mAdapter.setOnItemClickListener { _, _, position ->
      val item = mAdapter.getItem(position)

      if (entryMode == MODE_NORMAL) {
        val ae = AnywhereEntity().apply {
          appName = item.appName
          param1 = item.packageName
          param2 = item.className.removePrefix(item.packageName)
          type = AnywhereType.Card.ACTIVITY
        }
        startActivity(Intent(this, EditorActivity::class.java).apply {
          putExtra(EXTRA_ENTITY, ae)
          putExtra(EXTRA_EDIT_MODE, false)
        })
      } else {
        val intent = Intent().apply {
          putExtra(EXTRA_PACKAGE_NAME, item.className)
        }
        setResult(Const.REQUEST_CODE_APP_DETAIL_SELECT, intent)
        finish()
      }
    }
    binding.list.apply {
      layoutManager = WrapContentLinearLayoutManager(this@AppDetailActivity)
      adapter = mAdapter
      borderVisibilityChangedListener =
        BorderView.OnBorderVisibilityChangedListener { top: Boolean, _: Boolean, _: Boolean, _: Boolean ->
          getAppBarLayout().isLifted = !top
        }
      FastScrollerBuilder(this).useMd2Style().build()
    }
  }

  private fun initData(pkgName: String) {

    binding.progressHorizontal.show()

    lifecycleScope.launch(Dispatchers.IO) {
      try {
        //Get all activity classes in the AndroidManifest.xml
        val packageInfo = packageManager.getPackageInfo(pkgName, PackageManager.GET_ACTIVITIES)

        val isFrozen = try {
          IceBox.getAppEnabledSetting(this@AppDetailActivity, pkgName) != 0 //0 means available
        } catch (e: PackageManager.NameNotFoundException) {
          false
        }

        val appPackageInfo = if (!isFrozen) {
          packageInfo
        } else {
          val pmFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            PackageManager.MATCH_DISABLED_COMPONENTS
          } else {
            PackageManager.GET_DISABLED_COMPONENTS
          }
          packageManager.getPackageArchiveInfo(
            packageInfo.applicationInfo!!.sourceDir,
            PackageManager.GET_ACTIVITIES or pmFlag
          )
        }

        appPackageInfo?.activities?.let { activities ->
          for (ai in activities) {
            val launchActivity = ActivityUtils.getLauncherActivity(ai.packageName)
            val bean = AppListBean(
              id = ai.name,
              appName = when {
                ai.name == launchActivity -> {
                  "${ai.loadLabel(packageManager)} (Launcher)"
                }
                ai.exported -> {
                  "${ai.loadLabel(packageManager)} (Exported)"
                }
                else -> {
                  ai.loadLabel(packageManager).toString()
                }
              },
              isExported = ai.exported,
              packageName = pkgName,
              className = ai.name,
              icon = ai.loadIcon(packageManager),
              type = AnywhereType.Card.ACTIVITY
            )
            mItems.add(bean)
          }
        }
        mItems.sortByDescending { it.isExported }
      } catch (exception: PackageManager.NameNotFoundException) {
        exception.printStackTrace()
      } catch (exception: RuntimeException) {
        exception.printStackTrace()
      }

      if (mItems.isEmpty()) {
        withContext(Dispatchers.Main) {
          binding.vfContainer.displayedChild = 1
          binding.progressHorizontal.hide()
        }
      } else {
        val launchActivity = ActivityUtils.getLauncherActivity(mItems[0].packageName)
        mItems.find { it.className == launchActivity }?.let {
          it.isLaunchActivity = true
        }
        withContext(Dispatchers.Main) {
          mAdapter.setDiffNewData(mItems) {
            isListReady = true
            getToolBar().menu?.findItem(R.id.search)?.isVisible = true
            invalidateOptionsMenu()
            binding.progressHorizontal.hide()
            binding.vfContainer.displayedChild = 0
          }
        }
      }
    }
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.app_detail_menu, menu)

    val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
    val searchView = menu.findItem(R.id.search).actionView as SearchView

    searchView.apply {
      findViewById<LinearLayout>(androidx.appcompat.R.id.search_bar)?.layoutTransition =
        LayoutTransition()
      isSubmitButtonEnabled = true // Display "Start search" button
      isQueryRefinementEnabled = true
      setIconifiedByDefault(false)
      setSearchableInfo(searchManager.getSearchableInfo(componentName))
      setOnQueryTextListener(this@AppDetailActivity)
    }

    if (!isListReady) {
      menu.findItem(R.id.search).isVisible = false
      invalidateOptionsMenu()
    }

    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == R.id.open_detail) {
      try {
        startActivity(Intent().apply {
          action = "android.intent.action.SHOW_APP_INFO"
          putExtra(
            "android.intent.extra.PACKAGE_NAME",
            intent.getStringExtra(Const.INTENT_EXTRA_PKG_NAME)
          )
        })
      } catch (e: ActivityNotFoundException) {
        ToastUtil.makeText(R.string.toast_no_react_show_info)
      }
    }
    return super.onOptionsItemSelected(item)
  }

  override fun onQueryTextSubmit(query: String): Boolean {
    return false
  }

  override fun onQueryTextChange(newText: String): Boolean {
    val filter = mItems.filter {
      it.appName.contains(newText, ignoreCase = true) || it.className.contains(
        newText,
        ignoreCase = true
      )
    }
    mAdapter.setDiffNewData(filter.toMutableList())
    return false
  }

}
