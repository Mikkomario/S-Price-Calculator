package spadi.view.controller

import spadi.view.util.Setup._
import spadi.model.Product
import utopia.reflection.component.Refreshable
import utopia.reflection.component.context.{ColorContext, TextContext}
import utopia.reflection.component.swing.StackableAwtComponentWrapperWrapper
import utopia.reflection.component.swing.label.TextLabel
import utopia.reflection.container.stack.segmented.SegmentedGroup
import utopia.reflection.container.swing.SegmentedRow
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
	def apply(group: SegmentedGroup, product: Product)(implicit context: ColorContext) =
		new ProductRowVC(group, product)(context)
}

/**
 * Displays product's information on a row
 * @author Mikko Hilpinen
 * @since 9.5.2020, v1
 */
class ProductRowVC(segmentGroup: SegmentedGroup, initialProduct: Product)(parentContext: ColorContext)
	extends StackableAwtComponentWrapperWrapper with Refreshable[Product]
{
	// ATTRIBUTES   -------------------------------
	
	private var _content = initialProduct
	
	private implicit val context: TextContext = parentContext.forTextComponents()
	
	private val idLabel = TextLabel.contextual(initialProduct.id.noLanguageLocalizationSkipped)
	private val nameLabel = TextLabel.contextual(initialProduct.displayName.noLanguageLocalizationSkipped)
	private val priceLabel = TextLabel.contextual(initialProduct.priceString.noLanguageLocalizationSkipped)
	
	private val row = SegmentedRow.partOfGroupWithItems(segmentGroup, Vector(idLabel, nameLabel, priceLabel),
		margins.medium.any)
	
	
	// IMPLEMENTED  -------------------------------
	
	override protected def wrapped = row
	
	override def content_=(newContent: Product) =
	{
		_content = newContent
		idLabel.text = newContent.id.noLanguageLocalizationSkipped
		nameLabel.text = newContent.displayName.noLanguageLocalizationSkipped
		priceLabel.text = newContent.priceString.noLanguageLocalizationSkipped
	}
	
	override def content = _content
	
	
	// OTHER    -----------------------------------
	
	/**
	 * Removes this component from the segmented group it was registered to
	 */
	def detachFromSegmentedGroup() = segmentGroup.remove(row)
}
