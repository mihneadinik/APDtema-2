import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;

public class Task2 implements Runnable{
    private final String orderId;
    private final Semaphore sem;
    private final ExecutorService pool;
    private final long startPos;
    private RandomAccessFile file;

    public Task2(String orderId, Semaphore sem, ExecutorService pool, long startPos) throws FileNotFoundException {
        this.orderId = orderId;
        this.sem = sem;
        this.startPos = startPos;
        this.pool = pool;
        this.file = new RandomAccessFile(Tema2.products, "r");
    }

    private String[] parseLine(String line) {
        return line.split(",");
    }

    private void findOrder() throws IOException {
        file.seek(startPos);

        while(file.getFilePointer() < Tema2.productsLength) {
            String[] elems = parseLine(file.readLine());
            String lineOrderId = elems[0];
            String lineProductId = elems[1];

            if (Objects.equals(lineOrderId, orderId)) {
                writeOutput(lineProductId);
                closeTask();
                break;
            }
        }

        file.close();
    }

    private void closeTask() throws IOException {
        sem.release();

        // didn't finish the order yet => new task starting from where this one stopped
        if (sem.availablePermits() <= 0) {
            pool.submit(new Task2(orderId, sem, pool, file.getFilePointer()));
        }
    }

    private void writeOutput(String productId) throws IOException {
        if (Tema2.orderToObjects.get(orderId) == 0) {
            return;
        }

        synchronized (Tema2.orderProductsOutFile) {
            Tema2.orderProductsOutFile.print(orderId + "," + productId + ",shipped\n");
        }
    }

    @Override
    public void run() {
        try {
            findOrder();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
