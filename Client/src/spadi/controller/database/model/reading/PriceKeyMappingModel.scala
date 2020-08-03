package spadi.controller.database.model.reading

import spadi.controller.database.factory.reading.PriceKeyMappingFactory
import spadi.model.enumeration.PriceType
import spadi.model.partial.reading.PriceKeyMappingData
import spadi.model.stored.reading.PriceKeyMapping
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.model.immutable.StorableWithFactory

object PriceKeyMappingModel
{
	/**
	  * @param priceType Type of described price mapping
	  * @return A model with only price type set
	  */
	def withPriceType(priceType: PriceType) = apply(priceType = Some(priceType))
	
	/**
	  * @param shopId Id of the described shop
	  * @return A model with only shop id set
	  */
	def withShopId(shopId: Int) = apply(shopId = Some(shopId))
	
	/**
	  * Inserts a new price key mapping to DB
	  * @param data Data to insert
	  * @param connection DB Connection
	  * @return Newly inserted mapping
	  */
	def insert(data: PriceKeyMappingData)(implicit connection: Connection) =
	{
		val id  = apply(None, Some(data.priceType), Some(data.shopId), Some(data.electricIdKey), Some(data.nameKey),
			data.alternativeNameKey, Some(data.priceKey), data.saleUnitKey, data.saleCountKey, data.saleGroupKey)
			.insert().getInt
		PriceKeyMapping(id, data)
	}
}

/**
  * Used for interacting with price key mappings in DB
  * @author Mikko Hilpinen
  * @since 3.8.2020, v1.2
  */
case class PriceKeyMappingModel(id: Option[Int] = None, priceType: Option[PriceType] = None, shopId: Option[Int] = None,
								electricIdKey: Option[String] = None, nameKey: Option[String] = None,
								alternativeNameKey: Option[String] = None, priceKey: Option[String] = None,
								unitKey: Option[String] = None, saleCountKey: Option[String] = None,
								saleGroupKey: Option[String] = None) extends StorableWithFactory[PriceKeyMapping]
{
	override def factory = PriceKeyMappingFactory
	
	override def valueProperties = Vector("id" -> id, "typeIdentifier" -> priceType.map { _.id }, "shopId" -> shopId,
		"electricIdKey" -> electricIdKey, "productNameKey" -> nameKey, "productNameKeyAlternative" -> alternativeNameKey,
		"priceKey" -> priceKey, "saleUnitKey" -> unitKey, "saleCountKey" -> saleCountKey, "saleGroupKey" -> saleGroupKey)
}
