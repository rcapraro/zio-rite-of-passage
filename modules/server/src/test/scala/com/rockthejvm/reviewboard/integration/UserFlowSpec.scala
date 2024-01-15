package com.rockthejvm.reviewboard.integration

import com.rockthejvm.reviewboard.config.JWTConfig
import com.rockthejvm.reviewboard.domain.data.UserToken
import com.rockthejvm.reviewboard.http.controllers.*
import com.rockthejvm.reviewboard.http.requests.{
  DeleteAccountRequest,
  LoginRequest,
  RegisterUserAccount,
  UpdatePasswordRequest
}
import com.rockthejvm.reviewboard.http.responses.UserResponse
import com.rockthejvm.reviewboard.repositories.{Repository, RepositorySpec, UserRepository, UserRepositoryLive}
import com.rockthejvm.reviewboard.services.*
import sttp.client3.*
import sttp.client3.testing.SttpBackendStub
import sttp.model.Method
import sttp.monad.MonadError
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.ztapir.RIOMonadError
import zio.*
import zio.json.*
import zio.test.*

object UserFlowSpec extends ZIOSpecDefault with RepositorySpec {

  override val initScript: String = "sql/integration.sql"

  private given zioME: MonadError[Task] = new RIOMonadError[Any]

  private def backendStubZIO = for {
    controller <- UserController.makeZIO
    backendStub <- ZIO.succeed(
      TapirStubInterpreter(SttpBackendStub(MonadError[Task]))
        .whenServerEndpointsRunLogic(controller.routes)
        .backend()
    )
  } yield backendStub

  extension [A: JsonCodec](backend: SttpBackend[Task, Nothing]) {
    def sendRequest[B: JsonCodec](
        method: Method,
        path: String,
        payload: A,
        maybeToken: Option[String] = None
    ): Task[Option[B]] =
      basicRequest
        .method(method, uri"$path")
        .body(payload.toJson)
        .auth
        .bearer(maybeToken.getOrElse(""))
        .send(backend)
        .map(_.body)
        .map(_.toOption.flatMap(payload => payload.fromJson[B].toOption))

    def post[B: JsonCodec](path: String, payload: A): Task[Option[B]] =
      sendRequest(Method.POST, path, payload, None)

    def postAuth[B: JsonCodec](path: String, payload: A, token: String): Task[Option[B]] =
      sendRequest(Method.POST, path, payload, Some(token))

    def put[B: JsonCodec](path: String, payload: A): Task[Option[B]] =
      sendRequest(Method.PUT, path, payload, None)

    def putAuth[B: JsonCodec](path: String, payload: A, token: String): Task[Option[B]] =
      sendRequest(Method.PUT, path, payload, Some(token))

    def delete[B: JsonCodec](path: String, payload: A): Task[Option[B]] =
      sendRequest(Method.DELETE, path, payload, None)

    def deleteAuth[B: JsonCodec](path: String, payload: A, token: String): Task[Option[B]] =
      sendRequest(Method.DELETE, path, payload, Some(token))
  }

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("UserFlowSpec")(
      test("create user") {
        for {
          backendStub <- backendStubZIO
          maybeResponse <- backendStub
            .post[UserResponse]("/users", RegisterUserAccount(email = "daniel@rockthejvm.com", password = "rockthejvm"))
        } yield assertTrue(maybeResponse.contains(UserResponse("daniel@rockthejvm.com")))
      },
      test("create and log in") {
        for {
          backendStub <- backendStubZIO
          _ <- backendStub
            .post[UserResponse]("/users", RegisterUserAccount(email = "daniel@rockthejvm.com", password = "rockthejvm"))
          maybeToken <- backendStub
            .post[UserToken]("/users/login", LoginRequest("daniel@rockthejvm.com", "rockthejvm"))
        } yield assertTrue(
          maybeToken.exists(_.email == "daniel@rockthejvm.com")
        )
      },
      test("update password") {
        for {
          backendStub <- backendStubZIO
          _ <- backendStub
            .post[UserResponse]("/users", RegisterUserAccount(email = "daniel@rockthejvm.com", password = "rockthejvm"))
          userToken <- backendStub
            .post[UserToken]("/users/login", LoginRequest("daniel@rockthejvm.com", "rockthejvm"))
            .someOrFail(new RuntimeException("Authentication failed"))
          _ <- backendStub
            .putAuth[UserResponse](
              "/users/password",
              UpdatePasswordRequest("daniel@rockthejvm.com", "rockthejvm", "scalarulez"),
              userToken.token
            )
          maybeOldToken <- backendStub
            .post[UserToken]("/users/login", LoginRequest("daniel@rockthejvm.com", "rockthejvm"))
          maybeNewToken <- backendStub
            .post[UserToken]("/users/login", LoginRequest("daniel@rockthejvm.com", "scalarulez"))
        } yield assertTrue(maybeOldToken.isEmpty && maybeNewToken.nonEmpty)
      },
      test("delete user") {
        for {
          backendStub <- backendStubZIO
          userRepo    <- ZIO.service[UserRepository]
          _ <- backendStub
            .post[UserResponse]("/users", RegisterUserAccount(email = "daniel@rockthejvm.com", password = "rockthejvm"))
          maybeOldUser <- userRepo.getByEmail("daniel@rockthejvm.com")
          userToken <- backendStub
            .post[UserToken]("/users/login", LoginRequest("daniel@rockthejvm.com", "rockthejvm"))
            .someOrFail(new RuntimeException("Authentication failed"))
          _ <- backendStub
            .deleteAuth[UserResponse](
              "/users",
              DeleteAccountRequest("daniel@rockthejvm.com", "rockthejvm"),
              userToken.token
            )
          maybeUser <- userRepo.getByEmail("daniel@rockthejvm.com")
        } yield assertTrue(maybeOldUser.exists(_.email == "daniel@rockthejvm.com") && maybeUser.isEmpty)
      }
    ).provide(
      UserServiceLive.layer,
      JWTServiceLive.layer,
      UserRepositoryLive.layer,
      ZLayer.succeed(JWTConfig("secret", 3600)),
      Repository.quillLayer,
      postgresDataSourceLayer,
      Scope.default
    )
}
