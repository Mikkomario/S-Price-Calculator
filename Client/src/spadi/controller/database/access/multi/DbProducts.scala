package spadi.controller.database.access.multi

import spadi.controller.database.access.id.{ProductIds, SaleGroupIds, ShopProductIds}
import spadi.controller.database.factory.pricing.ProductFactory
import spadi.controller.database.model.pricing.{BasePriceModel, NetPriceModel, ProductModel, SaleGroupModel, ShopProductModel}
import spadi.model.enumeration.PriceType.{Base, Net}
import spadi.model.enumeration.PriceType
import spadi.model.partial.pricing.{SaleGroupData, ShopProductData}
import spadi.model.stored.pricing.Product
import utopia.flow.generic.ValueConversions._
import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.TimeLogger
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
	// ATTRIBUTES	--------------------------
	
	private val maxProductInsertCount = 500 // How many new products may be inserted at a time
	private val maxPriceInsertCount = 250 // How many prices may be inserted at a time
	
	
	// IMPLEMENTED	--------------------------
	
	override def factory = ProductFactory
	
	override def globalCondition = Some(factory.nonDeprecatedCondition)
	
	
	// COMPUTED	------------------------------
	
	private def shopProductModel = ShopProductModel
	
	private def idColumn = factory.table.primaryColumn.get
	
	
	// OTHER	------------------------------
	
	/**
	  * @param productIds A set of product ids
	  * @param connection DB Connection (implicit)
	  * @return Product data for products with those ids
	  */
	def withIds(productIds: Iterable[Int])(implicit connection: Connection) =
		find(idColumn.in(productIds))
	
	/**
	  * Inserts a number of new product prices to the database
	  * @param data Product price data
	  * @param contentType Type of price to insert (Net | Base), determines which price type is searched from the
	  *                    provided data
	  * @param isCompleteElectricIdRange Whether specified data is ordered and inclusive (contains all shop products in
	  *                                  that range without missing any) (default = true)
	  * @param connection Database connection (implicit)
	  */
	def insertData(data: Vector[ShopProductData], contentType: PriceType, isCompleteElectricIdRange: Boolean = true)
				  (implicit connection: Connection) =
	{
		println(s"Processing data. Sorted: $isCompleteElectricIdRange")
		
		// In case there are multiple shops, handles each one separately
		data.groupBy { _.shopId }.foreach { case (shopId, data) =>
			// TODO: Remove timing tests
			val logger = new TimeLogger()
			
			val electricIds = data.map { _.electricId }.sorted
			val electricIdsSet = electricIds.toSet
			val firstElectricId = electricIds.head
			val lastElectricId = electricIds.last
			// TODO: Remove test prints
			logger.checkPoint(s"Inserts products from $firstElectricId to $lastElectricId to shop $shopId (${data.size} prices in total)")
			// Finds existing shop product ids matching specified electric ids
			val existingShopProductIds = ShopProductIds.forElectricIdsBetween(shopId, firstElectricId, lastElectricId)
			
			// Finds electric ids that don't have a matching shop product id yet
			val electricIdsMissingShopProductIdSet = electricIdsSet -- existingShopProductIds.keySet
			logger.checkPoint(s"Found ${existingShopProductIds.size} existing electric ids. Will insert ${
				electricIdsMissingShopProductIdSet.size} new ids.")
			val shopProductIds =
			{
				// Might not need to insert any new shop product rows
				if (electricIdsMissingShopProductIdSet.isEmpty)
				{
					logger.checkPoint("Didn't need to insert any new shop product ids")
					existingShopProductIds
				}
				else
				{
					val electricIdsMissingShopProductId = electricIdsMissingShopProductIdSet.toVector.sorted
					// Finds existing product ids for those electric ids
					val existingProductIds = ProductIds.forElectricIdsBetween(
						electricIdsMissingShopProductId.head, electricIdsMissingShopProductId.last)
					val electricIdsMissingProductIdSet = electricIdsMissingShopProductIdSet -- existingProductIds.keySet
					// Inserts new product rows to make sure all electric ids are covered
					val productIds =
					{
						if (electricIdsMissingProductIdSet.nonEmpty)
						{
							val newProductIdsRange = electricIdsMissingProductIdSet.toVector
								.splitToSegments(maxProductInsertCount).map { electricIds => ProductModel.insertMany(electricIds) }
								.reduce { (a, b) => (a.min min b.min) to (a.max max b.max) }
							val insertedProductIds = ProductIds.electricIdMapForProductIdsBetween(
								newProductIdsRange.head, newProductIdsRange.last)
							logger.checkPoint(s"Inserted ${newProductIdsRange.size} new products")
							existingProductIds ++ insertedProductIds
						}
						else
						{
							logger.checkPoint("Didn't need to insert any new products")
							existingProductIds
						}
					}
					/*
					val insertedProductIds = (electricIdsMissingProductIdSet -- existingProductIds.keySet).map { electricId =>
						electricId -> model.insertEmptyProduct(electricId)
					}
					val productIds = existingProductIds ++ insertedProductIds*/
					
					// Divides data to new inserts and existing updates
					val (dataToInsert, dataToUpdate) = data.dividedWith { p => existingShopProductIds.get(p.electricId) match
					{
						case Some(shopProductId) => Right(shopProductId -> p)
						case None => Left(p)
					} }
					logger.checkPoint(s"Divided new products to ${dataToUpdate.size} updates and ${dataToInsert.size} inserts")
					// Updates existing shop product rows
					dataToUpdate.foreach { case (shopProductId, p) =>
						shopProductModel.withName(p.name).withAlternativeName(p.alternativeName).nowUpdated
							.updateWhere(shopProductModel.withId(shopProductId).toCondition)
					}
					logger.checkPoint(s"Updated ${dataToUpdate.size} shop products")
					// Inserts new shop product rows
					val insertedShopProductIdRange = dataToInsert.splitToSegments(maxProductInsertCount).map { newShopProducts =>
						ShopProductModel.insertMany(newShopProducts.map { p =>
							(productIds(p.electricId), shopId, p.name, p.alternativeName) })
					}.reduce { (a, b) => (a.min min b.min) to (a.max max b.max) }
					val insertedShopProductIds = ShopProductIds.electricIdMapForShopProductIdsBetween(shopId,
						insertedShopProductIdRange.min, insertedShopProductIdRange.max)
					logger.checkPoint(s"Inserted ${insertedShopProductIdRange.size} new shop products")
					
					// Updates or inserts shop product rows (old version)
					/*
					val ids = data.map { product =>
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
					}.toMap*/
					
					existingShopProductIds ++ insertedShopProductIds
				}
			}
			
			
			// Deprecates old prices and inserts new ones
			contentType match
			{
				case Net =>
					// Deprecates all existing net prices for the specified products
					if (isCompleteElectricIdRange)
						DbNetPrices.deprecateElectricIdRange(shopId, firstElectricId, lastElectricId)
					else
						DbNetPrices.deprecatePricesForShopProductIds(shopProductIds.valuesIterator.toSet)
					logger.checkPoint("Deprecated previous net prices")
					
					// Inserts new net prices (n prices at a time)
					data.flatMap { p => p.netPrice.map { price => shopProductIds(p.electricId) -> price } }
						.splitToSegments(maxPriceInsertCount).foreach { prices => NetPriceModel.insertMany(prices) }
					logger.checkPoint("Inserted new net prices")
				case Base =>
					// Makes sure all sale groups exist in the DB
					val saleGroupIdentifiers = data.flatMap { _.basePrice.flatMap { _.saleGroupIdentifier } }.sorted
					val existingSaleGroupIds =
					{
						if (saleGroupIdentifiers.isEmpty)
							Map[String, Int]()
						else
							SaleGroupIds.forShopWithId(shopId)
								.forIdentifiersBetween(saleGroupIdentifiers.head, saleGroupIdentifiers.last)
					}
					logger.checkPoint(s"Read ${existingSaleGroupIds.size} existing sale group identifiers")
					
					// Inserts missing sale group ids
					val unregisteredSaleGroupIdentifiers = saleGroupIdentifiers.toSet -- existingSaleGroupIds.keySet
					val insertedSaleGroupIds = unregisteredSaleGroupIdentifiers.map { identifier =>
						identifier -> SaleGroupModel.insert(SaleGroupData(shopId, identifier, None)).id
					}
					// Sale group identifier -> sale group id
					val saleGroupIds = existingSaleGroupIds ++ insertedSaleGroupIds
					logger.checkPoint(s"Inserted ${insertedSaleGroupIds.size} new sale groups")
					
					// Deprecates previous base prices for the specified products
					if (isCompleteElectricIdRange)
						DbBasePrices.deprecateElectricIdRange(shopId, firstElectricId, lastElectricId)
					else
						DbBasePrices.deprecatePricesForShopProductIds(shopProductIds.valuesIterator.toSet)
					logger.checkPoint("Deprecated previous base prices")
					
					// Inserts new base prices (n at a time)
					data.flatMap { p => p.basePrice.map { price => (shopProductIds(p.electricId), price.price,
						price.saleGroupIdentifier.map { saleGroupIds(_) }) } }.splitToSegments(maxPriceInsertCount)
						.foreach { prices => BasePriceModel.insertMany(prices) }
					logger.checkPoint("Inserted new base prices")
			}
		}
	}
}
