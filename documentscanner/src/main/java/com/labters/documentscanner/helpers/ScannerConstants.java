/*
 * *
 *  * Created by Ali YÜCE on 3/2/20 11:18 PM
 *  * https://github.com/mayuce/
 *  * Copyright (c) 2020 . All rights reserved.
 *  * Last modified 3/2/20 11:10 PM
 *
 */

package com.labters.documentscanner.helpers;

import android.graphics.Bitmap;

public class ScannerConstants {
    public static Bitmap selectedImageBitmap;
    public static String cropText="KIRP",backText="KAPAT",
            imageError="Görsel seçilmedi, lütfen tekrar deneyin.",
            cropError="Geçerli bir alan seçmediniz. Lütfen çizgiler mavi olana dek düzeltme yapın.";
    public static String cropColor="#6666ff",backColor="#ff0000",progressColor="#331199";
    public static boolean saveStorage=false;
}
