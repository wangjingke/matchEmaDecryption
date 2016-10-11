package com.wangjingke;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

public class Main {

    public static void main(String[] args) throws IOException {

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

        ProcessFile(String source, String target) {
            this.source = source;
            this.target = target;
        }

        public FileVisitResult visitFile(
                Path fileX, BasicFileAttributes aAttrs
        ) throws IOException {
            if(fileX.toString().endsWith(".zip")) {
                String newPath = Paths.get(target).resolve(Paths.get(source).getFileName()+File.separator+Paths.get(source).relativize(fileX)).toString().replace(".zip", "");
                Decipher.extractFilesToRemote(fileX, newPath);
                Decipher.extractFilesToLocal(newPath);
            }
            System.out.println(fileX);
            return FileVisitResult.CONTINUE;
        }
    }

    public static class Decipher {
        private static final String password = "MATCH";

        public static void extractFilesToRemote(Path filename, String output) {
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

        public static void extractFilesToLocal(String output) throws IOException {
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
