package spadi.view.dialog

import spadi.view.util.Setup._
import utopia.reflection.component.swing.TextField
import utopia.reflection.container.swing.window.dialog.interaction.{InputRowBlueprint, RowGroups}
import utopia.reflection.shape.LengthExtensions._

/**
 * Used for editing and creating shops
 * @author Mikko Hilpinen
 * @since 26.5.2020, v1.1
 */
class EditShopDialog(initialName: String = "") extends InputDialog[Option[String]]
{
	// ATTRIBUTES   -------------------------
	
	private implicit val languageCode: String = "fi"
	
	private val nameField = inputContext.forGrayFields.use { implicit c =>
		TextField.contextual(standardFieldWidth.any.expanding, initialText = initialName) }
	
	
	// IMPLEMENTED  -------------------------
	
	override protected def header = None
	
	override protected def fields = Vector(RowGroups.singleRow(new InputRowBlueprint("Tukun Nimi", nameField)))
	
	override protected def additionalButtons = Vector()
	
	override protected def produceResult = nameField.value match
	{
		case Some(shopName) => Right(Some(shopName))
		case None => Left(nameField, "Annathan vielä tukun nimen?")
	}
	
	override protected def defaultResult = None
	
	override protected def title = if (initialName.isEmpty) "Lisää tukku" else "Vaihda tukun nimi"
}
