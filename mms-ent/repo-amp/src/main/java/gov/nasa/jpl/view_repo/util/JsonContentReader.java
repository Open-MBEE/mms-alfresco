package gov.nasa.jpl.view_repo.util;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.alfresco.repo.content.AbstractContentReader;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.apache.log4j.Logger;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class JsonContentReader extends AbstractContentReader implements ContentReader {

    private static Logger logger = Logger.getLogger(JsonContentReader.class);

    private byte[] json;
    private String modified;
    private long size;
    private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    private static ObjectMapper om;
    static {
        om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    public JsonContentReader(Map<String, Object> json) throws JsonProcessingException {
        this(om.writeValueAsBytes(json), "store://");
        modified = (String) json.get(Sjm.MODIFIED);
        size = json.keySet().size();
    }

    public JsonContentReader(byte[] json, String url) {
        super(url);
        this.json = json;
    }

    public byte[] getJson() {
        return this.json;
    }

    public boolean exists() {
        return this.size > 0;
    }

    public long getSize() {
        return !this.exists() ? 0L : this.json.length;
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

    protected ContentReader createReader() {
        return new JsonContentReader(this.json, this.getContentUrl());
    }

    public void getStreamContent(OutputStream os) {
        try {
            StreamUtils.copy(new ByteArrayInputStream(this.json), os);
        } catch (IOException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Failed to copy content to output stream:" + System.lineSeparator() + "accessor: " + this,
                    e);
            }
        }
    }

    protected ReadableByteChannel getDirectReadableChannel() {
        if (!this.exists() && logger.isDebugEnabled()) {
            logger.debug("JSON does not exist");
        } else {
            try (ReadableByteChannel channel = Channels.newChannel(new ByteArrayInputStream(this.json))) {
                return channel;
            } catch (IOException e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Failed to convert json to input stream: " + this, e);
                }
            }
        }
        return null;
    }

    public InputStream getContentInputStream() {
        try {
            return Channels.newInputStream(this.getDirectReadableChannel());
        } catch (ContentIOException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Failed to open stream onto channel:" + System.lineSeparator() + "accessor: " + this, e);
            }
        }

        return null;
    }
}
