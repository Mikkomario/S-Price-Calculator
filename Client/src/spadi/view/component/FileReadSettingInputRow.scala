package spadi.view.component

import java.nio.file.Path

import spadi.view.util.Setup._
import spadi.model.PriceInputType
import spadi.view.controller.ShopSelectionVC
import spadi.view.util.Icons
import utopia.flow.util.FileExtensions._
import utopia.flow.util.CollectionExtensions._
import utopia.reflection.component.context.TextContext
import utopia.reflection.component.swing.StackableAwtComponentWrapperWrapper
import utopia.reflection.component.swing.button.ImageAndTextButton
import utopia.reflection.component.swing.label.TextLabel
import utopia.reflection.container.stack.segmented.SegmentedGroup
import utopia.reflection.container.swing.SegmentedRow
import utopia.reflection.container.swing.window.dialog.interaction.ButtonColor.Fixed
import utopia.reflection.container.swing.window.dialog.interaction.{MessageDialog, YesNoDialog}
import utopia.reflection.localization.DisplayFunction
import utopia.reflection.localization.LocalString._
import utopia.reflection.shape.LengthExtensions._

/**
 * Used for inputting file read settings
 * @author Mikko Hilpinen
 * @since 26.5.2020, v1.1
 */
class FileReadSettingInputRow(group: SegmentedGroup, path: Path)(onDeleteRequested: => Unit)
                             (implicit context: TextContext)
	extends StackableAwtComponentWrapperWrapper
{
	// ATTRIBUTES   ------------------------------
	
	private implicit val languageCode: String = "fi"
	
	private val (shopSelection, typeSelection) = context.forGrayFields.use { implicit ddC =>
		val shopSelection = new ShopSelectionVC()
		val typeSelection = Fields.dropDown[PriceInputType]("Sisältötyyppejä ei ole määritetty",
			"Valitse tiedostotyyppi", DisplayFunction.localized[PriceInputType] { _.name })
		shopSelection -> typeSelection
	}
	
	private val (openButton, deleteButton) = context.forPrimaryColorButtons.use { implicit btnC =>
		val openFileButton = ImageAndTextButton.contextual(Icons.file.inButton, "Avaa tiedosto") {
			path.openInDesktop().failure.foreach { error =>
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
		val deleteFileButton = ImageAndTextButton.contextual(Icons.delete.inButton, "Poista tiedosto") {
			parentWindow.foreach { window =>
				val dialogContext = baseContext.inContextWithBackground(colorScheme.gray.light).forTextComponents()
				new YesNoDialog(dialogContext, "Tiedoston poisto",
					"Haluatko varmasti poistaa tämän tiedoston koneeltasi.\nTätä toimintoa ei voi peruuttaa.",
					Map(true -> Icons.delete, false -> Icons.close), Map(true -> Fixed(colorScheme.error)))({ (color, _) =>
					dialogContext.forButtons(color) }).display(window)
					.foreach { shouldDelete =>
						if (shouldDelete)
						{
							path.delete()
							onDeleteRequested
						}
					}
			}
		}
		openFileButton -> deleteFileButton
	}
	
	private val view =
	{
		val pathLabel = TextLabel.contextual(path.toString.noLanguageLocalizationSkipped)
		SegmentedRow.partOfGroupWithItems(group,
			Vector(pathLabel, shopSelection, typeSelection, openButton, deleteButton), margins.medium.downscaling)
	}
	
	
	// IMPLEMENTED  -----------------------------
	
	override protected def wrapped = view
	
	
	// OTHER    ---------------------------------
	
	/**
	 * This method should be called before this component is disposed
	 */
	def end() =
	{
		shopSelection.end()
		group.remove(view)
	}
}
