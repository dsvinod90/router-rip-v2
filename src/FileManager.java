/**
 * FileManager.java
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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;

/**
 * The FileManager API takes care of all file operations as needed.
 * It opens up the quote file when instantiated and also provides
 * a getNextQuote() method to return the next available quote to the
 * caller in realtime.
 *
 * This is a singleton implementation of the FileManager since only
 * one shared file instance is to be used across multiple clients.
 */
public class FileManager {

    BufferedReader bufferedReader;
    public static final String FILENAME = "somenewfile";

    private static FileManager instance = null;

    /**
     * The singleton helper. Lazily loads FileManager instance
     * @return
     */
    public static FileManager getInstance() {
        if( instance == null)   {
            instance = new FileManager();
        }

        return instance;
    }

    /**
     * Open the file for reading in the constructor
     */
    public FileManager()    {
        try {
            bufferedReader = new BufferedReader(new FileReader(FILENAME));
        } catch (FileNotFoundException e) {
            Log.router("The file for reading quotes is not available");
            e.printStackTrace();
            System.exit(0);
        }
    }

    /**
     * Return the next quote from the linked file
     * @return
     */
    public String getNextQuote()    {
        String nextQuote = "";
        try {
            nextQuote = bufferedReader.readLine();
            Log.router("I am returning : " + (nextQuote != null ? nextQuote : new Date().toString()));
            return nextQuote != null ? nextQuote : new Date().toString();
        } catch (IOException e) {
            Log.router("File error: Cannot readline from file");
            e.printStackTrace();
            nextQuote = new Date().toString();
        }


        Log.router("I am returning : " + nextQuote);
        return new Date().toString();
    }
}
