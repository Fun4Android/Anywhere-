package com.absinthe.anywhere_.viewbuilder.entity

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.absinthe.anywhere_.R
import com.absinthe.anywhere_.model.GlobalValues
import com.absinthe.anywhere_.viewbuilder.ViewBuilder

class CollectorBuilder(context: Context, viewGroup: ViewGroup) : ViewBuilder(context, viewGroup) {

    lateinit var ibCollector: ImageButton
    lateinit var tvPkgName: TextView
    lateinit var tvClsName: TextView

    override fun init() {
        val wrapWrap = Params.LL.WRAP_WRAP.apply {
            gravity = Gravity.CENTER_HORIZONTAL
        }
        (root as LinearLayout).apply {
            layoutParams = wrapWrap
            orientation = LinearLayout.VERTICAL
        }

        ibCollector = ImageButton(mContext).apply {
            layoutParams = LinearLayout.LayoutParams(65.dp, 65.dp).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
            setImageResource(R.drawable.btn_collector)
            background = null
        }
        addView(ibCollector)

        if (GlobalValues.sIsCollectorPlus) {
            val infoLayout = LinearLayout(mContext).apply {
                layoutParams = wrapWrap
                orientation = LinearLayout.VERTICAL

                val padding = 5.dp
                setPadding(padding, padding, padding, padding)
                background = ContextCompat.getDrawable(mContext, R.drawable.bg_collector_info)
            }

            tvPkgName = TextView(mContext).apply {
                layoutParams = wrapWrap
                setTextColor(Color.WHITE)
                textSize = 15f
            }
            infoLayout.addView(tvPkgName)

            tvClsName = TextView(mContext).apply {
                layoutParams = wrapWrap
                setTextColor(Color.WHITE)
                textSize = 15f
            }
            infoLayout.addView(tvClsName)

            addView(infoLayout)
        }
    }
}