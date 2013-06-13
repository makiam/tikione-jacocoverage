package fr.tikione.jacocoverage.plugin.util;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.IOUtils;
import org.netbeans.api.project.Project;
import org.openide.util.Exceptions;

/**
 * Some utilities.
 *
 * @author Jonathan Lermitage
 */
public class Utils {

    private static final Logger LOGGER = Logger.getLogger(Utils.class.getName());

    /** Regular expression to recognize "${key}" patterns in Properties files used by NetBeans projects. */
    private static final String PA_NBPROPKEY_SHORTCUT = "\\$\\{([^\\}]+)\\}";

    /** Compiled regular expression to recognize "${key}" patterns in Properties files used by NetBeans projects. */
    private static final Pattern CPA_NBPROPKEY_SHORTCUT = Pattern.compile(PA_NBPROPKEY_SHORTCUT);

    /** The white-space character ({@code &#92;u0020}). */
    public static final char SPACE = '\u0020';

    /** The tabulation character ({@code &#92;u0009}). */
    public static final char TAB = '\u0009';

    private Utils() {
    }

    /**
     * Check a regular expression.
     *
     * @param src the text to examine.
     * @param pattern the compiled regular expression, targeted area are enclosed with parenthesis.
     * @return <code>true</code> if the regular expression is checked, otherwise <code>false</code>.
     */
    public static boolean checkRegex(String src, Pattern pattern) {
        return pattern.matcher(src).find();
    }

    /**
     * Return the capturing groups from the regular expression in the string.
     *
     * @param src the string to search in.
     * @param pattern the compiled pattern.
     * @param expectedNbMatchs the expected tokens matched, for performance purpose.
     * @return all the strings matched (in an ArrayList).
     */
    public static List<String> getGroupsFromRegex(String src, Pattern pattern, int expectedNbMatchs) {
        List<String> res = new ArrayList<String>(expectedNbMatchs);
        Matcher matcher = pattern.matcher(src);
        while (matcher.find()) {
            for (int group = 1; group <= matcher.groupCount(); group++) {
                res.add(matcher.group(group));
            }
        }
        return res;
    }

    /**
     * Get the JaCoCo binary report file of the given project.
     *
     * @param project the project to get JaCoCo report file.
     * @return the JaCoCo report file.
     */
    public static File getJacocoBinReportFile(Project project) {
        String jacocoExecPath = NBUtils.getProjectDir(project) + File.separator + "jacoco.exec";
        return new File(jacocoExecPath);
    }

    /**
     * Get the JaCoCo XML report file of the given project.
     *
     * @param project the project to get JaCoCo report file.
     * @return the JaCoCo report file.
     */
    public static File getJacocoXmlReportfile(Project project) {
        String jacocoXmlReportPath = NBUtils.getProjectDir(project) + File.separator + "jacocoverage.report.xml";
        return new File(jacocoXmlReportPath);
    }

    /**
     * Get a key value from a Properties object, with support of NetBeans key references (aka "${key}").
     *
     * @param props the Properties object to load key value from.
     * @param key the key value to get value.
     * @return the key value.
     */
    public static String getProperty(Properties props, String key) {
        String value = props.getProperty(key, "");
        int security = 0;
        while (security++ < 80 && checkRegex(value, CPA_NBPROPKEY_SHORTCUT)) {
            List<String> refs = getGroupsFromRegex(value, CPA_NBPROPKEY_SHORTCUT, 3);
            for (String ref : refs) {
                value = value.replaceFirst(PA_NBPROPKEY_SHORTCUT, props.getProperty(ref, ""));
            }
        }
        return value;
    }

    /**
     * Indicate if a Java line describes a finished instruction, otherwise a part of a multi-line instruction (or no instruction).
     *
     * @param inst the Java line.
     * @return {@code true} if instruction is finished, otherwise {@code false}.
     */
    public static boolean isIntructionFinished(String inst) {
        String trim = org.apache.commons.lang3.StringUtils.strip(inst);
        boolean finished = trim.endsWith(";") || trim.endsWith("}") || trim.endsWith("{");
        if (!finished) {
            // Remove comments and check again.
            trim = org.apache.commons.lang3.StringUtils.strip(trim.replaceAll("//.*$", "").replaceAll("/\\*.*\\*/", ""));
            finished = trim.endsWith(";") || trim.endsWith("}") || trim.endsWith("{");
        }
        return finished;
    }

    /**
     * Indicate if a project is supported by JaCoCoverage.
     * <br/>See <a href="http://wiki.netbeans.org/DevFaqActionAllAvailableProjectTypes">DevFaqActionAllAvailableProjectTypes</a> for help.
     *
     * @param project the project.
     * @return true if supported, otherwise false.
     */
    public static boolean isProjectSupported(Project project) {
        boolean supported;
        if (null == project) {
            supported = false;
        } else {
            String projectClass = project.getClass().getName();
            if (projectClass.equals("org.netbeans.modules.java.j2seproject.J2SEProject")
                    || projectClass.equals("org.netbeans.modules.apisupport.project.NbModuleProject")) {
                supported = true;
            } else {
                supported = false;
            }
        }
        return supported;
    }

    /**
     * Get a list of every subfolder contained in a given folder.
     *
     * @param root the root folder.
     * @return a list of subfolders.
     */
    public static List<File> listFolders(File root) {
        List<File> folders = new ArrayList<File>(16);
        File[] subfolders = root.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });
        folders.addAll(Arrays.asList(subfolders));
        for (File subfolder : subfolders) {
            folders.addAll(listFolders(subfolder));
        }
        return folders;
    }

    /**
     * Load an internal resource.
     *
     * @param internalResource the path of internal resource.
     * @return the loaded resource.
     * @throws IOException if an I/O error occurs.
     */
    public static byte[] toBytes(String internalResource)
            throws IOException {
        byte[] content = null;
        InputStream is = Utils.class.getResourceAsStream(internalResource);
        try {
            content = IOUtils.toByteArray(is);
        } finally {
            if (is != null) {
                is.close();
            }
        }
        return content;
    }

    /**
     * Compress a file to Zip. The archive contains an entry named as the source file.
     *
     * @param src the source file to compress.
     * @param dst the zipped output file.
     * @param entryname the name of the entry stored in the zipped file.
     * @param async if {@code true}, the compression process will be done in a separate parallel thread, otherwise the current thread.
     */
    public static void zip(final File src, final File dst, final String entryname, boolean async) {
        if (async) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    zip(src, dst, entryname);
                }
            }).start();
        } else {
            zip(src, dst, entryname);
        }
    }

    /**
     * Compress a file to Zip. The archive contains an entry named as the source file
     *
     * @param src the source file to compress.
     * @param dst the zipped output file.
     * @param entryname the name of the entry stored in the zipped file.
     * @throws FileNotFoundException if the source file doesn't exist.
     */
    private static void zip(File src, File dst, String entryname) {
        byte[] buffer = new byte[1024];
        try {
            FileOutputStream dstStrm = new FileOutputStream(dst);
            try {
                ZipOutputStream zipStrm = new ZipOutputStream(dstStrm);
                try {
                    FileInputStream srcStrm = new FileInputStream(src);
                    try {
                        ZipEntry entry = new ZipEntry(entryname);
                        zipStrm.putNextEntry(entry);
                        int len;
                        while ((len = srcStrm.read(buffer)) > 0) {
                            zipStrm.write(buffer, 0, len);
                        }
                    } finally {
                        srcStrm.close();
                    }
                    zipStrm.closeEntry();
                } finally {
                    zipStrm.close();
                }
            } finally {
                dstStrm.close();
            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }
}
