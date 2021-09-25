package de.crysxd.octoapp.base.data.source

interface DataSource<T> {

    fun store(t: T?)

    fun get(): T?

}