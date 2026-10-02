package com.example.nollybox;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class ActorAdapter extends RecyclerView.Adapter<ActorAdapter.ActorViewHolder> {

    private List<Actor> actorList;

    public ActorAdapter(List<Actor> actorList) {
        this.actorList = actorList;
    }

    @NonNull
    @Override
    public ActorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_actor, parent, false);
        return new ActorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ActorViewHolder holder, int position) {
        Actor actor = actorList.get(position);
        holder.tvName.setText(actor.getName());
        Glide.with(holder.itemView.getContext())
                .load(actor.getImageUrl())
                .placeholder(android.R.drawable.ic_menu_myplaces) // Professional Profile Placeholder
                .error(android.R.drawable.ic_menu_myplaces)       // Fallback if URL is blocked/broken
                .circleCrop()                                    // Ensure it's a perfect circle
                .into(holder.ivActor);
    }

    @Override
    public int getItemCount() {
        return actorList.size();
    }

    public static class ActorViewHolder extends RecyclerView.ViewHolder {
        ImageView ivActor;
        TextView tvName;

        public ActorViewHolder(@NonNull View itemView) {
            super(itemView);
            ivActor = itemView.findViewById(R.id.iv_actor);
            tvName = itemView.findViewById(R.id.tv_actor_name);
        }
    }
}