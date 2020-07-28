package spadi.model.cached.pricing.product

/**
  * Common trait for items that can be filtered / searched with words
  * @author Mikko Hilpinen
  * @since 21.5.2020, v1.1
  */
trait KeywordSearchable extends Searchable
{
	// ABSTRACT ----------------------------
	
	/**
	  * @return Search keywords used for this item. <b>Should all be lower case</b>.
	  */
	def keywords: Vector[String]
	
	
	// IMPLEMENTED    -----------------------
	
	/**
	  * @param search Search words
	  * @return How well this item matches that search
	  */
	def matches(search: Set[String]) = search.count
	{ s => keywords.exists
	{_.contains(s)}
	}
}
