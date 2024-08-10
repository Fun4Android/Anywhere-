package com.absinthe.anywhere_.ui.editor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import com.absinthe.anywhere_.model.database.AnywhereEntity
import com.absinthe.anywhere_.ui.dialog.EXTRA_FROM_WORKFLOW

abstract class BaseEditorFragment : Fragment(), IEditor {

  override var execWithRoot: Boolean = false
  protected val item by lazy {
    arguments?.let { BundleCompat.getParcelable(it, EXTRA_ENTITY, AnywhereEntity::class.java) } ?: AnywhereEntity()
  }
  protected val isEditMode by lazy { requireArguments().getBoolean(EXTRA_EDIT_MODE) }
  protected val isFromWorkflow by lazy { requireArguments().getBoolean(EXTRA_FROM_WORKFLOW) }
  protected var doneItem: AnywhereEntity = AnywhereEntity()

  protected abstract fun setBinding(inflater: LayoutInflater, container: ViewGroup?): View
  protected abstract fun initView()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val root = setBinding(inflater, container)
    initView()
    return root
  }

  override fun doneEdit(): Boolean {
    if (isFromWorkflow) {
      EditorActivity.workflowResultItem.value = doneItem
    }
    return isFromWorkflow
  }
}
