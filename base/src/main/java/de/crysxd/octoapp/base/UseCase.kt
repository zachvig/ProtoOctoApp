package de.crysxd.octoapp.base


interface UseCase<Param, Res> {

    suspend fun execute(param: Param): Res

}