package gov.nasa.jpl.view_repo.actions;

import gov.nasa.jpl.view_repo.util.EmsConfig;
import gov.nasa.jpl.view_repo.util.EmsNodeUtil;
import org.apache.commons.lang.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

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
    private String princeExec = EmsConfig.get("pandoc.princeexec");
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

    public void convert(String inputString, String cssString) {

        String title = StringUtils.substringBetween(inputString, "<title>", "</title>");
        if (title == null) {
            throw new RuntimeException("No title in HTML");
        } else {
            title += System.lineSeparator();
        }

        Path tempFile = null;

        int status;
        boolean withOutputStream = false;
        StringBuilder command = new StringBuilder();

        if (this.pandocOutputFormat.getFormatName().equals("PDF") && this.pdfEngine.contains("prince")
            && this.princeExec != null && !this.princeExec.isEmpty()) {

            try {
                String htmlName = String
                    .format("%s%s.html", Thread.currentThread().getName(), Long.toString(System.currentTimeMillis()));
                tempFile = EmsNodeUtil.saveToFilesystem(htmlName, new ByteArrayInputStream(inputString.getBytes()));
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }

            command.append(String.format("%s %s -o %s/%s.%s", this.princeExec, tempFile.toString(), PANDOC_DATA_DIR, this.outputFile,
                this.pandocOutputFormat.getFormatName()));
        } else {
            command.append(String.format("%s --mathml --variable=title: --from=html+raw_html+simple_tables", this.pandocExec));
            if (!cssString.isEmpty()) {
                try {
                    String cssName = String.format("%s%s.css", Thread.currentThread().getName(), Long.toString(System.currentTimeMillis()));
                    tempFile = EmsNodeUtil.saveToFilesystem(cssName, new ByteArrayInputStream(cssString.getBytes()));
                    command.append(String.format(" --css=%s", tempFile.toString()));
                } catch (Throwable t) {
                    throw new RuntimeException(t);
                }
            }
            if (this.pandocOutputFormat.getFormatName().equals("PDF")) {
                command.append(String.format(" --pdf-engine=%s", this.pdfEngine));
            }
            command.append(String.format(" -o %s/%s.%s", PANDOC_DATA_DIR, this.outputFile, this.pandocOutputFormat.getFormatName()));
            withOutputStream = true;
        }

        try {
            Process process = Runtime.getRuntime().exec(command.toString());
            if (withOutputStream) {
                OutputStream out = process.getOutputStream();
                out.write(inputString.getBytes());
                out.flush();
                out.close();
            }
            status = process.waitFor();
        } catch (InterruptedException ex) {
            throw new RuntimeException("Could not execute: " + command.toString(), ex);
        } catch (IOException ex) {
            throw new RuntimeException("Could not execute. Maybe pandoc is not in PATH?: " + command.toString(), ex);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        } finally {
            if (tempFile != null && !tempFile.toFile().delete()) {
                throw new RuntimeException("Could not delete temporary css file");
            }
        }

        if (status != 0) {
            throw new RuntimeException(
                "Conversion failed with status code: " + status + ". Command executed: " + command.toString());
        }
    }
}
