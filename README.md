# OboePlayer

Extract and save wallpaper from an Android phone — even if you lost the original file.

## Installation

There is no Play page because Android is a platform, not a private party.
Download the APK release (or build it yourself) and sideload it (either via ADB or by pushing it onto a phone via MTP and opening in a file manager).
(In fact, you can open this very page on your phone (or tablet), navigate to "Releases" and save a trip.)

## Use

* Click "Unlock" to request permisions to access your media collection. Click "Allow".
* Click "main" for the home screen wallpaper or "lock" for the lock screen wallpaper.
* Optionally, edit the file name.
* Click "save".

The application will place the output file in `Pictures`; if the `Pictures` folder is not found, it will be placed in the storage root. If the file with the name you provided already exists, a numeric suffix will be added. No file will ever be overwritten.

## Quality

The application will save the wallpaper bitmap as made available by the [WallpaperManager](https://developer.android.com/reference/android/app/WallpaperManager) component. It may be clipped or downscaled relative to the original file.
If the wallpaper is not a bitmap (i.e. it is a live wallpaper drawn by a script), two images will be created corresponding to your screen's portrait and landscape resolutions and the wallpaper will be painted into each.

## Compatibility

Technically, the involved APIs should work since the earliest versions of Android. The API level in the project is arbitrarily set to 16 (JellyBean); you can change that and rebuild.
It's possible to retrieve the lock screen wallpaper since API 24 (early Nougat); until then, only the home screen wallpaper is supported.

## Feature requests

If you'd like to enhance the functionality of the application or the level of detail of the documentation, file an issue. If you already have a solution on your mind, you can fork the repo and post a pull request right away.

## Design and branding

"Oboee" (обои) is the Russian word for "wallpaper"; hence the wordplay.

The name of the cat on the application icon is Ambrose. He is roughly 8 years old in 2022 (was approximately 7 when the picture was taken and speculatively 6 when we adopted him from the streets). His family tongue seems to have been Mexican Spanish.
