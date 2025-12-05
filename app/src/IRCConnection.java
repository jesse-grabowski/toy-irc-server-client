import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class IRCConnection implements Closeable {

    private static final ThreadFactory INGRESS_THREAD_FACTORY = Thread.ofVirtual().name("irc-ingress-", 0).factory();
    private static final ThreadFactory EGRESS_THREAD_FACTORY = Thread.ofVirtual().name("irc-egress-", 0).factory();

    private final BlockingQueue<String> egressQueue = new LinkedBlockingQueue<>();
    private final List<Consumer<String>> ingressHandlers = new CopyOnWriteArrayList<>();
    private final AtomicBoolean running = new AtomicBoolean(false);

    private final Socket socket;
    private final BufferedReader bufferedReader;
    private final BufferedWriter bufferedWriter;

    private final Thread ingressThread;
    private final Thread egressThread;

    public IRCConnection(Socket socket) throws IOException {
        this.socket = Objects.requireNonNull(socket);

        bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

        ingressThread = INGRESS_THREAD_FACTORY.newThread(this::doIngress);
        egressThread = EGRESS_THREAD_FACTORY.newThread(this::doEgress);
    }

    public void addIngressHandler(Consumer<String> handler) {
        ingressHandlers.add(handler);
    }

    public boolean send(String line) {
        return egressQueue.offer(Objects.requireNonNull(line));
    }

    public void start() {
        if (!running.compareAndSet(false, true)) {
            return; // not stopped
        }

        ingressThread.start();
        egressThread.start();
    }

    public boolean isRunning() {
        return running.get();
    }

    private void doIngress() {
        try {
            String line;
            while (running.get() && (line = bufferedReader.readLine()) != null) {
                for (Consumer<String> handler : ingressHandlers) {
                    try {
                        handler.accept(line);
                    } catch (Throwable t) {
                        // should never happen but just in case
                    }
                }
            }
        } catch (IOException e) {
            // connection is broken, cannot recover without intervention
        } finally {
            running.set(false);
            closeSilently();
        }
    }

    private void doEgress() {
        try {
            while (running.get() && !Thread.currentThread().isInterrupted()) {
                String line = egressQueue.take();

                if (!running.get()) {
                    break;
                }

                bufferedWriter.write(line);
                bufferedWriter.write("\r\n");
                bufferedWriter.flush();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            // connection is broken, cannot recover without intervention
        } finally {
            running.set(false);
            closeSilently();
        }
    }

    private void closeSilently() {
        try {
            socket.close();
        } catch (IOException ignored) {
            // unhandled
        }
    }

    @Override
    public void close() throws IOException {
        if (!running.compareAndSet(true, false)) {
            return; // already stopped
        }

        closeSilently();

        ingressThread.interrupt();
        egressThread.interrupt();
    }
}
