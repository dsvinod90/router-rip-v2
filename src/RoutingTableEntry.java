import java.io.Serializable;

public class RoutingTableEntry implements Serializable {
    public static final String ADDRESS_FAMILY_IP = "2"; // 2 for IP
    private String addressFamilyIdentifier;
    private String routeTag;
    private String address;
    private String subnetMask;
    private String nextHop;
    private Integer metric;

    public RoutingTableEntry(String addressFamilyIdentifier, String routeTag, String address, String subnetMask, String nextHop, Integer metric) {
        this.addressFamilyIdentifier = addressFamilyIdentifier;
        this.routeTag = routeTag;
        this.address = address;
        this.subnetMask = subnetMask;
        this.nextHop = nextHop;
        this.metric = metric;
    }

    public String getAddressFamilyIdentifier() {
        return addressFamilyIdentifier;
    }

    public void setAddressFamilyIdentifier(String addressFamilyIdentifier) {
        this.addressFamilyIdentifier = addressFamilyIdentifier;
    }

    public String getRouteTag() {
        return routeTag;
    }

    public void setRouteTag(String routeTag) {
        this.routeTag = routeTag;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getSubnetMask() {
        return subnetMask;
    }

    public void setSubnetMask(String subnetMask) {
        this.subnetMask = subnetMask;
    }

    public String getNextHop() {
        return nextHop;
    }

    public void setNextHop(String nextHop) {
        this.nextHop = nextHop;
    }

    public Integer getMetric() {
        return metric;
    }

    public void setMetric(Integer metric) {
        this.metric = metric;
    }

    @Override
    public String toString() {
        return "\nRoutingTableEntry{" +
                "addressFamilyIdentifier='" + addressFamilyIdentifier + '\'' +
                ", routeTag='" + routeTag + '\'' +
                ", address='" + address + '\'' +
                ", subnetMask='" + subnetMask + '\'' +
                ", nextHop='" + nextHop + '\'' +
                ", metric=" + metric +
                '}';
    }
}
