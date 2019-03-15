/**
 * qClientUdp.java
 *
 * @version:
 *      1.0.1
 *
 * @revision:
 *      1
 *
 * @author:
 *      ishanguliani aka ig5859
 *      saurabhgaikwad
 */

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;

/**
 * Multi client implementation of a client-server architecture
 * that communicates by exchanging quotes from the server using
 * {@link DatagramPacket}
 */
public class qClientUdp {

    static HashMap<String, String> arguments = null;

    public static void main(String[] args)  {

        if( args.length <= 1 )  {
            Log.client("Please enter arguments as: java qClientUdp -port <portNumber of the server> -server <hostname>");
            System.exit(0);
        }

        arguments = new HashMap<>();
        // set the parameters from the command line args
        arguments.put(args[0], args[1]);
        arguments.put(args[2], args[3]);
        new ClientTaskUdp(arguments).start();
    }
}

/**
 * Each client sends a trigger to the server socket to receive a
 * quote. The server returns a {@link DatagramPacket} that contains
 * either a new unique quote or the server timestamp if there are
 * no more quotes available to be returned.
 */
class ClientTaskUdp extends Thread {

    private DatagramSocket clientSocket;
    public static HashMap<String, String> mArguments = null;

    public ClientTaskUdp(HashMap<String, String> arguments) {
        mArguments = arguments;
        try {
            // create a datagram socket
            clientSocket = new DatagramSocket();
        } catch (IOException ex) {
            Log.client("Problem in opening a client socket...");
            ex.printStackTrace();
            System.exit(0);
        }
    }


    /**
     * Client sends a ping to the server over TCP/IP
     */
    @Override
    public void run() {
        try {

            // get the IP address and the port number of the server
            InetAddress serverAddress = InetAddress.getByName(mArguments.get("-server"));
            int serverPort = Integer.parseInt(mArguments.get("-port"));

            // bind the address and the port number to the packet
            byte[] buffer = new byte[256];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, serverAddress, serverPort);
            // just send an empty ping to the server
            clientSocket.send(packet);


            byte[] receptionBuffer = new byte[256];
            DatagramPacket incomingPacket = new DatagramPacket(receptionBuffer, receptionBuffer.length);
            clientSocket.receive(incomingPacket);

            // do something with the received data
            // get the quote from the packet
            String theQuote = new String(incomingPacket.getData(), 0, incomingPacket.getLength());
            // display it on the screen
            Log.client("Quote: " + theQuote);


        } catch (UnknownHostException ex) {
            Log.client("unkknown host ");
            ex.printStackTrace();
            System.exit(0);
        }catch (IOException ex) {
            Log.client("cannot read from client UDP");
            ex.printStackTrace();
            System.exit(0);
        }finally{
            clientSocket.close();
            Log.client(" Closed client socket connection");
        }
    }

}