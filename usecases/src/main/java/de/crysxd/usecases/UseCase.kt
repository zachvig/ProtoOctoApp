package de.crysxd.usecases


interface UseCase<Param, Res> {

    suspend fun execute(param: Param): Res

}