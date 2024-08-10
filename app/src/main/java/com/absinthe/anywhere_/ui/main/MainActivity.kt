package com.absinthe.anywhere_.ui.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.HapticFeedbackConstants
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.widget.ImageButton
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.absinthe.anywhere_.AnywhereApplication
import com.absinthe.anywhere_.BaseActivity
import com.absinthe.anywhere_.R
import com.absinthe.anywhere_.adapter.ItemTouchCallBack
import com.absinthe.anywhere_.adapter.manager.WrapContentLinearLayoutManager
import com.absinthe.anywhere_.adapter.page.PageListAdapter
import com.absinthe.anywhere_.adapter.page.PageTitleNode
import com.absinthe.anywhere_.adapter.page.PageTitleProvider
import com.absinthe.anywhere_.constants.AnywhereType
import com.absinthe.anywhere_.constants.Const
import com.absinthe.anywhere_.constants.EventTag
import com.absinthe.anywhere_.constants.GlobalValues
import com.absinthe.anywhere_.constants.GlobalValues.setsCategory
import com.absinthe.anywhere_.constants.OnceTag
import com.absinthe.anywhere_.databinding.ActivityMainBinding
import com.absinthe.anywhere_.model.database.AnywhereEntity
import com.absinthe.anywhere_.model.database.PageEntity
import com.absinthe.anywhere_.services.BackupIntentService
import com.absinthe.anywhere_.services.overlay.CollectorService
import com.absinthe.anywhere_.services.overlay.ICollectorService
import com.absinthe.anywhere_.ui.cloud.CloudRulesActivity
import com.absinthe.anywhere_.ui.editor.EXTRA_EDIT_MODE
import com.absinthe.anywhere_.ui.editor.EXTRA_ENTITY
import com.absinthe.anywhere_.ui.editor.EditorActivity
import com.absinthe.anywhere_.ui.list.AppListActivity
import com.absinthe.anywhere_.ui.qrcode.QRCodeCollectionActivity
import com.absinthe.anywhere_.ui.settings.SettingsActivity
import com.absinthe.anywhere_.ui.setup.SetupActivity
import com.absinthe.anywhere_.ui.shortcuts.ShortcutsActivity
import com.absinthe.anywhere_.ui.shortcuts.ThirdAppsShortcutActivity
import com.absinthe.anywhere_.utils.AppTextUtils
import com.absinthe.anywhere_.utils.CipherUtils.decrypt
import com.absinthe.anywhere_.utils.ClipboardUtil
import com.absinthe.anywhere_.utils.ClipboardUtil.clearClipboard
import com.absinthe.anywhere_.utils.ClipboardUtil.getClipBoardText
import com.absinthe.anywhere_.utils.ToastUtil
import com.absinthe.anywhere_.utils.UxUtils
import com.absinthe.anywhere_.utils.doOnMainThreadIdle
import com.absinthe.anywhere_.utils.handler.Opener
import com.absinthe.anywhere_.utils.manager.CardTypeIconGenerator
import com.absinthe.anywhere_.utils.manager.DialogManager.showAdvancedCardSelectDialog
import com.absinthe.anywhere_.utils.manager.URLManager
import com.absinthe.anywhere_.view.home.FabBuilder.build
import com.absinthe.anywhere_.viewmodel.AnywhereViewModel
import com.absinthe.libraries.utils.extensions.dp
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.AppUtils
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.entity.node.BaseNode
import com.google.android.material.transition.platform.MaterialContainerTransformSharedElementCallback
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.leinardi.android.speeddial.SpeedDialActionItem
import com.microsoft.appcenter.analytics.Analytics
import it.sephiroth.android.library.xtooltip.ClosePolicy.Companion.TOUCH_ANYWHERE_CONSUME
import it.sephiroth.android.library.xtooltip.Tooltip
import jonathanfinerty.once.Once
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class MainActivity : BaseActivity<ActivityMainBinding>() {

  private val viewModel by viewModels<AnywhereViewModel>()
  private lateinit var mItemTouchHelper: ItemTouchHelper
  private lateinit var mObserver: Observer<List<PageEntity>?>

  private var isBound = false
  private var isTitleShown = false
  private var shouldFinish = false
  private var hasResumed = false
  private var collectorService: ICollectorService? = null
  private var mToggle: ActionBarDrawerToggle? = null

  private val conn = object : ServiceConnection {
    override fun onServiceDisconnected(name: ComponentName?) {
      isBound = false
      collectorService = null
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
      isBound = true
      collectorService = ICollectorService.Stub.asInterface(service)
      collectorService?.startCollector()
      ActivityUtils.startHomeActivity()
    }
  }
  private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

  override fun setViewBinding() = ActivityMainBinding.inflate(layoutInflater)

  override fun onCreate(savedInstanceState: Bundle?) {
    if (!Once.beenDone(Once.THIS_APP_INSTALL, OnceTag.FIRST_GUIDE)) {
      finish()
      startActivity(Intent(this, SetupActivity::class.java))
    }

    window.apply {
      requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
      sharedElementsUseOverlay = false
    }
    setExitSharedElementCallback(MaterialContainerTransformSharedElementCallback())

    super.onCreate(savedInstanceState)
    initObserver()
    getAnywhereIntent(intent)
    backupIfNeeded()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
          Timber.d("Request post notification: $isGranted")
        }
    }
    checkNotificationPermission()
    checkCardCategory()
  }

  override fun onResume() {
    super.onResume()
    if (shouldFinish && hasResumed) {
      finish()
      return
    }
    hasResumed = true

    if (GlobalValues.shouldListenClipBoardPref && GlobalValues.shouldListenClipBoard) {
      getClipBoardText(this, object : ClipboardUtil.Function {
        override fun invoke(text: String) {
          if (text.contains(URLManager.ANYWHERE_SCHEME)) {
            processUri(text.toUri())
            clearClipboard(this@MainActivity)
          }
        }
      })
    } else {
      GlobalValues.shouldListenClipBoard = true
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    getAnywhereIntent(intent)
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    loadBackground(GlobalValues.backgroundUri)
    mToggle?.onConfigurationChanged(newConfig)
  }

  override fun onPrepareOptionsMenu(menu: Menu): Boolean {
    if (GlobalValues.actionBarType == Const.ACTION_BAR_TYPE_LIGHT
      || (isNightMode() && GlobalValues.backgroundUri.isEmpty())
    ) {
      UxUtils.tintToolbarIcon(this, menu, mToggle, Const.ACTION_BAR_TYPE_LIGHT)
    } else {
      UxUtils.tintToolbarIcon(this, menu, mToggle, Const.ACTION_BAR_TYPE_DARK)
    }
    return super.onPrepareOptionsMenu(menu)
  }

  @SuppressLint("RestrictedApi")
  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.toolbar_settings -> {
        startActivity(Intent(this, SettingsActivity::class.java))
      }
      R.id.toolbar_sort -> {
        val popup = PopupMenu(this, findViewById(R.id.toolbar_sort))
        popup.menuInflater.inflate(R.menu.sort_menu, popup.menu)

        if (popup.menu is MenuBuilder) {
          (popup.menu as MenuBuilder).setOptionalIconsVisible(true)
        }

        when (GlobalValues.sortMode) {
          Const.SORT_MODE_TIME_DESC -> popup.menu.getItem(0).isChecked = true
          Const.SORT_MODE_TIME_ASC -> popup.menu.getItem(1).isChecked = true
          Const.SORT_MODE_NAME_DESC -> popup.menu.getItem(2).isChecked = true
          Const.SORT_MODE_NAME_ASC -> popup.menu.getItem(3).isChecked = true
          else -> popup.menu.getItem(0).isChecked = true
        }

        popup.setOnMenuItemClickListener { popupItem: MenuItem ->
          when (popupItem.itemId) {
            R.id.sort_by_time_desc -> GlobalValues.sortMode = Const.SORT_MODE_TIME_DESC
            R.id.sort_by_time_asc -> GlobalValues.sortMode = Const.SORT_MODE_TIME_ASC
            R.id.sort_by_name_desc -> GlobalValues.sortMode = Const.SORT_MODE_NAME_DESC
            R.id.sort_by_name_asc -> GlobalValues.sortMode = Const.SORT_MODE_NAME_ASC
            R.id.sort -> {
              binding.viewPager.isUserInputEnabled = false
              CategoryCardFragment.currentReference?.get()?.sort()
            }
            R.id.multi_select -> {
              binding.viewPager.isUserInputEnabled = false
              CategoryCardFragment.currentReference?.get()?.multiSelect()
            }
          }
          if (popupItem.itemId == R.id.sort_by_time_desc ||
            popupItem.itemId == R.id.sort_by_time_asc ||
            popupItem.itemId == R.id.sort_by_name_desc ||
            popupItem.itemId == R.id.sort_by_name_asc
          ) {
            CategoryCardFragment.currentReference?.get()?.refreshSortMode()
          }
          true
        }
        popup.show()
      }
      R.id.toolbar_done -> {
        CategoryCardFragment.currentReference?.get()?.editDone()
        binding.viewPager.isUserInputEnabled = true
      }
      R.id.toolbar_delete -> {
        CategoryCardFragment.currentReference?.get()?.deleteSelected()
      }
      R.id.toolbar_move -> {
        CategoryCardFragment.currentReference?.get()?.moveSelected()
      }
      R.id.toolbar_create_sc -> {
        CategoryCardFragment.currentReference?.get()?.createShortcutSelected()
      }
    }

    return if (mToggle?.onOptionsItemSelected(item) == true) {
      true
    } else super.onOptionsItemSelected(item)
  }

  override fun onBackPressed() {
    super.onBackPressed()
    when {
      binding.drawer.isDrawerVisible(GravityCompat.START) -> {
        binding.drawer.closeDrawer(GravityCompat.START)
      }
      binding.fab.isOpen -> {
        binding.fab.close()
      }
      else -> {
        backupIfNeeded()
        finish()
      }
    }
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.main_menu, menu)
    return true
  }

  override fun initView() {
    setSupportActionBar(binding.toolbar)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    binding.toolbar.title = ""
    GlobalValues.backgroundUri.apply {
      if (isNotEmpty()) {
        loadBackground(this)
        UxUtils.setAdaptiveToolbarTitleColor(this@MainActivity, binding.tsTitle)
        UxUtils.setActionBarTransparent(this@MainActivity)
      }
    }
    binding.tsTitle.setText(UxUtils.getToolbarTitle())
    binding.fullDraggableContainer.setEnableDrawer(GlobalValues.isPages)

    initFab()
    CardTypeIconGenerator

    AnywhereApplication.sRepository.allPageEntities.observe(this) {
      if (it.isNotEmpty()) {
        binding.viewPager.apply {
          offscreenPageLimit = 2
          adapter = object : FragmentStateAdapter(this@MainActivity) {
            override fun getItemCount(): Int {
              return it.size.coerceAtLeast(1)
            }

            override fun createFragment(position: Int): Fragment {
              return CategoryCardFragment.newInstance(it[position].title)
            }
          }

          // 当ViewPager切换页面时，改变页码
          registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
              super.onPageSelected(position)
              val pos = if (position >= it.size) it.size - 1 else position
              setsCategory(it[pos].title, pos)

              if (isTitleShown) {
                binding.tsTitle.setText(it[pos].title)
              }
            }
          })

          registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrollStateChanged(state: Int) {
              if (binding.fab.isOpen) {
                binding.fab.close()
              }
            }
          })

          getChildAt(0)?.overScrollMode = RecyclerView.OVER_SCROLL_NEVER

          isUserInputEnabled = GlobalValues.isPages
          if (GlobalValues.isPages) {
            setCurrentItem(GlobalValues.currentPage, false)
          } else {
            setCurrentItem(0, false)
          }
        }
      }
    }

    supportActionBar?.let {
      if (GlobalValues.isPages) {
        mToggle = ActionBarDrawerToggle(
          this, binding.drawer, binding.toolbar,
          R.string.drawer_open, R.string.drawer_close
        ).also { toggle ->
          if (GlobalValues.actionBarType == Const.ACTION_BAR_TYPE_DARK) {
            toggle.drawerArrowDrawable.color = Color.BLACK
          } else {
            toggle.drawerArrowDrawable.color = Color.WHITE
          }

          it.setDisplayHomeAsUpEnabled(true)
          binding.drawer.addDrawerListener(toggle)
          toggle.syncState()
          AnywhereApplication.sRepository.allAnywhereEntities.observe(this) {
            initDrawer(binding.drawer)
          }
        }
      } else {
        it.setHomeButtonEnabled(false)
        it.setDisplayHomeAsUpEnabled(false)
        binding.drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
      }
    }

    if (GlobalValues.isPages) {
      lifecycleScope.launchWhenResumed {
        delay(2000)

        withContext(Dispatchers.Main) {
          binding.tsTitle.setText(GlobalValues.category)
          isTitleShown = true
        }
      }
    }
  }

  private fun initDrawer(drawer: DrawerLayout) {
    binding.drawer.setStatusBarBackground(null)
    val adapter = PageListAdapter()
    val touchCallBack = ItemTouchCallBack().apply {
      setOnItemTouchListener(adapter)
    }
    mItemTouchHelper = ItemTouchHelper(touchCallBack).apply {
      attachToRecyclerView(null)
    }

    adapter.setOnItemChildClickListener { _: BaseQuickAdapter<*, *>, view: View, position: Int ->

      if (view.id == R.id.iv_entry) {
        drawer.closeDrawer(GravityCompat.START)

        doOnMainThreadIdle({
          (adapter.getItem(position) as PageTitleNode?)?.let { titleNode ->
            AnywhereApplication.sRepository.allPageEntities.value?.find { it.title == titleNode.title }
              ?.let { pe ->
                try {
                  binding.viewPager.setCurrentItem(
                    AnywhereApplication.sRepository.allPageEntities.value!!.indexOf(
                      pe
                    ), true
                  )
                  setsCategory(pe.title, position)
                } catch (e: Exception) {
                  e.printStackTrace()
                }
              }
          }
        })
      }
    }

    AnywhereApplication.sRepository.allPageEntities.observe(
      this
    ) { pageEntities: List<PageEntity>? ->
      pageEntities?.let { setupDrawerData(adapter, it) }
    }

    binding.rvPages.apply {
      this.adapter = adapter
      layoutManager = WrapContentLinearLayoutManager(this@MainActivity)
      (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
    }
    val ibAdd: ImageButton = drawer.findViewById(R.id.ib_add)
    val ibPageSort: ImageButton = drawer.findViewById(R.id.ib_sort_page)
    val ibDone: ImageButton = drawer.findViewById(R.id.ib_done)

    ibAdd.setOnClickListener {
      viewModel.addPage()
    }
    ibPageSort.setOnClickListener {
      ibPageSort.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)

      var i = 0
      while (i < adapter.data.size) {
        adapter.collapse(i)
        i++
      }

      PageTitleProvider.isEditMode = true
      binding.rvPages.isEditMode = true
      mItemTouchHelper.attachToRecyclerView(binding.rvPages)

      ibAdd.visibility = View.GONE
      ibPageSort.visibility = View.GONE
      ibDone.visibility = View.VISIBLE
    }
    ibDone.setOnClickListener {
      ibDone.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
      PageTitleProvider.isEditMode = false
      binding.rvPages.isEditMode = false
      mItemTouchHelper.attachToRecyclerView(null)

      ibAdd.visibility = View.VISIBLE
      ibPageSort.visibility = View.VISIBLE
      ibDone.visibility = View.GONE

      val list: List<BaseNode> = adapter.data
      val map = HashMap<String, Int>()
      var i = 1
      for (node in list) {
        if (node is PageTitleNode) {
          map[node.title] = i
          i++
        }
      }

      AnywhereApplication.sRepository.allPageEntities.value?.let {
        for (pe in it) {
          map[pe.title]?.let { entity ->
            pe.priority = entity
            AnywhereApplication.sRepository.updatePage(pe)
          }
        }
      }
    }
  }

  private fun setupDrawerData(adapter: PageListAdapter, pageEntities: List<PageEntity>) =
    lifecycleScope.launch(Dispatchers.IO) {
      val list: MutableList<BaseNode> = ArrayList()
      for (pe in pageEntities) {
        list.add(viewModel.getEntity(pe.title))
      }
      withContext(Dispatchers.Main) {
        adapter.setList(list)
      }
    }

  private fun initObserver() {
    mObserver = Observer { pageEntities ->
      if (pageEntities == null) return@Observer

      AnywhereApplication.sRepository.allPageEntities.removeObserver(mObserver)

      if (pageEntities.isEmpty() && !isPageInit) {
        val pe = PageEntity().apply {
          title = GlobalValues.category
          priority = 1
        }
        AnywhereApplication.sRepository.insertPage(pe)
        isPageInit = true
      }
    }

    AnywhereApplication.sRepository.allPageEntities.observe(this, mObserver)

    viewModel.background.observe(this) { s: String ->
      GlobalValues.backgroundUri = s

      if (s.isNotEmpty()) {
        loadBackground(GlobalValues.backgroundUri)
        UxUtils.setAdaptiveToolbarTitleColor(this@MainActivity, binding.tsTitle)
        UxUtils.setActionBarTransparent(this)
      }
    }
    viewModel.shouldShowFab.observe(this) {
      binding.fab.isVisible = it
    }
  }

  private fun initFab() {
    build(binding.fab)
    binding.fab.apply {
      mainFab.transitionName = "item_container"
      translationY = -16.dp.toFloat()
      setOnActionSelectedListener { actionItem: SpeedDialActionItem ->
        when (actionItem.id) {
          R.id.fab_advanced -> showAdvancedCardSelectDialog(this@MainActivity)
          R.id.fab_activity_list -> {
            startActivity(Intent(this@MainActivity, AppListActivity::class.java))
            Analytics.trackEvent(EventTag.FAB_ACTIVITY_LIST_CLICK)
          }
          R.id.fab_collector -> {
            viewModel.startCollector(
              this@MainActivity,
              object : AnywhereViewModel.OnStartCollectorListener {
                override fun onStart() {
                  if (isBound) {
                    collectorService?.startCollector()
                    ActivityUtils.startHomeActivity()
                  } else {
                    bindService(
                      Intent(this@MainActivity, CollectorService::class.java),
                      conn,
                      Context.BIND_AUTO_CREATE
                    )
                  }
                }
              })
            Analytics.trackEvent(EventTag.FAB_COLLECTOR_CLICK)
          }
          R.id.fab_qr_code_collection -> {
            startActivity(Intent(this@MainActivity, QRCodeCollectionActivity::class.java))
            Analytics.trackEvent(EventTag.FAB_QR_CODE_COLLECTION_CLICK)
          }
          R.id.fab_cloud_rules -> {
            startActivity(Intent(this@MainActivity, CloudRulesActivity::class.java))
            Analytics.trackEvent(EventTag.FAB_CLOUD_RULES_CLICK)
          }
          R.id.fab_third_apps_shortcut -> {
            startActivity(Intent(this@MainActivity, ThirdAppsShortcutActivity::class.java))
            Analytics.trackEvent(EventTag.FAB_THIRD_APPS_SHORTCUT_CLICK)
          }
          else -> return@setOnActionSelectedListener false
        }
        close()
        true
      }
    }

    if (!Once.beenDone(Once.THIS_APP_INSTALL, OnceTag.FAB_TIP)) {
      showFirstTip(binding.fab)

      viewModel.insert(AnywhereEntity().apply {
        appName = getString(R.string.help_card_title)
        type = AnywhereType.Card.URL_SCHEME
        param1 = URLManager.DOCUMENT_PAGE
      })

      Once.markDone(OnceTag.FAB_TIP)
    }
  }

  private fun getAnywhereIntent(intent: Intent) {
    val action = intent.action
    Timber.d("action = %s", action)

    when (action) {
        null, Intent.ACTION_VIEW -> {
          intent.data?.let {
            Timber.d("Received Url = %s", it.toString())
            Timber.d("Received path = %s", it.path)
            processUri(it)
          }
        }
        Intent.ACTION_SEND -> {
          val sharing = intent.getStringExtra(Intent.EXTRA_TEXT)
          viewModel.setUpUrlScheme(this, AppTextUtils.parseUrlFromSharingText(sharing))
        }
        ShortcutsActivity.ACTION_START_DEVICE_CONTROL -> {
          val type = intent.getIntExtra(Const.INTENT_EXTRA_TYPE, -1)
          val param1 = intent.getStringExtra(Const.INTENT_EXTRA_PARAM_1) ?: return
          val param2 = intent.getStringExtra(Const.INTENT_EXTRA_PARAM_2) ?: return
          val param3 = intent.getStringExtra(Const.INTENT_EXTRA_PARAM_3) ?: return
          val entity = AnywhereEntity().apply {
            this.type = type
            this.param1 = param1
            this.param2 = param2
            this.param3 = param3
          }
          Opener.with(this)
            .load(entity)
            .setOpenedListener(object : Opener.OnOpenListener {
              override fun onOpened() {
                shouldFinish = true
              }
            })
            .open()
        }
    }
  }

  private fun processUri(uri: Uri) {
    if (uri.host == URLManager.URL_HOST) {
      val param1 = uri.getQueryParameter(Const.INTENT_EXTRA_PARAM_1).orEmpty()
      val param2 = uri.getQueryParameter(Const.INTENT_EXTRA_PARAM_2).orEmpty()
      val param3 = uri.getQueryParameter(Const.INTENT_EXTRA_PARAM_3).orEmpty()
      val type = uri.getQueryParameter(Const.INTENT_EXTRA_TYPE) ?: return
      val fromCollector =
        runCatching { uri.getQueryParameter("fromCollector").toBoolean() }.getOrDefault(false)

      if (fromCollector) {
        if (CollectorService.serviceConnection != null) {
          applicationContext.unbindService(CollectorService.serviceConnection!!)
          CollectorService.serviceConnection = null
        }
      }

      when (type.toInt()) {
        AnywhereType.Card.URL_SCHEME -> {
          viewModel.setUpUrlScheme(this, param1)
        }
        AnywhereType.Card.ACTIVITY -> {
          val appName = AppUtils.getAppName(param1)
          val ae = AnywhereEntity().apply {
            this.appName = appName
            this.param1 = param1
            this.param2 = param2
            this.param3 = param3
            this.type = AnywhereType.Card.ACTIVITY
          }
          startActivity(Intent(this, EditorActivity::class.java).apply {
            putExtra(EXTRA_ENTITY, ae)
            putExtra(EXTRA_EDIT_MODE, false)
          })
        }
        AnywhereType.Card.SHELL -> {
          val ae = AnywhereEntity().apply {
            this.appName = AnywhereType.Card.NEW_TITLE_MAP[AnywhereType.Card.SHELL]!!
            this.param1 = param1
            this.param2 = param2
            this.param3 = param3
            this.type = AnywhereType.Card.SHELL
          }
          startActivity(Intent(this, EditorActivity::class.java).apply {
            putExtra(EXTRA_ENTITY, ae)
            putExtra(EXTRA_EDIT_MODE, false)
          })
        }
      }
    } else if (uri.host == URLManager.CARD_SHARING_HOST) {
      uri.path?.let {
        if (uri.toString().isNotEmpty()) {
          val encrypted = it.substring(1)
          val decrypted = decrypt(encrypted)

          try {
            startActivity(Intent(this, EditorActivity::class.java).apply {
              putExtra(EXTRA_ENTITY, Gson().fromJson(decrypted, AnywhereEntity::class.java))
              putExtra(EXTRA_EDIT_MODE, false)
            })
          } catch (e: JsonSyntaxException) {
            ToastUtil.makeText(R.string.toast_json_error)
          }
        }
      }
    }
  }

  private fun loadBackground(url: String) {
    Glide.with(applicationContext)
      .load(url)
      .override(resources.displayMetrics.widthPixels, resources.displayMetrics.heightPixels)
      .transition(DrawableTransitionOptions.withCrossFade())
      .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
      .into(binding.ivBack)
  }

  private fun showFirstTip(target: View) {
    target.post {
      Tooltip.Builder(this@MainActivity)
        .anchor(target, 0, 0, false)
        .text(getText(R.string.first_launch_guide_title))
        .closePolicy(TOUCH_ANYWHERE_CONSUME)
        .maxWidth(150.dp)
        .create()
        .show(target, Tooltip.Gravity.LEFT, true)
    }
  }

  private fun backupIfNeeded() {
    if (GlobalValues.webdavHost.isEmpty() ||
      GlobalValues.webdavUsername.isEmpty() ||
      GlobalValues.webdavPassword.isEmpty()
    ) {
      return
    }
    if (GlobalValues.needBackup && GlobalValues.isAutoBackup) {
      BackupIntentService.enqueueWork(this, Intent())
      GlobalValues.needBackup = false
    }
  }

  private fun checkNotificationPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ContextCompat.checkSelfPermission(
          this,
          Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED
      ) {
        if (!shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
          requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
      }
    }
  }

  private fun checkCardCategory() {
    AnywhereApplication.sRepository.allAnywhereEntities.observe(this) {
      lifecycleScope.launch(Dispatchers.IO) {
        it.asSequence().forEach {
          if (AnywhereApplication.sRepository.getPageEntityByTitle(it.category) == null) {
            AnywhereApplication.sRepository.insertPage(
              PageEntity().apply {
                title = it.category ?: AnywhereType.Category.DEFAULT_CATEGORY
                priority = AnywhereApplication.sRepository.allPageEntities.value?.size ?: 0
              }
            )
          }
        }
      }
    }
  }

  companion object {
    private var isPageInit = false
  }
}
