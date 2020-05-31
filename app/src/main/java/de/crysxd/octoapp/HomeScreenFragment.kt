package de.crysxd.octoapp

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import de.crysxd.octoapp.signin.di.Injector
import kotlinx.android.synthetic.main.fragment_home.*

class HomeScreenFragment : Fragment(R.layout.fragment_home) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        button.setOnClickListener {
            Injector.get().octoprintRepository().clearOctoprintInstanceInformation()
        }
    }
}