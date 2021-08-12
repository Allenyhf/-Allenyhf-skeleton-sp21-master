package gitlet;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static gitlet.Utils.*;
import static gitlet.Utils.readContents;

/** Class doing actual work for "merge". */
public class MergeHelper {

    private static Commit split;
    private static Commit current;
    private static Commit other;
    private static Boolean isConflict = false;
    /** A HashSet contains SHA1 String of ancestors of "current" branch. */
    private static Set<String> ancestrorSha1Set = new HashSet<>();


    /** Return SHA1 String of Split point commit.
     *  Time Complexity : O(n), n is the total number of ancestors of the two commits.
     * @param branchName
     * @return SHA1 String of Split point commit.
     */
    protected static String findSplitPoint(String branchName) {
        Commit commit = Commit.readCommitFromFile(HEAD.whichCommit());

        /** Add ancestor of the commit indicated by HEAD into Set. **/
        /** O(n). n : length from initial commit to the commit indicated by HEAD. */
        String parent;
        while (true) {
            parent = commit.getfirstParent();
            ancestrorSha1Set.add(commit.getSHA1());
            if (parent == null) {
                break;
            }
            commit = Commit.readCommitFromFile(parent);
        }
        /** Read the commit indicated by branchName in. **/
        String id = Branch.readBranchIn(branchName, true).whichCommit();
        Commit commit2 = Commit.readCommitFromFile(id);
        if (commit2 == null) {
            return null;
        }

        /** Travel from the commit indicated by branchName back,
         *  util we came across the latest common ancestor of these two commit. */
        /** O(n). n : length from intial commit to the commit indicated by branchName. */
        String parent2;
        while (true) {
            parent2 = commit2.getfirstParent();
            /** The latest common ancestor of these two commit. */
            /** Expected time complexity of the op contains in Hashset if O(1). **/
            if (ancestrorSha1Set.contains(commit2.getSHA1())) {
                break;
            }
            /** In case of bad commit structure. */
            if (parent2 == null) {
                break;
            }
            commit2 = Commit.readCommitFromFile(parent2);
        }

//        if (parent2 == null) { }
        return commit2.getSHA1();
    }

    /** Do check work for merge.
     * @param branchName : name of branch.
     * @param splitSha1 : sha1 String of split commit.
     * @param commitSHA1 SHA1 String of branchName.
     */
    protected static void mergeCheck(String branchName, String splitSha1, String commitSHA1) {
        /** If there are staged additions or removals exist, print the error message and exit. */
        if (!Blob.isRemovalEmpty() || !Blob.isBlobMapEmpty()) {
            Utils.abort("You have uncommitted changes.");
        }

        Commit branchCommit = Commit.readCommitFromFile(commitSHA1);
        HEAD.readHEAD();
        /** If attempting to merge a branch with itself, print the error message. */
        if (branchName.equals(HEAD.getPointBranch())) {
            abort("Cannot merge a branch with itself.");
        }

        /** If the merge is complete, and the operation ends with the message. */
        if (ancestrorSha1Set.contains(branchCommit.getSHA1())) {
            abort("Given branch is an ancestor of the current branch.");
        }
        /** If the split point is the current branch, then the effect is to check out
         * the given branch, and the operation ends after printing the message */
        if (splitSha1 != null && splitSha1.equals(HEAD.whichCommit())) {
            Repository.checkout(branchName);
            abort("Current branch fast-forwarded.");
        }
        Repository.checkUncommited();
        Repository.checkUnstaged();
    }

    /** Read the commit (current, split and other) in. */
    private static void readCommit(String splitSha1, String commitSha1) {
        split = Commit.readCommitFromFile(splitSha1);
        current = Commit.readCommitFromFile(HEAD.whichCommit());
        other = Commit.readCommitFromFile(commitSha1);
    }

