package gov.nasa.jpl.view_repo.util;

import org.alfresco.repo.content.AbstractContentReader;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.apache.log4j.Logger;
import org.springframework.util.StreamUtils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class JsonContentReader extends AbstractContentReader implements ContentReader {

    private static Logger logger = Logger.getLogger(JsonContentReader.class);

    private JsonObject json;
    private String modified;
    private long size;
    private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    public JsonContentReader(JsonObject json) {
        this(json, "store://");
        modified = json.get(Sjm.MODIFIED).getAsString();
        size = 0L;
    }

    public JsonContentReader(JsonObject json, String url) {
        super(url);
        this.json = json;
    }

    public JsonObject getJson() {
        return this.json;
    }

    public boolean exists() {
        return this.size > 0;
    }

    public long getSize() {
        return !this.exists() ? 0L : this.size;
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
    	Gson gson = new Gson();
        try {
        	JsonWriter writer = new JsonWriter(new OutputStreamWriter(os, "UTF-8"));
        	gson.toJson(this.json, writer);
        } catch (IOException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Failed to copy content to output stream: \n   accessor: " + this, e);
            }
        }
    }

    protected ReadableByteChannel getDirectReadableChannel() {
        ReadableByteChannel channel = null;
        try {
            if (!this.exists() && logger.isDebugEnabled()) {
                logger.debug("JSON does not exist");
            } else {
            	// TODO: fix this to read json
            	byte [] array = new byte[0];
                channel = Channels.newChannel(new ByteArrayInputStream(array));
                return channel;
            }
        } catch (ContentIOException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Failed to open json channel: " + this, e);
            }
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
        return null;
    }

    public InputStream getContentInputStream() {
        try {
            return Channels.newInputStream(this.getDirectReadableChannel());
        } catch (ContentIOException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Failed to open stream onto channel: \n   accessor: " + this, e);
            }
        }

        return null;
    }
}
