package test

import models.{Comment, Todo}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.libs.ws.{EmptyBody, WSClient}

import scala.util.Random

class EndToEndSpec extends PlaySpec with GuiceOneServerPerSuite with ScalaFutures {

  implicit val defaultPatience =
    PatienceConfig(timeout = Span(2, Seconds), interval = Span(5, Millis))

  "/todos endpoint" must {
    "allow creation of a todo" in {
      val data = randomTodoCreate()
      val response = ws.url(todosUrl).post(Json.toJson(data)).futureValue
      assert(response.status === 201)
      val result = Json.parse(response.body).as[Todo]
      assert(result.text === data.text)
      assert(result.checked === data.checked)
    }

    "allow deleting a todo" in {
      val existing = getOrCreateTodo()

      val response = ws.url(todosUrl(existing.id)).delete.futureValue
      assert(response.status === 204)
      val getResponse = ws.url(todosUrl(existing.id)).get.futureValue
      assert(getResponse.status === 404)
    }

    "allow updating a todo" in {
      val existing = getOrCreateTodo()
      val modified = Todo.Create("modified", !existing.checked)

      val response = ws.url(todosUrl(existing.id)).put(Json.toJson(modified)).futureValue
      assert(response.status === 200)
      val getResponse = ws.url(todosUrl(existing.id)).get.futureValue
      assert(getResponse.status === 200)
      val result = Json.parse(getResponse.body).as[Todo]
      assert(result.id === existing.id)
      assert(result.text === modified.text)
      assert(result.checked === modified.checked)
    }

    "allow getting a todo by id" in {
      val existing = getOrCreateTodo()

      val response = ws.url(todosUrl(existing.id)).get.futureValue
      assert(response.status === 200)
      val result = Json.parse(response.body).as[Todo]
      assert(result === existing)
    }

    "allow listing all todos" in {
      val existing = getOrCreateTodo()
      val response = ws.url(todosUrl).get.futureValue
      assert(response.status === 200)
      val result = Json.parse(response.body).as[List[Todo]]
      assert(result.contains(existing))
    }

    "allow checking a todo" in {
      val todo = getOrCreateTodo(checked = Some(false))
      assert(todo.checked === false) // Double-check our test
      val response = ws.url(todosUrl(todo.id) + "/check").post(EmptyBody).futureValue
      assert(response.status === 200)
      val result = Json.parse(response.body).as[Todo]
      assert(result === todo.copy(checked = true))
    }

    "allow unchecking a todo" in {
      val todo = getOrCreateTodo(checked = Some(true))
      assert(todo.checked === true) // Double-check our test
      val response = ws.url(todosUrl(todo.id) + "/uncheck").post(EmptyBody).futureValue
      assert(response.status === 200)
      val result = Json.parse(response.body).as[Todo]
      assert(result === todo.copy(checked = false))
    }

    "allow checking a checked todo" in {
      val todo = getOrCreateTodo(checked = Some(true))
      assert(todo.checked === true) // Double-check our test
      val response = ws.url(todosUrl(todo.id) + "/check").post(EmptyBody).futureValue
      assert(response.status === 200)
      val result = Json.parse(response.body).as[Todo]
      assert(result === todo)
    }

    "allow unchecking an unchecked todo" in {
      val todo = getOrCreateTodo(checked = Some(false))
      assert(todo.checked === false) // Double-check our test
      val response = ws.url(todosUrl(todo.id) + "/uncheck").post(EmptyBody).futureValue
      assert(response.status === 200)
      val result = Json.parse(response.body).as[Todo]
      assert(result === todo)
    }

    "ignore an id provided on creation" in {
      val data = randomTodoCreate().withId(Todo.Id(-1))
      val response = ws.url(todosUrl).post(Json.toJson(data)).futureValue
      assert(response.status === 201)
      val result = Json.parse(response.body).as[Todo]
      assert(result.text === data.text)
      assert(result.checked === data.checked)
      assert(result.id !== Todo.Id(-1))
    }

    "return 404 when getting a todo that does not exist" in {
      val response = ws.url(todosUrl(Todo.Id(-1))).get.futureValue
      assert(response.status === 404)
    }

    "return 404 when deleting a todo that does not exist" in {
      val response = ws.url(todosUrl(Todo.Id(-1))).delete.futureValue
      assert(response.status === 404)
    }

    "return 404 when updating a todo that does not exist" in {
      val response =
        ws.url(todosUrl(Todo.Id(-1))).put(Json.toJson(randomTodoCreate())).futureValue
      assert(response.status === 404)
    }

    "return 404 when checking a todo that does not exist" in {
      val response = ws.url(todosUrl(Todo.Id(-1)) + "/check").delete.futureValue
      assert(response.status === 404)
    }

    "return 404 when unchecking a todo that does not exist" in {
      val response = ws.url(todosUrl(Todo.Id(-1)) + "/uncheck").delete.futureValue
      assert(response.status === 404)
    }

    "cascade deletes to comments" in {
      val existingTodo = getOrCreateTodo()
      val existingComments = List.fill(3)(createComment(existingTodo.id))
      withClue("check that we can get the comments before") {
        assert(ws.url(commentsUrl(existingComments.head.id)).get.futureValue.status === 200)
      }
      val response = ws.url(todosUrl(existingTodo.id)).delete.futureValue
      assert(response.status === 204)
      withClue("check that we cannot get the comments after") {
        for (comment <- existingComments) {
          assert(ws.url(commentsUrl(comment.id)).get.futureValue.status === 404)
        }
      }
    }
  }

