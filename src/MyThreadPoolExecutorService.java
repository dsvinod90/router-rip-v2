/**
 * {@link MyThreadPoolExecutorService}
 *
 * @version:
 *      1.0.2
 *
 * @revision:
 *      4
 *
 * @author:
 *      ishanguliani aka ig5859
 */

/**
 * An API to effectively manage all thread synchronization across
 * the whole project. Each thread leverages a shared instance of
 * {@link MyThreadPoolExecutorService} to execute
 */

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MyThreadPoolExecutorService {
    private static MyThreadPoolExecutorService myThreadPoolExecutorService = null;
    private static ExecutorService service;

    public static MyThreadPoolExecutorService getInstance() {
        if(myThreadPoolExecutorService == null) {
            myThreadPoolExecutorService = new MyThreadPoolExecutorService();
        }
        return myThreadPoolExecutorService;
    }

    public MyThreadPoolExecutorService() {
        service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    public static ExecutorService getService() {
        return service;
    }
}
