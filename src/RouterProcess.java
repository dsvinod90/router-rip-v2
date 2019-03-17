/**
 * RouterProcess.java
 *
 * @version:
 *      1.0.1
 *
 * @revision:
 *      1
 *
 * @author:
 *      ishanguliani aka ig5859
 *
 */

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * A running Router Process performs the following tasks -
 *  1.  Listen for incoming RIP messages over MulticastSocket (as UDP payload)
 *  2.  Broadcasts the routing table to neighboring rovers every 5 seconds
 *  3.  Maintain a mapping between each neighboring rover and last time a broadcast was
 *      received. Marks a rover as unreachable if it does hear an update from it in 10 seconds
 *  4.  Uses CIDR to represent networks. CIDR addressing depends on the network subnet mask
 *
 *  SOME ASSUMPTIONS:
 *  1.  The initial topology of the network is such that each of the 10 rovers are in the
 *      vicinity of all other 9 rovers
 *  2.  The lander is in the vicinity of at least one rover at any given point of time
 *  3.  For simplicity, the mustBeZero field of the RIP packet is used to transmit the unique
 *      router id between routers
 *
 *  EXECUTING:
 *  1.  java RouterProcess <multicast_ip> <unique_router_id> <port_number>
 *      java            -   the java program
 *      RouterProcess   -   the driver class
 *      multicast_id    -   the IP address over which the rovers will talk to each other
 *                          Recommended by RFC : 224.0.0.9
 *      unique_router_id-   a user-given unique router id between [1, 255]
 *      port_number     -   a shared port number over which multicast messages will be sent
 */
public class RouterProcess {

    public static void main(String[] args) {
        try {
            String multicastIp = args[0];
            String id = args[1];
            RoverManager.getInstance().setRoverId(id);
            String port = args[2];
            RoverManager.getInstance()
                    .getMyThreadPoolExecutorService()
                    .getService()
                    .execute(new MainRouterThread(multicastIp, port));
        }catch(ArrayIndexOutOfBoundsException ex){
            System.out.println("Please enter arguments as <multicast_ip> <unique_router_id> <port_number>");
        }
    }
}

/**
 * The main server logic that listens to client
 * requests on a port number and serves them as needed
 */
class MainRouterThread extends Thread{

    // the routing table
    private RIPPacket mRIPPacket = RoverManager.getInstance().getmRIPPacket();
    //    private ServerSocket routerSocket;
    private String multicastIp;
    private String port;
    //    private static final String MULTICAST_IP = "224.0.0.9";
    private static int ROUTER_PORT;
    private MulticastSocket routerSocket;

    /**
     * Constructor opens a server socket and listens
     */
    public MainRouterThread(String multicastIp, String port)   {
        this.multicastIp = multicastIp;
        ROUTER_PORT = Integer.parseInt(port);
        try {
            // fire up the router listening port by subscribing to a multi cast IP
            // this port only listens to incoming broadcasts and then assigns the processing to
            // a worker thread
            this.routerSocket = new MulticastSocket(ROUTER_PORT);
            // subscribe to multicast IP address
            this.routerSocket.joinGroup(InetAddress.getByName(multicastIp));
//            Log.router(RoverManager.getInstance().getFullRoverId() + ": Router is now running on port: " + ROUTER_PORT);
            // start broadcasting routing table updates
            startBroadcastingProcess();
            // fire up the timeout process
            RoverManager.getInstance()
                    .getMyThreadPoolExecutorService()
                    .getService()
                    .execute(RoverManager.getInstance().getTimeoutManagementProcess());
        }catch(IOException ex)  {
            System.err.println("There was some problem opening a socket on the server. Check again...");
            ex.printStackTrace();
            System.exit(0);
        }
    }

