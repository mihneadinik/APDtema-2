import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

import static java.lang.Math.max;

public class Task1 implements Runnable{
    private final Integer id;
    private final ExecutorService pool1;
    private final ExecutorService pool2;
    private long left, right;
    private final RandomAccessFile file;
    private final ArrayList<String> lines;
    private Boolean closedThread;

    public Task1(Integer id, ExecutorService pool1, ExecutorService pool2) throws FileNotFoundException {
        this.id = id;
        this.pool1 = pool1;
        this.pool2 = pool2;
        this.file = new RandomAccessFile(Tema2.orders, "r");
        this.lines = new ArrayList<>();
        this.closedThread = false;
    }

    /**
     * function that moves the cursor at the beginning
     * of a new line
     * @param pos starting index
     */
    private long moveUntilNewLine(long pos) throws IOException {
        // already at new line
        file.seek(pos - 1);
        if (file.read() == '\n') {
            return pos;
        }

        while (file.read() != '\n' && file.getFilePointer() < Tema2.orderFileLength) {}

        return file.getFilePointer();
    }

    /**
     * marks current thread as closed
     */
    private void closeThread() {
        this.closedThread = true;
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
        if (left >= Tema2.orderFileLength) {
            closeThread();
            return;
        }

        // correct start index
        if (left != 0) {
            left = moveUntilNewLine(left);

            // file finished here
            if (left >= Tema2.orderFileLength) {
                closeThread();
                return;
            }
        }

        // correct end index
        if (right >= Tema2.orderFileLength) {
            right = Tema2.orderFileLength;
        } else {
            right = moveUntilNewLine(right) - 1;
        }
    }

    /**
     * function that reads the lines assigned
     * to a level1 thread
     */
    private void readLines() throws IOException {
        file.seek(left);
        while (file.getFilePointer() < right) {
            String newLine = file.readLine();
            this.lines.add(newLine);
        }
        file.close();
    }

    /**
     * function that separates the elements
     * of a line by comma
     * @param pos line index relative to current
     * thread's area of work
     */
    private String[] parseLine(int pos) {
        String line = lines.get(pos);
        return line.split(Constants.separator);
    }

    /**
     * function that creates level2 tasks for each order
     */
    private void assignTasks() throws InterruptedException, IOException {
        for (int i = 0; i < lines.size(); i++) {
            String[] elems = parseLine(i);
            String orderId = elems[Constants.orderNamePosition];
            int nrProducts = Integer.parseInt(elems[Constants.orderNumberOfElementsPosition]);

            // empty order
            if (nrProducts == 0) {
                continue;
            }

            // update orders information
            Tema2.ordersInProgress.incrementAndGet();
            Tema2.orderToObjects.put(orderId, nrProducts);
            Tema2.orderToObjectsClone.put(orderId, nrProducts);

            pool2.submit(new Task2(orderId, pool1, pool2, 0, max(1, nrProducts / Tema2.P)));
        }

        closeThread();
    }

    @Override
    public void run() {
        try {
            correctReadMargins();
            // thread doesn't have a valid read portion
            if (this.closedThread) {
                return;
            }

            readLines();
            assignTasks();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
