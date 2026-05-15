package com.example.elderlauncher.ui;

import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.elderlauncher.R;
import com.example.elderlauncher.data.FamilyContact;

import java.util.ArrayList;
import java.util.List;

public class FamilyContactAdapter extends RecyclerView.Adapter<FamilyContactAdapter.ViewHolder> {

    public interface ContactActionListener {
        void onCall(FamilyContact contact);

        void onEdit(FamilyContact contact);
    }

    private final List<FamilyContact> items = new ArrayList<>();
    private final boolean editable;
    private final ContactActionListener listener;

    public FamilyContactAdapter(boolean editable, ContactActionListener listener) {
        this.editable = editable;
        this.listener = listener;
    }

    public void submitList(List<FamilyContact> contacts) {
        items.clear();
        if (contacts != null) {
            items.addAll(contacts);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_family_contact, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FamilyContact item = items.get(position);
        holder.name.setText(item.name);

        if (!TextUtils.isEmpty(item.avatarUri)) {
            holder.avatar.setImageURI(Uri.parse(item.avatarUri));
        } else {
            holder.avatar.setImageResource(R.drawable.ic_avatar_placeholder);
        }

        if (editable) {
            holder.action.setText(R.string.edit_contact);
            holder.itemView.setOnClickListener(v -> listener.onEdit(item));
            holder.action.setOnClickListener(v -> listener.onEdit(item));
        } else {
            holder.action.setText(R.string.tap_to_call);
            holder.itemView.setOnClickListener(v -> listener.onCall(item));
            holder.action.setOnClickListener(v -> listener.onCall(item));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView avatar;
        final TextView name;
        final TextView action;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.ivAvatar);
            name = itemView.findViewById(R.id.tvName);
            action = itemView.findViewById(R.id.tvAction);
        }
    }
}
