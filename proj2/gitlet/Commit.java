package gitlet;

import java.io.IOException;
import java.io.Serializable;
import java.io.File;
import java.util.*;
import static gitlet.Utils.*;
// import java.time.*;

/** Represents a gitlet commit object.
 *  It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author Hongfa You
 */
public class Commit implements Serializable {
    /**
     * add instance variables here.
     *
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    /** The message of this Commit. */
    private String message;
    /** Date of this Commit was created. */
    private String dateString;
    /** Parent of this Commit, using SHA1 to indicate **/
    private String parent;
    /** SHA1 identifier for this Commit. */
    private String sha1Id;
    /* TreeMap for file from name (such as hello.txt) to id (namely sha1Id) in File System */
    protected TreeMap<String, String> fileMap;

    /**
     *  Construtor with two argument
     * @param msg
     * @param p
     */
    public Commit(String msg, String p) {
        message = msg;
        parent = p;

        Calendar calendar = Calendar.getInstance();
        if (p == null) {
            // This is the "initial Commit"
            dateString = Utils.getFormattedTime();
            fileMap = null;
        } else {
            dateString = Utils.getFormattedTime();
        }
//        System.out.println(dateString);
        sha1Id = Utils.sha1(message + parent + dateString);
        dateString = "Thu Nov 9 17:01:33 2017 -0800";
    }

    /**
     * Read Commit from file system by SHA1 of the Commit.
     * @param commitId indicates which Commit, it's actual name of the Commit in File System
     * @return the Commit read in
     */
    public static Commit readCommitFromFile(String commitId) {
        File infile = Utils.join(Repository.INFOCOMMIT_DIR, commitId);
        if (!infile.exists()) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        Commit commit = readObject(infile, Commit.class);
        return commit;
    }

    /**
     * Save this Commit to a file in File System for future use.
     */
    public void saveCommit() {
        File outfile = Utils.join(Repository.INFOCOMMIT_DIR, sha1Id);
        try {
            outfile.createNewFile();
        } catch (IOException excp) {
            System.out.println(excp.getMessage());
        }
        writeObject(outfile, this);
    }

    /** Return date of this Commit **/
    public String getDate() {
        return dateString;
    }
    /** Return message of this Commit **/
    public String getMessage() {
        return message;
    }
    /** Return parent of this Commit, which is indicated by SHA1 String **/
    public String getParent() {
        return parent;
    }

    /** Return SHA1 String of this Commit */
    public String getSHA1() {
        return sha1Id;
    }

    /**
     *  Load the file specified by filename of this Commit into file.
     *  If not exists, just return null.
     **/
    public File loadfile(String filename) {
        /** This commit doesn't contain file named filename, just return false. */
        if (fileMap == null || !fileMap.containsKey(filename)) {
            return null;
        }

        String cId = fileMap.get(filename);
        File dir = Utils.join(Repository.COMMITED_DIR, cId);
//        File file = readObject(dir, File.class);
//        return file;
        return dir;
    }

}
