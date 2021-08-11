package gitlet;

import java.io.Serializable;
import java.io.File;

import static gitlet.Utils.readObject;
import static gitlet.Utils.writeObject;

public class HEAD implements Serializable {
//    private String pointBranch; // a String indicates the Commit HEAD should point to
    private static String pointBranchName;

    public static void initialize(String commitId) {
        pointBranchName = "master";
        Branch master = new Branch(pointBranchName, commitId);
        master.saveBranch();
        saveHEAD();
    }

    /** Make HEAD switch to branchName.
     * @param branchName
     */
    public static void switchHEAD(String branchName) {
        readHEAD();
        pointBranchName = branchName;
        saveHEAD();
    }

    /** Make HEAD switch to commit SHA1. */
    public static void switch2commit(String commitId) {
        readHEAD();
        Branch pointBranch = Branch.readBranchIn(pointBranchName, false);
        pointBranch.resetWhichCommit(commitId);
        saveHEAD();
    }

    /** Save HEAD to File System. */
    public static void saveHEAD() {
        File file = Utils.join(Repository.BRANCH_DIR, "HEAD");
        writeObject(file, pointBranchName);
    }

    /** Read HEAD from File System. */
    public static void readHEAD() {
        File file = Utils.join(Repository.BRANCH_DIR, "HEAD");
        pointBranchName = readObject(file, String.class);
    }

    /** Return SHA1 String of the commit pointed by HEAD. */
    public static String whichCommit() {
        readHEAD();
        Branch pointBranch = Branch.readBranchIn(pointBranchName, false);
        return pointBranch.whichCommit();
    }

    public static String getPointBranch() {
        return pointBranchName;
    }

    public static void setPointBranch(String name) {
        pointBranchName = name;
    }
}
