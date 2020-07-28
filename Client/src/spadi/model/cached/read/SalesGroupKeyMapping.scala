package spadi.model.cached.read

import spadi.model.cached.pricing.product.SalesGroup
import utopia.flow.datastructure.immutable.{Constant, Model, ModelDeclaration}
import utopia.flow.generic.ValueConversions._
import utopia.flow.generic.ValueUnwraps._
import utopia.flow.generic.{FromModelFactoryWithSchema, StringType}
import utopia.flow.util.StringExtensions._

object SalesGroupKeyMapping extends FromModelFactoryWithSchema[SalesGroupKeyMapping]
{
	override val schema = ModelDeclaration("id_key" -> StringType, "name_key" -> StringType,
		"sale_percent_key" -> StringType)

	override protected def fromValidatedModel(m: Model[Constant]) =
	{
		SalesGroupKeyMapping(m("id_key"), m("name_key"), m("sale_percent_key"), m("producer_key"))
	}
}

/**
 * Used for mapping excel keys into sales group properties
 * @author Mikko Hilpinen
 * @since 8.5.2020, v1
 * @param groupIdKey Key that contains sales group id
 * @param nameKey Key that contains group name
 * @param salePercentKey Key that contains sale amount in negative percentages
 * @param producerKey Key that contains product producer (optional)
 */
case class SalesGroupKeyMapping(groupIdKey: String, nameKey: String, salePercentKey: String,
																producerKey: Option[String] = None)
	extends KeyMapping[SalesGroup]
{
	override def toModel = Model(Vector("id_key" -> groupIdKey, "name_key" -> nameKey,
		"sale_percent_key" -> salePercentKey, "producer_key" -> producerKey))
	
	override def requiredKeys = Set(groupIdKey, nameKey, salePercentKey)
	
	override protected def fromValidatedModel(model: Model[Constant]) =
	{
		val salePercent = model(salePercentKey).getString.untilFirst("%").getInt
		val priceModifier = (100 - salePercent.abs) / 100.0
		SalesGroup(model(groupIdKey), model(nameKey), priceModifier, producerKey.flatMap { model(_) })
	}
}
