/*
 * *
 *  * Created by Muhammet Ali YÜCE on 3/5/19 4:26 PM
 *  * on Github: /mayuce
 *  * Copyright (c) 2019 . All rights reserved.
 *  * Last modified 3/5/19 3:57 PM
 *
 */

package team.clevel.documentscanner.helpers;

import android.graphics.Bitmap;

public class ScannerConstants {
    public static Bitmap selectedImageBitmap;
    public static String cropText = "Crop", backText = "Back",
            imageError = "Error while loading image",
            cropError = "You have not selected a valid field. Please make corrections until the lines are blue.";
    public static String cropColor = "#6666ff", backColor = "#ff0000", progressColor = "#331199";
    public static boolean saveStorage = false;
}
