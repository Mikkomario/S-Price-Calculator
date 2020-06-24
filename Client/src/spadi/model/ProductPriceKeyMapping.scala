package spadi.model

import utopia.flow.datastructure.immutable.{Constant, Model, ModelDeclaration}
import utopia.flow.generic.{FromModelFactoryWithSchema, StringType}
import utopia.flow.generic.ValueConversions._
import utopia.flow.generic.ValueUnwraps._

object ProductPriceKeyMapping extends FromModelFactoryWithSchema[ProductPriceKeyMapping]
{
	// IMPLEMENTED  ---------------------------------
	
	override val schema = ModelDeclaration("id_key" -> StringType, "price_key" -> StringType,
		"per_amount_key" -> StringType, "per_unit_key" -> StringType)
	
	override protected def fromValidatedModel(model: Model[Constant]) = ProductPriceKeyMapping(model("id_key"),
		model("name_keys").getVector.flatMap { _.string }, model("price_key"), model("per_amount_key"),
		model("per_unit_key"))
}

/**
 * Used for mapping excel keys with product price properties
 * @author Mikko Hilpinen
 * @since 24.5.2020, v1.1
 */
case class ProductPriceKeyMapping(idKey: String, nameKeys: Vector[String], priceKey: String,
                                  perAmountKey: String, perUnitKey: String) extends KeyMapping[ProductPrice]
{
	override def requiredKeys = Set(idKey, priceKey)
	
	override protected def fromValidatedModel(model: Model[Constant]) =
	{
		val perUnit = model(perUnitKey).stringOr("kpl")
		val perAmount = model(perAmountKey).intOr(1)
		val unit = s"€/${if (perAmount == 1) "" else perAmount}$perUnit"
		ProductPrice(model(idKey), nameKeys.flatMap { nameKey => model(nameKey).string }, model(priceKey), unit)
	}
	
	override def toModel = Model(Vector("id_key" -> idKey, "name_keys" -> nameKeys,
		"price_key" -> priceKey, "per_amount_key" -> perAmountKey, "per_unit_key" -> perUnitKey))
}
