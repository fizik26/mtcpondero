package pondero.util;

import static pondero.Logger.error;
import java.io.File;
import pondero.Globals;

public class FileUtil {

    public static void deleteTempFiles() {
        for (File file : Globals.getFolderResultsTemp().listFiles()) {
            try {
                file.deleteOnExit();
            } catch (Exception e) {
                error(e);
            }
        }
    }

}