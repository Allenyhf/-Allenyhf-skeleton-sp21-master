package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.TreeMap;
import java.util.Set;

import static gitlet.Utils.*;

/**
 *
 * @author Hongfa You
 */

public class Blob implements Serializable {

    /** Map from filename to SHA1 for each added file */
    protected static TreeMap<String, String> blobMap;
    // private String SHA1;
    // private Date date;
    protected static TreeMap<String, String> removal;

    /**
     *  Serialize and save file named "name" in CWD into .gitlet/staged_obj in CWD
     *  and update the blobMap to File System.
     *  Return value: return SHA1 of the file.
     */
    public static void add(String name) {

        String SHA1 = Utils.sha1(name);
        File file = Utils.join(Repository.CWD, name);
        File outfile = Utils.join(Repository.STAGE_DIR, SHA1);
//        writeObject(outfile, file);
        try {
            Files.copy(file.toPath(), outfile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException excp) {
            System.out.println(excp.getMessage());
        }
        loadBlobMap(SHA1, name);
        saveBlobMap();
    }

    /**
     *
     */
    public static void remove(String name) {
        String SHA1 = Utils.sha1(name);
        loadremoval(name, SHA1);
        saveremoval();
    }

    /**
     *  Load blobMap from file system, if not exists create new one.
     *  Then put the key-value pair <SHA1, name> of the new added file into it.
     * @param key SHA1 of the new added file
     * @param value name of the new added file
     */
    public static void loadBlobMap(String key, String value) {
        blobMap = getTreeMap(blobMap, false);
        blobMap.put(key, value);
    }

    public static void loadremoval(String key, String value) {
        removal = getTreeMap(removal, true);
        removal.put(key, value);
    }

    public static void saveremoval() {
        File blobmapfile = Utils.join(Repository.INFOSTAGE_DIR, "removal");
        writeObject(blobmapfile, removal);
    }

    /**
     *  Save blobMap into file system.
     *  Should be called after loadBlobMap().
     */
    public  static void saveBlobMap() {

        File blobmapfile = Utils.join(Repository.INFOSTAGE_DIR, "blobMap");
        writeObject(blobmapfile, blobMap);
    }

    /**
     *  Get blobMap from file system.
     * @return
     */
    public static TreeMap getTreeMap(TreeMap<String, String> map, boolean isRemoval){
        File blobmapfile;
        if (isRemoval) {
            blobmapfile = Utils.join(Repository.INFOSTAGE_DIR, "removal");
        } else {
            blobmapfile = Utils.join(Repository.INFOSTAGE_DIR, "blobMap");
        }
        if (blobmapfile.exists()) {
            map = readObject(blobmapfile, TreeMap.class);
        } else {
            map = new TreeMap<>();
        }
        return map;
    }

    /**
     *  Delete blobMap from file system.
     *  It is used for clear staged imformation.
     */
    public static void deleteBlobMap() {
        File blobmapfile = Utils.join(Repository.INFOSTAGE_DIR, "blobMap");
        if (blobmapfile.exists()) {
            blobmapfile.delete();
        }
    }

    /**
     * Delete key from blobMap.
     */
    public static void deteleItem(String key) {
        File blobmapfile = Utils.join(Repository.INFOSTAGE_DIR, "blobMap");
        if (blobmapfile.exists()) {
            blobMap = readObject(blobmapfile, TreeMap.class);
            blobMap.remove(Utils.sha1(key));
            saveBlobMap();
        }
    }

}
