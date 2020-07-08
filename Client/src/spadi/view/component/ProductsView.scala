package spadi.view.component

import spadi.model.Product
import spadi.view.controller.ProductRowVC
import spadi.view.util.Setup._
import utopia.flow.event.Changing
import utopia.reflection.component.context.ColorContext
import utopia.reflection.component.swing.template.StackableAwtComponentWrapperWrapper
import utopia.reflection.container.swing.layout.SegmentGroup
import utopia.reflection.container.swing.layout.multi.Stack
import utopia.reflection.container.swing.layout.wrapper.scrolling.ScrollView
import utopia.reflection.controller.data.ContainerContentDisplayer
import utopia.reflection.shape.{StackLength, StackLengthLimit}
import utopia.reflection.shape.LengthExtensions._

object ProductsView
{
	/**
	 * @param productsPointer Pointer to displayed products
	 * @param maxOptimalLength Maximum optimal length of the scroll view
	 * @param context Component creation context (implicit)
	 * @return A new products view
	 */
	def apply(productsPointer: Changing[Vector[Product]], maxOptimalLength: Double)(implicit context: ColorContext) =
		new ProductsView(productsPointer, maxOptimalLength)(context)
}

/**
 * Used for displaying a number of products at once
 * @author Mikko Hilpinen
 * @since 9.5.2020, v1
 */
class ProductsView(productsPointer: Changing[Vector[Product]], maxOptimalLength: Double)(parentContext: ColorContext)
	extends StackableAwtComponentWrapperWrapper
{
	// ATTRIBUTES   -------------------------------
	
	private val stackContext: ColorContext = parentContext.withLightGrayBackground
	
	// TODO: When rows get removed, they should be removed from the group as well
	private val segmentGroup = new SegmentGroup()
	private val scrollView = stackContext.use { implicit c =>
		val stack = Stack.column[ProductRowVC](margins.medium.any)
		// Registers a content displayer for the stack
		ContainerContentDisplayer.forImmutableStates[Product, ProductRowVC](stack, productsPointer) {
			(a, b) => a.id == b.id } { ProductRowVC(segmentGroup, _) }
		// The stack is placed within a scroll view
		val scrollView = ScrollView.contextual(stack, lengthLimits = StackLengthLimit(maxOptimal = Some(maxOptimalLength)))
		
		scrollView.framed(margins.small.any x margins.verySmall.any, stackContext.containerBackground)
	}
	private val view = parentContext.use { implicit c =>
		val header = ProductRowsHeader(segmentGroup)
		Stack.columnWithItems(Vector(header, scrollView), StackLength.fixedZero)
	}
	
	
	// IMPLEMENTED  --------------------------------
	
	override protected def wrapped = view
}
