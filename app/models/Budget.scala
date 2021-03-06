package models

import anorm._
import anorm.SqlParser._
import play.api.db._
import play.api.Play.current

case class Budget(id: Pk[Long], name: String, amount: Double, categoryId: Long)

object Budget {

  val budgetParser = {
    get[Pk[Long]]("id") ~
    get[String]("name") ~
    get[Double]("amount") ~
    get[Long]("category_id") map {
      case id~name~amount~categoryId => Budget(id, name, amount, categoryId)
    }
  }

  def all(): List[Budget] = DB.withConnection { implicit connection =>
    SQL("SELECT * FROM budgets").as(budgetParser *)
  }

  def allInAccount(token: String): List[Budget] = DB.withConnection { implicit connection =>
    SQL("SELECT budgets.* FROM budgets " +
          "INNER JOIN categories ON budgets.category_id = categories.id " +
          "INNER JOIN accounts ON categories.account_id = accounts.id " +
          "WHERE accounts.token = {token}").on(
      'token -> token
    ).as(budgetParser *)
  }

  def withoutCategory(): List[Budget] = DB.withConnection { implicit connection =>
    SQL("SELECT * FROM budgets WHERE category_id = NULL").as(budgetParser *)
  }

  def findById(id: Long): Option[Budget] = DB.withConnection { implicit connection =>
    SQL("SELECT * FROM budgets WHERE id = {id}").on(
      'id -> id
    ).as(budgetParser.singleOpt)
  }

  def create(budget: Budget) {
    DB.withConnection { implicit connection =>
      SQL("INSERT INTO budgets (name, amount, category_id) VALUES ({name}, {amount}, {categoryId})").on(
        'name -> budget.name,
        'amount -> budget.amount,
        'categoryId -> budget.categoryId
      ).executeUpdate()
    }
  }

  def update(id: Long, budget: Budget) {
    DB.withConnection { implicit connection =>
      SQL("UPDATE budgets SET name = {name}, amount = {amount}, category_id = {categoryId} WHERE id = {id}").on(
        'id -> id,
        'name -> budget.name,
        'amount -> budget.amount,
        'categoryId -> budget.categoryId
      ).executeUpdate()
    }
  }

  def destroy(id: Long): Option[Budget] = {
    val budget = Budget.findById(id)
    DB.withConnection { implicit connection =>
      SQL("DELETE FROM budgets WHERE id = {id}").on('id -> id).executeUpdate()
    } match {
      case 0 => None
      case 1 => budget
    }
  }

  def accountTotal(token: String): Double = {
    allInAccount(token).foldLeft(0.toDouble) { (z, a) =>
      z + a.amount
    }
  }

}
