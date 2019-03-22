/**
 * filename: @{@link Helper}
 *
 * author: @ishanguliani aka ig5859
 *
 * version:     1.0.1
 *
 * revision:    3
 *
 * description:
 * A helper class to provide
 * 1. frequently utilised bitwise operations like converting a byte to hex code or converting a byte to decimal
 * 2. Helper functions to parse CIDR addressing
 */

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Helper {

    public static String parseNetworkAsIpAddress(String nextHop) {
        return RoverManager.getInstance().getIpAddressMap().getOrDefault(nextHop, nextHop);
    }

    public static class BitwiseManager {
        /**
         * Helper method that uses bitwise operators to perform
         * manipulation to convert a byte to its hexadecimal representation
         * @param b the input byte
         * @return  the converted hex
         */
        public static String convertByteToHex(byte b) {
            StringBuilder fullHex = new StringBuilder();
            // logical right shift bits by 4 places
            // this allows us to obtain the most significant
            // half of the corresponding byte
            int hex = (b >>> 4) & 0x0F;
            for( int i = 0; i < 2; i++) {
                if(hex <= 9)    fullHex.append((char)('0' + hex));
                else    fullHex.append((char)('a' + (hex-10)));
                // update the hex to consider the least significant
                // half of the corresponding byte
                hex = b & 0x0F;

            }
            return fullHex.toString();
        }

        /***

         /**
         * Return the byte equivalent of a given decimal (string)
         * @param s the string
         * @return
         */
        public static byte convertIntegerToByte(String s)  {
            return (byte)Integer.parseInt(s, 10);
        }
    }

    /**
     * Parse sender address
     */
    public static String parseSenderAddress(String sender)  {
        return "10.0." + sender + ".0";
    }

    /**
     * Return the CIDR equivalent of the given subnet mask. This
     * function returns an IP address in the CIDR format (XX.X.X.Y/00)
     * @param ip            the ip address to be represented in CIDR
     * @param netmaskString the subnet mask to be parsed
     * @return              the CIDR representation of the ip address
     */
    public static String parseSubnetMaskToCIDR(String ip, String netmaskString){

        InetAddress netmask = null;
        try {
            netmask = InetAddress.getByName(netmaskString);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        byte[] netmaskBytes = new byte[0];
        if (netmask != null) {
            netmaskBytes = netmask.getAddress();
        }else   {
            System.out.println("Netmask looks invalid");
        }

        int cidr = 0;
        boolean zero = false;
        for(byte b : netmaskBytes){
            int mask = 0x80;

            for(int i = 0; i < 8; i++){
                int result = b & mask;
                if(result == 0){
                    zero = true;
                }else if(zero){
                    throw new IllegalArgumentException("Invalid netmask.");
                } else {
                    cidr++;
                }
                mask >>>= 1;
            }
        }
        return ip + "/" + cidr;
    }
}