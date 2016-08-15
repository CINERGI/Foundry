package org.neuinfo.foundry.common.util;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by bozyurt on 12/1/15.
 */
public class LargeDataSetDirectoryAssigner {
    private int maxNumOfFilesPerDir = 2000;
    private int fileCountInterval = 100; // every 100 internal counts count the number of files in the last dir;
    private int totalFileCount = 0;
    private int lastDirNo = 1;
    private File rootDir;
    private int dirFileCount = 0;
    private static Map<String, LargeDataSetDirectoryAssigner> instanceMap = new HashMap<String, LargeDataSetDirectoryAssigner>();

    public synchronized static LargeDataSetDirectoryAssigner getInstance(String rootDir) {
        return getInstance(rootDir, 2000);
    }

    public synchronized static LargeDataSetDirectoryAssigner getInstance(String rootDir, int maxNumOfFilesPerDir) {
        LargeDataSetDirectoryAssigner instance = instanceMap.get(rootDir);
        if (instance == null) {
            instance = new LargeDataSetDirectoryAssigner(rootDir, maxNumOfFilesPerDir);
            instanceMap.put(rootDir, instance);
        }
        return instance;
    }


    private LargeDataSetDirectoryAssigner(String rootDir, int maxNumOfFilesPerDir) {
        this.rootDir = new File(rootDir);
        this.maxNumOfFilesPerDir = maxNumOfFilesPerDir;
        Assertion.assertTrue(this.rootDir.isDirectory());
        File firstDir = new File(rootDir, String.format("%04d", lastDirNo));
        firstDir.mkdir();
        Assertion.assertTrue(firstDir.isDirectory(), "Cannot create dir:" + firstDir);
    }

    /**
     * not synchronized intentionally to be used in a single thread
     *
     * @return
     */
    public File getNextDirPath() {
        dirFileCount++;
        ++totalFileCount;
        if (dirFileCount > maxNumOfFilesPerDir) {
            ++lastDirNo;
            dirFileCount = 1;
            File currentDir = new File(rootDir, String.format("%04d", lastDirNo));
            while(currentDir.isDirectory()) {
                ++lastDirNo;
                currentDir = new File(rootDir, String.format("%04d", lastDirNo));
            }
            currentDir.mkdir();
            Assertion.assertTrue(currentDir.isDirectory(),"Cannot create dir:" + currentDir);
        }
        return new File(rootDir, String.format("%04d", lastDirNo));
    }


    public static void main(String[] args) {
        LargeDataSetDirectoryAssigner instance = LargeDataSetDirectoryAssigner.getInstance("/tmp/waf/Data.gov",50);

        for (int i = 1; i <= 213; i++) {
            System.out.println(i + ": " + instance.getNextDirPath());
        }
    }

}
