package com.jessegrabowski.irc.protocol;

import com.jessegrabowski.irc.protocol.model.*;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class FileTransferOperationSerializationTest {
    private final FileTransferOperationMarshaller marshaller = new FileTransferOperationMarshaller();
    private final FileTransferOperationUnmarshaller unmarshaller = new FileTransferOperationUnmarshaller();

    @Test
    void roundTrip_hello() throws Exception {
        UUID token = UUID.fromString("f7128efc-9fc5-4aaf-b8e8-108045f72bf7");
        FileTransferOperationHello op =
                new FileTransferOperationHello(FileTransferOperationVersion.V1, token, FileTransferOperationRole.SENDER);

        FileTransferOperation decoded = roundTrip(op);

        assertInstanceOf(FileTransferOperationHello.class, decoded);
        FileTransferOperationHello hello = (FileTransferOperationHello) decoded;
        assertEquals(op.getVersion(), hello.getVersion());
        assertEquals(op.getToken(), hello.getToken());
        assertEquals(op.getRole(), hello.getRole());
    }

    @Test
    void roundTrip_iSend() throws Exception {
        FileTransferOperationISend op = new FileTransferOperationISend(123456789L);

        FileTransferOperation decoded = roundTrip(op);

        assertInstanceOf(FileTransferOperationISend.class, decoded);
        assertEquals(op.getSize(), ((FileTransferOperationISend) decoded).getSize());
    }

    @Test
    void roundTrip_iReceive() throws Exception {
        FileTransferOperationIReceive op = new FileTransferOperationIReceive(9876543210L);

        FileTransferOperation decoded = roundTrip(op);

        assertInstanceOf(FileTransferOperationIReceive.class, decoded);
        assertEquals(op.getSize(), ((FileTransferOperationIReceive) decoded).getSize());
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
