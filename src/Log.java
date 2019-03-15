/**
 * Log.java
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

/**
 * A helper API to effectively lof Client and server events as needed.
 */
public class Log {
    public static void router(String message) {
        System.out.println( "Router: " + message );
    }

    public static void client(String message) {
        System.out.println( "Client: " + message );
    }

    public static void network(String s)  {
        System.out.println( "Network: " + s );
    }
}
