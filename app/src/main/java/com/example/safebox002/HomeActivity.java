package com.example.safebox002;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.opengl.Visibility;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.safebox002.utils.MyEncrypter;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.crypto.NoSuchPaddingException;

public class HomeActivity extends AppCompatActivity {
    String FILE_NAME_ENC;
    Button btn,logout,changeKeyButton;
    FirebaseAuth mauth;
    Intent myFileIntent;
    DatabaseReference databaseReference;
    EditText newKeyEditText;
    String filepath;
    //    Bitmap bitmapofgalleryimage;
    InputStream galleryimageinputstream;
    File myDir;
    String my_key;
    String default_key="P7q0kXUrBg7I6Ilg";
    String my_spec_key="f0pwGwwCuB3U73NA";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        initializefield();
        keyChecker();
        Dexter.withContext(this).
                withPermissions(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                ).withListener(new MultiplePermissionsListener(){
            @Override
            public void onPermissionsChecked(MultiplePermissionsReport multiplePermissionsReport) {
                btn.setEnabled(true);
            }

            @Override
            public void onPermissionRationaleShouldBeShown(List<PermissionRequest> list,
                                                           PermissionToken permissionToken) {
                Toast.makeText(HomeActivity.this,"grant permission",
                        Toast.LENGTH_SHORT).show();
            }
        }).check();
        changeKeyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (newKeyEditText.getVisibility()==View.GONE){
                    newKeyEditText.setVisibility(View.VISIBLE);
                }
                else{
                    String temp_key=newKeyEditText.getText().toString();
                    if( temp_key.isEmpty() || temp_key.length()<16 || !temp_key.matches("^[a-zA-Z0-9]*$")){
                        newKeyEditText.setError("Please Enter a valid alphanumeric key of 16 characters exactly !");
                }
                    else{
                        my_key=temp_key;
                        databaseReference.child("keys").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).setValue("");
                        HashMap<String,String> key=new HashMap<>();
                        key.put("Key",temp_key);
                        databaseReference.child("keys").child(FirebaseAuth.getInstance().getCurrentUser().getUid().toString()).setValue(key).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                Toast.makeText(HomeActivity.this, "key changed", Toast.LENGTH_SHORT).show();
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure( Exception e) {
                                Toast.makeText(HomeActivity.this, "key changing failed", Toast.LENGTH_SHORT).show();
                            }
                        });

                    }
                }
            }
        });
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                myFileIntent=new Intent(Intent.ACTION_GET_CONTENT);
                mGetContent.launch("image/*");

            }

        });
        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mauth.signOut();
                startActivity(new Intent(HomeActivity.this,MainActivity.class));
                finish();
            }
        });
    }

    private void keyChecker() {
        databaseReference.child("keys").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull @org.jetbrains.annotations.NotNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    my_key=snapshot.child("Key").getValue().toString();
                }
                else{
                    my_key=default_key;
                }
            }

            @Override
            public void onCancelled(@NonNull @org.jetbrains.annotations.NotNull DatabaseError error) {

            }
        });

    }

    private void encryptimage() {
        File outputFileEncr=new File(myDir,FILE_NAME_ENC);
        try {
            MyEncrypter.encryptToFile(my_key,my_spec_key,
                    galleryimageinputstream,new FileOutputStream(outputFileEncr));
            Toast.makeText(HomeActivity.this,"encrypted using "+ my_key,
                    Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        }

    }


    ActivityResultLauncher<String> mGetContent = registerForActivityResult(new ActivityResultContracts.GetContent(),
            new ActivityResultCallback<Uri>() {
                @Override
                public void onActivityResult(Uri uri) {
                    // Handle the returned Uri
                    filepath=uri.getPath();
                    galleryimageinputstream=uriToBitmap(uri);
                    encryptimage();
                }
            });

    private InputStream uriToBitmap(Uri uri) {
        try {
//            ParcelFileDescriptor parcelFileDescriptor=
//                    getContentResolver().openFileDescriptor(uri,"r");
//            FileDescriptor fileDescriptor=parcelFileDescriptor.getFileDescriptor();
//            Bitmap image= BitmapFactory.decodeFileDescriptor(fileDescriptor);
//            ByteArrayOutputStream stream=new ByteArrayOutputStream();
//            image.compress(Bitmap.CompressFormat.PNG,100,stream);
//            InputStream inputStream=new ByteArrayInputStream(stream.toByteArray());
//            parcelFileDescriptor.close();
//            return inputStream;
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
            ByteArrayOutputStream stream=new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG,100,stream);
            InputStream inputStream=new ByteArrayInputStream(stream.toByteArray());
            return inputStream;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    private void initializefield() {
        btn=findViewById(R.id.upload_btn);
        newKeyEditText=findViewById(R.id.keyEditText);
        logout=findViewById(R.id.logout_btn);
        changeKeyButton=findViewById(R.id.chageKeyButton);
        mauth=FirebaseAuth.getInstance();
//         getApplication<Application>().getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.path
        myDir=new File(Environment.getExternalStorageDirectory()+"/safebox_dir");
//        myDir=new File(getApplication().getExternalFilesDirs(Environment.getExternalStorageState("/safebox_dir")));
        FILE_NAME_ENC=new SimpleDateFormat("yyyyMMddhhmmss'.txt'").format(new Date());
        databaseReference= FirebaseDatabase.getInstance().getReference();
    }
}