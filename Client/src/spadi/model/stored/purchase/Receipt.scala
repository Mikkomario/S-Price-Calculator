package spadi.model.stored.purchase

import java.time.Instant

/**
  * Represents a list of chosen products
  * @author Mikko Hilpinen
  * @since 16.8.2020, v1.2.1
  */
case class Receipt(id: Int, currentVersion: ReceiptEdit, created: Instant, purchaseRecordTime: Option[Instant] = None)
