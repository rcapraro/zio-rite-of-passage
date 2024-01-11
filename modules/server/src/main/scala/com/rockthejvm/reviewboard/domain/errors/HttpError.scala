package com.rockthejvm.reviewboard.domain.errors

import sttp.model.StatusCode

case class HttpError(
    statusCode: StatusCode,
    message: String,
    cause: Throwable
) extends RuntimeException(message, cause)

object HttpError {
  def decode(tuple: (StatusCode, String)): HttpError = HttpError(tuple._1, tuple._2, new RuntimeException(tuple._2))

  def encode(error: Throwable): (StatusCode, String) =
    (StatusCode.InternalServerError, error.getMessage)
}
