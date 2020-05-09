package spadi.model

/**
 * Contains product price information and also takes effective sale into account
 * @author Mikko Hilpinen
 * @since 9.5.2020, v1
 * @param basePrice Product price without sale applied
 * @param sale Sale to apply to the price (optional)
 */
case class ProductSalePrice(basePrice: ProductPrice, sale: Option[SalesGroup])
