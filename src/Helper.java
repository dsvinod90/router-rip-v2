/**
 * filename: @{@link helper.Helper}
 *
 * author: @ishanguliani aka ig5859
 *
 * version:     1
 *
 * revision:    1
 *
 * description:
 * A helper class to provide frequently utilised bitwise operations like
 * converting a byte to hex code or converting a byte to decimal
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
         * @return  the converted hex
         */
        public static String convertByteToHex(byte b)    {
            StringBuilder fullHex = new StringBuilder();

            // logical right shift bits by 4 places
            // this allows us to obtain the most significant
            // half of the corresponding byte
            int hex = (b >>> 4) & 0x0F;

            for( int i = 0; i < 2; i++) {
                if(hex <= 9)
                    fullHex.append((char)('0' + hex));
                else
                    fullHex.append((char)('a' + (hex-10)));

                // update the hex to consider the least significant
                // half of the corresponding byte
                hex = b & 0x0F;
            }
            return fullHex.toString();
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

    }


}