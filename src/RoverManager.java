/**
 * RoverManager.java
 *
 * @version:
 *      1.0.1
 *
 * @revision:
 *      1
 *
 * @author:
 *      ishanguliani aka ig5859
 */

import java.util.ArrayList;
import java.util.HashMap;

/**
 * The Manager class responsible for driving the core functions
 * of the Rover like -
 * 1. Thread lifecycle management
 * 2. Routing Table management
 */

public class RoverManager {
    private String roverId;
    private MyThreadPoolExecutorService myThreadPoolExecutorService;
    private TimeoutManagementProcess timeoutManagementProcess;
    private RIPPacket mRIPPacket;
    private static RoverManager roverManager = null;
    private HashMap<String, String> IpAddressMap = new HashMap<>();
    private HashMap<String, ArrayList<Byte>> connectedHostsMap = new HashMap<>();

    public static RoverManager getInstance()    {
        if(roverManager == null)    {
            roverManager = new RoverManager();
        }
        return roverManager;
    }

    public RoverManager() {
        this.mRIPPacket = new RIPPacket();
        this.timeoutManagementProcess = new TimeoutManagementProcess();
        // initialise the executor service to handle thread effectively
        this.myThreadPoolExecutorService = MyThreadPoolExecutorService.getInstance();
    }

    public HashMap<String, String> getIpAddressMap() {
        return IpAddressMap;
    }

    public HashMap<String, ArrayList<Byte>> getConnectedHostsMap() {
        return connectedHostsMap;
    }

    public String getRoverId() {
        return roverId;
    }

    public void setRoverId(String roverId) {
        this.roverId = roverId;
//        Log.router("RoverManager: " + "rover id is set to: " + this.roverId);
    }

    public String getFullRoverId() {
        return "10.0." + roverId + ".0";
    }

    public RIPPacket getmRIPPacket() {
        return mRIPPacket;
    }

    public MyThreadPoolExecutorService getMyThreadPoolExecutorService() {
        return myThreadPoolExecutorService;
    }

    public TimeoutManagementProcess getTimeoutManagementProcess() {
        return timeoutManagementProcess;
    }
}
