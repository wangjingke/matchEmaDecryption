package com.wangjingke;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class Main {

    public static void main(String[] args) throws IOException, NullPointerException {

        String sourceDir = args[0];
        String outputDir = args[1];

        // check the existence of output dir and create one if none exist
        File output = new File(outputDir);
        output.mkdirs();
        FileVisitor<Path> fileProcessor = new ProcessFile(sourceDir, output.getAbsolutePath());
        Files.walkFileTree(Paths.get(sourceDir).toAbsolutePath(), fileProcessor);

    }

    public static class ProcessFile extends SimpleFileVisitor<Path> {
        private final String source;
        private final String target;
        private final Pattern studyDate = Pattern.compile("[0-9]{4}-[0-9]{2}-[0-9]{2}");
        private String password;

        ProcessFile(String source, String target) {
            this.source = source;
            this.target = target;
        }

        public FileVisitResult visitFile(
                Path fileX, BasicFileAttributes aAttrs
        ) throws IOException {
            String newPath = Paths.get(target).resolve(Paths.get(source).getFileName()+File.separator+Paths.get(source).relativize(fileX)).toString().replace(".zip", "");
            // choose password based on the date
            Matcher passwordPicker = studyDate.matcher(newPath);
            String dateX = null;
            if (passwordPicker.find()){
                dateX = passwordPicker.group(0);
            }
            if(dateX == null){
                return FileVisitResult.CONTINUE;
            } else if (dateX.compareTo("2016-11-27") <= 0) {
                password = "MATCH";
            } else {
                password = "7Qv8e3PfaXF25DLb";
            }
            try {
                if(fileX.toString().endsWith(".zip") || fileX.toString().endsWith(".zip.uploaded")) {
                    // decipher at first level
                    Decipher.extractFilesToRemote(fileX, newPath, password);
                    // decipher all following subfolders
                    Decipher.extractFilesToLocal(newPath, password);
                } else {
                    Paths.get(newPath).getParent().toFile().mkdirs();
                    Files.copy(fileX, Paths.get(newPath), REPLACE_EXISTING);
                }
            } catch (NullPointerException npe) {
                return FileVisitResult.CONTINUE;
            }

            System.out.println(fileX);
            return FileVisitResult.CONTINUE;
        }
    }

    public static class Decipher {

        public static void extractFilesToRemote(Path filename, String output, String password) {
            try {
                ZipFile zipFile = new ZipFile(filename.toString());
                if (zipFile.isEncrypted()) {
                    zipFile.setPassword(password);
                }
                List fileHeaderList = zipFile.getFileHeaders();
                // Loop through the file headers
                for (int i = 0; i < fileHeaderList.size(); i++) {
                    FileHeader fileHeader = (FileHeader)fileHeaderList.get(i);
                    // Extract the file to the specified destination
                    zipFile.extractFile(fileHeader, output);
                }
            } catch (ZipException e) {
                    e.printStackTrace();
            }
        }

        public static void extractFilesToLocal(String output, String password) throws IOException {
            File remoteFolder = new File(output);
            File[] remoteList = remoteFolder.listFiles();
            for (int j=0; j < remoteList.length; j++) {
                if (remoteList[j].getName().endsWith(".zip")) {
                    try {
                        ZipFile zipFile = new ZipFile(output+File.separator+remoteList[j].getName());
                        if (zipFile.isEncrypted()) {
                            zipFile.setPassword(password);
                        }
                        List fileHeaderList = zipFile.getFileHeaders();
                        // Loop through the file headers
                        for (int i = 0; i < fileHeaderList.size(); i++) {
                            FileHeader fileHeader = (FileHeader)fileHeaderList.get(i);
                            // Extract the file to the specified destination
                            zipFile.extractFile(fileHeader, output);
                        }
                    } catch (ZipException e) {
                        e.printStackTrace();
                    }
                    Files.delete(Paths.get(output+File.separator+remoteList[j].getName()));
                }
            }
        }
    }
}
