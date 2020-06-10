package spadi.view.dialog

import java.nio.file.Path

import spadi.model.FileReadSetting
import spadi.view.component.FileReadSettingInputRow
import spadi.view.util.Icons
import spadi.view.util.Setup._
import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.WaitUtils
import utopia.flow.util.TimeExtensions._
import utopia.genesis.shape.shape2D.{Direction2D, Point, Size}
import utopia.reflection.component.{Area, ComponentLike, Focusable}
import utopia.reflection.component.swing.{AwtComponentRelated, MultiLineTextView}
import utopia.reflection.component.swing.button.{ImageAndTextButton, ImageButton}
import utopia.reflection.component.swing.label.TextLabel
import utopia.reflection.container.stack.StackLayout.Center
import utopia.reflection.container.swing.window.{Frame, Popup}
import utopia.reflection.container.swing.window.WindowResizePolicy.Program
import utopia.reflection.container.swing.{AnimatedStack, SegmentGroup, Stack}
import utopia.reflection.localization.LocalizedString
import utopia.reflection.shape.LengthExtensions._
import utopia.reflection.shape.StackLength
import utopia.reflection.util.Screen

object FileReadSettingsFrame
{
	/**
	 * Creates a new frame for adding new settings
	 * @param paths Paths to add settings for
	 * @return A new frame
	 */
	def forAdding(paths: Vector[Path]) = new FileReadSettingsFrame(Left(paths))
	
	/**
	 * Creates a new frame for editing existing settings
	 * @param settings Settings to edit
	 * @return A new frame
	 */
	def forEditing(settings: Vector[FileReadSetting]) = new FileReadSettingsFrame(Right(settings))
}

/**
 * A dialog used for requesting read settings for new files
 * @author Mikko Hilpinen
 * @since 27.5.2020, v1.1
 */
class FileReadSettingsFrame(base: Either[Vector[Path], Vector[FileReadSetting]]) extends Area
{
	// ATTRIBUTES   -----------------------------------
	
	private var output: Vector[FileReadSetting] = Vector()
	
	private implicit val languageCode: String = "fi"
	private val backgroundContext = baseContext.inContextWithBackground(primaryColors)
	private val rowContext = backgroundContext.withLightGrayBackground.forTextComponents()
	
	private val segmentedGroup = new SegmentGroup()
	private val headerRow = backgroundContext.inContextWithBackground(primaryColors.dark).forTextComponents()
		.expandingToRight
		.use { implicit c =>
			val labels = Vector[LocalizedString]("Tiedosto", "Tukkuri", "Tyyppi", "Avaa", "Poista")
				.map { TextLabel.contextual(_) }
			val row = Stack.rowWithItems(segmentedGroup.wrap(labels), margins.medium.downscaling, margins.medium.any)
			row.background = c.containerBackground
			row
		}
	private val rowStack = rowContext.use { implicit c =>
		val rows: Vector[FileReadSettingInputRow] = base match
		{
			case Right(settings) => settings.map { s => new FileReadSettingInputRow(segmentedGroup, Right(s))(removeRow) }
			case Left(paths) => paths.map { p => new FileReadSettingInputRow(segmentedGroup, Left(p))(removeRow) }
		}
		val stack = AnimatedStack.contextualColumn(rows)
		stack.background = c.containerBackground
		stack
	}
	private val nextButton = backgroundContext.forTextComponents().forSecondaryColorButtons.use { implicit c =>
		ImageAndTextButton.contextualWithoutAction(Icons.next.inButton, "Seuraava")
	}
	private val closeButton = backgroundContext.forTextComponents().forPrimaryColorButtons.use { implicit c =>
		ImageAndTextButton.contextualWithoutAction(Icons.close.inButton, "Ohita")
	}
	private val settingsViewStack = Stack.columnWithItems(
		Vector(headerRow, rowStack.framed(margins.medium.any, rowContext.containerBackground)),
		StackLength.fixedZero)
	
	private val dialog = backgroundContext.use { implicit c =>
		val content = Stack.buildColumnWithContext() { mainStack =>
			mainStack += c.forTextComponents().use { implicit textC => MultiLineTextView.contextual(
				"Löysin uusia tiedostoja luettavaksi.\nKertoisitko miten näitä tiedostoja tulee tulkita?",
				Screen.width / 3, useLowPriorityForScalingSides = true, isHint = true) }
			mainStack += settingsViewStack
			mainStack += Stack.buildRowWithContext() { buttonRow =>
				buttonRow += nextButton
				buttonRow += closeButton
			}.alignedToSide(Direction2D.Right)
		}.framed(margins.medium.any, c.containerBackground)
		val dialog = Frame.windowed(content, "Uusien tiedostojen asetukset", Program)
		dialog.centerOnScreen()
		dialog.setToCloseOnEsc()
		dialog.startEventGenerators(c.actorHandler)
		
		nextButton.registerAction { () =>
			// Checks input first, may present a pop-up dialog prompting for additional input
			val (emptyFields, readyInput) = rowStack.components.map { _.currentInput }.divided
			emptyFields.headOption match
			{
				case Some(emptyField) => displayPopup(emptyField)
				case None =>
					output = readyInput
					dialog.close()
			}
		}
		closeButton.registerAction(dialog.close)
		
		dialog
	}
	
	private val future = dialog.closeFuture.map { _ =>
		rowStack.components.foreach { _.end() }
		output
	}
	
	
	// IMPLEMENTED  ---------------------------------
	
	override def position = dialog.position
	
	override def position_=(p: Point) = dialog.position = p
	
	override def size = dialog.size
	
	override def size_=(s: Size) = dialog.size = s
	
	
	// OTHER    -------------------------------------
	
	/**
	 * Displays this dialog
	 * @return Future of the selection in this dialog
	 */
	def display() =
	{
		dialog.display()
		future
	}
	
	private def removeRow(rowToRemove: FileReadSettingInputRow): Unit =
	{
		rowStack -= rowToRemove
		rowToRemove.end()
	}
	
	private def displayPopup(emptyField: ComponentLike with AwtComponentRelated with Focusable): Unit =
	{
		baseContext.inContextWithBackground(colorScheme.error).forTextComponents().use { implicit context =>
			val closeButton = ImageButton.contextualWithoutAction(Icons.close.asIndividualButton)
			val content = Stack.buildRowWithContext(layout = Center) { s =>
				s += closeButton
				s += TextLabel.contextual("Tämä tieto tarvitaan vielä")
			}.framed(margins.small.any, context.containerBackground)
			val popup = Popup(emptyField, content, context.actorHandler) { (fieldSize, popupSize) =>
				Point(fieldSize.width + margins.medium, (fieldSize.height - popupSize.height) / 2) }
			popup.display()
			WaitUtils.delayed(5.seconds) { popup.close() }
			popup.closeFuture.foreach { _ => emptyField.requestFocusInWindow() }
		}
	}
}
