package spadi.view.controller

import spadi.controller.ProfitsPercentage
import spadi.view.util.Setup._
import spadi.model.Product
import utopia.reflection.component.Refreshable
import utopia.reflection.component.context.{ColorContext, TextContext}
import utopia.reflection.component.swing.StackableAwtComponentWrapperWrapper
import utopia.reflection.component.swing.label.TextLabel
import utopia.reflection.container.swing.{SegmentGroup, Stack}
import utopia.reflection.localization.LocalString._
import utopia.reflection.shape.LengthExtensions._

object ProductRowVC
{
	/**
	 * @param group Segmented group used to lay out this row
	 * @param product First displayed product
	 * @param context Component creation context (implicit)
	 * @return New product row
	 */
	def apply(group: SegmentGroup, product: Product)(implicit context: ColorContext) =
		new ProductRowVC(group, product)(context)
}

/**
 * Displays product's information on a row
 * @author Mikko Hilpinen
 * @since 9.5.2020, v1
 */
class ProductRowVC(segmentGroup: SegmentGroup, initialProduct: Product)(parentContext: ColorContext)
	extends StackableAwtComponentWrapperWrapper with Refreshable[Product]
{
	// ATTRIBUTES   -------------------------------
	
	private var _content = initialProduct
	
	private implicit val context: TextContext = parentContext.forTextComponents()
	
	private val idLabel = TextLabel.contextual()
	private val nameLabel = TextLabel.contextual()(context.mapFont { _ * 0.8 })
	private val priceLabel = TextLabel.contextual()
	private val profitLabel = TextLabel.contextual()
	private val finalPriceLabel = TextLabel.contextual()
	
	private val row = Stack.rowWithItems(segmentGroup.wrap(Vector(idLabel, nameLabel, priceLabel,
		profitLabel, finalPriceLabel)), margins.medium.any)
	
	
	// INITIAL CODE -------------------------------
	
	updateLabels()
	
	
	// IMPLEMENTED  -------------------------------
	
	override protected def wrapped = row
	
	override def content_=(newContent: Product) =
	{
		_content = newContent
		updateLabels()
	}
	
	override def content = _content
	
	
	// OTHER    -----------------------------------
	
	private def percentString(percentage: Double) =
	{
		val rounded = math.round(percentage).toInt
		s"$rounded%"
	}
	
	private def updateLabels() =
	{
		idLabel.text = content.id.noLanguageLocalizationSkipped
		nameLabel.text = content.displayName.noLanguageLocalizationSkipped
		priceLabel.text = content.standardPriceString.noLanguageLocalizationSkipped
		val profitsPercentage = ProfitsPercentage.forPrice(content.totalPrice)
		profitLabel.text = percentString(profitsPercentage).noLanguageLocalizationSkipped
		finalPriceLabel.text = content.priceString(1 + profitsPercentage / 100.0).noLanguageLocalizationSkipped
	}
}
