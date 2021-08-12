package gitlet;

import java.io.File;
import java.util.*;

import static gitlet.Repository.COMMITED_DIR;
import static gitlet.Repository.STAGE_DIR;
import static gitlet.Utils.abort;
import static gitlet.Utils.plainFilenamesIn;

/** Helper class providing static helper method for "commit".
 *  staged2Commited(Commit commit) : Move the files in the directory .gitlet/staged_obj/
 *                                   to the directory .gitlet/commited_obj.
 *  copySnapshot(Commit commit) : Copy snapshots of current Commit to the new Commit commit.
 *                                If no snapshots, namely fileMap of current Commit is empty,
 *                                just create a new TreeMap.
 */
public class CommitHelper {

    /** helper function for commit().
     *  Move the files in the directory .gitlet/staged_obj/ to the directory .gitlet/commited_obj
     * @param commit : new commit.
     */
    protected static void staged2Commited(Commit commit) {
        moveFromStaged2Commited(commit);
        Blob.deleteBlobMap();
        Blob.deleteRemoval();
    }

    /**  Copy snapshots of current Commit to the new Commit commit.
     *   If no snapshots, namely fileMap of current Commit is empty,
     *   just create a new TreeMap.
     * @param commit : new commit.
     */
    protected static void copySnapshot(Commit commit) {
        Commit lastestCommit = Commit.readCommitFromFile(HEAD.whichCommit());
        if (!lastestCommit.isFilemapNull()) {
            commit.fileMap = (TreeMap<String, String>) lastestCommit.fileMap.clone();
        } else {
            commit.fileMap = new TreeMap<>();
        }
    }

    /** helper function for Staged2Commited().
     * @param commit : new commit.
     */
    private static void moveFromStaged2Commited(Commit commit) {
        List<String> listOfStaged = plainFilenamesIn(STAGE_DIR);
        Blob.loadremoval();
        Blob.loadBlobMap();
        if (Blob.blobMap.isEmpty() && Blob.isRemovalEmpty()) {
            abort("No changes added to the commit.");
        }

        File tmpfile;
        File destfile;
        Boolean isRmNotEmpty = !Blob.isRemovalEmpty();
        for (String file: listOfStaged) { // file is a SHA1 String
            tmpfile = Utils.join(STAGE_DIR, file);
            String name = Blob.blobMap.get(file); // name: hello.c (for example)

            Calendar calendar = Calendar.getInstance();
            Date date = calendar.getTime();
            String shaId = Utils.sha1(name + date.toString());

            if (commit.isFilemapContains(name)) {
                commit.fileMap.replace(name, shaId);
            } else {
                commit.fileMap.put(name, shaId); //map from file name (hello.c) to SHA1 String
            }
            destfile = Utils.join(COMMITED_DIR, shaId);
            Utils.secureCopyFile(tmpfile, destfile);
            tmpfile.delete();
        }

        if (!isRmNotEmpty) {
            return;
        }
        /** Remove the entry of unstaged files from fileMap. */
        for (Map.Entry<String, String> entry : Blob.removal.entrySet()) {
            if (commit.isFilemapContains(entry.getKey())) {
                commit.fileMap.remove(entry.getKey());
            }
        }
    }

}
