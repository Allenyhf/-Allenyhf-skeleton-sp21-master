package gitlet;

import java.io.Serializable;
import java.io.File;

import static gitlet.Utils.readObject;
import static gitlet.Utils.writeObject;

public class HEAD implements Serializable {
//    private String pointBranch; // a String indicates the Commit HEAD should point to
    protected static String pointBranchName;

//    public HEAD(String SHA1) {
//        pointBranchName = "master";
//        Branch pointBranch = new Branch("master", SHA1);
//        pointBranch.saveBranch();
//    }
    public static void initialize(String commitId) {
        pointBranchName = "master";
        Branch master = new Branch(pointBranchName, commitId);
        master.saveBranch();
        saveHEAD();
    }

    /**
     *  Make HEAD switch to BranchName.
     * @param BranchName
     */
    public static void switchHEAD(String branchName) {
        readHEAD();
        pointBranchName = branchName;
        saveHEAD();
    }

    /** Make HEAD switch to commit SHA1. */
    public static void switch2commit(String commitId) {
        readHEAD();
        Branch pointBranch = Branch.readBranchIn(pointBranchName);
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
        Branch pointBranch = Branch.readBranchIn(pointBranchName);
        return pointBranch.whichCommit();
    }
}
