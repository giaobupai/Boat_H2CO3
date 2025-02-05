package org.koishi.launcher.h2co3.core.utils.file;

import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.system.Os;
import android.text.TextUtils;
import android.util.Log;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.koishi.launcher.h2co3.core.utils.Lang;
import org.koishi.launcher.h2co3.core.utils.Logging;
import org.koishi.launcher.h2co3.core.utils.StringUtils;
import org.koishi.launcher.h2co3.core.utils.function.ExceptionalConsumer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.logging.Level;

public final class FileTools {

    private FileTools() {
    }

    /**
     * 从Uri获取完整路径
     *
     * @param context 上下文
     * @param uri     Uri对象
     * @return 完整路径字符串
     */
    public static String getFullPathFromUri(Context context, Uri uri) {
        String fullPath = null;
        if (DocumentsContract.isDocumentUri(context, uri)) { // 判断Uri是否为文档类型的Uri
            String documentId = DocumentsContract.getDocumentId(uri); // 获取文档的ID
            String[] split = documentId.split(":"); // 以冒号分割文档ID
            String type = split[0]; // 获取类型
            if ("primary".equalsIgnoreCase(type)) { // 如果类型为primary
                fullPath = Environment.getExternalStorageDirectory() + "/" + split[1]; // 获取外部存储路径
            }
        }
        if (fullPath == null) { // 如果完整路径为空
            fullPath = uri.getPath(); // 获取Uri的路径
        }
        return fullPath; // 返回完整路径
    }

    public static boolean canCreateDirectory(String path) {
        try {
            return canCreateDirectory(Paths.get(path));
        } catch (InvalidPathException e) {
            return false;
        }
    }

    public static boolean canCreateDirectory(Path path) {
        if (Files.isDirectory(path)) return true;
        else if (Files.exists(path)) return false;
        else {
            Path lastPath = path; // always not exist
            path = path.getParent();
            // find existent ancestor
            while (path != null && !Files.exists(path)) {
                lastPath = path;
                path = path.getParent();
            }
            if (path == null) return false; // all ancestors are nonexistent
            if (!Files.isDirectory(path)) return false; // ancestor is file
            try {
                Files.createDirectory(lastPath); // check permission
                Files.delete(lastPath); // safely delete empty directory
                return true;
            } catch (IOException e) {
                return false;
            }
        }
    }

    public static String getNameWithoutExtension(String fileName) {
        return StringUtils.substringBeforeLast(fileName, '.');
    }

    public static String getNameWithoutExtension(File file) {
        return StringUtils.substringBeforeLast(file.getName(), '.');
    }

    public static String getNameWithoutExtension(Path file) {
        return StringUtils.substringBeforeLast(getName(file), '.');
    }

    public static String getExtension(File file) {
        return StringUtils.substringAfterLast(file.getName(), '.');
    }

    public static String getExtension(Path file) {
        return StringUtils.substringAfterLast(getName(file), '.');
    }

    /**
     * This method is for normalizing ZipPath since Path.normalize of ZipFileSystem does not work properly.
     */
    public static String normalizePath(String path) {
        return StringUtils.addPrefix(StringUtils.removeSuffix(path, "/", "\\"), "/");
    }

    public static String getName(Path path) {
        if (path.getFileName() == null) return "";
        return StringUtils.removeSuffix(path.getFileName().toString(), "/", "\\");
    }

    public static String getName(Path path, String candidate) {
        if (path.getFileName() == null) return candidate;
        else return getName(path);
    }

    public static String readText(File file) throws IOException {
        return readText(file, StandardCharsets.UTF_8);
    }

    public static String readText(File file, Charset charset) throws IOException {
        return new String(Files.readAllBytes(file.toPath()), charset);
    }

    public static String readText(Path file) throws IOException {
        return readText(file, StandardCharsets.UTF_8);
    }

    public static String readText(Path file, Charset charset) throws IOException {
        return new String(Files.readAllBytes(file), charset);
    }

    public static void writeTextWithAppendMode(File file, String text) throws IOException {
        writeBytesWithAppendMode(file.toPath(), text.getBytes(StandardCharsets.UTF_8));
    }

