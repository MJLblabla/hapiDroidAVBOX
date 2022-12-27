
package com.hapi.srtlive.bitrateregulator

import kotlinx.coroutines.*

class Scheduler(
    private val delayTimeMillis: Long,
    private val coroutineScope: CoroutineScope = GlobalScope,
    private val action: suspend CoroutineScope.() -> Unit
) {
    private var job: Job? = null

    fun start() {
        job = coroutineScope.launch {
            while (true) {
                delay(delayTimeMillis)
                launch { action() }
            }
        }
    }

    fun cancel() {
        job?.cancel()
    }
}