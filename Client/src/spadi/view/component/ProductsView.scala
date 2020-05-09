package spadi.view.component

import spadi.model.Product
import spadi.view.controller.ProductRowVC
import spadi.view.util.Setup._
import utopia.flow.event.Changing
import utopia.genesis.shape.Axis.X
import utopia.reflection.component.context.ColorContext
import utopia.reflection.component.swing.StackableAwtComponentWrapperWrapper
import utopia.reflection.container.stack.segmented.SegmentedGroup
import utopia.reflection.container.swing.{AnimatedStack, ScrollView, Stack}
import utopia.reflection.controller.data.ContainerContentDisplayer
import utopia.reflection.shape.{StackLength, StackLengthLimit}

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
	
	private val segmentGroup = new SegmentedGroup(X)
	private val scrollView = stackContext.use { implicit c =>
		val stack = AnimatedStack.contextualColumn[ProductRowVC]()
		// Registers a content displayer for the stack
		ContainerContentDisplayer.forImmutableStates[Product, ProductRowVC](stack, productsPointer) {
			(a, b) => a.id == b.id } { ProductRowVC(segmentGroup, _) }
		// The stack is placed within a scroll view
		val scrollView = ScrollView.contextual(stack, lengthLimits = StackLengthLimit(maxOptimal = Some(maxOptimalLength)))
		
		scrollView
	}
	private val view = parentContext.use { implicit c =>
		val header = ProductRowsHeader(segmentGroup)
		Stack.columnWithItems(Vector(header, scrollView), StackLength.fixedZero)
	}
	
	
	// IMPLEMENTED  --------------------------------
	
	override protected def wrapped = view
}
