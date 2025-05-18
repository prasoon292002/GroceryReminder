package com.example.groceryreminder;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.groceryreminder.data.GroceryItem;

import java.util.List;

public class GroceryItemAdapter extends RecyclerView.Adapter<GroceryItemAdapter.GroceryViewHolder> {
    private List<GroceryItem> items;
    private GroceryItemListener listener;

    public interface GroceryItemListener {
        void onItemStatusChanged(GroceryItem item);
        void onItemDeleted(GroceryItem item);
    }

    public GroceryItemAdapter(List<GroceryItem> items, GroceryItemListener listener) {
        this.items = items;
        this.listener = listener;
    }

    public void updateItems(List<GroceryItem> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GroceryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_grocery, parent, false);
        return new GroceryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroceryViewHolder holder, int position) {
        GroceryItem item = items.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class GroceryViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbCompleted;
        TextView tvItemName;
        TextView tvQuantity;
        ImageView ivDelete;

        public GroceryViewHolder(@NonNull View itemView) {
            super(itemView);
            cbCompleted = itemView.findViewById(R.id.cbCompleted);
            tvItemName = itemView.findViewById(R.id.tvItemName);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            ivDelete = itemView.findViewById(R.id.ivDelete);
        }

        public void bind(final GroceryItem item) {
            tvItemName.setText(item.getName());
            tvQuantity.setText("x" + item.getQuantity());
            cbCompleted.setChecked(item.isCompleted());

            // Set listeners
            cbCompleted.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    item.setCompleted(cbCompleted.isChecked());
                    listener.onItemStatusChanged(item);
                }
            });

            ivDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onItemDeleted(item);
                }
            });
        }
    }
}