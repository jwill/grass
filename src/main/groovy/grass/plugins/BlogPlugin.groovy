package grass.plugins

import grass.*
import java.time.*
import java.time.format.DateTimeFormatter
import java.text.SimpleDateFormat
import static grass.plugins.PluginEventType.*
import com.google.common.eventbus.Subscribe

import static j2html.TagCreator.*;


class BlogPlugin implements GrassMixin {
    def templates

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
        templates = new BlogTemplates(config)
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

    def beforeWrite(page) {
        if (page.content.contains(INDEX_TAG)) {
            def builder = new StringBuilder()
            def templates = new BlogTemplates(config)

            def content
            if (config.indexPage.equals("full")) {
                content = templates.indexPosts(posts)
            } else {
                def i = Integer.parseInt(config.indexPage)
                content = templates.indexPosts(posts.take(i))
            }
            page.content = page.content.replace(INDEX_TAG, content)
        }

        // TODO Refactored -- Unused currently
        // if (page.content.contains(RECENT_TAG)) {
        // 	      page.content = page.content.replace(RECENT_TAG, applyTemplate('blog/recent.html', '', newBinding(posts: recent)).toString())
        //     }
        if (page.content.contains(PREVIOUS_TAG)) {
            def previousPostIndex = posts.indexOf(page) + 1
            if (previousPostIndex < posts.size()) {
                def previousPost = posts[previousPostIndex]
                page.content = page.content.replace(PREVIOUS_TAG, templates.previousPost(previousPost))
            } else {
                page.content = page.content.replace(PREVIOUS_TAG, '')
            }
        }

        if (page.content.contains(NEXT_TAG)) {
            def nextPostIndex = posts.indexOf(page) - 1
            if (nextPostIndex >= 0) {
                def nextPost = posts[nextPostIndex]
                page.content = page.content.replace(NEXT_TAG, templates.nextPost(nextPost))
            } else {
                page.content = page.content.replace(NEXT_TAG, '')
            }
        }
    }

    private decoratePage(page) {
        def isPost = paths.find { page.out.startsWith("/${it}") } != null
        page.metaClass.post = isPost
    }
}
