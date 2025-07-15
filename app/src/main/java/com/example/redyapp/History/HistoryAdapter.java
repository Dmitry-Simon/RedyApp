package com.example.redyapp.History;

import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.redyapp.R;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private List<HistoryItem> historyItems = new ArrayList<>();
    private final OnHistoryItemInteractionListener listener;

    private MediaPlayer mediaPlayer;
    private int currentlyPlayingPosition = -1;
    private HistoryViewHolder playingHolder;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public interface OnHistoryItemInteractionListener {
        void onDeleteClicked(HistoryItem historyItem);
    }

    public HistoryAdapter(OnHistoryItemInteractionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.history_item_layout, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        HistoryItem currentItem = historyItems.get(position);
        holder.bind(currentItem);
    }

    @Override
    public int getItemCount() {
        return historyItems.size();
    }

    public void setHistoryItems(List<HistoryItem> newHistoryItems) {
        this.historyItems = newHistoryItems;
        notifyDataSetChanged();
    }

    public void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            handler.removeCallbacks(updateSeekBar);
            mediaPlayer.release();
            mediaPlayer = null;
            currentlyPlayingPosition = -1;
        }
    }

    // Moved the Runnable here to be accessible by the whole adapter
    private final Runnable updateSeekBar = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null && mediaPlayer.isPlaying() && playingHolder != null) {
                int currentPos = mediaPlayer.getCurrentPosition();
                playingHolder.seekBar.setProgress(currentPos);

                String currentTimeStr = String.format(Locale.getDefault(), "%02d:%02d",
                        TimeUnit.MILLISECONDS.toMinutes(currentPos),
                        TimeUnit.MILLISECONDS.toSeconds(currentPos) -
                                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(currentPos))
                );
                String totalTimeStr = String.format(Locale.getDefault(), "%02d:%02d",
                        TimeUnit.MILLISECONDS.toMinutes(mediaPlayer.getDuration()),
                        TimeUnit.MILLISECONDS.toSeconds(mediaPlayer.getDuration()) -
                                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(mediaPlayer.getDuration()))
                );

                playingHolder.durationTextView.setText(String.format("%s / %s", currentTimeStr, totalTimeStr));

                handler.postDelayed(this, 200);
            }
        }
    };

    class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView dateTextView, durationTextView;
        ImageView playPauseButton, deleteButton;
        SeekBar seekBar;

        HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            dateTextView = itemView.findViewById(R.id.history_item_date);
            durationTextView = itemView.findViewById(R.id.history_item_duration);
            playPauseButton = itemView.findViewById(R.id.history_item_play_pause);
            deleteButton = itemView.findViewById(R.id.history_item_delete);
            seekBar = itemView.findViewById(R.id.history_item_seekbar);
        }

        void bind(final HistoryItem item) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yy, HH:mm", Locale.getDefault());
            dateTextView.setText(item.getTimestamp() != null ? sdf.format(item.getTimestamp()) : "No date");

            if (getAdapterPosition() == currentlyPlayingPosition) {
                playingHolder = this;
                updatePlayingView();
            } else {
                updateNonPlayingView();
            }

            playPauseButton.setOnClickListener(v -> handlePlayPauseClick(item));

            deleteButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClicked(item);
                }
            });

            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && mediaPlayer != null && getAdapterPosition() == currentlyPlayingPosition) {
                        mediaPlayer.seekTo(progress);
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }

        private void handlePlayPauseClick(HistoryItem item) {
            if (getAdapterPosition() == currentlyPlayingPosition) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                } else {
                    mediaPlayer.start();
                }
                updatePlayingView();
            } else {
                releaseMediaPlayer();
                currentlyPlayingPosition = getAdapterPosition();
                playingHolder = this;
                startPlaying(item.getLocalAudioPath());
            }
        }

        private void startPlaying(String localPath) {
            mediaPlayer = new MediaPlayer();
            try {
                mediaPlayer.setDataSource(localPath);
                mediaPlayer.setOnPreparedListener(mp -> {
                    mp.start();
                    updatePlayingView();
                });
                mediaPlayer.setOnCompletionListener(mp -> {
                    releaseMediaPlayer();
                    updateNonPlayingView();
                });
                mediaPlayer.prepareAsync();
            } catch (IOException e) {
                Log.e("HistoryAdapter", "MediaPlayer prepare() failed", e);
                releaseMediaPlayer();
            }
        }

        void updatePlayingView() {
            if (mediaPlayer != null) {
                seekBar.setMax(mediaPlayer.getDuration());
                if (mediaPlayer.isPlaying()) {
                    playPauseButton.setImageResource(R.drawable.play);
                    handler.post(updateSeekBar);
                } else {
                    playPauseButton.setImageResource(R.drawable.play);
                    handler.removeCallbacks(updateSeekBar);
                }
            }
        }

        void updateNonPlayingView() {
            playPauseButton.setImageResource(R.drawable.play);
            seekBar.setProgress(0);
            durationTextView.setText("00:00 / 00:05");
        }
    }
}