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
package com.jessegrabowski.irc.protocol;

import static org.junit.jupiter.api.Assertions.*;

import com.jessegrabowski.irc.protocol.model.*;
import java.io.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class FileTransferOperationSerializationTest {
    private final FileTransferOperationMarshaller marshaller = new FileTransferOperationMarshaller();
    private final FileTransferOperationUnmarshaller unmarshaller = new FileTransferOperationUnmarshaller();

    @Test
    void roundTrip_hello() throws Exception {
        UUID token = UUID.fromString("f7128efc-9fc5-4aaf-b8e8-108045f72bf7");
        FileTransferOperationHello op = new FileTransferOperationHello(
                FileTransferOperationVersion.V1, token, FileTransferOperationRole.SENDER);

        FileTransferOperation decoded = roundTrip(op);

        assertInstanceOf(FileTransferOperationHello.class, decoded);
        FileTransferOperationHello hello = (FileTransferOperationHello) decoded;
        assertEquals(op.getVersion(), hello.getVersion());
        assertEquals(op.getToken(), hello.getToken());
        assertEquals(op.getRole(), hello.getRole());
    }

    @Test
    void roundTrip_ready() throws Exception {
        FileTransferOperationReady op = new FileTransferOperationReady();

        FileTransferOperation decoded = roundTrip(op);

        assertInstanceOf(FileTransferOperationReady.class, decoded);
    }

    @Test
    void roundTrip_iSend() throws Exception {
        FileTransferOperationISend op = new FileTransferOperationISend(123456789L);

        FileTransferOperation decoded = roundTrip(op);

        assertInstanceOf(FileTransferOperationISend.class, decoded);
        assertEquals(op.getSize(), ((FileTransferOperationISend) decoded).getSize());
    }

    @Test
    void roundTrip_ack() throws Exception {
        FileTransferOperationAck op = new FileTransferOperationAck();

        FileTransferOperation decoded = roundTrip(op);

        assertInstanceOf(FileTransferOperationAck.class, decoded);
    }

    @Test
    void roundTrip_goodbye() throws Exception {
        FileTransferOperationGoodbye op = new FileTransferOperationGoodbye();

        FileTransferOperation decoded = roundTrip(op);

        assertInstanceOf(FileTransferOperationGoodbye.class, decoded);
    }

    @Test
    void roundTrip_error() throws Exception {
        FileTransferOperationError op = new FileTransferOperationError(FileTransferOperationErrorCode.BROKEN_PIPE);

        FileTransferOperation decoded = roundTrip(op);

        assertInstanceOf(FileTransferOperationError.class, decoded);
        assertEquals(op.getErrorCode(), ((FileTransferOperationError) decoded).getErrorCode());
    }

    @Test
    void unmarshal_misalignedFrame_throwsFramingException() throws Exception {
        byte[] bytes;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(baos)) {

            out.writeInt(0xCAFEBABE);
            out.writeByte(FileTransferOperationCode.ACK.getValue());
            out.flush();
            bytes = baos.toByteArray();
        }

        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            FileTransferFramingException ex =
                    assertThrows(FileTransferFramingException.class, () -> unmarshaller.unmarshal(in));
            assertTrue(ex.getMessage().contains("Misaligned frame"));
            assertTrue(ex.getMessage().contains("0xCAFEBABE"));
        }
    }

    @Test
    void unmarshal_truncatedFrame_throwsFramingExceptionWithCause() throws Exception {
        byte[] bytes;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(baos)) {

            out.writeInt(0xDEADBEEF);
            out.flush();
            bytes = baos.toByteArray();
        }

        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            FileTransferFramingException ex =
                    assertThrows(FileTransferFramingException.class, () -> unmarshaller.unmarshal(in));
            assertEquals("Truncated frame", ex.getMessage());
            assertNotNull(ex.getCause());
            assertInstanceOf(EOFException.class, ex.getCause());
        }
    }

    @Test
    void unmarshal_unknownOpcode_throwsUnknownOpCodeException() throws Exception {
        byte[] bytes;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(baos)) {

            out.writeInt(0xDEADBEEF);
            out.writeByte(FileTransferOperationCode.UNKNOWN.getValue());
            out.flush();
            bytes = baos.toByteArray();
        }

        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            assertThrows(FileTransferUnknownOpCodeException.class, () -> unmarshaller.unmarshal(in));
        }
    }

    @Test
    void unmarshal_hello_unknownRole_roundTripsAsUnknown() throws Exception {
        UUID token = UUID.fromString("f7128efc-9fc5-4aaf-b8e8-108045f72bf7");

        byte[] bytes;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(baos)) {

            out.writeInt(0xDEADBEEF);
            out.writeByte(FileTransferOperationCode.HELLO.getValue());
            out.writeByte(FileTransferOperationVersion.V1.getValue());
            out.writeLong(token.getMostSignificantBits());
            out.writeLong(token.getLeastSignificantBits());
            out.writeByte(99);
            out.flush();
            bytes = baos.toByteArray();
        }

        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            FileTransferOperation op = unmarshaller.unmarshal(in);
            assertInstanceOf(FileTransferOperationHello.class, op);
            FileTransferOperationHello hello = (FileTransferOperationHello) op;
            assertEquals(FileTransferOperationRole.UNKNOWN, hello.getRole());
        }
    }

    @Test
    void unmarshal_error_unknownErrorCode_roundTripsAsUnknown() throws Exception {
        byte[] bytes;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(baos)) {

            out.writeInt(0xDEADBEEF);
            out.writeByte(FileTransferOperationCode.ERROR.getValue());
            out.writeByte(42);
            out.flush();
            bytes = baos.toByteArray();
        }

        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            FileTransferOperation op = unmarshaller.unmarshal(in);
            assertInstanceOf(FileTransferOperationError.class, op);
            assertEquals(FileTransferOperationErrorCode.UNKNOWN, ((FileTransferOperationError) op).getErrorCode());
        }
    }

    private FileTransferOperation roundTrip(FileTransferOperation op) throws Exception {
        byte[] bytes;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(baos)) {

            marshaller.marshal(out, op);
            out.flush();
            bytes = baos.toByteArray();
        }

        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            return unmarshaller.unmarshal(in);
        }
    }
}
