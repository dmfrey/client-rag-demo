package com.example.clientragdemo.chat.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Externalizes the window size Spring AI's MessageWindowChatMemory otherwise hardcodes to 20
 * messages (~10 exchanges) with no property to change it - see the chatMemory bean in
 * ChatConfiguration. Defaults to 20 to match that built-in default when unconfigured, so setting
 * this up doesn't silently change behavior until someone actually overrides it.
 */
@ConfigurationProperties(prefix = "app.chat.memory")
record ChatMemoryProperties(@DefaultValue("20") int maxMessages) {}
