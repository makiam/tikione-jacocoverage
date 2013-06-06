package fr.tikione.jacocoverage.plugin;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
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

    /** Regex to recognize "${key}" patterns in Properties files used by NetBeans projects. */
    private static final String PA_NBPROPKEY_SHORTCUT = "\\$\\{([^\\}]+)\\}";

    /** Compiled regex to recognize "${key}" patterns in Properties files used by NetBeans projects. */
    private static final Pattern CPA_NBPROPKEY_SHORTCUT = Pattern.compile(PA_NBPROPKEY_SHORTCUT);

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
     * Get the JaCoCo bnary report file of the given project.
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
     * Indicate if a project is supported by JaCoCoverage.
     * See http://wiki.netbeans.org/DevFaqActionAllAvailableProjectTypes for help.
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
     * @param async if {@code true}, the compression process will be done in a separate parallel thread, otherwise the living thread.
     */
    public static void zip(final File src, final File dst, final String entryname, boolean async) {
        if (async) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        zip(src, dst, entryname);
                    } catch (IOException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                }
            }).start();
        } else {
            try {
                zip(src, dst, entryname);
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }

    /**
     * Compress a file to Zip. The archive contains an entry named as the source file
     *
     * @param src the source file to compress.
     * @param dst the zipped output file.
     * @param entryname the name of the entry stored in the zipped file.
     * @throws FileNotFoundException if the source file doesn't exist.
     * @throws IOException if an error occurs during compression.
     */
    private static void zip(File src, File dst, String entryname)
            throws FileNotFoundException,
                   IOException {
        byte[] buffer = new byte[1024];
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
    }
}
