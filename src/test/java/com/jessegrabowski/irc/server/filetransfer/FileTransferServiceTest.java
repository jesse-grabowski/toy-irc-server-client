/*
 * This project is licensed under the MIT License.
 *
 * In addition to the rights granted under the MIT License, explicit permission
 * is granted to the faculty, instructors, teaching assistants, and evaluators
 * of Ritsumeikan University for unrestricted educational evaluation and grading.
 *
 * ---------------------------------------------------------------------------
 *
 * MIT License
 *
 * Copyright (c) 2026 Jesse Grabowski
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.jessegrabowski.irc.server.filetransfer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.jessegrabowski.irc.network.Acceptor;
import com.jessegrabowski.irc.protocol.FileTransferFramingException;
import com.jessegrabowski.irc.protocol.FileTransferOperationMarshaller;
import com.jessegrabowski.irc.protocol.FileTransferOperationUnmarshaller;
import com.jessegrabowski.irc.protocol.FileTransferUnknownOpCodeException;
import com.jessegrabowski.irc.protocol.model.FileTransferOperation;
import com.jessegrabowski.irc.protocol.model.FileTransferOperationAck;
import com.jessegrabowski.irc.protocol.model.FileTransferOperationError;
import com.jessegrabowski.irc.protocol.model.FileTransferOperationErrorCode;
import com.jessegrabowski.irc.protocol.model.FileTransferOperationGoodbye;
import com.jessegrabowski.irc.protocol.model.FileTransferOperationHello;
import com.jessegrabowski.irc.protocol.model.FileTransferOperationISend;
import com.jessegrabowski.irc.protocol.model.FileTransferOperationReady;
import com.jessegrabowski.irc.protocol.model.FileTransferOperationRole;
import com.jessegrabowski.irc.protocol.model.FileTransferOperationVersion;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class FileTransferServiceTest {

    private static final FileTransferOperationMarshaller MARSHALLER = new FileTransferOperationMarshaller();
    private static final FileTransferOperationUnmarshaller UNMARSHALLER = new FileTransferOperationUnmarshaller();

    static Acceptor acceptor;
    static FileTransferService service;
    static int port;

    @BeforeAll
    static void setUp() throws IOException {
        service = new FileTransferService();
        acceptor = new Acceptor(InetAddress.getLoopbackAddress(), 0, service::accept);
        port = acceptor.start();
    }

    @AfterAll
    static void tearDown() throws IOException {
        acceptor.close();
        service.close();
    }

    @Test
    void shouldTransmitFile() throws IOException, FileTransferFramingException, FileTransferUnknownOpCodeException {
        ConnectionHolder a = connect();
        ConnectionHolder b = connect();

        UUID token = UUID.randomUUID();
        byte[] payload = new byte[20];
        new Random().nextBytes(payload);

        connectPair(a, b, token);

        FileTransferOperationISend send = transmit(a.out, b.in, new FileTransferOperationISend(payload.length));
        assertEquals(payload.length, send.getSize());

        a.out.write(payload);
        byte[] results = b.in.readNBytes(payload.length);
        assertArrayEquals(payload, results);

        transmit(b.out, a.in, new FileTransferOperationAck());
        transmit(a.out, b.in, new FileTransferOperationGoodbye());
        send(b.out, new FileTransferOperationGoodbye());

        assertEquals(-1, a.in.read());
        assertEquals(-1, b.in.read());
    }

    @Test
    void shouldRejectWhenNoHello() throws Exception {
        ConnectionHolder a = connect();

        send(a.out, new FileTransferOperationISend(1));

        FileTransferOperationError err = receive(a.in, FileTransferOperationError.class);
        assertEquals(FileTransferOperationErrorCode.NO_HELLO, err.getErrorCode());

        assertEquals(-1, a.in.read());
    }

    @Test
    void shouldRejectMultipleSenders() throws Exception {
        UUID token = UUID.randomUUID();

        ConnectionHolder sender1 = connect();
        ConnectionHolder receiver = connect();

        connectPair(sender1, receiver, token);

        ConnectionHolder sender2 = connect();

        hello(sender2.out, token, FileTransferOperationRole.SENDER);

        FileTransferOperationError err = receive(sender2.in, FileTransferOperationError.class);
        assertEquals(FileTransferOperationErrorCode.MULTIPLE_SENDERS, err.getErrorCode());

        transmit(sender1.out, receiver.in, new FileTransferOperationGoodbye());
        send(receiver.out, new FileTransferOperationGoodbye());

        assertEquals(-1, sender1.in.read());
        assertEquals(-1, receiver.in.read());
        assertEquals(-1, sender2.in.read());
    }

    @Test
    void shouldRejectMultipleReceivers() throws Exception {
        UUID token = UUID.randomUUID();

        ConnectionHolder sender = connect();
        ConnectionHolder receiver1 = connect();

        connectPair(sender, receiver1, token);

        ConnectionHolder receiver2 = connect();

        hello(receiver2.out, token, FileTransferOperationRole.RECEIVER);

        FileTransferOperationError err = receive(receiver2.in, FileTransferOperationError.class);
        assertEquals(FileTransferOperationErrorCode.MULTIPLE_RECEIVERS, err.getErrorCode());

        transmit(sender.out, receiver1.in, new FileTransferOperationGoodbye());
        send(receiver1.out, new FileTransferOperationGoodbye());

        assertEquals(-1, sender.in.read());
        assertEquals(-1, receiver1.in.read());
        assertEquals(-1, receiver2.in.read());
    }

    @Test
    void shouldRejectUnknownRoleWithoutReady() throws Exception {
        ConnectionHolder a = connect();

        UUID token = UUID.randomUUID();
        hello(a.out, token, FileTransferOperationRole.UNKNOWN);

        FileTransferOperationError err = receive(a.in, FileTransferOperationError.class);
        assertEquals(FileTransferOperationErrorCode.UNKNOWN_ROLE, err.getErrorCode());

        assertEquals(-1, a.in.read());
    }

    @Test
    void shouldRejectFileTooSmall() throws Exception {
        UUID token = UUID.randomUUID();

        ConnectionHolder sender = connect();
        ConnectionHolder receiver = connect();

        connectPair(sender, receiver, token);

        send(sender.out, new FileTransferOperationISend(-1));

        FileTransferOperationError err = receive(sender.in, FileTransferOperationError.class);
        assertEquals(FileTransferOperationErrorCode.FILE_TOO_SMALL, err.getErrorCode());

        transmit(sender.out, receiver.in, new FileTransferOperationGoodbye());
        send(receiver.out, new FileTransferOperationGoodbye());

        assertEquals(-1, sender.in.read());
        assertEquals(-1, receiver.in.read());
    }

    @Test
    void shouldRejectFileTooLarge() throws Exception {
        UUID token = UUID.randomUUID();

        ConnectionHolder sender = connect();
        ConnectionHolder receiver = connect();

        connectPair(sender, receiver, token);

        send(sender.out, new FileTransferOperationISend(10_000_001L));
        FileTransferOperationError err = receive(sender.in, FileTransferOperationError.class);
        assertEquals(FileTransferOperationErrorCode.FILE_TOO_LARGE, err.getErrorCode());

        transmit(sender.out, receiver.in, new FileTransferOperationGoodbye());
        send(receiver.out, new FileTransferOperationGoodbye());

        assertEquals(-1, sender.in.read());
        assertEquals(-1, receiver.in.read());
    }

    @Test
    void shouldRelayGoodbyeReceiverToSender() throws Exception {
        UUID token = UUID.randomUUID();

        ConnectionHolder sender = connect();
        ConnectionHolder receiver = connect();

        connectPair(sender, receiver, token);

        transmit(receiver.out, sender.in, new FileTransferOperationGoodbye());
        send(sender.out, new FileTransferOperationGoodbye());

        assertEquals(-1, sender.in.read());
        assertEquals(-1, receiver.in.read());
    }

    @Test
    void shouldRejectBadFramingWhenMagicIsWrong() throws Exception {
        ConnectionHolder a = connect();

        a.out.writeInt(0xCAFEBABE);
        a.out.writeByte(0x00);
        a.out.flush();

        FileTransferOperationError err = receive(a.in, FileTransferOperationError.class);
        assertEquals(FileTransferOperationErrorCode.BAD_FRAMING, err.getErrorCode());

        assertEquals(-1, a.in.read());
    }

    @Test
    void shouldRejectBadFramingWhenFrameIsTruncated() throws Exception {
        ConnectionHolder a = connect();

        a.out.writeShort(0xDEAD);
        a.out.flush();
        a.socket.shutdownOutput();

        FileTransferOperationError err = receive(a.in, FileTransferOperationError.class);
        assertEquals(FileTransferOperationErrorCode.BAD_FRAMING, err.getErrorCode());

        assertEquals(-1, a.in.read());
    }

    @Test
    void shouldRejectUnknownOpCode() throws Exception {
        ConnectionHolder a = connect();

        a.out.writeInt(0xDEADBEEF);
        a.out.writeByte(0x67);
        a.out.flush();

        FileTransferOperationError err = receive(a.in, FileTransferOperationError.class);
        assertEquals(FileTransferOperationErrorCode.UNKNOWN_OP, err.getErrorCode());

        assertEquals(-1, a.in.read());
    }

    @Test
    void shouldRejectUnknownVersion() throws Exception {
        ConnectionHolder a = connect();

        a.out.writeInt(0xDEADBEEF);
        a.out.writeByte(0x01);
        a.out.writeByte(0x7F);
        UUID token = UUID.randomUUID();
        a.out.writeLong(token.getMostSignificantBits());
        a.out.writeLong(token.getLeastSignificantBits());
        a.out.writeByte(0x01);
        a.out.flush();

        FileTransferOperationError err = receive(a.in, FileTransferOperationError.class);
        assertEquals(FileTransferOperationErrorCode.UNKNOWN_VERSION, err.getErrorCode());

        assertEquals(-1, a.in.read());
    }

    private void connectPair(ConnectionHolder sender, ConnectionHolder receiver, UUID token)
            throws IOException, FileTransferFramingException, FileTransferUnknownOpCodeException {
        hello(sender.out, token, FileTransferOperationRole.SENDER);
        hello(receiver.out, token, FileTransferOperationRole.RECEIVER);
        receive(sender.in, FileTransferOperationReady.class);
        receive(receiver.in, FileTransferOperationReady.class);
    }

    private void send(DataOutputStream dos, FileTransferOperation operation) throws IOException {
        MARSHALLER.marshal(dos, operation);
        dos.flush();
    }

    private <T extends FileTransferOperation> T receive(DataInputStream dis, Class<T> type)
            throws FileTransferFramingException, IOException, FileTransferUnknownOpCodeException {
        FileTransferOperation operation = UNMARSHALLER.unmarshal(dis);
        assertInstanceOf(type, operation);
        return (T) operation;
    }

    private <T extends FileTransferOperation> T transmit(DataOutputStream sender, DataInputStream receiver, T operation)
            throws IOException, FileTransferFramingException, FileTransferUnknownOpCodeException {
        Class<T> clazz = (Class<T>) operation.getClass();
        send(sender, operation);
        T result = receive(receiver, clazz);
        assertEquals(operation, result);
        return result;
    }

    private void hello(DataOutputStream to, UUID token, FileTransferOperationRole role) throws IOException {
        send(to, new FileTransferOperationHello(FileTransferOperationVersion.V1, token, role));
    }

    private ConnectionHolder connect() throws IOException {
        Socket socket = new Socket(InetAddress.getLoopbackAddress(), port);
        return new ConnectionHolder(
                socket, new DataOutputStream(socket.getOutputStream()), new DataInputStream(socket.getInputStream()));
    }

    private record ConnectionHolder(Socket socket, DataOutputStream out, DataInputStream in) {}
}
