package spadi.controller.database.access.multi

import spadi.controller.database.access.id.{ProductId, ProductIds, ShopProductIds}
import spadi.controller.database.access.single.{DbProductBasePrice, DbProductName, DbProductNetPrice, DbShopProduct}
import spadi.controller.database.factory.pricing.ProductFactory
import spadi.controller.database.model.pricing.{ProductModel, ShopProductModel}
import spadi.model.partial.pricing.{ProductData, ShopProductData}
import spadi.model.stored.pricing.Product
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.ManyModelAccess
import utopia.vault.sql.Extensions._

/**
  * Used for accessing multiple products at once
  * @author Mikko Hilpinen
  * @since 1.8.2020, v1.2
  */
object DbProducts extends ManyModelAccess[Product]
{
	// IMPLEMENTED	--------------------------
	
	override def factory = ProductFactory
	
	override def globalCondition = Some(factory.nonDeprecatedCondition)
	
	
	// COMPUTED	------------------------------
	
	private def model = ProductModel
	
	private def shopProductModel = ShopProductModel
	
	private def idColumn = factory.table.primaryColumn.get
	
	
	// OTHER	------------------------------
	
	/**
	  * @param productIds A set of product ids
	  * @param connection DB Connection (implicit)
	  * @return Product data for products with those ids
	  */
	def withIds(productIds: Iterable[Int])(implicit connection: Connection) =
		read(Some(idColumn.in(productIds)))
	
	def insertData(data: Vector[ShopProductData])(implicit connection: Connection) =
	{
		// In case there are multiple shops, handles each one separately
		data.groupBy { _.shopId }.foreach { case (shopId, data) =>
			val electricIds = data.map { _.electricId }.sorted
			// Finds existing shop product ids matching specified electric ids
			val existingShopProductIds = ShopProductIds.forElectricIdsBetween(shopId, electricIds.head, electricIds.last)
			
			// TODO: Handle case where no new electric id inserts are required
			// Finds electric ids that don't have a matching shop product id yet
			val electricIdsMissingProductIdSet = electricIds.toSet -- existingShopProductIds.keySet
			val electricIdsMissingShopProductId = electricIdsMissingProductIdSet.toVector.sorted
			// Finds existing product ids for those electric ids
			val existingProductIds = ProductIds.forElectricIdsBetween(
				electricIdsMissingShopProductId.head, electricIdsMissingShopProductId.last)
			// Inserts new product rows to make sure all electric ids are covered
			val insertedProductIds = (electricIdsMissingProductIdSet -- existingProductIds.keySet).map { electricId =>
				electricId -> model.insertEmptyProduct(electricId)
			}
			val productIds = existingProductIds ++ insertedProductIds
			
			// Updates or inserts shop product rows
			val shopProductIds = data.map { product =>
				existingShopProductIds.get(product.electricId) match
				{
					case Some(shopProductId) =>
						shopProductModel.withName(product.name).withAlternativeName(product.alternativeName).nowUpdated
							.updateWhere(shopProductModel.withId(shopProductId).toCondition)
						product.electricId -> shopProductId
					case None =>
						product.electricId -> shopProductModel.insert(productIds(product.electricId), shopId,
							product.name, product.alternativeName).id
				}
			}
			
			// TODO: Split into base and net prices. Insert or update each (deprecating old rows first).
		}
	}
	
	/*
	  * Inserts new product data
	  * @param data Product data to insert or update
	  * @param connection DB Connection (implicit)
	  */
	// TODO: Replace this with a version that inserts multiple products
	/*
	def insertData(data: ProductData)(implicit connection: Connection) =
	{
		// First makes sure the product row exists
		val productId = ProductId.forElectricIdentifier(data.electricId)
		// TODO: Remove test prints. Also, create a more optimized version of this method
		//  (inserting multiple products at once with minimal queries)
		println(s"Inserting data for product $productId (${data.electricId})")
		
		// Next inserts name, base price and net price data for each shop where applicable
		data.shopData.foreach { case (shopId, shopData) =>
			// FIXME: Insert or find the shop product first, then update prices
			
			DbProductName.forProductWithId(productId).inShopWithId(shopId)
				.set(shopData.name.name, shopData.name.alternativeName)
			shopData.basePrice.foreach { DbProductBasePrice.forProductWithId(productId).inShopWithId(shopId).set(_) }
			shopData.netPrice.foreach { DbProductNetPrice.forProductWithId(productId).inShopWithId(shopId).set(_) }
		}
	}*/
}
