package prefs;

public class PanelInfo {

    private final String name;
    private final String filter;

    public PanelInfo(String name, String filter) {
        this.name = name;
        this.filter = filter;
    }
    
    public PanelInfo() {
        this.name = "Panel";
        this.filter = "";
    }

    public String getPanelName() {
        return this.name;
    }

    public String getPanelFilter() {
        return this.filter;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PanelInfo panelInfo = (PanelInfo) o;

        if (name != null ? !name.equals(panelInfo.name) : panelInfo.name != null) return false;
        return filter != null ? filter.equals(panelInfo.filter) : panelInfo.filter == null;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (filter != null ? filter.hashCode() : 0);
        return result;
    }
}
