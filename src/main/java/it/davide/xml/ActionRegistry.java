package it.davide.xml;

import globalGraph.*;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * ActionRegistry is responsible for scanning the entire workspace for
 * ActionDefinition files
 * and providing a lookup service for actions.
 * All Properties.wr files are scanned from the /Model directories in all
 * projects.
 */
public class ActionRegistry {

    private final List<ActionDefinition> actions = new ArrayList<>();

    /**
     * Load all actions from the workspace
     * Scans all /Model directories recursively for Properties.wr files
     * 
     * @param rootPath The root path of the workspace (typically contains Projects/
     *                 folder)
     * @throws Exception If XML parsing fails
     */
    public void loadActionsFromWorkspace(String rootPath) throws Exception {
        // Find all Properties.wr files in /Model directories
        List<String> propertiesFilePaths = findPropertiesFiles(rootPath);
        List<String> allPropertiesFilePaths = new ArrayList<>();
        allPropertiesFilePaths.addAll(findWVPropertiesFiles(rootPath));
        allPropertiesFilePaths.addAll(findHMDPropertiesFiles(rootPath));

        for (String filePath : propertiesFilePaths) {
            try {
                if (filePath.contains("ada2ai")) {
                    System.out
                            .println("Debug: Found Properties.wr file containing 'ada2ai' in mapActionDefinitionToId: "
                                    + filePath);
                }
                ActionDefinition actionDef = parseActionDefinition(filePath);
                if (actionDef != null) {
                    mapActionDefinitionToId(actionDef, allPropertiesFilePaths);
                    actions.add(actionDef);
                    System.out.println("Loaded action: " + actionDef.getDefinition());
                }
            } catch (Exception e) {
                System.err.println("Failed to parse action from " + filePath + ": " + e.getMessage());
            }
        }

        System.out.println("Total actions loaded: " + actions.size());
    }

