import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class Tema2 {
    public static String orders;
    public static String products;
    public static Integer P;
    // save the indexes from where each thread should read
    public static ConcurrentHashMap<Integer, ArrayList<Long>> threadToBytes;
    // count how many objects each order has
    public static ConcurrentHashMap<String, Integer> orderToObjects;
    public static ConcurrentHashMap<String, Integer> orderToObjectsClone;
    // count how many orders are being processed
    public static AtomicInteger ordersInProgress;
    // file lengths
    public static long orderFileLength;
    public static long productsFileLength;
    // output printers
    public static PrintWriter ordersOutFile;
    public static PrintWriter orderProductsOutFile;

    /**
     * function that extracts input files and number
     * of threads from the command line arguments
     * @param args command line args
     */
    public static void extractParams(String[] args) {
        String dir = args[Constants.directoryPosition];
        orders = dir + Constants.ordersFileName;
        products = dir + Constants.orderProductsFileName;
        P = Integer.valueOf(args[Constants.threadsPosition]);
    }

    /**
     * function that computes for each level 1 thread
     * the beginning and ending byte to read from
     * (will be updated if they don't exactly match
     * with the beginning of a new line)
     */
    public static void computeBytes() {
        // get input file lengths
        {
            File file = new File(Tema2.orders);
            orderFileLength = file.length();
        }

        {
            File file = new File(Tema2.products);
            productsFileLength = file.length();
        }

        // compute each thread's read size
        long threadLength = orderFileLength / P;
        threadToBytes = new ConcurrentHashMap<>();

        // assigns rough portions of the input file for each tread
        for (int i = 0; i < P; i++) {
            threadToBytes.put(i, new ArrayList<>
                    (Arrays.asList(threadLength * i,
                            threadLength * (i + 1))));
        }
    }

    /**
     * function that initialises the data structures
     */
    public static void initialise() throws IOException {
        ordersInProgress = new AtomicInteger(0);
        orderToObjects = new ConcurrentHashMap<>();
        orderToObjectsClone = new ConcurrentHashMap<>();

        ordersOutFile = new PrintWriter(new FileWriter(Constants.orders_out));
        orderProductsOutFile = new PrintWriter(new FileWriter(Constants.order_products_out));

        // compute each level 1 thread's area of work
        computeBytes();
    }

    public static void main(String[] args) {
        // check for correct usage
        if (args.length < Constants.paramNr) {
            throw new RuntimeException(Constants.UsageError);
        }
        // extract command line args
        extractParams(args);

        try {
            initialise();
            // at most P level1 threads will assign new tasks
            ExecutorService level1Pool = Executors.newFixedThreadPool(P);
            // at most P level2 threads will solve those tasks
            ExecutorService level2Pool = Executors.newFixedThreadPool(P);

            for (int i = 0; i < P; i++) {
                level1Pool.submit(new Task1(i, level1Pool, level2Pool));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}