    /**
     * Run method implements the logic of collecting requests
     * from the client and processing them as needed
     */
    @Override
    public void run()   {
        while(true) {
            try{
                byte[] buffer = new byte[504];
                DatagramPacket incomingPacket = new DatagramPacket(buffer, buffer.length);
                // read the incoming data into the packet
                routerSocket.receive(incomingPacket);
                // dispatch this client socket to a worker thread
                RoverManager.getInstance().getMyThreadPoolExecutorService().getService().execute(new ParseReceivedPacketProcess(incomingPacket, routerSocket, mRIPPacket));
            }catch(IOException ex){
                ex.printStackTrace();
            }
        }
    }

    private void startBroadcastingProcess() {
        RoverManager.getInstance().getMyThreadPoolExecutorService().getService().execute(new BroadcastingProcess());
    }

    public class BroadcastingProcess extends Thread {
        DatagramSocket routingSocket;

        public BroadcastingProcess() {
            // initialise the socket to broadcast
            try {
                this.routingSocket = new DatagramSocket();
                Log.router(RoverManager.getInstance().getFullRoverId() + ": Ready! Broadcasting request and waiting for a response...");
            } catch (IOException e) {
                e.printStackTrace();
                Log.router(RoverManager.getInstance().getFullRoverId() + ": Failed! cannot initialise Datagram socket inside BroadcastingProcess");
            }
        }

        @Override
        public void run()   {
            // prepare the destination address of the packet
            InetAddress group = null;
            try {
                group = InetAddress.getByName(multicastIp);
            } catch (UnknownHostException e) {
                e.printStackTrace();
                Log.router(RoverManager.getInstance().getFullRoverId() + ": Unknown host Exception inside BroadcastingProcess");
            }

            // prepare the destination port
            Integer destinationPort = ROUTER_PORT;

            // prepare the payload
            while(true) {
                byte[] buff;
                if (mRIPPacket.getmList().size() == 0) {
                    // there is nothing in the table, broadcast request packet
                    buff = mRIPPacket.toByteArray(RIPPacket.COMMAND_REQUEST);
                } else{
                    // else broadcast response packet
                    buff = mRIPPacket.toByteArray(RIPPacket.COMMAND_RESPONSE);
                }

                try {
                    DatagramPacket packet = new DatagramPacket(buff, buff.length, group, destinationPort);
                    routingSocket.send(packet);
//                    System.out.print("\n"+RoverManager.getInstance().getFullRoverId() + ": The packet has been sent with payload: " + buff.length  +"\n[");
//                    for(int x = 0; x < buff.length; x++)    {
//                        System.out.print(buff[x] + " ");
//                    }
//                    System.out.print("]\n");

//                    Log.router(RoverManager.getInstance().getFullRoverId() + " My routing table");
//                    mRIPPacket.print();
                    // pause for 5 seconds before re-broadcasting the packet
                    sleep(5000);
                } catch(InterruptedException e){
                    e.printStackTrace();
                    Log.router(RoverManager.getInstance().getFullRoverId() + ": Something went wrong while sleeping...");
                    break;
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.router(RoverManager.getInstance().getFullRoverId() + ": Something went wrong while sending...");
                    break;
                }
            }
            Log.router(RoverManager.getInstance().getFullRoverId() + ": Stop sending broadcast packets");
        }
    }
}

/**
 * The thread which serves a single client
 */
class ParseReceivedPacketProcess extends Thread {
    private DatagramPacket clientPacket;
    private DatagramSocket clientSocket;
    private RIPPacket mRIPPacket;

    public ParseReceivedPacketProcess(DatagramPacket clientPacket, DatagramSocket clientSocket, RIPPacket mRIPPacket) {
        this.clientPacket = clientPacket;
        this.clientSocket = clientSocket;
        this.mRIPPacket = mRIPPacket;
    }

    @Override
    public void run()   {
        try{
            // extract data from the packet
            byte[] incomingBytes = clientPacket.getData();
            // return if sent by self
            String mSender = String.valueOf(Integer.parseInt(
                    Helper.BitwiseManager.convertByteToHex(incomingBytes[2]), 16));
            if(mSender.equalsIgnoreCase(RoverManager.getInstance().getRoverId())){
                return;
            }

            // update the access time of the sender in the timeout table
            RoverManager.getInstance()
                    .getTimeoutManagementProcess()
                    .updateTimeout(Helper.parseSenderAddress(mSender));
            // parse the input and update router's table
            parseBytes(incomingBytes);
        } catch (Exception ex) {
            Log.router(RoverManager.getInstance().getFullRoverId() + ": There was some problem reading data from the client");
            ex.printStackTrace();
        }
    }

