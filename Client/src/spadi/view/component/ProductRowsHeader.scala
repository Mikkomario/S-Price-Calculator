package spadi.view.component

import spadi.view.util.Setup._
import utopia.reflection.component.context.ColorContext
import utopia.reflection.component.swing.StackableAwtComponentWrapperWrapper
import utopia.reflection.component.swing.label.TextLabel
import utopia.reflection.container.stack.segmented.SegmentedGroup
import utopia.reflection.container.swing.SegmentedRow
import utopia.reflection.shape.LengthExtensions._

object ProductRowsHeader
{
	/**
	 * @param group Segmented group used to lay out components
	 * @param context Implicit component creation context
	 * @return A new product rows header
	 */
	def apply(group: SegmentedGroup)(implicit context: ColorContext) = new ProductRowsHeader(group)(context)
}

/**
 * A header component for the product rows area
 * @author Mikko Hilpinen
 * @since 9.5.2020, v1
 */
class ProductRowsHeader(segmentedGroup: SegmentedGroup)(parentContext: ColorContext) extends StackableAwtComponentWrapperWrapper
{
	// ATTRIBUTES   ---------------------------------
	
	private implicit val language: String = "fi"
	
	private val view = parentContext.withPrimaryBackground.forTextComponents().expandingHorizontally.use { implicit c =>
		val labels = Vector("ID", "Tuote", "Hinta").map { TextLabel.contextual(_) }
		SegmentedRow.partOfGroupWithItems(segmentedGroup, labels, margins.medium.any).framed(
			margins.medium.any x margins.small.any, c.containerBackground)
	}
	
	
	// IMPLEMENTED  ---------------------------------
	
	override protected def wrapped = view
}
