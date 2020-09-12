package de.crysxd.octoapp.octoprint.exceptions

class WebSocketZombieException : Exception("Web socket was closed, but still received message")