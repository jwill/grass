package grass.plugins

import grass.*

class MetaDescriptionPlugin implements GrassMixin{
	  def setupBinding(binding) {
	      binding.description = pageProperty.curry('description')
	  }

	  private Closure pageProperty = { p, v ->
	      page."$p" = v
	  }
}
