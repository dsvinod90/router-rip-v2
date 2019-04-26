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
 * 1.   Maintain a POJO model for RIP Packet and wrap all corresponding operations
 * 2.   Mark a given router as UNREACHABLE/DEAD when told to do so by the running {@link RouterProcess}
 * 3.   Print the current routing table when told to do so by the running {@link RouterProcess}
 */
import java.util.List;

public class NewPacket {

    public static final String END_OF_DATA_SET = "1";
    public static final String END_OF_DATA_NOT_SET = "0";
    private String endOfData;
    private String data;
    private String ack;
    private String srcAddress;
    private String destinationAddress;

    public NewPacket() {
        this.endOfData = "0";
        this.data = "10";
        this.ack = "1";
        this.srcAddress = "10.0.1.0";
        this.destinationAddress = "10.0.99.0";
    }

    public NewPacket(String endOfData, String data, String ack, String srcAddress, String destinationAddress) {
        this.endOfData = endOfData;
        this.data = data;
        this.ack = ack;
        this.srcAddress = srcAddress;
        this.destinationAddress = destinationAddress;
    }

    public String getEndOfData() {
        return endOfData;
    }

    public String getData() {
        return data;
    }

    public String getAck() {
        return ack;
    }

    public String getSrcAddress() {
        return srcAddress;
    }

    public String getDestinationAddress() {
        return destinationAddress;
    }

    @Override
    public String toString() {
        return "NewPacket{" +
                "endOfData='" + endOfData + '\'' +
                ", data='" + data + '\'' +
                ", ack='" + ack + '\'' +
                ", srcAddress='" + srcAddress + '\'' +
                ", destinationAddress='" + destinationAddress + '\'' +
                '}';
    }

    /**
     * Print the routing table
     */
    public void print() {

    }

    /**
     * Return the byte array equivalent of full RIP packet.
     * This method includes bit manipulations performed on various fields of
     * the packet
     * @return
     */
    public byte[] toByteArray() {
        byte[] arr = new byte[11];
        // loop over each header
        int i = 0;
        arr[i++] = (byte)Integer.parseInt(endOfData);
        System.out.println("endOfData: " + endOfData + ": " + arr[i-1]);
        arr[i++] = (byte)Integer.parseInt(data);
        System.out.println("data: " + data + ": " + arr[i-1]);
        arr[i++] = (byte)Integer.parseInt(ack);
        System.out.println("ack: " + ack + ": " + arr[i-1]);

        // add src address
        String IpAddress = srcAddress;
        // split the ip address by '.' and append 4 bytes to the byte array
        String[] s = IpAddress.split("\\.");
        for(int x = 0; x < 4; x++)  {
            arr[i++] = Helper.BitwiseManager.convertIntegerToByte(s[x]);
        }

        System.out.println("srcAddress: " + srcAddress + ": " + arr[i-4] + "." + arr[i-3] + "." + arr[i-2] + "." + arr[i-1]);

        // add destination address
        String destIpAddress = destinationAddress;
        // split the ip address by '.' and append 4 bytes to the byte array
        String[] destinationString = destIpAddress.split("\\.");
        for(int x = 0; x < 4; x++)  {
            arr[i++] = Helper.BitwiseManager.convertIntegerToByte(destinationString[x]);
        }
        System.out.println("destAddress: " + destinationAddress+ ": " + arr[i-4] + "." + arr[i-3] + "." + arr[i-2] + "." + arr[i-1]);
        // return the byte array
        return arr;
    }

    /***
     * Just a test method for performing modular testing
     * @param args
     */
    public static void main(String[] args) {
        new NewPacket().toByteArray();
    }
}
