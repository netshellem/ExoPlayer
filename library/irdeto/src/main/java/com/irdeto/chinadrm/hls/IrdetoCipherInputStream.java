package com.irdeto.chinadrm.hls;

import com.irdeto.drm.ChinaDrm;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

public class IrdetoCipherInputStream extends FilterInputStream {

    private final int I_BUFFER_SIZE = 16;
    private final byte[] i_buffer = new byte[I_BUFFER_SIZE];
    private int index; // index of the bytes to return from o_buffer
    private byte[] o_buffer;
    private boolean finished;
    private ChinaDrm drm;
    private byte[] m_result;
    private String line;

    public IrdetoCipherInputStream(InputStream is, ChinaDrm drm, String line) {
        super(is);
        this.drm = drm;
        this.line = line;
    }

    /**
     * Reads the next byte from this cipher input stream.
     *
     * @return the next byte, or {@code -1} if the end of the stream is reached.
     * @throws IOException
     *             if an error occurs.
     */
    @Override
    //todo: call netive method to decrypt buffer
    public int read() throws IOException {
        if (finished) {
            return ((o_buffer == null) || (index == o_buffer.length))
                    ? -1
                    : o_buffer[index++] & 0xFF;
        }
        if ((o_buffer != null) && (index < o_buffer.length)) {
            return o_buffer[index++] & 0xFF;
        }
        index = 0;
        o_buffer = null;
        int num_read;
        while (o_buffer == null) {
            if ((num_read = in.read(i_buffer)) == -1) {
                try {
                    o_buffer = doFinal();
                } catch (Exception e) {
                    throw new IOException(e.getMessage());
                }
                m_result = null;
                finished = true;
                break;
            }
            o_buffer = update(i_buffer, 0, num_read);
        }
        return read();
    }

    byte[] concat(byte[] a, byte[] b) {
        return com.google.common.primitives.Bytes.concat(a, b);
    }
    //todo: need to improve performance for inputstream
    private byte[] doFinal(){
        //native decrypt
        int len = m_result.length;

        return this.drm.native_decryptBuffer( m_result, line);
        //return null;
    }

    //todo: need to improve performance for inputstream
    private byte[] update(byte[] input, int offset, int inputLen){
        byte[] ret;
        byte[] trim_input = new byte[inputLen];
        System.arraycopy(input, offset, trim_input,0, inputLen);
        if(m_result == null) {
            m_result = trim_input;
        }
        else
        {
            m_result = concat(m_result, trim_input);
            if( m_result.length > 0 && (m_result.length >= 512) && (m_result.length %16 == 0)) {
                ret = this.drm.native_decryptBuffer( m_result, line);
                if(ret !=  null) {
                    m_result = null;
                    return ret;
                }
            }
        }
        return null;
    }
    /**
     * Reads the next {@code b.length} bytes from this input stream into buffer
     * {@code b}.
     *
     * @param b
     *            the buffer to be filled with data.
     * @return the number of bytes filled into buffer {@code b}, or {@code -1}
     *         if the end of the stream is reached.
     * @throws IOException
     *             if an error occurs.
     */
    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }
    /**
     * Reads the next {@code len} bytes from this input stream into buffer
     * {@code b} starting at offset {@code off}.
     * <p>
     * if {@code b} is {@code null}, the next {@code len} bytes are read and
     * discarded.
     *
     * @param b
     *            the buffer to be filled with data.
     * @param off
     *            the offset to start in the buffer.
     * @param len
     *            the maximum number of bytes to read.
     * @return the number of bytes filled into buffer {@code b}, or {@code -1}
     *         of the of the stream is reached.
     * @throws IOException
     *             if an error occurs.
     * @throws NullPointerException
     *             if the underlying input stream is {@code null}.
     */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (in == null) {
            throw new NullPointerException("Underlying input stream is null");
        }
        int read_b;
        int i;
        for (i=0; i<len; i++) {
            if ((read_b = read()) == -1) {
                return (i == 0) ? -1 : i;
            }
            if (b != null) {
                b[off+i] = (byte) read_b;
            }
        }
        return i;
    }


    @Override
    public long skip(long n) throws IOException {
        long i = 0;
        int available = available();
        if (available < n) {
            n = available;
        }
        while ((i < n) && (read() != -1)) {
            i++;
        }
        return i;
    }
    @Override
    public int available() throws IOException {
        return 0;
    }
    /**
     * Closes this {@code CipherInputStream}, also closes the underlying input
     * stream and call {@code doFinal} on the cipher object.
     *
     * @throws IOException
     *             if an error occurs.
     */
    @Override
    public void close() throws IOException {
        in.close();

        //todo: close file handler
       // IOException e = new IOException();
       // throw e;
    }
    /**
     * Returns whether this input stream supports {@code mark} and
     * {@code reset}, which it does not.
     *
     * @return false, since this input stream does not support {@code mark} and
     *         {@code reset}.
     */
    @Override
    public boolean markSupported() {
        return false;
    }
}
