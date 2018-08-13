package com.example.tracing

import java.util.Collections
import java.util.concurrent.AbstractExecutorService

import scala.concurrent.duration.TimeUnit
import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService}

object ExecutionContextExecutorServiceBridge {
  def apply(ec: ExecutionContext): ExecutionContextExecutorService = ec match {
    case null                                  => throw null
    case eces: ExecutionContextExecutorService => eces
    case other =>
      new AbstractExecutorService with ExecutionContextExecutorService {
        override def prepare(): ExecutionContext                             = other
        override def isShutdown                                              = false
        override def isTerminated                                            = false
        override def shutdown()                                              = ()
        override def shutdownNow()                                           = Collections.emptyList[Runnable]
        override def execute(runnable: Runnable): Unit                       = other execute runnable
        override def reportFailure(t: Throwable): Unit                       = other reportFailure t
        override def awaitTermination(length: Long, unit: TimeUnit): Boolean = false
      }
  }
}
