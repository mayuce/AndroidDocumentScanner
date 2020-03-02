/*
 * *
 *  * Created by Ali YÜCE on 3/2/20 11:18 PM
 *  * https://github.com/mayuce/
 *  * Copyright (c) 2020 . All rights reserved.
 *  * Last modified 3/2/20 11:10 PM
 *
 */

package com.labters.documentscanner.base;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.labters.documentscanner.R;
import com.labters.documentscanner.libraries.NativeClass;
import com.labters.documentscanner.libraries.PolygonView;

import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public abstract class DocumentScanActivity extends AppCompatActivity {

    protected CompositeDisposable disposable = new CompositeDisposable();
    private Bitmap selectedImage;
    private NativeClass nativeClass = new NativeClass();

    protected abstract FrameLayout getHolderImageCrop();

    protected abstract ImageView getImageView();

    protected abstract PolygonView getPolygonView();

    protected abstract void showProgressBar();

    protected abstract void hideProgressBar();

    protected abstract void showError(CropperErrorType errorType);

    protected abstract Bitmap getBitmapImage();

    private void setImageRotation() {
        Bitmap tempBitmap = selectedImage.copy(selectedImage.getConfig(), true);
        for (int i = 1; i <= 4; i++) {
            MatOfPoint2f point2f = nativeClass.getPoint(tempBitmap);
            if (point2f == null) {
                tempBitmap = rotateBitmap(tempBitmap, 90 * i);
            } else {
                selectedImage = tempBitmap.copy(selectedImage.getConfig(), true);
                break;
            }
        }
    }

    protected Bitmap rotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    private void setProgressBar(boolean isShow) {
        if (isShow)
            showProgressBar();
        else
            hideProgressBar();
    }

    protected void startCropping() {
        selectedImage = getBitmapImage();
        setProgressBar(true);
        disposable.add(Observable.fromCallable(() -> {
                    setImageRotation();
                    return false;
                })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe((result) -> {
                            initializeCropping();
                            setProgressBar(false);
                        })
        );
    }


    private void initializeCropping() {
        Bitmap scaledBitmap = scaledBitmap(selectedImage, getHolderImageCrop().getWidth(), getHolderImageCrop().getHeight());
        getImageView().setImageBitmap(scaledBitmap);

        Bitmap tempBitmap = ((BitmapDrawable) getImageView().getDrawable()).getBitmap();
        Map<Integer, PointF> pointFs = null;
        try {
            pointFs = getEdgePoints(tempBitmap);
            getPolygonView().setPoints(pointFs);
            getPolygonView().setVisibility(View.VISIBLE);

            int padding = (int) getResources().getDimension(R.dimen.scanPadding);

            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(tempBitmap.getWidth() + 2 * padding, tempBitmap.getHeight() + 2 * padding);
            layoutParams.gravity = Gravity.CENTER;

            getPolygonView().setLayoutParams(layoutParams);
            getPolygonView().setPointColor(getResources().getColor(R.color.blue));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected Bitmap getCroppedImage() {
        try {
            Map<Integer, PointF> points = getPolygonView().getPoints();

            float xRatio = (float) selectedImage.getWidth() / getImageView().getWidth();
            float yRatio = (float) selectedImage.getHeight() / getImageView().getHeight();

            float x1 = (Objects.requireNonNull(points.get(0)).x) * xRatio;
            float x2 = (Objects.requireNonNull(points.get(1)).x) * xRatio;
            float x3 = (Objects.requireNonNull(points.get(2)).x) * xRatio;
            float x4 = (Objects.requireNonNull(points.get(3)).x) * xRatio;
            float y1 = (Objects.requireNonNull(points.get(0)).y) * yRatio;
            float y2 = (Objects.requireNonNull(points.get(1)).y) * yRatio;
            float y3 = (Objects.requireNonNull(points.get(2)).y) * yRatio;
            float y4 = (Objects.requireNonNull(points.get(3)).y) * yRatio;
            return nativeClass.getScannedBitmap(selectedImage, x1, y1, x2, y2, x3, y3, x4, y4);
        } catch (Exception e) {
            showError(CropperErrorType.CROP_ERROR);
            return null;
        }
    }

    protected Bitmap scaledBitmap(Bitmap bitmap, int width, int height) {
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
        Map<Integer, PointF> orderedPoints = getPolygonView().getOrderedPoints(pointFs);
        if (!getPolygonView().isValidShape(orderedPoints)) {
            orderedPoints = getOutlinePoints(tempBitmap);
        }
        return orderedPoints;
    }

    @Override
    protected void onStop() {
        super.onStop();
        disposable.clear();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposable.dispose();
    }
}
