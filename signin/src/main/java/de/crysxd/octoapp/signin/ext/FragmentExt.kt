package de.crysxd.octoapp.signin.ext

import android.content.Intent
import android.net.Uri
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.base.feedback.SendFeedbackDialog
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity

fun Fragment.goBackToDiscover() {
    requireOctoActivity().showDialog(
        message = "Do you want to cancel and connect to an other OctoPrint? H",
        positiveButton = "Connect to other H",
        positiveAction = {
            val repo = de.crysxd.octoapp.base.di.Injector.get().octorPrintRepository()
            repo.getActiveInstanceSnapshot()?.let {
                // Case A: we got here because an API key is invalid. In this case we need to clear the active instance
                // and MainActivity will trigger the normal sign in flow
                repo.clearActive()
            }

            // Case B: Nothing is active, so we got here from the normal sign in flow (discover -> probe -> access). Pop all the way back to the start.
            findNavController().popBackStack(de.crysxd.octoapp.signin.R.id.discoverFragment, false)
        },
        negativeButton = "Cancel"
    )
}

fun Fragment.setUpAsHelpButton(view: View) {
    view.setOnClickListener {
        val uri = Uri.parse(Firebase.remoteConfig.getString("help_url_sign_in"))
        OctoAnalytics.logEvent(OctoAnalytics.Event.SignInHelpOpened)
        startActivity(Intent(Intent.ACTION_VIEW, uri))
    }
    view.setOnLongClickListener {
        SendFeedbackDialog().show(childFragmentManager, "feedback")
        true
    }
}