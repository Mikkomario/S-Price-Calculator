package spadi.model

/**
 * Contains product price information and also takes effective sale into account
 * @author Mikko Hilpinen
 * @since 9.5.2020, v1
 * @param basePrice Product price without sale applied
 * @param sale Sale to apply to the price (optional)
 */
case class ProductSalePrice(basePrice: ProductPrice, sale: Option[SalesGroup])
{
	// COMPUTED -------------------------
	
	/**
	 * @return Product price
	 */
	def price =
	{
		val base = basePrice.price
		sale match
		{
			case Some(sale) => base * sale.priceModifier
			case None => base
		}
	}
	
	/**
	 * @return Name that should be displayed for this product
	 */
	def displayName =
	{
		val base = basePrice.displayName
		sale.flatMap { _.producerName } match
		{
			case Some(producer) => s"$base ($producer)"
			case None => base
		}
	}
	
	
	// OTHER    ----------------------------
	
	/**
	 * @param search Search words
	 * @return How well this product matches specified search
	 */
	def matches(search: Set[String]) = basePrice.matches(search) * 2 + sale.map { _.matches(search) }.getOrElse(0)
}
