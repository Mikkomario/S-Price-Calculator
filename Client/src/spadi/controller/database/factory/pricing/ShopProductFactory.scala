package spadi.controller.database.factory.pricing

import spadi.controller.database.Tables
import spadi.model.stored.pricing.ShopProduct
import utopia.flow.generic.ValueUnwraps._
import utopia.vault.model.immutable.Row
import utopia.vault.nosql.factory.{Deprecatable, FromRowFactory}
import utopia.vault.sql.JoinType

/**
  * Used for reading shop product data from DB
  * @author Mikko Hilpinen
  * @since 9.8.2020, v1.2
  */
object ShopProductFactory extends FromRowFactory[ShopProduct] with Deprecatable
{
	// ATTRIBUTES	------------------------------
	
	val productIdAttName = "productId"
	
	/**
	  * Name of the attribute that contains linked shop's id
	  */
	val shopIdAttName = "shopId"
	
	val nameAttName = "name"
	
	val alternativeNameAttName = "nameAlternative"
	
	lazy val productIdColumn = table(productIdAttName)
	
	lazy val nameColumn = table(nameAttName)
	
	lazy val alternativeNameColumn = table(alternativeNameAttName)
	
	
	// IMPLEMENTED	------------------------------
	
	override def nonDeprecatedCondition = NetPriceFactory.nonDeprecatedCondition &&
		BasePriceFactory.nonDeprecatedCondition
	
	override def apply(row: Row) = table.requirementDeclaration.validate(row(table)).toTry.map { model =>
		val netPrice = NetPriceFactory.parseIfPresent(row)
		val basePrice = BasePriceFactory.parseIfPresent(row)
		
		ShopProduct(model("id"), model(productIdAttName), model(shopIdAttName), model(nameAttName),
			model(alternativeNameAttName), basePrice, netPrice)
	}
	
	override def table = Tables.shopProduct
	
	override def joinedTables = NetPriceFactory.tables ++ BasePriceFactory.tables
	
	override def joinType = JoinType.Left
}
