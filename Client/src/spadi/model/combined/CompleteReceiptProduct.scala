package spadi.model.combined

import spadi.model.Extender
import spadi.model.stored.pricing.ShopProduct
import spadi.model.stored.purchase.ReceiptProduct

/**
  * Combines receipt product with up-to-date product information
  * @author Mikko Hilpinen
  * @since 16.8.2020, v1.2.1
  */
case class CompleteReceiptProduct(receiptInfo: ReceiptProduct, electricId: String, primaryProductInfo: ShopProduct,
								  alternativeProductInfo: Set[ShopProduct]) extends Extender[ReceiptProduct]
{
	override def wrapped = receiptInfo
}
