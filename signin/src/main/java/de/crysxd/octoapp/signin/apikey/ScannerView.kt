package de.crysxd.octoapp.signin.apikey

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import me.dm7.barcodescanner.core.ViewFinderView
import me.dm7.barcodescanner.zxing.ZXingScannerView

class ScannerView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : ZXingScannerView(context, attrs) {

    override fun createViewFinderView(context: Context) = ViewFinderView(context).also {
        it.setBorderColor(Color.WHITE)
        it.setLaserColor(Color.TRANSPARENT)
    }
}