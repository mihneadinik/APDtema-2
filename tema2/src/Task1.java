import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class Task1 implements Runnable{
    private final Integer id;
    private final ExecutorService pool1;
    private final ExecutorService pool2;
    private long left, right;
    private RandomAccessFile file;
    private ArrayList<String> lines;
    private Boolean closedThread;

    public Task1(Integer id, ExecutorService pool1, ExecutorService pool2) throws FileNotFoundException {
        this.id = id;
        this.pool1 = pool1;
        this.pool2 = pool2;
        this.file = new RandomAccessFile(Tema2.orders, "r");
        this.lines = new ArrayList<>();
        this.closedThread = false;
    }

    private long moveUntilNewLine(long pos) throws IOException {
        file.seek(pos);
        int currChar = file.read();
        while(currChar != '\n' && file.getFilePointer() < Tema2.orderLength) {
            currChar = file.read();
        }

        return file.getFilePointer();
    }

    private void closeThread() throws IOException {
        Tema2.level1ThreadsActive.getAndDecrement();
        this.closedThread = true;

        // check if needed to close pools
        if (Tema2.level1ThreadsActive.get() == 0) {
            pool2.shutdown();
            pool1.shutdown();
            Tema2.orderProductsOutFile.close();
            Tema2.ordersOutFile.close();
        }
    }

    /**
     * if left position doesn't match with a new line,
     * should be moved to the right until it does; same
     * for the right position
     */
    private void correctReadMargins() throws IOException {
        ArrayList<Long> margins = Tema2.threadToBytes.get(this.id);
        left = margins.get(0);
        right = margins.get(1);

        // out of file starting position
        if (left >= Tema2.orderLength) {
            closeThread();
            return;
        }

        // correct start index
        if (left != 0) {
            left = moveUntilNewLine(left);

            // file finished here
            if (left >= Tema2.orderLength) {
                closeThread();
                return;
            }
        }

        // correct end index
        if (right >= Tema2.orderLength) {
            right = Tema2.orderLength;
        } else {
            right = moveUntilNewLine(right) - 1;
        }
    }

    private void updateReadMargins() {
        Tema2.threadToBytes.put(this.id, new ArrayList<>(Arrays.asList(left, right)));
    }

    private void readLines() throws IOException {
        file.seek(left);
        while (file.getFilePointer() < right) {
            String newLine = file.readLine();
            this.lines.add(newLine);
        }
        Tema2.threadToOrdersNumber.put(id, new AtomicInteger(lines.size()));
        file.close();
    }

    private String[] parseLine(int pos) {
        String line = lines.get(pos);
        return line.split(",");
    }

    private void assignTasks() throws InterruptedException, IOException {
        for (int i = 0; i < lines.size(); i++) {
            String[] elems = parseLine(i);
            String orderId = elems[0];
            int nrProducts = Integer.parseInt(elems[1]);

            Tema2.orderToObjects.put(orderId, nrProducts);
            // wait until order is finished
            Semaphore sem = new Semaphore(-nrProducts + 1);
            pool2.submit(new Task2(orderId, sem, pool2, 0));
            sem.acquire();
            writeOutput(orderId, nrProducts);
        }

        closeThread();
    }

    private void writeOutput(String order, int products) throws IOException {
        if (products == 0) {
            return;
        }
        synchronized (Tema2.ordersOutFile) {
            Tema2.ordersOutFile.print(order + "," + products + ",shipped\n");
        }
    }

    @Override
    public void run() {
        try {
            correctReadMargins();
            if (this.closedThread) {
                return;
            }

            updateReadMargins();
            readLines();
            assignTasks();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
