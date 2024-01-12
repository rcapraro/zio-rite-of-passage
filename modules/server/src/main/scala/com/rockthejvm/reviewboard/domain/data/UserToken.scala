package com.rockthejvm.reviewboard.domain.data

final case class UserToken(
    email: String,
    token: String,
    expires: Long
)
