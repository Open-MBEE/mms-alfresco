package gov.nasa.jpl.view_repo.util;

import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

public class SerialJSONTokener extends JSONTokener {
    private long character;
    private boolean eof;
    private long index;
    private long line;
    private char previous;
    private Reader reader;
    private boolean usePrevious;

    public SerialJSONTokener(Reader reader) {
        super(reader);
    }

    public SerialJSONTokener(InputStream inputStream) {
        this((Reader)(new InputStreamReader(inputStream)));
    }

    public SerialJSONTokener(String s) {
        this((Reader)(new StringReader(s)));
    }

    @Override
    public Object nextValue() {
        char c = this.nextClean();
        switch(c) {
            case '"':
            case '\'':
                return this.nextString(c);
            case '[':
                this.back();
                return new SerialJSONArray(this);
            case '{':
                this.back();
                return new SerialJSONObject(this);
            default:
                StringBuilder sb;
                for(sb = new StringBuilder(); c >= ' ' && ",:]}/\\\"[{;=#".indexOf(c) < 0; c = this.next()) {
                    sb.append(c);
                }

                this.back();
                String string = sb.toString().trim();
                if ("".equals(string)) {
                    throw this.syntaxError("Missing value");
                } else {
                    return SerialJSONObject.stringToValue(string);
                }
        }
    }
}
