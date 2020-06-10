package spadi.view.component

import java.nio.file.Path

import spadi.view.util.Setup._
import spadi.model.{FileReadSetting, PriceInputType}
import spadi.view.controller.ShopSelectionVC
import spadi.view.util.Icons
import utopia.flow.util.FileExtensions._
import utopia.reflection.component.context.TextContext
import utopia.reflection.component.swing.StackableAwtComponentWrapperWrapper
import utopia.reflection.component.swing.button.ImageAndTextButton
import utopia.reflection.component.swing.label.TextLabel
import utopia.reflection.container.swing.{SegmentGroup,Stack}
import utopia.reflection.container.swing.window.dialog.interaction.ButtonColor.Fixed
import utopia.reflection.container.swing.window.dialog.interaction.YesNoDialog
import utopia.reflection.localization.DisplayFunction
import utopia.reflection.localization.LocalString._
import utopia.reflection.shape.LengthExtensions._

/**
 * Used for inputting file read settings
 * @author Mikko Hilpinen
 * @since 26.5.2020, v1.1
 */
class FileReadSettingInputRow(group: SegmentGroup, base: Either[Path, FileReadSetting])
                             (onDeleteRequested: FileReadSettingInputRow => Unit)(implicit context: TextContext)
	extends StackableAwtComponentWrapperWrapper
{
	// ATTRIBUTES   ------------------------------
	
	private implicit val languageCode: String = "fi"
	
	private val (shopSelection, typeSelection) = context.forGrayFields.use { implicit ddC =>
		val shopSelection = new ShopSelectionVC()
		val typeSelection = Fields.dropDown[PriceInputType]("Sisältötyyppejä ei ole määritetty",
			"Valitse tiedostotyyppi", DisplayFunction.localized[PriceInputType] { _.name })
		typeSelection.content = PriceInputType.values
		shopSelection -> typeSelection
	}
	
	private val (openButton, deleteButton) = context.forPrimaryColorButtons.use { implicit btnC =>
		val openFileButton = Fields.openFileButton(path, parentWindow)
		val deleteFileButton = ImageAndTextButton.contextual(Icons.delete.inButton, "Poista") {
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
							onDeleteRequested(this)
						}
					}
			}
		}
		openFileButton -> deleteFileButton
	}
	
	private val view =
	{
		val pathLabel = TextLabel.contextual(path.fileName.noLanguageLocalizationSkipped)
		Stack.rowWithItems(group.wrap(Vector(pathLabel, shopSelection, typeSelection, openButton, deleteButton)),
			margins.medium.downscaling)
	}
	
	
	// INITIAL CODE -----------------------------
	
	// On edit mode, sets default values
	base.toOption.foreach { settings =>
		shopSelection.value = Some(settings.shop)
		typeSelection.value = Some(settings.inputType)
	}
	
	
	// COMPUTED ---------------------------------
	
	/**
	 * @return Either Right: Current user input in this row or Left: Field that is missing a value
	 */
	def currentInput = shopSelection.value match
	{
		case Some(shop) =>
			typeSelection.value match
			{
				case Some(inputType) => Right(FileReadSetting(path, shop, inputType))
				case None => Left(typeSelection)
			}
		case None => Left(shopSelection)
	}
	
	private def path = base match
	{
		case Right(settings) => settings.path
		case Left(path) => path
	}
	
	
	// IMPLEMENTED  -----------------------------
	
	override protected def wrapped = view
	
	
	// OTHER    ---------------------------------
	
	/**
	 * This method should be called before this component is disposed
	 */
	def end() = shopSelection.end()
}
