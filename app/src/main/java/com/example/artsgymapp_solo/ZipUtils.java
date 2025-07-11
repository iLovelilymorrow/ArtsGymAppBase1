package com.example.artsgymapp_solo;

import android.util.Log;
import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream; // Needed for unzip
import java.util.zip.ZipOutputStream;

public class ZipUtils {

    private static final String TAG = "ZipUtils";
    private static final int BUFFER_SIZE = 4096; // 4KB buffer

    /**
     * Compresses a directory (and its subdirectories) into a ZIP file.
     *
     * @param sourceDir          The directory to compress.
     * @param destinationZipFile The output ZIP file stream.
     * @throws IOException If an I/O error occurs.
     */
    public static void zipDirectory(File sourceDir, OutputStream destinationZipFile) throws IOException {
        if (!sourceDir.isDirectory()) {
            throw new IOException("Source is not a directory: " + sourceDir);
        }
        // When zipping, we want the *contents* of sourceDir to be at the root of the zip.
        // So, the initial parentPath for items inside sourceDir should be "" or just their name.
        try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(destinationZipFile))) {
            // Corrected: Pass "" as initial parentPath for contents of sourceDir
            addDirectoryContentsToZip(sourceDir, "", zos);
        } catch (IOException e) {
            Log.e(TAG, "Error zipping directory: " + sourceDir.getAbsolutePath(), e);
            throw e; // Re-throw to be handled by caller
        }
    }

    /**
     * Recursively adds the *contents* of a source directory to the ZipOutputStream.
     *
     * @param sourceDirectory      The current directory whose contents are to be added.
     * @param currentPathInZip  The path of this file/directory within the ZIP archive.
     * @param zos         The ZipOutputStream.
     * @throws IOException If an I/O error occurs.
     */
    private static void addDirectoryContentsToZip(File sourceDirectory, String currentPathInZip, ZipOutputStream zos) throws IOException {
        File[] files = sourceDirectory.listFiles();
        if (files == null) {
            Log.w(TAG, "Could not list files in directory: " + sourceDirectory.getAbsolutePath() + " (might be empty or no permissions)");
            // If the source directory itself is empty, and we are not at the root,
            // we might want to create an entry for the empty directory itself.
            if (!currentPathInZip.isEmpty()) { // Only add if it's not the root virtual path
                ZipEntry zipEntry = new ZipEntry(currentPathInZip.endsWith("/") ? currentPathInZip : currentPathInZip + "/");
                zos.putNextEntry(zipEntry);
                zos.closeEntry();
                Log.d(TAG, "Adding empty directory to ZIP: " + zipEntry.getName());
            }
            return;
        }

        for (File file : files) {
            // Construct the entry name: if currentPathInZip is empty, it's just file.getName()
            // Otherwise, it's currentPathInZip + "/" + file.getName()
            String entryName = currentPathInZip.isEmpty() ? file.getName() : currentPathInZip + "/" + file.getName();

            if (file.isDirectory()) {
                Log.d(TAG, "Adding directory to ZIP: " + entryName);
                addDirectoryContentsToZip(file, entryName, zos); // Recursive call for subdirectory contents
            } else {
                Log.d(TAG, "Adding file to ZIP: " + entryName);
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
                    zos.closeEntry(); // Important to close each entry
                }
            }
        }
    }

    // --- UNZIP FUNCTIONALITY ---

    /**
     * Unzips a ZIP file from an InputStream to a destination directory.
     * The destination directory will be cleared if it exists.
     *
     * @param zipFileInputStream   InputStream of the ZIP file.
     * @param destinationDirectory The directory where files will be extracted.
     * @throws IOException If an I/O error occurs.
     */
    public static void unzip(InputStream zipFileInputStream, File destinationDirectory) throws IOException {
        if (!destinationDirectory.exists()) {
            if (!destinationDirectory.mkdirs()) {
                throw new IOException("Failed to create destination directory: " + destinationDirectory.getAbsolutePath());
            }
        } else { // Clear the destination directory if it exists to avoid conflicts
            deleteRecursive(destinationDirectory);
            if (!destinationDirectory.mkdirs()) { // Recreate after deleting
                throw new IOException("Failed to recreate destination directory after clearing: " + destinationDirectory.getAbsolutePath());
            }
        }

        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(zipFileInputStream))) {
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                File newFile = newFile(destinationDirectory, zipEntry);
                Log.d(TAG, "Unzipping: " + newFile.getAbsolutePath());
                if (zipEntry.isDirectory()) {
                    if (!newFile.isDirectory() && !newFile.mkdirs()) {
                        throw new IOException("Failed to create directory " + newFile);
                    }
                } else {
                    // Create parent directories if they don't exist
                    File parent = newFile.getParentFile();
                    if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
                        throw new IOException("Failed to create directory " + parent);
                    }

                    // Write file content
                    try (FileOutputStream fos = new FileOutputStream(newFile);
                         BufferedOutputStream bos = new BufferedOutputStream(fos, BUFFER_SIZE)) { // Use BufferedOutputStream for writing
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

    /**
     * Helper to create a new file within the destination directory, protecting against Zip Slip.
     * @param destinationDir The directory where files are being extracted.
     * @param zipEntry The current zip entry.
     * @return The new File object.
     * @throws IOException If the entry is outside the target directory.
     */
    private static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());
        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }
        return destFile;
    }

    /**
     * Deletes a file or a directory and its contents recursively.
     *
     * @param fileOrDirectory The file or directory to delete.
     */
    public static void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            File[] children = fileOrDirectory.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        if (!fileOrDirectory.delete()) {
            Log.e(TAG, "Failed to delete: " + fileOrDirectory.getAbsolutePath());
        } else {
            Log.d(TAG, "Successfully deleted: " + fileOrDirectory.getAbsolutePath());
        }
    }
}