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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.Thread.sleep;

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
            String destinationIp = null;
            String filename = null;
            if(args.length == 5) {
                destinationIp = args[3];
                filename = args[4];
            }
//            else
//                System.out.println("DID NOT GET DEST IP AND FILENAME");

//            RoverManager.getInstance()
//                    .getMyThreadPoolExecutorService()
//                    .getService()
//                    .execute(new MainRouterProcess(multicastIp, port, destinationIp, filename));

            new MainRouterProcess(multicastIp, port, destinationIp, filename).start();
            sleep(200);
            new BroadcastingProcess(multicastIp, port).start();
            sleep(200);
            RoverManager.getInstance().getTimeoutManagementProcess().start();
            sleep(200);

//            new ByteReceivingProcess();
//
//            RoverManager.getInstance()
//                    .getMyThreadPoolExecutorService()
//                    .getService()
//                    .execute(new AckReceivingProcess());
//
//            if(!startedSendingBytes)    {
//            if(filename != null && destinationIp != null)   {
//                RoverManager.getInstance()
//                        .getMyThreadPoolExecutorService()
//                        .getService()
//                        .execute(new ByteSendingProcess(destinationIp, filename));
//            }

        }catch(ArrayIndexOutOfBoundsException ex){
            System.out.println("Please enter arguments as <multicast_ip> <id> <port>. Please refer to README.txt for reference.");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

/**
 * The Broadcast manager
 * 1.   This thread is responsible for timely broadcast of the current
 * routing table to all the neighboring rovers that have subscribed
 * to the same multicast IP address.
 * 2.   It sends RIP packet over UDP protocol through DatagramSocket
 * 3.   The interval is set to 5 seconds
 */
class BroadcastingProcess extends Thread {
    private static final int BROADCASTING_INTERVAL_IN_SECONDS = 5;
    DatagramSocket routingSocket;
    RIPPacket mRIPPacket;
    String multicastIp;
    private static int ROUTER_PORT;

    public BroadcastingProcess(String multicastIp, String port) {
        mRIPPacket = RoverManager.getInstance().getmRIPPacket();
        ROUTER_PORT = Integer.parseInt(port);
        this.multicastIp = multicastIp;
        // initialise the socket to broadcast
        try {
            this.routingSocket = new DatagramSocket();
//            Log.router(RoverManager.getInstance().getFullRoverId() + ": Ready! Broadcasting request and waiting for a response on port : " + routingSocket.getLocalPort());
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
                DatagramPacket packet = new DatagramPacket(buff, buff.length, group, ROUTER_PORT);
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

/**
 * The main server logic that listens to client
 * requests on a port number and serves them as needed
 */
class MainRouterProcess extends Thread{
    private static final Integer ACK_PORT = 61122;
    // the routing table
    private RIPPacket mRIPPacket = RoverManager.getInstance().getmRIPPacket();
    private String multicastIp;
    private String destinationIp;
    private String filename;
    private boolean startedSendingBytes = false;
    private static int ROUTER_PORT;
    private static int FILE_TRANSFER_PORT = 65122;
    private MulticastSocket multicastRouterSocket;

    /**
     * Constructor opens a server socket and listens
     */
    public MainRouterProcess(String multicastIp, String port, String destinationIp, String filename)   {
        Log.router(RoverManager.getInstance().getFullRoverId() + ": NOTE: Sending will only begin when complete routing table has been populated\n");
        this.filename = filename;
        this.multicastIp = multicastIp;
        this.destinationIp = destinationIp;
        ROUTER_PORT = Integer.parseInt(port);
        try {
            // fire up the router listening port by subscribing to a multi cast IP
            // this port only listens to incoming broadcasts and then assigns the processing to a worker thread
            try {
                this.multicastRouterSocket = new MulticastSocket(ROUTER_PORT);
                this.multicastRouterSocket.joinGroup(InetAddress.getByName(multicastIp));
            } catch (IOException e) {
                e.printStackTrace();
            }
//            NetworkInterface networkInterface = getInterfaceName();
//            System.out.println("MainRouterProcess(): interface toString(): " + networkInterface.toString());
//            System.out.println(" MainRouterProcess(): interfaceName displayName(): " + networkInterface.getDisplayName());
//            try {
//                this.multicastRouterSocket.joinGroup(new InetSocketAddress(InetAddress.getByName(multicastIp), ROUTER_PORT), networkInterface);
//                System.out.println("MainRouterProcess(): multicastRouterSocket is set");
//            } catch (IOException e) {
//                e.printStackTrace();
//            }

            // start executing the routing table broadcasting process in the central thread pool service
//            RoverManager.getInstance()
//                    .getMyThreadPoolExecutorService()
//                    .getService().execute(new BroadcastingProcess());
            // fire up the timeout process
//            RoverManager.getInstance()
//                    .getMyThreadPoolExecutorService()
//                    .getService()
//                    .execute(RoverManager.getInstance().getTimeoutManagementProcess());
            if (filename != null && destinationIp != null) {
//                System.out.println("firing sending");
                new ByteSendingProcess(destinationIp, filename, null).start();
                RoverManager.getInstance()
                        .getMyThreadPoolExecutorService()
                        .getService()
                        .execute(new AckReceivingProcess());
            } else {
//                System.out.println("firing receiving");
                RoverManager.getInstance()
                        .getMyThreadPoolExecutorService()
                        .getService()
                        .execute(new ByteReceivingProcess());
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("BroadcastingProcess(): could not sleep well ");
        }
    }

    /**
     * A thread to send bytes of the given file over the network
     */
    public class ByteReceivingProcess extends Thread {
        DatagramSocket byteReceivingSocket;

        public ByteReceivingProcess() {
            // initialise the socket to broadcast
            try {
                this.byteReceivingSocket = new DatagramSocket(FILE_TRANSFER_PORT);
//                Log.router(RoverManager.getInstance().getFullRoverId() + ": byte receiving socket binded to port : " + byteReceivingSocket.getLocalPort());
                Log.router(RoverManager.getInstance().getFullRoverId() + ": Ready for action");
            } catch (IOException e) {
                e.printStackTrace();
                Log.router(RoverManager.getInstance().getFullRoverId() + ": Failed! cannot initialise Datagram socket inside ByteReceivingProcess");
            }
        }

        @Override
        public void run()   {
            System.out.println("Receiving data...\n");
            while (true) {
                try {
                    byte[] buffer = new byte[11];
                    DatagramPacket incomingPacket = new DatagramPacket(buffer, buffer.length);
                    // read the incoming data into the packet
                    byteReceivingSocket.receive(incomingPacket);
//                    System.out.println("got something in...");
                    // check if the destination address of the packet equals the current rover's address
                    String fullDestinationAddress = Helper.parseSenderAddress(String.valueOf(Integer.parseInt(
                            Helper.BitwiseManager.convertByteToHex(incomingPacket.getData()[9]), 16)));
                    String fullSenderIpAddress = Helper.parseSenderAddress(String.valueOf(Integer.parseInt(
                            Helper.BitwiseManager.convertByteToHex(incomingPacket.getData()[5]), 16)));
                    String endOfData = String.valueOf(Integer.parseInt(
                            Helper.BitwiseManager.convertByteToHex(incomingPacket.getData()[0]), 16));
                    String data = String.valueOf(Integer.parseInt(
                            Helper.BitwiseManager.convertByteToHex(incomingPacket.getData()[1]), 16));
                    String ack = String.valueOf(Integer.parseInt(
                            Helper.BitwiseManager.convertByteToHex(incomingPacket.getData()[2]), 16));

                    NewPacket receivedPacket = new NewPacket(endOfData, data, ack, fullSenderIpAddress, fullDestinationAddress);

//                    System.out.println("ByteReceivingProcess(): received endOfData: " + endOfData);
//                    System.out.println("ByteReceivingProcess(): received data: " + data);

                    // check if this packet was destined to me, if yes then proceed else forward it...
//                    System.out.println("ByteReceivingProcess(): received ack: " + ack);
                    if(!fullDestinationAddress.equalsIgnoreCase(RoverManager.getInstance().getFullRoverId()))    {
                        // not destined to reach me, hence lets just forward it
                        new ByteSendingProcess(fullDestinationAddress, null, receivedPacket).start();
                        continue;
                    }

                    // if the above condition was not met, this means this packet is destined to reach me
                    // check if this incoming byte has already been acknowledged in the last ack
                    if(data.equalsIgnoreCase(control.lastAckSent)) {
                        // if already acked, then ack again
//                        System.out.println("ByteReceivingProcess(): received data looks redundant as last ack: " + control.lastAckSent + " - skipping reception...");
                        RoverManager.getInstance()
                                .getMyThreadPoolExecutorService()
                                .getService()
                                .execute(new AckSendingProcess(fullSenderIpAddress, control.lastAckSent));
                        continue;
                    }

                    // avoid processing packet if the sender is myself
                    if(fullSenderIpAddress.equalsIgnoreCase(RoverManager.getInstance().getFullRoverId()))    {
//                        System.out.println("ByteReceivingProcess(): received fullSenderIpAddress : " + fullSenderIpAddress + ", this is me, skipping");
                        continue;
                    }else   {
//                        System.out.println("ByteReceivingProcess(): received fullSenderIpAddress : " + fullSenderIpAddress + ", a new sender, processing...");
                    }


//                    System.out.println("ByteReceivingProcess(): received packet: " + receivedPacket);

                    byte parsedData = incomingPacket.getData()[1];
//                    System.out.println("ByteReceivingProcess(): run(): data: " + data + ", byte parsedData: " + parsedData);
                    // check if this sender exists in the connected hosts hashmap
                    if(RoverManager.getInstance().getConnectedHostsMap().containsKey(fullSenderIpAddress))  {
//                        System.out.println("ByteReceivingProcess(): run(): found connected host: " + fullSenderIpAddress + ", with byte length: " + RoverManager.getInstance().getConnectedHostsMap().get(fullSenderIpAddress).size());
                    }else   {
                        // add the host to the connected hosts list with an empty byte array
                        RoverManager.getInstance().getConnectedHostsMap().put(fullSenderIpAddress, new ArrayList<>());
//                        System.out.println("ByteReceivingProcess(): run(): added new host: " + fullSenderIpAddress);
                    }

                    // check if this the packet with endOfData flag set
                    if(endOfData.matches("1*")) {
                        // just mark the end of the reception on the connected host
                        ArrayList<Byte> completedFile = RoverManager.getInstance().getConnectedHostsMap().get(fullSenderIpAddress);
                        // remove the host from the connected hist list
                        RoverManager.getInstance().getConnectedHostsMap().remove(fullSenderIpAddress);
                        // print the completed file
                        System.out.println("\n\nYaaayy! Got the file!\n" + completedFile);
                        System.out.println("\n\nSuccessfully received all packets!");
                        break;
                    }else   {
                        // since the endOfData is not set, this file has some data so
                        // add it to the corresponding byte list of the host
                        RoverManager.getInstance().getConnectedHostsMap().get(fullSenderIpAddress).add(parsedData);
                        System.out.print(parsedData + ", ");
                    }

                    // now since we have taken care of the complete byte, this is the right time
                    // to send an acknowledgement back to the original sender, do it now
                    RoverManager.getInstance()
                            .getMyThreadPoolExecutorService()
                            .getService()
                            .execute(new AckSendingProcess(fullSenderIpAddress, Byte.toString(parsedData)));

//                    RoverManager.getInstance().getConnectedHostsMap().put(fullDestinationAddress, incomingPacket.getAddress().toString().substring(1));
//                System.out.println("mapped: " + RoverManager.getInstance().getIpAddressMap());
//                System.out.println("Packet from : " + incomingPacket.getAddress().toString() + ", SocketAddress: " + incomingPacket.getSocketAddress() + ", Port: " + incomingPacket.getPort());
                    // dispatch this client socket to a worker thread

                } catch (IOException ex) {
                    ex.printStackTrace();
                    break;
                }
            }
//            Log.router(RoverManager.getInstance().getFullRoverId() + ": ByteReceivingProcess(): Stop sending broadcast packets");
            Log.router(RoverManager.getInstance().getFullRoverId() + ": Not receiving anymore. All done.");
        }
    }

    /**
     * Run method implements the logic of collecting requests
     * from the client and processing them as needed
     */
    @Override
    public void run() {
        while (true) {
            try {
                byte[] buffer = new byte[504];
                DatagramPacket incomingPacket = new DatagramPacket(buffer, buffer.length);
                // read the incoming data into the packet
                multicastRouterSocket.receive(incomingPacket);
                RoverManager.getInstance().getIpAddressMap().put(
                        Helper.parseSenderAddress(String.valueOf(Integer.parseInt(
                                Helper.BitwiseManager.convertByteToHex(incomingPacket.getData()[2]), 16))), incomingPacket.getAddress().toString().substring(1));
//                System.out.println("mapped: " + RoverManager.getInstance().getIpAddressMap());
//                System.out.println("Packet from : " + incomingPacket.getAddress().toString() + ", SocketAddress: " + incomingPacket.getSocketAddress() + ", Port: " + incomingPacket.getPort());
                // dispatch this client socket to a worker thread
                RoverManager.getInstance().getMyThreadPoolExecutorService().getService().execute(
                        new ParseReceivedPacketProcess(incomingPacket, multicastRouterSocket, mRIPPacket));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private NetworkInterface getInterfaceName()   {
        // get the current ip address
        InetAddress ip = null;
        String hostname;
        try {
            ip = InetAddress.getLocalHost();
            System.out.println("Your current IP  : " + ip);
            System.out.println("Your current IP getAddress : " + ip.getHostAddress());
            System.out.println("Your current IP Hostname : " + ip.getHostName());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        // subscribe to multicast IP address
        Enumeration<NetworkInterface> netifs = null;
        try {
            netifs = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            e.printStackTrace();
        }

        NetworkInterface resultInterface = null;
        // hostname is passed to your method
        InetAddress myAddr = null;
        try {
            myAddr = InetAddress.getByName(ip.getHostName());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        while (netifs.hasMoreElements()) {
            NetworkInterface networkInterface = netifs.nextElement();
            Enumeration<InetAddress> inAddrs = networkInterface.getInetAddresses();
            while (inAddrs.hasMoreElements()) {
                InetAddress inAddr = inAddrs.nextElement();
                if (inAddr.equals(myAddr)) {
                    resultInterface = networkInterface;
                }
            }
        }

        return resultInterface;
    }


    /**
     * A control class that controls the synchronization between sending
     * packets and receiving acknoeledgements at the Datagram socket.
     *
     * This class manages the ackReceived boolean variable which is set
     * when the bytes are received successfully by the receiver and the sender
     * is notified of the same with an ACK.
     */
    class Control {
        // this variable allows the program to send the next few bytes
        // in the packet. Sender sends only if ackReceived is true
        public volatile boolean ackReceived = true;
        public volatile String lastAckSent;
        public volatile int lastAckLength;
    }

    final Control control = new Control();

    /**
     * A thread to send bytes of the given file over the network
     */
    public class ByteSendingProcess extends Thread {
        private String destinationIpString;
        private String filename;
        DatagramSocket byteSendingSocket;
        NewPacket singlePacket;

        /**
         * If filename==null and singleByte!=null this means that this request
         * is coming from an intermediate router
         * @param destinationIpString
         * @param filename
         * @param singlePacket
         */
        public ByteSendingProcess(String destinationIpString, String filename, NewPacket singlePacket) {
            if(filename == null && singlePacket != null)    {
                new PacketForwardingProcess(destinationIpString, singlePacket, false).start();
                System.out.println("ByteSendingProcess(): handing over control to forwarding process");
                return;
            }

            this.singlePacket = singlePacket;
            this.destinationIpString = destinationIpString;
            this.filename = filename;

            // initialise the socket to broadcast
            try {
                this.byteSendingSocket = new DatagramSocket();
//                Log.router(RoverManager.getInstance().getFullRoverId() + ": byte sending socket binded to port : " + byteSendingSocket.getLocalPort());
                Log.router(RoverManager.getInstance().getFullRoverId() + ": Initializing transfer...");
            } catch (IOException e) {
                e.printStackTrace();
                Log.router(RoverManager.getInstance().getFullRoverId() + ": Failed! cannot initialise Datagram socket inside ByteSendingProcess");
            }
        }

        @Override
        public void run()   {
            System.out.println("Creating chunks of file to send...");
            try {
                sleep(8000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("Sending file...");
//            System.out.println("ByteSendingProcess(): entered");
            // prepare the destination address of the packet
            InetAddress destinationInetAddress = null;

            String nextHopIp = getNextHopOfDestinationIp(destinationIpString);
            if(nextHopIp == null)    {
                System.err.println("ByteSendingProcess(): run(): nextHopIp is null");
                System.exit(1);
            }

            try {
                destinationInetAddress = InetAddress.getByName(nextHopIp);
            } catch (UnknownHostException e) {
                e.printStackTrace();
                Log.router(RoverManager.getInstance().getFullRoverId() + ": Unknown host Exception inside BroadcastingProcess");
            }

            FileManager fileManager = null;
            // hook up the file to the file manager
            fileManager = new FileManager(filename);
            if (!fileManager.openFile()) {
                System.out.println("ByteSendingProcess(): FileManager: Something went wrong in opening the file");
                System.exit(1);

            }
//            System.out.println("ByteSendingProcess(): FileManager: file is open for reading");
            // prepare a new packet with some new data each time and send
            String srcAddress = RoverManager.getInstance().getFullRoverId();
            String ack = "0";
            Timer timer = null;
            boolean allBytesSent = false;
            while(!allBytesSent) {
                if(control.ackReceived) {
//                    System.out.println("ByteSendingProcess(): proceedSinceAckReceived : " + control.ackReceived);
                    // cancel the current timer if it is not null
                    if (timer != null) {
                        timer.cancel();
                    }
                    byte nextByte = 0;
                    String endOfData = "0";

                    if (fileManager.hasNextByte()) {
                        nextByte = fileManager.getByte();
                    } else {
                        endOfData = "1";
                        allBytesSent = true;
                    }

                    // create a new packet
                    NewPacket mPacket = new NewPacket(endOfData, Byte.toString(nextByte), ack, srcAddress, destinationIpString);
//                    System.out.println("New packet created : " + mPacket);
                    // put end of data, put data, ack, src and destination address
                    byte[] buff;
                    buff = mPacket.toByteArray();
                    try {
                        DatagramPacket packet = new DatagramPacket(buff, buff.length, destinationInetAddress, FILE_TRANSFER_PORT);
                        byteSendingSocket.send(packet);
                        /*System.out.println("ByteSendingProcess(): packet sent to public address :" + destinationInetAddress.getHostAddress());
                        System.out.println("ByteSendingProcess(): packet sent to internal address :" + mPacket.getDestinationAddress());
                        Syst    em.out.println("ByteSendingProcess(): packet sent from :" + mPacket.getSrcAddress());
                        System.out.println("ByteSendingProcess(): packet endOfData :" + mPacket.getEndOfData());
                        System.out.println("ByteSendingProcess(): packet data :" + mPacket.getData());
                        System.out.println("ByteSendingProcess(): packet ack :" + mPacket.getAck());*/

                        // start a timer fore resending packet
                        timer = new Timer();
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                try {
                                    byteSendingSocket.send(packet);
//                                    System.out.println("ByteSendingProcess(): TimerTask(): packet sent again");
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }, 5000, 5000);

                        control.ackReceived = false;
//                        System.out.println("ByteSendingProcess(): proceedSinceAckReceived set to false");
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.router(RoverManager.getInstance().getFullRoverId() + "ByteSendingProcess(): Something went wrong while sending...");
                        break;
                    }
                }
            }
            if(timer != null)
                timer.cancel();

            if(allBytesSent)    {
                System.out.println("All bytes have been sent successfully");
            }
//            Log.router(RoverManager.getInstance().getFullRoverId() + "ByteSendingProcess(): Stop sending broadcast packets");
        }
    }

    /**
     * A thread to send bytes of the given file over the network
     */
    public class PacketForwardingProcess extends Thread {
        private boolean isAck = false;
        private String destinationIpString;
        private String filename;
        DatagramSocket byteSendingSocket;
        NewPacket singlePacket;

        /**
         * This thread comes into play when the packet in consideration is
         * passing through an intermediate router, in this case the rover
         * just simple forwards the packet to the destination hep count
         * as mentioned in his own routing table
         * @param destinationIpString
         * @param singlePacket
         */
        public PacketForwardingProcess(String destinationIpString, NewPacket singlePacket, boolean isAck) {
            this.isAck = isAck;
            this.singlePacket = singlePacket;
            this.destinationIpString = destinationIpString;
            // initialise the socket to broadcast
            try {
                this.byteSendingSocket = new DatagramSocket();
                Log.router(RoverManager.getInstance().getFullRoverId() + ": byte sending socket binded to port : " + byteSendingSocket.getLocalPort());
            } catch (IOException e) {
                e.printStackTrace();
                Log.router(RoverManager.getInstance().getFullRoverId() + ": Failed! cannot initialise Datagram socket inside PacketForwardingProcess");
            }
        }

        @Override
        public void run()   {
            if(isAck)   System.out.println("forwarding ACK...");
            else    System.out.println("forwarding file chunk...");

            // prepare the destination address of the packet
            InetAddress destinationInetAddress = null;
            String nextHopIp = getNextHopOfDestinationIp(destinationIpString);
            if(nextHopIp == null)    {
                System.err.println("PacketForwardingProcess(): run(): nextHopIp is null: The destination Ip does not exist in my routing table");
                System.exit(1);
            }
            try {
                destinationInetAddress = InetAddress.getByName(nextHopIp);
            } catch (UnknownHostException e) {
                e.printStackTrace();
                Log.router(RoverManager.getInstance().getFullRoverId() + ": Unknown host Exception inside BroadcastingProcess");
            }

//            System.out.println("PacketForwardingProcess(): FileManager: file is open for reading");
            // prepare a new packet with some new data each time and send
            int port = isAck? ACK_PORT : FILE_TRANSFER_PORT;

            boolean allBytesSent = false;
            while(!allBytesSent) {
//                    System.out.println("PacketForwardingProcess(): proceedSinceAckReceived : " + control.ackReceived);
                // create a new packet
                NewPacket mPacket = singlePacket;
//                    System.out.println("New packet created : " + mPacket);
                // put end of data, put data, ack, src and destination address
                byte[] buff;
                buff = mPacket.toByteArray();
                try {
                    DatagramPacket packet = new DatagramPacket(buff, buff.length, destinationInetAddress, port);
                    byteSendingSocket.send(packet);
                    /*System.out.println("PacketForwardingProcess(): packet sent to public address :" + destinationInetAddress.getHostAddress());
                    System.out.println("PacketForwardingProcess(): packet sent to internal address :" + mPacket.getDestinationAddress());
                    System.out.println("PacketForwardingProcess(): packet sent from :" + mPacket.getSrcAddress());
                    System.out.println("PacketForwardingProcess(): packet endOfData :" + mPacket.getEndOfData());
                    System.out.println("PacketForwardingProcess(): packet data :" + mPacket.getData());
                    System.out.println("PacketForwardingProcess(): packet ack :" + mPacket.getAck());*/
                    allBytesSent = true;
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.router(RoverManager.getInstance().getFullRoverId() + "PacketForwardingProcess(): Something went wrong while sending...");
                    break;
                }
            }
            System.out.println("PacketForwardingProcess(): forwarding complete!");
        }
    }

    /**
     * Helper method that skims through the routing table entries and returns
     * the next hop IP address of the fullSenderIpAddress received as argument
     * @param destinationIpString
     * @return
     */
    private String getNextHopOfDestinationIp(String destinationIpString) {
//        System.out.println("getNextHopOfDestinationIp(): entered with destinationIpString: " + destinationIpString);
        RIPPacket mPacket = RoverManager.getInstance().getmRIPPacket();
//        System.out.println("getNextHopOfDestinationIp(): mPacket.getmList().size(): " + mPacket.getmList().size());
        for(RoutingTableEntry myEntry: mPacket.getmList()) {
            String current = myEntry.getAddress();
            if(current.equalsIgnoreCase(destinationIpString))  {
//                System.out.println("getNextHopOfDestinationIp(): match found for : " + destinationIpString);
//                System.out.println("getNextHopOfDestinationIp(): returning nextHop for : " + myEntry.getNextHop() + ", from map : " + RoverManager.getInstance().getIpAddressMap().get(myEntry.getNextHop()));
                return RoverManager.getInstance().getIpAddressMap().get(myEntry.getNextHop());
            }
        }

        return null;
    }


    /**
     * A thread to send ack packet over the network
     */
    public class AckSendingProcess extends Thread {
        private String fullSenderIpAddress;
        private String byteReceived;
        DatagramSocket ackSendingSocket;

        public AckSendingProcess(String fullSenderIpAddress, String byteReceived) {
            this.fullSenderIpAddress = fullSenderIpAddress;
            this.byteReceived = byteReceived;
            // initialise the socket to broadcast
            try {
                this.ackSendingSocket = new DatagramSocket();
//                Log.router(RoverManager.getInstance().getFullRoverId() + ": AckSendingProcess binded to port: " + ackSendingSocket.getLocalPort());
            } catch (IOException e) {
                e.printStackTrace();
                Log.router(RoverManager.getInstance().getFullRoverId() + ": Failed! cannot initialise Datagram socket inside AckSendingProcess");
            }
        }

        @Override
        public void run()   {
//            System.out.println("AckSendingProcess(): entered");
            // prepare the destination address of the packet
            InetAddress destinationInetAddress = null;
            String nextHopIp = getNextHopOfDestinationIp(fullSenderIpAddress);
            if(nextHopIp == null)    {
//                System.err.println("AckSendingProcess(): run(): nextHopIp is null");
                System.exit(1);
            }

            try {
                destinationInetAddress = InetAddress.getByName(nextHopIp);
            } catch (UnknownHostException e) {
                e.printStackTrace();
                Log.router(RoverManager.getInstance().getFullRoverId() + ": Unknown host Exception inside AckSendingProcess");
            }

            // prepare a new packet with some new data each time and send
            String srcAddress = RoverManager.getInstance().getFullRoverId();
            // create a new packet
            NewPacket mPacket = new NewPacket("0", byteReceived, byteReceived, srcAddress, fullSenderIpAddress);
//            System.out.println("New ack packet created : " + mPacket);
            // put end of data, put data, ack, src and destination address
            byte[] buff;
            buff = mPacket.toByteArray();
            try {
                DatagramPacket packet = new DatagramPacket(buff, buff.length, destinationInetAddress, ACK_PORT);
                ackSendingSocket.send(packet);
//                System.out.println("AckSendingProcess(): ack sent back!");
                control.lastAckSent = byteReceived;
              /*  System.out.println("AckSendingProcess(): packet sent to public address :" + destinationInetAddress.getHostAddress());
                System.out.println("AckSendingProcess(): packet sent to :" + mPacket.getDestinationAddress());
                System.out.println("AckSendingProcess(): packet sent from :" + mPacket.getSrcAddress());
                System.out.println("AckSendingProcess(): packet data :" + mPacket.getData());
                System.out.println("AckSendingProcess(): packet ack :" + mPacket.getAck());*/

                if(!ackSendingSocket.isClosed()) {
                    ackSendingSocket.close();
//                    System.out.println("AckSendingProcess(): socket closed since job done");
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.router(RoverManager.getInstance().getFullRoverId() + ": AckSendingProcess(): Something went wrong while sending...");
            }
//            Log.router(RoverManager.getInstance().getFullRoverId() + ": AckSendingProcess(): Stop sending broadcast packets");
        }
    }

    /**
     * A thread to receive ack packets over the network
     */
    public class AckReceivingProcess extends Thread {
        DatagramSocket ackReceivingSocket;

        public AckReceivingProcess() {
            // initialise the socket to broadcast
            try {
                this.ackReceivingSocket = new DatagramSocket(ACK_PORT);
//                Log.router(RoverManager.getInstance().getFullRoverId() + ": AckReceivingProcess(): byte receiving socket binded to port : " + ackReceivingSocket.getLocalPort());
            } catch (IOException e) {
                e.printStackTrace();
                Log.router(RoverManager.getInstance().getFullRoverId() + ": Failed! cannot initialise Datagram socket inside AckReceivingProcess");
            }
        }

        @Override
        public void run()   {
            while (true) {
                try {
                    byte[] buffer = new byte[11];
                    DatagramPacket incomingPacket = new DatagramPacket(buffer, buffer.length);
                    // read the incoming data into the packet
                    ackReceivingSocket.receive(incomingPacket);
//                    System.out.println("Received some ack...");
                    // check if the destination address of the packet equals the current rover's address
                    String fullDestinationAddress = Helper.parseSenderAddress(String.valueOf(Integer.parseInt(
                            Helper.BitwiseManager.convertByteToHex(incomingPacket.getData()[9]), 16)));
                    String fullSenderIpAddress = Helper.parseSenderAddress(String.valueOf(Integer.parseInt(
                            Helper.BitwiseManager.convertByteToHex(incomingPacket.getData()[5]), 16)));
                    String endOfData = String.valueOf(Integer.parseInt(
                            Helper.BitwiseManager.convertByteToHex(incomingPacket.getData()[0]), 16));
                    String data = String.valueOf(Integer.parseInt(
                            Helper.BitwiseManager.convertByteToHex(incomingPacket.getData()[1]), 16));
                    String ack = String.valueOf(Integer.parseInt(
                            Helper.BitwiseManager.convertByteToHex(incomingPacket.getData()[2]), 16));

//                    System.out.println("AckReceivingProcess(): received ack: " + ack);
                    // check if the destination address of the ack packet is this current rover
                    if(fullDestinationAddress.equalsIgnoreCase(RoverManager.getInstance().getFullRoverId()))    {
                        // cool, this ack is mine reflect this ack in the control class
//                        System.out.println("AckReceivingProcess(): received destinationAddress: " + fullDestinationAddress + " : destined to reach me");
                    }else   {
                        // ack is not mine, just forward
                        new PacketForwardingProcess(fullDestinationAddress, new NewPacket(endOfData, data, ack, fullSenderIpAddress, fullDestinationAddress), true);
//                        System.out.println("AckReceivingProcess(): received destinationAddress: " + fullDestinationAddress + " : not sent for me");
                    }

                    // avoid processing packet if the sender is myself
                    if(fullSenderIpAddress.equalsIgnoreCase(RoverManager.getInstance().getFullRoverId()))    {
//                        System.out.println("AckReceivingProcess(): received fullSenderIpAddress : " + fullSenderIpAddress + ", this is me, skipping");
                        continue;
                    }else   {
//                        System.out.println("AckReceivingProcess(): received fullSenderIpAddress : " + fullSenderIpAddress + ", ack sent by a new sender");
                    }

                    // trigger a successful ACK
                    control.ackReceived = true;

                } catch (IOException ex) {
                    ex.printStackTrace();
                    break;
                }
            }
//            Log.router(RoverManager.getInstance().getFullRoverId() + ": AckReceivingProcess(): Stop sending broadcast packets");
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
