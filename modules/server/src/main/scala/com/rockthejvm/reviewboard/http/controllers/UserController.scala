package com.rockthejvm.reviewboard.http.controllers

import com.rockthejvm.reviewboard.domain.data.UserId
import com.rockthejvm.reviewboard.domain.errors.UnAuthorizedException
import com.rockthejvm.reviewboard.http.endpoints.UserEndpoints
import com.rockthejvm.reviewboard.http.requests.{DeleteAccountRequest, UpdatePasswordRequest}
import com.rockthejvm.reviewboard.http.responses.UserResponse
import com.rockthejvm.reviewboard.services.{JWTService, UserService}
import sttp.tapir.*
import sttp.tapir.server.*
import sttp.tapir.server.ServerEndpoint.Full
import zio.*

class UserController private (userService: UserService, jwtService: JWTService)
    extends BaseController
    with UserEndpoints {

  val create: ServerEndpoint[Any, Task] = createUserEndpoint
    .serverLogic { req =>
      userService
        .registerUser(req.email, req.password)
        .map(user => UserResponse(user.email))
        .either
    }

  val login: ServerEndpoint[Any, Task] = loginTokenEndpoint
    .serverLogic { req =>
      userService
        .generateToken(req.email, req.password)
        .someOrFail(UnAuthorizedException)
        .either
    }

  val updatePassword: ServerEndpoint[Any, Task] =
    updatePasswordEndpoint
      .serverSecurityLogic[UserId, Task](token => jwtService.verifyToken(token).either)
      .serverLogic { userId => req =>
        userService
          .updatePassword(req.email, req.oldPassword, req.newPassword)
          .map(user => UserResponse(user.email))
          .either
      }

  val delete: ServerEndpoint[Any, Task] =
    deleteEndpoint
      .serverSecurityLogic[UserId, Task](token => jwtService.verifyToken(token).either)
      .serverLogic { userId => req =>
        userService
          .deleteUser(req.email, req.password)
          .map(user => UserResponse(user.email))
          .either
      }
  override val routes: List[ServerEndpoint[Any, Task]] = List(create, login, updatePassword, delete)
}

object UserController {
  val makeZIO: URIO[JWTService with UserService, UserController] = for {
    userService <- ZIO.service[UserService]
    jwtService  <- ZIO.service[JWTService]
  } yield UserController(userService, jwtService)
}
