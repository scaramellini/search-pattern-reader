package it.davide.xml;

import java.io.File;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.*;

import IFMLElements.NavigationFlow;
import globalGraph.Edge;
import globalGraph.FlowType;
import globalGraph.IFMLGraph;
import globalGraph.GraphNode;
import globalGraph.NodeType;

public class NewIFMLPatternExtractor {

    public class ComponentInfo {
        private String id;
        private String type;

        public ComponentInfo(String id, String type) {
            this.id = id;
            this.type = type;
        }

        public String getId() {
            return id;
        }

        public String getType() {
            return type;
        }
    }

    private static List<String> navFlowParentElements = Arrays.asList("Form", "List", "Details", "Action",
            "ViewComponent");

    private void collectDirectComponents(Element viewComponents, List<ComponentInfo> components) {

        NodeList children = viewComponents.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);

            if (child.getNodeType() == Node.ELEMENT_NODE) {

                Element element = (Element) child;

                // Qui prendiamo solo i componenti diretti
                // NON scendiamo nei figli

                String id = element.getAttribute("id");

                if (id != null && !id.isEmpty()) {
                    components.add(new ComponentInfo(id, element.getNodeName()));
                }
            }
        }
    }

    public List<ComponentInfo> getViewComponents(Document document) {

        List<ComponentInfo> components = new ArrayList<>();

        Element root = document.getDocumentElement(); // <Page>

        // 1️⃣ ViewComponents della Page principale
        NodeList pageChildren = root.getChildNodes();

        for (int i = 0; i < pageChildren.getLength(); i++) {
            Node node = pageChildren.item(i);

            if (node.getNodeType() == Node.ELEMENT_NODE &&
                    node.getNodeName().equals("ViewComponents")) {

                collectDirectComponents((Element) node, components);
            }
        }

        // 2️⃣ DialogPage → ViewComponents
        NodeList dialogPages = root.getElementsByTagName("DialogPage");

        for (int i = 0; i < dialogPages.getLength(); i++) {
            Element dialogPage = (Element) dialogPages.item(i);

            NodeList dialogChildren = dialogPage.getChildNodes();

            for (int j = 0; j < dialogChildren.getLength(); j++) {
                Node node = dialogChildren.item(j);

                if (node.getNodeType() == Node.ELEMENT_NODE &&
                        node.getNodeName().equals("ViewComponents")) {

                    collectDirectComponents((Element) node, components);
                }
            }
        }

        return components;
    }

    private void extractNodes(List<String> pagePaths, IFMLGraph graph) throws Exception {

        for (String pagePath : pagePaths) {

            String pageId = pagePath.substring(pagePath.lastIndexOf("/") + 1);

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();

            Document doc = builder.parse(new File(pagePath));

            getViewComponents(doc).forEach(component -> {
                GraphNode node = new GraphNode(component.getId(), resolveNodeType(component.getType()), pageId);

                graph.addNode(node);
            });
        }
    }

    private NodeType resolveNodeType(String component) {

        if (component.equals("Form"))
            return NodeType.FORM;
        if (component.equals("Details"))
            return NodeType.DETAILS;
        if (component.equals("List"))
            return NodeType.LIST;
        if (component.equals("ViewContainer"))
            return NodeType.VIEW_CONTAINER;

        return NodeType.UNKNOWN;
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

    private void extractEdges(List<String> pagePaths, IFMLGraph graph) throws Exception {

        for (String pagePath : pagePaths) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();

            try {
                Document doc = builder.parse(new File(pagePath));

                NodeList navNodeList = doc.getElementsByTagNameNS("*", "NavigationFlow");
                NodeList dataNodeList = doc.getElementsByTagNameNS("*", "DataFlow");

                List<Element> documentFlows = new ArrayList<>();
                for (int i = 0; i < navNodeList.getLength(); i++) {
                    documentFlows.add((Element) navNodeList.item(i));
                }
                for (int i = 0; i < dataNodeList.getLength(); i++) {
                    documentFlows.add((Element) dataNodeList.item(i));
                }
                
                for (int i = 0; i < documentFlows.size(); i++) {
                    Element flowElement = documentFlows.get(i);

                    String sourceId = findSource(flowElement);
                    String targetId = flowElement.getAttribute("to");

                    Edge edge = new Edge(
                            sourceId,
                            targetId,
                            FlowType.NAVIGATION);

                    graph.addEdge(edge);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public IFMLGraph buildGraph(List<String> pagePaths) throws Exception {
        IFMLGraph graph = new IFMLGraph();

        extractNodes(pagePaths, graph);
        extractEdges(pagePaths, graph);
        validateGraph(graph);
        return graph;
    }

    private void validateGraph(IFMLGraph graph) {

        for (Edge edge : graph.getAllEdges()) {
            if (graph.getNode(edge.getSourceId()) == null ||
                    graph.getNode(edge.getTargetId()) == null) {
                throw new IllegalStateException("Edge references missing node");
            }
        }
    }

}
