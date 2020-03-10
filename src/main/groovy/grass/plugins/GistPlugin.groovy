package grass.plugins
import grass.*

class GistPlugin implements GrassMixin{
	def config

	def setupBinding(binding) {
		binding.gist = { user, id ->
			"""<script src="https://gist.github.com/${user}/${id}.js"></script>"""
		}
	}
}
