class MetaDescriptionPlugin {	
	def setupBinding(binding) {
	    binding.description = pageProperty.curry('description')
	}

	private Closure pageProperty = { p, v ->
	    page."$p" = v
	}
}