    private void parseBytes(byte[] incomingBytes) {
//        System.out.println("printing whats received " + incomingBytes.length + "bytes");
//        for (byte el: incomingBytes)    {
//            System.out.print (el + " ");
//        }
//        System.out.println();

        // extract command
        String mCommand =  String.valueOf(incomingBytes[0] & 0xff);
//        System.out.println("command: "+ mCommand);
        // extract version
        String mVersion = String.valueOf(incomingBytes[1] & 0xff);
//        System.out.println("version: " + mVersion);
        // extract mustBeZero
        String mMustBeZero = String.valueOf(Long.parseLong(
                Helper.BitwiseManager.convertByteToHex(incomingBytes[3]), 16));

        String mSender = String.valueOf(Integer.parseInt(
                Helper.BitwiseManager.convertByteToHex(incomingBytes[2]), 16));

//        System.out.println("mustBeZero: " + mMustBeZero);
//        System.out.println("mSender: " + mSender);

        int i = 4;
        List<RoutingTableEntry> tempList = new ArrayList<>();
        // get the RTEs
        while(true) {
            try {
                int mAddressFamily = Integer.parseInt(
                        Helper.BitwiseManager.convertByteToHex(incomingBytes[i++])
                                + Helper.BitwiseManager.convertByteToHex(incomingBytes[i++]), 16);


                int mRouteTag = Integer.parseInt(
                        Helper.BitwiseManager.convertByteToHex(incomingBytes[i++])
                                + Helper.BitwiseManager.convertByteToHex(incomingBytes[i++]), 16);

                String mIpv4 = String.valueOf( Long.parseLong(
                        Helper.BitwiseManager.convertByteToHex(incomingBytes[i++]), 16) + "." +
                        + Long.parseLong(Helper.BitwiseManager.convertByteToHex(incomingBytes[i++]), 16) + "." +
                        + Long.parseLong(Helper.BitwiseManager.convertByteToHex(incomingBytes[i++]), 16) + "." +
                        + Long.parseLong(Helper.BitwiseManager.convertByteToHex(incomingBytes[i++]), 16));

                String mSubnet = String.valueOf(Long.parseLong(
                        Helper.BitwiseManager.convertByteToHex(incomingBytes[i++]), 16) + "." +
                        + Long.parseLong(Helper.BitwiseManager.convertByteToHex(incomingBytes[i++]), 16) + "." +
                        + Long.parseLong(Helper.BitwiseManager.convertByteToHex(incomingBytes[i++]), 16) + "." +
                        + Long.parseLong(Helper.BitwiseManager.convertByteToHex(incomingBytes[i++]), 16));

                String mNextHop = String.valueOf(Long.parseLong(
                        Helper.BitwiseManager.convertByteToHex(incomingBytes[i++]), 16) + "." +
                        + Long.parseLong(Helper.BitwiseManager.convertByteToHex(incomingBytes[i++]), 16) + "." +
                        + Long.parseLong(Helper.BitwiseManager.convertByteToHex(incomingBytes[i++]), 16) + "." +
                        + Long.parseLong(Helper.BitwiseManager.convertByteToHex(incomingBytes[i++]), 16));

                int mMetric = Integer.parseInt(
                        Helper.BitwiseManager.convertByteToHex(incomingBytes[i++])
                                + Helper.BitwiseManager.convertByteToHex(incomingBytes[i++])
                                + Helper.BitwiseManager.convertByteToHex(incomingBytes[i++])
                                + Helper.BitwiseManager.convertByteToHex(incomingBytes[i++]), 16);

                if(isPacketOver(mAddressFamily, mRouteTag, mIpv4, mSubnet)) {
                    break;
                }

             /*   System.out.println("addressFamily1: " + mAddressFamily);
                System.out.println("RouteTag: " + mRouteTag);
                System.out.println("Ipv4: " + mIpv4);
                System.out.println("Subnet: " + mSubnet);
                System.out.println("NextHop: " + mNextHop);
                System.out.println("Metric: " + mMetric);*/
//                System.out.println();

                // add a new routing entry to the temp routing table list
                tempList.add(new RoutingTableEntry(mAddressFamily, mRouteTag, mIpv4, mSubnet, mNextHop, mMetric));
            }catch(ArrayIndexOutOfBoundsException ex)   {
//                ex.printStackTrace();
                break;
            }
        }
        updateMyRoutingTable(new RIPPacket(mCommand, mVersion, mMustBeZero, mSender, tempList));
    }

