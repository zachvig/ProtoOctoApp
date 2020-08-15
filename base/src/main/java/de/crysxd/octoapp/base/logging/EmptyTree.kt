package de.crysxd.octoapp.base.logging

import timber.log.Timber

object EmptyTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) = Unit
}