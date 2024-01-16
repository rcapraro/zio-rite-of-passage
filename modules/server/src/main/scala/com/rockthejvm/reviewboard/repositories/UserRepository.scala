package com.rockthejvm.reviewboard.repositories

import com.rockthejvm.reviewboard.domain.data.User
import io.getquill.*
import io.getquill.jdbczio.Quill
import zio.*

trait UserRepository {
  def create(user: User): Task[User]
  def getById(id: Long): Task[Option[User]]
  def getByEmail(email: String): Task[Option[User]]
  def update(id: Long, op: User => User): Task[User]
  def delete(id: Long): Task[User]
}

class UserRepositoryLive private (quill: Quill.Postgres[SnakeCase]) extends UserRepository {
  import quill.*

  inline given schema: SchemaMeta[User] = schemaMeta[User]("users")

  inline given insMeta: InsertMeta[User] = insertMeta[User](_.id)

  inline given updMeta: UpdateMeta[User] = updateMeta[User](_.id)

  override def create(user: User): Task[User] =
    run(
      query[User]
        .insertValue(lift(user))
        .returning(r => r)
    )

  override def getById(id: Long): Task[Option[User]] =
    run(
      query[User]
        .filter(_.id == lift(id))
    ).map(_.headOption)

  override def getByEmail(email: String): Task[Option[User]] =
    run(
      query[User]
        .filter(_.email == lift(email))
    ).map(_.headOption)

  override def update(id: Long, op: User => User): Task[User] =
    for {
      current <- getById(id).someOrFail(new RuntimeException(s"Could not update: missing id $id"))
      updated <- run(
        query[User]
          .filter(_.id == lift(id))
          .updateValue(lift(op(current)))
          .returning(r => r)
      )
    } yield updated

  override def delete(id: Long): Task[User] =
    run(
      query[User]
        .filter(_.id == lift(id))
        .delete
        .returning(r => r)
    )
}

object UserRepositoryLive {
  val layer: URLayer[Quill.Postgres[SnakeCase], UserRepositoryLive] = ZLayer(
    ZIO.serviceWith[Quill.Postgres[SnakeCase]](quill => UserRepositoryLive(quill))
  )
}
