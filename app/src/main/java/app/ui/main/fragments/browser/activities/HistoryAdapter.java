package app.ui.main.fragments.browser.activities;

import static app.core.AIOApp.INSTANCE;
import static lib.networks.URLUtilityKT.removeWwwFromUrl;
import static lib.process.AsyncJobUtils.executeInBackground;
import static lib.process.AsyncJobUtils.executeOnMainThread;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aio.R;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import app.core.bases.BaseActivity;
import app.core.engines.browser.history.HistoryModel;
import app.core.engines.caches.AIOFavicons;
import lib.device.DateTimeUtils;

/**
 * Adapter class for displaying browser history items in a list.
 * It supports lazy loading of history records and callback interactions for item clicks and long clicks.
 */
public class HistoryAdapter extends BaseAdapter {

    // Holds a weak reference to the activity context to avoid memory leaks.
    private final WeakReference<BaseActivity> safeBaseActivityRef;

    // Callback for history item click events.
    @Nullable
    private final OnHistoryItemClick onHistoryItemClick;

    // Callback for history item long-click events.
    @Nullable
    private final OnHistoryItemLongClick onHistoryItemLongClick;

    // Number of history items currently loaded into the adapter.
    private int currentIndex = 0;

    // Snapshot of currently displayed history items.
    private final List<HistoryModel> displayedHistory = new ArrayList<>();

    /**
     * Constructor to initialize the adapter.
     *
     * @param historyActivity         The activity using this adapter.
     * @param onHistoryItemClick      Click listener for history items.
     * @param onHistoryItemLongClick  Long-click listener for history items.
     */
    public HistoryAdapter(@Nullable HistoryActivity historyActivity,
                          @Nullable OnHistoryItemClick onHistoryItemClick,
                          @Nullable OnHistoryItemLongClick onHistoryItemLongClick) {
        this.safeBaseActivityRef = new WeakReference<>(historyActivity);
        this.onHistoryItemClick = onHistoryItemClick;
        this.onHistoryItemLongClick = onHistoryItemLongClick;
        loadMoreHistory(); // Load initial batch of history records
    }

    /**
     * Returns the number of history items currently displayed.
     */
    @Override
    public int getCount() {
        return displayedHistory.size();
    }

    /**
     * Returns the history model at a given position.
     */
    @Nullable
    @Override
    public HistoryModel getItem(int position) {
        if (position >= 0 && position < displayedHistory.size()) {
            return displayedHistory.get(position);
        }
        return null;
    }

    /**
     * Returns the item ID for the history entry.
     */
    @Override
    public long getItemId(int position) {
        return position;
    }

    /**
     * Returns the view for each history row.
     */
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        BaseActivity activity = this.safeBaseActivityRef.get();
        if (activity == null) {
            return convertView != null ? convertView : new View(parent.getContext());
        }

        ViewHolder holder;

        // Inflate new view if needed
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(activity);
            convertView = inflater.inflate(R.layout.activity_browser_history_1_row_1, parent, false);

            holder = new ViewHolder();
            holder.historyFavicon = convertView.findViewById(R.id.history_url_favicon_indicator);
            holder.historyTitle = convertView.findViewById(R.id.history_url_title);
            holder.historyDate = convertView.findViewById(R.id.history_url_date);
            holder.historyUrl = convertView.findViewById(R.id.history_url);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        // Populate view with history data
        HistoryModel historyModel = getItem(position);
        if (historyModel != null) {
            holder.historyTitle.setText(historyModel.getHistoryTitle());
            holder.historyUrl.setText(removeWwwFromUrl(historyModel.getHistoryUrl()));

            Date visitedDate = historyModel.getHistoryVisitDateTime();
            String formattedDate = DateTimeUtils.formatDateWithSuffix(visitedDate);
            holder.historyDate.setText(formattedDate);

            // Handle click event
            convertView.setOnClickListener(view -> {
                if (onHistoryItemClick != null) {
                    onHistoryItemClick.onHistoryItemClick(historyModel);
                }
            });

            // Handle long click event
            convertView.setOnLongClickListener(view -> {
                if (onHistoryItemLongClick != null) {
                    onHistoryItemLongClick.onHistoryItemLongClick(historyModel, position, view);
                }
                return true;
            });

            // Load favicon asynchronously
            executeInBackground(() -> {
                AIOFavicons aioFavicon = INSTANCE.getAIOFavicon();
                String faviconCachedPath = aioFavicon.getFavicon(historyModel.getHistoryUrl());
                if (faviconCachedPath != null && !faviconCachedPath.isEmpty()) {
                    File faviconImg = new File(faviconCachedPath);
                    if (faviconImg.exists()) {
                        executeOnMainThread(() ->
                                holder.historyFavicon.setImageURI(Uri.fromFile(faviconImg)));
                    }
                }
            });
        }

        return convertView;
    }

    /**
     * Loads more history entries from the source list in chunks (default 50).
     */
    public void loadMoreHistory() {
        List<HistoryModel> fullList = INSTANCE.getAIOHistory().getHistoryLibrary();

        if (currentIndex >= fullList.size()) {
            return;
        }

        int itemsToLoad = Math.min(50, fullList.size() - currentIndex);
        int endIndex = currentIndex + itemsToLoad;

        for (int i = currentIndex; i < endIndex; i++) {
            displayedHistory.add(fullList.get(i));
        }

        currentIndex = endIndex;
        notifyDataSetChanged();
    }

    /**
     * Resets the adapter to initial state (no history shown).
     */
    public void resetHistoryAdapter() {
        currentIndex = 0;
        displayedHistory.clear();
        notifyDataSetChanged();
    }

    /**
     * Interface definition for a callback to be invoked when a history item is clicked.
     */
    public interface OnHistoryItemClick {
        void onHistoryItemClick(@NonNull HistoryModel historyModel);
    }

    /**
     * Interface definition for a callback to be invoked when a history item is long-clicked.
     */
    public interface OnHistoryItemLongClick {
        void onHistoryItemLongClick(@NonNull HistoryModel historyModel,
                                    int position, @NonNull View listView);
    }

    /**
     * ViewHolder class to cache the history row views.
     */
    private static class ViewHolder {
        ImageView historyFavicon;
        TextView historyTitle;
        TextView historyUrl;
        TextView historyDate;
    }
}
