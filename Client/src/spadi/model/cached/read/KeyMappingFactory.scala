package spadi.model.cached.read

import utopia.flow.datastructure.immutable.{ModelDeclaration, PropertyDeclaration}
import utopia.flow.generic.{FromModelFactoryWithSchema, StringType}
import utopia.reflection.localization.LocalString

/**
  * Used for creating new key mappings
  * @author Mikko Hilpinen
  * @since 27.5.2020, v1.1
  */
trait KeyMappingFactory[+A, +M <: KeyMapping[A]] extends FromModelFactoryWithSchema[M]
{
	// ABSTRACT ------------------------------------
	
	/**
	  * @return Fields that are used for creating a new key mapping. Each field matches a mapped property.
	  */
	def fields: Seq[InputField]
	
	
	// IMPLEMENTED  --------------------------------
	
	override def schema = ModelDeclaration(fields.filter { _.isRequired }.map { field =>
		PropertyDeclaration(field.name.string, StringType)
	})
}