    public static void writeTextWithAppendMode(Path file, String text) throws IOException {
        writeBytesWithAppendMode(file, text.getBytes(StandardCharsets.UTF_8));
    }

    public static void writeBytesWithAppendMode(Path file, byte[] data) throws IOException {
        Files.createDirectories(file.getParent());
        Files.write(file, data, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    /**
     * Write plain text to file. Characters are encoded into bytes using UTF-8.
     * <p>
     * We don't care about platform difference of line separator. Because readText accept all possibilities of line separator.
     * It will create the file if it does not exist, or truncate the existing file to empty for rewriting.
     * All characters in text will be written into the file in binary format. Existing data will be erased.
     *
     * @param file the path to the file
     * @param text the text being written to file
     * @throws IOException if an I/O error occurs
     */
    public static void writeText(File file, String text) throws IOException {
        writeText(file, text, StandardCharsets.UTF_8);
    }

    /**
     * Write plain text to file. Characters are encoded into bytes using UTF-8.
     * <p>
     * We don't care about platform difference of line separator. Because readText accept all possibilities of line separator.
     * It will create the file if it does not exist, or truncate the existing file to empty for rewriting.
     * All characters in text will be written into the file in binary format. Existing data will be erased.
     *
     * @param file the path to the file
     * @param text the text being written to file
     * @throws IOException if an I/O error occurs
     */
    public static void writeText(Path file, String text) throws IOException {
        writeText(file, text, StandardCharsets.UTF_8);
    }

    /**
     * Write plain text to file.
     * <p>
     * We don't care about platform difference of line separator. Because readText accept all possibilities of line separator.
     * It will create the file if it does not exist, or truncate the existing file to empty for rewriting.
     * All characters in text will be written into the file in binary format. Existing data will be erased.
     *
     * @param file    the path to the file
     * @param text    the text being written to file
     * @param charset the charset to use for encoding
     * @throws IOException if an I/O error occurs
     */
    public static void writeText(File file, String text, Charset charset) throws IOException {
        writeBytes(file, text.getBytes(charset));
    }

    /**
     * Write plain text to file.
     * <p>
     * We don't care about platform difference of line separator. Because readText accept all possibilities of line separator.
     * It will create the file if it does not exist, or truncate the existing file to empty for rewriting.
     * All characters in text will be written into the file in binary format. Existing data will be erased.
     *
     * @param file    the path to the file
     * @param text    the text being written to file
     * @param charset the charset to use for encoding
     * @throws IOException if an I/O error occurs
     */
    public static void writeText(Path file, String text, Charset charset) throws IOException {
        writeBytes(file, text.getBytes(charset));
    }

    /**
     * Write byte array to file.
     * It will create the file if it does not exist, or truncate the existing file to empty for rewriting.
     * All bytes in byte array will be written into the file in binary format. Existing data will be erased.
     *
     * @param file the path to the file
     * @param data the data being written to file
     * @throws IOException if an I/O error occurs
     */
    public static void writeBytes(File file, byte[] data) throws IOException {
        writeBytes(file.toPath(), data);
    }

    /**
     * Write byte array to file.
     * It will create the file if it does not exist, or truncate the existing file to empty for rewriting.
     * All bytes in byte array will be written into the file in binary format. Existing data will be erased.
     *
     * @param file the path to the file
     * @param data the data being written to file
     * @throws IOException if an I/O error occurs
     */
    public static void writeBytes(Path file, byte[] data) throws IOException {
        Files.createDirectories(file.getParent());
        Files.write(file, data);
    }

    public static void deleteDirectory(File directory)
            throws IOException {
        if (!directory.exists())
            return;

        if (!isSymlink(directory))
            cleanDirectory(directory);

        if (!directory.delete()) {
            String message = "Unable to delete directory " + directory + ".";

            throw new IOException(message);
        }
    }

    public static boolean deleteDirectoryQuietly(File directory) {
        try {
            deleteDirectory(directory);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Copy directory.
     * Paths of all files relative to source directory will be the same as the ones relative to destination directory.
     *
     * @param src  the source directory.
     * @param dest the destination directory, which will be created if not existing.
     * @throws IOException if an I/O error occurs.
     */
    public static void copyDirectory(Path src, Path dest) throws IOException {
        copyDirectory(src, dest, path -> true);
    }

    public static void copyDirectory(Path src, Path dest, Predicate<String> filePredicate) throws IOException {
        Files.walkFileTree(src, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (!filePredicate.test(src.relativize(file).toString())) {
                    return FileVisitResult.SKIP_SUBTREE;
                }

                Path destFile = dest.resolve(src.relativize(file).toString());
                Files.copy(file, destFile, StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (!filePredicate.test(src.relativize(dir).toString())) {
                    return FileVisitResult.SKIP_SUBTREE;
                }

                Path destDir = dest.resolve(src.relativize(dir).toString());
                Files.createDirectories(destDir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static void cleanDirectory(File directory)
            throws IOException {
        if (!directory.exists()) {
            if (!makeDirectory(directory))
                throw new IOException("Failed to create directory: " + directory);
            return;
        }

        if (!directory.isDirectory()) {
            String message = directory + " is not a directory";
            throw new IllegalArgumentException(message);
        }

        File[] files = directory.listFiles();
        if (files == null)
            throw new IOException("Failed to list contents of " + directory);

        IOException exception = null;
        for (File file : files)
            try {
                forceDelete(file);
            } catch (IOException ioe) {
                exception = ioe;
            }

        if (null != exception)
            throw exception;
    }

    public static boolean cleanDirectoryQuietly(File directory) {
        try {
            cleanDirectory(directory);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static void forceDelete(File file)
            throws IOException {
        if (file.isDirectory()) {
            deleteDirectory(file);
        } else {
            boolean filePresent = file.exists();
            if (!file.delete()) {
                if (!filePresent)
                    throw new FileNotFoundException("File does not exist: " + file);
                throw new IOException("Unable to delete file: " + file);
            }
        }
    }

    public static boolean isSymlink(File file)
            throws IOException {
        Objects.requireNonNull(file, "File must not be null");
        if (File.separatorChar == '\\')
            return false;
        File fileInCanonicalDir;
        if (file.getParent() == null)
            fileInCanonicalDir = file;
        else {
            File canonicalDir = file.getParentFile().getCanonicalFile();
            fileInCanonicalDir = new File(canonicalDir, file.getName());
        }

        return !fileInCanonicalDir.getCanonicalFile().equals(fileInCanonicalDir.getAbsoluteFile());
    }

    public static void copyFile(File srcFile, File destFile)
            throws IOException {
        Objects.requireNonNull(srcFile, "Source must not be null");
        Objects.requireNonNull(destFile, "Destination must not be null");
        if (!srcFile.exists())
            throw new FileNotFoundException("Source '" + srcFile + "' does not exist");
        if (srcFile.isDirectory())
            throw new IOException("Source '" + srcFile + "' exists but is a directory");
        if (srcFile.getCanonicalPath().equals(destFile.getCanonicalPath()))
            throw new IOException("Source '" + srcFile + "' and destination '" + destFile + "' are the same");
        File parentFile = destFile.getParentFile();
        if (parentFile != null && !FileTools.makeDirectory(parentFile))
            throw new IOException("Destination '" + parentFile + "' directory cannot be created");
        if (destFile.exists() && !destFile.canWrite())
            throw new IOException("Destination '" + destFile + "' exists but is read-only");

        Files.copy(srcFile.toPath(), destFile.toPath(), StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
    }

    public static void copyFile(Path srcFile, Path destFile)
            throws IOException {
        Objects.requireNonNull(srcFile, "Source must not be null");
        Objects.requireNonNull(destFile, "Destination must not be null");
        if (!Files.exists(srcFile))
            throw new FileNotFoundException("Source '" + srcFile + "' does not exist");
        if (Files.isDirectory(srcFile))
            throw new IOException("Source '" + srcFile + "' exists but is a directory");
        Path parentFile = destFile.getParent();
        Files.createDirectories(parentFile);
        if (Files.exists(destFile) && !Files.isWritable(destFile))
            throw new IOException("Destination '" + destFile + "' exists but is read-only");

        Files.copy(srcFile, destFile, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
    }

    public static void moveFile(File srcFile, File destFile) throws IOException {
        copyFile(srcFile, destFile);
        srcFile.delete();
    }

    public static boolean makeDirectory(File directory) {
        directory.mkdirs();
        return directory.isDirectory();
    }

    public static boolean makeFile(File file) {
        return makeDirectory(Objects.requireNonNull(file.getAbsoluteFile().getParentFile())) && (file.exists() || Lang.test(file::createNewFile));
    }

    public static List<File> listFilesByExtension(File file, String extension) {
        List<File> result = new ArrayList<>();
        File[] files = file.listFiles();
        if (files != null)
            for (File it : files)
                if (extension.equals(getExtension(it)))
                    result.add(it);
        return result;
    }

    /**
     * Tests whether the file is convertible to [java.nio.file.Path] or not.
     *
     * @param file the file to be tested
     * @return true if the file is convertible to Path.
     */
    public static boolean isValidPath(File file) {
        try {
            file.toPath();
            return true;
        } catch (InvalidPathException ignored) {
            return false;
        }
    }

    public static Optional<Path> tryGetPath(String first, String... more) {
        if (first == null) return Optional.empty();
        try {
            return Optional.of(Paths.get(first, more));
        } catch (InvalidPathException e) {
            return Optional.empty();
        }
    }

    public static Path tmpSaveFile(Path file) {
        return file.toAbsolutePath().resolveSibling("." + file.getFileName().toString() + ".tmp");
    }

    public static void saveSafely(Path file, String content) throws IOException {
        Path tmpFile = tmpSaveFile(file);
        try (BufferedWriter writer = Files.newBufferedWriter(tmpFile, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)) {
            writer.write(content);
        }

        try {
            if (Files.exists(file) && Files.getAttribute(file, "dos:hidden") == Boolean.TRUE) {
                Files.setAttribute(tmpFile, "dos:hidden", true);
            }
        } catch (Throwable ignored) {
        }

        Files.move(tmpFile, file, StandardCopyOption.REPLACE_EXISTING);
    }

    public static void saveSafely(Path file, ExceptionalConsumer<? super OutputStream, IOException> action) throws IOException {
        Path tmpFile = tmpSaveFile(file);

        try (OutputStream os = Files.newOutputStream(tmpFile, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)) {
            action.accept(os);
        }

        try {
            if (Files.exists(file) && Files.getAttribute(file, "dos:hidden") == Boolean.TRUE) {
                Files.setAttribute(tmpFile, "dos:hidden", true);
            }
        } catch (Throwable ignored) {
        }

        Files.move(tmpFile, file, StandardCopyOption.REPLACE_EXISTING);
    }

    public static void deleteFile(File file) {
        if (file.exists()) {
            if (file.isFile()) {
                file.delete();
                return;
            }
            //Folder then
            File[] files = file.listFiles();
            for (File value : Objects.requireNonNull(files)) {
                deleteFile(value);
            }
            file.delete();
        }
    }

    /**
     * 【重写数据到文件】
     **/
    public static void writeData(String path, String fileData) {
        try {
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                File file = new File(path);
                try (FileOutputStream out = new FileOutputStream(file, false)) {
                    out.write(fileData.getBytes(StandardCharsets.UTF_8));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 【续写数据到文件】
     **/
    public static void writtenFileData(String path, String data) {
        try {
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                File file = new File(path);

                //按读写方式
                try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
                    //将文件指针移到文件尾
                    raf.seek(file.length());
                    //将数据写入到文件中
                    raf.write(data.getBytes(StandardCharsets.UTF_8));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getFileName(Context ctx, Uri uri) {
        Cursor c = ctx.getContentResolver().query(uri, null, null, null, null);
        if (c == null)
            return uri.getLastPathSegment(); // idk myself but it happens on asus file manager
        c.moveToFirst();
        int columnIndex = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        if (columnIndex == -1) return uri.getLastPathSegment();
        String fileName = c.getString(columnIndex);
        c.close();
        return fileName;
    }

    /**
     * 【读取文件内容】
     **/
    public static String readFileContent(String path) {
        try {
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                File file = new File(path);
                byte[] buffer = new byte[32 * 1024];

                try (FileInputStream fis = new FileInputStream(file)) {
                    StringBuilder sb = new StringBuilder();
                    int len;
                    while ((len = fis.read(buffer)) > 0) {
                        sb.append(new String(buffer, 0, len));
                    }
                    return sb.toString();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 【判断文件是否存在】
     **/
    public static boolean isFileExists(String fileName) {
        File file = new File(fileName);
        return file.exists();
    }

    /**
     * 【判断文件夹是否存在】
     **/
    public static boolean isFolderExists(String directoryPath) {
        if (TextUtils.isEmpty(directoryPath)) {
            return false;
        }
        File dire = new File(directoryPath);
        //如果是文件夹并且文件夹存在则返回true
        return (dire.exists() && dire.isDirectory());
    }

    /**
     * 【获取文件夹名称】
     **/
    public static String getFolderName(String folderName) {
        if (TextUtils.isEmpty(folderName)) {
            return folderName;
        }
        int filePosi = folderName.lastIndexOf(File.separator);
        return (filePosi == -1) ? "" : folderName.substring(0, filePosi);
    }

    /**
     * 【重命名文件】
     **/
    public static boolean renameFile(String oldFileName, String newFileName) {
        File oldName = new File(oldFileName);
        File newName = new File(newFileName);
        return oldName.renameTo(newName);
    }

    /**
     * 【判断文件夹里是否有文件】
     **/
    public static boolean hasFileExists(String folderPath) {
        File file = new File(folderPath);
        if (file.exists()) {
            File[] files = file.listFiles();
            return Objects.requireNonNull(files).length > 0;
        }
        return false;
    }

    /**
     * 【复制文件】参数为：String
     **/
    public static void copyFile(String fromFile, String toFile) {
        try (InputStream fisfrom = Files.newInputStream(Paths.get(fromFile));
             OutputStream outto = Files.newOutputStream(Paths.get(toFile))) {
            byte[] bt = new byte[1024];
            int len = fisfrom.read(bt);
            if (len > 0) {
                outto.write(bt, 0, len);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("FileTool", "Copy Failed");
        }
    }

    public static void copyFile(String from, String to, Boolean rewrite) {
        File fromFile = new File(from);
        File toFile = new File(to);

        if (!fromFile.exists()) {
            return;
        }
        if (!fromFile.isFile()) {
            return;
        }
        if (!fromFile.canRead()) {
            return;
        }
        if (!Objects.requireNonNull(toFile.getParentFile()).exists()) {
            toFile.getParentFile().mkdirs();
        }
        if (toFile.exists() && rewrite) {
            toFile.delete();
        }

        try (FileInputStream fisfrom = new FileInputStream(fromFile);
             FileOutputStream fosto = new FileOutputStream(toFile)) {
            byte[] bt = new byte[1024];
            int c;
            while ((c = fisfrom.read(bt)) > 0) {
                //将内容写到新文件当中
                fosto.write(bt, 0, c);
            }
        } catch (Exception ex) {
            Log.e("readfile", Objects.requireNonNull(ex.getMessage()));
        }

    }

    /**
     * 【复制文件夹】
     **/
    public static void copyDir(String fromFolder, String toFolder) {
        File[] currentFiles;
        File root = new File(fromFolder);
        if (!root.exists()) {
            return;
        }
        currentFiles = root.listFiles();
        File targetDir = new File(toFolder);
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }
        for (File currentFile : Objects.requireNonNull(currentFiles)) {
            if (currentFile.isDirectory()) {
                copyDir(currentFile.getPath() + "/", currentFile.getName() + "/");
            } else {
                copyFile(currentFile.getPath(), toFolder + currentFile.getName());
            }
        }
    }

    /**
     * 【获取文件夹下的所有文件夹名称(不包括子文件夹内)】
     **/
    public static ArrayList<String> listChildDirFromTargetDir(String targetFolder) {
        File folder = new File(targetFolder);
        if (!folder.exists()) {
            return new ArrayList<>();
        }
        ArrayList<String> dirsname = new ArrayList<>();
        File[] dirs = folder.listFiles();
        for (File file : Objects.requireNonNull(dirs)) {
            if (file.isDirectory()) {
                dirsname.add(file.getName());
            }
        }
        return dirsname;
    }

    /**
     * 【获取文件夹下的所有文件名称(不包括子文件夹内(不包括子文件夹内)】
     **/
    public static ArrayList<String> listChildFilesFromTargetDir(String targetFolder) {
        File folder = new File(targetFolder);
        ArrayList<String> filesname = new ArrayList<>();
        File[] files = folder.listFiles();
        for (File file : Objects.requireNonNull(files)) {
            if (file.isFile()) {
                filesname.add(file.getName());
            }
        }
        return filesname;
    }

    /**
     * 【删除某一文件夹和其下的所有文件及文件夹】
     **/
    public static void deleteDir(String dirPath) {
        File file = new File(dirPath);
        if (file.isFile()) {
            file.delete();
            return;
        }

        File[] files = file.listFiles();
        if (files != null) {
            for (File value : files) {
                deleteDir(value.getAbsolutePath());
            }
        }
        file.delete();
    }

    public static boolean makeFolder(String dirPath) {
        return makeFolder(new File(dirPath));
    }

    public static boolean makeFolder(File dir) {
        //Check if a directory exists, and try to make one if it doesn't
        return (!dir.exists()) && dir.mkdirs();
    }

    public static void moveFile(String fromFile, String toFile) {
        copyFile(fromFile, toFile);
        if (new File(fromFile).exists()) {
            new File(fromFile).delete();
        }
    }

    /**
     * 【创建文件】
     **/
    public static void addFile(String filePath) {
        File file = new File(filePath);
        try {
            file.createNewFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String readToString(String filePath) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (InputStream is = Files.newInputStream(Paths.get(filePath));
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
            return sb.toString();
        }
    }

    /**
     * [检查文件后缀名是否匹配]
     **/
    public static boolean FileSuffixFilter(String suffix, String fileName) {
        return fileName.length() > 0 && fileName.charAt(0) != '.' && fileName.endsWith("." + suffix);
    }

    public static boolean FileSuffixFilter(String suffix, File file) {
        return FileSuffixFilter(suffix, file.getName());
    }

    /**
     * [获取过滤后缀名后的文件夹下的子文件名列表]
     **/
    public static String[] listChildFileFromTargetDirFilterSuffix(String suffix, String dirPath) {
        ArrayList<String> tmp = listChildFilesFromTargetDir(dirPath);
        ArrayList<String> result = new ArrayList<>();
        for (String str : tmp) {
            if (FileSuffixFilter(suffix, str)) {
                result.add(str);
            }
        }
        return result.toArray(new String[0]);
    }

    /**
     * [给文本文件添加一行字符串]
     **/
    public static boolean addStringLineToFile(String content, File file) {
        try (FileWriter fw = new FileWriter(file, true)) {
            fw.write(content);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean addStringLineToFile(String content, String filePath) {
        return addStringLineToFile(content, new File(filePath));
    }

    public static File createFile(String filePath) {
        File file = new File(filePath);
        return FileTools.createFile(file);
    }

    public static File createFile(File file) {
        if (file.exists()) {
            file.delete();
        }
        Objects.requireNonNull(file.getParentFile()).mkdirs();

        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return file;
    }

    public static byte[] readFile(File file) {
        FileInputStream fis = null;
        try {

            fis = new FileInputStream(file);
            byte[] result = new byte[(int) file.length()];
            fis.read(result);
            fis.close();
            return result;
        } catch (Exception e) {

            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    public static String readFileToString(File file) {
        FileInputStream fis = null;
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            fis = new FileInputStream(file);
            byte[] result = new byte[(int) file.length()];
            fis.read(result);
            fis.close();
            return new String(result, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }


    public static boolean writeFile(File file, byte[] bytes) {

        file = FileTools.createFile(file);

        if (file == null) {
            return false;
        }
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            fos.write(bytes);
            fos.flush();
            fos.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    public static boolean writeFile(File file, String str) {

        boolean retval;
        retval = FileTools.writeFile(file, str.getBytes(StandardCharsets.UTF_8));
        return retval;
    }

    public static void writeFile(String outFile, String str) {
        writeFile(new File(outFile), str);
    }

    public static boolean extractAsset(AssetManager am, String src, File targetFile) {
        FileOutputStream fos = null;
        InputStream is = null;

        try {
            targetFile = FileTools.createFile(targetFile);

            fos = new FileOutputStream(targetFile);

            is = am.open(src);
            byte[] buf = new byte[1024];
            int count;

            while ((count = is.read(buf)) != -1) {
                fos.write(buf, 0, count);
            }

            fos.flush();
            fos.close();
            is.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();


            return false;
        } finally {

            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }


    }

    public static boolean extractAsset(AssetManager am, String src, String target) {
        return extractAsset(am, src, new File(target));
    }

    public static void extractTarXZ(File tarFile, File destDir) {

        FileInputStream fis = null;
        XZCompressorInputStream xzcis = null;
        TarArchiveInputStream tais = null;

        OutputStream fos = null;
        try {
            fis = new FileInputStream(tarFile);
            xzcis = new XZCompressorInputStream(fis);
            tais = new TarArchiveInputStream(xzcis, 1024);

            TarArchiveEntry entry;
            while ((entry = tais.getNextTarEntry()) != null) {
                File target = new File(destDir, entry.getName());
                if (entry.isDirectory()) {
                    target.mkdirs();
                } else {
                    fos = new FileOutputStream(target);

                    IOUtils.copy(tais, fos);
                    fos.flush();
                    fos.close();
                }
            }

            tais.close();
            xzcis.close();
            fis.close();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
                if (fos != null) {
                    fos.close();
                }
                if (xzcis != null) {
                    xzcis.close();
                }
                if (tais != null) {
                    tais.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }


    public static void extractTarXZ(String tar, File destDir) {
        extractTarXZ(new File(tar), destDir);
    }

    public static void extractTarXZ(File tarFile, String dir) {
        extractTarXZ(tarFile, new File(dir));
    }

    public static void extractTarXZ(String tar, String dir) {
        extractTarXZ(new File(tar), new File(dir));
    }

    public static boolean setExecutable(File file) {
        boolean retval = true;
        if (file.isDirectory()) {
            File[] subFiles = file.listFiles();
            for (File subFile : Objects.requireNonNull(subFiles)) {
                retval = retval && setExecutable(subFile);
            }
        }
        retval = retval && file.setExecutable(true);
        return retval;
    }

    public static boolean setExecutable(String file) {
        return setExecutable(new File(file));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void uncompressTarXZ(final InputStream tarFileInputStream, final File dest) throws IOException {
        dest.mkdirs();
        TarArchiveInputStream tarIn = new TarArchiveInputStream(new XZCompressorInputStream(tarFileInputStream));
        TarArchiveEntry tarEntry = tarIn.getNextTarEntry();
        while (tarEntry != null) {
            if (tarEntry.getSize() <= 20480) {
                try {
                    Thread.sleep(25);
                } catch (InterruptedException ignored) {

                }
            }
            File destPath = new File(dest, tarEntry.getName());
            if (tarEntry.isSymbolicLink()) {
                Objects.requireNonNull(destPath.getParentFile()).mkdirs();
                try {
                    Os.symlink(tarEntry.getLinkName().replace("..", dest.getAbsolutePath()), new File(dest, tarEntry.getName()).getAbsolutePath());
                } catch (Throwable e) {
                    Logging.LOG.log(Level.WARNING, e.getMessage());
                }
            } else if (tarEntry.isDirectory()) {
                destPath.mkdirs();
                destPath.setExecutable(true);
            } else if (!destPath.exists() || destPath.length() != tarEntry.getSize()) {
                Objects.requireNonNull(destPath.getParentFile()).mkdirs();
                destPath.createNewFile();
                FileOutputStream os = new FileOutputStream(destPath);
                byte[] buffer = new byte[1024];
                int byteCount;
                while ((byteCount = tarIn.read(buffer)) != -1) {
                    os.write(buffer, 0, byteCount);
                }
                os.close();
            }
            tarEntry = tarIn.getNextTarEntry();
        }
        tarIn.close();
    }

    public static void forceReleaseFile(File file) {
        try {
            RandomAccessFile raf = new RandomAccessFile(file, "rw");
            FileChannel channel = raf.getChannel();
            channel.lock().close();
            channel.close();
            raf.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 强制解除文件和文件夹的占用
    private void forceReleaseFiles(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        forceReleaseFiles(file); // 递归调用，处理子文件夹
                    } else {
                        forceReleaseFile(file); // 强制解除文件的占用
                    }
                }
            }
        } else {
            forceReleaseFile(dir); // 强制解除文件的占用
        }
    }
}