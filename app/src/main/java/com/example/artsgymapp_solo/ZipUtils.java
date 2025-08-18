package com.example.artsgymapp_solo;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream; 
import java.util.zip.ZipOutputStream;

public class ZipUtils {
    private static final int BUFFER_SIZE = 4096; 

    public static void zipDirectory(File sourceDir, OutputStream destinationZipFile) throws IOException {
        if (!sourceDir.isDirectory()) {
            throw new IOException("Source is not a directory: " + sourceDir);
        }
        
        try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(destinationZipFile))) {
            
            addDirectoryContentsToZip(sourceDir, "", zos);
        }
    }

    private static void addDirectoryContentsToZip(File sourceDirectory, String currentPathInZip, ZipOutputStream zos) throws IOException {
        File[] files = sourceDirectory.listFiles();
        if (files == null) {
            
            if (!currentPathInZip.isEmpty()) { 
                ZipEntry zipEntry = new ZipEntry(currentPathInZip.endsWith("/") ? currentPathInZip : currentPathInZip + "/");
                zos.putNextEntry(zipEntry);
                zos.closeEntry();
            }
            return;
        }

        for (File file : files)
        {
            String entryName = currentPathInZip.isEmpty() ? file.getName() : currentPathInZip + "/" + file.getName();

            if (file.isDirectory()) {
                
                addDirectoryContentsToZip(file, entryName, zos); 
            } else {
                
                ZipEntry zipEntry = new ZipEntry(entryName);
                zos.putNextEntry(zipEntry);
                try (FileInputStream fis = new FileInputStream(file);
                     BufferedInputStream bis = new BufferedInputStream(fis, BUFFER_SIZE)) {
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int bytesRead;
                    while ((bytesRead = bis.read(buffer, 0, BUFFER_SIZE)) != -1) {
                        zos.write(buffer, 0, bytesRead);
                    }
                } finally {
                    zos.closeEntry(); 
                }
            }
        }
    }

    public static void unzip(InputStream zipFileInputStream, File destinationDirectory) throws IOException {
        if (!destinationDirectory.exists()) {
            if (!destinationDirectory.mkdirs()) {
                throw new IOException("Failed to create destination directory: " + destinationDirectory.getAbsolutePath());
            }
        } else { 
            deleteRecursive(destinationDirectory);
            if (!destinationDirectory.mkdirs()) { 
                throw new IOException("Failed to recreate destination directory after clearing: " + destinationDirectory.getAbsolutePath());
            }
        }

        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(zipFileInputStream))) {
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                File newFile = newFile(destinationDirectory, zipEntry);
                
                if (zipEntry.isDirectory()) {
                    if (!newFile.isDirectory() && !newFile.mkdirs()) {
                        throw new IOException("Failed to create directory " + newFile);
                    }
                } else {
                    
                    File parent = newFile.getParentFile();
                    if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
                        throw new IOException("Failed to create directory " + parent);
                    }

                    
                    try (FileOutputStream fos = new FileOutputStream(newFile);
                         BufferedOutputStream bos = new BufferedOutputStream(fos, BUFFER_SIZE)) { 
                        byte[] buffer = new byte[BUFFER_SIZE];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            bos.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
                zipEntry = zis.getNextEntry();
            }
        }
    }

    private static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());
        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }
        return destFile;
    }

    public static void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            File[] children = fileOrDirectory.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        fileOrDirectory.delete();
    }
}