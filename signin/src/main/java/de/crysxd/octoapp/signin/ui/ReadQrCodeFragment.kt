package de.crysxd.octoapp.signin.ui

import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.base.ui.base.InsetAwareScreen
import de.crysxd.octoapp.signin.R
import de.crysxd.octoapp.signin.databinding.ReadQrCodeFragmentBinding

class ReadQrCodeFragment : Fragment(), InsetAwareScreen {

    private lateinit var binding: ReadQrCodeFragmentBinding
    private var systemUiVisibilityBackup = 0
    private var navigationBarColorBackup = Color.WHITE

    companion object {
        const val RESULT_API_KEY = "api_key"
        private const val REQUEST_CODE_PERMISSION = 100
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        ReadQrCodeFragmentBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.scannerView.setResultHandler {
            OctoAnalytics.logEvent(OctoAnalytics.Event.QrCodeCompleted)
            findNavController().previousBackStackEntry?.savedStateHandle?.set(RESULT_API_KEY, it.text)
            findNavController().popBackStack()
        }

        binding.buttonCancel.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    override fun onStart() {
        super.onStart()

        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            binding.scannerView?.postDelayed({
                binding.scannerView?.startCamera()
            }, 300)
        } else {
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), REQUEST_CODE_PERMISSION)
        }

        requireActivity().window.let {
            it.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            it.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION, WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)

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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                binding.scannerView.startCamera()
            } else {
                findNavController().popBackStack(R.id.loginFragment, false)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        binding.scannerView.stopCamera()

        requireActivity().window.let {
            it.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            it.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
            it.decorView.systemUiVisibility = systemUiVisibilityBackup
            it.navigationBarColor = navigationBarColorBackup
        }
    }

    override fun handleInsets(insets: Rect) {
        val margin2 = requireContext().resources.getDimension(R.dimen.margin_2).toInt()
        ConstraintSet().apply {
            clone(binding.constraintLayout)
            connect(
                R.id.buttonCancel,
                ConstraintSet.BOTTOM,
                ConstraintSet.PARENT_ID,
                ConstraintSet.BOTTOM,
                insets.bottom + margin2
            )
            connect(
                R.id.octoView,
                ConstraintSet.TOP,
                ConstraintSet.PARENT_ID,
                ConstraintSet.TOP,
                insets.bottom
            )
        }.applyTo(binding.constraintLayout)
    }
}