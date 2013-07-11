import groovy.text.SimpleTemplateEngine

class Page {
	File path
	String template
	String name
	String out
	String title
	Date date
	String summary
	String content
	List tags = []
}

// command line parsing
def cli = new CliBuilder(usage: 'groovy grass -s "source" -d "destination"')
cli.s(longOpt: 'source', args: 1, required: true, 'source')
cli.d(longOpt: 'destination', args: 1, required: true, 'destination')

// parse options
opt = cli.parse(args)
if (!opt) { return }

// load our config
config = loadConfig()
config.source = new File("${opt.s}")
config.destination = new File("${opt.d}")
config.destination.mkdirs()

// load the plugins
plugins = loadPlugins()
trigger('init')

// find all pages
pages = loadPages()

// render pages
renderPages()

// render the index
renderIndex()

// write pages
writePages()

// trigger a cleanup event
trigger('cleanup')

/* helper methods */
def renderIndex() {
	def index = new Page(content: '', template: 'index', name: config?.site?.name ?: 'Index', title: config?.site?.title, date: new Date(), out: 'index.html')
	pages << index
	def engine = new SimpleTemplateEngine()

	// preprocess index
	trigger('beforeIndex', [index, pages])
	trigger('beforePage', index)

	// add all pages to the binding
	def binding = newBinding(index)
	binding.pages = pages

	// evaluate index as groovy template
	index.content = engine.createTemplate(index.content).make(binding.variables)

	// render index
	trigger('renderIndex', [index, pages])
	trigger('renderPage', index)

	// apply index template
	applyTemplate(index, binding)

	// post process index
	trigger('afterIndex', [index, pages])
	trigger('afterPage', index)
}

def renderPages() {
	def engine = new SimpleTemplateEngine()

	pages.each { page ->
		// preprocess page
		trigger('beforePage', page)

		def binding = newBinding(page)

		// evaluate page as groovy template
		page.content = engine.createTemplate(page.content).make(binding.variables)

		// render page
		trigger('renderPage', page)

		// apply page template
		applyTemplate(page, binding)

		// post process page
		trigger('afterPage', page)
	}
}

def writePages() {
	pages.each { page ->
		writeFile(page.out, page.content)
	}
}

def applyTemplate(id, content, binding) {
	def template = expandPaths(config?.paths?.templates ?: []).inject([]) { list, dir ->
		list << new File(dir, id)
		list << new File(dir, "${id}.html")
		list
	}.find { it.exists() }

	if (template) {
		// apply the template
		new SimpleTemplateEngine().createTemplate(template.text).make(binding.variables)
	} else {
		content
	}
}

def applyTemplate(page, binding) {
	if (!page.out || !page.template) { return }
	page.content = applyTemplate(page.template, page.content, binding)
}

def newBinding(page) {
	def binding = new Binding(config: config, page: page)
	trigger('setupBinding', [binding])
	binding
}

def loadConfig() {
	def config = new ConfigObject()
	def global = new File("global-config.groovy")
	if (global.exists()) {
		config.merge(new ConfigSlurper().parse(global.toURL()))
	}
	def local = new File("${opt.s}/site-config.groovy")
	if (local.exists()) {
		config.merge(new ConfigSlurper().parse(local.toURL()))
	}
	config
}

def loadPlugins() {
	def plugins = []
	def enabled = config?.plugins?.enabled ?: []
	def disabled = config?.plugins?.disabled ?: []

	// load the plugin classes
	def classloader = new GroovyClassLoader()
	expandPaths(config?.paths?.plugins ?: []).each { dir ->
		dir.eachFileMatch(~/.*\.groovy/) { file ->
			def clazz = classloader.parseClass(file)
			if ((!enabled || enabled.contains(clazz.simpleName)) && !disabled.contains(clazz.simpleName)) {
				def instance = clazz.newInstance()
				if (instance.hasProperty('config')) {
					instance.config = config
				}
				plugins << instance
			}
		}
	}

	plugins
}

def loadPages() {
	def pages = []

	expandPaths(config?.paths?.pages ?: []).each { dir ->
		dir.eachFile { file ->
			// create our page object
			def name = file.name
			if (name.lastIndexOf('.') > 0) {
				name = name[0..(name.lastIndexOf('.') - 1)]
			}
			def title = name.split('-').collect { it.capitalize() }.join(' ')
			def out = file.parentFile.absolutePath - config.source.absolutePath + "${File.separator}${name}.html"

			pages << new Page(path: file, content: file.text, template: 'page', name: name, title: title, date: new Date(file.lastModified()), out: out)
		}
	}

	pages
}

def trigger(event, args = null) {
	plugins.each { plugin ->
		if (plugin.respondsTo(event)) {
			if (!args) {
				plugin."$event"()
			} else if (args instanceof List) {
				plugin."$event"(*args)
			} else {
				plugin."$event"(args)
			}
		}
	}
}

def expandPaths(paths) {
	[paths].flatten().inject([]) { list, path ->
		list << new File(path)
		list << new File(config.source, path)
		list
	}.findAll { it.exists() }
}

def writeFile(path, content) {
	if (path && content) {
		def out = new File(config.destination, path)
		out.parentFile.mkdirs()
		out.write(content)
	}
}