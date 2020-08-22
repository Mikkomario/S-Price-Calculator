package spadi.model.stored.purchase

import java.time.Instant

/**
  * Represents a receipt edit version
  * @author Mikko Hilpinen
  * @since 16.8.2020, v1.2.1
  */
case class ReceiptEdit(id: Int, receiptId: Int, name: String, created: Instant)
