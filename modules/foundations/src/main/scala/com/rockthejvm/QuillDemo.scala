package com.rockthejvm

import io.getquill.*
import io.getquill.jdbczio.Quill
import zio.*

object QuillDemo extends ZIOAppDefault {

  val program = for {
    repo <- ZIO.service[JobRepository]
    _ <- repo.create(Job(-1, "Software engineer", "rockthejvm.com", "Rock the JVM"))
    _ <- repo.create(Job(-1, "Instructor", "rockthejvm.com", "Rock the JVM"))
  } yield ()

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = program.provide(
    JobRepositoryLive.layer,
    Quill.Postgres.fromNamingStrategy(SnakeCase),
    Quill.DataSource.fromPrefix("mydbconf") // reads the config section in application.conf and spins up a datasource
  )

}

// repository
trait JobRepository {
  def create(job: Job): Task[Job]
  def update(id: Long, op: Job => Job): Task[Job]
  def delete(id: Long): Task[Job]
  def getById(id: Long): Task[Option[Job]]
  def get(id: Long): Task[List[Job]]
}

class JobRepositoryLive(quill: Quill.Postgres[SnakeCase]) extends JobRepository {

  // step 1 - some methods, e.g run a query
  import quill.*

  // step 2 = schemas for create, update ...
  inline given schema: SchemaMeta[Job]  = schemaMeta[Job]("jobs") // specify the table name
  inline given inMeta: InsertMeta[Job] = insertMeta[Job](_.id)   // id column will be excluded in insert statements
  inline given upMeta: UpdateMeta[Job] = updateMeta[Job](_.id)   // same for update statements

  override def create(job: Job): Task[Job] = run {
      query[Job]
        .insertValue(lift(job))
        .returning(j => j)
    }

  override def update(id: Long, op: Job => Job): Task[Job] = for {
    current <- getById(id).someOrFail(new RuntimeException("Could not update: missing key $id"))
    updated <- run {
      query[Job]
        .filter(_.id == lift(id))
        .updateValue(lift(current))
        .returning(j => j)
    }
  } yield updated

  override def delete(id: Long): Task[Job] = run {
    query[Job]
      .filter(_.id == lift(id))
      .delete
      .returning(j => j)
  }

  override def getById(id: Long): Task[Option[Job]] = run {
    query[Job]
      .filter(_.id == lift(id))
  }.map(_.headOption)

  override def get(id: Long): Task[List[Job]] = run(query[Job])

}

object JobRepositoryLive {
  val layer = ZLayer {
    ZIO.serviceWith[Quill.Postgres[SnakeCase]](quill => JobRepositoryLive(quill))
  }
}
