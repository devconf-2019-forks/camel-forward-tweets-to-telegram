package sample.camel;

import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.telegram.model.IncomingMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Echoes the message and adds the {@code chatId} to {@link #chatIds}.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
@Component
public class TelegramBot implements Processor {
    private static final Logger log = LoggerFactory.getLogger(TelegramBot.class);

    private final Set<String> chatIds;

    public TelegramBot() {
        final Set<String> chatIds = new LinkedHashSet<>();
        chatIds.add("434822960"); // ppalaga's chat with camelDemoBot
        this.chatIds = chatIds;
    }

    /**
     * @return a copy of the internal list of {@code chatIds} seen throughout the lifetime of this {@link TelegramBot}
     * instance.
     */
    public Set<String> getChatIds() {
        synchronized (chatIds) {
            return new LinkedHashSet<>(chatIds);
        }
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        final IncomingMessage m = exchange.getIn().getBody(IncomingMessage.class);
        final String id = m.getChat().getId();
        synchronized (chatIds) {
            if (chatIds.add(id)) {
                log.warn("Added chatId {}", id);
            }
        }
    }

}