package com.rockthejvm.reviewboard.config

final case class EmailServiceConfig(
    host: String,
    port: Int,
    user: String,
    passwd: String
)
