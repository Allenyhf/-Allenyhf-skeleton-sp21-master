package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.TreeMap;

import static gitlet.Utils.*;

/**
 *
 * @author Hongfa You
 */

public class Blob implements Serializable {

    /** Map from filename to SHA1 for each added file */
    // TreeMap for staged.
    protected static TreeMap<String, String> blobMap;
    // TreeMap for unstaged.
    protected static TreeMap<String, String> removal;

    /**
     *  Serialize and save file named "name" in CWD into .gitlet/staged_obj in CWD
     *  and update the blobMap to File System.
     */
    public static void add(String name) {

        String sha1Id = Utils.sha1(name);
        File file = Utils.join(Repository.CWD, name);
        File outfile = Utils.join(Repository.STAGE_DIR, sha1Id);
        secureCopyFile(file, outfile);
        loadBlobMap(sha1Id, name);
        saveBlobMap();
    }

    /** Add file whose name is "name" to removal. */
    public static void remove(String name, boolean toRemoval) {
        String sha1Id = Utils.sha1(name);
//        blobMap = getTreeMap(blobMap, false);
//        blobMap.remove(name);
        if (toRemoval) {
            loadremoval(name, sha1Id);
            saveremoval();
        }
    }

    /**
     *  Load blobMap from file system, if not exists create new one.
     *  Then put the key-value pair <SHA1, name> of the new-added file into it.
     * @param key SHA1 of the new-added file.
     * @param value name of the new-added file.
     */
    public static void loadBlobMap(String key, String value) {
        blobMap = getTreeMap(blobMap, false);
        blobMap.put(key, value);
    }

    /**
     *  Load removal from file system, if not exists create new one.
     *  Then put the key-value pair <SHA1, name> of the new-removed file into it.
     * @param key SHA1 of the new-removed file.
     * @param value name of the new-removed file.
     */
    public static void loadremoval(String key, String value) {
        removal = getTreeMap(removal, true);
        removal.put(key, value);
    }

    /** Save removal into file system.
     *  Should be called after loadremoval().
     */
    public static void saveremoval() {
        File blobmapfile = Utils.join(Repository.INFOSTAGE_DIR, "removal");
        writeObject(blobmapfile, removal);
    }

    /** Save blobMap into file system.
     *  Should be called after loadBlobMap().
     */
    public  static void saveBlobMap() {
        File blobmapfile = Utils.join(Repository.INFOSTAGE_DIR, "blobMap");
        writeObject(blobmapfile, blobMap);
    }

    /** Get blobMap or removal from file system.
     * @return
     */
    public static TreeMap getTreeMap(TreeMap<String, String> map, boolean isRemoval) {
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
     *  Delete removal from file system.
     *  It is used for clear staged imformation.
     */
    public static void deleteRemoval() {
        File removal = Utils.join(Repository.INFOSTAGE_DIR, "removal");
        if (removal.exists()) {
            removal.delete();
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

    /**
     * Check if removal contains file "name"
     * @param name
     * @return
     */
    public static boolean isRemovalContains(String name) {
        removal = getTreeMap(removal, true);
        if (removal.containsKey(name)) {
            return true;
        }
        return false;
    }

    /** unremove the file "name" **/
    public static void unremove(String name) {
        removal = getTreeMap(removal, true);
        removal.remove(name);
        saveremoval();
    }

    /** Check if unstage area is empty. */
    public static boolean isRemovalNotEmpty() {
        removal = getTreeMap(removal, true);
        if (removal == null || removal.isEmpty()) {
            return true;
        }
        return false;
    }
}
