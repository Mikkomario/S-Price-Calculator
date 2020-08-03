package spadi.model.partial.reading

import spadi.model.enumeration.PriceType

/**
  * Contains basic information about a price key mapping
  * @author Mikko Hilpinen
  * @since 3.8.2020, v1.2
  */
case class PriceKeyMappingData(priceType: PriceType, shopId: Int, electricIdKey: String, nameKey: String,
							   alternativeNameKey: Option[String], priceKey: String, saleUnitKey: Option[String],
							   saleCountKey: Option[String], saleGroupKey: Option[String])
