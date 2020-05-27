package spadi.view.component

import java.nio.file.Path

import spadi.view.util.Icons
import spadi.view.util.Setup._
import utopia.flow.util.FileExtensions._
import utopia.flow.util.CollectionExtensions._
import utopia.reflection.component.context.ButtonContextLike
import utopia.reflection.component.swing.DropDown
import utopia.reflection.component.swing.button.ImageAndTextButton
import utopia.reflection.component.swing.label.TextLabel
import utopia.reflection.container.swing.window.dialog.interaction.MessageDialog
import utopia.reflection.localization.{DisplayFunction, LocalizedString}
import utopia.reflection.localization.LocalString._

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
}
