package de.crysxd.octoapp.signin.probe

import android.content.Context
import de.crysxd.octoapp.base.usecase.TestFullNetworkStackUseCase
import de.crysxd.octoapp.signin.R

class FindingDescriptionLibrary(private val context: Context) {

    fun getTitleForFinding(finding: TestFullNetworkStackUseCase.Finding) = when (finding) {
        is TestFullNetworkStackUseCase.Finding.BasicAuthRequired -> context.getString(R.string.sign_in___probe_finding___title_basic_auth)
        is TestFullNetworkStackUseCase.Finding.DnsFailure -> context.getString(R.string.sign_in___probe_finding___title_dns_failure, finding.host)
        is TestFullNetworkStackUseCase.Finding.LocalDnsFailure -> context.getString(R.string.sign_in___probe_finding___title_local_dns_failure, finding.host)
        is TestFullNetworkStackUseCase.Finding.HostNotReachable -> context.getString(R.string.sign_in___probe_finding___title_host_unreachable, finding.host)
        is TestFullNetworkStackUseCase.Finding.HttpsNotTrusted -> context.getString(R.string.sign_in___probe_finding___title_https_not_trusted, finding.host)
        is TestFullNetworkStackUseCase.Finding.InvalidUrl -> context.getString(R.string.sign_in___probe_finding___title_url_syntax)
        is TestFullNetworkStackUseCase.Finding.NotFound -> context.getString(R.string.sign_in___probe_finding___title_octoprint_not_found)
        is TestFullNetworkStackUseCase.Finding.PortClosed -> context.getString(R.string.sign_in___probe_finding___title_port_closed, finding.host, finding.port)
        is TestFullNetworkStackUseCase.Finding.UnexpectedHttpIssue -> context.getString(R.string.sign_in___probe_finding___title_failed_to_connect_via_http)
        is TestFullNetworkStackUseCase.Finding.UnexpectedIssue -> context.getString(R.string.sign_in___probe_finding___title_unexpected_issue)
        is TestFullNetworkStackUseCase.Finding.ServerIsNotOctoPrint -> context.getString(R.string.sign_in___probe_finding___title_might_not_be_octoprint, finding.host)
        is TestFullNetworkStackUseCase.Finding.InvalidApiKey -> "" // Never shown
        is TestFullNetworkStackUseCase.Finding.OctoPrintReady -> "" // Never shown
        is TestFullNetworkStackUseCase.Finding.WebSocketUpgradeFailed -> context.getString(
            R.string.sign_in___probe_finding___title_websocket_upgrade_failed,
            finding.host
        )
        is TestFullNetworkStackUseCase.Finding.EmptyUrl -> context.getString(R.string.sign_in___probe_finding___title_url_syntax)
        is TestFullNetworkStackUseCase.Finding.NoImage -> "" // Never shown
        is TestFullNetworkStackUseCase.Finding.WebcamReady -> "" // Never shown
    }

    fun getExplainerForFinding(finding: TestFullNetworkStackUseCase.Finding) = when (finding) {
        is TestFullNetworkStackUseCase.Finding.BasicAuthRequired -> context.getString(R.string.sign_in___probe_finding___explainer_basic_auth)
        is TestFullNetworkStackUseCase.Finding.DnsFailure -> context.getString(
            R.string.sign_in___probe_finding___explainer_dns_failure,
            finding.host,
            finding.webUrl
        )
        is TestFullNetworkStackUseCase.Finding.LocalDnsFailure -> context.getString(
            R.string.sign_in___probe_finding___explainer_local_dns_failure,
            finding.host,
            finding.webUrl
        )
        is TestFullNetworkStackUseCase.Finding.HostNotReachable -> context.getString(
            R.string.sign_in___probe_finding___explainer_host_unreachable,
            finding.host,
            finding.ip,
            finding.webUrl
        )
        is TestFullNetworkStackUseCase.Finding.HttpsNotTrusted -> if (finding.weakHostnameVerificationRequired) {
            context.getString(R.string.sign_in___probe_finding___explainer_https_not_trusted_weak_hostname_verification)
        } else {
            context.getString(R.string.sign_in___probe_finding___explainer_https_not_trusted)
        }
        is TestFullNetworkStackUseCase.Finding.InvalidUrl -> context.getString(
            R.string.sign_in___probe_finding___explainer_url_syntax,
            finding.webUrl,
            finding.exception.localizedMessage ?: "Unknown error"
        )
        is TestFullNetworkStackUseCase.Finding.NotFound -> context.getString(
            R.string.sign_in___probe_finding___explainer_octoprint_not_found,
            finding.webUrl
        )
        is TestFullNetworkStackUseCase.Finding.PortClosed -> context.getString(
            R.string.sign_in___probe_finding___explainer_port_closed,
            finding.host,
            finding.port,
            finding.webUrl
        )
        is TestFullNetworkStackUseCase.Finding.UnexpectedHttpIssue -> context.getString(
            R.string.sign_in___probe_finding___explainer_failed_to_connect_via_http,
            finding.host,
            finding.exception.localizedMessage ?: "Unknown error"
        )
        is TestFullNetworkStackUseCase.Finding.UnexpectedIssue -> context.getString(
            R.string.sign_in___probe_finding___explainer_unexpected_issue,
            finding.exception.localizedMessage ?: "Unknown error"
        )
        is TestFullNetworkStackUseCase.Finding.ServerIsNotOctoPrint -> context.getString(
            R.string.sign_in___probe_finding___explainer_might_not_be_octoprint,
            finding.host
        )
        is TestFullNetworkStackUseCase.Finding.InvalidApiKey -> "" // Never shown
        is TestFullNetworkStackUseCase.Finding.OctoPrintReady -> "" // Never shown
        is TestFullNetworkStackUseCase.Finding.WebSocketUpgradeFailed -> context.getString(
            R.string.sign_in___probe_finding___explainer_websocket_upgrade_failed,
            finding.host,
            finding.responseCode,
            finding.webSocketUrl
        )
        is TestFullNetworkStackUseCase.Finding.EmptyUrl -> context.getString(
            R.string.sign_in___probe_finding___explainer_url_syntax,
            finding.webUrl,
            "URL empty"
        )
        is TestFullNetworkStackUseCase.Finding.NoImage -> "" // Never shown
        is TestFullNetworkStackUseCase.Finding.WebcamReady -> "" // Never shown
    }
}