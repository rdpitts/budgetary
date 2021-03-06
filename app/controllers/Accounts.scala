package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import models.Account
import anorm.{Pk, NotAssigned}
import play.api.data.format.Formats._

object Accounts extends Controller {

  val accountForm = Form(
    mapping(
      "id" -> ignored(NotAssigned: Pk[Long]),
      "token" -> nonEmptyText,
      "monthly_income" -> of[Double]
    )(Account.apply)(Account.unapply)
  )

  def showAccount(token: String) = Action { implicit request =>
    Account.findByTokenAndDo(token) { account =>
      Redirect(routes.Budgets.accountIndex(account.token))
    }
  }

  def newAccount = Action {
    val token: String = Account.generateToken
    Ok(views.html.Accounts.newAccount(accountForm.bind(Map("token" -> token)))(token))
  }

  def createAccount = Action { implicit request =>

    accountForm.bindFromRequest.fold(
      errors => {
        BadRequest(views.html.Accounts.newAccount(errors)(errors.apply("token").value.getOrElse("")))
      },
      account => {
        Account.create(account)
        Redirect(routes.Accounts.showAccount(account.token))
      }
    )

  }

}
