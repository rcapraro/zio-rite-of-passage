package com.rockthejvm.reviewboard.config

final case class JWTConfig(
    secret: String,
    ttl: Long
)
