package de.crysxd.baseui.ext

import android.content.Context
import android.view.inputmethod.InputMethodManager
import android.widget.EditText

fun EditText.requestFocusAndOpenSoftKeyboard() {
    requestFocus()
    (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).toggleSoftInput(
        InputMethodManager.SHOW_IMPLICIT,
        InputMethodManager.HIDE_IMPLICIT_ONLY
    )
}

fun EditText.clearFocusAndHideSoftKeyboard() {
    clearFocus()
    (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(windowToken, 0)
}