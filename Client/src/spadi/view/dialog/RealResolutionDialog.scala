package spadi.view.dialog

import spadi.view.component.Fields
import utopia.genesis.shape.shape2D.Size
import utopia.reflection.component.swing.{MultiLineTextView, Switch, TextField}
import utopia.reflection.shape.LengthExtensions._
import spadi.view.util.Setup._
import utopia.genesis.util.Screen
import utopia.genesis.util.DistanceExtensions._
import utopia.reflection.container.swing.window.dialog.interaction.{InputRowBlueprint, RowGroup, RowGroups}
import utopia.reflection.localization.DisplayFunction
import utopia.reflection.localization.LocalString._

/**
 * Used for requesting the user for the actual screen resolution
 * @author Mikko Hilpinen
 * @since 24.6.2020, v1.1
 */
class RealResolutionDialog extends InputDialog[Either[Boolean, Size]]
{
	// ATTRIBUTES   -----------------------------------
	
	private implicit val languageCode: String = "fi"
	
	private val defaultSize = Screen.size
	
	private val defaultIsCorrectSwitch = inputContext.use { implicit c =>
		Switch.contextual(1.cm.upTo(1.5.cm), initialState = true)
	}
	private val scalingDD = inputContext.forGrayFields.use { implicit c =>
		Fields.dropDown[Option[Double]]("Ei vaihtoehtoja saatavilla", "Valitse Tarkkuus",
			DisplayFunction.noLocalization[Option[Double]] {
				case Some(scaling) =>
					val newSize = defaultSize * scaling
					s"${newSize.width.toInt} x ${newSize.height.toInt}"
				case None => "Jokin muu"
			}
		)
	}
	private val (customWidthField, customHeightField) = inputContext.forGrayFields.use { implicit c =>
		val wField = TextField.contextualForPositiveInts(standardFieldWidth.any, Some(1920))
		val hField = TextField.contextualForPositiveInts(standardFieldWidth.any, Some(1080))
		wField -> hField
	}
	
	
	// INITIAL CODE -----------------------------------
	
	scalingDD.content = Vector(0.5, 0.75, 1.25, 1.5, 1.75, 2, 2.25, 2.5, 3, 3.5).map { Some(_) } :+ None
	
	
	// IMPLEMENTED  -----------------------------------
	
	override protected def header = standardContext.forTextComponents().mapFont { _ * 0.8 }
		.use { implicit c =>
			Some(MultiLineTextView.contextual("Joskus käyttöjärjestelmä ei kerro minulle oikeaa näytön tarkkuutta.\n" +
				"Voisitko tarkistaa näyttöasetukset -> näytön tarkkuus?", defaultSize.width / 2,
				useLowPriorityForScalingSides = true, isHint = true))
		}
	
	override protected def fields =
	{
		val showCustomFieldsPointer = scalingDD.valuePointer.map { _.exists { _.isEmpty } }
		Vector(
			RowGroups(
				Vector(
					RowGroup.singleRow(new InputRowBlueprint("Näyttöni tarkkuus on %s".localized
						.interpolated(Vector(s"${defaultSize.width.toInt} x ${defaultSize.height.toInt}")),
						defaultIsCorrectSwitch, spansWholeRow = false)),
					RowGroup.singleRow(new InputRowBlueprint("Valitse oikea tarkkuus", scalingDD,
						Some(defaultIsCorrectSwitch.valuePointer.map { _ == false }))),
					RowGroup(
						new InputRowBlueprint("Näytön leveys (pikseleinä)", customWidthField, Some(showCustomFieldsPointer)),
						new InputRowBlueprint("Näytön korkeus (pikseleinä)", customHeightField, Some(showCustomFieldsPointer))
					)
				)
			)
		)
	}
	
	override protected def additionalButtons = Vector()
	
	override protected def produceResult =
	{
		if (defaultIsCorrectSwitch.isOn)
			Right(Left(true))
		else
		{
			scalingDD.value match
			{
				case Some(scalingResult) =>
					scalingResult match
					{
						case Some(scaling) => Right(Right(defaultSize * scaling))
						case None =>
							customWidthField.intValue match
							{
								case Some(w) =>
									customHeightField.intValue match
									{
										case Some(h) => Right(Right(Size(w, h)))
										case None => Left(customHeightField -> "Tämä tieto tarvitaan")
									}
								case None => Left(customWidthField -> "Tämä tieto tarvitaan")
							}
					}
				case None => Left(scalingDD -> "Tämä tieto tarvitaan")
			}
		}
	}
	
	override protected def defaultResult = Left(false)
	
	override protected def title = "Näytön asetusten tarkistus"
}
