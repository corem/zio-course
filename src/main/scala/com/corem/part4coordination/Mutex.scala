package com.corem.part4coordination

import zio._
import com.corem.utils._

abstract class Mutex {
  def acquire: UIO[Unit]
  def release: UIO[Unit]
}

object Mutex {
  def make: UIO[Mutex] = ???
}