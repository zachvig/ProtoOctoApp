package de.crysxd.octoapp.base.logging

import java.util.logging.Level
import java.util.logging.Logger

class TimberLogger(val logger: Logger = Logger.getAnonymousLogger()) {

    init {
        logger.handlers.toList().forEach { logger.removeHandler(it) }
        logger.handlers.forEach { logger.removeHandler(it) }
        logger.addHandler(TimberHandler())
        logger.level = Level.ALL
        logger.useParentHandlers = false
    }
}

