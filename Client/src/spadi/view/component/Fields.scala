package spadi.view.component

import java.nio.file.Path
import java.time.Instant

import spadi.view.util.Icons
import spadi.view.util.Setup._
import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.flow.util.FileExtensions._
import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.TimeExtensions._
import utopia.genesis.event.{KeyStateEvent, MouseButtonStateEvent}
import utopia.genesis.handling.{KeyStateListener, MouseButtonStateListener}
import utopia.genesis.shape.shape2D.Point
import utopia.genesis.util.Screen
import utopia.genesis.view.{GlobalKeyboardEventHandler, GlobalMouseEventHandler}
import utopia.inception.handling.Mortal
import utopia.inception.handling.immutable.Handleable
import utopia.reflection.color.ColorRole.Info
import utopia.reflection.component.context.{ButtonContextLike, ColorContextLike}
import utopia.reflection.component.swing.animation.AnimatedVisibility
import utopia.reflection.component.swing.button.{ImageAndTextButton, ImageButton}
import utopia.reflection.component.swing.display.MultiLineTextView
import utopia.reflection.component.swing.input.{DropDown, SearchFrom}
import utopia.reflection.component.swing.label.TextLabel
import utopia.reflection.container.swing.window.{Popup, Window}
import utopia.reflection.container.swing.window.interaction.ButtonColor.Fixed
import utopia.reflection.container.swing.window.interaction.{MessageWindow, YesNoWindow}
import utopia.reflection.event.VisibilityChange.Appearing
import utopia.reflection.localization.{DisplayFunction, LocalizedString}
import utopia.reflection.localization.LocalString._
import utopia.reflection.shape.Alignment
import utopia.reflection.shape.LengthExtensions.LengthNumber
import utopia.reflection.shape.stack.StackLength

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
				new MessageWindow(dialogContext, dialogContext.forSecondaryColorButtons,
					"Tiedoston avaaminen epäonnistui",
					"Tiedoston avaaminen ei onnistunut.\nVirheilmoitus: %s".localized.interpolated(
						Vector(error.getLocalizedMessage)), "OK", Some(Icons.close), Some(Icons.warning))
					.displayOver(window)
			}
		}
	}
	
	/**
	  * Creates a bew button that is used for displaying more information / help
	  * @param infoText Text to display when user clicks the button
	  * @param context Button creation context
	  * @return A new button
	  */
	def infoButton(infoText: LocalizedString)(implicit context: ColorContextLike) =
	{
		val buttonColor = context.color(Info)
		val button = ImageButton.contextualWithoutAction(Icons.help.asIndividualButtonWithColor(buttonColor),
			isLowPriority = true)
		button.registerAction { () =>
			// When button is pressed, display a pop-up with info text
			val content = baseContext.inContextWithBackground(buttonColor).forTextComponents().mapFont { _ * 0.85 }
				.use { implicit c =>
					val view = MultiLineTextView.contextual(infoText, Screen.width / 4)
						.inRoundedFraming(margins.medium.any, buttonColor)
					AnimatedVisibility.contextual(view, initialState = Appearing)
				}
			val popup = Popup(button, content, actorHandler, resizeAlignment = Alignment.Left) { (cSize, pSize) =>
				Point(cSize.width + margins.medium, (cSize.height - pSize.height) / 2) }
			// Closes the pop-up (with animation) when any key is pressed or when the user clicks outside
			// the window area
			GlobalMouseEventHandler.registerButtonListener(new AnimatedHideOnOutsideClickListener(popup, content))
			GlobalKeyboardEventHandler.registerKeyStateListener(
				KeyStateListener.oneTimeListener(KeyStateEvent.wasPressedFilter) { _ =>
					content.hide().onComplete { _ => popup.close() } })
			popup.display(gainFocus = false)
		}
		button
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
		new MessageWindow(context.mapFont { _ * 0.8 }, context.forSecondaryColorButtons, title, text, "OK",
			Some(Icons.close), Some(Icons.large.info))
	}
	
	/**
	  * Creates a new dialog for success messages
	  * @param text Text displayed on the dialog
	  * @param title Title for the dialog (default = "Onnistui")
	  * @return A new dialog
	  */
	def successDialog(text: LocalizedString, title: LocalizedString = "Onnistui") =
	{
		val context = baseContext.inContextWithBackground(colorScheme.success).forTextComponents()
		new MessageWindow(context.mapFont { _ * 0.8 }, context.forSecondaryColorButtons, title, text, "OK",
			Some(Icons.checkCircle), Some(Icons.large.success))
	}
	
	/**
	 * Creates a new error message dialog
	 * @param text Text to display
	 * @return New dialog
	 */
	def errorDialog(text: LocalizedString) =
	{
		val context = baseContext.inContextWithBackground(colorScheme.error).forTextComponents()
		new MessageWindow(context.mapFont { _ * 0.8 }, context.forSecondaryColorButtons, "Virhe", text,
			"OK", Some(Icons.close), Some(Icons.large.warning))
	}
	
	/**
	  * Creates a new yes no window for checking whether an item should be deleted
	  * @param text Question text
	  * @return A new window
	  */
	def deletionQuestionDialog(text: LocalizedString) =
	{
		val dialogContext = baseContext.inContextWithBackground(colorScheme.gray).forTextComponents()
		new YesNoWindow(dialogContext, "Oletko varma?", text,
			Map(true -> Icons.delete, false -> Icons.close), Map(true -> Fixed(colorScheme.error)))({ (color, _) =>
			dialogContext.forButtons(color) })
	}
	
	private class AnimatedHideOnOutsideClickListener(window: Window[_], content: AnimatedVisibility[_])
		extends MouseButtonStateListener with Handleable with Mortal
	{
		// ATTRIBUTES	---------------------------
		
		private val actionThreshold = Instant.now() + 0.1.seconds
		
		private var triggered = false
		
		override val mouseButtonStateEventFilter = e => e.isDown && window.isVisible &&
			Instant.now() > actionThreshold && !window.bounds.contains(e.absoluteMousePosition)
		
		
		// IMPLEMENTED	---------------------------
		
		override def isDead = triggered || window.isClosed
		
		override def onMouseButtonState(event: MouseButtonStateEvent) =
		{
			triggered = true
			content.show().onComplete { _ => window.close() }
			None
		}
	}
}
