package spadi.model.partial.reading

import spadi.model.cached.read.KeyMapping
import spadi.model.partial.pricing.SaleGroupData
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.flow.generic.ValueConversions._
import utopia.flow.generic.ValueUnwraps._
import utopia.flow.util.StringExtensions._

/**
  * Contains mappings between sale group fields and document headers
  * @author Mikko Hilpinen
  * @since 3.8.2020, v1.2
  */
case class SaleKeyMappingData(shopId: Int, groupIdKey: String, salePercentKey: String) extends KeyMapping[SaleGroupData]
{
	override def requiredKeys = Set(groupIdKey, salePercentKey)
	
	override def toModel = Model(Vector("shop_id" -> shopId, "group_id_key" -> groupIdKey,
		"sale_percent_key" -> salePercentKey))
	
	override protected def fromValidatedModel(model: Model[Constant]) =
	{
		val salePercent = model(salePercentKey).getString.untilFirst("%").getInt
		val priceModifier = (100 - salePercent.abs) / 100.0
		
		SaleGroupData(shopId, model(groupIdKey), priceModifier)
	}
}
