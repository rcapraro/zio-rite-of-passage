package com.rockthejvm.reviewboard.domain.errors

import sttp.model.StatusCode

final case class HttpError(
    statusCode: StatusCode,
    message: String,
    cause: Throwable
) extends RuntimeException(message, cause)

object HttpError {
  def decode(tuple: (StatusCode, String)): HttpError = HttpError(tuple._1, tuple._2, new RuntimeException(tuple._2))

  def encode(error: Throwable): (StatusCode, String) = error match {
    case UnAuthorizedException => (StatusCode.Unauthorized, error.getMessage)
    // TODO add more cases
    case _ => (StatusCode.Unauthorized, error.getMessage)
  }

}
