package spadi.controller.database.factory.pricing

import spadi.controller.database.Tables
import spadi.model.stored.pricing.{SaleAmount, SaleGroup}
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.flow.generic.ValueUnwraps._
import utopia.vault.nosql.factory.{Deprecatable, PossiblyLinkedFactory}

/**
  * Used for reading sale group data from DB
  * @author Mikko Hilpinen
  * @since 31.7.2020, v1.2
  */
object SaleGroupFactory extends PossiblyLinkedFactory[SaleGroup, SaleAmount] with Deprecatable
{
	// ATTRIBUTES	------------------------------
	
	val groupIdentifierAttName = "groupIdentifier"
	
	lazy val groupIdentifierColumn = table(groupIdentifierAttName)
	
	
	// IMPLEMENTED	------------------------------
	
	override def childFactory = SaleAmountFactory
	
	override def apply(model: Model[Constant], child: Option[SaleAmount]) =
		table.requirementDeclaration.validate(model).toTry.map { valid =>
			SaleGroup(valid("id"), valid("shopId"), valid(groupIdentifierAttName), child)
		}
	
	override def table = Tables.saleGroup
	
	override def nonDeprecatedCondition = childFactory.nonDeprecatedCondition
}
