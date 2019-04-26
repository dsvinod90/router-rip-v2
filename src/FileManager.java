import java.io.*;

public class FileManager {

    private String filename;
    private BufferedInputStream reader;
    private int byteCounter;
    private int byteLimit;
    private byte[] mByteArray;

    /**
     * The constructor to bind a filename to this class
     * @param filename
     */
    public FileManager(String filename) {
        this.filename = filename;
    }

    /**
     * Get a buffered input stream reader to the current filename
     * @return
     */
    public boolean openFile() {
        boolean openSuccess = true;

        try {
            reader = new BufferedInputStream(new FileInputStream(this.filename));
        } catch (FileNotFoundException e) {
            openSuccess = false;
            e.printStackTrace();
            System.err.println("cannot open file input stream");
        }

        try {
            this.byteCounter = -1;
            this.byteLimit = reader.available();
            System.out.println("byteLimit set to " + byteLimit);
        } catch (IOException e) {
            openSuccess = false;
            e.printStackTrace();
            System.err.println("problem reading available number of bytes into byte counter");
        }

        // now since the reader is ready to read bytes, lets read them all
        // in the byte array mByteArray
        int readResult = -1;
        mByteArray = new byte[0];
        try {
            System.out.println("mByteArray(): available bytes: " + reader.available());
            mByteArray = new byte[reader.available()];
        } catch (IOException e) {
            openSuccess = false;
            e.printStackTrace();
            System.err.println("reader has no bytes available");;
            System.exit(1);
        }

        try {
            if((readResult = reader.read(mByteArray, 0, mByteArray.length)) != -1)    {
                System.out.println("read something!\n");
                for(int i = 0; i < mByteArray.length; i++) {
                    System.out.print(mByteArray[i] + ", ");
                }
            }
        } catch (IOException e) {
            openSuccess = false;
            e.printStackTrace();
            System.err.println("cannot read from reader");
        }

        System.out.println("\nmByteArray(): finished and ready");

        return openSuccess;
    }

    /**
     * Check if the next byte is available
     * @return  True if yes
     */
    public boolean hasNextByte() {
//        System.out.println("\nhasNextByte(): entered");
        boolean hasNextByte;
        // increment the byte counter by 1
        this.byteCounter++;
        // check if it still lies within the byte limit of the file
        hasNextByte = byteCounter < byteLimit;
//        System.out.println("hasNextByte(): returning " + hasNextByte);
        return hasNextByte;
    }

    /**
     * Get the next byte
     * @return  the next byte
     */
    public byte getByte()    {
        return mByteArray[byteCounter];
    }

    /**
     * The driver class to test the FileManager
     * @param args
     */
    public static void main(String[] args) {
        FileManager fileManager = new FileManager("output.txt");
        if(fileManager.openFile()) {
            System.out.println();
            while(fileManager.hasNextByte())    {
                System.out.print("next byte: " + fileManager.getByte() + "\n");
            }
        }
    }
}