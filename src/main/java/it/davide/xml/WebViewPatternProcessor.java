package it.davide.xml;

import globalGraph.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * WebViewPatternProcessor processes a single WebView at a time.
 * It is responsible for:
 * 1. Loading all pages of the WebView
 * 2. Loading all Action definitions for that WebView
 * 3. Building a unified graph that contains both page components and actions
 * 4. Providing the unified graph for pattern detection
 * 
 * Since each WebView is isolated, no links can exist between different WebViews.
 */
public class WebViewPatternProcessor {
    private final String projectPath;
    private final ActionRegistry actionRegistry;
    
    private IFMLGraph unifiedGraph;

    public WebViewPatternProcessor(String projectPath, ActionRegistry actionRegistry, IFMLGraph unifiedGraph) {
        this.projectPath = projectPath;
        this.actionRegistry = actionRegistry;
        this.unifiedGraph = unifiedGraph;
    }

    /**
     * Process the WebView by building the unified graph
     * 
     * @return IFMLGraph - unified graph containing both page components and actions
     * @throws Exception If XML parsing or file operations fail
     */
    public IFMLGraph processWebView() throws Exception {
        // Add action nodes to the graph
        addActionNodesToGraph();

        // Connect page components to actions via edges
        connectPagesToActions();

        System.out.println("Unified graph for " + projectPath + " has " +
                unifiedGraph.getAllNodes().size() + " nodes, " + 
                unifiedGraph.getAllEdges().size() + " edges");

        return unifiedGraph;
    }

    /**
     * Get all page files for a specific webview
     * Pages are stored in paths like: Model/WebModel/wv1/page13w.wr
     * 
     * @param folderPath The project folder path
     * @param webviewId  The webview ID (e.g., "wv1")
     * @return List of absolute file paths to page files for the specified webview
     * @throws Exception If directory traversal fails
     */
    public List<String> getPageFilesForWebview(String folderPath, String webviewId) throws Exception {
        List<String> filesInFolder = Files.walk(Paths.get(folderPath))
                .filter(Files::isRegularFile)
                .filter(file -> {
                    String fileName = file.getFileName().toString();
                    return (fileName.startsWith("page") || fileName.startsWith("apg")) && fileName.endsWith(".wr");
                })
                .filter(file -> {
                    // Check if the file is in a path containing the webview ID
                    String path = file.toString();
                    return path.contains(File.separator + webviewId + File.separator);
                })
                .map(Path::toString)
                .collect(Collectors.toList());

        return filesInFolder;
    }

    /**
     * Add action nodes to the unified graph
     */
    private void addActionNodesToGraph() {
        List<ActionDefinition> actions = actionRegistry.getAllActions();
        
        for (ActionDefinition action : actions) {
            for(String actionId : action.getIds()) {
                GraphNode actionNode = new GraphNode(actionId, action);
                unifiedGraph.addNode(actionNode);
            }
        }
        
        System.out.println("Added " + actions.size() + " action nodes for project: " + projectPath);
    }

    /**
     * Connect page components to actions by updating edges
     * When a NavigationFlow points to an action ID, modify the edge to reference the action
     */
    private void connectPagesToActions() {
        List<Edge> edges = new ArrayList<>(unifiedGraph.getAllEdges());

        for (Edge edge : edges) {
            String targetId = edge.getTargetId();

            if (targetId != null && actionRegistry.hasAction(targetId)) {

                Edge actionEdge = new Edge(edge.getSourceId(), targetId, edge.getType(), false);

                for (EdgeBinding binding : edge.getBindings()) {
                    actionEdge.addBinding(binding);
                }

                unifiedGraph.replaceEdge(edge, actionEdge);
                System.out.println("Converted edge to action call: " + edge.getSourceId() + " -> " + targetId);
            }
        }
    }

    /**
     * Get the unified graph for pattern detection
     * 
     * @return IFMLGraph - the unified graph
     */
    public IFMLGraph getUnifiedGraph() {
        return unifiedGraph;
    }

    /**
     * Get statistics about the processed webview
     * 
     * @return String - statistics summary
     */
    public String getStatistics() {
        
        int nodeCount = (int) unifiedGraph.getAllNodes().size();
        int edgeCount = unifiedGraph.getAllEdges().size();
        
        long pageComponentCount = unifiedGraph.getAllNodes().stream()
                .filter(node -> !node.isAction())
                .count();
        
        long actionNodeCount = unifiedGraph.getAllNodes().stream()
                .filter(GraphNode::isAction)
                .count();
        
        return String.format(
                "Nodes: %d (Components: %d, Actions: %d), Edges: %d",
                nodeCount, pageComponentCount, actionNodeCount, edgeCount
        );
    }
}
