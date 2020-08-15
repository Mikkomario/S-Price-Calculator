package spadi.controller.database.factory.pricing

import spadi.controller.database.Tables
import spadi.model.stored.pricing.Shop
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.flow.generic.ValueUnwraps._
import utopia.vault.nosql.factory.FromValidatedRowModelFactory

/**
  * Used for reading shop data from the DB
  * @author Mikko Hilpinen
  * @since 2.8.2020, v1.2
  */
object ShopFactory extends FromValidatedRowModelFactory[Shop]
{
	override def table = Tables.shop
	
	override protected def fromValidatedModel(model: Model[Constant]) = Shop(model("id"), model("name"))
}
