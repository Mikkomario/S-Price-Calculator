package spadi.view.dialog

import java.nio.file.Path

import spadi.model.cached.pricing.shop.Shop
import spadi.view.util.Setup._
import spadi.model.cached.read.KeyMappingFactory
import utopia.flow.datastructure.immutable.Value
import utopia.flow.util.CollectionExtensions._
import utopia.flow.generic.ValueConversions._
import utopia.reflection.component.swing.input.TextField

/**
 * This data source dialog variation uses text fields for requesting data
 * @author Mikko Hilpinen
 * @since 27.5.2020, v1.1
 */
class DataSourceWindowWithTextFields[+A](path: Path, shop: Shop, mappingFactory: KeyMappingFactory[A])
	extends DataSourceWindowLike[A, TextField, TextField](path, shop, mappingFactory)
{
	// ATTRIBUTES   ------------------------
	
	private implicit val languageCode: String = "fi"
	
	protected override val (headerRowField, firstDataRowField) = inputContext.forGrayFields.use { implicit context =>
		TextField.contextualForPositiveInts(fieldWidth, prompt = Some("Ensimmäinen rivi on 1")) ->
			TextField.contextualForPositiveInts(fieldWidth, prompt = Some("Yleensä otsikkorivi +1"))
	}
	
	
	// IMPLEMENTED  ------------------------
	
	override protected def keyField(isRequired: Boolean) = inputContext.forGrayFields.use { implicit context =>
		TextField.contextual(fieldWidth, prompt = if (isRequired) None else Some("Vapaaehtoinen"))
	}
	
	override protected def valueOfField(field: Either[TextField, TextField]) =
		field.mapToSingle { f => f } { f => f }.value
	
	override protected def setFieldValue(field: Either[TextField, TextField], value: Value) =
		field.mapToSingle { f => f } { f => f }.text = value.string
}
