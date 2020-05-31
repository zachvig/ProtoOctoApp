package de.crysxd.octoapp.base.usecase


interface UseCase<Param, Res> {

    suspend fun execute(param: Param): Res

}