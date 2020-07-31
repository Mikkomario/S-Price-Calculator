package spadi.controller.database.factory.pricing

import spadi.controller.database.Tables
import spadi.model.partial.pricing.ProductNameData
import spadi.model.stored.pricing.ProductName
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.flow.generic.ValueUnwraps._
import utopia.vault.nosql.factory.{Deprecatable, FromValidatedRowModelFactory}

/**
  * Used for reading product name data from DB
  * @author Mikko Hilpinen
  * @since 1.8.2020, v1.2
  */
object ProductNameFactory extends FromValidatedRowModelFactory[ProductName] with Deprecatable
{
	override lazy val nonDeprecatedCondition = table("deprecatedAfter").isNull
	
	override def table = Tables.productName
	
	override protected def fromValidatedModel(model: Model[Constant]) = ProductName(model("id"), model("productId"),
		model("shopId"), ProductNameData(model("namePrimary"), model("nameAlternative")))
}
