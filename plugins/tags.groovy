import java.net.URLEncoder

class TagsPlugin {
	def PAGE_TAG = /<tags:page\/>/
	def LIST_TAG = /<tags:list\/>/

	def config

	def init() {
		if (config?.plugin?.links?.dispatch) {
			config?.plugin?.links?.dispatch << 'tag'
		}
		config.tags = [:]
	}


	def setupBinding(binding) {
		binding.tag = { Object[] args ->
			(args as List).flatten().each { tag(page, it) }
		}
		binding.createLinkToTag = { String tag, boolean absolute ->
			binding.createLinkToUrl("tags/${normalize(tag)}.html", absolute)
		}
	}

	def beforePage(page) {
		page.metaClass.tags = []
	}

	def afterIndex(index, pages) {
		config.tags.each { k, v ->
			def posts = v.sort { a, b -> b.date <=> a.date }
			def content = applyTemplate('tags/index.html', '', newBinding(pages: posts))
			addPage(content: content, name: "Category: $k", title: "Category: $k", date: new Date(), out: "tags/${normalize(k)}.html")
		}
	}

	def beforeWrite(page) {
		if (page.content.contains(PAGE_TAG)) {
			if (page?.tags) {
				page.content = page.content.replace(PAGE_TAG, applyTemplate('tags/page.html', '', newBinding(tags: page.tags)).toString())
			} else {
				page.content = page.content.replace(PAGE_TAG, '')
			}
		}
		if (config.tags && page.content.contains(LIST_TAG)) {
			page.content = page.content.replace(LIST_TAG, applyTemplate('tags/list.html', '', newBinding(tags: config.tags)).toString())
		}
	}

	private normalize(tag) {
		URLEncoder.encode(tag.toLowerCase().replace(' ', '-'), "UTF-8")
	}

	private tag(page, name) {
		page.tags << name
		if (config.tags.containsKey(name)) {
			config.tags[name] << page
		} else {
			config.tags[name] = [page]
		}
	}
}