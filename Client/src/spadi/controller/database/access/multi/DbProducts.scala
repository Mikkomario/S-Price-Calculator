package spadi.controller.database.access.multi

import spadi.controller.database.access.id.{ProductIds, SaleGroupIds, ShopProductIds}
import spadi.controller.database.factory.pricing.ProductFactory
import spadi.controller.database.model.pricing.{ProductModel, SaleGroupModel, ShopProductModel}
import spadi.model.enumeration.PriceType.{Base, Net}
import spadi.model.enumeration.PriceType
import spadi.model.partial.pricing.{SaleGroupData, ShopProductData}
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
		// In case there are multiple shops, handles each one separately
		data.groupBy { _.shopId }.foreach { case (shopId, data) =>
			val electricIds = data.map { _.electricId }.sorted
			val electricIdsSet = electricIds.toSet
			val firstElectricId = electricIds.head
			val lastElectricId = electricIds.last
			// Finds existing shop product ids matching specified electric ids
			val existingShopProductIds = ShopProductIds.forElectricIdsBetween(shopId, firstElectricId, lastElectricId)
			
			// Finds electric ids that don't have a matching shop product id yet
			val electricIdsMissingProductIdSet = electricIdsSet -- existingShopProductIds.keySet
			val shopProductIds =
			{
				// Might not need to insert any new shop product rows
				if (electricIdsMissingProductIdSet.isEmpty)
					existingShopProductIds
				else
				{
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
					data.map { product =>
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
					
					// Inserts new net prices
					data.foreach { product => product.netPrice.foreach { newPrice =>
						DbNetPrices.insert(shopProductIds(product.electricId), newPrice)
					} }
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
					
					// Inserts missing sale group ids
					val unregisteredSaleGroupIdentifiers = saleGroupIdentifiers.toSet -- existingSaleGroupIds.keySet
					val insertedSaleGroupIds = unregisteredSaleGroupIdentifiers.map { identifier =>
						identifier -> SaleGroupModel.insert(SaleGroupData(shopId, identifier, None)).id
					}
					// Sale group identifier -> sale group id
					val saleGroupIds = existingSaleGroupIds ++ insertedSaleGroupIds
					
					// Deprecates previous base prices for the specified products
					if (isCompleteElectricIdRange)
						DbBasePrices.deprecateElectricIdRange(shopId, firstElectricId, lastElectricId)
					else
						DbBasePrices.deprecatePricesForShopProductIds(shopProductIds.valuesIterator.toSet)
					
					// Inserts new base prices
					data.foreach { product => product.basePrice.foreach { newPrice =>
						DbBasePrices.insert(shopProductIds(product.electricId), newPrice.price,
							newPrice.saleGroupIdentifier.map { saleGroupIds(_) })
					} }
			}
		}
	}
}
