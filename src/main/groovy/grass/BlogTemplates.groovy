package grass

import static j2html.TagCreator.*;
import java.time.LocalDate

import groovy.text.SimpleTemplateEngine

class BlogTemplates {
    def config
    def plugins
    def link

    def headerText = new File("templates/_head.html").getText()
    def navBarText = new File("templates/_navBar-infogram.html").getText()
    def footerText = new File("templates/_footer.html").getText()

    public BlogTemplates(config) {
        this.config = config
        plugins = config.get("plugins")
        link = plugins.get("LinksPlugin")

    }

    def brief(post) {
        article(
                header(
                        p(attrs(".meta"),
                                time(post.date.toString())
                        ),
                        h1(
                                a(post.title).withHref(link.createLinkToPage(post, true).toString)
                        )
                ),
                footer(
                        rawHtml("<tags:page/>")
                )
        ).render()
    }

    def full(post) {
        article(
                tag("time").withText(post.date.toString()),
                br(),
                h2(post.title),
                rawHtml(post.content),
                p(),
                footer(
                        nav(
                                rawHtml("<blog:previous/>"),
                                rawHtml("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"),
                                rawHtml("<blog:next/>")
                        )
                )
        ).render()
    }

    def list(post) {
        li(attrs("#post"),
                a(post.title).withHref(link.createLinkToPage(post, true)).render())
    }

    def nextPost(post) {
        a(attrs(".next"), rawHtml(post.title + "&rarr;"))
                .withRel("next-article")
                .withHref(link.createLinkToPage(post, true).toString())
                .render()
    }

    def previousPost(post) {
        a(attrs(".prev"), rawHtml("&larr;" + post.title))
                .withRel("next-article")
                .withHref(link.createLinkToPage(post, true).toString())
                .render()
    }

    def summary(post) {
        def linkText = "Permalink &rarr;"
        article(
                header(
                        p(tag("time").withText(post.date.toString())),
                        h2(post.title)
                ),
                rawHtml(post.summary),
                a(rawHtml(linkText)).withRel("full-article").withHref(link.createLinkToPage(post, true).toString())

        )
    }

    def indexPage(page) {
        html(
                head(
                        rawHtml(headerText),
                        title(page.title),
                        link().withRel("canonical").withHref("https://jameswilliams.be/blog")
                ),
                body(
                        rawHtml(navBarText),
                        main(
                                div(attrs(".container"),
                                        div(attrs(".row"),
                                                rawHtml("<blog:index/>")
                                        ),
                                    rawHtml(footer())
                                )
                        )
                )
        ).render()
    }

    def indexPosts(posts) {
        each(posts, { post ->
            summary(post)
        }).render()
    }

    def post(page) {
        html(
                head(
                        rawHtml(headerText),
                        title(page.title),
                        link(),
                        meta()
                ),
                body(
                        rawHtml(navBarText),
                        main(
                                section(attrs("#content"),
                                        rawHtml(full(page)),
                                        p(),
                                        p(),
                                        rawHtml(footer())
                                )
                        )
                )
        ).render()
    }

    def footer() {
        footer(attrs("#footer"),
               rawHtml("Copyright &copy; 2006 - ${config?.site?.year ?: LocalDate.now().getYear()} - ${config.site.author}"),
               br(),
               rawHtml(footerText),
               rawHtml("<ga:tracker/>")
        ).render()
    }

    def recent(posts) {
        section(attrs("#recent"),
                h3("Recent Posts"),
                ul(attrs(".recent"),
                        each(posts, { post ->
                            list(post)
                        })
                )
        )
    }
}
