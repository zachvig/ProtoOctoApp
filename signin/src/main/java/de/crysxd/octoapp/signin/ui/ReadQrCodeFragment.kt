package de.crysxd.octoapp.signin.ui

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import de.crysxd.octoapp.signin.R
import kotlinx.android.synthetic.main.fragment_read_qr_code.*

class ReadQrCodeFragment : Fragment(R.layout.fragment_read_qr_code) {

    companion object {
        const val RESULT_API_KEY = "api_key"
        private const val REQUEST_CODE_PERMISSION = 100
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        scannerView.setResultHandler {
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
    }
}