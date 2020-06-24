package spadi.view.component

import java.nio.file.Path

import spadi.view.util.Icons
import spadi.view.util.Setup._
import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.flow.util.FileExtensions._
import utopia.flow.util.CollectionExtensions._
import utopia.reflection.component.context.ButtonContextLike
import utopia.reflection.component.swing.{DropDown, SearchFrom}
import utopia.reflection.component.swing.button.ImageAndTextButton
import utopia.reflection.component.swing.label.TextLabel
import utopia.reflection.container.swing.window.dialog.interaction.MessageDialog
import utopia.reflection.localization.{DisplayFunction, LocalizedString}
import utopia.reflection.localization.LocalString._
import utopia.reflection.shape.StackLength

import scala.concurrent.ExecutionContext

/**
 * Used for creating standard components
 * @author Mikko Hilpinen
 * @since 26.5.2020, v1.1
 */
object Fields
{
	private implicit val languageCode: String = "fi"
	
	/**
	 * Creates a new drop down component
	 * @param noResultsText Text displayed when there are no results available
	 * @param selectionPrompt Prompt displayed when no value is selected
	 * @param displayFunction Display function for selectable values (default = toString)
	 * @param contentPointer Pointer used for managing selectable content (default = new empty pointer)
	 * @param valuePointer Pointer used for managing selected value (default = new empty pointer)
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
	                contentPointer: PointerWithEvents[Vector[A]] = new PointerWithEvents[Vector[A]](Vector()),
	                valuePointer: PointerWithEvents[Option[A]] = new PointerWithEvents[Option[A]](None),
	                sameInstanceCheck: (A, A) => Boolean = (a: Any, b: Any) => a == b, contentIsStateless: Boolean = true)
	               (implicit context: ButtonContextLike, exc: ExecutionContext) =
	{
		val noResultsView = TextLabel.contextual(noResultsText)
		noResultsView.background = context.buttonColor
		DropDown.contextualWithTextOnly[A](noResultsView, Icons.dropDown.singleColorImage,
			selectionPrompt, displayFunction, contentPointer, valuePointer,
			sameInstanceCheck = sameInstanceCheck, contentIsStateless = contentIsStateless)
	}
	
	/**
	 * Creates a new drop down component
	 * @param standardWidth The width of the search input field when selection options haven't yet been evaluated
	 * @param noResultsText Text displayed when there are no results available
	 * @param selectionPrompt Prompt displayed when no value is selected
	 * @param displayFunction Display function for selectable values (default = toString)
	 * @param contentPointer Pointer used for managing selectable content (default = new empty pointer)
	 * @param valuePointer Pointer used for managing selected value (default = new empty pointer)
	 * @param sameInstanceCheck Function for comparing items (default = equals)
	 * @param contentIsStateless Whether items only have one possible state (default = true, set to false if you
	 *                           specify your own same instance check function)
	 * @param context Component creation context (implicit)
	 * @param exc Execution context (implicit)
	 * @tparam A Type of selected item
	 * @return A new drop down field
	 */
	def searchFrom[A](standardWidth: StackLength, noResultsText: LocalizedString, selectionPrompt: LocalizedString,
	                  displayFunction: DisplayFunction[A] = DisplayFunction.raw,
	                  contentPointer: PointerWithEvents[Vector[A]] = new PointerWithEvents[Vector[A]](Vector()),
	                  valuePointer: PointerWithEvents[Option[A]] = new PointerWithEvents[Option[A]](None),
	                  sameInstanceCheck: (A, A) => Boolean = (a: Any, b: Any) => a == b, contentIsStateless: Boolean = true)
	               (implicit context: ButtonContextLike, exc: ExecutionContext) =
	{
		val searchStringPointer = new PointerWithEvents[Option[String]](None)
		val noResultsView = SearchFrom.noResultsLabel(noResultsText, searchStringPointer)
		noResultsView.background = context.buttonColor
		
		SearchFrom.contextualWithTextOnly(noResultsView, selectionPrompt, standardWidth, displayFunction,
			searchIcon = Some(Icons.search.singleColorImage), contentPointer = contentPointer,
			selectedValuePointer = valuePointer, searchFieldPointer = searchStringPointer,
			sameInstanceCheck = sameInstanceCheck, contentIsStateless = contentIsStateless)
	}
	
	/**
	 * Creates a button that opens a file when clicked
	 * @param path Path to the file to open
	 * @param parentWindow Window that will host an error message if file opening fails (option, call by name)
	 * @param context Button creation context
	 * @return A new file opening button
	 */
	def openFileButton(path: Path, parentWindow: => Option[java.awt.Window])(implicit context: ButtonContextLike) = ImageAndTextButton
		.contextual(Icons.file.inButton, "Avaa") { path.openInDesktop().failure.foreach { error =>
			parentWindow.foreach { window =>
				val dialogContext = baseContext.inContextWithBackground(colorScheme.error).forTextComponents()
				new MessageDialog(dialogContext, dialogContext.forSecondaryColorButtons,
					"Tiedoston avaaminen epäonnistui",
					"Tiedoston avaaminen ei onnistunut.\nVirheilmoitus: %s".localized.interpolated(
						Vector(error.getLocalizedMessage)), "OK", Some(Icons.close), Some(Icons.warning))
					.display(window)
			}
		}
	}
	
	/**
	 * Creates a new message dialog
	 * @param title Dialog title
	 * @param text Text to display
	 * @return New dialog
	 */
	def messageDialog(title: LocalizedString, text: LocalizedString) =
	{
		val context = baseContext.inContextWithBackground(primaryColors.dark).forTextComponents()
		new MessageDialog(context.mapFont { _ * 0.8 }, context.forSecondaryColorButtons, title, text, "OK",
			Some(Icons.close), Some(Icons.info))
	}
}
