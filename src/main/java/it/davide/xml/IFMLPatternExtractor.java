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

    /*
     * private static int searchPatternCounter = 0;
     * private static int masterDetailCounter = 0;
     */

    private static List<String> navFlowParentElements = Arrays.asList("Form", "List", "Details", "Action",
            "ViewComponent");

    public record PatternDef(
            String sourceType,
            String targetType) {
    }

    private static final List<String> SOURCE_ATTRS = List.of(
            "source",
            "sourceValue",
            "sourceParameter",
            "sourceImplicitParameter",
            "sourceParameterBinding",
            "blank");

    private static final List<String> TARGET_ATTRS = List.of(
            "target",
            "targetValue",
            "targetParameter",
            "targetImplicitParameter",
            "targetParameterBinding",
            "targetExpressionVariable",
            "blank");

    private static void identifyPattern(
            JsonPatternStructure.JsonReport report,
            JsonPatternStructure.PagePatterns page,
            List<NavigationFlow> flows,
            List<NavigationFlow> propertiesFlows) throws Exception {
        try {
            PatternEngine engine = new PatternEngine();
            // for each flow found in page, detect if there is a pattern
            engine.detectPatterns(flows, page, propertiesFlows);

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

    private static boolean isPassing(Element el) {
        String val = el.getAttribute("passing");
        return "true".equals(val);
    }

    private static String resolveAttribute(Element el, List<String> candidates) {
        NamedNodeMap attrs = el.getAttributes();

        for (String name : candidates) {
            Node attr = attrs.getNamedItem(name);
            if (attr != null) {
                return attr.getNodeValue();
            }
        }
        return null;
    }

    public List<NavigationFlow> extractFlows(String filePath) throws Exception {
        //System.out.println(filePath);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();

        Document doc = builder.parse(new File(filePath));

        NodeList navNodeList = doc.getElementsByTagNameNS("*", "NavigationFlow");
        NodeList dataNodeList = doc.getElementsByTagNameNS("*", "DataFlow");

        List<Element> documentFlows = new ArrayList<>();
        for (int i = 0; i < navNodeList.getLength(); i++) {
            documentFlows.add((Element) navNodeList.item(i));
        }
        for (int i = 0; i < dataNodeList.getLength(); i++) {
            documentFlows.add((Element) dataNodeList.item(i));
        }

        List<NavigationFlow> navFlows = new ArrayList<>();

        // check every flow in page
        for (int i = 0; i < documentFlows.size(); i++) {

            Element flow = documentFlows.get(i);

            // get starting and target component ID
            String toID = flow.getAttribute("to");
            String fromID = findSource(flow);

            if (fromID == null || toID == null) {
                continue; // skip if source or target not found
            }

            Element toElement = findElementById(doc, toID);
            String toElementName = "";
            if (toElement == null) {
                toElementName = "externalElement";
            } else {
                toElementName = toElement.getTagName();
            }

            Element fromElement = findElementById(doc, fromID);
            String fromElementName = "";
            if (fromElement == null) {
                fromElementName = "externalElement";
            } else {
                fromElementName = fromElement.getTagName();
            }

            if (fromID == null || toID == null || fromElementName == null || toElementName == null) {
                continue; // skip if source or target not found
            }

            NodeList params = flow.getElementsByTagNameNS("*", "ParameterBinding");
            List<Binding> bindings = new ArrayList<>();

            if ("true".equals(flow.getAttribute("automaticCoupling"))) {
                bindings.add(new Binding(null, null, true));
            } else {

                for (int j = 0; j < params.getLength(); j++) {
                    Element param = (Element) params.item(j);

                    if (isPassing(param)) {
                        continue;
                    }

                    String source = resolveAttribute(param, SOURCE_ATTRS);
                    String target = resolveAttribute(param, TARGET_ATTRS);

                    bindings.add(new Binding(source, target, false));
                }
            }

            NavigationFlow navFlow = new NavigationFlow(
                    fromID,
                    fromElementName,
                    toID,
                    toElementName,
                    bindings,
                    flow.getAttribute("automaticCoupling").equals("true"));

            navFlows.add(navFlow);
        }
        return navFlows;
    }

    public void patternFinder(JsonPatternStructure.JsonReport report, String filePath, String directory,
            List<NavigationFlow> propertiesFlows)
            throws Exception {
        // works on the single page
        try {

            List<NavigationFlow> navFlows = extractFlows(filePath);

            File file = new File(filePath);
            String pageId = FilenameUtils.removeExtension(file.getName()) + ".wr";

            JsonPatternStructure.PagePatterns page = new JsonPatternStructure.PagePatterns();
            page.pageId = pageId;

            // create an element in the xml to identify the pattern
            identifyPattern(report, page, navFlows, propertiesFlows);

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

            /*
             * System.out.println("Patterns found summary " + pageId + ":");
             * System.out.println("SearchPattern: " + searchPatternCounter);
             * System.out.println("MasterDetail: " + masterDetailCounter);
             */
        } catch (Exception e) {
            System.err.println(e.toString());
            e.printStackTrace();
        }
    }
}