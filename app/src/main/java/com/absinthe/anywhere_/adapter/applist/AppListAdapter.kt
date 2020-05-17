package com.absinthe.anywhere_.adapter.applist

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.anywhere_.R
import com.absinthe.anywhere_.constants.AnywhereType
import com.absinthe.anywhere_.constants.Const
import com.absinthe.anywhere_.constants.EventTag
import com.absinthe.anywhere_.constants.GlobalValues
import com.absinthe.anywhere_.model.AnywhereEntity
import com.absinthe.anywhere_.model.AppListBean
import com.absinthe.anywhere_.model.Settings
import com.absinthe.anywhere_.ui.list.AppDetailActivity
import com.absinthe.anywhere_.utils.AppUtils
import com.absinthe.anywhere_.utils.TextUtils
import com.absinthe.anywhere_.utils.UiUtils
import com.absinthe.anywhere_.view.editor.AnywhereEditor
import com.microsoft.appcenter.analytics.Analytics
import java.util.*


class AppListAdapter(private val mContext: Context, mode: Int) : RecyclerView.Adapter<AppListAdapter.ViewHolder>(), Filterable {

    private var mList: List<AppListBean> = ArrayList()
    private var mTempList: MutableList<AppListBean> = ArrayList()
    private var mFilter: ListFilter = ListFilter()
    private val mMode: Int = mode
    private var mListener: OnItemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_app_list, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(mList[position])

        val item = mList[position]
        holder.clAppList.setOnClickListener {
            when (mMode) {
                MODE_APP_LIST -> {
                    val intent = Intent(mContext, AppDetailActivity::class.java).apply {
                        putExtra(Const.INTENT_EXTRA_APP_NAME, item.appName)
                        putExtra(Const.INTENT_EXTRA_PKG_NAME, item.packageName)
                    }
                    mContext.startActivity(intent)
                }
                MODE_APP_DETAIL -> {
                    var exported = 0
                    if (UiUtils.isActivityExported(mContext, ComponentName(item.packageName,
                                    item.className))) {
                        exported = 100
                    }
                    val ae = AnywhereEntity.Builder().apply {
                        appName = item.appName
                        param1 = item.packageName
                        param2 = item.className.trim { it <= ' ' }.replace(item.packageName, "")
                        type = AnywhereType.ACTIVITY + exported
                    }

                    val editor = AnywhereEditor(mContext)
                            .item(ae)
                            .isEditorMode(false)
                            .isShortcut(false)
                            .build()
                    editor.show()
                }
                MODE_ICON_PACK -> {
                    GlobalValues.iconPack = item.packageName
                    Settings.initIconPackManager()
                    AppUtils.restart()
                }
                MODE_CARD_LIST -> {
                    mListener?.onClick(mList[position], position)
                }
            }

            val properties: MutableMap<String, String> = HashMap()
            properties["AppName"] = item.appName
            properties["Package Name"] = item.packageName
            properties["Class Name"] = item.className

            Analytics.trackEvent(EventTag.ACTIVITY_LIST_ITEM, properties)
        }
    }

    override fun getItemCount(): Int {
        return mList.size
    }

    fun setList(list: MutableList<AppListBean>) {
        mList = list
        mTempList = list
        notifyDataSetChanged()
    }

    override fun getFilter(): Filter {
        return mFilter
    }

    fun setOnItemClickListener(listener: OnItemClickListener?) {
        mListener = listener
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val ivIcon: ImageView = itemView.findViewById(R.id.iv_app_icon)
        private val tvAppName: TextView = itemView.findViewById(R.id.tv_app_name)
        private val tvPkgName: TextView = itemView.findViewById(R.id.tv_pkg_name)
        val clAppList: ConstraintLayout = itemView.findViewById(R.id.cl_app_list)

        fun bind(item: AppListBean) {
            tvAppName.text = item.appName

            when (mMode) {
                MODE_APP_LIST -> {
                    ivIcon.setImageDrawable(item.icon)
                    tvPkgName.text = item.packageName
                }
                MODE_APP_DETAIL -> {
                    ivIcon.setImageDrawable(UiUtils.getActivityIcon(mContext, ComponentName(item.packageName, item.className)))
                    tvPkgName.text = item.className

                    if (item.appName.endsWith(" (Exported)")) {
                        itemView.rootView.setBackgroundColor(ContextCompat.getColor(mContext, R.color.exported_background))
                    } else {
                        itemView.rootView.setBackgroundColor(Color.TRANSPARENT)
                    }
                }
                MODE_CARD_LIST -> {
                    ivIcon.setImageDrawable(UiUtils.getAppIconByPackageName(mContext, item))
                    tvPkgName.text = item.className
                }
                MODE_ICON_PACK -> {
                    ivIcon.setImageDrawable(UiUtils.getAppIconByPackageName(mContext, item.packageName))
                    tvPkgName.text = item.packageName
                }
            }
        }
    }

    internal inner class ListFilter : Filter() {

        override fun performFiltering(constraint: CharSequence): FilterResults {
            var newList: MutableList<AppListBean> = ArrayList()

            if (constraint.toString().trim { it <= ' ' }.isNotEmpty()) {
                for (bean in mTempList) {
                    val content = bean.appName + bean.packageName + bean.className
                    if (TextUtils.containsIgnoreCase(content, constraint.toString())) {
                        newList.add(bean)
                    }
                }
            } else {
                newList = mTempList
            }

            return FilterResults().apply {
                count = newList.size
                values = newList
            }
        }

        override fun publishResults(constraint: CharSequence, results: FilterResults) {
            //这里对 number 进行过滤后重新赋值
            results.values?.let {
                mList = if (results.count == 0) {
                    ArrayList()
                } else {
                    it as List<AppListBean>
                }
                notifyDataSetChanged()
            }
        }
    }

    interface OnItemClickListener {
        fun onClick(bean: AppListBean, which: Int)
    }

    companion object {
        const val MODE_APP_LIST = 0
        const val MODE_APP_DETAIL = 1
        const val MODE_ICON_PACK = 2
        const val MODE_CARD_LIST = 3
    }
}