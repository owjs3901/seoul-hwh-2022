package com.devfive.shw2022;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    CustomAdapter adapter;
    ArrayList<DataModel> list;
    Button reset;
    Button purchase;
    EditText input;
    TextView total;
    TextToSpeech tts;
    Button capture;
    File file;
    Uri uri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* tts */
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                System.out.println("wtf" + status);
                if (status != android.speech.tts.TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.KOREAN);
                }
            }
        });

        /* ????????? ?????? ??? ????????? ?????? */
        list = new ArrayList<>();
        RecyclerView recyclerView = findViewById(R.id.recycleView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        total = findViewById(R.id.total);
        reset = findViewById(R.id.reset);
        purchase = findViewById(R.id.purchase);
        adapter = new CustomAdapter(list, total, purchase, reset);
        recyclerView.setAdapter(adapter);

        /* ????????? ????????? */
        reset.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setMessage("????????? ???????????????????")
                    .setPositiveButton("?????????", (dialog, which) -> {
                        adapter.clearData();
                    })
                    .setNegativeButton("??????", (dialog, which) -> {
                        dialog.dismiss();
                    });
            AlertDialog msgDlg = builder.create();
            msgDlg.show();
        });

        /* ?????? ?????? */
        purchase.setOnClickListener(v -> {
            tts(adapter.sum + "?????? ?????????????????????????");
            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setTitle("?????? ??????")
                    .setMessage(adapter.sum + "?????? ?????????????????????????")
                    .setPositiveButton("??????", (dialog, which) -> {
                        Intent intent = new Intent(getApplicationContext(), ResultActivity.class);
                        startActivity(intent);
                        tts("????????? ?????????????????????.");
                        adapter.clearData();
                    })
                    .setNegativeButton("??????", (dialog, which) -> {
                        dialog.dismiss();
                    });
            AlertDialog msgDlg = builder.create();
            msgDlg.show();
        });

        /* ????????? ??????????????? */
        input = findViewById(R.id.input);
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().contains("\n")) {
                    //???????????? ??????????????????.
                    String barcode = s.toString();
                    switch (barcode.replace("\n", "")) {
                        case "8801056028855":
                            adapter.addData("??????", 2000);
                            tts("????????? ??????????????????");
                            break;
                        case "8801121764510":
                            adapter.addData("??????", 1500);
                            tts("????????? ??????????????????");
                            break;
                        case "8886467105333":
                            adapter.addData("????????????", 2500);
                            tts("??????????????? ??????????????????");
                            break;
                        case "8801223007478":
                            adapter.addData("?????????", 1500);
                            tts("?????????????????? ??????????????????");
                            break;
                        case "4897036693728":
                            adapter.addData("????????? ?????????", 2000);
                            tts("????????? ???????????? ??????????????????");
                            break;
                        default:
                            Toast.makeText(MainActivity.this, "????????? ??? ?????? ??????????????????", Toast.LENGTH_SHORT).show();
                            System.out.println(barcode);
                    }
                    input.setText(null);
                }
            }
        });
        /* ????????? ?????? */
        capture = findViewById(R.id.capture);
        capture.setOnClickListener(v -> {
            takePicture();
        });
    }

    ActivityResultLauncher<Intent> mGetContent = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            (ActivityResultCallback<ActivityResult>) res -> {
                if (res.getResultCode() != RESULT_OK)
                    return;
                Bundle extras = res.getData().getExtras();
                Bitmap imageBitmap = (Bitmap) extras.get("data");
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                OkHttpClient client = new OkHttpClient();
                String URLString = "http://172.16.30.31:5000/image";
                Request request = new Request.Builder()
                        .addHeader("content-type", "multipart/form-data")
                        .url(URLString)
                        .post(new MultipartBody.Builder().setType(MultipartBody.FORM)
                                .addFormDataPart("file", "img.jpg", RequestBody.create(stream.toByteArray(), MediaType.parse("image/*jpg")))
                                .build())
                        .build();
                System.out.println("POST: calling: " + URLString);
                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        String d = response.body().string();
                        runOnUiThread(() -> {
                            if (d.contains("can"))
                                adapter.addData("??????", 2000);
                            else if (d.contains("cell"))
                                adapter.addData("?????????", 1000);
                            else
                                Toast.makeText(MainActivity.this, "????????? ???????????? ???????????????", Toast.LENGTH_SHORT).show();
                        });

                    }
                });
            });

    public void takePicture() {
        mGetContent.launch(new Intent(MediaStore.ACTION_IMAGE_CAPTURE));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    private File createFile() {
        String filename = "capture.jpg";
        File outFile = new File(getFilesDir(), filename);
        Log.d("Main", "File path : " + outFile.getAbsolutePath());

        return outFile;
    }

    /* tts */
    public void TTSActivity(String str, int flag) {
        String text = null;
        if (flag == 0)
            text = str + "??? ?????????????????????";
        else if (flag == 1)
            text = str + "?????? ?????????????????????????";
        else if (flag == 2)
            text = str;
        tts.setPitch(1.0f);
        tts.setSpeechRate(1.0f);
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }
    public void tts(String str) {
        OkHttpClient client = new OkHttpClient();
        String URLString = "https://translate.google.com/translate_tts?ie=UTF-8&tl=ko-KR&client=tw-ob&q="+str;
        Request request = new Request.Builder()
                .url(URLString)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                playMp3(response.body().bytes());
            }
        });
    }

    private MediaPlayer mediaPlayer = new MediaPlayer();
    private void playMp3(byte[] mp3SoundByteArray) {
        try {
            File tempMp3 = File.createTempFile("kurchina", "mp3", getCacheDir());
            tempMp3.deleteOnExit();
            FileOutputStream fos = new FileOutputStream(tempMp3);
            fos.write(mp3SoundByteArray);
            fos.close();

            mediaPlayer.reset();
            FileInputStream fis = new FileInputStream(tempMp3);
            mediaPlayer.setDataSource(fis.getFD());

            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException ex) {
            String s = ex.toString();
            ex.printStackTrace();
        }
    }
}

