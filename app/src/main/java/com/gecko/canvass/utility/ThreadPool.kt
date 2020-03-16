package com.gecko.canvass.utility

import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

object ThreadPool
{
    var executor: ExecutorService
    private val size: Int = 5;//thread pool size

    //initializing the base constructor
    init {
        executor = Executors.newFixedThreadPool(size)
    }

    fun postTask(runnable: Runnable)
    {
        executor.submit(runnable)
    }
}