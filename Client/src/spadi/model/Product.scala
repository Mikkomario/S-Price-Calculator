package spadi.model

import scala.math.Ordering.Double.TotalOrdering

/**
 * Contains all price information about a single product
 * @author Mikko Hilpinen
 * @since 9.5.2020, v1
 * @param id Product id
 * @param prices Available prices for this product (Must not be empty)
 */
case class Product(id: String, prices: Map[Shop, ProductPriceLike with Searchable]) extends ProductPriceLike with Searchable
{
	// ATTRIBUTES   --------------------------
	
	private val (cheapestShop, cheapestProduct) = prices.minBy { _._2.price }
	
	
	// IMPLEMENTED ---------------------------
	
	override def productId = id
	
	override def priceUnit = cheapestProduct.priceUnit
	
	def displayName = s"${cheapestProduct.displayName} (${cheapestShop.name})"
	
	def price = cheapestProduct.price
	
	def matches(search: Set[String]) = prices.valuesIterator.map { _.matches(search) }.max
}
