package controllers

import javax.inject.Inject

import models.{Comment, CommentRepository, Todo}
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}

import scala.concurrent.ExecutionContext

class CommentController @Inject()(commentRepo: CommentRepository, cc: ControllerComponents)(
    implicit ec: ExecutionContext)
    extends AbstractController(cc) {

  def create(todoId: Todo.Id) = Action.async(parse.json) { request =>
    commentRepo
      .insert(todoId, request.body.as[Comment.Create])
      .map(todo => Created(Json.toJson(todo)))
  }

  def delete(id: Comment.Id) = Action.async {
    commentRepo.delete(id).map {
      case false => NotFound
      case true => NoContent
    }
  }

  def list(todoId: Todo.Id) = Action.async {
    commentRepo.list(todoId).map(comments => Ok(Json.toJson(comments)))
  }

  def get(id: Comment.Id) = Action.async {
    commentRepo.getById(id).map {
      case Some(todo) => Ok(Json.toJson(todo))
      case None => NotFound
    }
  }
}
