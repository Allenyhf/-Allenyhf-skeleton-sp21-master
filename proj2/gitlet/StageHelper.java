package gitlet;

import java.io.File;

import static gitlet.Repository.STAGE_DIR;
import static gitlet.Repository.CWD;

import static gitlet.Utils.*;

/** **/
public class StageHelper {

    /***  PROTECTED METHOD ***/


    /** Check current Commit for file to unstage from it.
     *  If the file exists in the Commit, move it from .gitlet/commited_obj to .gitlet/unstaged_obj.
     *  --And do not remove the key-value pair from fileMap.--
     *  If it is in the Commit, remove the file from CWD.
     * @param filename : name of file.
     * @return true if file is in commit, or return false.
     */
    protected static boolean checkCommit2Unstaged(String filename) {
        Commit commit = Commit.readCommitFromFile(HEAD.whichCommit());
        if (commit.isFilemapNull()) {
            return false;
        }
        if (commit.isFilemapContains(filename)) {
            File cwdfile = Utils.join(CWD, filename);
            cwdfile.delete();
            return true;
        }
        return false;
    }

    /** Unstaged the file if it is currently staged for addition.
     *  Delete the file and update the blobMap.
     * @param filename : name of file.
     */
    protected static boolean unstageOne(String filename) {
        String shaId = Utils.sha1(filename);
        File destfile = join(STAGE_DIR, shaId);
        if (destfile.exists()) {
            destfile.delete();
            Blob.deteleItem(filename);
            return true;
        }
        return false;
    }

    /*** PRIVATE METHOD ***/



}
