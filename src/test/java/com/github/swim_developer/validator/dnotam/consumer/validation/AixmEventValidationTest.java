package com.github.swim_developer.validator.dnotam.consumer.validation;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AIXM Event XML Validation")
class AixmEventValidationTest {

    private static final Path AIXM_EVENTS_PATH = Paths.get("src/main/resources/aixm-events");
    private static final String SCHEMA_PATH = "src/test/resources/schemas/aixm-wrapper.xsd";
    private static final String AIXM_MESSAGE_NAMESPACE = "http://www.aixm.aero/schema/5.1.1/message";

    private static DocumentBuilderFactory documentBuilderFactory;
    private static Schema aixmSchema;
    private static List<Path> xmlFiles;

    @BeforeAll
    static void setup() throws Exception {
        documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);

        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        aixmSchema = schemaFactory.newSchema(Paths.get(SCHEMA_PATH).toFile());

        xmlFiles = Files.list(AIXM_EVENTS_PATH)
                .filter(path -> path.toString().endsWith(".xml"))
                .sorted()
                .toList();
    }

    @Test
    @DisplayName("All XML files should contain exactly one message element")
    void allFilesShouldContainExactlyOneMessageElement() throws Exception {
        List<String> failures = new ArrayList<>();

        for (Path xmlFile : xmlFiles) {
            String content = Files.readString(xmlFile);
            Document document = parseXml(content);
            NodeList messageElements = document.getElementsByTagName("message");

            if (messageElements.getLength() != 1) {
                failures.add("%s: expected 1, found %d".formatted(xmlFile.getFileName(), messageElements.getLength()));
            }
        }

        assertThat(failures)
                .as("All files should contain exactly 1 <message> element")
                .isEmpty();
    }

    @Test
    @DisplayName("All XML files should contain exactly one AIXMBasicMessage in CDATA")
    void allFilesShouldContainExactlyOneAixmBasicMessage() throws Exception {
        List<String> failures = new ArrayList<>();

        for (Path xmlFile : xmlFiles) {
            String content = Files.readString(xmlFile);
            String cdataContent = extractCdataContent(content);

            if (cdataContent == null || cdataContent.isEmpty()) {
                failures.add("%s: no CDATA content".formatted(xmlFile.getFileName()));
                continue;
            }

            Document aixmDocument = parseXml(cdataContent);
            NodeList aixmMessages = aixmDocument.getElementsByTagNameNS(AIXM_MESSAGE_NAMESPACE, "AIXMBasicMessage");

            if (aixmMessages.getLength() != 1) {
                failures.add("%s: expected 1, found %d".formatted(xmlFile.getFileName(), aixmMessages.getLength()));
            }
        }

        assertThat(failures)
                .as("All files should contain exactly 1 AIXMBasicMessage")
                .isEmpty();
    }

    @Test
    @DisplayName("All AIXMBasicMessage content should be valid against AIXM schema")
    void allFilesShouldBeValidAgainstAixmSchema() throws Exception {
        List<String> failures = new ArrayList<>();

        for (Path xmlFile : xmlFiles) {
            String content = Files.readString(xmlFile);
            String cdataContent = extractCdataContent(content);

            if (cdataContent == null || cdataContent.isEmpty()) {
                failures.add("%s: no CDATA content".formatted(xmlFile.getFileName()));
                continue;
            }

            Validator validator = aixmSchema.newValidator();
            try {
                validator.validate(new StreamSource(new StringReader(cdataContent)));
            } catch (SAXException e) {
                failures.add("%s: %s".formatted(xmlFile.getFileName(), e.getMessage()));
            }
        }

        assertThat(failures)
                .as("All AIXMBasicMessage content should be schema-valid")
                .isEmpty();
    }

    private Document parseXml(String xmlContent) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
        return builder.parse(new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8)));
    }

    private String extractCdataContent(String xmlContent) {
        int cdataStart = xmlContent.indexOf("<![CDATA[");
        int cdataEnd = xmlContent.indexOf("]]>");

        if (cdataStart == -1 || cdataEnd == -1 || cdataEnd <= cdataStart) {
            return null;
        }

        return xmlContent.substring(cdataStart + 9, cdataEnd).trim();
    }
}
