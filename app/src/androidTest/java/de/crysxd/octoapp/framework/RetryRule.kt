package de.crysxd.octoapp.framework

import okhttp3.internal.closeQuietly
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.util.concurrent.atomic.AtomicInteger

class RetryRule(retries: Int = 2, private val activityScenarioRule: LazyActivityScenarioRule<*>? = null) : TestRule {
    private val retryCount: AtomicInteger = AtomicInteger(retries)

    override fun apply(base: Statement, description: Description) = object : Statement() {
        override fun evaluate() {
            while (retryCount.getAndDecrement() > 0) {
                try {
                    base.evaluate()
                } catch (t: Throwable) {
                    if (retryCount.get() > 0) {
                        activityScenarioRule?.getScenario()?.closeQuietly()
//                        activityScenarioRule.la
                        System.err.println(
                            description.displayName +
                                    ": Failed, " +
                                    retryCount.toString() +
                                    "retries remain"
                        )
                    } else {
                        throw t
                    }
                }
            }
        }
    }
}