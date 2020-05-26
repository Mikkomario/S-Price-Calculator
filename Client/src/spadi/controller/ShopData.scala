package spadi.controller

import spadi.model.{Product, ProductBasePrice, ProductPrice, ProductPriceWithSale, SalesGroup, Shop, ShopSetup}
import utopia.flow.datastructure.immutable.{Model, Value}
import utopia.flow.generic.ValueConversions._
import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.FileExtensions._

import scala.collection.immutable.VectorBuilder
import scala.math.Ordering.Double.TotalOrdering

/**
 * Stores all processed shop data locally
 * @author Mikko Hilpinen
 * @since 24.5.2020, v1.1
 */
object ShopData
{
	// ATTRIBUTES   -----------------------------
	
	private var _shopData = ShopsContainer.shops.map { shop =>
		shop -> new ProductsContainer(productsFileNameForShop(shop))
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
	
	/**
	 * @return Currently registered shop setups
	 */
	def shopSetups = ShopsContainer.current
	
	/**
	 * Overwrites the current products list
	 * @param newProducts New products
	 */
	def updateProducts(newProducts: Iterable[(Shop, Either[(Vector[ProductBasePrice], Vector[SalesGroup]), Vector[ProductPrice]])]) =
	{
		val groupedProducts = newProducts.groupMap { _._1 } { _._2 }.view.mapValues { products =>
			// Groups the models based on type
			val baseBuilder = new VectorBuilder[ProductBasePrice]
			val salesBuilder = new VectorBuilder[SalesGroup]
			val comboBuilder = new VectorBuilder[ProductPrice]
			
			products.foreach
			{
				case Right(combo) => comboBuilder ++= combo
				case Left((base, sales)) =>
					baseBuilder ++= base
					salesBuilder ++= sales
			}
			
			(baseBuilder.result(), salesBuilder.result(), comboBuilder.result())
		}.toMap
		
		// Updates existing shop containers and possibly adds new ones
		val newContainersBuilder = new VectorBuilder[(Shop, ProductsContainer)]
		groupedProducts.foreach { case (shop, products) =>
			_shopData.find { _._1.id == shop.id }.map { _._2 } match
			{
				case Some(container) => container.current = products
				case None =>
					val newContainer = new ProductsContainer(productsFileNameForShop(shop))
					newContainer.current = products
					newContainersBuilder += (shop -> newContainer)
			}
		}
		val newContainers = newContainersBuilder.result()
		
		if (newContainers.nonEmpty)
			_shopData ++= newContainers
		
		// Deletes containers that were not included
		val shopsToDelete = _shopData.filterNot { case (shop, _) => groupedProducts.exists { case (newShop, _) =>
			shop.id == newShop.id } }
		if (shopsToDelete.nonEmpty)
		{
			shopsToDelete.foreach { _._2.fileLocation.delete() }
			_shopData = _shopData.filterNot { case (shop, _) => shopsToDelete.exists { _._1 == shop } }
		}
	}
	
	
	// OTHER    ---------------------------------
	
	/**
	 * @param shopId A shop id
	 * @return A shop matching that id
	 */
	def shopForId(shopId: String) = ShopsContainer.shops.find { _.id == shopId }
	
	private def productsFileNameForShop(shop: Shop) = s"shop-products-${shop.id}.json"
	
	
	// NESTED   ---------------------------------
	
	private object ShopsContainer extends LocalModelsContainer[ShopSetup]("shops.json", ShopSetup)
	{
		/**
		 * @return All shops currently in this container
		 */
		def shops = current.map { _.shop }
	}
	
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
