package gov.nasa.jpl.view_repo.util;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

/*
Copyright (c) 2002 JSON.org

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

The Software shall be used for Good, not Evil.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

/**
 * A SerialJSONTokener takes a source string and extracts characters and tokens from
 * it. It is used by the SerialJSONObject and SerialJSONArray constructors to parse
 * JSON source strings.
 * @author JSON.org
 * @version 2014-05-03
 */
public class SerialJSONTokener {
    private int index;
    private Reader reader;
    private char lastChar;
    private boolean useLastChar;

    public SerialJSONTokener(Reader var1) {
        this.reader = (Reader)(var1.markSupported() ? var1 : new BufferedReader(var1));
        this.useLastChar = false;
        this.index = 0;
    }

    public SerialJSONTokener(String var1) {
        this((Reader)(new StringReader(var1)));
    }

    public void back() throws JSONException {
        if (!this.useLastChar && this.index > 0) {
            --this.index;
            this.useLastChar = true;
        } else {
            throw new JSONException("Stepping back two steps is not supported");
        }
    }

    public static int dehexchar(char var0) {
        if (var0 >= '0' && var0 <= '9') {
            return var0 - 48;
        } else if (var0 >= 'A' && var0 <= 'F') {
            return var0 - 55;
        } else {
            return var0 >= 'a' && var0 <= 'f' ? var0 - 87 : -1;
        }
    }

    public boolean more() throws JSONException {
        char var1 = this.next();
        if (var1 == 0) {
            return false;
        } else {
            this.back();
            return true;
        }
    }

    public char next() throws JSONException {
        if (this.useLastChar) {
            this.useLastChar = false;
            if (this.lastChar != 0) {
                ++this.index;
            }

            return this.lastChar;
        } else {
            int var1;
            try {
                var1 = this.reader.read();
            } catch (IOException var3) {
                throw new JSONException(var3);
            }

            if (var1 <= 0) {
                this.lastChar = 0;
                return '\u0000';
            } else {
                ++this.index;
                this.lastChar = (char)var1;
                return this.lastChar;
            }
        }
    }

    public char next(char var1) throws JSONException {
        char var2 = this.next();
        if (var2 != var1) {
            throw this.syntaxError("Expected '" + var1 + "' and instead saw '" + var2 + "'");
        } else {
            return var2;
        }
    }

    public String next(int var1) throws JSONException {
        if (var1 == 0) {
            return "";
        } else {
            char[] var2 = new char[var1];
            int var3 = 0;
            if (this.useLastChar) {
                this.useLastChar = false;
                var2[0] = this.lastChar;
                var3 = 1;
            }

            int var4;
            try {
                while(var3 < var1 && (var4 = this.reader.read(var2, var3, var1 - var3)) != -1) {
                    var3 += var4;
                }
            } catch (IOException var5) {
                throw new JSONException(var5);
            }

            this.index += var3;
            if (var3 < var1) {
                throw this.syntaxError("Substring bounds error");
            } else {
                this.lastChar = var2[var1 - 1];
                return new String(var2);
            }
        }
    }

    public char nextClean() throws JSONException {
        char var1;
        do {
            var1 = this.next();
        } while(var1 != 0 && var1 <= ' ');

        return var1;
    }

    public String nextString(char var1) throws JSONException {
        StringBuffer var3 = new StringBuffer();

        while(true) {
            char var2 = this.next();
            switch(var2) {
                case '\u0000':
                case '\n':
                case '\r':
                    throw this.syntaxError("Unterminated string");
                case '\\':
                    var2 = this.next();
                    switch(var2) {
                        case 'b':
                            var3.append('\b');
                            continue;
                        case 'c':
                        case 'd':
                        case 'e':
                        case 'g':
                        case 'h':
                        case 'i':
                        case 'j':
                        case 'k':
                        case 'l':
                        case 'm':
                        case 'o':
                        case 'p':
                        case 'q':
                        case 's':
                        case 'v':
                        case 'w':
                        default:
                            var3.append(var2);
                            continue;
                        case 'f':
                            var3.append('\f');
                            continue;
                        case 'n':
                            var3.append('\n');
                            continue;
                        case 'r':
                            var3.append('\r');
                            continue;
                        case 't':
                            var3.append('\t');
                            continue;
                        case 'u':
                            var3.append((char)Integer.parseInt(this.next((int)4), 16));
                            continue;
                        case 'x':
                            var3.append((char)Integer.parseInt(this.next((int)2), 16));
                            continue;
                    }
                default:
                    if (var2 == var1) {
                        return var3.toString();
                    }

                    var3.append(var2);
            }
        }
    }

    public String nextTo(char var1) throws JSONException {
        StringBuffer var2 = new StringBuffer();

        while(true) {
            char var3 = this.next();
            if (var3 == var1 || var3 == 0 || var3 == '\n' || var3 == '\r') {
                if (var3 != 0) {
                    this.back();
                }

                return var2.toString().trim();
            }

            var2.append(var3);
        }
    }

    public String nextTo(String var1) throws JSONException {
        StringBuffer var3 = new StringBuffer();

        while(true) {
            char var2 = this.next();
            if (var1.indexOf(var2) >= 0 || var2 == 0 || var2 == '\n' || var2 == '\r') {
                if (var2 != 0) {
                    this.back();
                }

                return var3.toString().trim();
            }

            var3.append(var2);
        }
    }

    public Object nextValue() throws JSONException {
        char var1 = this.nextClean();
        switch(var1) {
            case '"':
            case '\'':
                return this.nextString(var1);
            case '(':
            case '[':
                this.back();
                return new SerialJSONArray(this);
            case '{':
                this.back();
                return new SerialJSONObject(this);
            default:
                StringBuffer var3;
                for(var3 = new StringBuffer(); var1 >= ' ' && ",:]}/\\\"[{;=#".indexOf(var1) < 0; var1 = this.next()) {
                    var3.append(var1);
                }

                this.back();
                String var2 = var3.toString().trim();
                if (var2.equals("")) {
                    throw this.syntaxError("Missing value");
                } else {
                    return SerialJSONObject.stringToValue(var2);
                }
        }
    }

    public char skipTo(char var1) throws JSONException {
        char var2;
        try {
            int var3 = this.index;
            this.reader.mark(2147483647);

            do {
                var2 = this.next();
                if (var2 == 0) {
                    this.reader.reset();
                    this.index = var3;
                    return var2;
                }
            } while(var2 != var1);
        } catch (IOException var4) {
            throw new JSONException(var4);
        }

        this.back();
        return var2;
    }

    public JSONException syntaxError(String var1) {
        return new JSONException(var1 + this.toString());
    }

    public String toString() {
        return " at character " + this.index;
    }
}
