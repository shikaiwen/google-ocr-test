
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements


def Map doParse(){
    Document doc = Jsoup.connect("${siteUrl}").get();

    doc.select("#mp-itn b a");

    Map result = new LinkedHashMap();
    title = doc.selectFirst("${titleSelector}").text();
    String content = doc.selectFirst("${contentSelector}").outerHtml();

    result.put("title",title);
    result.put("content",content);
     return result;

}