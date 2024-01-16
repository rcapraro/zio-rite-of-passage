package com.rockthejvm.reviewboard.domain.data

final case class PasswordRecoveryToken(
    email: String,
    token: String,
    expiration: Long
)
