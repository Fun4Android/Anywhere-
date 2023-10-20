package com.absinthe.anywhere_.model.manager

import android.content.Context
import android.content.Intent
import android.view.accessibility.AccessibilityManager
import com.absinthe.anywhere_.R
import com.absinthe.anywhere_.a11y.A11yActionBean
import com.absinthe.anywhere_.a11y.A11yEntity
import com.absinthe.anywhere_.a11y.A11yType
import com.absinthe.anywhere_.constants.AnywhereType
import com.absinthe.anywhere_.constants.Const
import com.absinthe.anywhere_.listener.OnQRLaunchedListener
import com.absinthe.anywhere_.model.QREntity
import com.absinthe.anywhere_.model.database.AnywhereEntity
import com.absinthe.anywhere_.model.viewholder.FlowStepBean
import com.absinthe.anywhere_.utils.handler.Opener
import com.absinthe.anywhere_.utils.handler.URLSchemeHandler
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.Utils
import com.google.gson.Gson
import java.lang.ref.WeakReference

object QRCollection {

  val entitySet = mutableSetOf<AnywhereEntity>()
  private val context = WeakReference<Context>(Utils.getApp())
  private val accessibilityManager: AccessibilityManager
  private val map: HashMap<String, QREntity>

  private const val wechatScanId = "wechatScan"
  private const val wechatPayId = "wechatPay"
  private const val wechatPayAcsId = "wechatPayAcs"
  private const val wechatCollectId = "wechatCollect"
  private const val wechatCollectAcsId = "wechatCollectAcs"
  private const val wechatMyQrCodeId = "wechatMyQrCode"
  private const val alipayScanId = "alipayScan"
  private const val alipayPayId = "alipayPay"
  private const val alipayBusId = "alipayBus"
  private const val alipayCollectId = "alipayCollect"
  private const val qqScanId = "qqScan"
  private const val unionpayPayId = "unionpayPay"
  private const val unionpayCollectId = "unionpayCollect"
  private const val unionpayScanId = "unionpayScan"
  private const val unionpayBusId = "unionpayBus"

  init {
    accessibilityManager =
      getContext().getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager

    map = HashMap()
    map[wechatScanId] = wechatScan
    map[wechatPayId] = wechatPay
    map[wechatPayAcsId] = wechatPay
    map[wechatCollectId] = wechatCollect
    map[wechatCollectAcsId] = wechatCollectAcs
    map[wechatMyQrCodeId] = wechatMyQrCode
    map[alipayScanId] = alipayScan
    map[alipayPayId] = alipayPay
    map[alipayBusId] = alipayBus
    map[alipayCollectId] = alipayCollect
    map[qqScanId] = qqScan
    map[unionpayPayId] = unionpayPay
    map[unionpayCollectId] = unionpayCollect
    map[unionpayScanId] = unionpayScan
    map[unionpayBusId] = unionpayBus
  }

  fun getQREntity(id: String): QREntity? {
    return map[id]
  }

  private fun getContext(): Context {
    return context.get()!!
  }

  /**
   * Wechat scan page
   */
  private val wechatScan: QREntity
    get() {
      val pkgName = "com.tencent.mm"
      entitySet.add(AnywhereEntity().apply {
        id = wechatScanId
        appName = "微信扫码"
        param1 = pkgName
        description = getContext().getString(R.string.desc_work_at_any_mode)
        type = AnywhereType.Card.QR_CODE
      })
      return QREntity(object : OnQRLaunchedListener {
        override fun onLaunched() {
          try {
            val intent = getContext().packageManager.getLaunchIntentForPackage(pkgName)
            if (intent != null) {
              intent.putExtra("LauncherUI.From.Scaner.Shortcut", true)
              intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
              getContext().startActivity(intent)
            }
          } catch (e: Exception) {
            e.printStackTrace()
          }
        }
      }).apply {
        this.pkgName = pkgName
      }
    }