    /** Driver method for merge.
     * @param splitSha1 : SHA1 String of "split" commit.
     * @param commitSHA1 : SHA1 String of "other" commit.
     */
    protected static void doMerge(String splitSha1, String commitSHA1) {
        /** Read the three commit in. */
        readCommit(splitSha1, commitSHA1);
        traverseSplitMap();
        traverseCurMap();
        traverseOtherMap();
        if (isConflict) {
            message("Encountered a merge conflict.");
        }
    }

    /** Helper function for doMerge.
     *  Traverse the fileMap of "split" commit and check the files.
     * */
    private static void traverseSplitMap() {
        String errMsg = "File does not exist in ";
        for (Map.Entry<String, String> entry : split.fileMap.entrySet()) {
            String fileName = entry.getKey();
            Boolean isInCurrent = current.isFilemapContains(fileName);
            Boolean isInOther = other.isFilemapContains(fileName);
            File otherFile = null;
            File currentFile = null;
            File splitFile = split.getFilefromCommit(fileName, errMsg + "split.");
            /** count = 3 : file both in current and other.
             *  count = 2 : file in current but not in other.
             *  count = 1 : file in other but not in current. */
            int count = 0;
            if (isInCurrent && isInOther) {
                count = 3;
                otherFile = other.getFilefromCommit(fileName, errMsg + "other.");
                currentFile = current.getFilefromCommit(fileName, errMsg + "current.");
            } else if (!isInCurrent && !isInOther) {
                count = 0;
            } else if (!isInOther) {
                currentFile = current.getFilefromCommit(fileName, errMsg + "current.");
                count = 2;
            } else if (!isInCurrent) {
                otherFile = other.getFilefromCommit(fileName, errMsg + "other.");
                count = 1;
            }

            switch (count) {
                case 3: /** **/
                    Boolean modifiedInOther = !isFileSame(splitFile, otherFile);
                    Boolean modifiedInCurrent = !isFileSame(splitFile, currentFile);
                    if (modifiedInCurrent && modifiedInOther) {
                        /** Modified in other and HEAD. **/
                        Boolean modifiedSameWay = isFileSame(otherFile, currentFile);
                        if (!modifiedSameWay) {
                            /**In different way: in conflict. **/
                            overwriteConfilctFile(currentFile, otherFile, fileName);
                            isConflict = true;
                        }
                        /** ELSE : in the same way, be left unchanged. */
                    } else if (!modifiedInCurrent) {
                        /** 1. Modified in other but not in HEAD: be checked out and staged. **/
                        String otherFileName = other.getCommitedFileFromFilemap(fileName);
                        File dir = Utils.join(Repository.COMMITED_DIR, otherFileName);
                        File dest = join(Repository.CWD, fileName);
                        Utils.secureCopyFile(dir, dest);
                        Blob.add(fileName);
                    } else if (!modifiedInOther) {
                        /** 2. Modified in HEAD but not in other. Stay as they are. **/
                    }
                    break;
                case 2:
                    Boolean unModifiedInCurrent = isFileSame(splitFile, currentFile);
                    if (unModifiedInCurrent) {
                        /**6. Unmodified in HEAD but not present in other: be removed and untracked. */
                        File file = Utils.join(Repository.CWD, fileName);
                        file.delete();
                        Blob.remove(fileName, true);
                    } else {
                        /** In different way*/
                        overwriteConfilctFile(currentFile, null, fileName);
                        isConflict = true;
                    }
                    break;
                case 1:
                    Boolean unModifiedInOther = isFileSame(splitFile, otherFile);
                    if (!unModifiedInOther) {
                        /** In different way. */
                        overwriteConfilctFile(null, otherFile, fileName);
                        isConflict = true;
                    }
                    /**ELSE: 7.Unmodified in other but not present in HEAD: remain absent.*/
                    break;
                default:
                    break;
            }
        }
    }

