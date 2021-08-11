package gitlet;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static gitlet.Utils.readContents;

public class DebugHelper {
    /** Debug helper function.
     *  Print fileMap of the Commit pointed by HEAD.
     */
    private static void testCommit() {
        Commit tmp = Commit.readCommitFromFile(HEAD.whichCommit());
        for (Map.Entry<String, String> entry : tmp.fileMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            System.out.println(key + " => " + value);
        }
    }

    /** Debug helper function. */
    public static void testBlob() {
        Blob.blobMap = Blob.getTreeMap(Blob.blobMap, false);
        for (Map.Entry<String, String> entry : Blob.blobMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            System.out.println(key + " => " + value);
        }
    }

    private static void traceback() {
        Commit commit = Commit.readCommitFromFile(HEAD.whichCommit());
        String parent;
        while (true) {
            parent = commit.getfirstParent();
            printCommitInfo();
            if (parent == null) {
                break;
            }
            commit = Commit.readCommitFromFile(parent);
        }
    }

    public static void printCommitInfo() {
        traceback();
        HEAD.switchHEAD("fork");
        traceback();
    }

    public static void printStaged() {

    }

    public static void printUnstaged() {

    }

    public static void printSplitPoint() {
        String sha1 = MergeHelper.findSplitPoint("fork");
        Commit commit = Commit.readCommitFromFile(sha1);
        System.out.println(commit.getMessage());
    }

    public static void main(String[] args) {
        String msg = "select --> blobMap, commit, remove, branch";
        System.out.println(Utils.getFormattedTime());
        if (args.length == 0) {
            Utils.abort(msg);
        }

        switch (args[0]) {
            case "blobMap":
                testBlob();
                break;
            case "commit":
                testCommit();
                break;
            case "remove":
                break;

            case "branch":
                printCommitInfo();
                break;

            case "split":
                printSplitPoint();
                break;

            default:
                break;
        }
    }


}
