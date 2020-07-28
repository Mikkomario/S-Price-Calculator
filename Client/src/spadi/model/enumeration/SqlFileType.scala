package spadi.model.enumeration

/**
  * Enumeration for different sql file purposes
  * @author Mikko Hilpinen
  * @since 28.7.2020, v1.2
  */
sealed trait SqlFileType

object SqlFileType
{
	// OTHER	------------------------
	
	/**
	  * Interprets a sql file type string
	  * @param typeString A sql file type string
	  * @return File type that best matches specified string
	  */
	def forString(typeString: String) =
	{
		val lower = typeString.toLowerCase
		
		if (lower.contains("full"))
			Full
		else if (lower.contains("changes") || lower.contains("update"))
			Changes
		else
			Full
	}
	
	
	// NESTED	------------------------
	
	/**
	  * Used when a file contains the whole database structure
	  */
	object Full extends SqlFileType
	
	/**
	  * Used when a file contains changes between database versions
	  */
	object Changes extends SqlFileType
}
