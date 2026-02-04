package it.davide.xml;

import org.w3c.dom.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import IFMLElements.Binding;
import IFMLElements.NavigationFlow;

import javax.xml.parsers.*;
import java.io.File;
import org.apache.commons.io.FilenameUtils;
import java.util.*;

public class IFMLPatternExtractor {

    /* private static int searchPatternCounter = 0;
    private static int masterDetailCounter = 0; */

    private static List<String> navFlowParentElements = Arrays.asList("Form", "List", "Details");

    public record PatternDef(
            String sourceType,
            String targetType) {
    }

    private static void identifyPattern(
            JsonPatternStructure.JsonReport report,
            JsonPatternStructure.PagePatterns page,
            List<NavigationFlow> flows) throws Exception {
        try {
            PatternEngine engine = new PatternEngine();
            // for each flow found in page, detect if there is a pattern
            engine.detectPatterns(flows, page);

            if (page.patterns.size() > 0) {
                report.pages.add(page);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static String findSource(Element flow) {
        Node current = flow.getParentNode();

        while (current != null && current.getNodeType() == Node.ELEMENT_NODE) {
            Element el = (Element) current;

            if (navFlowParentElements.contains(el.getLocalName())) {
                return el.getAttribute("id");
            }

            current = current.getParentNode();
        }

        return null;
    }

    public static Element findElementById(Document doc, String id) {
        NodeList all = doc.getElementsByTagName("*");

        for (int i = 0; i < all.getLength(); i++) {
            Node node = all.item(i);

            if (node.getNodeType() != Node.ELEMENT_NODE)
                continue;

            Element el = (Element) node;

            if (id.equals(el.getAttribute("id"))) {
                return el;
            }
        }
        return null;
    }

    public void patternFinder(JsonPatternStructure.JsonReport report, String filePath, String directory)
            throws Exception {
        // works on the single page
        try {

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();

            Document doc = builder.parse(new File(filePath));

            NodeList documentFlows = doc.getElementsByTagNameNS("*", "NavigationFlow");

            File file = new File(filePath);
            String pageId = FilenameUtils.removeExtension(file.getName()) + ".wr";

            JsonPatternStructure.PagePatterns page = new JsonPatternStructure.PagePatterns();
            page.pageId = pageId;

            List<NavigationFlow> navFlows = new ArrayList<>();

            // check every flow in page
            for (int i = 0; i < documentFlows.getLength(); i++) {

                Element flow = (Element) documentFlows.item(i);

                // get starting and target component ID
                String toID = flow.getAttribute("to");
                Element toElement = findElementById(doc, toID);
                String fromID = findSource(flow);
                if (fromID == null) {
                    continue;
                }
                Element fromElement = findElementById(doc, fromID);

                if (fromID == null || toID == null || fromElement == null || toElement == null) {
                    continue; // skip if source or target not found
                }

                NodeList params = flow.getElementsByTagNameNS("*", "ParameterBinding");

                List<Binding> bindings = new ArrayList<>();

                if (flow.getAttribute("automaticCoupling").equals("true")) {
                    bindings.add(new Binding(null, null, true));
                } else {
                    // add bindings to a list
                    for (int j = 0; j < params.getLength(); j++) {
                        String bindingSource;
                        String bindingTarget;

                        if (params.item(j).getAttributes().getNamedItem("source") != null) {
                            bindingSource = params.item(j).getAttributes().getNamedItem("source").getNodeValue();
                        } else if (params.item(j).getAttributes().getNamedItem("sourceValue") != null) {
                            bindingSource = params.item(j).getAttributes().getNamedItem("sourceValue").getNodeValue();
                        } else {
                            bindingSource = params.item(j).getAttributes().getNamedItem("sourceParameter").getNodeValue();
                        }

                        if (params.item(j).getAttributes().getNamedItem("target") != null) {
                            bindingTarget = params.item(j).getAttributes().getNamedItem("target").getNodeValue();
                        } else if (params.item(j).getAttributes().getNamedItem("targetValue") != null) {
                            bindingTarget = params.item(j).getAttributes().getNamedItem("targetValue").getNodeValue();
                        } else {
                            bindingTarget = params.item(j).getAttributes().getNamedItem("targetParameter").getNodeValue();
                        }

                        bindings.add(new Binding(bindingSource, bindingTarget, false));

                    }
                }

                NavigationFlow navFlow = new NavigationFlow(
                        fromID,
                        fromElement.getLocalName(),
                        toID, toElement.getLocalName(),
                        bindings,
                        flow.getAttribute("automaticCoupling").equals("true"));

                navFlows.add(navFlow);
            }

            // create an element in the xml to identify the pattern
            identifyPattern(report, page, navFlows);

            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);

            // path di destinazione
            File outputDir = new File("output/" + directory);
            if (!outputDir.exists()) {
                outputDir.mkdirs(); // create directory folder if not exists
            }

            // path completo del file
            File outputFile = new File(outputDir, "pattern-report.json");

            mapper.writeValue(
                    outputFile,
                    report);

            /* System.out.println("Patterns found summary " + pageId + ":");
            System.out.println("SearchPattern: " + searchPatternCounter);
            System.out.println("MasterDetail: " + masterDetailCounter); */
        } catch (Exception e) {
            System.err.println(e.toString());
            e.printStackTrace();
        }
    }
}