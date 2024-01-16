package com.rockthejvm.reviewboard.http.endpoints

import com.rockthejvm.reviewboard.domain.data.UserToken
import com.rockthejvm.reviewboard.http.requests.*
import com.rockthejvm.reviewboard.http.responses.UserResponse
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.*

trait UserEndpoints extends BaseEndpoint {

  val createUserEndpoint: Endpoint[Unit, RegisterUserAccount, Throwable, UserResponse, Any] = baseEndpoint
    .tag("users")
    .name("register")
    .description("Register a user account with username and password")
    .in("users")
    .post
    .in(jsonBody[RegisterUserAccount])
    .out(jsonBody[UserResponse])

  val updatePasswordEndpoint: Endpoint[String, UpdatePasswordRequest, Throwable, UserResponse, Any] =
    secureBaseEndpoints
      .tag("users")
      .name("updatePassword")
      .description("Update user password")
      .in("users" / "password")
      .put
      .in(jsonBody[UpdatePasswordRequest])
      .out(jsonBody[UserResponse])

  val deleteEndpoint: Endpoint[String, DeleteAccountRequest, Throwable, UserResponse, Any] = secureBaseEndpoints
    .tag("users")
    .name("delete")
    .description("Delete user account")
    .in("users")
    .delete
    .in(jsonBody[DeleteAccountRequest])
    .out(jsonBody[UserResponse])

  val loginTokenEndpoint: Endpoint[Unit, LoginRequest, Throwable, UserToken, Any] = baseEndpoint
    .tag("users")
    .name("login")
    .description("Log in and generate a JWT token")
    .in("users" / "login")
    .post
    .in(jsonBody[LoginRequest])
    .out(jsonBody[UserToken])

  val forgotPasswordEndpoint: Endpoint[Unit, ForgotPasswordRequest, Throwable, Unit, Any] = baseEndpoint
    .tag("users")
    .name("forgotPassword")
    .description("Trigger mail for password recovery")
    .in("users" / "forgot")
    .post
    .in(jsonBody[ForgotPasswordRequest])

  val recoverPasswordEndpoint: Endpoint[Unit, RecoverPasswordRequest, Throwable, Unit, Any] = baseEndpoint
    .tag("users")
    .name("recoverPassword")
    .description("Set new password based on OTP")
    .in("users" / "recover")
    .post
    .in(jsonBody[RecoverPasswordRequest])

}
