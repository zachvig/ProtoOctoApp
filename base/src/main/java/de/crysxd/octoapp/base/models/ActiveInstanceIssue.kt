package de.crysxd.octoapp.base.models

import de.crysxd.octoapp.base.R

// Use enum class instead of sealed class for easy serialization with gson
enum class ActiveInstanceIssue {
    INVALID_API_KEY,
    HTTP_ISSUE,
    BASIC_AUTH_REQUIRED;

    val messageRes: Int
        get() = when (this) {
            INVALID_API_KEY -> R.string.signin___broken_setup___api_key_revoked
            HTTP_ISSUE -> R.string.signin___broken_setup___https_issue
            BASIC_AUTH_REQUIRED -> R.string.signin___broken_setup___basic_auth_required
        }
}