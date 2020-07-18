package de.crysxd.octoapp.base.ui

import android.view.WindowInsets
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class WindowInsetsViewModel : ViewModel() {

    var systemInsets = MutableLiveData<WindowInsets>()

}