    /**
     * Find all Properties.wr files in /Model directories
     * 
     * @param rootPath The root path to search
     * @return List of absolute paths to Properties.wr files
     * @throws Exception If directory traversal fails
     */
    private List<String> findPropertiesFiles(String rootPath) throws Exception {
        try (Stream<Path> paths = Files.walk(Paths.get(rootPath))) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().equals("Properties.wr"))
                    .filter(file -> file.getParent().toString().contains(File.separator + "Model" + File.separator))
                    .map(Path::toString)
                    .collect(Collectors.toList());
        }
    }

    /**
     * Find all Properties.wr files in /wv directories
     * 
     * @param rootPath The root path to search
     * @return List of absolute paths to Properties.wr files
     * @throws Exception If directory traversal fails
     */
    private List<String> findWVPropertiesFiles(String rootPath) throws Exception {
        try (Stream<Path> paths = Files.walk(Paths.get(rootPath))) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().equals("Properties.wr"))
                    .filter(file -> file.getParent().toString().contains(File.separator + "wv"))
                    .map(Path::toString)
                    .collect(Collectors.toList());
        }
    }

    /**
     * Find all Properties.wr files in /hmd directories
     * 
     * @param rootPath The root path to search
     * @return List of absolute paths to Properties.wr files
     * @throws Exception If directory traversal fails
     */
    private List<String> findHMDPropertiesFiles(String rootPath) throws Exception {
        try (Stream<Path> paths = Files.walk(Paths.get(rootPath))) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().equals("Properties.wr"))
                    .filter(file -> file.getParent().toString().contains(File.separator + "hmd"))
                    .map(Path::toString)
                    .collect(Collectors.toList());
        }
    }

    /**
     * Parse a single ActionDefinition from a Properties.wr file
     * 
     * @param filePath Path to the Properties.wr file
     * @return ActionDefinition if file contains ActionDefinition, null otherwise
     * @throws Exception If XML parsing fails
     */
    private ActionDefinition parseActionDefinition(String filePath) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document actionDoc = builder.parse(new File(filePath));

        Element root = actionDoc.getDocumentElement();

        // Check if root is ActionDefinition
        if (!root.getNodeName().equals("ActionDefinition")) {
            return null;
        }

        String actionDefinition = root.getAttribute("id");
        String webviewId = ""; // Not used for categorization, kept for ActionDefinition only

        // Parse input parameters
        List<PortParameter> inputParameters = parseInputPortDefinition(root);

        // Parse success and error output ports
        Map<String, List<PortParameter>> successPorts = parseOutputPorts(root, "SuccessPortDefinition");
        Map<String, List<PortParameter>> errorPorts = parseOutputPorts(root, "ErrorPortDefinition");

        // Parse operation components
        List<OperationComponent> operationComponents = parseOperationComponents(root);

        return new ActionDefinition(
                new ArrayList<>(), // IDs will be set later based on webview context
                actionDefinition,
                webviewId,
                filePath,
                inputParameters,
                successPorts,
                errorPorts,
                operationComponents,
                new HashMap<>(), // Success events will be set later
                new HashMap<>() // Error events will be set later
        );
    }

    private void mapActionDefinitionToId(ActionDefinition actionDef, List<String> allPropertiesFilePaths)
            throws Exception {
        for (String propertiesFilePath : allPropertiesFilePaths) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document actionDoc = builder.parse(new File(propertiesFilePath));

            Element root = actionDoc.getDocumentElement();

            NodeList pageChildren = root.getChildNodes();

            for (int i = 0; i < pageChildren.getLength(); i++) {
                Node node = pageChildren.item(i);

                if (node.getNodeType() == Node.ELEMENT_NODE &&
                        node.getNodeName().equals("Action")) {

                    Element actionNode = (Element) node;

                    String actionId = actionNode.getAttribute("id");
                    String actionDefinition = (actionNode.getAttribute("definition") != null
                            && !actionNode.getAttribute("definition").isEmpty())
                                    ? actionNode.getAttribute("definition")
                                    : actionNode.getAttribute("actionServiceRes");

                    if (utilityTools.extractId(actionDefinition).equals(utilityTools.extractId(actionDef.getDefinition()))) {

                        NodeList eventsList = actionNode.getElementsByTagNameNS("*", "Events");

                        // there is only one Events tag per Action
                        Element events = eventsList.getLength() > 0 ? (Element) eventsList.item(0) : null;

                        for (int j = 0; j < events.getChildNodes().getLength(); j++) {
                            Node eventNode = events.getChildNodes().item(j);
                            if (eventNode.getNodeType() == Node.ELEMENT_NODE) {
                                Element eventElement = (Element) eventNode;
                                String eventId = eventElement.getAttribute("id");
                                String eventDefinition = eventElement.getAttribute("definition");

                                ActionEvent actionEvent = new ActionEvent(eventId, eventDefinition,
                                        eventElement.getNodeName());

                                if (eventElement.getNodeName().equals("SuccessEvent")) {
                                    actionDef.getSuccessEventsMap().put(eventId, actionEvent);
                                } else if (eventElement.getNodeName().equals("ErrorEvent")) {
                                    actionDef.getErrorEventsMap().put(eventId, actionEvent);
                                }

                                // Parse navigation flows from events
                                NodeList navFlows = eventElement.getElementsByTagNameNS("*", "NavigationFlow");
                                for (int k = 0; k < navFlows.getLength(); k++) {
                                    Element navFlow = (Element) navFlows.item(k);
                                    String targetId = navFlow.getAttribute("to");

                                    if (targetId != null && !targetId.isEmpty()) {
                                        Edge edge = new Edge(eventId, targetId, FlowType.NAVIGATION, false);

                                        // Parse parameter bindings
                                        NodeList bindings = navFlow.getElementsByTagNameNS("*", "ParameterBinding");
                                        for (int x = 0; x < bindings.getLength(); x++) {
                                            Element binding = (Element) bindings.item(x);

                                            boolean automatic = Boolean
                                                    .parseBoolean(binding.getAttribute("automaticCoupling"));
                                            String sourceAttr = resolveAttribute(binding,
                                                    Arrays.asList("source", "sourceValue"));
                                            String targetAttr = resolveAttribute(binding,
                                                    Arrays.asList("target", "targetValue"));

                                            edge.addBinding(new EdgeBinding(automatic, sourceAttr, targetAttr));
                                        }

                                        actionEvent.addNavigationFlow(edge);
                                    }
                                }
                            }
                        }

                        actionDef.addId(actionId);
                    }
                }
            }
        }

    }

    /**
     * Parse the InputPortDefinition element
     * 
     * @param rootElement The root ActionDefinition element
     * @return List of input port parameters
     */
    private List<PortParameter> parseInputPortDefinition(Element rootElement) {
        List<PortParameter> parameters = new ArrayList<>();
        NodeList inputPorts = rootElement.getElementsByTagNameNS("*", "InputPortDefinition");

        if (inputPorts.getLength() == 0) {
            return parameters;
        }

        Element inputPort = (Element) inputPorts.item(0);
        NodeList portParams = inputPort.getElementsByTagNameNS("*", "PortDefinitionParameter");

        for (int i = 0; i < portParams.getLength(); i++) {
            Element param = (Element) portParams.item(i);
            String id = param.getAttribute("id");
            String name = param.getAttribute("name");
            parameters.add(new PortParameter(id, name));
        }

        return parameters;
    }

    /**
     * Parse the Success/Error PortDefinition elements
     * 
     * @param rootElement The root ActionDefinition element
     * @param portType    "SuccessPortDefinition" or "ErrorPortDefinition"
     * @return Map of port ID to list of parameters
     */
    private Map<String, List<PortParameter>> parseOutputPorts(Element rootElement, String portType) {
        Map<String, List<PortParameter>> portMap = new HashMap<>();
        NodeList ports = rootElement.getElementsByTagNameNS("*", portType);

        for (int i = 0; i < ports.getLength(); i++) {
            Element port = (Element) ports.item(i);
            String portId = port.getAttribute("id");

            List<PortParameter> parameters = new ArrayList<>();
            NodeList portParams = port.getElementsByTagNameNS("*", "PortDefinitionParameter");

            for (int j = 0; j < portParams.getLength(); j++) {
                Element param = (Element) portParams.item(j);
                String id = param.getAttribute("id");
                String name = param.getAttribute("name");
                parameters.add(new PortParameter(id, name));
            }

            portMap.put(portId, parameters);
        }

        return portMap;
    }

    /**
     * Parse the Operations tag to extract all operation components
     * 
     * @param rootElement The root ActionDefinition element
     * @return List of OperationComponent
     */
    private List<OperationComponent> parseOperationComponents(Element rootElement) {
        List<OperationComponent> components = new ArrayList<>();
        NodeList operationsList = rootElement.getElementsByTagNameNS("*", "Operations");

        if (operationsList.getLength() == 0) {
            return components;
        }

        Element operations = (Element) operationsList.item(0);
        NodeList children = operations.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);

            if (child.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) child;

                String componentId = element.getAttribute("id");
                String componentType = element.getNodeName();
                String componentName = element.getAttribute("name");
                String actionDefRef = element.getAttribute("definition");

                if (componentId != null && !componentId.isEmpty()) {
                    OperationComponent component = new OperationComponent(
                            componentId,
                            componentType,
                            componentName,
                            actionDefRef);

                    // Parse flows (SuccessFlow, ErrorFlow, DataFlow)
                    parseComponentFlows(element, component);

                    components.add(component);
                }
            }
        }

        return components;
    }

    /**
     * Parse flows within an operation component
     * 
     * @param componentElement The operation component element
     * @param component        The OperationComponent to populate
     */
    private void parseComponentFlows(Element componentElement, OperationComponent component) {
        List<String> flowTypes = Arrays.asList("SuccessFlow", "ErrorFlow", "DataFlow");

        for (String flowType : flowTypes) {
            NodeList flows = componentElement.getElementsByTagNameNS("*", flowType);

            for (int i = 0; i < flows.getLength(); i++) {
                Element flow = (Element) flows.item(i);

                String flowId = flow.getAttribute("id");
                String targetId = flow.getAttribute("to");

                if (targetId != null && !targetId.isEmpty()) {
                    ComponentFlow componentFlow = new ComponentFlow(flowId, flowType, targetId);

                    // Parse parameter bindings
                    NodeList bindings = flow.getElementsByTagNameNS("*", "ParameterBinding");
                    for (int j = 0; j < bindings.getLength(); j++) {
                        Element binding = (Element) bindings.item(j);

                        boolean automatic = Boolean.parseBoolean(binding.getAttribute("automaticCoupling"));
                        String sourceAttr = resolveAttribute(binding,
                                Arrays.asList("source", "sourceValue", "sourceParameter"));
                        String targetAttr = resolveAttribute(binding,
                                Arrays.asList("target", "targetValue", "targetParameter"));

                        componentFlow.addBinding(new EdgeBinding(automatic, sourceAttr, targetAttr));
                    }

                    component.addFlow(componentFlow);
                }
            }
        }
    }

    /**
     * Utility method to resolve attribute values from a list of candidates
     * 
     * @param element    The XML element
     * @param candidates List of possible attribute names
     * @return The value of the first matching attribute, or null
     */
    private static String resolveAttribute(Element element, List<String> candidates) {
        NamedNodeMap attrs = element.getAttributes();

        for (String name : candidates) {
            Node attr = attrs.getNamedItem(name);
            if (attr != null) {
                return attr.getNodeValue();
            }
        }
        return null;
    }

    /**
     * Get an action by its defintion
     * 
     * @param actionId The action defintion (e.g., "tlads0#ad2w")
     * @return The ActionDefinition, or null if not found
     */
    public List<ActionDefinition> getActions(String actionId) {
        return actions;
    }

    public ActionDefinition getAction(String actionId) {
        for (ActionDefinition action : actions) {
            if (action.getIds().contains(actionId)) {
                return action;
            }
        }
        return null;
    }

    /**
     * Get all loaded actions
     * 
     * @return List of all ActionDefinitions
     */
    public List<ActionDefinition> getAllActions() {
        return actions;
    }

    /**
     * Check if an action exists
     * 
     * @param actionId The action ID
     * @return true if action exists, false otherwise
     */
    public boolean hasAction(String actionId) {
        return actions.stream().anyMatch(action -> action.getIds().contains(actionId));
    }

    /**
     * Get all loaded action IDs
     * 
     * @return Set of action IDs
     */
    public Set<String> getActionIds() {
        Set<String> actionIds = new HashSet<>();
        for (ActionDefinition action : actions) {
            actionIds.addAll(action.getIds());
        }
        return Collections.unmodifiableSet(actionIds);
    }

    /**
     * Get the total number of loaded actions
     * 
     * @return Number of actions
     */
    public int getActionCount() {
        return actions.size();
    }
}
