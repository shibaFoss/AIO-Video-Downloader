package app.ui.main.fragments.browser.activities;

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

import app.core.AIOApp;
import app.core.bases.BaseActivity;
import app.core.engines.browser.bookmarks.BookmarkModel;
import app.core.engines.caches.AIOFavicons;
import lib.device.DateTimeUtils;

/**
 * Adapter class for displaying browser bookmarks in a list.
 * It supports lazy loading of bookmarks and callback interactions for item clicks and long clicks.
 */
public class BookmarkAdapter extends BaseAdapter {

    // Holds a weak reference to the activity context to avoid memory leaks.
    private final WeakReference<BaseActivity> safeBaseActivityRef;

    // Callback for bookmark click events.
    @Nullable
    private final OnBookmarkItemClick onBookmarkItemClick;

    // Callback for bookmark long-click events.
    @Nullable
    private final OnBookmarkItemLongClick onBookmarkItemLongClick;

    // Number of bookmarks currently loaded into the adapter.
    private int currentIndex = 0;

    // Snapshot of currently displayed bookmarks to prevent data shift
    private final List<BookmarkModel> displayedBookmarks = new ArrayList<>();

    /**
     * Constructor to initialize the adapter.
     *
     * @param bookmarkActivity        The activity using this adapter.
     * @param onBookmarkItemClick     Click listener for bookmark items.
     * @param onBookmarkItemLongClick Long-click listener for bookmark items.
     */
    public BookmarkAdapter(@Nullable BookmarksActivity bookmarkActivity,
                           @Nullable OnBookmarkItemClick onBookmarkItemClick,
                           @Nullable OnBookmarkItemLongClick onBookmarkItemLongClick) {
        this.safeBaseActivityRef = new WeakReference<>(bookmarkActivity);
        this.onBookmarkItemClick = onBookmarkItemClick;
        this.onBookmarkItemLongClick = onBookmarkItemLongClick;
        loadMoreBookmarks(); // Load initial batch of bookmarks
    }

    /**
     * Returns the number of bookmarks currently displayed.
     */
    @Override
    public int getCount() {
        return displayedBookmarks.size();
    }

    /**
     * Returns the bookmark model at a given position.
     */
    @Nullable
    @Override
    public BookmarkModel getItem(int position) {
        if (position >= 0 && position < displayedBookmarks.size()) {
            return displayedBookmarks.get(position);
        }
        return null;
    }

    /**
     * Returns the item ID for the bookmark.
     */
    @Override
    public long getItemId(int position) {
        return position;
    }

    /**
     * Returns the view for each bookmark row.
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
            convertView = inflater.inflate(R.layout.activity_bookmarks_1_row_1, parent, false);

            holder = new ViewHolder();
            holder.bookmarkFavicon = convertView.findViewById(R.id.bookmark_url_favicon_indicator);
            holder.bookmarkTitle = convertView.findViewById(R.id.bookmark_url_title);
            holder.bookmarkDate = convertView.findViewById(R.id.bookmark_url_date);
            holder.bookmarkUrl = convertView.findViewById(R.id.bookmark_url);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        // Populate view with bookmark data
        BookmarkModel bookmarkModel = getItem(position);
        if (bookmarkModel != null) {
            holder.bookmarkTitle.setText(bookmarkModel.getBookmarkName());
            holder.bookmarkUrl.setText(removeWwwFromUrl(bookmarkModel.getBookmarkUrl()));

            Date visitedDate = bookmarkModel.getBookmarkCreationDate();
            String formattedDate = DateTimeUtils.formatDateWithSuffix(visitedDate);
            holder.bookmarkDate.setText(formattedDate);

            // Handle click event
            convertView.setOnClickListener(view -> {
                if (onBookmarkItemClick != null) {
                    onBookmarkItemClick.onBookmarkClick(bookmarkModel);
                }
            });

            // Handle long click event
            convertView.setOnLongClickListener(view -> {
                if (onBookmarkItemLongClick != null) {
                    onBookmarkItemLongClick.onBookmarkLongClick(bookmarkModel, position, view);
                }
                return true;
            });

            // Load favicon asynchronously
            executeInBackground(() -> {
                AIOFavicons aioFavicon = AIOApp.INSTANCE.getAIOFavicon();
                String faviconCachedPath = aioFavicon.getFavicon(bookmarkModel.getBookmarkUrl());
                if (faviconCachedPath != null && !faviconCachedPath.isEmpty()) {
                    File faviconImg = new File(faviconCachedPath);
                    if (faviconImg.exists()) {
                        executeOnMainThread(() ->
                                holder.bookmarkFavicon.setImageURI(Uri.fromFile(faviconImg)));
                    }
                }
            });
        }

        return convertView;
    }

    /**
     * Loads more bookmarks from the source list in chunks (default 50)
     */
    public void loadMoreBookmarks() {
        List<BookmarkModel> fullList = AIOApp.INSTANCE.getAIOBookmarks().getBookmarkLibrary();

        // If we've already loaded everything, return
        if (currentIndex >= fullList.size()) {
            return;
        }

        // Calculate how many items to load (either 50 or remaining items)
        int itemsToLoad = Math.min(50, fullList.size() - currentIndex);
        int endIndex = currentIndex + itemsToLoad;

        // Add the new items
        for (int i = currentIndex; i < endIndex; i++) {
            displayedBookmarks.add(fullList.get(i));
        }

        currentIndex = endIndex;
        notifyDataSetChanged();
    }

    /**
     * Resets the adapter to initial state (no bookmarks shown).
     */
    public void resetBookmarkAdapter() {
        currentIndex = 0;
        displayedBookmarks.clear();
        notifyDataSetChanged();
    }

    /**
     * Interface definition for a callback to be invoked when a bookmark is clicked.
     */
    public interface OnBookmarkItemClick {
        void onBookmarkClick(@NonNull BookmarkModel bookmarkModel);
    }

    /**
     * Interface definition for a callback to be invoked when a bookmark is long-clicked.
     */
    public interface OnBookmarkItemLongClick {
        void onBookmarkLongClick(@NonNull BookmarkModel bookmarkModel,
                                 int position, @NonNull View listView);
    }

    /**
     * ViewHolder class to cache the bookmark row views.
     */
    private static class ViewHolder {
        ImageView bookmarkFavicon;
        TextView bookmarkTitle;
        TextView bookmarkUrl;
        TextView bookmarkDate;
    }
}
