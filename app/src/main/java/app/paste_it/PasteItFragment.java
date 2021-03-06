package app.paste_it;


import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.flexbox.JustifyContent;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;

import java.util.ArrayList;

import app.paste_it.adapters.ImageAdapter;
import app.paste_it.callbacks.ItemRemovedCallback;
import app.paste_it.models.Identity;
import app.paste_it.models.ImageModel;
import app.paste_it.models.Paste;
import app.paste_it.models.Tag;
import app.paste_it.service.ImageImportService;
import app.paste_it.service.ImageUploadService;
import butterknife.BindView;
import butterknife.ButterKnife;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PasteItFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PasteItFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener, View.OnClickListener, ItemRemovedCallback {

    private static final String TAG = PasteItFragment.class.getSimpleName();

    private static final int RC_SELECT_PICTURE = 1;
    private static final int RC_CAPTURE_IMAGE = 2;
    private static final String UID = FirebaseAuth.getInstance().getCurrentUser().getUid();
    private static final String ARG_PARAM1 = "param1";
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.rvImages)
    RecyclerView rvImages;
    @BindView(R.id.etTitle)
    EditText etTitle;
    @BindView(R.id.etContent)
    EditText etContent;
    @BindView(R.id.tvLastUpdated)
    TextView tvLastUpdated;
    @BindView(R.id.llTagHolder)
    LinearLayout llTagHolder;
    private ImageAdapter imageAdapter;
    private Paste paste;

    public PasteItFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param paste The Paste being shown in this fragment
     * @return A new instance of fragment PasteItFragment.
     */
    public static PasteItFragment newInstance(Paste paste) {
        PasteItFragment fragment = new PasteItFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_PARAM1, paste);
        fragment.setArguments(args);
        return fragment;
    }

    public Paste getPaste() {
        return paste;
    }

    public void setPaste(Paste paste) {
        this.paste = paste;
        savePaste();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //when fragment is first launched
        if (getArguments() != null) {
            paste = getArguments().getParcelable(ARG_PARAM1);
        }
        PreferenceManager.getDefaultSharedPreferences(getContext()).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(getString(R.string.key_paste), paste);
        outState.putParcelable(getString(R.string.key_ll_os), rvImages.getLayoutManager().onSaveInstanceState());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_paste_it, menu);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        PreferenceManager.getDefaultSharedPreferences(getContext()).unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                savePaste();
                NavUtils.navigateUpFromSameTask(getActivity());
                break;
            case R.id.miAttachImageFromCamera:
                launchCameraActivity();
                break;
            case R.id.miAttachImageFromFile:
                pickImage();
                break;
            case R.id.miTag:
                showTagFragment();

            default:
                super.onOptionsItemSelected(item);
        }
        return super.onOptionsItemSelected(item);
    }

    private void launchCameraActivity() {
        Utils.verifyStoragePermissions(getActivity());
        Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent,RC_CAPTURE_IMAGE);
    }

    private void showTagFragment() {
        TagFragment tagFragment = TagFragment.newInstance(paste);
        getActivity().getSupportFragmentManager().beginTransaction().addToBackStack(getString(R.string.tag_tag_fragment)).add(R.id.frame, tagFragment, getString(R.string.tag_tag_fragment)).commit();
    }

    private void pickImage() {
        Utils.verifyStoragePermissions(getActivity());
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_PICK);
        startActivityForResult(Intent.createChooser(intent, getString(R.string.select_picture)), RC_SELECT_PICTURE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);



        if (resultCode == Activity.RESULT_OK && data != null) {

            if (requestCode == RC_SELECT_PICTURE || requestCode == RC_CAPTURE_IMAGE) {
                String pasteId = savePaste();
                String id = String.valueOf(System.currentTimeMillis());
                ImageModel imageModel = new ImageModel();
                imageModel.setId(id);
                imageModel.setPasteId(pasteId);
                if (requestCode == RC_SELECT_PICTURE && data.getData()!=null) {
                    imageModel.setFileName(id + "_" + PasteUtils.getFileName(getContext(), data.getData()));
                    paste.getUrls().put(imageModel.getId(), imageModel);
                    ImageImportService.startActionImport(getContext(), data.getData(), imageModel);
                    imageAdapter.addItem(imageModel);
                }
                if (requestCode == RC_CAPTURE_IMAGE) {
                    Log.d(TAG,"RESCODE: "+ resultCode+" REQCODE: "+requestCode + " Data: "+data.getExtras().get("data"));
                    Bitmap bitmap = (Bitmap) data.getExtras().get("data");
                    imageModel.setFileName(id+"_Camera_Capture.png");
                    paste.getUrls().put(imageModel.getId(),imageModel);
                    ImageImportService.startActionImportPicture(getContext(),bitmap,imageModel);
                    imageAdapter.addItem(imageModel);
                }
            }
        }
    }

    public String savePaste() {
        if (paste == null)
            paste = new Paste();
        String title = etTitle.getText().toString();
        String content = etContent.getText().toString();
        if (paste.getCreated() == 0)
            paste.setCreated(System.currentTimeMillis());
        paste.setModified(System.currentTimeMillis());
        paste.setTitle(title);
        paste.setText(content);

        String id;
        DatabaseReference newPasteRef;
        if (paste.getId() == null) {
            newPasteRef = FirebaseDatabase.getInstance().getReference("pastes/" + UID).push();
            id = newPasteRef.getKey();
            paste.setId(id);
        } else {
            id = paste.getId();
            newPasteRef = FirebaseDatabase.getInstance().getReference("pastes/" + UID).child(id);
        }
        newPasteRef.setValue(paste);

        if (paste.getModified() != 0) {

            tvLastUpdated.setVisibility(View.VISIBLE);
            tvLastUpdated.setText(String.format(getString(R.string.changes_saved),PasteUtils.getAgoString(paste.getModified())));
        }
        return id;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d(TAG, "Preferences changed with key: " + key);
        //if we are still here, the user has imported the images and doing some other task
        //The images have been loaded locally, but not yet uploaded to the cloud
        Gson gson = new Gson();
        if (key.equals(getString(R.string.key_image_model_updated))) {
            String jsonString = sharedPreferences.getString(key, "");
            ImageModel imageModel = gson.fromJson(jsonString, ImageModel.class);
            paste.getUrls().put(imageModel.getId(), imageModel);
            int index = PasteUtils.findIndex(imageAdapter.getItems(), imageModel);
            Log.d(TAG, "Index is: " + index);
            Log.d(TAG, "Items in Adapter are: " + imageAdapter.getItems());
            if (index > -1) {
                imageAdapter.getItems().set(index, imageModel);
                imageAdapter.notifyDataSetChanged();
            }
            //start the image upload service
            Intent intent = new Intent(getContext(), ImageUploadService.class);
            intent.setAction(ImageUploadService.ACTION_IMAGE_UPLOAD);
            getContext().startService(intent);
        }
        if (key.equals(getString(R.string.key_dload_uri_available))) {
            String jsonString = sharedPreferences.getString(key,"");
            ImageModel imageModel = gson.fromJson(jsonString, ImageModel.class);
            if (imageModel.getPasteId().equals(paste.getId())) {
                paste.getUrls().put(imageModel.getId(), imageModel);
                int index = PasteUtils.findIndexOfItemWithId(imageAdapter.getItems(), imageModel.getId());
                Log.d(TAG, "Index is: " + index);
                if (index > -1) {
                    imageAdapter.getItems().set(index, imageModel);
                    imageAdapter.notifyDataSetChanged();
                }
            }
        }
        //remove image
        if(key.equals(getString(R.string.key_sp_image_removed_id))){
            String id = sharedPreferences.getString(key, null);
            if(id!=null){
                paste.getUrls().remove(id);
                int index = PasteUtils.findIndexOfItemWithId(imageAdapter.getItems(),id);
                if(index > -1){
                    imageAdapter.getItems().remove(index);
                    imageAdapter.notifyItemRemoved(index);
                }
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_paste_it, container, false);
        ButterKnife.bind(this, view);

        AppCompatActivity appCompatActivity = (AppCompatActivity) getActivity();
        appCompatActivity.setSupportActionBar(toolbar);
        appCompatActivity.setTitle(getString(R.string.paste_it));
        setHasOptionsMenu(true);

        appCompatActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        FlexboxLayoutManager flexboxLayoutManager = new FlexboxLayoutManager(FlexDirection.ROW, JustifyContent.FLEX_END);
        rvImages.setLayoutManager(flexboxLayoutManager);

        //when fragment is recreated
        if (savedInstanceState != null) {
            paste = savedInstanceState.getParcelable(getString(R.string.key_paste));
            Parcelable lloS = savedInstanceState.getParcelable(getString(R.string.key_ll_os));
            flexboxLayoutManager.onRestoreInstanceState(lloS);
        }
        imageAdapter = new ImageAdapter(paste.getUrls().values(), this);
        rvImages.setAdapter(imageAdapter);

        etTitle.setText(paste.getTitle());
        etContent.setText(paste.getText());
        addTags();

        llTagHolder.setOnClickListener(this);
        if (paste.getModified() != 0) {
            tvLastUpdated.setVisibility(View.VISIBLE);
            tvLastUpdated.setText(String.format(getString(R.string.last_modified),PasteUtils.getAgoString(paste.getModified())));
        }
        return view;
    }


    public void addTags() {
        llTagHolder.removeAllViews();
        for (Tag tag : paste.getTags().values()) {
            TextView textView = (TextView) LayoutInflater.from(getContext()).inflate(R.layout.item_textview_tag, llTagHolder,false);
            textView.setText(tag.getLabel());
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutParams.setMargins(16, 16, 16, 16);
            textView.setLayoutParams(layoutParams);
            llTagHolder.addView(textView);
        }
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.llTagHolder:
                showTagFragment();
                break;
            case R.id.ivImage:
                int positionClicked = Integer.parseInt(v.getTag().toString());
                launchImagePagerActivity(positionClicked);
                break;
        }
    }

    private void launchImagePagerActivity(int position){
        Intent intent = new Intent(getContext(),ImagePagerActivity.class);
        intent.putParcelableArrayListExtra(getString(R.string.key_image_models),(ArrayList<? extends Parcelable>) imageAdapter.getItems());
        intent.putExtra(getString(R.string.key_position), position);
        startActivity(intent);
    }

    @Override
    public void onItemRemoved(Identity item) {
        String id = item.getId();
        if(item instanceof ImageModel){
            ImageAdapter imageAdapter = (ImageAdapter)rvImages.getAdapter();
            int index = PasteUtils.findIndexOfItemWithId(imageAdapter.getItems(),id);
            if(index > -1){
                imageAdapter.getItems().remove(index);
                imageAdapter.notifyItemRemoved(index);
            }
            paste.getUrls().remove(id);
        }
    }
}
