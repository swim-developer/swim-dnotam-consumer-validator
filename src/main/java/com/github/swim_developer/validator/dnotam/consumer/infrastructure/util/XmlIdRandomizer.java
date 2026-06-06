package com.github.swim_developer.validator.dnotam.consumer.infrastructure.util;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ApplicationScoped
public class XmlIdRandomizer {

    private static final Pattern GML_ID = Pattern.compile("(gml:id=\")([^\"]+)(\")");

    public String randomizeIds(String xmlContent) {
        Matcher m = GML_ID.matcher(xmlContent);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String newId = "uuid." + UUID.randomUUID();
            m.appendReplacement(sb, Matcher.quoteReplacement(m.group(1) + newId + m.group(3)));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
