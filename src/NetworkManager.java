/**
 * NetworkManager.java
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.Date;

/**
 * The NetworkManager API takes care of all file/network operations as needed.
 * It opens up a connection to the quotes file when instantiated and also provides
 * a getNextQuote() method to return the next available quote to the
 * caller in realtime.
 *
 * This is a singleton implementation of the FileManager since only
 * one shared file instance is to be used across multiple clients.
 *
 * The file endpoint is hosted on github.com
 */
public class NetworkManager {

    private BufferedReader bufferedReader;
    private static final String ENDPOINT = "https://ishanguliani.github.io/";
    private static NetworkManager instance = null;

    /**
     * The singleton helper. Lazily loads FileManager instance
     * @return
     */
    public static NetworkManager getInstance() {
        if( instance == null)   {
            instance = new NetworkManager();
        }

        return instance;
    }

    /**
     * Open the file for reading in the constructor
     */
    public NetworkManager()    {
        // establish connection to the network endpoint
        try {
            URI mURI = new URI(ENDPOINT);
            URL mURL = mURI.toURL();

            // open connection
            URLConnection connection = mURL.openConnection();

            // get an input stream from the connection
            this.bufferedReader =  new BufferedReader(new InputStreamReader(connection.getInputStream()));

            Log.network("Connection established to endpoint: " + mURL.toString());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Return the next quote from the linked file
     * @return
     */
    public String getNextQuote()    {
        String inputLine = "";
        try {
            if( (inputLine = bufferedReader.readLine()) != null) {
                // print the line
                Log.network("Reading: " + inputLine);
            }else   {
                inputLine = new Date().toString();
            }
        }catch(IOException ex)  {
            ex.printStackTrace();
        }

        Log.router("I read : " + inputLine);
        return inputLine;
    }
}
