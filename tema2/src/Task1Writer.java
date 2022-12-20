import java.io.IOException;
import java.util.concurrent.ExecutorService;

public class Task1Writer implements Runnable{
    private final ExecutorService pool1;
    private final ExecutorService pool2;
    private final String orderId;

    public Task1Writer(ExecutorService pool1, ExecutorService pool2, String orderId) {
        this.pool1 = pool1;
        this.pool2 = pool2;
        this.orderId = orderId;
    }

    /**
     * function that writes to order output file
     * when all order's products have been shipped
     */
    private void writeOutput() throws IOException {
        synchronized (Tema2.ordersOutFile) {
            Tema2.ordersOutFile.print(orderId + Constants.separator + Tema2.orderToObjectsClone.get(orderId) + Constants.outputFileEnding);
        }
    }

    /**
     * function that marks an order as closed
     * Last order also closes the pools
     */
    private void closeThread() throws IOException {
        Tema2.ordersInProgress.getAndDecrement();

        // check if needed to close pools
        if (Tema2.ordersInProgress.get() == 0) {
            pool2.shutdown();
            pool1.shutdown();
            Tema2.orderProductsOutFile.close();
            Tema2.ordersOutFile.close();
        }
    }

    @Override
    public void run() {
        try {
            writeOutput();
            closeThread();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
