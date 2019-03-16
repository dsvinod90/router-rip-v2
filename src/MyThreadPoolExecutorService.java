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
