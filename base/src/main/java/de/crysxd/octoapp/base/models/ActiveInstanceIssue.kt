package de.crysxd.octoapp.base.models

import androidx.annotation.StringRes
import de.crysxd.octoapp.base.R

sealed class ActiveInstanceIssue(@StringRes val messageRes: Int) {
    object InvalidApiKey : ActiveInstanceIssue(R.string.signin___broken_setup___api_key_revoked)
    object HttpsIssue : ActiveInstanceIssue(R.string.signin___broken_setup___https_issue)
    object BasicAuthRequired : ActiveInstanceIssue(R.string.signin___broken_setup___basic_auth_required)
}