  "/todos/:id/comments endpoint" must {
    "allow creation of a comment" in {
      val todo = getOrCreateTodo()
      val data = randomCommentCreate()
      val response = ws.url(commentsUrl(todo.id)).post(Json.toJson(data)).futureValue
      assert(response.status === 201)
      val result = Json.parse(response.body).as[Comment]
      assert(result.text === data.text)
      assert(result.todoId === todo.id)
    }

    "allow listing all comments for a todo" in {
      val existingTodo = getOrCreateTodo()
      val existingComments = List.fill(3)(createComment(existingTodo.id))
      val response = ws.url(commentsUrl(existingTodo.id)).get.futureValue
      assert(response.status === 200)
      val result = Json.parse(response.body).as[List[Comment]]
      withClue("all comments should include comments just created") {
        assert(result.size >= 3)
        for (existingComment <- existingComments) {
          assert(result.contains(existingComment))
        }
      }
      withClue("listed comments should be for the expected todo") {
        for (foundComment <- result) {
          assert(foundComment.todoId === existingTodo.id)
        }
      }
    }
  }

  "/comments endpoint" must {
    "allow getting a comment by id" in {
      val existingTodo = getOrCreateTodo()
      val existingComment = createComment(existingTodo.id)

      val response = ws.url(commentsUrl(existingComment.id)).get.futureValue
      assert(response.status === 200)
      val result = Json.parse(response.body).as[Comment]
      assert(result === existingComment)
    }

    "allow deleting a comment" in {
      val existingTodo = getOrCreateTodo()
      val existingComment = createComment(existingTodo.id)

      val response = ws.url(commentsUrl(existingComment.id)).delete.futureValue
      assert(response.status === 204)
      val getResponse = ws.url(commentsUrl(existingComment.id)).get.futureValue
      assert(getResponse.status === 404)
    }

    "return 404 when getting a comment that does not exist" in {
      val response = ws.url(commentsUrl(Comment.Id(-1))).get.futureValue
      assert(response.status === 404)
    }

    "return 404 when deleting a comment that does not exist" in {
      val response = ws.url(commentsUrl(Comment.Id(-1))).delete.futureValue
      assert(response.status === 404)
    }
  }

  lazy val ws: WSClient = app.injector.instanceOf(classOf[WSClient])

  val urlBase = s"http://localhost:$port"
  val random = new Random()

  val todosUrl: String = s"$urlBase/todos"
  def todosUrl(id: Todo.Id): String = s"$urlBase/todos/${id.unwrap}"

  def commentsUrl(todoId: Todo.Id): String = s"$urlBase/todos/${todoId.unwrap}/comments"
  def commentsUrl(id: Comment.Id): String = s"$urlBase/comments/${id.unwrap}"

  def getOrCreateTodo(checked: Option[Boolean] = None): Todo = {
    val response = ws.url(todosUrl).get.futureValue
    assert(response.status === 200)
    val allTodos = Json.parse(response.body).as[Vector[Todo]]
    val todoOpt = if (checked.isDefined) {
      allTodos.find(_.checked === checked.get)
    } else {
      allTodos.headOption
    }
    todoOpt match {
      case Some(t) => t
      case None => {
        val todo = checked match {
          case Some(c) => randomTodoCreate().copy(checked = c)
          case None => randomTodoCreate()
        }
        val response = ws.url(todosUrl).post(Json.toJson(todo)).futureValue
        assert(response.status === 201)
        Json.parse(response.body).as[Todo]
      }
    }
  }

  def createComment(todoId: Todo.Id): Comment = {
    val data = randomCommentCreate()
    val response = ws.url(commentsUrl(todoId)).post(Json.toJson(data)).futureValue
    assert(response.status === 201)
    Json.parse(response.body).as[Comment]
  }

  def randomText(minLength: Int = 0, maxLength: Int = 255): String = {
    val size = random.nextInt(maxLength - minLength) + minLength
    random.alphanumeric.take(size).mkString
  }

  def randomTodoCreate(): Todo.Create =
    Todo.Create(text = randomText(), checked = random.nextBoolean())

  def randomCommentCreate(): Comment.Create =
    Comment.Create(text = randomText())
}
