package gitlet;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.HashSet;

import static gitlet.Utils.abort;
import static gitlet.Utils.isFileSame;

public class LogHelper {

    private static Set<String> nameSet = new HashSet<>();
    /** Helper function for status().
     *  Print all of the branches and indicate current branch with '*'. */
    protected static void printBranch() {
        System.out.println("=== Branches ===");
        List<String> branchList = Utils.plainFilenamesIn(Repository.BRANCH_DIR);
        HEAD.readHEAD();

        for (String branchName: branchList) {
            if (branchName.equals("HEAD")) { continue; }
            if (branchName.equals(HEAD.getPointBranch())) {
                System.out.print('*');
            }
            System.out.println(branchName);
        }
        System.out.println();
    }

    /** Helper function for status().
     *  List all of the files in staging area. */
    protected static void printStagedFiles() {
        System.out.println("=== Staged Files ===");
        TreeMap<String, String> blobMap = Blob.getTreeMap(Blob.blobMap, false);
        if (blobMap == null) { abort(""); }
        String value;
        for (Map.Entry<String, String> entry : blobMap.entrySet()) {
            value = entry.getValue();
            nameSet.add(value);
            System.out.println(value);
        }
        System.out.println();
    }

    /** Helper function for status(). */
    protected static void printRemovedFiles() {
        System.out.println("=== Removed Files ===");
        TreeMap<String, String> removal = Blob.getTreeMap(Blob.removal, true);
        String key;
        for (Map.Entry<String, String> entry : removal.entrySet()) {
            key = entry.getKey();
            System.out.println(key);
        }
        System.out.println();
    }

    /** Helper function for status(). List all of the files modified but not staged.
     *  Modified but not staged is :
     *  Tracked in the current commit, changed in the working directory, but not staged.
     *  Staged for addition, but with different contents than in the working directory.
     *  Staged for addition, but deleted in the working directory.
     *  Not staged for removal, but tracked in the current commit and deleted from the
     *  working directory.
     * */
    protected static void printModifications() {
        System.out.println("=== Modifications Not Staged For Commit ===");

        Commit currentCommit = Commit.readCommitFromFile(HEAD.whichCommit());
        List<String> fileList = Utils.plainFilenamesIn(Repository.CWD);
        Blob.loadBlobMap();
        Blob.loadremoval();
        for (String file : fileList) {
            if ((Blob.blobMap == null  || !Blob.isBlobmapContains(file))
                    && currentCommit.isFilemapContains(file)) {
                /** Committed but changed and unstaged. */
                File rawfile = Utils.join(Repository.COMMITED_DIR, currentCommit.fileMap.get(file));
                File cwdfile = Utils.join(Repository.CWD, file);
                if (!isFileSame(cwdfile, rawfile)) {
                    System.out.println(file + "(modified)");
                }
            } else if (!currentCommit.isFilemapContains(file)
                    && Blob.blobMap != null && Blob.isBlobmapContains(file)) {
                /** Staged but not commited and changed.*/
                String sha1 = Utils.sha1(file);
                File rawfile = Utils.join(Repository.STAGE_DIR, sha1);
                File cwdfile = Utils.join(Repository.CWD, file);
                if (!isFileSame(cwdfile, rawfile)) {
                    System.out.println(file + "(modified)");
                }
            }
        }

        if (Blob.blobMap != null) {
            for (Map.Entry<String, String> entry : Blob.blobMap.entrySet()) {
                if (!fileList.contains(entry.getValue())) {
                    /** file is deleted. */
                    System.out.println(entry.getValue() + "(deleted)");
                }
            }
        }

        if (currentCommit.fileMap != null) {
            for (Map.Entry<String, String> entry : currentCommit.fileMap.entrySet()) {
                if (!fileList.contains(entry.getKey())
                        && (Blob.removal == null || !Blob.isRemovalContains(entry.getKey()))) {
                    /** Committed and deleted but not unstaged. **/
                    System.out.println(entry.getKey() + "(deleted)");
                }
            }
        }
        System.out.println();
    }

    /** Helper function for status().
     *  List all of the untracked files. */
    protected static void printUntrackedFiles() {
        System.out.println("=== Untracked Files ===");
        Blob.loadBlobMap();
        Commit currentCommit = Commit.readCommitFromFile(HEAD.whichCommit());
        List<String> fileList = Utils.plainFilenamesIn(Repository.CWD);
        for (String file : fileList) {
            /** Files presents in CWD, but neither staged nor tracked. **/
            if (!Blob.isBlobmapContains(file)) {
                if (currentCommit.fileMap == null || !currentCommit.isFilemapContains(file)) {
                    System.out.println(file);
                }
            }
        }
        System.out.println();
    }

}
