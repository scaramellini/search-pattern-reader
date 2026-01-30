package IFMLElements;

import java.util.List;

public class NavigationFlow {
    private String fromId;
    private String fromElement;
    private String toId;
    private String toElement;
    private List<Binding> bindings;

    public NavigationFlow(String fromId, String fromElement, String toId, String toElement,
            List<Binding> bindings, boolean automaticCoupling) {
        this.fromId = fromId;
        this.fromElement = fromElement;
        this.toId = toId;
        this.toElement = toElement;
        this.bindings = bindings;
    }

    public String getFromId() {
        return fromId;
    }

    public String getFromElement() {
        return fromElement;
    }

    public String getToId() {
        return toId;
    }

    public String getToElement() {
        return toElement;
    }

    public List<Binding> getBindings() {
        return bindings;
    }
}
