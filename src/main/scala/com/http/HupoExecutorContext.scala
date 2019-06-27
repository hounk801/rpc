package com.http

import java.util.concurrent._

import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.util.{HupoCompletableFuture, HupoCompletionStageWrapper}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.java8.FuturesConvertersImpl.{CF, InternalCallbackExecutor, P}

/**
  * @author hnk
  * @date 2019/6/27
  */
object FutureConverter {
  implicit lazy val hupoForkJoinExecutor: ExecutorService = {
    new ForkJoinPool(64, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true)
  }

  lazy val hupoForkJoinExecutorContext = ExecutionContext.fromExecutorService(hupoForkJoinExecutor)

  implicit lazy val hupoCommonExecutorPool: ExecutorService = {
    val namedThreadFactory: ThreadFactory = new ThreadFactoryBuilder().setNameFormat("default-async-worker-pool-%d").build
    new ThreadPoolExecutor(10, 500, 30000L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue[Runnable](10), namedThreadFactory,
      new ThreadPoolExecutor.AbortPolicy)

  }

  @deprecated(message = "By hupoForkJoinExecutor", since = "2019-01-14")
  implicit lazy val javaExecutorService: ExecutorService = hupoForkJoinExecutor

  def toJava[T](f: Future[T]): CompletionStage[T] = {
    f match {
      case p: P[T] => {
        new HupoCompletableFuture(p.wrapped)
      }
      case _ =>
        val cf = new CF[T](f)
        implicit val ec = InternalCallbackExecutor
        f onComplete cf
        new HupoCompletionStageWrapper[T](cf)
    }
  }

  def toScala[T](cs: CompletionStage[T]): Future[T] = {
    cs match {
      case cf: CF[T] => cf.wrapped
      case cf: HupoCompletionStageWrapper[T] => toScala(cf.getDelegated)
      case _ =>
        val p = new P[T](cs)
        cs whenComplete p
        p.future
    }
  }
}

trait HupoExecutorContext {
  implicit lazy val hupoExecutionContextExecutor = FutureConverter.hupoForkJoinExecutorContext
}
