package models

import javax.inject.Inject

import anorm._
import anorm.SqlParser.{get, int, str}
import play.api.db.DBApi
import play.api.libs.json.{JsValue, Json, Reads, Writes}
import play.api.mvc.PathBindable

import scala.concurrent.Future

case class Comment(id: Comment.Id, text: String, todoId: Todo.Id)
object Comment {
  case class Id(unwrap: Int)
  object Id {
    // Read/write Ids to json as though they were Ints
    implicit val reads: Reads[Id] = Reads.of[Int].map(Id.apply)
    implicit val writes: Writes[Id] = new Writes[Id] {
      override def writes(o: Id): JsValue = Writes.of[Int].writes(o.unwrap)
    }
    // Bind Ids in url paths as though they were Ints
    implicit val pathBindable: PathBindable[Id] = new PathBindable.Parsing[Id](
      parse = str => Id.apply(str.toInt),
      serialize = v => v.unwrap.toString,
      error = (key: String, err: Exception) => s"$key must be an integer"
    )
  }

  case class Create(text: String)
  object Create {
    implicit val readsJson: Reads[Create] = Json.reads[Create]
    implicit val writesJson: Writes[Create] = Json.writes[Create]
  }

  implicit val readsJson: Reads[Comment] = Json.reads[Comment]
  implicit val writesJson: Writes[Comment] = Json.writes[Comment]
}

@javax.inject.Singleton
class CommentRepository @Inject()(dbapi: DBApi)(implicit ec: DatabaseExecutionContext) {

  private val db = dbapi.database("default")

  private val parser: RowParser[Comment] = {
    (get[Int]("comments.id") ~
      str("comments.text") ~
      get[Int]("comments.todoId"))
      .map {
        case id ~ text ~ todoId => Comment(Comment.Id(id), text, Todo.Id(todoId))
      }
  }

  def insert(todoId: Todo.Id, commentCreate: Comment.Create): Future[Comment] =
    Future(db.withConnection { implicit connection =>
      val id = SQL"""INSERT INTO comments (text, todoId) VALUES(
              ${commentCreate.text},
              ${todoId.unwrap})""".executeInsert(int(1).single)
      Comment(id = Comment.Id(id), text = commentCreate.text, todoId = todoId)
    })

  def delete(id: Comment.Id): Future[Boolean] =
    Future(db.withConnection { implicit connection =>
      val rowsDeleted =
        SQL"""DELETE FROM comments WHERE id = ${id.unwrap}""".executeUpdate()
      rowsDeleted == 1
    })

  def getById(id: Comment.Id): Future[Option[Comment]] =
    Future(db.withConnection { implicit connection =>
      SQL"SELECT * FROM comments WHERE id = ${id.unwrap}".executeQuery().as(parser.singleOpt)
    })

  def list(todoId: Todo.Id): Future[List[Comment]] =
    Future(db.withConnection { implicit connection =>
      SQL"SELECT * FROM comments WHERE comments.todoId = ${todoId.unwrap} ORDER BY id"
        .executeQuery()
        .as(parser.*)
    })
}
