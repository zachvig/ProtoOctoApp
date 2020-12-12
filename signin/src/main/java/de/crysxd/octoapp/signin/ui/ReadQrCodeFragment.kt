package de.crysxd.octoapp.signin.ui

import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.signin.R
import kotlinx.android.synthetic.main.fragment_read_qr_code.*

class ReadQrCodeFragment : Fragment(R.layout.fragment_read_qr_code) {

    private var systemUiVisibilityBackup = 0
    private var navigationBarColorBackup = Color.WHITE

    companion object {
        const val RESULT_API_KEY = "api_key"
        private const val REQUEST_CODE_PERMISSION = 100
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        scannerView.setResultHandler {
            OctoAnalytics.logEvent(OctoAnalytics.Event.QrCodeCompleted)
            findNavController().previousBackStackEntry?.savedStateHandle?.set(RESULT_API_KEY, it.text)
            findNavController().popBackStack()
        }

        buttonCancel.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    override fun onResume() {
        super.onResume()

        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            scannerView.postDelayed({
                scannerView.startCamera()
            }, 300)
        } else {
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), 1)
        }

        requireActivity().window.let {
            it.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            it.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION, WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                applyInsets()
            }

            navigationBarColorBackup = it.navigationBarColor
            systemUiVisibilityBackup = it.decorView.systemUiVisibility
            it.navigationBarColor = Color.BLACK

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                it.decorView.systemUiVisibility = it.decorView.systemUiVisibility xor View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    it.decorView.systemUiVisibility = it.decorView.systemUiVisibility xor View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun applyInsets() {
        lifecycleScope.launchWhenCreated {
            requireActivity().window.let {
                val margin2 = requireContext().resources.getDimension(R.dimen.margin_2).toInt()
                ConstraintSet().apply {
                    clone(constraintLayout)
                    connect(
                        R.id.buttonCancel,
                        ConstraintSet.BOTTOM,
                        ConstraintSet.PARENT_ID,
                        ConstraintSet.BOTTOM,
                        it.decorView.rootWindowInsets.stableInsetBottom + margin2
                    )
                    connect(
                        R.id.octoView,
                        ConstraintSet.TOP,
                        ConstraintSet.PARENT_ID,
                        ConstraintSet.TOP,
                        it.decorView.rootWindowInsets.stableInsetBottom
                    )
                }.applyTo(constraintLayout)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                scannerView.startCamera()
            } else {
                findNavController().popBackStack()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        scannerView.stopCamera()

        requireActivity().window.let {
            it.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            it.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
            it.decorView.systemUiVisibility = systemUiVisibilityBackup
            it.navigationBarColor = navigationBarColorBackup
        }
    }
}