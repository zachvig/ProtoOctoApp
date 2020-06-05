package de.crysxd.octoapp.base.ui.temperature

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText

fun EditText.requestFocusAndOpenSoftKeyboard() {
    requestFocus()
    (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).toggleSoftInput(
        InputMethodManager.SHOW_FORCED,
        InputMethodManager.HIDE_IMPLICIT_ONLY
    )
}

fun View.clearFocusAndHideSoftKeyboard() {
    clearFocus()
    (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(windowToken, 0)
}