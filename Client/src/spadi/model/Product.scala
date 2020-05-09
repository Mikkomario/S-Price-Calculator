package spadi.model

/**
 * Contains all price information about a single product
 * @author Mikko Hilpinen
 * @since 9.5.2020, v1
 * @param id Product id
 * @param prices Available prices for this product
 */
case class Product(id: String, prices: Set[ProductSalePrice])