    /** Helper function for doMerge.
     *  Traverse the fileMap of "current" commit and check the files.
     * */
    private static void traverseCurMap() {
        String errMsg = "File does not exist in ";
        for (Map.Entry<String, String> entry : current.fileMap.entrySet()) {
            String key = entry.getKey();
            if (!split.isFilemapContains(key) && !other.isFilemapContains(key)) {
                /** 4. Not in split nor other but in HEAD: remain as they are. */
                Blob.stageForMerge(key, entry.getValue());
            } else if (!split.isFilemapContains(key)) {
                /** Both in current and in other, but absent in split. **/
                File otherFile = other.getFilefromCommit(key, errMsg + "other.");
                File currentFile = current.getFilefromCommit(key, errMsg + "current.");
                if (!isFileSame(currentFile, otherFile)) {
                    /** In different way. **/
                    overwriteConfilctFile(currentFile, otherFile, key);
                    isConflict = true;
                }
            }
        }
    }

    /** Helper function for doMerge.
     *  Traverse the fileMap of "other" commit and check the files.
     * */
    private static void traverseOtherMap() {
        for (Map.Entry<String, String> entry : other.fileMap.entrySet()) {
            String key = entry.getKey();
            if (!split.isFilemapContains(key) && !current.isFilemapContains(key)) {
                /** 5. Not in split nor HEAD but in other: be checked out and staged. */
                String fileId = other.getCommitedFileFromFilemap(key);
                File dir = Utils.join(Repository.COMMITED_DIR, fileId);
                File dest = join(Repository.CWD, key);
                Utils.secureCopyFile(dir, dest);
                Blob.stageForMerge(key, entry.getValue());
            }
        }

    }

    /** Overwrite conflict file.
     *  FORMAT:
     *          <<<<<<< HEAD
     *          contents of file in current branch
     *          =======
     *          contents of file in given branch
     *          >>>>>>>
     * */
    private static void overwriteConfilctFile(File currFile, File otherFile, String fileName) {
        File newFile = Utils.join(Repository.CWD, fileName);
        if (newFile.exists()) { newFile.delete(); }
        try {
            newFile.createNewFile();
        } catch (IOException ioexcp) {
            System.out.println(ioexcp.getMessage());
        }
        String headStr = "<<<<<<< HEAD\n";
        String sepStr = "=======\n";
        String endStr = ">>>>>>>\n";
        if (currFile != null && otherFile != null) {
            byte[] headbyte = readContents(currFile);
            byte[] otherbyte = readContents(otherFile);
            Utils.writeContents(newFile, headStr, headbyte, sepStr, otherbyte, endStr);
        } else if (currFile == null) {
            byte[] otherbyte = readContents(otherFile);
            Utils.writeContents(newFile, headStr, sepStr, otherbyte, endStr);
        } else if (otherFile == null) {
            byte[] headbyte = readContents(currFile);
            Utils.writeContents(newFile, headStr, headbyte, sepStr, endStr);
        }
        Blob.add(fileName);
    }

}


    /** Old Method for doing merge actually.
     * @param splitSha1
     * @param commitSHA1
     */
