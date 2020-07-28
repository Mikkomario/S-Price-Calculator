package spadi.model.cached.read

import utopia.flow.datastructure.immutable.{ModelDeclaration, PropertyDeclaration}
import utopia.flow.generic.{FromModelFactoryWithSchema, StringType}
import utopia.reflection.localization.LocalString

/**
  * Used for creating new key mappings
  * @author Mikko Hilpinen
  * @since 27.5.2020, v1.1
  */
trait KeyMappingFactory[+A] extends FromModelFactoryWithSchema[KeyMapping[A]]
{
	// ABSTRACT ------------------------------------
	
	/**
	  * @return Displayable names of the fields that are used for creating a new key mapping. Each name should be paired
	  *         with a boolean that indicates whether the field is required.
	  */
	def fieldNames: Seq[(LocalString, Boolean)]
	
	
	// IMPLEMENTED  --------------------------------
	
	override def schema = ModelDeclaration(fieldNames.filter
	{_._2}.map
	{ case (fName, _) =>
		PropertyDeclaration(fName.string, StringType)
	})
}
