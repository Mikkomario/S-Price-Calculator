package spadi.model.cached.read

import spadi.model.cached.pricing.product.ProductBasePrice
import utopia.flow.datastructure.immutable.{Constant, Model, ModelDeclaration}
import utopia.flow.generic.ValueConversions._
import utopia.flow.generic.ValueUnwraps._
import utopia.flow.generic.{FromModelFactoryWithSchema, StringType}

object ProductBasePriceKeyMapping extends FromModelFactoryWithSchema[ProductBasePriceKeyMapping]
{
	// IMPLEMENTED  ---------------------------
	
	override val schema = ModelDeclaration("id_key" -> StringType, "group_id_key" -> StringType,
		"price_key" -> StringType, "per_amount_key" -> StringType, "per_unit_key" -> StringType)

	override protected def fromValidatedModel(model: Model[Constant]) = ProductBasePriceKeyMapping(model("id_key"),
		model("group_id_key"), model("name_keys").getVector.flatMap { _.string }, model("price_key"),
		model("per_amount_key"), model("per_unit_key"))
}

/**
 * Contains mappings between excel keys and product price properties
 * @author Mikko Hilpinen
 * @since 8.5.2020, v1
 * @param idKey Key that contains product id
 * @param groupIdKey Key that contains product group id
 * @param nameKeys Keys for product names
 * @param priceKey Key for product price
 * @param perAmountKey Key for price divider
 * @param perUnitKey Key for price divider unit
 */
case class ProductBasePriceKeyMapping(idKey: String, groupIdKey: String, nameKeys: Vector[String], priceKey: String,
                                      perAmountKey: String, perUnitKey: String)
	extends KeyMapping[ProductBasePrice]
{
	override def toModel = Model(Vector("id_key" -> idKey, "group_id_key" -> groupIdKey,
		"name_keys" -> nameKeys, "price_key" -> priceKey, "per_amount_key" -> perAmountKey, "per_unit_key" -> perUnitKey))
	
	override def requiredKeys = Set(idKey, priceKey)
	
	override protected def fromValidatedModel(model: Model[Constant]) =
	{
		val perUnit = model(perUnitKey).stringOr("kpl")
		val perAmount = model(perAmountKey).intOr(1)
		ProductBasePrice(model(idKey), model(groupIdKey), nameKeys.flatMap { nameKey => model(nameKey).string },
			model(priceKey), perAmount, perUnit)
	}
}
