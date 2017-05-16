package app.paste_it.adapters;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import app.paste_it.R;
import app.paste_it.models.greendao.Paste;
import app.paste_it.models.holders.LoadingHolder;
import app.paste_it.models.holders.PasteHolder;
import app.paste_it.models.holders.TextHolder;
import butterknife.BindView;

/**
 * Created by Madeyedexter on 16-05-2017.
 */

public class PasteAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = PasteAdapter.class.getSimpleName();

    private boolean loading=false;

    public void setLoading(boolean loading) {
        this.loading = loading;
        notifyItemChanged(getItemCount()-1);
    }

    public void setEnded(boolean ended) {
        this.ended = ended;
        notifyItemChanged(getItemCount()-1);
    }

    public void setError(boolean error) {
        this.error = error;
        notifyItemChanged(getItemCount()-1);
    }

    private boolean ended =false;
    private boolean error =false;


    public static final int ITEM_TYPE_DATA = 0;
    public static final int ITEM_TYPE_LOADING = 1;
    public static final int ITEM_TYPE_ENDED = 2;
    public static final int ITEM_TYPE_ERROR = 3;
    public static final int ITEM_TYPE_EMPTY = 4;
    public static final int ITEM_TYPE_IDLE = 5;


    private List<Paste> pastes;

    public void addPaste(int i, Paste paste) {
        pastes.add(i,paste);
        notifyItemInserted(i);
    }

    public interface ThumbClickListener{
        void onThumbClicked(Paste paste);
    }

    public void setPastes(List<Paste> pastes) {
        this.pastes = pastes;
        notifyDataSetChanged();
    }

    public ThumbClickListener clickListener;

    public PasteAdapter(ThumbClickListener clickListener){
        this.clickListener=clickListener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Log.d(TAG,"Item Type is: "+viewType);
        switch(viewType){
            case ITEM_TYPE_DATA: //default item
                //Log.d(TAG,"Created MovieHolder");
                return new PasteHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_paste,parent,false));
            case ITEM_TYPE_LOADING: //Loading indicator
                Log.d(TAG,"Created LoadingHolder");
                return new LoadingHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_loading,parent,false));
            default: //ITEM_TYPE_ENDED|ITEM_TYPE_ERROR|ITEM_TYPE_EMPTY
                Log.d(TAG,"Created TextHolder");
                return new TextHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_text_message,parent,false));
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        switch (getItemViewType(position)){
            case ITEM_TYPE_DATA: ((PasteHolder)holder).bindData(pastes.get(position));
                break;
            case ITEM_TYPE_LOADING: break;
            //all others
            case ITEM_TYPE_EMPTY: ((TextHolder)holder).setLightEmptyMessage("No Items to show here.");
                break;
            case ITEM_TYPE_ERROR: ((TextHolder)holder).setLightMessage("An error occurred while fetching data");
                break;
            case ITEM_TYPE_ENDED: ((TextHolder)holder).setLightMessage("End of Content.");
                break;
            case ITEM_TYPE_IDLE: ((TextHolder)holder).tvMessage.setVisibility(View.GONE);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return pastes==null?1:pastes.size()+1;
    }

    //resets state variables
    public void resetSpecialStates(){
        loading=ended=error=false;
        notifyItemChanged(getItemCount()-1);
    }

    @Override
    public int getItemViewType(int position) {
        //The check for last position
        if(getItemCount()-1==position && loading)
            return ITEM_TYPE_LOADING;
        if(getItemCount()-1==position && ended)
            return ITEM_TYPE_ENDED;
        if(getItemCount()-1==position && error)
            return ITEM_TYPE_ERROR;
        if(getItemCount()-1==position && getItemCount()==1)
            return ITEM_TYPE_EMPTY;
        if(getItemCount()-1==position)
            return ITEM_TYPE_IDLE;
        return ITEM_TYPE_DATA;
    }



    public void clear(){
        if(pastes!=null)
            pastes.clear();
        resetSpecialStates();
        notifyDataSetChanged();
    }
}