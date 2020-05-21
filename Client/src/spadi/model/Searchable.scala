package spadi.model

/**
 * Common trait for items that may be filtered / searched
 * @author Mikko Hilpinen
 * @since 21.5.2020, v1.1
 */
trait Searchable
{
	// ABSTRACT -----------------------------------
	
	/**
	 * @param search Search words
	 * @return How well this item matches that search
	 */
	def matches(search: Set[String]): Int
}
