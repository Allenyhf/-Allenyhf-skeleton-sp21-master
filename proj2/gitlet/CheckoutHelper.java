package gitlet;

import java.io.File;
import java.util.List;
import java.util.Map;

import static gitlet.Repository.*;
import static gitlet.Utils.abort;
import static gitlet.Utils.join;


/** Helper class providing static helper method for "checkout" and "reset".
 *  unstageAll() : Unstaged all of the files if it is currently staged for addition.
 *  overwriteOne(String commitSHA, String filename) : Overwrite file named filename in commitSHA.
 *  overwriteAll(String commitSHA) : Overwrite all files in CWD with files in commitSHA.
 *  checkUntracked() : Check if there are some files untracked, just abort.
 * */
public class CheckoutHelper {

    /** Unstaged all of the files if it is currently staged for addition.
     *  Delete them and update the blobMap.
     */
    protected static void unstageAll() {
        List<String> fileList = Utils.plainFilenamesIn(STAGE_DIR);
        File file;
        for (String filename: fileList) {
            file = Utils.join(STAGE_DIR, filename);
            file.delete();
//            restrictedDelete(file);
        }
        Blob.deleteBlobMap();
    }

    /** Overwrite file named filename in commitSHA.
     * @param commitSHA : SHA1 String of commit.
     * @param filename : name of file.
     * */
    protected static void overwriteOne(String commitSHA, String filename) {
        Commit commit = Commit.readCommitFromFile(commitSHA);

        String errMsg = "File does not exist in that commit.";
        File dest = join(CWD, filename);
        File dir = commit.getFilefromCommit(filename, errMsg);
        Utils.secureCopyFile(dir, dest);
    }

    /** Overwrite all files in CWD with files in commitSHA.
     * @param commitSHA : SHA1 String of commit.
     * */
    protected static void overwriteAll(String commitSHA) {
        Commit commit = Commit.readCommitFromFile(commitSHA);
        if (commit.isFilemapNull()) {
            return;
        }
        for (Map.Entry<String, String> entry : commit.fileMap.entrySet()) {
            String key = entry.getKey();
            File dir = commit.getFilefromCommit(entry.getKey(), "no file named " + key);
            File dest = join(CWD, entry.getKey());
            Utils.secureCopyFile(dir, dest);
        }
    }

    /** If there are some files untracked, just abort. */
    protected static void checkUntracked() {
        List<String> fileList = Utils.plainFilenamesIn(CWD);
        Commit currentCommit = Commit.readCommitFromFile(HEAD.whichCommit());
        Boolean toAbort = false;
        String errMsg = "There is an untracked file in the way; "
                + "delete it, or add and commit it first.";
        for (String file : fileList) {
            if (!currentCommit.isFilemapContains(file) && !Blob.isBlobmapContains(file)) {
                toAbort = true;
                break;
            }
        }
        if (toAbort) {
            abort(errMsg);
        }
    }

    /** Get commitId by its abbreviation.
     * @param commitName : abbreviation of commitId.
     * @return commitId.
     * */
    protected static String find(String commitName) {
        List<String> commitList = Utils.plainFilenamesIn(INFOCOMMIT_DIR);
        String result = null;
        for (String commit : commitList) {
            if (result == null && commit.contains(commitName)) {
                result = commit;
            } else if (commit.contains(commitName)) {
                abort("There exists more than one commitId.");
            }
        }
        if (result != null) {
            return result;
        } else {
            return null;
        }
    }

}
