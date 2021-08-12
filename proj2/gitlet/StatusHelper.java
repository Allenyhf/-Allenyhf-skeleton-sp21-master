package gitlet;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.HashSet;

import static gitlet.Utils.*;

/** Class doing actual work for "status". */
public class StatusHelper {

    private static Set<String> nameSet = new HashSet<>();

    protected static void printStatus() {
        printBranch();
        printStagedFiles();
        printRemovedFiles();
        printModifications();
        printUntrackedFiles();
    }

    /** Helper function for status().
     *  Print all of the branches and indicate current branch with '*'. */
    private static void printBranch() {
        message("=== Branches ===");
        List<String> branchList = Utils.plainFilenamesIn(Repository.BRANCH_DIR);
        HEAD.readHEAD();

        for (String branchName: branchList) {
            if (branchName.equals("HEAD")) {
                continue;
            }
            if (branchName.equals(HEAD.getPointBranch())) {
                System.out.print('*');
            }
            message(branchName);
        }
        message("");
    }

    /** Helper function for status().
     *  List all of the files in staging area. */
    private static void printStagedFiles() {
        message("=== Staged Files ===");
        TreeMap<String, String> blobMap = Blob.getTreeMap(Blob.blobMap, false);
        if (blobMap == null) {
            abort("");
        }
        String value;
        for (Map.Entry<String, String> entry : blobMap.entrySet()) {
            value = entry.getValue();
            nameSet.add(value);
            message(value);
        }
        message("");
    }

    /** Helper function for status(). */
    private static void printRemovedFiles() {
        message("=== Removed Files ===");
        TreeMap<String, String> removal = Blob.getTreeMap(Blob.removal, true);
        String key;
        for (Map.Entry<String, String> entry : removal.entrySet()) {
            key = entry.getKey();
            message(key);
        }
        message("");
    }

    /** Helper function for status(). List all of the files modified but not staged.
     *  Modified but not staged is :
     *  Tracked in the current commit, changed in the working directory, but not staged.
     *  Staged for addition, but with different contents than in the working directory.
     *  Staged for addition, but deleted in the working directory.
     *  Not staged for removal, but tracked in the current commit and deleted from the
     *  working directory.
     * */
    private static void printModifications() {
        message("=== Modifications Not Staged For Commit ===");
        Commit currentCommit = Commit.readCommitFromFile(HEAD.whichCommit());
        List<String> fileList = Utils.plainFilenamesIn(Repository.CWD);
        Blob.loadBlobMap();
        Blob.loadremoval();
        for (String file : fileList) {
            if ((Blob.blobMap == null || !Blob.isBlobmapContains(file))
                    && currentCommit.isFilemapContains(file)) {
                /** Committed but changed and unstaged. */
                File rawfile = Utils.join(Repository.COMMITED_DIR, currentCommit.fileMap.get(file));
                File cwdfile = Utils.join(Repository.CWD, file);
                if (!isFileSame(cwdfile, rawfile)) {
                    message(file + "(modified)");
                }
            } else if (!currentCommit.isFilemapContains(file)
                    && Blob.blobMap != null && Blob.isBlobmapContains(file)) {
                /** Staged but not commited and changed.*/
                String sha1 = Utils.sha1(file);
                File rawfile = Utils.join(Repository.STAGE_DIR, sha1);
                File cwdfile = Utils.join(Repository.CWD, file);
                if (!isFileSame(cwdfile, rawfile)) {
                    message(file + "(modified)");
                }
            }
        }

        if (Blob.blobMap != null) {
            for (Map.Entry<String, String> entry : Blob.blobMap.entrySet()) {
                if (!fileList.contains(entry.getValue())) {
                    /** file is deleted. */
                    message(entry.getValue() + "(deleted)");
                }
            }
        }

        if (currentCommit.fileMap != null) {
            for (Map.Entry<String, String> entry : currentCommit.fileMap.entrySet()) {
                if (!fileList.contains(entry.getKey())
                        && (Blob.removal == null || !Blob.isRemovalContains(entry.getKey()))) {
                    /** Committed and deleted but not unstaged. **/
                    message(entry.getKey() + "(deleted)");
                }
            }
        }
        message("");
    }

    /** Helper function for status().
     *  List all of the untracked files. */
    private static void printUntrackedFiles() {
        message("=== Untracked Files ===");
        Blob.loadBlobMap();
        Commit currentCommit = Commit.readCommitFromFile(HEAD.whichCommit());
        List<String> fileList = Utils.plainFilenamesIn(Repository.CWD);
        for (String file : fileList) {
            /** Files presents in CWD, but neither staged nor tracked. **/
            if (!Blob.isBlobmapContains(file)) {
                if (currentCommit.fileMap == null || !currentCommit.isFilemapContains(file)) {
                    message(file);
                }
            }
        }
        message("");
    }

}
