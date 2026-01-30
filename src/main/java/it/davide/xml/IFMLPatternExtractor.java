package it.davide.xml;

import org.w3c.dom.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import IFMLElements.Binding;
import IFMLElements.NavigationFlow;
import it.davide.xml.JsonPatternStructure.FilterBinding;
import patternsClasses.GenericPattern;

import javax.xml.parsers.*;
/*import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;*/
import java.io.File;
import org.apache.commons.io.FilenameUtils;
import java.util.*;

public class IFMLPatternExtractor {

    private static int searchPatternCounter = 0;
    private static int masterDetailCounter = 0;

    private static List<String> navFlowParentElements = Arrays.asList("Form", "List", "Details");

    public record PatternDef(
            String sourceType,
            String targetType) {
    }

    /*
     * private static final Map<String, PatternDef> PATTERNS = Map.of(
     * "SearchPattern", new PatternDef("Form", "List"),
     * "MasterDetail", new PatternDef("List", "Details"),
     * "NavigationPattern", new PatternDef("ViewComponent", "Page"));
     */

    private static void identifyPattern(
            JsonPatternStructure.JsonReport report,
            JsonPatternStructure.PagePatterns page,
            String filePath,
            List<NavigationFlow> flows) throws Exception {
        try {
            /*
             * for (Map.Entry<String, PatternDef> entry : PATTERNS.entrySet()) {
             * PatternDef def = entry.getValue();
             * 
             * if (def.sourceType().equals(flow.getFromElement())
             * && def.targetType().equals(flow.getToElement())) {
             * 
             * String patternName = entry.getKey();
             * createPattern(report, page, filePath, patternName, flow);
             * }
             * }
             */

            PatternEngine engine = new PatternEngine();
            engine.detect(flows).forEach(pattern -> {
                try {
                    createPattern(report, page, filePath, pattern.getName(), pattern);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /*
     * private static void createPattern(String filePath, String patternName, String
     * fromID, String toID,
     * List<Element> bindings, Document doc) throws Exception {
     * try {
     * Element page = doc.getDocumentElement();
     * String NS = "http://example.org/ifml/patterns";
     * 
     * Element pattern = doc.createElementNS(NS, "pattern:" + patternName);
     * pattern.setAttribute("id",
     * fromID.substring(fromID.lastIndexOf("#") + 1) + '_' +
     * toID.substring(toID.lastIndexOf("#") + 1));
     * 
     * Element fromRef = doc.createElementNS(NS, "pattern:fromRef");
     * fromRef.setAttribute("ref", fromID);
     * pattern.appendChild(fromRef);
     * 
     * Element toRef = doc.createElementNS(NS, "pattern:toRef");
     * toRef.setAttribute("ref", toID);
     * pattern.appendChild(toRef);
     * 
     * Element filters = doc.createElementNS(NS, "pattern:FilterBindings");
     * 
     * for (Element pb : bindings) {
     * Element filter = doc.createElementNS(NS, "pattern:Filter");
     * if (pb.hasAttribute("automaticCoupling")) {
     * filter.setAttribute("automaticCoupling",
     * pb.getAttribute("automaticCoupling"));
     * } else {
     * filter.setAttribute("source", pb.getAttribute("source"));
     * filter.setAttribute("target", pb.getAttribute("target"));
     * }
     * filters.appendChild(filter);
     * }
     * 
     * pattern.appendChild(filters);
     * page.appendChild(pattern);
     * System.out.println(patternName + "found on " + filePath);
     * 
     * if (patternName.equals("MasterDetail"))
     * masterDetailCounter++;
     * else if (patternName.equals("SearchPattern"))
     * searchPatternCounter++;
     * 
     * } catch (Exception e) {
     * System.err.println("Error creating pattern: " + e.toString());
     * e.printStackTrace();
     * }
     * 
     * }
     */

    private static void createPattern(
            JsonPatternStructure.JsonReport report,
            JsonPatternStructure.PagePatterns page,
            String filePath,
            String patternName,
            GenericPattern patternInstance) throws Exception {

        JsonPatternStructure.FlowPattern pattern = new JsonPatternStructure.FlowPattern();
        pattern.patternType = patternName;

        NavigationFlow flow = patternInstance.getFlows().get(0);

        JsonPatternStructure.Endpoint from = new JsonPatternStructure.Endpoint();
        from.id = flow.getFromId();
        from.type = flow.getFromElement();

        JsonPatternStructure.Endpoint to = new JsonPatternStructure.Endpoint();
        to.id = flow.getToId();
        to.type = flow.getToElement();
        pattern.from = from;
        pattern.to = to;

        for (Binding binding : flow.getBindings()) {
            FilterBinding b = new FilterBinding();

            if (binding.isAutomaticCoupling()) {
                b.automaticCoupling = true;
            } else {
                b.source = binding.getFromAttribute();
                b.target = binding.getToAttribute();
            }

            pattern.bindings.add(b);
        }

        page.patterns.add(pattern);

        report.pages.add(page);

        if (patternName.equals("MasterDetail"))
            masterDetailCounter++;
        else if (patternName.equals("SearchPattern"))
            searchPatternCounter++;
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
                    /*
                     * Element autoBinding = doc.createElement("ParameterBinding");
                     * autoBinding.setAttribute("automaticCoupling", "true");
                     * autoBinding.setAttribute("source", fromID);
                     * autoBinding.setAttribute("target", toID);
                     */
                    bindings.add(new Binding(null, null, true));
                } else {
                    // add bindings to a list
                    for (int j = 0; j < params.getLength(); j++) {
                        String bindingSource = params.item(j).getAttributes().getNamedItem("source").getNodeValue();
                        String bindingTarget = params.item(j).getAttributes().getNamedItem("target").getNodeValue();
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
            identifyPattern(report, page, filePath, navFlows);

            /*
             * Transformer transformer = TransformerFactory.newInstance().newTransformer();
             * transformer.setOutputProperty(OutputKeys.INDENT, "yes");
             * transformer.setOutputProperty(
             * "{http://xml.apache.org/xslt}indent-amount", "2");
             * 
             * File file = new File(filePath);
             * String filename = FilenameUtils.removeExtension(file.getName());
             * 
             * // path di destinazione
             * File outputDir = new File("output/" + directory);
             * if (!outputDir.exists()) {
             * outputDir.mkdirs(); // create directory folder if not exists
             * }
             * 
             * // path completo del file
             * File outputFile = new File(outputDir, filename + ".xml");
             * 
             * // create output file
             * transformer.transform(
             * new DOMSource(doc),
             * new StreamResult(outputFile));
             */

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

            System.out.println("Patterns found summary: ");
            System.out.println("SearchPattern: " + searchPatternCounter);
            System.out.println("MasterDetail: " + masterDetailCounter);
        } catch (Exception e) {
            System.err.println(e.toString());
            e.printStackTrace();
        }
    }
}