    /**
     * Method to compare each entry in the incoming table and update
     * the routing table as needed.
     * @param receivedRIPPacket
     */
    private void updateMyRoutingTable(RIPPacket receivedRIPPacket) {

        // check if there is an entry for this neighbor network itself
//        boolean neighborExistsInRoutingTable = false;

        boolean hasRoutingTableChanged = false;
        boolean isThisSenderInMyTable = false;
        for(RoutingTableEntry myEntry: mRIPPacket.getmList()) {
            if(myEntry.getAddress().equalsIgnoreCase(receivedRIPPacket.getSender()))  {
                isThisSenderInMyTable = true;
//                System.out.println("SOME match found : pre existing entry for that sender");
//                neighborExistsInRoutingTable = true;
                // now this sender is in my own routing table and also is talking to me directly,
                // hence update the current routing table
                // Check if the next hop to go to this sender is itself, if yes. ALL GOOD else update
                // the next hop to the sender and the metric to 1 (to denote direct connection)
                if(!myEntry.getNextHop().equalsIgnoreCase(receivedRIPPacket.getSender()))   {
                    System.out.println("SOME match found : pre existing entry for that sender");
                    // update
                    myEntry.setNextHop(receivedRIPPacket.getSender());
                    myEntry.setMetric(1);
                    hasRoutingTableChanged = true;
                }
            }
        }

        // if my routing table is empty, then just add this neighbor rover
//        if((mRIPPacket.getmList().size() == 0) || (!neighborExistsInRoutingTable))   {
        if((mRIPPacket.getmList().size() == 0) || !isThisSenderInMyTable)   {
            mRIPPacket.getmList().add(new RoutingTableEntry(RoutingTableEntry.ADDRESS_FAMILY_IP
                    , RoutingTableEntry.ROUTE_TAG
                    , receivedRIPPacket.getSender()
                    , RoutingTableEntry.SUBNET_MASK
                    , receivedRIPPacket.getSender()
                    , 1));
            hasRoutingTableChanged = true;
        }

        // go over each entry in the incoming table
        for(RoutingTableEntry incomingEntry: receivedRIPPacket.getmList())  {
            // check if the destination address of this entry
            // matches with the destination address of any entry in the current table
            boolean isMatchFound = false;
            for(RoutingTableEntry myEntry: mRIPPacket.getmList())    {
                if(myEntry.getAddress().equalsIgnoreCase(incomingEntry.getAddress()))    {
//                    System.out.println("updateMyRoutingTable: Entry" +  incomingEntry.getAddress() + " exists in my table");
                    isMatchFound = true;
                    // if the entry already matches some entry in the current table then some cases arise
                    // Check if the NEXT HOP of the entry in the CURRENT table
                    // equals the incoming router network address
                    if(myEntry.getNextHop().equalsIgnoreCase(receivedRIPPacket.getSender()))  {
                        // trust the incoming packet blindly
                        // overwrite the metric current instance for this entry
                        if(incomingEntry.getMetric() == RIPPacket.METRIC_UNREACHABLE)  {
                            myEntry.setMetric(RIPPacket.METRIC_UNREACHABLE);
                        }else {
                            myEntry.setMetric(1 + incomingEntry.getMetric());
                            hasRoutingTableChanged = true;
                            Log.router(RoverManager.getInstance().getFullRoverId() + " changed 1");
                        }
                    }else   {
                        // find the better one of the two options
                        if((1+incomingEntry.getMetric()) < myEntry.getMetric())  {
                            // incoming is better, time to update the current entry
                            myEntry.setMetric(1+incomingEntry.getMetric());
                            // update the next hop to this new client
                            myEntry.setNextHop(receivedRIPPacket.getSender());
                            // mark as changed
                            hasRoutingTableChanged = true;
                            Log.router(RoverManager.getInstance().getFullRoverId() + " changed 2");
                        }
                    }
                }
            }
            // if matches == false this means that this particular destination
            // address has no mention in the router's own routing table
            // hence we can just add this to the current routing table
            // also ensure that this address is not the same as my own address
            if(!isMatchFound && !incomingEntry.getAddress().equalsIgnoreCase(RoverManager.getInstance().getFullRoverId()))    {
                System.out.println("ADDED TO TABLE : " + incomingEntry.getAddress() + ", " + receivedRIPPacket.getSender() + ", " + (1+incomingEntry.getMetric()));
                mRIPPacket.getmList().add(new RoutingTableEntry(RoutingTableEntry.ADDRESS_FAMILY_IP
                        , RoutingTableEntry.ROUTE_TAG
                        , incomingEntry.getAddress()
                        , RoutingTableEntry.SUBNET_MASK
                        , receivedRIPPacket.getSender()
                        , (1+incomingEntry.getMetric())));
                hasRoutingTableChanged = true;
            }
        }

        if(hasRoutingTableChanged) {
            mRIPPacket.print();
        }
    }

