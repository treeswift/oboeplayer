package club.keepyourdevice.oboe;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableContainer;
import android.graphics.drawable.DrawableWrapper;
import android.media.MediaScannerConnection;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.view.Display;
import android.view.WindowManager;
import android.view.WindowMetrics;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.LinkedList;
import java.util.List;

public class OboeSavior extends AppCompatActivity {

    static final boolean postNApi = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;

    static final int UNLOCK_READ = 0x10cc;

    public static final String[] APIS = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ?
            new String[] {
                    "android.permission.READ_EXTERNAL_STORAGE",
                    "android.permission.WRITE_EXTERNAL_STORAGE",
                    "android.permission.MANAGE_EXTERNAL_STORAGE"
            } :
            new String[]  {
                    "android.permission.READ_EXTERNAL_STORAGE",
                    "android.permission.WRITE_EXTERNAL_STORAGE"
            };
    public static final String PNG_EXT = ".png";

    WallpaperManager wallman;

    Button unlockRead;
    EditText fileName;
    RadioButton bMain;
    RadioButton bLock;
    ImageView preview;
    Button buttonSave;

    Drawable curImage;
    Bitmap paperImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        wallman = WallpaperManager.getInstance(this);

        unlockRead = findViewById(R.id.unlock);
        fileName = findViewById(R.id.f_name);
        bMain = findViewById(R.id.choose_main);
        bLock = findViewById(R.id.choose_lock);
        preview = findViewById(R.id.preview);
        buttonSave = findViewById(R.id.save);

        boolean hasPerm = true;
        for(String api : APIS) {
            hasPerm &= ContextCompat.checkSelfPermission(this, api) == PackageManager.PERMISSION_GRANTED;
        }
        updateUnlockedStatus(hasPerm);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            unlockRead.setOnClickListener(v -> requestPermissions(APIS, UNLOCK_READ));
        } else {
            unlockRead.setEnabled(false);
            bMain.setEnabled(true);
            bLock.setEnabled(postNApi);
        }

        bMain.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(isChecked) {
                selectWallpaper(false);
            }
        });

        bLock.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(isChecked) {
                selectWallpaper(true);
            }
        });

        buttonSave.setOnClickListener(v -> {
            String providedName = fileName.getText().toString();
            List<File> out = new LinkedList<>();
            // now extract bitmap from drawable
            if(postNApi) {
                out.add(saveBitmap(paperImage, providedName));
            } else {
                // search for a preexisting bitmap
                BitmapDrawable bd = searchForBitmap(curImage);
                if (bd != null) {
                    out.add(saveBitmap(bd.getBitmap(), providedName));
                }
                else
                {
                    boolean tryBothOrientations = false;
                    int width = curImage.getIntrinsicWidth(), height = curImage.getIntrinsicHeight();
                    if(width <= 0 || height <= 0) {
                        tryBothOrientations = true;
                        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            WindowMetrics dd = wm.getMaximumWindowMetrics();
                            width = dd.getBounds().width();
                            height = dd.getBounds().height();
                        }
                        else {
                            Display dp = wm.getDefaultDisplay();
                            width = dp.getWidth();
                            height = dp.getHeight();
                        }
                    }

                    out.add(drawInternally(width, height, providedName));
                    if(height != width && tryBothOrientations) {
                        //noinspection SuspiciousNameCombination
                        out.add(drawInternally(height, width, providedName));
                    }
                }
            }
            // we could do the above in an AsyncTask but we don't care
            StringBuilder msg = new StringBuilder(getString(R.string.written));
            String[] paths = new String[out.size()];
            String[] mimes = new String[out.size()];
            int i = 0;
            for(File file : out) {
                final String path = file.getAbsolutePath();
                msg.append("\n").append(path);
                paths[i] = path;
                mimes[i] = "image/png";
                ++i;
            }
            MediaScannerConnection.scanFile(this, paths, mimes, null);
            new AlertDialog.Builder(this)
                    .setMessage(msg)
                    .create().show();
        });
    }

    private BitmapDrawable searchForBitmap(Drawable curImage) {
        if(curImage instanceof BitmapDrawable) {
            return (BitmapDrawable) curImage;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(curImage instanceof DrawableWrapper) {
                return searchForBitmap(((DrawableWrapper) curImage).getDrawable());
            }
        }
        //noinspection StatementWithEmptyBody
        if(curImage instanceof DrawableContainer) {
            // TODO play with selectDrawable(index);
        }
        Drawable curChild = curImage.getCurrent();
        if(curChild != curImage) {
            return searchForBitmap(curChild);
        }
        return null;
    }

    private File drawInternally(int width, int height, String providedName) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        curImage.draw(canvas);
        return saveBitmap(bitmap, providedName);
    }

    private File saveBitmap(Bitmap bitmap, String providedName) {
        File out = getAFile(providedName);
        return saveBitmap(bitmap, out);
    }

    private File saveBitmap(Bitmap bitmap, File out) {
        try {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100,
                    new FileOutputStream(out));
            return out;
        } catch (FileNotFoundException e) {
            return new File("/dev/null");
        }
    }

    private File getAFile(String providedName) {
        File dir = getPicDir();
        String name = providedName;
        File file;
        int postfix = 0;
        while((file = new File(dir, name + getString(R.string.def_ext))).exists()) {
            name = providedName + "-" + (++postfix);
        }
        return file;
    }

    private static File getPicDir() {
        File sdcard = Environment.getExternalStorageDirectory();
        File pictures = new File(sdcard, Environment.DIRECTORY_PICTURES);
        return pictures.isDirectory() ? pictures : sdcard;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == UNLOCK_READ) {
            boolean enabled = true;
            for(int res : grantResults) {
                enabled &= res == PackageManager.PERMISSION_GRANTED;
            }
            updateUnlockedStatus(enabled);
        }
    }

    private void updateUnlockedStatus(boolean enabled) {
        bMain.setEnabled(enabled);
        bLock.setEnabled(postNApi);
        unlockRead.setEnabled(!enabled);
    }

    @SuppressLint("MissingPermission")
    private void selectWallpaper(boolean lock) {
        if(postNApi) {
            ParcelFileDescriptor descriptor = wallman.getWallpaperFile(lock ? WallpaperManager.FLAG_LOCK : WallpaperManager.FLAG_SYSTEM);
            if (descriptor != null) {
                if(validate(paperImage = BitmapFactory.decodeFileDescriptor(descriptor.getFileDescriptor()))) {
                    preview.setImageBitmap(paperImage);
                }
            }
        } else {
            if(lock) {
                Toast.makeText(this, R.string.pre_nougat, Toast.LENGTH_LONG).show();
            }
            if(validate(curImage = lock ? null : wallman.getDrawable())) {
                preview.setImageDrawable(curImage);
            }
        }
    }

    private boolean validate(Object image) {
        boolean valid = image != null;
        buttonSave.setEnabled(valid);
        if(!valid) {
            preview.setImageResource(R.drawable.nothing);
        }
        return valid;
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        fileName.setSelection(fileName.getText().toString().length());
    }
}