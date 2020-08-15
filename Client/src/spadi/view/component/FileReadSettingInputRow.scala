package spadi.view.component

import java.nio.file.Path

import spadi.controller.Log
import spadi.model.cached.read
import spadi.model.cached.read.FileReadSetting
import spadi.model.enumeration.PriceInputType
import spadi.model.stored.pricing.Shop
import spadi.view.controller.ShopSelectionVC
import spadi.view.util.Icons
import spadi.view.util.Setup._
import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.FileExtensions._
import utopia.reflection.component.context.TextContext
import utopia.reflection.component.swing.StackSpace
import utopia.reflection.component.swing.button.ImageAndTextButton
import utopia.reflection.component.swing.label.TextLabel
import utopia.reflection.component.swing.template.StackableAwtComponentWrapperWrapper
import utopia.reflection.container.swing.layout.SegmentGroup
import utopia.reflection.container.swing.layout.multi.Stack
import utopia.reflection.container.swing.window.interaction.ButtonColor.Fixed
import utopia.reflection.container.swing.window.interaction.YesNoWindow
import utopia.reflection.localization.{DisplayFunction, LocalizedString}
import utopia.reflection.localization.LocalString._
import utopia.reflection.shape.LengthExtensions._
import utopia.reflection.shape.StackSize

import scala.util.{Failure, Success}

object FileReadSettingInputRow
{
	private val fileSizeWarningThreshold = 3000000 // 3 Mb
	private val criticalFileSizeWarningThreshold = 10000000 // 10 Mb
	
	private val fileConversionHelpPath: Path = "ohjeet/tiedoston-muuntaminen-csv.pdf"
}

/**
 * Used for inputting file read settings
 * @author Mikko Hilpinen
 * @since 26.5.2020, v1.1
 */
class FileReadSettingInputRow(group: SegmentGroup, base: Either[Path, FileReadSetting],
							  shopsPointer: PointerWithEvents[Vector[Shop]])
							 (onDeleteRequested: FileReadSettingInputRow => Unit)(implicit context: TextContext)
	extends StackableAwtComponentWrapperWrapper
{
	// ATTRIBUTES   ------------------------------
	
	import FileReadSettingInputRow._
	
	private implicit val languageCode: String = "fi"
	
	private val (shopSelection, typeSelection) = context.forGrayFields.use { implicit ddC =>
		val shopSelection = new ShopSelectionVC(shopsPointer)
		val typeSelection = Fields.dropDown[PriceInputType]("Sisältötyyppejä ei ole määritetty",
			"Valitse tiedostotyyppi", DisplayFunction.localized[PriceInputType] { _.name })
		typeSelection.content = PriceInputType.values
		shopSelection -> typeSelection
	}
	// private val isSortedSwitch = Switch.contextual(standardSwitchWidth.downscaling, initialState = true)
	// If file size is very large and excel format is used, displays a warning
	// (program may run out of memory if such files are read)
	private val warning =
	{
		val path = base.mapToSingle { p => p } { _.path }
		if (path.fileType == "csv")
			None
		else
		{
			path.size match
			{
				case Success(fileSize) =>
					if (fileSize >= fileSizeWarningThreshold)
					{
						val isCritical = fileSize >= criticalFileSizeWarningThreshold
						val text: LocalizedString = "Tiedosto on hyvin suuri ja ohjelmalta saattaa loppua muisti " +
							"sitä luettaessa.\nSuosittelen muuttamaan tiedoston .csv muotoon tai pilkkomaan sen osiin."
						if (fileConversionHelpPath.exists)
							Some(Warning.actionable(text, isCritical) { implicit c =>
								ImageAndTextButton.contextual(Icons.help.inButton, "Ohje") {
									fileConversionHelpPath.openInDesktop().failure.foreach { error =>
										Log(error, "Failed to open help file")
										fileConversionHelpPath.openFileLocation().failure.foreach { error2 =>
											Log(error2, "Failed to open help file location")
											Fields.errorDialog("Jokin meni mönkään ohjeen avaamisessa :(")
												.display(parentWindow)
										}
									}
								} }(context.base))
						else
							Some(Warning.nonActionable(text, isCritical)(context.base))
					}
					else
						None
				case Failure(error) =>
					Log(error, s"Failed to read file size of $path")
					None
			}
		}
	}
	
	private val (openButton, deleteButton) = context.forPrimaryColorButtons.use { implicit btnC =>
		val openFileButton = Fields.openFileButton(path, parentWindow)
		val deleteFileButton = ImageAndTextButton.contextual(Icons.delete.inButton, "Poista") {
			parentWindow.foreach { window =>
				val dialogContext = baseContext.inContextWithBackground(colorScheme.gray.light).forTextComponents()
				new YesNoWindow(dialogContext, "Tiedoston poisto",
					"Haluatko varmasti poistaa tämän tiedoston koneeltasi.\nTätä toimintoa ei voi peruuttaa.",
					Map(true -> Icons.delete, false -> Icons.close), Map(true -> Fixed(colorScheme.error)))({ (color, _) =>
					dialogContext.forButtons(color) }).displayOver(window)
					.foreach { shouldDelete =>
						if (shouldDelete)
						{
							path.delete().failure.foreach { Log(_, s"Failed to delete $path") }
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
		Stack.rowWithItems(group.wrap(Vector(pathLabel, shopSelection, typeSelection,
			warning.map { _.alignedToCenter }.getOrElse(new StackSpace(StackSize.any.withLowPriority)),
			/*isSortedSwitch,*/ openButton, deleteButton)), margins.medium.downscaling)
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
				case Some(inputType) => Right(read.FileReadSetting(path, shop, inputType/*, isSortedSwitch.value*/))
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
