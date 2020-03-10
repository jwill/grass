package grass.plugins
import grass.*
import java.time.*
import java.time.format.DateTimeFormatter
import java.text.SimpleDateFormat
import static grass.plugins.PluginEventType.*
import com.google.common.eventbus.Subscribe

import static j2html.TagCreator.*;


class BlogPlugin implements GrassMixin {
	  def FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE
	  def INDEX_TAG = /<blog:index\/>/
	  def RECENT_TAG = /<blog:recent\/>/
	  def PREVIOUS_TAG = /<blog:previous\/>/
	  def NEXT_TAG = /<blog:next\/>/

	  def paths
	  def config
	  def posts
	  def recent

	  def init() {
        println "init in Blog plugin"
		    // add 'paths' to the page search path
		    paths = config?.paths?.posts ?: []
		    if (paths && config?.paths?.pages) {
			      config.paths.pages.addAll(paths)
		    }

		    if (config?.plugin?.links?.dispatch) {
			      config?.plugin?.links?.dispatch << 'post'
		    }
	  }

	  def setupBinding(binding) {
		    binding.createLinkToPost = { Page post, boolean absolute ->
			      binding.createLinkToPage(post, absolute)
		    }
	  }

	  def beforePage(page) {
		    decoratePage(page)

		    if (page.post) {
			      // set the template to post
			      page.template = 'blog/post'

			      // check for date in filename
			      if (page.name =~ /^(19|20)\d\d-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])/) {
				        page.date = LocalDate.parse(page.name[0..9], FORMATTER)
                page.dateString = page.date.format("yyyy/MM/dd")
				        page.name = page.name.substring(11)
				        page.title = page.title.substring(11)
			      }
		    }
	  }

	  def afterPage(page) {
		    if (page.post) {
			      // rewrite output filename using date
			      page.out = "${page.dateString}/${page.name}.html"
		    }
	  }

	  def beforeIndex(payload) {
		    def index = payload[0]
        def pages = payload[1]
        println "beforeIndex" + index
        println "pages size" + pages.size

        //def x = applyTemplate('blog/index.html', '', newBinding(posts: pages.take(5)))
        //println(x)

		    // if the index has no content, insert the blog summary tag
		    if (!index.content) {
			      index.content = INDEX_TAG
		    }
	  }

	  def afterIndex(payload) {
		    def index = payload[0]
		    def pages = payload[1]
		    posts = pages.findAll { it.post }.sort { a, b -> b.date <=> a.date }
		    recent = posts.take(config?.blog?.recent ?: 5)
	  }

    def summaryTemplate(post) {
        article(
        header(
                p(tag("date").withText(post.date.toString())),
                h2(post.title)
            ),
            rawHtml(post.summary),
        )
    }

	  def beforeWrite(page) {
        if (page.content.contains(INDEX_TAG)) {
            def builder = new StringBuilder()
            recent.each {
                builder.append summaryTemplate(it).render()
            }
            page.content = page.content.replace(INDEX_TAG, builder.toString())
        }

     // TODO Refactored -- Unused currently
		// if (page.content.contains(RECENT_TAG)) {
		// 	      page.content = page.content.replace(RECENT_TAG, applyTemplate('blog/recent.html', '', newBinding(posts: recent)).toString())
		//     }
		//     if (page.content.contains(PREVIOUS_TAG)) {
		// 	      def previous = posts.indexOf(page) + 1
		// 	      if (previous < posts.size()) {
		// 		        page.content = page.content.replace(PREVIOUS_TAG, applyTemplate('blog/_previous.html', '', newBinding(post: posts[previous])).toString())
		// 	      } else {
		// 		        page.content = page.content.replace(PREVIOUS_TAG, '')
		// 	      }
		//     }
		//     if (page.content.contains(NEXT_TAG)) {
		// 	      def next = posts.indexOf(page) - 1
		// 	      if (next >= 0) {
		// 		        page.content = page.content.replace(NEXT_TAG, applyTemplate('blog/_next.html', '', newBinding(post: posts[next])).toString())
		// 	      } else {
		// 		        page.content = page.content.replace(NEXT_TAG, '')
		// 	      }
		//     }
	  }

	  private decoratePage(page) {
		    def isPost = paths.find { page.out.startsWith("/${it}") } != null
		    page.metaClass.post = isPost
	  }
}
