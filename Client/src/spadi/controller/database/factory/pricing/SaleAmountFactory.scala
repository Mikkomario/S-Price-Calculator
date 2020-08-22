package spadi.controller.database.factory.pricing

import spadi.controller.database.Tables
import spadi.model.stored.pricing.SaleAmount
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.flow.generic.ValueUnwraps._
import utopia.vault.nosql.factory.{Deprecatable, FromValidatedRowModelFactory}

/**
  * Used for reading sale amounts from DB
  * @author Mikko Hilpinen
  * @since 31.7.2020, v1.2
  */
object SaleAmountFactory extends FromValidatedRowModelFactory[SaleAmount] with Deprecatable
{
	// ATTRIBUTES	-----------------------------
	
	val deprecationAttName = "deprecatedAfter"
	
	lazy val deprecationColumn = table(deprecationAttName)
	
	
	// IMPLEMENTED	-----------------------------
	
	override def table = Tables.saleAmount
	
	override protected def fromValidatedModel(model: Model[Constant]) =
		SaleAmount(model("id"), model("groupId"), model("priceModifier"))
	
	override lazy val nonDeprecatedCondition = deprecationColumn.isNull
}
