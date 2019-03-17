/**
 * {@link RoutingTableEntry}
 *
 * @version:
 *      1.0.1
 *
 * @revision:
 *      1
 *
 * @author:
 *      ishanguliani aka ig5859
 */

/**
 * A {@link RoutingTableEntry} model class to represent an RTE
 * This is used to obtain an object oriented representation of the RIPPacket RTEs
 */

public class RoutingTableEntry {
    public static final int ADDRESS_FAMILY_IP = 2; // 2 for IP
    public static final String SUBNET_MASK = "255.255.255.0"; // 2 for IP

    public static final int ROUTE_TAG = 1; // 2 for IP
    private int addressFamilyIdentifier;
    private int routeTag;
    private String address;
    private String subnetMask;
    private String nextHop;
    private int metric;

    public RoutingTableEntry(int addressFamilyIdentifier, int routeTag, String address, String subnetMask, String nextHop, int metric) {
        this.addressFamilyIdentifier = addressFamilyIdentifier;
        this.routeTag = routeTag;
        this.address = address;
        this.subnetMask = subnetMask;
        this.nextHop = nextHop;
        this.metric = metric;
    }

    public int getAddressFamilyIdentifier() {
        return addressFamilyIdentifier;
    }

    public int getRouteTag() {
        return routeTag;
    }

    public String getAddress() {
        return address;
    }

    public String getSubnetMask() {
        return subnetMask;
    }

    public String getNextHop() {
        return nextHop;
    }

    public void setNextHop(String nextHop) {
        this.nextHop = nextHop;
    }

    public int getMetric() {
        return metric;
    }

    public void setMetric(int metric) {
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
