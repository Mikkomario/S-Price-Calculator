package spadi.model.partial.reading

import spadi.model.cached.pricing.Price
import spadi.model.cached.read.KeyMapping
import spadi.model.enumeration.PriceType
import spadi.model.enumeration.PriceType.{Base, Net}
import spadi.model.partial.pricing.{BasePriceData, ProductData, ShopProductData}
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.flow.generic.ValueConversions._
import utopia.flow.generic.ValueUnwraps._

/**
  * Contains basic information about a price key mapping
  * @author Mikko Hilpinen
  * @since 3.8.2020, v1.2
  */
case class PriceKeyMappingData(priceType: PriceType, shopId: Int, electricIdKey: String, nameKey: String,
							   alternativeNameKey: Option[String], priceKey: String, saleUnitKey: Option[String],
							   saleCountKey: Option[String], saleGroupKey: Option[String])
	extends KeyMapping[ProductData]
{
	override def toModel = Model(Vector("type_id" -> priceType.id, "shop_id" -> shopId,
		"electric_id_key" -> electricIdKey, "name_key" -> nameKey, "alternative_name_key" -> alternativeNameKey,
		"price_key" -> priceKey, "sale_unit_key" -> saleUnitKey, "sale_count_key" -> saleCountKey,
		"sale_group_key" -> saleGroupKey))
	
	override def requiredKeys = Set(electricIdKey, nameKey, priceKey)
	
	override protected def fromValidatedModel(model: Model[Constant]) =
	{
		val price = Price(model(priceKey), saleUnitKey.flatMap { model(_).string }.getOrElse("kpl"),
			saleCountKey.flatMap { model(_).int }.getOrElse(1))
		val name = model(nameKey).getString
		val altName = alternativeNameKey.flatMap { model(_).string }
		
		val shopProductData = priceType match
		{
			case Net => ShopProductData.netPrice(name, altName, price)
			case Base => ShopProductData.basePrice(name, altName, BasePriceData(price, saleGroupKey.flatMap { model(_) }))
		}
		
		ProductData(model(electricIdKey), shopId, shopProductData)
	}
}
