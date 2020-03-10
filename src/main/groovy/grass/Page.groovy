package grass
import java.time.LocalDate

import groovy.transform.ToString

//@ToString
class Page {
	  File path
	  String template
	  String name
	  String out
	  String title
    LocalDate date
    String dateString
	  String summary
	  String content
	  String description
}
