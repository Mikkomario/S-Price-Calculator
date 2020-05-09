package spadi.model

import utopia.flow.datastructure.immutable.{Constant, Model, ModelDeclaration}
import utopia.flow.generic.{DoubleType, FromModelFactoryWithSchema, ModelConvertible, StringType}
import utopia.flow.generic.ValueConversions._

object SalesGroup extends FromModelFactoryWithSchema[SalesGroup]
{
	override val schema = ModelDeclaration("id" -> StringType, "name" -> StringType, "price_modifier" -> DoubleType)
	
	override protected def fromValidatedModel(model: Model[Constant]) = SalesGroup(model("id").getString,
		model("name").getString, model("price_modifier").getDouble, model("producer").string)
}

/**
 * Represents a sale modifier applied to a group of products
 * @author Mikko Hilpinen
 * @since 8.5.2020, v1
 * @param salesGroupId Id of this sales group
 * @param name Name of this sales group
 * @param priceModifier Modifier applied to product prices in this sales group
 * @param producerName Name of the product producer (optional)
 */
case class SalesGroup(salesGroupId: String, name: String, priceModifier: Double, producerName: Option[String] = None)
	extends ModelConvertible
{
	// IMPLEMENTED  -------------------------------
	
	override def toModel = Model("id" -> salesGroupId, "name" -> name, "producer" -> producerName,
		"price_modifier" -> priceModifier)
	
	
	// OTHER    -----------------------------------
	
	/**
	 * @param search Search words
	 * @return How well this sales group matches that search
	 */
	def matches(search: Set[String]) = search.count { s => salesGroupId.toLowerCase.contains(s) ||
		name.toLowerCase.contains(s) || producerName.exists { _.toLowerCase.contains(s) } }
}
