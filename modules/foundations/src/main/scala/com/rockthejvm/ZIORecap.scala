package com.rockthejvm

import zio.*

import java.io.IOException
import scala.io.StdIn

object ZIORecap extends ZIOAppDefault {

  // success
  val meaningOfLife: UIO[Int] = ZIO.succeed(42)

  // failure
  val aFailure: IO[String, Nothing] = ZIO.fail("Boom!")

  // suspension/delay
  val aSuspension: Task[RuntimeFlags] = ZIO.suspend(meaningOfLife)

  // map/flatMap

  val improvedMol: UIO[Int] = meaningOfLife.map(_ * 2)

  val printingMol: UIO[Unit] = meaningOfLife.flatMap(mol => ZIO.succeed(println(mol)))

  val smallProgram: IO[IOException, Unit] = for {
    _    <- Console.printLine("What's your name?")
    name <- ZIO.succeed(StdIn.readLine())
    _    <- Console.printLine(s"Welcome to ZIO, $name")
  } yield ()

  // error handling
  val anAttempt: Task[Int] = ZIO.attempt {
    // expr which can throw
    println("trying something")
    val string: String = null
    string.length
  }

  // catch errors effectfully
  val catchError: ZIO[Any, Nothing, Any] =
    anAttempt.catchAll(e => ZIO.succeed("Returning some different value"))
  val catchSelective: ZIO[Any, Throwable, Any] = anAttempt.catchSome {
    case e: RuntimeException => ZIO.succeed("Ignoring runtime exception: $e")
    case _                   => ZIO.succeed("Ignoring everything else")
  }

  // fibers
  val delayedValue: UIO[Int] = Random.nextIntBetween(0, 100).delay(1.second)
  val aPair: ZIO[Any, Nothing, (Int, Int)] = for {
    a <- delayedValue
    b <- delayedValue
  } yield (a, b) // this takes 2 seconds

  val aPairPar: UIO[(RuntimeFlags, RuntimeFlags)] = for {
    fibA <- delayedValue.fork // returns some other effect which has a Fiber
    fibB <- delayedValue.fork
    a    <- fibA.join
    b    <- fibB.join
  } yield (a, b) // this takes 1 second

  val interruptedFiber = for {
    fib <- delayedValue.map(println).onInterrupt(ZIO.succeed(println("I'm interrupted"))).fork
    _   <- ZIO.succeed(println("cancelling fiber")).delay(500.millis) *> fib.interrupt
    _   <- fib.join
  } yield ()

  val ignoreInterruption = for {
    fib <- ZIO
      .uninterruptible(
        delayedValue.map(println).onInterrupt(ZIO.succeed(println("I'm interrupted")))
      )
      .fork
    _ <- ZIO.succeed(println("cancelling fiber")).delay(500.millis) *> fib.interrupt
    _ <- fib.join
  } yield ()

  // many APIs on top of fibers
  val aPairPar_v2: UIO[(RuntimeFlags, RuntimeFlags)] = delayedValue.zipPar(delayedValue)
  val randomx10: UIO[Seq[RuntimeFlags]] = ZIO.foreachPar(1 to 10)(_ => delayedValue) // "traverse"

  // dependencies
  case class User(name: String, email: String)

  // ---
  class UserSubscription(emailService: EmailService, userDatabase: UserDatabase) {
    def subscribeUser(user: User): Task[Unit] = for {
      _ <- emailService.email(user)
      _ <- userDatabase.insert(user)
      _ <- ZIO.succeed(s"Subscribed $user")
    } yield ()
  }
  object UserSubscription {
    val live: URLayer[EmailService & UserDatabase, UserSubscription] =
      ZLayer.fromFunction(UserSubscription(_, _))
  }

  // ---
  class EmailService {
    def email(user: User): Task[Unit] = ZIO.succeed(s"Emailed $user")
  }
  object EmailService {
    val live: ULayer[EmailService] = ZLayer.succeed(EmailService())
  }

  // ---
  class UserDatabase(connectionPool: ConnectionPool) {
    def insert(user: User): Task[Unit] = ZIO.succeed(s"Inserted user $user")
  }
  object UserDatabase {
    val live: URLayer[ConnectionPool, UserDatabase] =
      ZLayer.fromFunction(UserDatabase(_))
  }

  // ---
  class ConnectionPool(nConnections: Int) {
    def get: Task[Connection] = ZIO.succeed(Connection())
  }
  object ConnectionPool {
    def live(nConnections: Int): ULayer[ConnectionPool] =
      ZLayer.succeed(ConnectionPool(nConnections))
  }

  case class Connection()

  def subscribe(user: User): RIO[UserSubscription, Unit] = for {
    sub <- ZIO.service[UserSubscription]
    _   <- sub.subscribeUser(user)
  } yield ()

  val program: RIO[UserSubscription, Unit] = for {
    _ <- subscribe(User("Richard", "richard@rockthejvm.com"))
    _ <- subscribe(User("Daniel", "daniel@rockthejvm.com"))
  } yield ()

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = program.provide(
    ConnectionPool.live(10),
    UserDatabase.live,
    EmailService.live,
    UserSubscription.live
  )
}
