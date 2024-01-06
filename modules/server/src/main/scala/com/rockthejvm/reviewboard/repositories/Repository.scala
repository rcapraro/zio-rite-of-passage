package com.rockthejvm.reviewboard.repositories

import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill
import zio.*

import javax.sql.DataSource

object Repository {

  def quillLayer: URLayer[DataSource, Quill.Postgres[SnakeCase.type]] =
    Quill.Postgres.fromNamingStrategy(SnakeCase)

  private def dataSourceLayer: TaskLayer[DataSource] =
    Quill.DataSource.fromPrefix("rockthejvm.db")

  def dataLayer: TaskLayer[Quill.Postgres[SnakeCase.type]] = dataSourceLayer >>> quillLayer

}
