package com.rockthejvm.part3concurrency

import zio._
import com.rockthejvm.utils._

object Resources extends ZIOAppDefault {
  // Finalizers
  def unsafeMethod(): Int = throw new RuntimeException("Not an int here for you !")
  val anAttempt = ZIO.attempt(unsafeMethod())

  // Finalizers can be attached to an object
  val attemptWithFinalizer = anAttempt.ensuring(ZIO.succeed("Finalizer!").debugThread)

  // Multiple finalizers
  val attemptWith2Finalizers = attemptWithFinalizer.ensuring(ZIO.succeed("another finalizer!").debugThread)

  // .onInterrupt, .onError, .onDone, .onExit


  def run = attemptWith2Finalizers
}
