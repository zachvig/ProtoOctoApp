package de.crysxd.octoapp.base.datasource

interface DataSource<T> {

    fun store(t: T?)

    fun get(): T?

}