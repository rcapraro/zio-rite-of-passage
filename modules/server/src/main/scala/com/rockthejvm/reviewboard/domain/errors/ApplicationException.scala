package com.rockthejvm.reviewboard.domain.errors

abstract class ApplicationException(message: String) extends RuntimeException(message)

case object UnAuthorizedException extends ApplicationException("Unauthorized")
