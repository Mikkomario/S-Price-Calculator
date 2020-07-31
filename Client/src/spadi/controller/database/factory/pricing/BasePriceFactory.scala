package spadi.controller.database.factory.pricing

import spadi.controller.database.Tables
import spadi.model.cached.pricing.Price
import spadi.model.partial.pricing.BasePriceData
import spadi.model.stored.pricing.BasePrice
import utopia.flow.generic.ValueUnwraps._
import utopia.vault.model.immutable.Row
import utopia.vault.nosql.factory.{Deprecatable, FromRowFactory}
import utopia.vault.sql.JoinType

/**
  * Used for reading product base prices (including affecting sale) from DB
  * @author Mikko Hilpinen
  * @since 1.8.2020, v1.2
  */
object BasePriceFactory extends FromRowFactory[BasePrice] with Deprecatable
{
	// ATTRIBUTES	-------------------------
	
	lazy val deprecationColumn = table("deprecatedAfter")
	
	
	// IMPLEMENTED	-------------------------
	
	override def apply(row: Row) = table.requirementDeclaration.validate(row(table)).toTry.map { valid =>
		BasePrice(valid("id"), valid("productId"), valid("shopId"), BasePriceData(Price(valid("basePrice"),
			valid("saleUnit").stringOr("kpl"), valid("saleCount").intOr(1)),
			valid("saleGroupIdentifier")), SaleGroupFactory.parseIfPresent(row))
	}
	
	override def table = Tables.basePrice
	
	override lazy val joinedTables = Tables.basePriceSaleLink +: SaleGroupFactory.tables
	
	override def joinType = JoinType.Left
	
	override lazy val nonDeprecatedCondition = deprecationColumn.isNull && SaleGroupFactory.nonDeprecatedCondition
}
