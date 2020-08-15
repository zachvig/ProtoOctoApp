package de.crysxd.octoapp.base.logging

import timber.log.Timber

class TaggedTree(private val tag: String) : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        Timber.tag(this.tag).log(priority, t, message)
    }
}