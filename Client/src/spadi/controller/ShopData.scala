package spadi.controller

import spadi.model.{Product, ProductBasePrice, ProductPrice, ProductPriceWithSale, SalesGroup, Shop}
import utopia.flow.datastructure.immutable.{Model, Value}
import utopia.flow.generic.ValueConversions._
import utopia.flow.util.CollectionExtensions._

/**
 * Stores all processed shop data locally
 * @author Mikko Hilpinen
 * @since 24.5.2020, v1.1
 */
object ShopData
{
	// ATTRIBUTES   -----------------------------
	
	private var _shopData = ShopsContainer.current.map { shop =>
		shop -> new ProductsContainer(s"shop-products-${shop.id}.json")
	}
	
	
	// COMPUTED ---------------------------------
	
	/**
	 * @return All current products
	 */
	def products =
	{
		// Reads product prices for each shop, combines them based on product id and wraps them in product models
		_shopData.flatMap { case (shop, productsContainer) => productsContainer.prices.map { shop -> _ } }
			.groupBy { _._2.productId }
			.map { case (productId, prices) =>
				val pricesByShop = prices.groupMap { _._1 } { _._2 }
				val cheapestPriceByShop = pricesByShop.view.mapValues { _.minBy { _.price } }.toMap
				Product(productId, cheapestPriceByShop)
			}.toVector
	}
	
	// TODO: Add update methods
	
	
	// NESTED   ---------------------------------
	
	private object ShopsContainer extends LocalModelsContainer[Shop]("shops.json", Shop)
	
	private class ProductsContainer(fileName: String)
		extends LocalContainer[(Vector[ProductBasePrice], Vector[SalesGroup], Vector[ProductPrice])](fileName)
	{
		// COMPUTED -----------------------------
		
		def basePrices = current._1
		
		def salesGroups = current._2
		
		private def fullPrices = current._3
		
		def prices =
		{
			val sales = salesGroups.map { g => g.salesGroupId -> g }.toMap
			val basesWithSales = basePrices.map { base => ProductPriceWithSale(base, sales.get(base.salesGroupId)) }
			basesWithSales ++ fullPrices
		}
		
		
		// IMPLEMENTED  -------------------------
		
		override protected def toJsonValue(item: (Vector[ProductBasePrice], Vector[SalesGroup], Vector[ProductPrice])) =
		{
			Model("base" -> basePrices.map { _.toModel }, "sales" -> salesGroups.map { _.toModel },
				"full" -> fullPrices.map { _.toModel })
		}
		
		override protected def fromJsonValue(value: Value) =
		{
			val model = value.getModel
			model("base").getVector.flatMap { _.model }.tryMap { ProductBasePrice(_) }.flatMap { base =>
				model("sales").getVector.flatMap { _.model }.tryMap { SalesGroup(_) }.flatMap { sales =>
					model("full").getVector.flatMap { _.model }.tryMap { ProductPrice(_) }.map { full =>
						(base, sales, full)
					}
				}
			}
		}
		
		override protected def empty = (Vector(), Vector(), Vector())
	}
}
