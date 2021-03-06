package app.paste_it.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import app.paste_it.R;
import app.paste_it.Utils;
import app.paste_it.models.ImageModel;

public class ImageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = ImageAdapter.class.getSimpleName();
    private List<ImageModel> items;

    public ImageAdapter() {
        items = new ArrayList<>();
    }

    private View.OnClickListener onClickListener;

    public ImageAdapter(Collection<ImageModel> images, View.OnClickListener onClickListener) {
        items = new ArrayList<>(images);
        this.onClickListener = onClickListener;
    }

    public List<ImageModel> getItems() {
        return items;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image, parent, false);
        view.setOnClickListener(onClickListener);
        return new RecyclerView.ViewHolder(view) {
        };
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ImageView imageView = (ImageView) holder.itemView;
        imageView.setTag(position);
        String path = Utils.getFullPath(imageView.getContext(),items.get(position).getFileName());
        File file = new File(path);
        if (file.exists()) {
            Picasso.with(imageView.getContext()).load(file).into(imageView);
        } else if(items.get(position).getDownloadURL()!=null){
            Picasso.with(imageView.getContext()).load(items.get(position).getDownloadURL()).into(imageView);
        } else{
            imageView.setImageBitmap(null);
        }
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    public void addItem(ImageModel imageModel) {
        items.add(imageModel);
        notifyItemInserted(getItemCount() - 1);
    }
}