package de.crysxd.octoapp.filemanager.upload

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import de.crysxd.baseui.utils.NavigationResultMediator
import kotlinx.coroutines.delay
import timber.log.Timber

class PickFileForUploadFragment : Fragment() {

    private lateinit var launcher: ActivityResultLauncher<String>
    private var started = false
    private val args by navArgs<PickFileForUploadFragmentArgs>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = View(context)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launcher = registerForActivityResult(ActivityResultContracts.GetContent()) {
            Timber.i("Got file for upload: $it")
            NavigationResultMediator.postResult(
                resultId = args.resultId,
                result = it
            )
        }
    }

    override fun onStart() {
        super.onStart()
        if (!started) {
            Timber.i("Picking file for upload")
            started = true
            launcher.launch("*/*")
        } else {
            lifecycleScope.launchWhenCreated {
                // Delay a bit to ensure we got the result first
                delay(50)
                Timber.i("User returned, ending file selection")
                NavigationResultMediator.postResult(args.resultId, null)
                findNavController().popBackStack()
            }
        }
    }
}