    /**
     * Return true if the end of packet is reached. The end is determined by
     * the values of the various fields within the packet. If all of them converge
     * to null at any given point of time then that means that there is nothing else
     * to go over inside the packet
     * @param addressFamily
     * @param routeTag
     * @param ipv4
     * @param subnet
     * @return
     */
    private boolean isPacketOver(int addressFamily, int routeTag, String ipv4, String subnet) {
        return addressFamily == 0
                && routeTag == 0
                && ipv4.equalsIgnoreCase("0.0.0.0")
                && subnet.equalsIgnoreCase("0.0.0.0");
    }
}

/**
 * The Time out manager
 * This process runs every 10 seconds for all the immediate neighbors in
 * the routing table. If any neighbor has not responded over the last 10
 * seconds, then an action is taken -
 * Action :
 * The action includes marking the corresponding neighbor as UNREACHABLE in
 * the routing table and then triggering this change to the subsequent neigh-
 * boring rovers
 */
class TimeoutManagementProcess extends Thread {
    private HashMap<String, Long> timeoutTable = new HashMap<>();
    /**
     * Update the current access time of a rover
     * in the hashtable
     * @param neighbor
     */
    public void updateTimeout(String neighbor) {
        timeoutTable.put(neighbor, System.currentTimeMillis() / 1000);
//        System.out.println("TimeoutManagementProcess: timeout updated : <" + neighbor + timeoutTable.get(neighbor) + ">");
    }

    @Override
    public void run()   {
//        System.out.println("TimeoutManagementProcess: TIMEOUT MANAGEMENT FIRED UP");
        while(true) {
            try {
                sleep(10000);
                // do something
                // check which entries are timing out
                for(Map.Entry<String, Long> entry: timeoutTable.entrySet()) {
                    long currentTime = System.currentTimeMillis() / 1000;
                    if((currentTime - entry.getValue()) > 10)   {
                        // mark the neighboring rover as dead
                        RoverManager.getInstance().getmRIPPacket().markAsDead(entry.getKey());
                        // print the new table
                        RoverManager.getInstance().getmRIPPacket().print();
                        // remove this entry from the hashmap
                        this.timeoutTable.remove(entry.getKey());
                    }
                }
            } catch (Exception ex) {
                break;
            }
        }
    }
}