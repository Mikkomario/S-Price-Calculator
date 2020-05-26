package spadi.view.component

import spadi.view.util.Icons
import utopia.reflection.component.context.ButtonContextLike
import utopia.reflection.component.swing.DropDown
import utopia.reflection.component.swing.label.TextLabel
import utopia.reflection.localization.{DisplayFunction, LocalizedString}

import scala.concurrent.ExecutionContext

/**
 * Used for creating standard components
 * @author Mikko Hilpinen
 * @since 26.5.2020, v1.1
 */
object Fields
{
	/**
	 * Creates a new drop down component
	 * @param noResultsText Text displayed when there are no results available
	 * @param selectionPrompt Prompt displayed when no value is selected
	 * @param displayFunction Display function for selectable values (default = toString)
	 * @param sameInstanceCheck Function for comparing items (default = equals)
	 * @param contentIsStateless Whether items only have one possible state (default = true, set to false if you
	 *                           specify your own same instance check function)
	 * @param context Component creation context (implicit)
	 * @param exc Execution context (implicit)
	 * @tparam A Type of selected item
	 * @return A new drop down field
	 */
	def dropDown[A](noResultsText: LocalizedString, selectionPrompt: LocalizedString,
	                displayFunction: DisplayFunction[A] = DisplayFunction.raw,
	                sameInstanceCheck: (A, A) => Boolean = (a: Any, b: Any) => a == b, contentIsStateless: Boolean = true)
	               (implicit context: ButtonContextLike, exc: ExecutionContext) =
	{
		val noResultsView = TextLabel.contextual(noResultsText)
		noResultsView.background = context.buttonColor
		DropDown.contextualWithTextOnly[A](noResultsView, Icons.dropDown.singleColorImage,
			selectionPrompt, displayFunction,
			sameInstanceCheck = sameInstanceCheck, contentIsStateless = contentIsStateless)
	}
}
