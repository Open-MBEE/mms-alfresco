package gov.nasa.jpl.view_repo.util;

import org.alfresco.repo.content.AbstractContentReader;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.text.SimpleDateFormat;
import java.util.Date;

public class JsonContentReader extends AbstractContentReader implements ContentReader {

    private static Logger logger = Logger.getLogger(JsonContentReader.class);

    private String json;
    private String modified;
    private long size;
    private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    public JsonContentReader(JSONObject json) {
        this(json.toString(), "store://");
        modified = json.optString(Sjm.MODIFIED);
        size = json.length();
    }

    public JsonContentReader(String json, String url) {
        super(url);
        this.json = json;
    }

    public String getJson() {
        return this.json;
    }

    public boolean exists() {
        return this.size > 0;
    }

    public long getSize() {
        return !this.exists() ? 0L : this.json.length();
    }

    public long getLastModified() {
        Date lastModified = new Date();
        try {
            lastModified = df.parse(this.modified);
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("getLastModified Error: ", e);
            }
        }
        return lastModified.getTime();
    }

    protected ContentReader createReader() throws ContentIOException {
        return new JsonContentReader(this.json, this.getContentUrl());
    }

    public void getStreamContent(OutputStream os) throws ContentIOException {
        InputStream is = new ByteArrayInputStream(this.json.getBytes());
        try {
            StreamUtils.copy(is, os);
        } catch (IOException var3) {
            throw new ContentIOException("Failed to copy content to output stream: \n   accessor: " + this, var3);
        } finally {
            try {
                is.close();
                os.close();
            } catch (IOException var12) {
                // Intentionally Blank
            }
        }
    }

    protected ReadableByteChannel getDirectReadableChannel() throws ContentIOException {
        ReadableByteChannel channel = null;
        InputStream is = new ByteArrayInputStream(this.json.getBytes());
        try {
            if (!this.exists()) {
                throw new IOException("File does not exist: " + this.json);
            } else {

                channel = Channels.newChannel(is);

                return channel;
            }
        } catch (Throwable var3) {
            throw new ContentIOException("Failed to open json channel: " + this, var3);
        } finally {
            try {
                if (channel != null) {
                    channel.close();
                }
            } catch (IOException e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Error: ", e);
                }
            }
        }
    }
}
