package spadi.controller.database.access.multi

import spadi.controller.database.access.id.{ProductId, ProductIds, SaleGroupIds, ShopProductIds}
import spadi.controller.database.access.single.{DbProductBasePrice, DbProductName, DbProductNetPrice, DbShopProduct}
import spadi.controller.database.factory.pricing.{NetPriceFactory, ProductFactory}
import spadi.controller.database.model.pricing.{BasePriceModel, NetPriceModel, ProductModel, SaleGroupModel, ShopProductModel}
import spadi.model.enumeration.PriceType.{Base, Net}
import spadi.model.enumeration.{PriceInputType, PriceType}
import spadi.model.partial.pricing.{ProductData, SaleGroupData, ShopProductData}
import spadi.model.stored.pricing.Product
import utopia.flow.generic.ValueConversions._
import utopia.flow.util.CollectionExtensions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.ManyModelAccess
import utopia.vault.sql.Extensions._

import scala.collection.immutable.VectorBuilder

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
	
	private def netPriceModel = NetPriceModel
	
	private def basePriceModel = BasePriceModel
	
	private def netPriceFactory = NetPriceFactory
	
	private def idColumn = factory.table.primaryColumn.get
	
	
	// OTHER	------------------------------
	
	/**
	  * @param productIds A set of product ids
	  * @param connection DB Connection (implicit)
	  * @return Product data for products with those ids
	  */
	def withIds(productIds: Iterable[Int])(implicit connection: Connection) =
		read(Some(idColumn.in(productIds)))
	
	def insertData(data: Vector[ShopProductData], contentType: PriceType)(implicit connection: Connection) =
	{
		// In case there are multiple shops, handles each one separately
		data.groupBy { _.shopId }.foreach { case (shopId, data) =>
			val electricIds = data.map { _.electricId }.sorted
			val firstElectricId = electricIds.head
			val lastElectricId = electricIds.last
			// Finds existing shop product ids matching specified electric ids
			val existingShopProductIds = ShopProductIds.forElectricIdsBetween(shopId, firstElectricId, lastElectricId)
			
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
			}.toMap
			
			// Deprecates old prices and inserts new ones
			contentType match
			{
				case Net =>
					// Deprecates all existing net prices for the specified products
					// TODO: Add an alternative version that works even when electric ids are not ordered
					DbNetPrices.deprecateElectricIdRange(shopId, firstElectricId, lastElectricId)
					
					// Inserts new net prices
					data.foreach { product => product.netPrice.foreach { newPrice =>
						DbNetPrices.insert(shopProductIds(product.electricId), newPrice)
					} }
				case Base =>
					// Makes sure all sale groups exist in the DB
					val saleGroupIdentifiers = data.flatMap { _.basePrice.flatMap { _.saleGroupIdentifier } }.sorted
					// TODO: Handle case where there are no sale group ids listed
					val existingSaleGroupIds = SaleGroupIds.forShopWithId(shopId)
						.forIdentifiersBetween(saleGroupIdentifiers.head, saleGroupIdentifiers.last)
					
					// Inserts missing sale group ids
					// TODO: Handle case where no ids require inserting
					val unregisteredSaleGroupIdentifiers = saleGroupIdentifiers.toSet -- existingSaleGroupIds.keySet
					val insertedSaleGroupIds = unregisteredSaleGroupIdentifiers.map { identifier =>
						identifier -> SaleGroupModel.insert(SaleGroupData(shopId, identifier, None)).id
					}
					val saleGroupIds = existingSaleGroupIds ++ insertedSaleGroupIds
					
					// Deprecates previous base prices for the specified products
					// TODO: Add an alternative cersion that works even when electric ids are not ordered
			}
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
