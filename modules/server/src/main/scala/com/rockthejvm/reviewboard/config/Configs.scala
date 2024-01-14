package com.rockthejvm.reviewboard.config
import com.typesafe.config.ConfigFactory
import zio.*
import zio.config.*
import zio.config.magnolia.*
import zio.config.typesafe.*

object Configs {
  def makeConfig[C](path: String)(using derive: DeriveConfig[C]): IO[Config.Error, C] = {
    ConfigProvider
      .fromTypesafeConfig(ConfigFactory.load().getConfig(path))
      .load(deriveConfig[C])
  }
}