  /**
   * Wechat pay page
   */
  private val wechatPay: QREntity
    get() {
      val pkgName = "com.tencent.mm"
      entitySet.add(AnywhereEntity().apply {
        id = wechatPayId
        appName = "微信付款码"
        param1 = pkgName
        description = getContext().getString(R.string.desc_work_at_any_mode)
        type = AnywhereType.Card.QR_CODE
      })
      return QREntity(object : OnQRLaunchedListener {
        override fun onLaunched() {
          try {
            val intent = Intent("com.tencent.mm.action.BIZSHORTCUT").apply {
              putExtra("LauncherUI.Shortcut.LaunchType", "launch_type_offline_wallet")
              flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            getContext().startActivity(intent)
          } catch (e: Exception) {
            e.printStackTrace()
          }
        }
      }).apply {
        this.pkgName = pkgName
      }
    }

  /**
   * Wechat collect page
   */
  private val wechatCollect: QREntity
    get() {
      val pkgName = "com.tencent.mm"
      val clsName = ".plugin.collect.ui.CollectMainUI"
      val cmd = String.format(Const.CMD_OPEN_ACTIVITY_FORMAT, pkgName, pkgName + clsName)
      entitySet.add(AnywhereEntity().apply {
        id = wechatCollectId
        appName = "微信收款码"
        param1 = pkgName
        param2 = clsName
        description = getContext().getString(R.string.desc_need_root)
        type = AnywhereType.Card.QR_CODE
      })
      return QREntity(object : OnQRLaunchedListener {
        override fun onLaunched() {
          Opener.with(getContext()).load(cmd).openWithPackageName(pkgName)
        }
      }).apply {
        this.pkgName = pkgName
        this.clsName = clsName
      }
    }

  /**
   * Wechat collect page Accessibility
   */
  private val wechatCollectAcs: QREntity
    get() {
      val pkgName = "com.tencent.mm"
      val clsName = "com.tencent.mm.ui.LauncherUI"
      entitySet.add(AnywhereEntity().apply {
        id = wechatCollectAcsId
        appName = "微信收款码"
        param1 = pkgName
        param2 = clsName
        description = getContext().getString(R.string.desc_need_accessibility)
        type = AnywhereType.Card.QR_CODE
      })
      return QREntity(object : OnQRLaunchedListener {
        override fun onLaunched() {
          val gson = Gson()
          val a11yEntity = A11yEntity().apply {
            applicationId = pkgName
            entryActivity = clsName
            actions = listOf(
              A11yActionBean(
                A11yType.TEXT,
                content = "二维码收款|Receive Money",
                activityId = "com.tencent.mm.plugin.offline.ui.WalletOfflineCoinPurseUI",
                delay = 0L
              ),
            )
          }
          val ae = AnywhereEntity(
            type = AnywhereType.Card.WORKFLOW,
            param1 = gson.toJson(
              listOf(
                FlowStepBean(
                  entity = AnywhereEntity(
                    type = AnywhereType.Card.URL_SCHEME,
                    param1 = "android-app://com.tencent.mm/#Intent;action=com.tencent.mm.action.BIZSHORTCUT;launchFlags=0x4000000;S.LauncherUI.Shortcut.LaunchType=launch_type_offline_wallet;end\'"
                  ),
                  delay = 0
                ),
                FlowStepBean(
                  entity = AnywhereEntity(
                    type = AnywhereType.Card.ACCESSIBILITY,
                    param1 = gson.toJson(a11yEntity)
                  ),
                  delay = 1500
                )
              )
            )
          )

          Opener.with(getContext()).load(ae).open()
        }
      }).apply {
        this.pkgName = pkgName
        this.clsName = clsName
      }
    }

  /**
   * Wechat my qrcode page
   */
  private val wechatMyQrCode: QREntity
    get() {
      val pkgName = "com.tencent.mm"
      entitySet.add(AnywhereEntity().apply {
        id = wechatMyQrCodeId
        appName = "微信二维码名片"
        param1 = pkgName
        description = getContext().getString(R.string.desc_work_at_any_mode)
        type = AnywhereType.Card.QR_CODE
      })
      return QREntity(object : OnQRLaunchedListener {
        override fun onLaunched() {
          try {
            val intent = Intent("com.tencent.mm.action.BIZSHORTCUT").apply {
              putExtra("LauncherUI.Shortcut.LaunchType", "launch_type_my_qrcode")
              flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            getContext().startActivity(intent)
          } catch (e: Exception) {
            e.printStackTrace()
          }
        }
      }).apply {
        this.pkgName = pkgName
      }
    }

  /**
   * Alipay scan page
   */
  private val alipayScan: QREntity
    get() {
      val urlScheme = "alipayqr://platformapi/startapp?saId=10000007"
      val pkgName = "com.eg.android.AlipayGphone"
      entitySet.add(AnywhereEntity().apply {
        id = alipayScanId
        appName = "支付宝扫码"
        param1 = pkgName
        param3 = urlScheme
        description = getContext().getString(R.string.desc_work_at_any_mode)
        type = AnywhereType.Card.QR_CODE
      })
      return QREntity(object : OnQRLaunchedListener {
        override fun onLaunched() {
          try {
            URLSchemeHandler.parse(getContext(), urlScheme, pkgName)
          } catch (e: java.lang.Exception) {
            e.printStackTrace()
          }
        }
      }).apply {
        this.urlScheme = urlScheme
      }
    }

  /**
   * Alipay pay page
   */
  private val alipayPay: QREntity
    get() {
      val urlScheme = "alipays://platformapi/startapp?appId=20000056"
      val pkgName = "com.eg.android.AlipayGphone"
      entitySet.add(AnywhereEntity().apply {
        id = alipayPayId
        appName = "支付宝付款码"
        param1 = pkgName
        param3 = urlScheme
        description = getContext().getString(R.string.desc_work_at_any_mode)
        type = AnywhereType.Card.QR_CODE
      })
      return QREntity(object : OnQRLaunchedListener {
        override fun onLaunched() {
          try {
            URLSchemeHandler.parse(getContext(), urlScheme, pkgName)
          } catch (e: java.lang.Exception) {
            e.printStackTrace()
          }
        }
      }).apply {
        this.urlScheme = urlScheme
      }
    }


  /**
   * Alipay bus page
   */
  private val alipayBus: QREntity
    get() {
      val urlScheme = "alipayqr://platformapi/startapp?saId=200011235"
      val pkgName = "com.eg.android.AlipayGphone"
      entitySet.add(AnywhereEntity().apply {
        id = alipayBusId
        appName = "支付宝公交码"
        param1 = pkgName
        param3 = urlScheme
        description = getContext().getString(R.string.desc_work_at_any_mode)
        type = AnywhereType.Card.QR_CODE
      })
      return QREntity(object : OnQRLaunchedListener {
        override fun onLaunched() {
          try {
            URLSchemeHandler.parse(getContext(), urlScheme, pkgName)
          } catch (e: java.lang.Exception) {
            e.printStackTrace()
          }
        }
      }).apply {
        this.urlScheme = urlScheme
      }
    }

  /**
   * Alipay collect page
   */
  private val alipayCollect: QREntity
    get() {
      val urlScheme = "alipays://platformapi/startapp?appId=20000123"
      val pkgName = "com.eg.android.AlipayGphone"
      entitySet.add(AnywhereEntity().apply {
        id = alipayCollectId
        appName = "支付宝收款码"
        param1 = pkgName
        param3 = urlScheme
        description = getContext().getString(R.string.desc_work_at_any_mode)
        type = AnywhereType.Card.QR_CODE
      })
      return QREntity(object : OnQRLaunchedListener {
        override fun onLaunched() {
          try {
            URLSchemeHandler.parse(getContext(), urlScheme, pkgName)
          } catch (e: java.lang.Exception) {
            e.printStackTrace()
          }
        }
      }).apply {
        this.urlScheme = urlScheme
      }
    }

  /**
   * QQ scan page
   */
  private val qqScan: QREntity
    get() {
      val pkgName = "com.tencent.mobileqq"
      val clsName = "com.tencent.mobileqq.olympic.activity.ScanTorchActivity"
      val cmd = String.format(Const.CMD_OPEN_ACTIVITY_FORMAT, pkgName, clsName)
      entitySet.add(AnywhereEntity().apply {
        id = qqScanId
        appName = "QQ 扫码"
        param1 = pkgName
        param2 = clsName
        description = getContext().getString(R.string.desc_need_root)
        type = AnywhereType.Card.QR_CODE
      })
      return QREntity(object : OnQRLaunchedListener {
        override fun onLaunched() {
          Opener.with(getContext()).load(cmd).open()
        }
      }).apply {
        this.pkgName = pkgName
        this.clsName = clsName
      }
    }

  private fun genUnionPay(id: String, text: String): QREntity {
    val pkgName = "com.unionpay"
    entitySet.add(AnywhereEntity().apply {
      this.id = id
      appName = "云闪付${text}"
      param1 = pkgName
      description = getContext().getString(R.string.desc_work_at_any_mode)
      type = AnywhereType.Card.QR_CODE
    })
    val urlScheme = when (text) {
      "乘车码" -> "upwallet://rn/rnhtmlridingcode"
      "付款码" -> "upwallet://native/codepay"
      "收款码" -> "upwallet://native/codecollect"
      "扫一扫" -> "upwallet://native/scanCode"
      else -> ""
    }
    return QREntity(object : OnQRLaunchedListener {
      override fun onLaunched() {
        val entity = AnywhereEntity().apply {
          type = AnywhereType.Card.URL_SCHEME
          param1 = urlScheme
          param2 = pkgName
        }
        Opener.with(getContext()).load(entity).open()
      }
    }).apply {
      this.pkgName = pkgName
    }
  }

  /**
   * UnionPay pay page
   */
  private val unionpayPay: QREntity
    get() = genUnionPay(unionpayPayId, "付款码")

  /**
   * UnionPay bus page
   */
  private val unionpayBus: QREntity
    get() = genUnionPay(unionpayBusId, "乘车码")

  /**
   * UnionPay scan page
   */
  private val unionpayScan: QREntity
    get() = genUnionPay(unionpayScanId, "扫一扫")

  /**
   * UnionPay collect page
   */
  private val unionpayCollect: QREntity
    get() {
      val pkgName = "com.unionpay"
      val clsName = "com.unionpay.activity.payment.UPActivityPaymentQrCodeOut"
      entitySet.add(AnywhereEntity().apply {
        id = unionpayCollectId
        appName = "云闪付收款码"
        param1 = pkgName
        description = getContext().getString(R.string.desc_need_accessibility)
        type = AnywhereType.Card.QR_CODE
      })
      return QREntity(object : OnQRLaunchedListener {
        override fun onLaunched() {
          val a11yEntity = A11yEntity().apply {
            applicationId = pkgName
            entryActivity = clsName
            actions = listOf(
              A11yActionBean(A11yType.TEXT, content = "收款", activityId = "", delay = 0L)
            )
          }
          val flowStepList = listOf(
            FlowStepBean(
              entity = AnywhereEntity().apply {
                type = AnywhereType.Card.URL_SCHEME
                param1 = "upwallet://native/codepay"
                param2 = pkgName
              },
              delay = 500L
            ),
            FlowStepBean(
              entity = AnywhereEntity().apply {
                type = AnywhereType.Card.ACCESSIBILITY
                param1 = GsonUtils.toJson(a11yEntity)
              },
              delay = 0L
            )
          )
          val entity = AnywhereEntity().apply {
            type = AnywhereType.Card.WORKFLOW
            param1 = Gson().toJson(flowStepList)
          }
          Opener.with(getContext()).load(entity).open()
        }
      }).apply {
        this.pkgName = pkgName
      }
    }

}
