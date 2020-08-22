package spadi.controller.database.access.single

import spadi.controller.database.access.multi.DbProducts
import spadi.controller.database.factory.pricing.{BasePriceFactory, NetPriceFactory, SaleAmountFactory, SaleGroupFactory, ShopFactory, ShopProductFactory}
import spadi.controller.database.model.pricing.{SaleGroupModel, ShopModel, ShopProductModel}
import spadi.model.cached.ProgressState
import spadi.model.stored.pricing.Shop
import spadi.view.util.Setup._
import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.vault.database.Connection
import utopia.vault.nosql.access.{SingleRowModelAccess, UniqueAccess}
import utopia.vault.sql.{Delete, Where}

import scala.concurrent.Future

/**
  * Used for accessing individual shops
  * @author Mikko Hilpinen
  * @since 15.8.2020, v1.2
  */
object DbShop extends SingleRowModelAccess[Shop]
{
	// ATTRIBUTES	-------------------------------
	
	private implicit val languageCode: String = "fi"
	
	
	// IMPLEMENTED	-------------------------------
	
	override def factory = ShopFactory
	
	override def globalCondition = None
	
	
	// COMPUTED	-----------------------------------
	
	private def model = ShopModel
	
	
	// OTHER	-----------------------------------
	
	/**
	  * @param shopId A shop id
	  * @return An access point to that shop's data
	  */
	def apply(shopId: Int) = SingleDbShop(shopId)
	
	
	// NESTED	-----------------------------------
	
	case class SingleDbShop(shopId: Int) extends SingleRowModelAccess[Shop] with UniqueAccess[Shop]
	{
		// IMPLEMENTED	---------------------------
		
		override def condition = DbShop.mergeCondition(model.withId(shopId))
		
		override def factory = DbShop.factory
		
		
		// OTHER	-------------------------------
		
		/**
		  * Deletes this shop and all linked data
		  * @param connection DB Connection (implicit)
		  */
		def delete()(implicit connection: Connection): Unit = connection(Delete(table) + Where(condition))
		
		/**
		  * Deletes this shop's data asynchronously. The process may take a while, so it's performed asynchronously and
		  * tracked by a progress pointer
		  * @return Pointer to tracked progress, as well as future of progress completion and results
		  *         (may contain a failure)
		  */
		def deleteAsync() =
		{
			/*
			Progress states:
				- 0-2% Initializing DB
				- 2-20% Deprecated net prices
				- 20-37% Deprecated base prices
				- 37-45% Deprecated sale amounts
				- 45-55% Net prices
				- 55-65% Base prices
				- 65-75% Sale groups
				- 75-85% Shop products
				- 85-95% Products without shop data
				- 95-100% Shop
			 */
			val progressPointer = new PointerWithEvents(ProgressState.initial("Muodostetaan tietokantayhteyttä"))
			val future = Future {
				connectionPool.tryWith { implicit connection =>
					// Starts by deleting deprecated net prices
					progressPointer.value = ProgressState(0.02, "Poistetaan vanhentuneita nettohintoja")
					val shopProductCondition = ShopProductModel.withShopId(shopId).toCondition
					val shopProductTable = ShopProductFactory.table
					val netPriceTable = NetPriceFactory.table
					val netPriceTarget = netPriceTable join shopProductTable
					connection(Delete(netPriceTarget, netPriceTable) +
						Where(shopProductCondition && NetPriceFactory.deprecationColumn.isNotNull))
					// Continues with deprecated base prices
					progressPointer.value = ProgressState(0.2, "Poistetaan vanhentuneita perushintoja")
					val basePriceTable = BasePriceFactory.table
					val basePriceTarget = basePriceTable join shopProductTable
					connection(Delete(basePriceTarget, basePriceTable) +
						Where(shopProductCondition && BasePriceFactory.deprecationColumn.isNotNull))
					// Continues with deprecated sale amounts
					progressPointer.value = ProgressState(0.37, "Poistetaan vanhentuneita alennusprosentteja")
					val saleGroupCondition = SaleGroupModel.withShopId(shopId).toCondition
					val saleGroupTable = SaleGroupFactory.table
					val saleAmountTable = SaleAmountFactory.table
					val saleAmountTarget = saleAmountTable join saleGroupTable
					connection(Delete(saleAmountTarget, saleAmountTable) +
						Where(saleGroupCondition && SaleAmountFactory.deprecationColumn.isNotNull))
					// Next deletes active net prices
					progressPointer.value = ProgressState(0.45, "Poistetaan loput nettohinnat")
					connection(Delete(netPriceTarget, netPriceTable) + Where(shopProductCondition))
					// Next deletes active base prices
					progressPointer.value = ProgressState(0.55, "Poistetaan loput perushinnat")
					connection(Delete(basePriceTarget, basePriceTable) + Where(shopProductCondition))
					// Next deletes sale groups
					progressPointer.value = ProgressState(0.65, "Poistetaan alennusryhmät")
					connection(Delete(saleGroupTable) + Where(saleGroupCondition))
					// Next deletes shop products
					progressPointer.value = ProgressState(0.75, "Poistetaan tuotetiedot")
					connection(Delete(shopProductTable) + Where(shopProductCondition))
					// Deletes products that are no longer associated with shop products
					progressPointer.value = ProgressState(0.85, "Poistetaan ilman hintaa jääneet tuotteet")
					DbProducts.deleteProductsWithoutShopData()
					// Finally deletes the shop itself
					progressPointer.value = ProgressState(0.85, "Poistetaan tukku")
					delete()
				}
			}
			future.onComplete { result =>
				if (result.flatten.isSuccess)
					progressPointer.value = ProgressState.finished("Kaikki tiedot poistettu")
				else
					progressPointer.value = ProgressState.finished("Tietojen poistaminen epäonnistui")
			}
			progressPointer.view -> future
		}
	}
}
