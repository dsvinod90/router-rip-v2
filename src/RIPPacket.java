/**
 * {@link RIPPacket}
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
 * The model class responsible for the following -
 * 1. Maintain a POJO model for RIP Packet
 * 2.
 */
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
public class RIPPacket {

    public static final int METRIC_UNREACHABLE = 16;
    public static final String COMMAND_REQUEST = "1";
    public static final String COMMAND_RESPONSE = "2";
    public static final String RIP_VERSION_2 = "2";
    public static final String MUST_BE_ZERO = "0";
    public static final int ROUTER_TAG = 1;

    private String command;
    private String version;
    private String mustBeZero;
    private String sender;
    private List<RoutingTableEntry> mList = new ArrayList<>();

    public RIPPacket() {
        this.command = COMMAND_REQUEST;
        this.version = RIP_VERSION_2;
        this.mustBeZero = MUST_BE_ZERO;
    }

    public RIPPacket(String command, String version, String mustBeZero, String sender,  List<RoutingTableEntry> mList) {
        this.command = command;
        this.version = version;
        this.mustBeZero = mustBeZero;
        this.mList = mList;
        this.sender = sender;
//        addDummyData();
    }

    public List<RoutingTableEntry> getmList() {
        return mList;
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

    /**
     * Return the byte array equivalent of full RIP packet.
     * This method includes bit manipulations performed on various fields of
     * the packet
     * @return
     */
    public byte[] toByteArray(String commandType) {
        byte[] arr = new byte[4 + (mList.size()*20)];
        // loop over each header
        int i = 0;
        // add HEADER: command
        if(commandType.equalsIgnoreCase(COMMAND_REQUEST))
            arr[i++] = (byte)Integer.parseInt(COMMAND_REQUEST);
        else
            arr[i++] = (byte)Integer.parseInt(COMMAND_RESPONSE);
        // add HEADER: version
        arr[i++] = (byte)Integer.parseInt(version);
        // add HEADER: mustBeZero
        byte roverId = Byte.valueOf(RoverManager.getInstance().getRoverId());
//        Log.router("byte roverId: " + roverId);
//        arr[i++] = (byte)Integer.parseInt(mustBeZero);
        arr[i++] = roverId;
        arr[i++] = (byte)Integer.parseInt(mustBeZero);

        // proceed only if the command type is response
        if(commandType.equalsIgnoreCase(COMMAND_RESPONSE))  {
            // add Routing Table Entries
            for(int j = 0; j < mList.size(); j++)   {
                RoutingTableEntry currentRTE = mList.get(j);
                // add address family identifier
                int addressFamily = currentRTE.getAddressFamilyIdentifier();
                byte[] byteEquivalent = new byte[2];
                byteEquivalent[0] = (byte) (addressFamily >> 8);
                byteEquivalent[1] = (byte) (addressFamily);
                for (byte b : byteEquivalent) {
                    arr[i++] = b;
                }

                // add route tag
                int routeTag = currentRTE.getRouteTag();
                byteEquivalent = new byte[2];
                byteEquivalent[0] = (byte) (routeTag >> 8);
                byteEquivalent[1] = (byte) (routeTag);
                for (byte b : byteEquivalent) {
                    arr[i++] = b;
                }

                // add IPv4 address
                String IpAddress = currentRTE.getAddress();
                // split the ip address by '.' and append 4 bytes to the byte array
                String[] s = IpAddress.split("\\.");
                for(int x = 0; x < 4; x++)  {
                    arr[i++] = Helper.BitwiseManager.convertIntegerToByte(s[x]);
                }

                // add subnet mask
                String subnetMask = currentRTE.getSubnetMask();
                // split the ip address by '.' and append 4 bytes to the byte array
                s = subnetMask.split("\\.");
                for(int x = 0; x < 4; x++)  {
                    arr[i++] = Helper.BitwiseManager.convertIntegerToByte(s[x]);
                }

                // add next hop IP
                String nextHop = currentRTE.getNextHop();
                // split the ip address by '.' and append 4 bytes to the byte array
                s = nextHop.split("\\.");
                for(int x = 0; x < 4; x++)  {
                    arr[i++] = Helper.BitwiseManager.convertIntegerToByte(s[x]);
                }

                // add metric
                int metric = currentRTE.getMetric();
                byteEquivalent = new byte[4];
                byteEquivalent[0] = (byte) (metric >> 24);
                byteEquivalent[1] = (byte) (metric >> 16);
                byteEquivalent[2] = (byte) (metric >> 8);
                byteEquivalent[3] = (byte) (metric);
                for (byte b : byteEquivalent) {
                    arr[i++] = b;
                }
            }
        }
        // return the byte array
//        System.out.println("Returning arr with size: " + arr.length);
        return arr;
    }

    /**
     * Return the sender's IP address
     * @return
     */
    public String getSender() {
        return "10.0." + sender + ".0";
    }

    /**
     * Print the routing table
     */
    public void print() {
        System.out.println("Address\t\tNextHop\t\tCost");
        System.out.println("===========================================");
        for(int i = 0; i < mList.size(); i++) {
            // get CIDR addressing from the given subnet mask
            String CIDRString = Helper.convertNetmaskToCIDR(mList.get(i).getAddress(), mList.get(i).getSubnetMask());
            System.out.println(CIDRString + "\t\t" + mList.get(i).getNextHop() + "\t\t" + mList.get(i).getMetric());
        }
    }

    /**
     * Mark a given neighboring network as dead (metric: unreachable)
     */
    public void markAsDead(String neighbor) {
        System.out.println("markAsDead: " + neighbor + " marked as DEAD");
        // go over all entries and mark the corresponding one as having metric 16
        for(RoutingTableEntry entry: mList)  {
            if(entry.getAddress().equalsIgnoreCase(neighbor))   {
                entry.setMetric(METRIC_UNREACHABLE);
            }
        }
    }


    /***
     * Test method
     * @param args
     */
    public static void main(String[] args) {
        new RIPPacket().toByteArray(COMMAND_REQUEST);
//        new RIPPacket().test();
    }
}
