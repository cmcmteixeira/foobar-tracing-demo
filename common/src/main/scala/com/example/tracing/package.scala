package com.example

import java.util.concurrent.Executors

import kamon.executors.{Executors => KamonExecutors}
import kamon.executors.util.ContextAwareExecutorService

import scala.concurrent.ExecutionContext

package object tracing {
  private val threadPool = Executors.newFixedThreadPool(30)
  private val _ec        = ExecutionContext.fromExecutor(threadPool)
  KamonExecutors.register("fixed-thread-pool", threadPool)
  val ec: ExecutionContext = ExecutionContext
    .fromExecutorService(
      ContextAwareExecutorService(ExecutionContextExecutorServiceBridge(_ec))
    )
}
