import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class Tema2 {
    public static String orders;
    public static String products;
    private static Integer P;
    // save the indexes from where each thread should read
    public static ConcurrentHashMap<Integer, ArrayList<Long>> threadToBytes;
    // count how many active orders each thread has
    public static ConcurrentHashMap<Integer, AtomicInteger> threadToOrdersNumber;
    // count how many objects each order has
    public static ConcurrentHashMap<String, Integer> orderToObjects;
    public static AtomicInteger level1ThreadsActive;
    public static long orderLength;
    public static long productsLength;
    public static PrintWriter ordersOutFile;
    public static PrintWriter orderProductsOutFile;

    /**
     * function that extracts input files and number
     * of threads from the command line arguments
     * @param args command line args
     */
    public static void extractParams(String[] args) {
        String dir = args[0];
        orders = dir + "/orders.txt";
        products = dir + "/order_products.txt";
        P = Integer.valueOf(args[1]);
    }

    /**
     * function computes for each level 1 thread
     * the beginning and ending byte to read from
     * (will be updated if they don't exactly match
     * with the beginning of a new line)
     */
    public static void computeBytes() throws IOException {
        {
            RandomAccessFile file = new RandomAccessFile(Tema2.orders, "r");
            orderLength = file.length();
            file.close();
        }

        {
            RandomAccessFile file = new RandomAccessFile(Tema2.products, "r");
            productsLength = file.length();
            file.close();
        }

        long threadLength = orderLength / P;
        threadToBytes = new ConcurrentHashMap<>();
        for (int i = 0; i < P; i++) {
            threadToBytes.put(i, new ArrayList<Long>
                    (Arrays.<Long>asList(threadLength * i,
                            threadLength * (i + 1))));
        }
    }

    public static void initialise() throws IOException {
        level1ThreadsActive = new AtomicInteger(P);
        orderToObjects = new ConcurrentHashMap<>();
        threadToOrdersNumber = new ConcurrentHashMap<>();
        for (int i = 0; i < P; i++) {
            threadToOrdersNumber.put(i, new AtomicInteger(0));
        }

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