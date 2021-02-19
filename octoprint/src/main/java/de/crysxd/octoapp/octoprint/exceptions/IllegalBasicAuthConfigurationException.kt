package de.crysxd.octoapp.octoprint.exceptions

class IllegalBasicAuthConfigurationException(url: String) : OctoPrintException(
    message = ProxyException.mask("Illegal basic auth setup in $url", url),
    userFacingMessage = "<b>$url</b>\n\ncontains a illegal Basic Auth setup. Please make sure to follow the scheme \n\nhttp(s)://<b>username:password</b>@host"
)