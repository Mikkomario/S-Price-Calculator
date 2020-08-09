package spadi.controller.database.access.multi

import spadi.controller.database.access.id.ProductId
import spadi.controller.database.access.single.{DbProductBasePrice, DbProductName, DbProductNetPrice}
import spadi.controller.database.factory.pricing.ProductFactory
import spadi.model.partial.pricing.ProductData
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
	
	private def idColumn = factory.table.primaryColumn.get
	
	
	// OTHER	------------------------------
	
	/**
	  * @param productIds A set of product ids
	  * @param connection DB Connection (implicit)
	  * @return Product data for products with those ids
	  */
	def withIds(productIds: Iterable[Int])(implicit connection: Connection) =
		read(Some(idColumn.in(productIds)))
	
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
