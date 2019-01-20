/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package sample.camel;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * A simple Camel route that triggers from a timer and calls a bean and prints to system out.
 * <p/>
 * Use <tt>@Component</tt> to make Camel auto detect this route when starting.
 */
@Component
public class MyCamelRouter extends RouteBuilder {

    @Autowired
    private TelegramBot bot;

    @Autowired
    private UpperCaseKeywordsProcessor upperCaseKeywordsProcessor;

    @Value("${telegramAuthorizationToken}")
    private String telegramAuthorizationToken;

    @Override
    public void configure() throws Exception {

        /* The main route to forward the messages from Twitter to Telegram */
        fromF("twitter-search:{{twitterKeywords}}?delay={{twitterDelayMs}}"
                + "&consumerKey={{twitterConsumerKey}}"
                + "&consumerSecret={{twitterConsumerSecret}}"
                + "&accessToken={{twitterAccessToken}}"
                + "&accessTokenSecret={{twitterAccessTokenSecret}}")
        .log("Raw tweet: ${body}")
        .process(upperCaseKeywordsProcessor)
        .process(exchange -> {
            /* Create list of telegram recipient URIs and store it to telegramRecipients header */
            final List<String> telegramRecipients = new ArrayList<>();
            for (String chatId : bot.getChatIds()) {
                telegramRecipients.add(String.format("telegram:bots/%s?chatId=%s", telegramAuthorizationToken, chatId));
            }
            exchange.getIn().setHeader("telegramRecipients", telegramRecipients);
        })
        .recipientList(header("telegramRecipients"));

        /* Helper route to store Telegram chatIds where the messages from twitter should be forwarded */
        fromF("telegram:bots/{{telegramAuthorizationToken}}")
        .process(bot)
        .toF("telegram:bots/{{telegramAuthorizationToken}}");

    }

}
