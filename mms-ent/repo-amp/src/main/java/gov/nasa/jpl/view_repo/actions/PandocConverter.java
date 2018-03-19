package gov.nasa.jpl.view_repo.actions;

import gov.nasa.jpl.view_repo.util.EmsConfig;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author Dan Karlsson
 * @version 3.2.2
 * @since 3.2.2
 */
public class PandocConverter {
    private String pandocExec = EmsConfig.get("pandoc.exec");
    private PandocOutputFormat pandocOutputFormat;
    private String outputFile = EmsConfig.get("pandoc.output.filename");
    private String pdfEngine = EmsConfig.get("pandoc.pdfengine");
    private boolean useCustomCss = false;
    public static final String PANDOC_DATA_DIR = EmsConfig.get("pandoc.output.dir");


    public enum PandocOutputFormat {
        DOCX("docx"), PDF("pdf"), LATEX("latex");

        private String formatName;

        PandocOutputFormat(String name) {
            this.formatName = name;
        }

        public String getFormatName() {
            return formatName;
        }

        public static boolean exists(String find) {

            for (PandocOutputFormat c : PandocOutputFormat.values()) {
                if (c.getFormatName().equals(find)) {
                    return true;
                }
            }

            return false;
        }

    }

    public PandocConverter(String outputFileName) {
        // Get the full path for Pandoc executable
        if (outputFileName != null) {
            this.outputFile = outputFileName;
        }
    }

    public PandocConverter(String outputFileName, String extension) {
        // Get the full path for Pandoc executable
        if (outputFileName != null) {
            this.outputFile = outputFileName;
        }

        String format = extension.substring(1);

        if (format.equals(PandocOutputFormat.DOCX.getFormatName())) {
            this.pandocOutputFormat = PandocOutputFormat.DOCX;
        } else {
            this.pandocOutputFormat = PandocOutputFormat.PDF;
        }

    }

    public void setOutputFile(String outputFile) {
        this.outputFile = outputFile;
    }

    public void setOutFormat(PandocOutputFormat pandocOutputFormat) {
        this.pandocOutputFormat = pandocOutputFormat;
    }

    public String getOutputFile() {
        return this.outputFile + "." + this.pandocOutputFormat.getFormatName();
    }

    public void convert(String inputString) {

        String title = StringUtils.substringBetween(inputString, "<title>", "</title>");
        if (title == null) {
            throw new RuntimeException("No title in HTML");
        } else {
            title += "";
        }

        StringBuilder command = new StringBuilder();
        command.append(String.format("%s -f html", this.pandocExec));
        if (this.pandocOutputFormat.getFormatName().equals("pdf")) {
            command.append(String.format(" --pdf-engine=%s", this.pdfEngine));
        }
        command.append(String.format(" -o %s/%s.%s", PANDOC_DATA_DIR, this.outputFile, this.pandocOutputFormat.getFormatName()));

        int status;

        try {
            Process process = Runtime.getRuntime().exec(command.toString());
            OutputStream out = process.getOutputStream();
            out.write(title.getBytes());
            out.flush();
            out.write(inputString.getBytes());
            out.flush();
            out.close();
            status = process.waitFor();
        } catch (InterruptedException ex) {
            throw new RuntimeException("Could not execute: " + command.toString(), ex);
        } catch (IOException ex) {
            throw new RuntimeException("Could not execute. Maybe pandoc is not in PATH?: " + command.toString(), ex);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

        if (status != 0) {
            throw new RuntimeException(
                "Conversion failed with status code: " + status + ". Command executed: " + command.toString());
        }
        useCustomCss = false;
    }
}
