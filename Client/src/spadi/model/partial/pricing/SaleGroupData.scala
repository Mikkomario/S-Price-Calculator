package spadi.model.partial.pricing

object SaleGroupData
{
	/**
	  * @param shopId Id of shop that provides this sale
	  * @param groupIdentifier Identifier of this sale
	  * @param priceModifier Modifier applied to product prices [0, 1]
	  * @return A new sale group data
	  */
	def apply(shopId: Int, groupIdentifier: String, priceModifier: Double): SaleGroupData =
		apply(shopId, groupIdentifier, Some(priceModifier))
	
	/**
	  * Creates a sale group without known sale percentage
	  * @param shopId Id of the shop that provides this sale
	  * @param groupIdentifier Identifier of this sale
	  * @return A new sale group data without sale amount included
	  */
	def unknownAmount(shopId: Int, groupIdentifier: String) = apply(shopId, groupIdentifier, None)
}

/**
  * Contains basic information about a product sale group
  * @author Mikko Hilpinen
  * @since 31.7.2020, v1.2
  */
case class SaleGroupData(shopId: Int, groupIdentifier: String, priceModifier: Option[Double])
