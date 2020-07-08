package spadi.view.component

import spadi.view.util.Setup._
import utopia.reflection.component.context.ColorContext
import utopia.reflection.component.swing.label.TextLabel
import utopia.reflection.component.swing.template.StackableAwtComponentWrapperWrapper
import utopia.reflection.container.swing.layout.SegmentGroup
import utopia.reflection.container.swing.layout.multi.Stack
import utopia.reflection.shape.LengthExtensions._

object ProductRowsHeader
{
	/**
	 * @param group Segmented group used to lay out components
	 * @param context Implicit component creation context
	 * @return A new product rows header
	 */
	def apply(group: SegmentGroup)(implicit context: ColorContext) = new ProductRowsHeader(group)(context)
}

/**
 * A header component for the product rows area
 * @author Mikko Hilpinen
 * @since 9.5.2020, v1
 */
class ProductRowsHeader(segmentedGroup: SegmentGroup)(parentContext: ColorContext) extends StackableAwtComponentWrapperWrapper
{
	// ATTRIBUTES   ---------------------------------
	
	private implicit val language: String = "fi"
	
	private val view = parentContext.inContextWithBackground(colorScheme.primary.dark).forTextComponents()
		.expandingHorizontally.use { implicit c =>
		val labels = Vector("ID", "Tuote", "Tukkuhinta", "Kate", "Hinta").map { TextLabel.contextual(_) }
		Stack.rowWithItems(segmentedGroup.wrap(labels), margins.medium.any).framed(
			margins.small.any x margins.verySmall.any, c.containerBackground)
	}
	
	
	// IMPLEMENTED  ---------------------------------
	
	override protected def wrapped = view
}
