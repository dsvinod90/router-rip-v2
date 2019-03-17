/**
 * {@link RouterProcess}
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
 *  4.  A lander is just another rover
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
                    .execute(new MainRouterProcess(multicastIp, port));
        }catch(ArrayIndexOutOfBoundsException ex){
            System.out.println("Please enter arguments as <multicast_ip> <id> <port>. Please refer to README.txt for reference.");
        }
    }
}

/**
 * The main server logic that listens to client
 * requests on a port number and serves them as needed
 */
class MainRouterProcess extends Thread{
    // the routing table
    private RIPPacket mRIPPacket = RoverManager.getInstance().getmRIPPacket();
    private String multicastIp;
    private static int ROUTER_PORT;
    private MulticastSocket routerSocket;

    /**
     * Constructor opens a server socket and listens
     */
    public MainRouterProcess(String multicastIp, String port)   {
        this.multicastIp = multicastIp;
        ROUTER_PORT = Integer.parseInt(port);
        try {
            // fire up the router listening port by subscribing to a multi cast IP
            // this port only listens to incoming broadcasts and then assigns the processing to
            // a worker thread
            this.routerSocket = new MulticastSocket(ROUTER_PORT);
            // subscribe to multicast IP address
            this.routerSocket.joinGroup(InetAddress.getByName(multicastIp));
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

    /**
     * Helper function to start executing the broadcasting process
     * in the central thread pool service
     */
    private void startBroadcastingProcess() {
        RoverManager.getInstance().getMyThreadPoolExecutorService().getService().execute(new BroadcastingProcess());
    }

    /**
     * The Broadcast manager
     * 1.   This thread is responsible for timely broadcast of the current
     * routing table to all the neighboring rovers that have subscribed
     * to the same multicast IP address.
     * 2.   It sends RIP packet over UDP protocol through DatagramSocket
     * 3.   The interval is set to 5 seconds
     */
    private static final int BROADCASTING_INTERVAL_IN_SECONDS = 5;

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

                    // pause for 5 seconds before re-broadcasting the packet
                    sleep(BROADCASTING_INTERVAL_IN_SECONDS*1000);
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
 * The received packet manager.
 * This thread is responsible for -
 * 1.   Updating the current access time for a given neighboring router
 * 2.   Parsing the incoming bytes and creating a new RIPPacket class out of them
 * 3.
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

            // do nothing if this packet belongs to this rover itself
            String mSender = String.valueOf(Integer.parseInt(
                    Helper.BitwiseManager.convertByteToHex(incomingBytes[2]), 16));
            if(mSender.equalsIgnoreCase(RoverManager.getInstance().getRoverId())){
                return;
            }

            // update the access time of the sender in the timeout table
            RoverManager.getInstance()
                    .getTimeoutManagementProcess()
                    .updateTimeout(Helper.parseSenderAddress(mSender));

            // parse the input and update rover's routing table
            parseBytes(incomingBytes);
        } catch (Exception ex) {
            Log.router(RoverManager.getInstance().getFullRoverId() + ": There was some problem reading data from the client");
            ex.printStackTrace();
        }
    }

    /**
     * Parse the incoming byte array to an RIPPacket
     * @param incomingBytes the incoming byte stream to be parsed
     */
    private void parseBytes(byte[] incomingBytes) {
        // extract command from header
        String mCommand =  String.valueOf(incomingBytes[0] & 0xff);

        // extract version from header
        String mVersion = String.valueOf(incomingBytes[1] & 0xff);

        // extract mustBeZero from header
        String mMustBeZero = String.valueOf(Long.parseLong(
                Helper.BitwiseManager.convertByteToHex(incomingBytes[3]), 16));

        // extract the sender router id from header
        String mSender = String.valueOf(Integer.parseInt(
                Helper.BitwiseManager.convertByteToHex(incomingBytes[2]), 16));

        // set the index to the first RTE index (i=4)
        int i = 4;

        // a temporary list of RoutingTableEntry that later becomes a part of the received RITPacket
        List<RoutingTableEntry> tempRoutingTableEntryList = new ArrayList<>();
        // loop over all the RTEs and extract relevant fields
        while(true) {
            try {
                int mAddressFamily = Integer.parseInt(
                        Helper.BitwiseManager.convertByteToHex(incomingBytes[i++])
                                + Helper.BitwiseManager.convertByteToHex(incomingBytes[i++]), 16);

                int mRouteTag = Integer.parseInt(
                        Helper.BitwiseManager.convertByteToHex(incomingBytes[i++])
                                + Helper.BitwiseManager.convertByteToHex(incomingBytes[i++]), 16);

                String mIpv4 = Long.parseLong(
                        Helper.BitwiseManager.convertByteToHex(incomingBytes[i++]), 16) + "." +
                        + Long.parseLong(Helper.BitwiseManager.convertByteToHex(incomingBytes[i++]), 16) + "." +
                        + Long.parseLong(Helper.BitwiseManager.convertByteToHex(incomingBytes[i++]), 16) + "." +
                        + Long.parseLong(Helper.BitwiseManager.convertByteToHex(incomingBytes[i++]), 16);

                String mSubnet = Long.parseLong(
                        Helper.BitwiseManager.convertByteToHex(incomingBytes[i++]), 16) + "." +
                        + Long.parseLong(Helper.BitwiseManager.convertByteToHex(incomingBytes[i++]), 16) + "." +
                        + Long.parseLong(Helper.BitwiseManager.convertByteToHex(incomingBytes[i++]), 16) + "." +
                        + Long.parseLong(Helper.BitwiseManager.convertByteToHex(incomingBytes[i++]), 16);

                String mNextHop = Long.parseLong(
                        Helper.BitwiseManager.convertByteToHex(incomingBytes[i++]), 16) + "." +
                        + Long.parseLong(Helper.BitwiseManager.convertByteToHex(incomingBytes[i++]), 16) + "." +
                        + Long.parseLong(Helper.BitwiseManager.convertByteToHex(incomingBytes[i++]), 16) + "." +
                        + Long.parseLong(Helper.BitwiseManager.convertByteToHex(incomingBytes[i++]), 16);

                int mMetric = Integer.parseInt(
                        Helper.BitwiseManager.convertByteToHex(incomingBytes[i++])
                                + Helper.BitwiseManager.convertByteToHex(incomingBytes[i++])
                                + Helper.BitwiseManager.convertByteToHex(incomingBytes[i++])
                                + Helper.BitwiseManager.convertByteToHex(incomingBytes[i++]), 16);

                // check if there are any more RTE bytes to be read from this packet
                if(isPacketOver(mAddressFamily, mRouteTag, mIpv4, mSubnet)) {
                    break;
                }

                // create a RoutingTableEntry from the above data
                // and append to the end of the tempList
                tempRoutingTableEntryList.add(new RoutingTableEntry(mAddressFamily, mRouteTag, mIpv4, mSubnet, mNextHop, mMetric));
            }catch(ArrayIndexOutOfBoundsException ex)   {
                break;
            }
        }

        // now that we have all the data from the received RIPPacket
        // let us update the current routing table accordingly
        updateMyRoutingTable(new RIPPacket(mCommand, mVersion, mMustBeZero, mSender, tempRoutingTableEntryList));
    }

