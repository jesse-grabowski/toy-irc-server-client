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

import com.jessegrabowski.irc.protocol.model.FileTransferOperation;
import com.jessegrabowski.irc.protocol.model.FileTransferOperationAck;
import com.jessegrabowski.irc.protocol.model.FileTransferOperationError;
import com.jessegrabowski.irc.protocol.model.FileTransferOperationGoodbye;
import com.jessegrabowski.irc.protocol.model.FileTransferOperationHello;
import com.jessegrabowski.irc.protocol.model.FileTransferOperationISend;
import com.jessegrabowski.irc.protocol.model.FileTransferOperationReady;
import java.io.DataOutputStream;
import java.io.IOException;

public class FileTransferOperationMarshaller {

    public void marshal(DataOutputStream out, FileTransferOperation op) throws IOException {
        out.writeInt(0xDEADBEEF);
        out.writeByte(op.getOpCode().getValue());

        switch (op) {
            case FileTransferOperationHello o -> marshalHello(out, o);
            case FileTransferOperationReady o -> marshalReady(out, o);
            case FileTransferOperationISend o -> marshalISend(out, o);
            case FileTransferOperationAck o -> marshalAck(out, o);
            case FileTransferOperationGoodbye o -> marshalGoodbye(out, o);
            case FileTransferOperationError o -> marshalError(out, o);
        }
    }

    private void marshalHello(DataOutputStream out, FileTransferOperationHello op) throws IOException {
        out.writeByte(op.getVersion().getValue());
        out.writeLong(op.getToken().getMostSignificantBits());
        out.writeLong(op.getToken().getLeastSignificantBits());
        out.writeByte(op.getRole().getValue());
    }

    private void marshalReady(DataOutputStream out, FileTransferOperationReady op) {}

    private void marshalISend(DataOutputStream out, FileTransferOperationISend op) throws IOException {
        out.writeLong(op.getSize());
    }

    private void marshalAck(DataOutputStream out, FileTransferOperationAck op) {}

    private void marshalGoodbye(DataOutputStream out, FileTransferOperationGoodbye op) {}

    private void marshalError(DataOutputStream out, FileTransferOperationError op) throws IOException {
        out.writeByte(op.getErrorCode().getValue());
    }
}
