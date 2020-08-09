package spadi.controller.database.factory.pricing

import spadi.controller.database.Tables
import spadi.model.stored.pricing.{Product, ShopProductInfo}
import utopia.flow.datastructure.immutable.{Constant, Model, Value}
import utopia.flow.generic.ValueUnwraps._
import utopia.vault.nosql.factory.{Deprecatable, PossiblyMultiLinkedFactory}

/**
  * Used for reading complete product data from DB
  * @author Mikko Hilpinen
  * @since 1.8.2020, v1.2
  */
object ProductFactory extends PossiblyMultiLinkedFactory[Product, ShopProductInfo] with Deprecatable
{
	// ATTRIBUTES	-------------------------
	
	val electricIdAttName = "electricId"
	
	lazy val electricIdColumn = table(electricIdAttName)
	
	
	// IMPLEMENTED	-------------------------
	
	override def table = Tables.product
	
	override def childFactory = ShopProductFactory
	
	override def apply(id: Value, model: Model[Constant], children: Seq[ShopProductInfo]) =
	{
		table.requirementDeclaration.validate(model).toTry.map { valid =>
			Product(id, valid(electricIdAttName), children.toSet)
		}
	}
	
	override def nonDeprecatedCondition = childFactory.nonDeprecatedCondition
}
