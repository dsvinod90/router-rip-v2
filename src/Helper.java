import java.net.InetAddress;
import java.net.UnknownHostException;
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

public class Helper {

    public static void print(String message)    {
        System.out.print(message);
    }

    public static class BitwiseManager {

        /**
         * Helper method that uses bitwise operators to perform
         * manipulation to convert a byte to its hexadecimal representation
         * @param b the input byte
         * @param base the base of the returned hex string
         * @return  the converted hex
         */
        public static String convertByteToHex(byte b, int base) {
            StringBuilder fullHex = new StringBuilder();
            if(base == 16) {
                // logical right shift bits by 12 places
                // this allows us to obtain the most significant
                // half of the corresponding byte
                int hex = (b >>> 12) & 0x000F;
                int shift = 8;
                for (int i = 0; i < 4; i++) {
                    if (hex <= 9) fullHex.append((char) ('0' + hex));
                    else fullHex.append((char) ('a' + (hex - 10)));
                    // update the hex to consider the least significant
                    // half of the remaining byte
                    hex = (b >>> shift) & 0x000F;
                    shift -= 4;
                }
            }else   {
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
            }
            return fullHex.toString();
        }

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
         * Return the Integer equivalent of the given hex string
         * @param hex
         * @return
         */
        public static Integer convertHexToInteger(String hex)   {
            return Integer.parseInt(hex.toString(), 16);
        }
        /**
         * Return decimal equivalent of the given byte of data
         * @param b the byte to be processed using bitwise operations
         * @return  the equivalent decimal value
         */
        public static int convertByteToDecimal(byte b)   {
            int decimal = b & 0xFF;
            return decimal;
        }

        /**
         * Return the twos complement of the given byte
         * @param b the byte to be processed using bitwise operations
         * @return  the equivalent 2s complement
         */
        public static byte performTwosComplement(int b)   {
            return (byte)((b & 127) - (b & ~127));
        }


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

 /*   public static String parseCIDR(String ip, String subnet)    {
        String[] s = subnet.split("(\\.)");
        for(String el: s)   {
            System.out.println(el + ",");
        }
        int classA = Integer.parseInt(s[0]);
        int classB = Integer.parseInt(s[1]);
        int classC = Integer.parseInt(s[2]);
        int classD = Integer.parseInt(s[3]);

        System.out.println("classA: " + classA + ", classB: " + classB + ", classC: " + classC + ", classD: " + classD);
        int howManyBytes = 0:
        if(classA == 255) howManyBytes++;
        if(classB == 255) howManyBytes++;
        if(classC == 255) howManyBytes++;
        if(classA == 255) howManyBytes++;

        double noOfHostSubnets = (howManyBytes*8) + (8-Math.log(classD));
        return ip + "/" + noOfHostSubnets;
    }
*/
    public static String convertNetmaskToCIDR(String ip, String netmaskString){

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