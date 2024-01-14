package com.rockthejvm.reviewboard.services

import com.rockthejvm.reviewboard.domain.data.{User, UserId, UserToken}
import com.rockthejvm.reviewboard.repositories.UserRepository
import zio.*
import zio.test.*
object UserServiceSpec extends ZIOSpecDefault {

  private val daniel = User(
    1L,
    "daniel@rockthejvm.com",
    "1000:6BDF049DD0D7F8D60583291270E9F5DC39AE82CE81FA79B1:7BC8C1162CC36C0A04394C67787853C4974AE43565251018"
  )

  private val stubRepoLayer = ZLayer.succeed {
    new UserRepository {

      private val db = collection.mutable.Map[Long, User](1L -> daniel)

      override def create(user: User): Task[User] =
        ZIO.succeed {
          db += (user.id -> user)
          user
        }

      override def getById(id: Long): Task[Option[User]] = ZIO.succeed(db.get(id))

      override def getByEmail(email: String): Task[Option[User]] = ZIO.succeed(db.values.find(_.email == email))

      override def update(id: Long, op: User => User): Task[User] = ZIO.attempt {
        val newUser = op(db(id))
        db += (newUser.id -> newUser)
        newUser
      }

      override def delete(id: Long): Task[User] = ZIO.attempt {
        val user = db(id)
        db -= id
        user
      }
    }
  }

  private val stubJwtLayer = ZLayer.succeed {
    new JWTService {
      override def createToken(user: User): Task[UserToken] =
        ZIO.succeed(UserToken(user.email, "MY SUPER TOKEN", Long.MaxValue))

      override def verifyToken(token: String): Task[UserId] = ZIO.succeed(UserId(daniel.id, daniel.email))
    }

  }
  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("UserServiceSpec")(
      test("create and validate a user") {
        for {
          service <- ZIO.service[UserService]
          user    <- service.registerUser(daniel.email, "rockthejvm")
          valid   <- service.verifyPassword(daniel.email, "rockthejvm")
        } yield assertTrue(valid && user.email == daniel.email)
      },
      test("validate correct credentials") {
        for {
          service <- ZIO.service[UserService]
          valid   <- service.verifyPassword(daniel.email, "rockthejvm")
        } yield assertTrue(valid)
      },
      test("invalidate incorrect credentials") {
        for {
          service <- ZIO.service[UserService]
          valid   <- service.verifyPassword(daniel.email, "somethingelse")
        } yield assertTrue(!valid)
      },
      test("invalidate non existing user") {
        for {
          service <- ZIO.service[UserService]
          valid   <- service.verifyPassword("someone@gmail.com", "somethingelse")
        } yield assertTrue(!valid)
      },
      test("update a password") {
        for {
          service  <- ZIO.service[UserService]
          _        <- service.updatePassword(daniel.email, "rockthejvm", "scalarulez")
          oldValid <- service.verifyPassword(daniel.email, "rockthejvm")
          newValid <- service.verifyPassword(daniel.email, "scalarulez")
        } yield assertTrue(newValid && !oldValid)
      },
      test("delete non existing user should fail") {
        for {
          service <- ZIO.service[UserService]
          err     <- service.deleteUser("someone@gmail.com", "somethingelse").flip
        } yield assertTrue(err.isInstanceOf[RuntimeException])
      },
      test("delete with incorrect credentials should fail") {
        for {
          service <- ZIO.service[UserService]
          err     <- service.deleteUser(daniel.email, "somethingelse").flip
        } yield assertTrue(err.isInstanceOf[RuntimeException])
      },
      test("delete user") {
        for {
          service <- ZIO.service[UserService]
          user    <- service.deleteUser(daniel.email, "rockthejvm")
        } yield assertTrue(user.email == daniel.email)
      }
    ).provide(
      UserServiceLive.layer,
      stubJwtLayer,
      stubRepoLayer
    )
}