    /**
     * Method to compare each entry in the incoming table and update
     * the routing table as needed.
     * @param receivedRIPPacket
     */
    private void updateMyRoutingTable(RIPPacket receivedRIPPacket) {
        boolean hasRoutingTableChanged = false;
        boolean isThisSenderInMyTable = false;
        // loop over each entry in the current table and check if the incoming
        // RIPPacket belongs to a sender that we have already saved before
        for(RoutingTableEntry myEntry: mRIPPacket.getmList()) {
            if(myEntry.getAddress().equalsIgnoreCase(receivedRIPPacket.getSender()))  {
                isThisSenderInMyTable = true;
                myEntry.setNextHop(receivedRIPPacket.getSender());
                if(myEntry.getMetric() != 1) {
                    myEntry.setMetric(1);
                    hasRoutingTableChanged = true;
                }
            }
        }

        // if my routing table is empty or the sender address was not found in the
        // routing table, then just add this neighbor rover to the routing table
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
                    isMatchFound = true;
                    // if the entry already matches some entry in the current table then some cases arise
                    // Check if the NEXT HOP of the entry in the CURRENT table
                    // equals the incoming router network address
                    if(myEntry.getNextHop().equalsIgnoreCase(receivedRIPPacket.getSender()))  {
                        // trust the incoming packet blindly
                        // overwrite the metric current instance for this entry
                        if(1+incomingEntry.getMetric() >= RIPPacket.METRIC_UNREACHABLE)   {
                            myEntry.setMetric(RIPPacket.METRIC_UNREACHABLE);
                        } else {
                            myEntry.setMetric(1 + incomingEntry.getMetric());
                        }
                        hasRoutingTableChanged = true;
                    }else   {
                        // find the better one of the two options
                        if(incomingEntry.getMetric() >= RIPPacket.METRIC_UNREACHABLE)   {
                            myEntry.setMetric(RIPPacket.METRIC_UNREACHABLE);
                        }
                        else if((1+incomingEntry.getMetric()) < myEntry.getMetric())  {
                            // incoming is better, time to update the current entry
                            myEntry.setMetric(1+incomingEntry.getMetric());
                            // update the next hop to this new client
                            myEntry.setNextHop(receivedRIPPacket.getSender());
                            // mark as changed
                            hasRoutingTableChanged = true;
                        }
                    }
                }
            }

            // if matches == false this means that this particular destination
            // address has no mention in the router's own routing table
            // hence we can just add this to the current routing table
            // also ensure that this address is not the same as my own address
            if(!isMatchFound && !incomingEntry.getAddress().equalsIgnoreCase(RoverManager.getInstance().getFullRoverId()))    {
                mRIPPacket.getmList().add(new RoutingTableEntry(RoutingTableEntry.ADDRESS_FAMILY_IP
                        , RoutingTableEntry.ROUTE_TAG
                        , incomingEntry.getAddress()
                        , RoutingTableEntry.SUBNET_MASK
                        , receivedRIPPacket.getSender()
                        , (1+incomingEntry.getMetric())));
                hasRoutingTableChanged = true;
            }
        }

        // print the routing table if anything changed
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
    private ConcurrentHashMap<String, Long> timeoutTable = new ConcurrentHashMap<>();
    /**
     * Update the current access time of a rover
     * in the hashtable
     * @param neighbor
     */
    public void updateTimeout(String neighbor) {
        long currentTime = System.currentTimeMillis() / 1000;
        timeoutTable.put(neighbor, currentTime);
    }

    @Override
    public void run()   {
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
                ex.printStackTrace();
                break;
            }
        }
    }
}