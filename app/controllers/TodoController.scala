package controllers

import javax.inject.Inject

import models.{Todo, TodoRepository}
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}

import scala.concurrent.ExecutionContext

class TodoController @Inject()(todoRepo: TodoRepository, cc: ControllerComponents)(
    implicit ec: ExecutionContext)
    extends AbstractController(cc) {

  def create = Action.async(parse.json) { request =>
    todoRepo.insert(request.body.as[Todo.Create]).map(todo => Created(Json.toJson(todo)))
  }

  def update(id: Todo.Id) = Action.async(parse.json) { request =>
    todoRepo.update(id, request.body.as[Todo.Create]).map {
      case Some(todo) => Ok(Json.toJson(todo))
      case None => NotFound
    }
  }

  def check(id: Todo.Id) = setChecked(id, true)
  def uncheck(id: Todo.Id) = setChecked(id, false)

  private def setChecked(id: Todo.Id, checked: Boolean) = Action.async {
    todoRepo.setChecked(id, checked).map {
      case Some(todo) => Ok(Json.toJson(todo))
      case None => NotFound
    }
  }

  def delete(id: Todo.Id) = Action.async {
    todoRepo.delete(id).map {
      case false => NotFound
      case true => NoContent
    }
  }

  def list = Action.async {
    todoRepo.list.map(todos => Ok(Json.toJson(todos)))
  }

  def get(id: Todo.Id) = Action.async {
    todoRepo.getById(id).map {
      case Some(todo) => Ok(Json.toJson(todo))
      case None => NotFound
    }
  }
}
