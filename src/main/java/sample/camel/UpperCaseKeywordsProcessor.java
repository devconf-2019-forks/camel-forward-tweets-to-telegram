package sample.camel;

import java.util.Locale;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Transfroms {@link #twitterKeywords} in the message to upper case.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
@Component
public class UpperCaseKeywordsProcessor implements Processor {

    @Value("${twitterKeywords}")
    private String twitterKeywords;

    private Pattern[] twitterKeywordPatterns;

    @PostConstruct
    public void initIt() throws Exception {
        final String[] twitterKeywordsArray = twitterKeywords.split(",");
        twitterKeywordPatterns = new Pattern[twitterKeywordsArray.length];
        for (int i = 0; i < twitterKeywordsArray.length; i++) {
            final String keyword = twitterKeywordsArray[i];
            twitterKeywordPatterns[i] = Pattern.compile(keyword, Pattern.CASE_INSENSITIVE);
        }
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        /* uppercase the keyword occurences in the message */
        String body = exchange.getIn().getBody(String.class);
        for (Pattern pattern : twitterKeywordPatterns) {
            body = pattern.matcher(body).replaceAll(pattern.pattern().toUpperCase(Locale.US));
        }
        exchange.getIn().setBody(body);
    }

}
