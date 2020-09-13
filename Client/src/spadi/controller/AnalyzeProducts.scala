package spadi.controller

import scala.math.Ordering.Double.TotalOrdering
import spadi.controller.database.access.id.{ProductId, ShopIds}
import spadi.controller.database.access.multi.DbProducts
import spadi.model.cached.ProgressState
import spadi.model.cached.analysis.ShopPriceReport
import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.flow.util.CollectionExtensions._
import utopia.reflection.localization.Localizer
import utopia.vault.database.Connection

import scala.collection.immutable.VectorBuilder

/**
  * Used for scanning though the product data, analyzing how prices could be altered to increase sales
  * @author Mikko Hilpinen
  * @since 13.9.2020, v1.2.3
  */
object AnalyzeProducts
{
	private implicit val languageCode: String = "fi"
	
	private val productsPerIteration = 10000
	private val discountStep = 0.05
	
	/**
	  * Analyzes products data. This method may take a while to complete
	  * @param progressPointer A pointer containing followed progress state (will get updated during this process)
	  * @param connection Database connection (implicit)
	  * @param localizer Localizer (implicit)
	  * @return Analysis results. One report for each shop id.
	  */
	def apply(progressPointer: PointerWithEvents[ProgressState])(implicit connection: Connection, localizer: Localizer) =
	{
		// Finds the available shop ids (uses those to group the data)
		val shopIds = ShopIds.all
		
		// Finds the minimum and maximum product id in order to find range start & end
		val idRange = ProductId.min match
		{
			case Some(smallest) => smallest to ProductId.max.getOrElse(smallest)
			case None => 0 to 0
		}
		
		val estimatedProductsCount = idRange.end - idRange.start
		progressPointer.value = ProgressState(0.05, s"Prosessoidaan ${idRange.end - idRange.start} hintatietoa")
		val progressPerIteration = productsPerIteration.toDouble / estimatedProductsCount * 0.95
		
		// Handles the data in chunks
		val result = idRange.subRangeIterator(productsPerIteration).map { range =>
			// Reads product data, including prices
			val products = DbProducts.betweenIds(range.start, range.end)
			val numberOfProducts = products.size
			
			// Analyzes data from each shop's perspective
			val result = shopIds.map { shopId =>
				val productsForShop = products.filter { _.shopData.exists { _.shopId == shopId } }
				val sharedProducts = productsForShop.filter { _.shopData.size > 1 }
				val sharedProductsCount = sharedProducts.size
				// Checks which prices are not the cheapest and how much more wins could be gained by altering
				// those prices
				val losingProducts = sharedProducts.filterNot { _.cheapestShopId.contains(shopId) }
				val requiredDiscounts = losingProducts.flatMap { _.priceRatioForShopWithId(shopId) }
					.map { ratio => 1.0 - (0.99 / ratio) }.sorted
				val losingProductsCount = losingProducts.size
				val discountGains = calculateDiscountGains(requiredDiscounts)
				if (losingProductsCount > 0)
					println(s"Discount gains for shop $shopId based on $sharedProductsCount products: ${
						discountGains.map { case (discount, gain) => s"-${(discount * 100).toInt}% => ${
							(gain.toDouble / losingProductsCount * 100).toInt}%" }.mkString(", ") }")
				
				shopId -> ShopPriceReport(numberOfProducts, productsForShop.size, sharedProductsCount,
					sharedProductsCount - losingProducts.size, discountGains)
			}.toMap
			
			progressPointer.update { p => p.copy(progress = p.progress + progressPerIteration) }
			result
			// Combines the chunks two at a time to save memory
		}.reduce { (a, b) => shopIds.map { shopId => shopId -> (a(shopId) + b(shopId)) }.toMap }
		
		progressPointer.value = ProgressState.finished("Hintatiedot analysoitu")
		result
	}
	
	// Returns a vector of required discount, gained product pairs (from smallest to largest discount)
	private def calculateDiscountGains(requiredDiscounts: Vector[Double]) =
	{
		val productsCount = requiredDiscounts.size
		val view = requiredDiscounts.view
		
		var discount = 0.0
		var totalGain = 0
		
		// Increases the discount and counts how many new products can be won over
		val resultsBuilder = new VectorBuilder[(Double, Int)]()
		while (discount < 1.0 && totalGain < productsCount)
		{
			discount += discountStep
			val gain = view.drop(totalGain).takeWhile { _ <= discount }.size
			// If new products were won, records this step
			if (gain > 0)
			{
				totalGain += gain
				resultsBuilder += (discount -> totalGain)
			}
		}
		resultsBuilder.result()
	}
}
