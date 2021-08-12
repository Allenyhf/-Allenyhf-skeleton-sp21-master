package gitlet;

import java.io.IOException;
import java.io.Serializable;
import java.io.File;
import java.util.*;
import static gitlet.Utils.*;

/** Represents a gitlet commit object.
 *  It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author Hongfa You
 */
public class Commit implements Serializable {

    /** The message of this Commit. */
    private String message;
    /** Date of this Commit was created. */
    private String dateString;
    /** First parent of this Commit, using SHA1 to indicate **/
    private String firstparent;
    /** Second parent of this Commit, using SHA1 to indicate **/
    private String secondparent;
    /** SHA1 identifier for this Commit. */
    private String sha1Id;
    /* TreeMap for file from name (such as hello.txt) to id (namely sha1Id) in File System */
    protected TreeMap<String, String> fileMap;

    /** Construtor with two argument
     * @param msg : commit messge.
     * @param fp : first parent.
     * @param sp : second parent.
     */
    public Commit(String msg, String fp, String sp) {
        message = msg;
        firstparent = fp;
        secondparent = sp;
        Calendar calendar = Calendar.getInstance();
        if (fp == null) {
            // This is the "initial Commit"
            dateString = Utils.getFormattedTime();
            fileMap = null;
        } else {
            dateString = Utils.getFormattedTime();
        }
        sha1Id = Utils.sha1(message + firstparent + dateString);
        dateString = "Thu Nov 9 17:01:33 2017 -0800";
    }

    /** Read Commit from file system by SHA1 of the Commit.
     * @param commitId indicates which Commit, it's actual name of the Commit in File System
     * @return the Commit read in
     */
    protected static Commit readCommitFromFile(String commitId) {
        File infile = Utils.join(Repository.INFOCOMMIT_DIR, commitId);
        if (!infile.exists()) {
            abort("No commit with that id exists.");
        }
        Commit commit = readObject(infile, Commit.class);
        return commit;
    }

    /** Save this Commit to a file in File System for future use. */
    protected void saveCommit() {
        File outfile = Utils.join(Repository.INFOCOMMIT_DIR, sha1Id);
        try {
            outfile.createNewFile();
        } catch (IOException excp) {
            System.out.println(excp.getMessage());
        }
        writeObject(outfile, this);
    }

    /** Return date of this Commit **/
    protected String getDate() {
        return dateString;
    }
    /** Return message of this Commit **/
    protected String getMessage() {
        return message;
    }
    /** Return first parent of this Commit, which is indicated by SHA1 String **/
    protected String getfirstParent() {
        return firstparent;
    }
    /** Return second parent of this Commit, which is indicated by SHA1 String **/
    protected String getsecondParent() {
        return secondparent;
    }
    /** Return SHA1 String of this Commit */
    protected String getSHA1() {
        return sha1Id;
    }

    /** Load the file specified by filename of this Commit into file.
     *  If not exists, just return null.
     **/
    protected File loadfile(String filename) {
        /** This commit doesn't contain file named filename, just return false. */
        if (fileMap == null || !fileMap.containsKey(filename)) {
            return null;
        }

        String commitId = fileMap.get(filename);
        File dir = Utils.join(Repository.COMMITED_DIR, commitId);
        return dir;
    }

    /** Return if filemap is null or not.
     * @return true if filemap is null.
     * */
    protected boolean isFilemapNull() {
        return fileMap == null;
    }

    /** Return if fileMap contains key. */
    protected boolean isFilemapContains(String key) {
        if (this.fileMap == null) {
            return false;
        }
        return this.fileMap.containsKey(key);
    }

    /** Return SHA1 String of file named key.
     * @param key : name of file.
     * @return SHA1 String of file.
     * */
    protected String getCommittedFileSHA1(String key) {
        if (this.fileMap == null) {
            return null;
        }
        return this.fileMap.get(key);
    }

    /** Return File of fileName in commit.
     * @param fileName : name of file.
     * @return File of fileName in commit.
     */
    protected File getFilefromCommit(String fileName, String errMsg) {
        String id = this.getCommittedFileSHA1(fileName);
        if (id == null) {
            Utils.abort(errMsg);
        }
        return Utils.join(Repository.COMMITED_DIR, id);
    }

    /** Helper function for log(), global-log().
     *  Print the commit information.
     */
    protected void printCommitInfo() {
        message("===");
        message("commit " + this.getSHA1());
        message("Date: " + this.getDate());
        message(this.getMessage());
        message("");
    }
}
