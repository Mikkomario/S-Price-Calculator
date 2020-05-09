package spadi.model

import utopia.flow.datastructure.immutable.{Constant, Model, ModelDeclaration}
import utopia.flow.generic.{FromModelFactoryWithSchema, ModelConvertible, StringType}
import utopia.flow.generic.ValueConversions._

object SalesGroupKeyMapping extends FromModelFactoryWithSchema[SalesGroupKeyMapping]
{
	/**
	 * Mapping used when no other mappings are available
	 */
	val default = SalesGroupKeyMapping("Alennusryhmä", "Alennusryhmänimi", "Ale%", Some("Valmistaja"))

	override val schema = ModelDeclaration("id_key" -> StringType, "name_key" -> StringType,
		"sale_percent_key" -> StringType)

	override protected def fromValidatedModel(m: Model[Constant]) =
	{
		import utopia.flow.generic.ValueUnwraps._
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
																producerKey: Option[String] = None) extends ModelConvertible
{
	override def toModel = Model(Vector("id_key" -> groupIdKey, "name_key" -> nameKey,
		"sale_percent_key" -> salePercentKey, "producer_key" -> producerKey))
}
