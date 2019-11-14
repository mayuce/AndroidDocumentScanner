/*
 * *
 *  * Created by Muhammet Ali YÜCE on 3/5/19 4:26 PM
 *  * on Github: /mayuce
 *  * Copyright (c) 2019 . All rights reserved.
 *  * Last modified 3/5/19 4:16 PM
 *
 */

package team.clevel.documentscanner;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import team.clevel.documentscanner.helpers.ScannerConstants;
import team.clevel.documentscanner.libraries.NativeClass;
import team.clevel.documentscanner.libraries.PolygonView;

public class ImageCropActivity extends Activity {

    private FrameLayout holderImageCrop;
    private ImageView ivRotate, ivInvert, ivRebase;
    private ImageView imageView;
    private PolygonView polygonView;
    private Bitmap selectedImageBitmap, tempBitmapOrginal;
    private Button btnImageCrop, btnClose;
    private NativeClass nativeClass;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_crop);
        if (ScannerConstants.selectedImageBitmap != null)
            initializeElement();
        else {
            Toast.makeText(this, ScannerConstants.imageError, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void setImageRotation() {
        Bitmap tempBitmap = ScannerConstants.selectedImageBitmap.copy(ScannerConstants.selectedImageBitmap.getConfig(), true);
        for (int i = 1; i <= 4; i++) {
            MatOfPoint2f point2f = nativeClass.getPoint(tempBitmap);
            if (point2f == null) {
                tempBitmap = rotateBitmap(tempBitmap, 90 * i);
            } else {
                ScannerConstants.selectedImageBitmap = tempBitmap.copy(ScannerConstants.selectedImageBitmap.getConfig(), true);
                break;
            }
        }
    }

    public static Bitmap rotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    private void setProgressBar(boolean isShow) {
        RelativeLayout rlContainer = findViewById(R.id.rlContainer);
        setViewInterract(rlContainer, !isShow);
        if (isShow)
            progressBar.setVisibility(View.VISIBLE);
        else
            progressBar.setVisibility(View.GONE);
    }

    private void setViewInterract(View view, boolean canDo) {
        view.setEnabled(canDo);
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                setViewInterract(((ViewGroup) view).getChildAt(i), canDo);
            }
        }
    }

    @SuppressLint({"CheckResult", "ClickableViewAccessibility"})
    private void initializeElement() {
        nativeClass = new NativeClass();
        btnImageCrop = findViewById(R.id.btnImageCrop);
        btnClose = findViewById(R.id.btnClose);
        holderImageCrop = findViewById(R.id.holderImageCrop);
        imageView = findViewById(R.id.imageView);
        ivRotate = findViewById(R.id.ivRotate);
        ivRebase = findViewById(R.id.ivRebase);
        btnImageCrop.setText(ScannerConstants.cropText);
        btnClose.setText(ScannerConstants.backText);
        polygonView = findViewById(R.id.polygonView);
        progressBar = findViewById(R.id.progressBar);
        if (progressBar.getIndeterminateDrawable() != null && ScannerConstants.progressColor != null)
            progressBar.getIndeterminateDrawable().setColorFilter(Color.parseColor(ScannerConstants.progressColor), android.graphics.PorterDuff.Mode.MULTIPLY);
        else if (progressBar.getProgressDrawable() != null && ScannerConstants.progressColor != null)
            progressBar.getProgressDrawable().setColorFilter(Color.parseColor(ScannerConstants.progressColor), android.graphics.PorterDuff.Mode.MULTIPLY);
        setProgressBar(true);
        btnImageCrop.setBackgroundColor(Color.parseColor(ScannerConstants.cropColor));
        btnClose.setBackgroundColor(Color.parseColor(ScannerConstants.backColor));
        Observable.fromCallable(() -> {
            setImageRotation();
            return false;
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((result) -> {
                    setProgressBar(false);
                    holderImageCrop.post(this::initializeCropping);
                    btnImageCrop.setOnClickListener(btnImageEnhanceClick);
                    btnClose.setOnClickListener(btnCloseClick);
                    ivRotate.setOnClickListener(onRotateClick);
                    ivInvert.setOnClickListener(btnInvertColor);
                    ivRebase.setOnClickListener(btnRebase);
                });
    }


    private void initializeCropping() {

        selectedImageBitmap = ScannerConstants.selectedImageBitmap;
        tempBitmapOrginal = selectedImageBitmap.copy(selectedImageBitmap.getConfig(), true);
        ScannerConstants.selectedImageBitmap = null;

        Bitmap scaledBitmap = scaledBitmap(selectedImageBitmap, holderImageCrop.getWidth(), holderImageCrop.getHeight());
        imageView.setImageBitmap(scaledBitmap);

        Bitmap tempBitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
        Map<Integer, PointF> pointFs = null;
        try {
            pointFs = getEdgePoints(tempBitmap);
            polygonView.setPoints(pointFs);
            polygonView.setVisibility(View.VISIBLE);

            int padding = (int) getResources().getDimension(R.dimen.scanPadding);

            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(tempBitmap.getWidth() + 2 * padding, tempBitmap.getHeight() + 2 * padding);
            layoutParams.gravity = Gravity.CENTER;

            polygonView.setLayoutParams(layoutParams);
            polygonView.setPointColor(getResources().getColor(R.color.blue));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private View.OnClickListener btnImageEnhanceClick = new View.OnClickListener() {
        @SuppressLint("CheckResult")
        @Override
        public void onClick(View v) {
            setProgressBar(true);
            Observable.fromCallable(() -> {
                ScannerConstants.selectedImageBitmap = getCroppedImage();
                if (ScannerConstants.selectedImageBitmap == null)
                    return false;
                if (ScannerConstants.saveStorage)
                    saveToInternalStorage(ScannerConstants.selectedImageBitmap);
                return false;
            })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe((result) -> {
                        setProgressBar(false);
                        if (ScannerConstants.selectedImageBitmap != null) {
                            setResult(RESULT_OK);
                            finish();
                        }
                    });
        }
    };

    private View.OnClickListener btnRebase = v -> {
        ScannerConstants.selectedImageBitmap = tempBitmapOrginal.copy(tempBitmapOrginal.getConfig(), true);
        initializeElement();
    };

    private View.OnClickListener btnCloseClick = v -> finish();

    private void invertColor() {
            Bitmap bmpMonochrome = Bitmap.createBitmap(selectedImageBitmap.getWidth(), selectedImageBitmap.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bmpMonochrome);
            ColorMatrix ma = new ColorMatrix();
            ma.setSaturation(0);
            Paint paint = new Paint();
            paint.setColorFilter(new ColorMatrixColorFilter(ma));
            canvas.drawBitmap(selectedImageBitmap, 0, 0, paint);
            selectedImageBitmap = bmpMonochrome.copy(bmpMonochrome.getConfig(), true);

            Mat src = new Mat(selectedImageBitmap.getHeight(), selectedImageBitmap.getWidth(), CvType.CV_8UC4);
            Utils.bitmapToMat(selectedImageBitmap, src);
            Mat src_gray = new Mat(selectedImageBitmap.getHeight(), selectedImageBitmap.getWidth(), CvType.CV_8UC1);

            Imgproc.cvtColor(src, src_gray, Imgproc.COLOR_BGR2GRAY);
            Imgproc.adaptiveThreshold(src_gray, src_gray, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 31, 8);
            Imgproc.cvtColor(src_gray, src, Imgproc.COLOR_GRAY2RGBA, 4);

            Bitmap processedImage = Bitmap.createBitmap(src.cols(), src.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(src, processedImage);

            selectedImageBitmap = processedImage;
    }

    private View.OnClickListener btnInvertColor = new View.OnClickListener() {
        @SuppressLint("CheckResult")
        @Override
        public void onClick(View v) {
            setProgressBar(true);
            Observable.fromCallable(() -> {
                invertColor();
                return false;
            })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe((result) -> {
                        setProgressBar(false);
                        Bitmap scaledBitmap = scaledBitmap(selectedImageBitmap, holderImageCrop.getWidth(), holderImageCrop.getHeight());
                        imageView.setImageBitmap(scaledBitmap);
                    });


        }
    };

    private View.OnClickListener onRotateClick = new View.OnClickListener() {
        @SuppressLint("CheckResult")
        @Override
        public void onClick(View v) {
            setProgressBar(true);
            Observable.fromCallable(() -> {
                ScannerConstants.selectedImageBitmap = rotateBitmap(selectedImageBitmap, 90);
                return false;
            })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe((result) -> {
                        setProgressBar(false);
                        initializeElement();
                    });
        }
    };

    private String saveToInternalStorage(Bitmap bitmapImage) {
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "cropped_" + timeStamp + ".png";
        File mypath = new File(directory, imageFileName);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mypath);
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return directory.getAbsolutePath();
    }

    protected Bitmap getCroppedImage() {
        try {
            invertColor();

            Map<Integer, PointF> points = polygonView.getPoints();

            float xRatio = (float) selectedImageBitmap.getWidth() / imageView.getWidth();
            float yRatio = (float) selectedImageBitmap.getHeight() / imageView.getHeight();

            float x1 = (points.get(0).x) * xRatio;
            float x2 = (points.get(1).x) * xRatio;
            float x3 = (points.get(2).x) * xRatio;
            float x4 = (points.get(3).x) * xRatio;
            float y1 = (points.get(0).y) * yRatio;
            float y2 = (points.get(1).y) * yRatio;
            float y3 = (points.get(2).y) * yRatio;
            float y4 = (points.get(3).y) * yRatio;
            return nativeClass.getScannedBitmap(selectedImageBitmap, x1, y1, x2, y2, x3, y3, x4, y4);
        } catch (Exception e) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(ImageCropActivity.this, ScannerConstants.cropError, Toast.LENGTH_SHORT).show();
                }
            });
            return null;
        }
    }

    private Bitmap scaledBitmap(Bitmap bitmap, int width, int height) {
        Matrix m = new Matrix();
        m.setRectToRect(new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight()), new RectF(0, 0, width, height), Matrix.ScaleToFit.CENTER);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
    }

    private Map<Integer, PointF> getEdgePoints(Bitmap tempBitmap) throws Exception {
        List<PointF> pointFs = getContourEdgePoints(tempBitmap);
        Map<Integer, PointF> orderedPoints = orderedValidEdgePoints(tempBitmap, pointFs);
        return orderedPoints;
    }

    private List<PointF> getContourEdgePoints(Bitmap tempBitmap) {
        MatOfPoint2f point2f = nativeClass.getPoint(tempBitmap);
        if (point2f == null)
            point2f = new MatOfPoint2f();
        List<Point> points = Arrays.asList(point2f.toArray());
        List<PointF> result = new ArrayList<>();
        for (int i = 0; i < points.size(); i++) {
            result.add(new PointF(((float) points.get(i).x), ((float) points.get(i).y)));
        }

        return result;

    }

    private Map<Integer, PointF> getOutlinePoints(Bitmap tempBitmap) {
        Map<Integer, PointF> outlinePoints = new HashMap<>();
        outlinePoints.put(0, new PointF(0, 0));
        outlinePoints.put(1, new PointF(tempBitmap.getWidth(), 0));
        outlinePoints.put(2, new PointF(0, tempBitmap.getHeight()));
        outlinePoints.put(3, new PointF(tempBitmap.getWidth(), tempBitmap.getHeight()));
        return outlinePoints;
    }

    private Map<Integer, PointF> orderedValidEdgePoints(Bitmap tempBitmap, List<PointF> pointFs) {
        Map<Integer, PointF> orderedPoints = polygonView.getOrderedPoints(pointFs);
        if (!polygonView.isValidShape(orderedPoints)) {
            orderedPoints = getOutlinePoints(tempBitmap);
        }
        return orderedPoints;
    }


}
