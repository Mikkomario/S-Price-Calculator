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
	// ATTRIBUTES	-------------------------------
	
	val productIdAttName = "productId"
	
	val nameAttName = "namePrimary"
	
	val alternativeNameAttName = "nameAlternative"
	
	/**
	  * @return Column that contains the product name
	  */
	lazy val productIdColumn = table(productIdAttName)
	
	lazy val nameColumn = table(nameAttName)
	
	lazy val alternativeNameColumn = table(alternativeNameAttName)
	
	
	// IMPLEMENTED	-------------------------------
	
	override lazy val nonDeprecatedCondition = table("deprecatedAfter").isNull
	
	override def table = Tables.productName
	
	override protected def fromValidatedModel(model: Model[Constant]) = ProductName(model("id"), model(productIdAttName),
		model("shopId"), ProductNameData(model(nameAttName), model(alternativeNameAttName)))
}
