package models

import javax.inject.Inject

import anorm._
import anorm.SqlParser.{bool, get, int, str}
import play.api.db.DBApi
import play.api.libs.json.{JsValue, Json, Reads, Writes}
import play.api.mvc.PathBindable

import scala.concurrent.Future

case class Todo(id: Todo.Id, text: String, checked: Boolean)
object Todo {
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

  case class Create(text: String, checked: Boolean = false) {
    def withId(id: Todo.Id): Todo = Todo(id, text, checked)
  }
  object Create {
    implicit val readsJson: Reads[Create] = Json.reads[Create]
    implicit val writesJson: Writes[Create] = Json.writes[Create]
  }

  implicit val readsJson: Reads[Todo] = Json.reads[Todo]
  implicit val writesJson: Writes[Todo] = Json.writes[Todo]
}

@javax.inject.Singleton
class TodoRepository @Inject()(dbapi: DBApi)(implicit ec: DatabaseExecutionContext) {

  private val db = dbapi.database("default")

  private val parser: RowParser[Todo] = {
    (get[Int]("todos.id") ~
      str("todos.text") ~
      bool("todos.checked"))
      .map {
        case id ~ text ~ checked => Todo(Todo.Id(id), text, checked)
      }
  }

  def insert(todoCreate: Todo.Create): Future[Todo] =
    Future(db.withConnection { implicit connection =>
      val id = SQL"""INSERT INTO todos (text, checked) VALUES(
              ${todoCreate.text},
              ${todoCreate.checked})""".executeInsert(int(1).single)
      todoCreate.withId(Todo.Id(id))
    })

  def update(id: Todo.Id, todoCreate: Todo.Create): Future[Option[Todo]] =
    Future(db.withConnection { implicit connection =>
      SQL"""UPDATE todos SET
            text = ${todoCreate.text},
            checked = ${todoCreate.checked} WHERE id = ${id.unwrap} RETURNING *"""
        .executeInsert(parser.singleOpt)
    })

  def delete(id: Todo.Id): Future[Boolean] =
    Future(db.withConnection { implicit connection =>
      val rowsDeleted = SQL"""DELETE FROM todos WHERE id = ${id.unwrap}""".executeUpdate()
      rowsDeleted == 1
    })

  def getById(id: Todo.Id): Future[Option[Todo]] =
    Future(db.withConnection { implicit connection =>
      SQL"SELECT * FROM todos WHERE id = ${id.unwrap}".executeQuery().as(parser.singleOpt)
    })

  def list: Future[List[Todo]] =
    Future(db.withConnection { implicit connection =>
      SQL"SELECT * FROM todos ORDER BY id".executeQuery().as(parser.*)
    })

  def setChecked(id: Todo.Id, checked: Boolean): Future[Option[Todo]] =
    Future(db.withConnection { implicit connection =>
      SQL"UPDATE todos SET checked = $checked WHERE id = ${id.unwrap} RETURNING *"
        .executeInsert(parser.singleOpt)
    })

}