//    private static void doMerge(String splitSha1, String commitSHA1) {
//        /** Read the three commit in. */
//        Commit split;
//        split = Commit.readCommitFromFile(splitSha1);
//        Commit current = Commit.readCommitFromFile(HEAD.whichCommit());
//        Commit other = Commit.readCommitFromFile(commitSHA1);
//
//        Boolean isConflict = false;
//        for (Map.Entry<String, String> entry : split.fileMap.entrySet()) {
//            String fileName = entry.getKey();
//            Boolean isInCurrent = current.isFilemapContains(fileName);
//            Boolean isInOther = other.isFilemapContains(fileName);
//            if (isInCurrent && isInOther) {
//                /** **/
//                File otherFile = other.getFilefromCommit(fileName);
//                File currentFile = current.getFilefromCommit(fileName);
//                File splitFile = split.getFilefromCommit(fileName);
//                Boolean modifiedInOther = !isFileSame(splitFile, otherFile);
//                Boolean modifiedInCurrent = !isFileSame(splitFile, currentFile);
//                if (modifiedInCurrent && modifiedInOther) {
//                    /** Modified in other and HEAD. **/
//                    Boolean modifiedSameWay = isFileSame(otherFile, currentFile);
//                    if (!modifiedSameWay) {
//                        /**In different way: in conflict. **/
//                        overwriteConfilctFile(currentFile, otherFile, fileName);
//                        isConflict = true;
//                    }
//                    /** ELSE : in the same way, be left unchanged. */
//                } else if (!modifiedInCurrent) {
//                    /** 1. Modified in other but not in HEAD: be checked out and staged. **/
//                    String otherFileName = other.getCommitedFileFromFilemap(fileName);
//                    File dir = Utils.join(Repository.COMMITED_DIR, otherFileName);
//                    File dest = join(CWD, fileName);
//                    Utils.secureCopyFile(dir, dest);
//                    Blob.add(fileName);
//                } else if (!modifiedInOther) {
//                    /** 2. Modified in HEAD but not in other. Stay as they are. **/
//                }
//            } else if (!isInCurrent && !isInOther) {
//                /** Both be removed: be left unchanged. **/
//            } else if (!isInOther) {
//                File currentFile = current.getFilefromCommit(fileName);
//                File splitFile = split.getFilefromCommit(fileName);
//                Boolean unModifiedInCurrent = isFileSame(splitFile, currentFile);
//                if (unModifiedInCurrent) {
//                    /** 6. Unmodified in HEAD but not present in other: be removed and untracked. */
//                    File file = Utils.join(CWD, fileName);
//                    file.delete();
//                    Blob.remove(fileName, true);
//                } else {
//                    /** In different way*/
//                    overwriteConfilctFile(currentFile, null, fileName);
//                    isConflict = true;
//                }
//            } else if (!isInCurrent) {
//                File otherFile = other.getFilefromCommit(fileName);
//                File splitFile = split.getFilefromCommit(fileName);
//                Boolean unModifiedInOther = isFileSame(splitFile, otherFile);
//                if (!unModifiedInOther) {
//                    /** In different way. */
//                    overwriteConfilctFile(null, otherFile, fileName);
//                    isConflict = true;
//                }
//                /**ELSE:7.Unmodified in other but not present in HEAD: remain absent.*/
//            }
//        }
//
//        for (Map.Entry<String, String> entry : current.fileMap.entrySet()) {
//            String key = entry.getKey();
//            if (!split.isFilemapContains(key) && !other.isFilemapContains(key)) {
//                /** 4. Not in split nor other but in HEAD: remain as they are. */
//                Blob.stageForMerge(key, entry.getValue());
//            } else if (!split.isFilemapContains(key)) {
//                /** Both in current and in other, but absent in split. **/
//                File otherFile = other.getFilefromCommit(key);
//                File currentFile = current.getFilefromCommit(key);
//                if (!isFileSame(currentFile, otherFile)) {
//                    /** In different way. **/
//                    overwriteConfilctFile(currentFile, otherFile, key);
//                    isConflict = true;
//                }
//            }
//        }
//
//        for (Map.Entry<String, String> entry : other.fileMap.entrySet()) {
//            String key = entry.getKey();
//            if (!split.isFilemapContains(key) && !current.isFilemapContains(key)) {
//                /** 5. Not in split nor HEAD but in other: be checked out and staged. */
//                String fileId = other.getCommitedFileFromFilemap(key);
//                File dir = Utils.join(Repository.COMMITED_DIR, fileId);
//                File dest = join(CWD, key);
//                Utils.secureCopyFile(dir, dest);
//                Blob.stageForMerge(key, entry.getValue());
//            }
//        }
//        if (isConflict) {
//            System.out.println("Encountered a merge conflict.");
//        }
//    }