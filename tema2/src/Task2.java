import java.io.*;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

import static java.lang.Math.min;

public class Task2 implements Runnable{
    private final String orderId;
    private final ExecutorService pool1;
    private final ExecutorService pool2;
    private long startPos;
    private final BufferedReader file;
    private Integer objectsCounter;
    private final Integer objectsCounterInitial;

    public Task2(String orderId, ExecutorService pool1, ExecutorService pool2,
                 long startPos, int objectsCounter) throws FileNotFoundException {
        this.orderId = orderId;
        this.startPos = startPos;
        this.pool1 = pool1;
        this.pool2 = pool2;
        this.file = new BufferedReader(new FileReader(Tema2.products));
        this.objectsCounter = objectsCounter;
        this.objectsCounterInitial = objectsCounter;
    }

    /**
     * function that separates the elements
     * of a line by comma
     * @param line products information
     */
    private String[] parseLine(String line) {
        return line.split(Constants.separator);
    }

    /**
     * function that searches in the input file
     * for an order's products
     */
    private void findOrder() throws IOException {
        // start at the last position where same order had a product
        file.skip(startPos);

        String line = file.readLine();
        while(line != null && objectsCounter > 0) {
            String[] elems = parseLine(line);
            String lineOrderId = elems[0];
            String lineProductId = elems[1];

            startPos += line.length() + 1;

            // order product found
            if (Objects.equals(lineOrderId, orderId)) {
                writeOutput(lineProductId);
            }

            line = file.readLine();
        }

        closeTask();
    }

    /**
     * function that marks a task as completed
     * If the order is not yet finished it creates
     * a new task
     */
    private void closeTask() throws IOException {
        Tema2.orderToObjects.put(orderId, Tema2.orderToObjects.get(orderId) - 1);

        if (Tema2.orderToObjects.get(orderId) > 0) {
            // didn't finish the order yet => new task starting from where this one stopped
            pool2.submit(new Task2(orderId, pool1, pool2, startPos,
                    min(objectsCounterInitial, Tema2.orderToObjects.get(orderId))));
        } else {
            // order finished, print its output
            pool1.submit(new Task1Writer(pool1, pool2, orderId));
        }

        file.close();
    }

    /**
     * function that writes to order_products output file
     * when a product has been found and shipped
     * @param productId order's name
     */
    private void writeOutput(String productId) {
        objectsCounter--;

        synchronized (Tema2.orderProductsOutFile) {
            Tema2.orderProductsOutFile.print(orderId + Constants.separator +
                    productId + Constants.outputFileEnding);
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
