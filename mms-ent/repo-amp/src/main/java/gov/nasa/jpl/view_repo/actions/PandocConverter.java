package gov.nasa.jpl.view_repo.actions;

import gov.nasa.jpl.view_repo.util.EmsConfig;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author Dan Karlsson
 * @version 3.2.2
 * @since 3.2.2
 */
public class PandocConverter {
    private String pandocExec = EmsConfig.get("pandoc.exec");
    private OutputFormat outputFormat;
    private String outputFile = EmsConfig.get("pandoc.output.filename");
    public static final String PANDOC_DATA_DIR = EmsConfig.get("pandoc.output.dir");


    public enum OutputFormat {
        DOCX("docx"), ODT("odt"), PDF("pdf");

        private String formatName;

        OutputFormat(String name) {
            this.formatName = name;
        }

        public String getFormatName() {
            return formatName;
        }

    }

    public PandocConverter(String outputFileName) {
        // Get the full path for Pandoc executable
        if (outputFileName != null) {
            this.outputFile = outputFileName;
        }
        this.outputFormat = OutputFormat.DOCX;
    }

    public void setOutputFile(String outputFile) {
        this.outputFile = outputFile;
    }

    public void setOutFormat(OutputFormat outputFormat) {
        this.outputFormat = outputFormat;
    }

    public String getOutputFile() {
        return this.outputFile + "." + this.outputFormat.getFormatName();
    }

    public void convert(String inputString) {

        String command = String.format("%s -o %s/%s.%s", this.pandocExec, PANDOC_DATA_DIR, this.outputFile,
            this.outputFormat.getFormatName());

        int status;

        try {
            Process process = Runtime.getRuntime().exec(command);
            OutputStream out = process.getOutputStream();
            out.write(inputString.getBytes());
            out.flush();
            out.close();
            status = process.waitFor();
        } catch (InterruptedException ex) {
            throw new RuntimeException("Could not execute: " + command, ex);
        } catch (IOException ex) {
            throw new RuntimeException("Could not execute. Maybe pandoc is not in PATH?: " + command, ex);
        }

        if (status != 0) {
            throw new RuntimeException(
                "Conversion failed with status code: " + status + ". Command executed: " + command);
        }
    }
}
