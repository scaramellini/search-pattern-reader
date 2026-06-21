package globalGraph;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an event within an Action.
 * Events can trigger NavigationFlows to navigate to other pages or components.
 * This can be: SuccessEvent or ErrorEvent
 */
public class ActionEvent {
    private final String id;
    private final String type;  // "SuccessEvent" or "ErrorEvent"
    private final List<Edge> navigationFlows;

    public ActionEvent(String id, String type) {
        this.id = id;
        this.type = type;
        this.navigationFlows = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public List<Edge> getNavigationFlows() {
        return navigationFlows;
    }

    public void addNavigationFlow(Edge flow) {
        navigationFlows.add(flow);
    }

    @Override
    public String toString() {
        return "ActionEvent{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", navigationFlowsCount=" + navigationFlows.size() +
                '}';
    }
}
