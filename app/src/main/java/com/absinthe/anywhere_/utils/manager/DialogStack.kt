package com.absinthe.anywhere_.utils.manager

import android.app.Dialog
import androidx.fragment.app.DialogFragment
import com.absinthe.anywhere_.BuildConfig
import com.absinthe.anywhere_.view.app.AnywhereBottomSheetDialog
import timber.log.Timber
import java.util.Stack

/**
 * Created by Absinthe at 2020/1/13
 *
 *
 * Dialog Stack
 *
 *
 * Make it display unique Dialog at the same time
 * to make app delegate.
 */
object DialogStack {

  private val stack: Stack<Any> = Stack()
  private val isPrintStack = BuildConfig.DEBUG

  fun push(dialog: Any) {
    printStack()
    Timber.i("Push Start")

    if (dialog !is Dialog && dialog !is DialogFragment) {
      return
    }

    if (stack.empty()) {
      stack.push(dialog)
    } else {
      when (val peekObject = stack.peek()) {
        is AnywhereBottomSheetDialog -> {
          peekObject.hide()
        }
        is Dialog -> {
          peekObject.hide()
        }
        is DialogFragment -> {
          peekObject.dialog?.hide()
        }
      }
      stack.push(dialog)
    }

    dialog.apply {
      if (this is AnywhereBottomSheetDialog) {
        isPush = true
        show()
      } else if (this is Dialog) {
        show()
      }
    }

    Timber.i("Push End")
    printStack()
  }

  fun pop() {
    printStack()
    Timber.i("Pop Start")

    if (stack.empty()) {
      return
    }

    stack.peek()?.let {
      try {
        if (it is Dialog) {
          it.dismiss()
        } else if (it is DialogFragment) {
          it.dismiss()
        }
        stack.pop()

        if (stack.isNotEmpty()) {
          stack.peek()?.let { peek ->
            if (peek is Dialog) {
              peek.show()
            } else if (peek is DialogFragment) {
              peek.dialog?.show()
            }
          } ?: let {
            stack.pop()
          }
        }
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }

    Timber.i("Pop End")
    printStack()
  }

  private fun printStack() {
    if (isPrintStack) {
      Timber.i("DialogStack:")

      for (obj in stack) {
        Timber.i(obj.javaClass.toString())
      }
      Timber.i("--------------------------------------")
    }
  }
}
