package grass.plugins
import grass.GrassMixin
import com.petebevin.markdown.MarkdownProcessor

class MarkdownPlugin implements GrassMixin {

	  def renderPage(page) {
		    if (page?.path?.name?.endsWith('.md') || page?.path?.name?.endsWith('.markdown')) {
			      def processor = new MarkdownProcessor()
			      page.content = processor.markdown(page.content)
			      page.summary = page.content
		    }
	  }
} 
