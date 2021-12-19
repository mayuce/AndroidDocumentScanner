# Android Document Scanner Library

image::https://img.shields.io/badge/version-1.6.1-green.svg[]
image::https://img.shields.io/badge/minSDK-21-blue.svg[]
image::https://img.shields.io/badge/license-MIT-yellowgreen.svg[]

If you liked the work you can think about https://www.paypal.com/donate/?hosted_button_id=MCMYPAN46SKFA[Donate via Paypal].

This library helps you to scan any document like CamScanner.

image::documentscannerMockup.png[]

## Requirements

Add line below to your *top* level build.gradle

[source,bourne]
----
allprojects {
    repositories {
        /// ....
        mavenCentral()
    }
}
----

Add lines below to your *app* level build.gradle

[source,bourne]
----
    implementation 'io.github.mayuce:AndroidDocumentScanner:1.6.1'
----

And Sync the gradle

## Usage

New version of this library provides you the gear instead of ready to go car. So you need to build your own implementation with it, before you upgrade your project make sure that you know it's gonna be a braking change in your project.

* Add DocumentScannerView to your layout

[source,xml]
----
...
            <com.labters.documentscanner.DocumentScannerView
                    android:id="@+id/document_scanner"
                    android:layout_width="match_parent"
                    android:layout_height="match_parent" />
...
----

* Set loading listener

[source,kotlin]
----
        binding.documentScanner.setOnLoadListener { loading ->
            binding.progressBar.isVisible = loading
        }
----

* Set selected Bitmap into the view

[source,kotlin]
----
        binding.documentScanner.setImage(bitmap)
----

* After selecting the edge points get cropped image

[source,kotlin]
----
        binding.btnImageCrop.setOnClickListener {
            lifecycleScope.launch {
                binding.progressBar.isVisible = true
                val image = binding.documentScanner.getCroppedImage()
                binding.progressBar.isVisible = false
                binding.resultImage.isVisible = true
                binding.resultImage.setImageBitmap(image)
            }
        }
----

### Additional Features

On above Android 9.0 there is magnifier to help user to see zoomed image to crop.

If you face with any issues you can take a look at com.labters.documentscannerandroid.ImageCropActivity to see how does it works.

## TO-DO

- Nothing so far, you may tell me?..

## Thanks

* Thanks OpenCV for this awesome library. - https://opencv.org/

* Inspiration from *aashari* . Thanks to him for his https://github.com/aashari/android-opencv-camera-scanner[source codes].

[source,bourne]
----
MIT License

Copyright (c) 2020 Muhammet Ali YUCE

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
----
