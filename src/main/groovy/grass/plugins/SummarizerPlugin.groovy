package grass.plugins
import grass.*

class SummarizerPlugin implements GrassMixin {
	  private static final String DIVIDER = '<!-- more -->'

	  def afterPage(page) {
		    if (page?.summary?.contains(DIVIDER)) {
			      page.summary = page.summary.substring(0, page.summary.indexOf(DIVIDER))
			      page.metaClass.isSummarized = { -> true }
		    } else {
			      page.metaClass.isSummarized = { -> false }
		    }
	  }
}
