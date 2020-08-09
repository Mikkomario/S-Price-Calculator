package spadi.controller.database.factory.pricing

import spadi.controller.database.Tables
import spadi.model.cached.pricing.Price
import spadi.model.stored.pricing.NetPrice
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.flow.generic.ValueUnwraps._
import utopia.vault.nosql.factory.{Deprecatable, FromValidatedRowModelFactory}

/**
  * Used for reading product net price information from the DB
  * @author Mikko Hilpinen
  * @since 1.8.2020, v1.2
  */
object NetPriceFactory extends FromValidatedRowModelFactory[NetPrice] with Deprecatable
{
	override def table = Tables.netPrice
	
	override protected def fromValidatedModel(model: Model[Constant]) = NetPrice(model("id"), model("shopProductId"),
		Price(model("netPrice"), model("saleUnit").stringOr("kpl"), model("saleCount").intOr(1)))
	
	override lazy val nonDeprecatedCondition = table("deprecatedAfter").isNull
}
