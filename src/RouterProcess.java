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

import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * A running Router Process
 * 1. Listens to multicasts for incoming routing tables
 * 2. Broadcasts its routing table to neighbors
 * 3. Keeps a record of interactions from neighbors as timeout
 */
public class RouterProcess {

    public static void main(String[] args) {
        try {
            String multicastIp = args[0];
            String id = args[1];
            String port = args[2];
            new MainRouterThread(id, multicastIp, port).start();
        }catch(ArrayIndexOutOfBoundsException ex){
            System.out.println("Please enter arguments as <multicast IP> <router id> <port>");
        }
    }
}

/**
 * The main server logic that listens to client
 * requests on a port number and serves them as needed
 */
class MainRouterThread extends Thread{

    // the routing table
    private RIPPacket mRIPPacket = new RIPPacket();
    //    private ServerSocket routerSocket;
    private String id;
    private String multicastIp;
    private String port;
    //    private static final String MULTICAST_IP = "224.0.0.9";
    private static int ROUTER_PORT;
    private MulticastSocket routerSocket;
    ExecutorService service;

    /**
     * Constructor opens a server socket and listens
     */
    public MainRouterThread(String id, String multicastIp, String port)   {

        this.id = id;
        this.multicastIp = multicastIp;
        ROUTER_PORT = Integer.parseInt(port);
        // initialise the executor service to handle thread effectively
        service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        try {
            // fire up the router listening port by subscribing to a multi cast IP
            // this port only listens to incoming broadcasts and then assigns the processing to
            // a worker thread
            this.routerSocket = new MulticastSocket(ROUTER_PORT);
            // subscribe to multicast IP address
            this.routerSocket.joinGroup(InetAddress.getByName(multicastIp));
            Log.router(this.id + ": Router is now running on port: " + ROUTER_PORT);
            // start broadcasting routing table updates
            startBroadcastingProcess();
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
                byte[] buffer = new byte[1000];
                DatagramPacket incomingPacket = new DatagramPacket(buffer, buffer.length);
                // read the incoming data into the packet
                routerSocket.receive(incomingPacket);
//                Log.router(this.id + ": Connection established with a router...");
                // dispatch this client socket to a worker thread
                service.execute(new ClientWorkerThread(incomingPacket, routerSocket, id));
            }catch(IOException ex){
                ex.printStackTrace();
            }
        }
    }

    private void startBroadcastingProcess() {
        new BroadcastingProcess().start();
    }

    public class BroadcastingProcess extends Thread {
        DatagramSocket routingSocket;

        public BroadcastingProcess() {
            // initialise the socket to broadcast
            try {
                this.routingSocket = new DatagramSocket();
                Log.router(id + ": Broadcasting datagram socket is now open!");
            } catch (IOException e) {
                e.printStackTrace();
                Log.router(id + ": Failed! cannot initialise Datagram socket inside BroadcastingProcess");
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
                Log.router(id + ": Unknown host Exception inside BroadcastingProcess");
            }

            // prepare the destination port
            Integer destinationPort = ROUTER_PORT;

            // prepare the payload (byte buffer)
            ByteArrayOutputStream out = null;
            ObjectOutputStream objectOut = null;

            while(true) {
                try {
                    out = new ByteArrayOutputStream();
                    objectOut = new ObjectOutputStream(out);
                    // serialize my id
                    objectOut.writeObject(id);
                    // serialize my routing table
                    objectOut.writeObject(mRIPPacket);
                    // create the packet
                    DatagramPacket packet = new DatagramPacket(out.toByteArray(), out.toByteArray().length, group, destinationPort);
                    routingSocket.send(packet);
                    Log.router(id + ": The packet has been sent with payload: " + packet.getAddress().toString() + ", " + packet.getPort());
                    // close this stream
                    objectOut.close();
                    sleep(5000);
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Log.router(id + ": Something went wrong while sleeping");
                }
            }

            Log.router(id + ": Stop sending broadcast packets");
        }
    }
}


/**
 * The thread which serves a single client
 */
class ClientWorkerThread extends Thread {
    private DatagramPacket clientPacket;
    private DatagramSocket clientSocket;
    private String id;

    public ClientWorkerThread(DatagramPacket clientPacket, DatagramSocket clientSocket, String id) {
        this.clientPacket = clientPacket;
        this.clientSocket = clientSocket;
        this.id = id;
    }

    @Override
    public void run()   {
        try{
            // extract the IP address and the port number of the
            // client from the packet
            InetAddress inetAddress = clientPacket.getAddress();
            int portNumber = clientPacket.getPort();

            // extract data from the packet
            ByteArrayInputStream in = new ByteArrayInputStream(clientPacket.getData());
            ObjectInputStream objectIn = new ObjectInputStream(in);

//            while(true) {
            String mId = null;
            RIPPacket mRIPPacket = null;
//            mId = (String)objectIn.readObject();
//            Log.router(id + ": mId: " + mId);
//
//            mRIPPacket = (RoutingTable) objectIn.readObject();
//            Log.router(id + ": mRIPPacket: " + mRIPPacket.getmList());

            if(((mId = objectIn.readObject().toString()) != null ) && (mRIPPacket = (RIPPacket) objectIn.readObject()) != null)   {
                // return if the ID matches the current router id
                if(mId.equalsIgnoreCase(id))    {
                    return;
                }
                Log.router(id + ": Received: " + " mId: " + mId + ", RIPPacket: Command: " +  mRIPPacket.getCommand() + ", Version: " + mRIPPacket.getVersion() + ", MustBeZero: " + mRIPPacket.getMustBeZero() + ", " + "mRIPPacket.getmList() + " + mRIPPacket.getmList() + ", inetAddress: " + inetAddress.toString() + ", port: " + portNumber);
            }else   {
                Log.router(id + ": Corrupt id and routing table received");
            }

//            }
            // get a new quote
//            String newQuote = FileManager.getInstance().getNextQuote();
//            String newQuote = NetworkManager.getInstance().getNextQuote();
            // send the new quote to the client
//            DatagramPacket packetToSend = new DatagramPacket(newQuote.getBytes(), newQuote.getBytes().length, inetAddress, portNumber);
            // dispatch now
//            clientSocket.send(packetToSend);
//            System.out.println(id + " TALKING BACK TO THE ROUTER");
        } catch (Exception ex) {
            Log.router(this.id + ": There was some problem reading data from the client");
            ex.printStackTrace();
        }
    }
}
