package grass
import grass.plugins.PluginEvent
import static grass.plugins.PluginEventType.*

import com.google.common.eventbus.Subscribe
import groovy.text.SimpleTemplateEngine

trait Events {
    def init() {}
    def setupBinding(payload) {}

    def beforePage(payload) {}
    def renderPage(payload) {}
    def afterPage(payload) {}

    def beforeIndex(payload) {}
    def afterIndex(payload) {}

    def beforeWrite(payload) {}
    def afterWrite(payload) {}
    def cleanup(){}
}


// added to this script as well as all plugins
trait GrassMixin extends Events {

    def eventBus = App.rootEventBus

	  def addPage(Map data) {
		    addPage(new Page(data))
	  }
    def defaultHandler = { clazz, eventType ->
        println "${clazz} doesn't know how to handle " + eventType
    }

    @Subscribe
    public void listener(evt) {
        switch(evt.eventType) {
            case INIT:
                init();
                break;
            case SETUP_BINDING:
                setupBinding(evt.payload)
                break;
            case BEFORE_PAGE:
                beforePage(evt.payload)
                break
            case AFTER_PAGE:
                afterPage(evt.payload)
                break
            case BEFORE_INDEX:
                beforeIndex(evt.payload)
                break
            case AFTER_INDEX:
                afterIndex(evt.payload)
                break
            case RENDER_PAGE:
                renderPage(evt.payload)
                break
            case BEFORE_WRITE:
                beforeWrite(evt.payload)
                break
            case AFTER_WRITE:
                afterWrite(evt.payload)
                break
            default:
                defaultHandler(this.getClass(), evt.eventType)
                break
        }
    }


	  def addPage(Page page) {
		    def engine = new SimpleTemplateEngine()
		    // preprocess page
        eventBus.post( [eventType:BEFORE_PAGE, payload:page ] as PluginEvent)

		    def binding = newBinding(page: page)

		    // evaluate page as groovy template
		    page.content = evaluate(page.content, binding)

		    // render page
        eventBus.post( [ eventType: RENDER_PAGE, payload:page] as PluginEvent )

		    // apply page template
		    applyTemplate(page, binding)

		    // post process page
        eventBus.post( [ eventType: AFTER_PAGE, payload: page ] as PluginEvent )

		    // add the page to the list
		    config.pages << page
	  }

	  def evaluate(template, binding) {
		    binding.values().each { v ->
			      if (v instanceof Closure) {
				        v.delegate = binding
			      }
		    }
		    new SimpleTemplateEngine().createTemplate(template).make(binding).toString()
	  }

	  def applyTemplate(page, binding) {
		    if (!page.out || !page.template) { return }

        def templates = new BlogTemplates(config)
        if (page.template.equals("blog/post")) {
            page.content = templates.post(page)
        }

        if (page.template.equals("index")) {
            page.content = templates.indexPage(page)
        }

        // Mostly Old template system
		    //page.content = applyTemplate(page.template, page.content, binding)
	  }


	  def applyTemplate(id, content, binding) {
		    def template = findTemplate(id, binding)

		    if (template) {
			      binding['.'] = template
			      binding.content = content
			      // apply the template
			      evaluate(template.text, binding)
		    } else {
			      content
		    }
	  }

	  def findTemplate(id, binding = [:]) {
		    // check relative to '.' in the binding
		    if (!id.startsWith('/') && binding['.'] instanceof File) {
			      def relative = binding['.']
			      if (relative?.isFile()) {
				        relative = relative.parentFile
			      }
			      def test = new File(relative, id)
			      if (test.exists()) {
				        return test
			      }
			      test = new File(relative, "${id}.html")
			      if (test.exists()) {
				        return test
			      }
		    }

		    // strip leading slash
		    def path = id
		    if (path.startsWith('/')) {
			      path = path[1..-1]
		    }

		    // check all template roots
		    expandPaths(config?.paths?.templates ?: []).inject([]) { list, dir ->
			      list << new File(dir, path)
			      list << new File(dir, "${path}.html")
			      list
		    }.find { it.exists() }
	  }

	  def expandPaths(paths) {
		    [paths].flatten().inject([]) { list, path ->
			      list << new File(config.source, path)
			      list << new File(path)
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

	  def newBinding(Map binding = [:]) {
		    binding.config = config
        eventBus.post( [ eventType: SETUP_BINDING, payload: binding ] as PluginEvent )
		    binding
	  }

	  def fail(msg) {
		    System.err.println(msg)
		    System.exit(-1)
	  }
}
