import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static RoutingTableEntry.ADDRESS_FAMILY_IP;

public class RIPPacket implements Serializable {

    public static final String COMMAND_REQUEST = "1";
    public static final String COMMAND_RESPONSE = "2";
    public static final String RIP_VERSION_2 = "2";
    public static final String ROUTER_TAG = "1";


    private String command;
    private String version;
    private String mustBeZero;
    private List<RoutingTableEntry> mList = new ArrayList<>();

    public RIPPacket() {
        addDummyData();
    }

    public RIPPacket(String command, String version, String mustBeZero, List<RoutingTableEntry> mList) {
        this.command = command;
        this.version = version;
        this.mustBeZero = mustBeZero;
        this.mList = mList;

        addDummyData();
    }

    private void addDummyData() {
        this.command = COMMAND_REQUEST;
        this.version = RIP_VERSION_2;
        this.mustBeZero = "0";
        /*
         * private String addressFamilyIdentifier;
         * private String routeTag;
         * private String address;
         * private String subnetMask;
         * private String nextHop;
         * private Integer metric;
         * */
        mList.add(new RoutingTableEntry(ADDRESS_FAMILY_IP, ROUTER_TAG, "10.0.1.0/24", "255.255.255.0", "1", 0));
        mList.add(new RoutingTableEntry(ADDRESS_FAMILY_IP, ROUTER_TAG, "10.0.2.0/24", "255.255.255.0", "1", 0));
        mList.add(new RoutingTableEntry(ADDRESS_FAMILY_IP, ROUTER_TAG, "10.0.6.0/24", "255.255.255.0", "1", 0));
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getMustBeZero() {
        return mustBeZero;
    }

    public void setMustBeZero(String mustBeZero) {
        this.mustBeZero = mustBeZero;
    }

    public List<RoutingTableEntry> getmList() {
        return mList;
    }

    public void setmList(List<RoutingTableEntry> mList) {
        this.mList = mList;
    }

    @Override
    public String toString() {
        return "RIPPacket{" +
                "command='" + command + '\'' +
                ", version='" + version + '\'' +
                ", mustBeZero='" + mustBeZero + '\'' +
                ", RTE=" + mList +
                '}';
    }
}
