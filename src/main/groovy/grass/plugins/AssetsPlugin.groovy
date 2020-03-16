package grass.plugins
import grass.*
import com.google.common.io.Files

class AssetsPlugin implements GrassMixin{
	  def config

	  def init() {
		    if (config?.plugin?.links?.dispatch) {
			      config?.plugin?.links?.dispatch << 'asset'
		    }
	  }

	  def setupBinding(binding) {
        binding.findAssetPath = { String name ->
            // returns parent path or null
            return config?.paths?.assets.find {
                def global = new File(config.source, it)

                global.exists() && new File(global, name).exists()
            }
        }

		    binding.createLinkToAsset = { String name, boolean absolute ->
			      // check if the asset exists

            def asset = binding.findAssetPath(name)

            if (asset != null)
                binding.createLinkToUrl(asset ? "/${asset}/${name}" : "${name}", absolute)
		    }

        binding.image = {String filename, String altDesc ->
            def path = binding.createLinkToAsset(filename, false)
            if (path == null)
                ""
            else {
                """<div>
            <img src=\"/blog/${path}\" alt=\"${altDesc}\">
            <center><span>${altDesc}</span></center>
            </div>
            """
            }
        }

        binding.snippet = { String name ->
            def path = binding.findAssetPath(name)
            if (path == null)
                ""
            else {
                def directory = new File(config.source, path)
                def assetPath = new File(directory, name)
                "<pre><code>${assetPath.getText()}</code></pre>"
            }

        }
	  }

	  def afterIndex(payload) {
		    def index = payload[0]
		    def pages = payload[1]
		    // walk through our paths and copy over assets
		    config?.paths?.assets?.each { path ->
			      def global = new File(path)
			      if (global.exists()) {
				        copy(global, new File(config.destination, path))
			      }

			      def local = new File(config.source, path)
			      if (local.exists()) {
				        copy(local, new File(config.destination, path))
			      }
		    }
	  }

	  private copy(from, to) {
		    def ant = new AntBuilder()
		    if (from.directory) {
			      ant.copy(todir: to.absolutePath) {
				        fileset(dir: from.absolutePath)
			      }
		    } else {
			      ant.copy(todir: config.destination.absolutePath) {
				        fileset(file: from.absolutePath)
			      }
		    }
	  }
}
