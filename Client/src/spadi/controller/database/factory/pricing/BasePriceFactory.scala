package spadi.controller.database.factory.pricing

import spadi.controller.database.Tables
import spadi.model.cached.pricing.Price
import spadi.model.stored.pricing.{BasePrice, SaleGroup}
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.flow.generic.ValueUnwraps._
import utopia.vault.nosql.factory.{Deprecatable, PossiblyLinkedFactory}

/**
  * Used for reading product base prices (including affecting sale) from DB
  * @author Mikko Hilpinen
  * @since 1.8.2020, v1.2
  */
object BasePriceFactory extends PossiblyLinkedFactory[BasePrice, SaleGroup] with Deprecatable
{
	// ATTRIBUTES	-------------------------
	
	lazy val deprecationColumn = table("deprecatedAfter")
	
	
	// IMPLEMENTED	-------------------------
	
	override def childFactory = SaleGroupFactory
	
	override def apply(model: Model[Constant], child: Option[SaleGroup]) =
	{
		table.requirementDeclaration.validate(model).toTry.map { valid =>
			BasePrice(valid("id"), valid("shopProductId"), Price(valid("basePrice"),
				valid("saleUnit").stringOr("kpl"), valid("saleCount").intOr(1)), child)
		}
	}
	
	override def table = Tables.basePrice
	
	override lazy val nonDeprecatedCondition = deprecationColumn.isNull && SaleGroupFactory.nonDeprecatedCondition
}
