package gov.nasa.jpl.view_repo.actions;

import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.util.EmsConfig;
import org.apache.commons.lang.StringUtils;
import org.omg.SendingContext.RunTime;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
        DOCX("docx"), ODT("odt"), PDF("pdf");

        private String formatName;

        PandocOutputFormat(String name) {
            this.formatName = name;
        }

        public String getFormatName() {
            return formatName;
        }

    }

    public PandocConverter(String outputFileName, String format) {
        // Get the full path for Pandoc executable
        if (outputFileName != null) {
            this.outputFile = outputFileName;
        }

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

    public void setCustomCss(String customCss) throws RuntimeException {
        useCustomCss = true;
        Path customCssPath = Paths.get(PandocConverter.PANDOC_DATA_DIR, "customStyles.css");
        saveStringToFileSystem(customCss, customCssPath);
    }

    private void saveStringToFileSystem(String stringContent, Path filePath) throws RuntimeException {
        if (Utils.isNullOrEmpty(stringContent))
            return;
        if (filePath == null)
            return;
        Path folder = filePath.getParent();
        try {
            try {
                if (!Files.exists(folder))
                    new File(folder.toString()).mkdirs();
            } catch (Throwable ex) {
                throw new RuntimeException(
                    String.format("Failed to create %s directory! %s", folder.toString(), ex.getMessage()));
            }

            File file = new File(filePath.toString());
            BufferedWriter bw = new BufferedWriter(new FileWriter(file));
            bw.write(stringContent);
            bw.close();
        } catch (Throwable ex) {
            throw new RuntimeException(
                String.format("Failed to save %s to filesystem! %s", filePath.toString(), ex.getMessage()));
        }
    }

    public void convert(String inputString) throws InterruptedException, RuntimeException {

        String title = StringUtils.substringBetween(inputString, "<title>", "</title>");
        if (title == null) {
            throw new RuntimeException("No title in HTML");
        } else {
            title += "\n";
        }
        String command = String.format("%s --pdf-engine=%s ", this.pandocExec, this.pdfEngine);
        if (useCustomCss) {
            command += String.format("--css=%s/%s ", PandocConverter.PANDOC_DATA_DIR, "customStyles.css");
        }
        command +=
            String.format(" -o %s/%s.%s", PANDOC_DATA_DIR, this.outputFile, this.pandocOutputFormat.getFormatName());

        int status;

        try {
            Process process = Runtime.getRuntime().exec(command);
            OutputStream out = process.getOutputStream();
            out.write(title.getBytes());
            out.flush();
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
        useCustomCss = false;
    }
}
