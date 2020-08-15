package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.base.logging.EmptyTree
import de.crysxd.octoapp.base.logging.TaggedTree
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.atomic.AtomicInteger


abstract class UseCase<Param, Res> {

    companion object {
        val executionCounter = AtomicInteger(0)
    }

    protected var suppressLogging = false

    suspend fun execute(param: Param): Res = withContext(Dispatchers.Default) {
        val executionId = executionCounter.incrementAndGet()
        val start = System.currentTimeMillis()
        val name = this@UseCase::class.java.simpleName
        val tag = "UC/${name}/$executionId"
        val timber = if (suppressLogging) EmptyTree else TaggedTree(tag)

        try {
            timber.i("😶️ Executing with param=$param")
            val res = doExecute(param, timber)
            timber.i("🥳 Finished time=${System.currentTimeMillis() - start}ms res=$res")
            return@withContext res
        } catch (e: Exception) {
            timber.e("🤬 Failed time=${System.currentTimeMillis() - start}ms exception=${e::class.java.simpleName}")
            timber.e(e)
            throw e
        }
    }

    protected abstract suspend fun doExecute(param: Param, timber: Timber.Tree): Res

}

suspend fun <T> UseCase<Unit, T>.execute(): T = execute(Unit)