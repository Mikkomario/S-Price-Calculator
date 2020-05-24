package spadi.model

/**
 * Contains product price information and also takes effective sale into account
 * @author Mikko Hilpinen
 * @since 9.5.2020, v1
 * @param basePrice Product price without sale applied
 * @param sale Sale to apply to the price (optional)
 */
case class ProductPriceWithSale(basePrice: ProductBasePrice, sale: Option[SalesGroup])
	extends ProductPriceLike with Searchable
{
	// IMPLEMENTED  ------------------------
	
	override def productId = basePrice.productId
	
	def price =
	{
		val base = basePrice.price
		sale match
		{
			case Some(sale) => base * sale.priceModifier
			case None => base
		}
	}
	
	override def priceUnit = basePrice.priceUnit
	
	def displayName =
	{
		val base = basePrice.displayName
		sale.flatMap { _.producerName } match
		{
			case Some(producer) => s"$base ($producer)"
			case None => base
		}
	}
	
	def matches(search: Set[String]) = basePrice.matches(search) * 2 + sale.map { _.matches(search) }.getOrElse(0)
}
