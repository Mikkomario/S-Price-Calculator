package spadi.model.cached.analysis

/**
  * Contains statistics about a shop's state in the marketplace
  * @author Mikko Hilpinen
  * @since 13.9.2020, v1.2.3
  */
case class ShopPriceReport(totalProductCount: Int, shopProductCount: Int, sharedProductCount: Int,
						   bestSharedPriceCount: Int, discountSalesGains: Vector[(Double, Int)])
{
	// ATTRIBUTES	--------------------------
	
	/**
	  * A vector that shows how much discount affects total product selling % (as a ratio)
	  */
	lazy val discountSaleRatioIncreases =
	{
		val saleCount = bestSharedPriceCount + uniqueProductCount
		discountSalesGains.map { case (discount, gain) => discount -> ((saleCount + gain).toDouble / saleCount) }
	}
	
	/**
	  * A vector that shows how much discount affects winning over competition % (as a ratio)
	  */
	lazy val discountWinRatioIncreases = discountSalesGains.map { case (discount, gain) =>
		discount -> ((bestSharedPriceCount + gain).toDouble / bestSharedPriceCount) }
	
	/**
	  * A vector that shows how discounts affect win over competition ratio
	  */
	lazy val discountWinRatios = discountSalesGains.map { case (discount, gain) =>
		discount -> ((bestSharedPriceCount + gain).toDouble / sharedProductCount) }
	
	
	// COMPUTED	------------------------------
	
	/**
	  * @return The number of products unique to this shop
	  */
	def uniqueProductCount = shopProductCount - sharedProductCount
	
	/**
	  * @return Ratio of total products coverage [0, 1]
	  */
	def coverageRatio = shopProductCount.toDouble / totalProductCount
	
	/**
	  * @return Ratio of products with competition in relation to all this shop's products [0, 1]
	  */
	def sharedRatio = sharedProductCount.toDouble / shopProductCount
	
	/**
	  * @return Ratio of unique products in this shop [0, 1]
	  */
	def uniqueProductRatio = 1 - sharedRatio
	
	/**
	  * @return Ratio of selling prices [0, 1]
	  */
	def winRatio = (bestSharedPriceCount + uniqueProductCount).toDouble / shopProductCount
	
	/**
	  * @return Ratio of prices that won't sell [0, 1]
	  */
	def loseRatio = 1 - winRatio
	
	/**
	  * @return Ratio of selling prices when only considering areas with competition [0, 1]
	  */
	def winOverCompetitionRatio = bestSharedPriceCount.toDouble / sharedProductCount
	
	/**
	  * @return Ratio of losing prices when only considering areas with competition [0, 1]
	  */
	def loseOverCompetitionRatio = 1 - winOverCompetitionRatio
	
	
	// IMPLEMENTED	--------------------------
	
	override def toString = s"Coverage: ${ratioString(coverageRatio)}, Uniqueness: ${
		ratioString(uniqueProductRatio)}, Selling: ${ratioString(winRatio)}, Chance to Win: ${
		ratioString(winOverCompetitionRatio)}, n=$totalProductCount, Sale ratio increase: ${
		discountRatioString(discountSaleRatioIncreases)}, Win ratio increase: ${
		discountRatioString(discountWinRatioIncreases)}, Win ratio changes: ${
		discountWinRatios.map { case (discount, ratio) => s"${ratioString(discount)} => ${ratioString(ratio)}" }.mkString(", ")}"
	
	
	// OTHER	------------------------------
	
	/**
	  * Combines these two reports to form larger report
	  * @param another Another report
	  * @return A combination of these reports
	  */
	def +(another: ShopPriceReport) =
	{
		// Combines the other report's gains to this report's gains, adding additional discount values when necessary
		val theirDiscountsView = another.discountSalesGains.view
		var myLastGain = 0
		var theirLastGain = 0
		var analyzedGainsCount = 0
		val combinedGains = discountSalesGains.flatMap { case (myDiscount, myGain) =>
			// Collects affecting items from the other list
			val gainsFromOther = theirDiscountsView.drop(analyzedGainsCount).takeWhile { _._1 <= myDiscount }.toVector
			analyzedGainsCount += gainsFromOther.size
			val newGains = gainsFromOther.lastOption match
			{
				// Case: Other list contains overlapping items
				case Some((theirDiscount, theirGain)) =>
					theirLastGain = theirGain
					// Case: All of the items can be added before this discount value
					if (theirDiscount < myDiscount)
						gainsFromOther.map { case (discount, theirGain) => discount -> (theirGain + myLastGain) } :+
							(myDiscount -> (myGain + theirGain))
					// Case: The last of the items must be combined with this discount value
					else
						gainsFromOther.dropRight(1)
							.map { case (discount, theirGain) => discount -> (theirGain + myLastGain) }
				// Case: No new gains were introduced in this discount range
				case None => Some(myDiscount -> (myGain + theirLastGain))
			}
			myLastGain = myGain
			newGains
		}
		// Finds the items that were not yet added
		val additionalGains =
		{
			// Case: Own discount gains list is empty
			if (discountSalesGains.isEmpty)
				another.discountSalesGains
			// Case: Other list may contain higher discount values
			else
			{
				val (myMaxDiscount, myMaxGain) = discountSalesGains.last
				theirDiscountsView.reverse.takeWhile { _._1 > myMaxDiscount }
					.map { case (discount, gain) => discount -> (gain + myMaxGain) }.toVector.reverse
			}
		}
		
		// Combines other values as well
		ShopPriceReport(totalProductCount + another.totalProductCount, shopProductCount + another.shopProductCount,
			sharedProductCount + another.sharedProductCount, bestSharedPriceCount + another.bestSharedPriceCount,
			combinedGains ++ additionalGains)
	}
	
	private def ratioString(ratio: Double) = s"${(ratio * 100).round.toInt}%"
	
	private def discountRatioString(discountRatios: Iterable[(Double, Double)]) =
		discountRatios.map { case (discount, ratio) => s"-${ratioString(discount)} => +${ratioString(ratio - 1)}" }
			.mkString(", ")
}
